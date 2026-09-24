package fish.monitoring.dao

import groovy.transform.CompileStatic
import jandcode.commons.UtCnv
import jandcode.commons.UtFile
import jandcode.commons.datetime.XDate
import jandcode.commons.datetime.XDateTime
import jandcode.commons.datetime.XDateTimeFormatter
import jandcode.commons.error.XError
import jandcode.commons.variant.VariantMap
import jandcode.core.auth.AuthService
import jandcode.core.dao.DaoMethod
import jandcode.core.dbm.mdb.BaseMdbUtils
import jandcode.core.store.Store
import jandcode.core.store.StoreField
import jandcode.core.store.StoreIndex
import jandcode.core.store.StoreRecord
import tofi.api.dta.ApiMonitoringData
import tofi.api.dta.ApiNSIData
import tofi.api.dta.model.utils.EntityMdbUtils
import tofi.api.dta.model.utils.PeriodGenerator
import tofi.api.dta.model.utils.UtPeriod
import tofi.api.mdl.ApiMeta
import tofi.api.mdl.model.consts.FD_AttribValType_consts
import tofi.api.mdl.model.consts.FD_InputType_consts
import tofi.api.mdl.model.consts.FD_PeriodType_consts
import tofi.api.mdl.model.consts.FD_PropType_consts
import tofi.api.mdl.utils.dbfilestorage.DbFileStorageItem
import tofi.api.mdl.utils.dbfilestorage.DbFileStorageService
import tofi.apinator.ApinatorApi
import tofi.apinator.ApinatorService

import java.math.RoundingMode
import java.nio.file.Files
import java.nio.file.Paths

import static java.lang.Math.*

@CompileStatic
class DataDao extends BaseMdbUtils {

    ApinatorApi apiMeta() { return app.bean(ApinatorService).getApi("meta") }

    ApinatorApi apiNSIData() { return app.bean(ApinatorService).getApi("nsidata") }

    ApinatorApi apiMonitoringData() { return app.bean(ApinatorService).getApi("monitoringdata") }
    //-----------------------------------------------------------------------------------------------//

    /**
     * fvs второго участника Typ_Reservoir <=> Typ_Fish
     * @param reservoir
     * @return set oj fvs
     */
    Set<Object> getFvs(long reservoir) {
        Store st = loadSqlMeta("""
            select c.id from Cls c, Typ t
            where c.typ=t.id and t.cod='Typ_WaterBodies'
        """, "")
        Set<Object> setCls1 = st.getUniqueValues("id")
        st = loadSqlMeta("""
            select c.id from Cls c, Typ t
            where c.typ=t.id and t.cod='Typ_Fish'
        """, "")
        Set<Object> setCls2 = st.getUniqueValues("id")
        //
        st = mdb.loadQuery("""
            select r2.cls, null as factorval
            from RelObj ro
                join relobjmember r1 on r1.relobj=ro.id and r1.cls in (${setCls1.join(",")})
                join relobjmember r2 on r2.relobj=ro.id and r2.cls in (${setCls2.join(",")})
            where r1.obj=${reservoir}
        """)
        Store stCls = loadSqlMeta("""
            select c.cls, c.factorval  
            from clsfactorval c, factor f 
            where c.factorval=f.id and f.cod <> 'FV_Fictive' and
                c.cls in (${setCls2.join(",")})
        """, "")
        StoreIndex indCls = stCls.getIndex("cls")
        for (StoreRecord r in st) {
            StoreRecord rec = indCls.get(r.getLong("cls"))
            if (rec != null) {
                r.set("factorval", rec.getLong("factorval"))
            }
        }
        return st.getUniqueValues("factorval") as Set<Object>
    }

    private Map<String, Store> loadAlgoMatrix(Map<String, Object> params) {
        VariantMap pms = new VariantMap(params)
        long own = pms.getLong("own")
        long obj2 = pms.getLong("obj2")
        long prop = pms.getLong("prop")
        boolean dependperiod = pms.getBoolean("dependperiod")
        String dte = pms.getString("dte")
        long periodType = pms.getLong("periodType")
        //
        long meter = loadSqlMeta("""
            select meter from Prop where id=${prop}
        """, "").get(0).getLong("meter")
        //
        Store stProp2Lev = loadSqlMeta("""
            with mrfv as (
            select meterrate,
                STRING_AGG (cast(factorval as varchar(200)), ',') as fvs,
                ARRAY_LENGTH(STRING_TO_ARRAY(STRING_AGG (cast(factorval as varchar(200)), ','), ','), 1) sz
            from meterratefv
            group by meterrate
            )
            select id, fvs   
            from Prop p, mrfv
            where p.meter=${meter} and p.meterrate=mrfv.meterrate and mrfv.sz=2
        """, "")
        Set<Object> idsPropsAll = stProp2Lev.getUniqueValues("id")
        StoreIndex indProp2Lev = stProp2Lev.getIndex("fvs")
        //
        Set<Object> fvsFromRelObj = getFvs(obj2)
        //
        Set<Long> setFv1 = new HashSet<>()
        Set<Long> setFv2 = new HashSet<>()
        for (StoreRecord r in stProp2Lev) {
            String[] arr = r.getString("fvs").split(",")
            if (fvsFromRelObj.contains(arr[0]))
                setFv1.add(UtCnv.toLong(arr[0]))
            setFv2.add(UtCnv.toLong(arr[1]))
        }
        //
        Store stFv1 = loadSqlMeta("""
            select id, name
            from factor
            where id in (0${setFv1.join(",")})
            order by ord
        """, "")

        List<Map<String, String>> cols = new ArrayList<>();
        cols.add(Map.of("name", "name", "label", "Возраст", "field", "name",
                "align", "left", "classes", "bg-blue-grey-1", "headerStyle", "font-size: 1.3em", "style", "width: 30%"));


        Store stFv2 = mdb.createStore()
        stFv2.addField("id", "long")
        stFv2.addField("name", "string", 20)
        //
        Store stFv2Cpy = mdb.createStore()
        stFv2Cpy.addField("ord", "int");
        stFv2Cpy.addField("id", "long")
        stFv2Cpy.addField("name", "string", 20)

        //
        List<String> sel = new ArrayList<>();
        String sep = "";
        for (StoreRecord r in stFv1) {
            for (StoreField f : r.getFields()) {
                if (f.getName().equalsIgnoreCase("id")) {
                    stFv2.addField("v" + r.getString(f.getName()), "long")
                    stFv2.addField("p" + r.getString(f.getName()), "long")
                    stFv2.addField("fv" + r.getString(f.getName()), "double")
                    sel.add("0 as v" + r.getString(f.getName()) + ", 0 as p" + r.getString(f.getName()) + ", null as fv" + r.getString(f.getName()))
                    //
                    stFv2Cpy.addField("v" + r.getString(f.getName()), "long")
                    stFv2Cpy.addField("p" + r.getString(f.getName()), "long")
                    stFv2Cpy.addField("fv" + r.getString(f.getName()), "double")
                }
            }
            sep = (!sel.isEmpty()) ? ", " : ""
            cols.add(Map.of("name", "fv" + r.getValue("id"),
                    "label", UtCnv.toString(r.getValue("name")), "field", "fv" + r.getValue("id"),
                    "align", "center", "classes", "bg-blue-grey-1", "headerStyle", "font-size: 1.2em",
                    "style", "width: 10%"))
        }

        stFv2 = loadSqlMeta("""
            select id, name ${sep}  ${String.join(",", sel)}  from factor where id in (0${setFv2.join(",")}) order by ord
        """, "")

        if (stFv2.size() == 0)
            throw new XError("Нет возраст рыбы")

        stFv2.get(0).set("id", 0)
        String name = "Количество"
        if (pms.getString("cod") == "Prop_WaterFishAverageWeight")
            name = "Вес"
        stFv2.get(0).set("name", name)
        //
        Store stProp1Lev = loadSqlMeta("""
            with mrfv as (
            select meterrate,
                STRING_AGG (cast(factorval as varchar(20)), ',') as fvs,
                ARRAY_LENGTH(STRING_TO_ARRAY(STRING_AGG (cast(factorval as varchar(20)), ','), ','), 1) sz
            from meterratefv
            group by meterrate
            )
            select id, fvs   
            from Prop p, mrfv
            where p.meter=${meter} and p.meterrate=mrfv.meterrate and mrfv.sz=1
        """, "")
        StoreIndex indProp1Lev = stProp1Lev.getIndex("fvs")
        idsPropsAll.addAll(stProp1Lev.getUniqueValues("id"))
        //Проставляем в каждую ячейку prop
        for (StoreRecord r in stFv2) {
            for (StoreField fld in r.getFields()) {
                if (fld.name.startsWith("fv")) {
                    String fvs = ""
                    if (r.getLong("id") == 0) {
                        fvs = "${fld.name.substring(2)}"
                        StoreRecord rec = indProp1Lev.get(fvs)
                        if (rec != null) {
                            r.set("p" + fld.name.substring(2), rec.getLong("id"))
                        }
                    } else {
                        fvs = "${fld.name.substring(2)},${r.getString("id")}"
                        StoreRecord rec = indProp2Lev.get(fvs)
                        if (rec != null) {
                            r.set("p" + fld.name.substring(2), rec.getLong("id"))
                        }
                    }
                }
            }
        }
        // Далее проставляем данные
        String d1 = "1800-01-01"
        String d2 = "3333-12-01"
        if (dependperiod) {
            UtPeriod up = new UtPeriod()
            d1 = up.calcDbeg(XDate.create(dte), periodType, 0).toString(XDateTimeFormatter.ISO_DATE)
            d2 = up.calcDend(XDate.create(dte), periodType, 0).toString(XDateTimeFormatter.ISO_DATE)
        }
        String sql = """
            select d.prop, v.numberval, v.id as idval
            from DataProp d, DataPropVal v
            where d.id=v.dataProp and d.isObj=1 and d.objorrelobj=${own} and d.prop in (${idsPropsAll.join(",")}) and d.periodType is null
        """
        if (dependperiod)
            sql = """
            select d.prop, v.numberval, v.id as idval
            from DataProp d, DataPropVal v
            where d.id=v.dataProp and d.isObj=1 and d.objorrelobj=${own} and d.prop in (${idsPropsAll.join(",")}) and d.periodType=${periodType}
                and v.dbeg='${d1}' and v.dend='${d2}'
        """
        Store stVal = mdb.loadQuery(sql)
        // Has data Lev1
        mdb.outTable(stVal)
        Set<Object> idsProp1Lev = stProp1Lev.getUniqueValues("id")
        boolean hasData1Lev = false
        for (StoreRecord record in stVal) {
            if (idsProp1Lev.contains(record.getLong("prop"))) {
                hasData1Lev = true
            }
        }
        //
        StoreIndex indVal = stVal.getIndex("prop")

        if (!hasData1Lev || stVal.size() == 0) {
            params.put("NoData", true)
        } else {
            params.put("NoData", false)
            for (StoreRecord r in stFv2) {
                for (StoreField fld in r.getFields()) {
                    if (fld.name.startsWith("fv")) {
                        StoreRecord rec = indVal.get(r.getLong("p" + fld.name.substring(2)))
                        if (rec != null) {
                            r.set(fld.name, rec.getDouble("numberval"))
                            r.set("v" + fld.name.substring(2), rec.getDouble("idval"))
                        }
                    }
                }
            }
            //mdb.outTable(stFv2)
        }
        Map<String, Store> rez = new HashMap<>()
        rez.put("stMatrix", stFv2)
        rez.put("stMatrixCpy", stFv2Cpy)
        //
        return rez
    }


    @DaoMethod
    void smearing2age(String reservoirs, String dbeg, String dend) {
        Set<Object> setCls = apiMeta().get(ApiMeta).setIdsOfCls("Typ_FishCatch")
        if (setCls.isEmpty()) setCls.add(0L)
        String whe = "cls in (${setCls.join(",")})"
        String wheReservoirs = "v6.obj in (${reservoirs}) and v1.dateTimeVal between '${dbeg}' and '${dend}'"

        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "", "Prop_%")
        Store st = mdb.loadQuery("""
            with ob as (
            select
                id, cls from Obj               
                where ${whe}
            )
            select ob.id as obj, ob.cls,
                v1.dateTimeVal::date as StartDate,
                v6.obj as objReservoirShore
            from ob
                join DataProp d1 on d1.isObj=1 and d1.objorrelobj=ob.id and d1.prop=:Prop_StartDate
                join DataPropVal v1 on d1.id=v1.dataprop
                left join DataProp d6 on d6.isObj=1 and d6.objorrelobj=ob.id and d6.prop=:Prop_ReservoirShore
                left join DataPropVal v6 on d6.id=v6.dataprop
            where ${wheReservoirs}
            order by v1.dateTimeVal
        """, map)
        //
        Map<String, Object> mapParamBio = new HashMap<>()
        mapParamBio.put("prop", map.get("Prop_WaterNumberFishBio"))
        mapParamBio.put("cod", "Prop_WaterNumberFishBio")
        mapParamBio.put("dependperiod", true)
        mapParamBio.put("periodType", 11)
        //
        Map<String, Object> mapParamCath = new HashMap<>()
        mapParamCath.put("dependperiod", true)
        mapParamCath.put("periodType", 71)
        mapParamCath.put("prop", map.get("Prop_NumberFishCaught"))
        mapParamCath.put("cod", "Prop_NumberFishCaught")

        int cntAll = 0
        int cntOk = 0
        int cntNo = 0
        for (StoreRecord r in st) {
            cntAll++
            long obj = r.getLong("obj")
            long reservoir = r.getLong("objReservoirShore")
            String dte = r.getString("StartDate")
            mapParamBio.put("dte", dte)
            mapParamBio.put("own", reservoir)
            mapParamBio.put("obj2", reservoir)
            Store stBio = loadAlgoNumberFishBio(mapParamBio)
            //
            println("Prop_WaterNumberFishBio")
            mdb.outTable(stBio)
            int o = 0

            if (!mapParamBio.get("NoData")) { // Данные БИО для водоема есть!
                cntOk++
                mapParamCath.put("own", obj)
                mapParamCath.put("obj2", reservoir)
                mapParamCath.put("dte", dte)
                //
                Map<String, Store> mapCatch = loadAlgoMatrix(mapParamCath)
                Store stCath = mapCatch.get("stMatrix")         //сторе с данными
                Store stCathCpy = mapCatch.get("stMatrixCpy")   //сторе структура
                //
                println("Prop_NumberFishCaught До")
                mdb.outTable(stCath)
                ////////////////////


                stCath.copyTo(stCathCpy)
                int ord = 1
                for (StoreRecord rr in stCathCpy) {
                    rr.set("ord", ord++)
                }
                Map<String, Double> mapRasn = new HashMap<>()
                mapRasn = stCath.get(0).getValues() as Map<String, Double>

                int index = 0
                for (StoreRecord rr in stCath) {
                    if (rr.getLong("id") == 0) {
                        index++
                        continue
                    }
                    for (StoreField fld in rr.getFields()) {
                        if (fld.name.startsWith("fv") && rr.getLong("p" + fld.name.substring(2)) != 0) {
                            if (stCath.get(0).getDouble(fld.name) != 0 && stBio.get(index).getDouble(fld.name) != 0) {
                                double v = stCath.get(0).getDouble(fld.name) * stBio.get(index).getDouble(fld.name)
                                rr.set(fld.name, round(v))
                                stCathCpy.get(index).set(fld.name, v)
                                //
                                double razn = UtCnv.toDouble(mapRasn.get(fld.name)) - rr.getDouble(fld.name)
                                mapRasn.put(fld.name, razn)
                            }
                        }
                    }
                    index++
                }

                // Размазывание
                for (StoreField fld in stCath.get(0).getFields()) {
                    // Нас интересуют только fv-колонки
                    if (!fld.name.startsWith("fv")) continue
                    // Проверяем, есть ли вообще итог по этой колонке
                    if (stCath.get(0).getDouble(fld.name) == 0) continue

                    int razn = abs(UtCnv.toInt(mapRasn.get(fld.name)))
                    if (razn == 0) continue

                    double eps = UtCnv.toInt(mapRasn.get(fld.name)) > 0 ? 1 as double : -1 as double

                    stCathCpy.sort("ord")
                    stCathCpy.sort("*" + fld.name)

                    int i = 1
                    for (StoreRecord rr in stCathCpy) {
                        if (rr.getLong("id") == 0) continue // пропускаем Итого
                        if (rr.getLong("p" + fld.name.substring(2)) == 0) continue

                        rr.set(fld.name, rr.getDouble(fld.name) + eps)

                        if (i == razn) {
                            break
                        }
                        i++
                    }
                }

                //
                println("stCathCpy")
                stCathCpy.sort("ord") // Возвращаем сортировку по умолчанию для вывода
                mdb.outTable(stCathCpy)
                //
                for (StoreRecord rr in stCathCpy) {
                    if (rr.getLong("id") == 0) continue
                    for (StoreField fld in rr.getFields()) {
                        if (fld.name.startsWith("fv") && rr.getLong("p" + fld.name.substring(2)) != 0) {
                            if (rr.getDouble(fld.name) != 0) {
                                double v = rr.getDouble(fld.name)
                                rr.set(fld.name, round(v))
                            }
                        }
                    }
                }

                /////////////////////

                println("Prop_NumberFishCaught После")
                mdb.outTable(stCathCpy)
                // Save DB
                Map<String, Object> params = new HashMap<>()
                params.put("dte", dte)
                params.put("dependperiod", true)
                params.put("periodType", 71L)
                params.put("obj", r.getLong("obj"))
                params.put("obj2", reservoir)

                for (StoreRecord rr in stCathCpy) {
                    if (rr.getLong("id")==0) continue
                    for (StoreField fld in rr.getFields()) {
                        if (fld.name.startsWith("fv") && rr.getLong("p" + fld.name.substring(2)) != 0) {
                            if (rr.getDouble(fld.name) != 0) {
                                params.put("prop", rr.getLong("p" + fld.name.substring(2)))
                                params.put("numberval", rr.getDouble(fld.name))
                                params.put("idval", rr.getLong("v" + fld.name.substring(2)))
                                saveMeter(params)
                                int oo=0
                            }
                        }
                    }
                }



                int e = 0

            } else {            // Нет данных БИО для водоема
                cntNo++
                println("\n\n")
                println("=========================== " + mapParamBio)
                println("\n\n")
            }

            //break

        }
        println("=========================== " + cntAll + " " + cntOk + " " + cntNo)

    }

    private Store loadAlgoNumberFishBio(Map<String, Object> params) {
        VariantMap pms = new VariantMap(params)

        Store st = loadAlgoMatrix(params).get("stMatrix")
        System.out.println("Before " + pms.getString("cod"))
        mdb.outTable(st)

        // Прибавляем +1
        for (StoreRecord r in st) {
            if (r.getLong("id") == 0) continue
            for (StoreField fld in r.getFields()) {
                if (fld.name.startsWith("fv")) {
                    if (st.get(0).getDouble(fld.name) != 0 && r.getLong("p" + fld.name.substring(2)) > 0) {
                        st.get(0).set(fld.name, st.get(0).getDouble(fld.name) + 1)
                        r.set(fld.name, r.getDouble(fld.name) + 1)
                    } else {
                        r.set(fld.name, 0)
                    }
                }
            }
        }
        //
        System.out.println("После +1")
        mdb.outTable(st)
        //
        for (StoreRecord r in st) {
            if (r.getLong("id") == 0) continue
            for (StoreField fld in r.getFields()) {
                if (fld.name.startsWith("fv")) {
                    if (st.get(0).getDouble(fld.name) != 0 && r.getLong("p" + fld.name.substring(2)) > 0) {
                        r.set(fld.name, r.getDouble(fld.name) / st.get(0).getDouble(fld.name))
                    } else {
                        r.set(fld.name, 0)
                    }
                }
            }
        }

        System.out.println("После деления")
        mdb.outTable(st)

        return st
    }

    private Store loadAlgoReservoirPdy(Map<String, Object> params) {
        //Map<String, Object> res = new HashMap<>()
        VariantMap pms = new VariantMap(params)
        long own = pms.getLong("own")
        long prop = pms.getLong("prop")
        boolean dependperiod = pms.getBoolean("dependperiod")
        String dte = pms.getString("dte")
        long periodType = pms.getLong("periodType")
        //
        long meter = loadSqlMeta("""
            select meter from Prop where id=${prop}
        """, "").get(0).getLong("meter")
        //

        Store stProp2Lev = loadSqlMeta("""
            with mrfv as (
            select meterrate,
                STRING_AGG (cast(factorval as varchar(200)), ',') as fvs,
                ARRAY_LENGTH(STRING_TO_ARRAY(STRING_AGG (cast(factorval as varchar(200)), ','), ','), 1) sz
            from meterratefv
            group by meterrate
            )
            select id, fvs   
            from Prop p, mrfv
            where p.meter=${meter} and p.meterrate=mrfv.meterrate and mrfv.sz=1
        """, "")
        Set<Object> idsPropsAll = stProp2Lev.getUniqueValues("id")
        StoreIndex indProp2Lev = stProp2Lev.getIndex("fvs")
        Set<Long> setFv1 = new HashSet<>()
        Set<Long> setFv2 = new HashSet<>()
        //
        Set<Object> fvsFromRelObj = getFvs(own)
        //
        for (StoreRecord r in stProp2Lev) {
            String[] arr = r.getString("fvs").split(",")
            if (fvsFromRelObj.contains(arr[0]))
                setFv1.add(UtCnv.toLong(arr[0]))
            //setFv2.add(UtCnv.toLong(arr[1])) // В модели от второго фактора не зависит
        }
        //
        Store stFv1 = loadSqlMeta("""
            select id, name
            from factor
            where id in (0${setFv1.join(",")})
            order by ord
        """, "")

        List<Map<String, String>> cols = new ArrayList<>();
        cols.add(Map.of("name", "name", "label", "Возраст", "field", "name",
                "align", "left", "classes", "bg-blue-grey-1", "headerStyle", "font-size: 1.3em", "style", "width: 30%"));


        Store stFv2 = mdb.createStore()
        stFv2.addField("id", "long");
        stFv2.addField("name", "string", 20);

        List<String> sel = new ArrayList<>();
        String sep = "";
        for (StoreRecord r in stFv1) {
            for (StoreField f : r.getFields()) {
                if (f.getName().equalsIgnoreCase("id")) {
                    stFv2.addField("v" + r.getString(f.getName()), "long")
                    stFv2.addField("p" + r.getString(f.getName()), "long")
                    stFv2.addField("fv" + r.getString(f.getName()), "string", 20)
                    sel.add("0 as v" + r.getString(f.getName()) + ", 0 as p" + r.getString(f.getName()) + ", null as fv" + r.getString(f.getName()))
                }
            }
            sep = (!sel.isEmpty()) ? ", " : ""
            cols.add(Map.of("name", "fv" + r.getValue("id"),
                    "label", UtCnv.toString(r.getValue("name")), "field", "fv" + r.getValue("id"),
                    "align", "center", "classes", "bg-blue-grey-1", "headerStyle", "font-size: 1.2em",
                    "style", "width: 10%"))
        }

        stFv2 = loadSqlMeta("""
            select 0 as id, 't' as name ${sep}  ${String.join(",", sel)} 
        """, "")
//select id, name ${sep}  ${String.join(",", sel)}  from factor where id in (0${setFv2.join(",")}) order by ord

//        if (stFv2.size()==0)
//            throw new XError("Нет возраст рыбы")

/*
        stFv2.get(0).set("id", 0)
        String name = "Количество"
        if (pms.getString("cod")== "Prop_WaterFishAverageWeight")
            name = "Вес"
        stFv2.get(0).set("name", name)
*/

        //
        Store stProp1Lev = loadSqlMeta("""
            with mrfv as (
            select meterrate,
                STRING_AGG (cast(factorval as varchar(20)), ',') as fvs,
                ARRAY_LENGTH(STRING_TO_ARRAY(STRING_AGG (cast(factorval as varchar(20)), ','), ','), 1) sz
            from meterratefv
            group by meterrate
            )
            select id, fvs   
            from Prop p, mrfv
            where p.meter=${meter} and p.meterrate=mrfv.meterrate and mrfv.sz=1
        """, "")
        StoreIndex indProp1Lev = stProp1Lev.getIndex("fvs")
        idsPropsAll.addAll(stProp1Lev.getUniqueValues("id"))
        //Проставляем в каждую ячейку prop
        for (StoreRecord r in stFv2) {
            for (StoreField fld in r.getFields()) {
                if (fld.name.startsWith("fv")) {
                    String fvs = ""
                    if (r.getLong("id") == 0) {
                        fvs = "${fld.name.substring(2)}"
                        StoreRecord rec = indProp1Lev.get(fvs)
                        if (rec != null) {
                            r.set("p" + fld.name.substring(2), rec.getLong("id"))
                        }
                    } else {
                        fvs = "${fld.name.substring(2)},${r.getString("id")}"
                        StoreRecord rec = indProp2Lev.get(fvs)
                        if (rec != null) {
                            r.set("p" + fld.name.substring(2), rec.getLong("id"))
                        }
                    }
                }
            }
        }
        // Далее проставляем данные
        String d1 = "1800-01-01"
        String d2 = "3333-12-01"
        if (dependperiod) {
            UtPeriod up = new UtPeriod()
            d1 = up.calcDbeg(XDate.create(dte), periodType, 0).toString(XDateTimeFormatter.ISO_DATE)
            d2 = up.calcDend(XDate.create(dte), periodType, 0).toString(XDateTimeFormatter.ISO_DATE)
        }
        String sql = """
            select d.prop, v.numberval, v.id as idval
            from DataProp d, DataPropVal v
            where d.id=v.dataProp and d.isObj=1 and d.objorrelobj=${own} and d.prop in (${idsPropsAll.join(",")}) and d.periodType is null
        """
        if (dependperiod)
            sql = """
            select d.prop, v.numberval, v.id as idval
            from DataProp d, DataPropVal v
            where d.id=v.dataProp and d.isObj=1 and d.objorrelobj=${own} and d.prop in (${idsPropsAll.join(",")}) and d.periodType=${periodType}
                and v.dbeg='${d1}' and v.dend='${d2}'
        """
        Store stVal = mdb.loadQuery(sql)
        StoreIndex indVal = stVal.getIndex("prop")

        for (StoreRecord r in stFv2) {
            for (StoreField fld in r.getFields()) {
                if (fld.name.startsWith("fv")) {
                    StoreRecord rec = indVal.get(r.getLong("p" + fld.name.substring(2)))
                    if (rec != null) {
                        r.set(fld.name, rec.getDouble("numberval"))
                        r.set("v" + fld.name.substring(2), rec.getDouble("idval"))
                    }
                }
            }
        }
        //
        for (StoreRecord r in stFv2) {
            if (r.getLong("id") == 0) continue
            for (StoreField fld in r.getFields()) {
                if (fld.name.startsWith("fv")) {
                    if (stFv2.get(0).getDouble(fld.name) != 0) {
                        r.set(fld.name, r.getDouble(fld.name) / stFv2.get(0).getDouble(fld.name))
                    }
                }
            }
        }

        //mdb.outTable(stFv2)

        return stFv2
    }

    @DaoMethod
    Map<String, Object> loadAlgo(Map<String, Object> params) {
        Map<String, Object> res = new HashMap<>()
        VariantMap pms = new VariantMap(params)
        long own = pms.getLong("own")
        long prop = pms.getLong("prop")
        String codProp = pms.getString("cod")
        boolean dependperiod = pms.getBoolean("dependperiod")
        String dte = pms.getString("dte")
        long periodType = pms.getLong("periodType")
        //
        long meter = loadSqlMeta("""
            select meter from Prop where id=${prop}
        """, "").get(0).getLong("meter")
        //
        Store stProp2Lev = loadSqlMeta("""
            with mrfv as (
            select meterrate,
                STRING_AGG (cast(factorval as varchar(200)), ',') as fvs,
                ARRAY_LENGTH(STRING_TO_ARRAY(STRING_AGG (cast(factorval as varchar(200)), ','), ','), 1) sz
            from meterratefv
            group by meterrate
            )
            select id, fvs   
            from Prop p, mrfv
            where p.meter=${meter} and p.meterrate=mrfv.meterrate and mrfv.sz=2
        """, "")
        Set<Object> idsPropsAll = stProp2Lev.getUniqueValues("id")
        StoreIndex indProp2Lev = stProp2Lev.getIndex("fvs")
        //
        Set<Object> fvsFromRelObj = getFvs(own)
        //
        Set<Long> setFv1 = new HashSet<>()
        Set<Long> setFv2 = new HashSet<>()
        for (StoreRecord r in stProp2Lev) {
            String[] arr = r.getString("fvs").split(",")
            if (fvsFromRelObj.contains(arr[0]))
                setFv1.add(UtCnv.toLong(arr[0]))
            setFv2.add(UtCnv.toLong(arr[1]))
        }
        //
        Store stFv1 = loadSqlMeta("""
            select id, name
            from factor
            where id in (0${setFv1.join(",")})
            order by ord
        """, "")

        List<Map<String, String>> cols = new ArrayList<>();
        cols.add(Map.of("name", "name", "label", "Возраст", "field", "name",
                "align", "left", "classes", "bg-blue-grey-1", "headerStyle", "font-size: 1.3em", "style", "width: 30%"));


        Store stFv2 = mdb.createStore()
        stFv2.addField("id", "long");
        stFv2.addField("name", "string", 20);

        List<String> sel = new ArrayList<>();
        String sep = "";
        for (StoreRecord r in stFv1) {
            for (StoreField f : r.getFields()) {
                if (f.getName().equalsIgnoreCase("id")) {
                    stFv2.addField("v" + r.getString(f.getName()), "long")
                    stFv2.addField("p" + r.getString(f.getName()), "long")
                    stFv2.addField("fv" + r.getString(f.getName()), "string", 20)
                    sel.add("0 as v" + r.getString(f.getName()) + ", 0 as p" + r.getString(f.getName()) + ", null as fv" + r.getString(f.getName()))
                }
            }
            sep = (!sel.isEmpty()) ? ", " : ""
            cols.add(Map.of("name", "fv" + r.getValue("id"),
                    "label", UtCnv.toString(r.getValue("name")), "field", "fv" + r.getValue("id"),
                    "align", "center", "classes", "bg-blue-grey-1", "headerStyle", "font-size: 1.2em",
                    "style", "width: 10%"))
        }

        stFv2 = loadSqlMeta("""
            select id, name ${sep}  ${String.join(",", sel)}  from factor where id in (0${setFv2.join(",")}) order by ord
        """, "")

        if (stFv2.size() == 0)
            throw new XError("Нет возраст рыбы")

        stFv2.get(0).set("id", 0)
        String name = "Количество"
        if (codProp == "Prop_WaterFishAverageWeight")
            name = "Вес"
        stFv2.get(0).set("name", name)
        //
        Store stProp1Lev = loadSqlMeta("""
            with mrfv as (
            select meterrate,
                STRING_AGG (cast(factorval as varchar(20)), ',') as fvs,
                ARRAY_LENGTH(STRING_TO_ARRAY(STRING_AGG (cast(factorval as varchar(20)), ','), ','), 1) sz
            from meterratefv
            group by meterrate
            )
            select id, fvs   
            from Prop p, mrfv
            where p.meter=${meter} and p.meterrate=mrfv.meterrate and mrfv.sz=1
        """, "")
        StoreIndex indProp1Lev = stProp1Lev.getIndex("fvs")
        idsPropsAll.addAll(stProp1Lev.getUniqueValues("id"))
        //Проставляем в каждую ячейку prop
        for (StoreRecord r in stFv2) {
            for (StoreField fld in r.getFields()) {
                if (fld.name.startsWith("fv")) {
                    String fvs = ""
                    if (r.getLong("id") == 0) {
                        fvs = "${fld.name.substring(2)}"
                        StoreRecord rec = indProp1Lev.get(fvs)
                        if (rec != null) {
                            r.set("p" + fld.name.substring(2), rec.getLong("id"))
                        }
                    } else {
                        fvs = "${fld.name.substring(2)},${r.getString("id")}"
                        StoreRecord rec = indProp2Lev.get(fvs)
                        if (rec != null) {
                            r.set("p" + fld.name.substring(2), rec.getLong("id"))
                        }
                    }
                }
            }
        }
        // Далее проставляем данные
        String d1 = "1800-01-01"
        String d2 = "3333-12-01"
        if (dependperiod) {
            UtPeriod up = new UtPeriod()
            d1 = up.calcDbeg(XDate.create(dte), periodType, 0).toString(XDateTimeFormatter.ISO_DATE)
            d2 = up.calcDend(XDate.create(dte), periodType, 0).toString(XDateTimeFormatter.ISO_DATE)
        }
        String sql = """
            select d.prop, v.numberval, v.id as idval
            from DataProp d, DataPropVal v
            where d.id=v.dataProp and d.isObj=1 and d.objorrelobj=${own} and d.prop in (${idsPropsAll.join(",")}) and d.periodType is null
        """
        if (dependperiod)
            sql = """
            select d.prop, v.numberval, v.id as idval
            from DataProp d, DataPropVal v
            where d.id=v.dataProp and d.isObj=1 and d.objorrelobj=${own} and d.prop in (${idsPropsAll.join(",")}) and d.periodType=${periodType}
                and v.dbeg='${d1}' and v.dend='${d2}'
        """
        Store stVal = mdb.loadQuery(sql)
        StoreIndex indVal = stVal.getIndex("prop")

        for (StoreRecord r in stFv2) {
            for (StoreField fld in r.getFields()) {
                if (fld.name.startsWith("fv")) {
                    StoreRecord rec = indVal.get(r.getLong("p" + fld.name.substring(2)))
                    if (rec != null) {
                        r.set(fld.name, rec.getDouble("numberval"))
                        r.set("v" + fld.name.substring(2), rec.getDouble("idval"))
                    }
                }
            }
        }

        res.put("cols", cols)
        res.put("store", stFv2)

        System.out.println("prop = " + codProp + " - " + prop)
        mdb.outTable(stFv2)
        //1. Prop_NumberFishCaught
        if (pms.getString("cod") == "Prop_NumberFishCaught") {      //Количество пойманных рыб
            Store stProp = apiMeta().get(ApiMeta).loadSql("""
                select id from Prop where cod='Prop_WaterNumberFishBio'
            """, "")
            pms.put("cod", "Prop_WaterNumberFishBio")
            pms.put("prop", stProp.get(0).getLong("id"))
            Store stBio = loadAlgoNumberFishBio(pms)
            //
            //System.out.println("Prop_WaterNumberFishBio")
            //mdb.outTable(stBio)
            //
            int index = 0
            for (StoreRecord r in stFv2) {
                if (r.getLong("id") == 0) {
                    index++
                    continue
                }
                for (StoreField fld in r.getFields()) {
                    if (fld.name.startsWith("fv") && r.getLong("p" + fld.name.substring(2)) != 0) {
                        if (stFv2.get(0).getDouble(fld.name) != 0 && stBio.get(index).getDouble(fld.name) != 0) {
                            //r.set(fld.name, round(stFv2.get(0).getDouble(fld.name) * stBio.get(index).getDouble(fld.name)+0.5 as Double))
                            double v = round(stFv2.get(0).getDouble(fld.name) * stBio.get(index).getDouble(fld.name))
                            r.set(fld.name, v)
                        }
                    }
                }
                index++
            }
        } else if (pms.getString("cod") == "Prop_CalcPdy") {    //Предельно допустимый улов, экземпляр
            Store stProp = apiMeta().get(ApiMeta).loadSql("""
                select id from Prop where cod='Prop_ReservoirPdy'
            """, "")
            pms.put("cod", "Prop_ReservoirPdy")
            pms.put("prop", stProp.get(0).getLong("id"))
            Store stPdy = loadAlgoReservoirPdy(pms)
            //
            System.out.println("Prop_ReservoirPdy")
            mdb.outTable(stPdy)
            //....
        } else if (pms.getString("cod") == "Prop_GearCatchabilityNet") {    //Коэффициент уловистости сети
            //
            System.out.println("\n\n\n\n")
            StoreRecord r = stFv2.get(0)
            Map<String, Double> map_CalcAgeSex = new HashMap<>()
            Map<String, Long> mapProp = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "", "Prop_Calc%")
            for (StoreField fld in r.getFields()) {
                if (fld.name.startsWith("fv") && r.getLong("p" + fld.name.substring(2)) != 0) {
                    Store stCls = loadSqlMeta("""
                        select cls from clsfactorval c 
                        where factorval=${fld.name.substring(2)}
                    """, "")
                    long objFish = mdb.loadQuery("""
                        select id from Obj
                        where cls=${stCls.get(0).getLong("cls")}
                    """).get(0).getLong("id")
                    //Prop_CalcAgeSex       Возраст половой зрелости рыбы
                    Store stCalcAgeSex = mdb.loadQuery("""
                        select v.numberval
                        from Obj o
                            left join DataProp d on d.isObj=1 and d.objorrelobj=${objFish} and d.prop=${mapProp.get("Prop_CalcAgeSex")}
                            left join DataPropVal v on d.id=v.dataProp
                        where o.id=${objFish}
                    """)
                    map_CalcAgeSex.put(fld.name, stCalcAgeSex.get(0).getDouble("numberval"))
                }
            }

            System.out.println("map_CalcAgeSex")    //Возраст половой зрелости рыбы
            mdb.outMap(map_CalcAgeSex)

            // Peac year Prop_NumberFishCaught
            Store stProp = apiMeta().get(ApiMeta).loadSql("""
                    select id from Prop where cod='Prop_NumberFishCaught'
                """, "")
            pms.put("cod", "Prop_NumberFishCaught")
            pms.put("prop", stProp.get(0).getLong("id"))
            pms.put("dependperiod", true)
            Store stFishCaught = loadAlgoMatrix(pms).get("stMatrix")
            //
            System.out.println("Prop_NumberFishCaught")
            mdb.outTable(stFishCaught)
            //
            Map<String, Double> mapPeakCatch = new HashMap<>()
            Map<String, Double> mapPeakCatchAge = new HashMap<>()
            //
            System.out.println("mapPeakCatch 0")
            mdb.outMap(mapPeakCatch)    //Улов по возрастам
            //
            for (StoreRecord rr in stFishCaught) {
                if (rr.getLong("id") == 0) continue
                for (StoreField fld in rr.getFields()) {
                    if (fld.name.startsWith("fv") && rr.getLong("p" + fld.name.substring(2)) != 0) {
                        if (rr.getLong(fld.name) > mapPeakCatch.get(fld.name)) {
                            mapPeakCatch.put(fld.name, rr.getDouble(fld.name))
                            double age = UtCnv.toDouble(rr.getString("name").split(" ")[0])
                            mapPeakCatchAge.put(fld.name, age)
                        }
                    }
                }
            }
            //
            System.out.println("mapPeakCatch; mapPeakCatchAge")
            mdb.outMap(mapPeakCatch)
            mdb.outMap(mapPeakCatchAge)

            //Берем max(mapPeakCatchAge, map_CalcAgeSex)

            for (def key in mapPeakCatchAge.keySet()) {
                def v = max(UtCnv.toDouble(mapPeakCatchAge.get(key)), UtCnv.toDouble(map_CalcAgeSex.get(key)))
                mapPeakCatchAge.put(key, v)
            }
            System.out.println("mapPeakCatch Max")
            mdb.outMap(mapPeakCatchAge)
            // Находим fishObj from fv: mapPeakCatchAge.keySet()
            Set<Object> setFv = new HashSet<>()
            mapPeakCatchAge.keySet().forEach { String it ->
                setFv.add(UtCnv.toLong(it.substring(2)))
            }
            Store stCls = loadSqlMeta("""
                select cls, factorval from clsfactorval 
                where factorval in (0${setFv.join(",")})
            """, "")
            StoreIndex indCls = stCls.getIndex("cls")
            mapProp = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "Prop_FishMaxAge", "")
            Store stFishObjData = mdb.loadQuery("""
                select cls, v.numberval 
                from Obj o
                    left join DataProp d on d.isObj=1 and d.objorrelobj=o.id and d.prop=${mapProp.get("Prop_FishMaxAge")} and d.periodType is null
                    left join DataPropVal v on d.id=v.dataProp
                where o.cls in (${stCls.getUniqueValues("cls").join(",")}) 
            """)
            //Максимальный возраст рыбы, лет
            Map<String, Double> mapMaxAgeFish = new HashMap<>()
            for (StoreRecord rr in stFishObjData) {
                StoreRecord rec = indCls.get(rr.getLong("cls"))
                if (rec != null) {
                    mapMaxAgeFish.put("fv" + rec.getString("factorval"), rr.getDouble("numberval"))
                }
            }
            //
            System.out.println("mapMaxAgeFish Максимальный возраст рыбы")
            mdb.outMap(mapMaxAgeFish)
            //Границы
            Map<String, Double> mapDistLeft = new HashMap<>()
            Map<String, Double> mapDistRight = new HashMap<>()
            /*
                 dist_left  = max(пик − 2,  0.5)
                 dist_right = max(m1 − пик, 0.5)
             */
            //КРУТИЗНА СКЛОНОВ
            Map<String, Double> k_up = new HashMap<>()
            Map<String, Double> k_down = new HashMap<>()
            /*
                k_up   = L / dist_left
                k_down = L / dist_right               # k_down < k_up ⇒ склон положе
             */
            double L = log(9.0 as double)

            for (StoreField fld in stFishCaught.get(0).getFields()) {
                if (fld.name.startsWith("fv") && stFishCaught.get(0).getLong(fld.name) != 0
                        && stFishCaught.get(0).getLong("p" + fld.name.substring(2)) != 0) {
                    try {
                        double v1 = mapPeakCatchAge.get(fld.name) - 2.0
                        double v2 = 0.5
                        double d_left = max(v1, v2)
                        mapDistLeft.put(fld.name, d_left)
                        //
                        v1 = mapMaxAgeFish.get(fld.name) - mapPeakCatchAge.get(fld.name)
                        double d_right = max(v1, v2)
                        mapDistRight.put(fld.name, d_right)
                        k_up.put(fld.name, L / mapDistLeft.get(fld.name))
                        k_down.put(fld.name, L / mapDistRight.get(fld.name))
                    } catch (e) {
                        e.printStackTrace()
                    }
                }
            }
            System.out.println("mapDistLeft, mapDistRight Границы")
            mdb.outMap(mapDistLeft)
            mdb.outMap(mapDistRight)
            System.out.println("k_up, k_down  КРУТИЗНА СКЛОНОВ")
            mdb.outMap(k_up)
            mdb.outMap(k_down)
            //
            /*
              АСИММЕТРИЧНЫЙ КОЛОКОЛ (для каждого age)
                 sel_up   = 1 / (1 + exp(−k_up   · (age − пик)))
                 sel_down = 1 / (1 + exp( k_down · (age − пик)))
            * */
            for (StoreRecord rr in stFv2) {
                if (rr.getLong("id") == 0) continue
                Map<String, Double> sel_up = new HashMap<>()
                Map<String, Double> sel_down = new HashMap<>()
                double age = UtCnv.toDouble(rr.getString("name").split(" ")[0])
                for (StoreField fld in rr.getFields()) {
                    if (fld.name.startsWith("fv") && rr.getLong("p" + fld.name.substring(2)) != 0) {
                        sel_up.put(fld.name, 1 / (1 + exp(-k_up.get(fld.name) * (age - mapPeakCatchAge.get(fld.name)))))
                        sel_down.put(fld.name, 1 / (1 + exp(k_down.get(fld.name) * (age - mapPeakCatchAge.get(fld.name)))))
                        //
                        double bell = sel_up.get(fld.name) * sel_down.get(fld.name)
                        //if (age > mapMaxAgeFish.get(fld.name)) bell = 0 as Double
                        bell = new BigDecimal(bell).setScale(3, RoundingMode.HALF_EVEN).doubleValue()
                        rr.set(fld.name, bell)
                    }
                }
            }
            //
            System.out.println("stFv2 =bell=")
            mdb.outTable(stFv2)
            //
            Map<String, Double> max_beel = new HashMap<>()
            Map<String, Double> mean_beel = new HashMap<>()
            //
            Map<String, List<Double>> lst_mean_beel = new HashMap<>()
            //Выделяем памяти для списка
            for (StoreField fld in stFv2.get(0).getFields()) {
                if (fld.name.startsWith("fv") && stFv2.get(0).getLong("p" + fld.name.substring(2)) != 0) {
                    lst_mean_beel.put(fld.name, new ArrayList<>())
                }
            }
            //
            for (StoreRecord rr in stFv2) {
                if (rr.getLong("id") == 0) continue
                for (StoreField fld in rr.getFields()) {
                    if (fld.name.startsWith("fv") && rr.getLong("p" + fld.name.substring(2)) != 0) {
                        if (rr.getDouble(fld.name) > max_beel.get(fld.name)) {
                            max_beel.put(fld.name, new BigDecimal(rr.getDouble(fld.name)).setScale(3, RoundingMode.HALF_EVEN).doubleValue())
                        }
                        lst_mean_beel.get(fld.name).add(rr.getDouble(fld.name))
                    }
                }
            }

            for (String key in lst_mean_beel.keySet()) {
                List<Double> lst = lst_mean_beel.get(key)
                double s = 0
                lst.forEach {
                    s += it
                }
                double d = (s / lst.size()) as double
                d = new BigDecimal(d).setScale(3, RoundingMode.HALF_EVEN).doubleValue()
                mean_beel.put(key, d)
            }

            System.out.println("max_beel, mean_beel")
            mdb.outMap(max_beel)
            mdb.outMap(mean_beel)

            //
            //scale = min( k_эксперт / mean_bell ,  0.85 / max_bell )
            //result(age) = bell(age) × scale
            for (StoreRecord rr in stFv2) {
                if (rr.getLong("id") == 0) continue
                for (StoreField fld in rr.getFields()) {
                    if (fld.name.startsWith("fv") && rr.getLong("p" + fld.name.substring(2)) != 0) {
                        double k_exp = stFv2.get(0).getDouble(fld.name)
                        double scale = min(k_exp / mean_beel.get(fld.name) as Double, 0.85 / max_beel.get(fld.name) as Double)
                        double v = rr.getDouble(fld.name) * scale
                        v = new BigDecimal(v).setScale(3, RoundingMode.HALF_EVEN).doubleValue()
                        rr.set(fld.name, v)
                    }
                }
            }
        }


        mdb.outTable(stFv2)

        return res
    }


    @DaoMethod
    Map<String, Object> loadAlgoFishing(Map<String, Object> params) {
        Map<String, Object> res = new HashMap<>()
        VariantMap pms = new VariantMap(params)
        long own = pms.getLong("own")
        long reservoir = pms.getLong("reservoir")
        long prop = pms.getLong("prop")
        String codProp = pms.getString("cod")
        boolean dependperiod = pms.getBoolean("dependperiod")
        String dte = pms.getString("dte")
        long periodType = pms.getLong("periodType")
        //
        long meter = loadSqlMeta("""
            select meter from Prop where id=${prop}
        """, "").get(0).getLong("meter")
        //
        Store stProp2Lev = loadSqlMeta("""
            with mrfv as (
            select meterrate,
                STRING_AGG (cast(factorval as varchar(200)), ',') as fvs,
                ARRAY_LENGTH(STRING_TO_ARRAY(STRING_AGG (cast(factorval as varchar(200)), ','), ','), 1) sz
            from meterratefv
            group by meterrate
            )
            select id, fvs   
            from Prop p, mrfv
            where p.meter=${meter} and p.meterrate=mrfv.meterrate and mrfv.sz=2
        """, "")
        Set<Object> idsPropsAll = stProp2Lev.getUniqueValues("id")
        StoreIndex indProp2Lev = stProp2Lev.getIndex("fvs")
        //
        Set<Object> fvsFromRelObj = getFvs(reservoir)
        //
        Set<Long> setFv1 = new HashSet<>()
        Set<Long> setFv2 = new HashSet<>()
        for (StoreRecord r in stProp2Lev) {
            String[] arr = r.getString("fvs").split(",")
            if (fvsFromRelObj.contains(arr[0]))
                setFv1.add(UtCnv.toLong(arr[0]))
            setFv2.add(UtCnv.toLong(arr[1]))
        }
        //
        Store stFv1 = loadSqlMeta("""
            select id, name
            from factor
            where id in (0${setFv1.join(",")})
            order by ord
        """, "")

        List<Map<String, String>> cols = new ArrayList<>();
        cols.add(Map.of("name", "name", "label", "Возраст", "field", "name",
                "align", "left", "classes", "bg-blue-grey-1", "headerStyle", "font-size: 1.3em", "style", "width: 30%"));


        Store stFv2 = mdb.createStore()
        stFv2.addField("id", "long");
        stFv2.addField("name", "string", 20);

        List<String> sel = new ArrayList<>();
        String sep = "";
        for (StoreRecord r in stFv1) {
            for (StoreField f : r.getFields()) {
                if (f.getName().equalsIgnoreCase("id")) {
                    stFv2.addField("v" + r.getString(f.getName()), "long")
                    stFv2.addField("p" + r.getString(f.getName()), "long")
                    stFv2.addField("fv" + r.getString(f.getName()), "double")
                    sel.add("0 as v" + r.getString(f.getName()) + ", 0 as p" + r.getString(f.getName()) + ", null as fv" + r.getString(f.getName()))
                }
            }
            sep = (!sel.isEmpty()) ? ", " : ""
            cols.add(Map.of("name", "fv" + r.getValue("id"),
                    "label", UtCnv.toString(r.getValue("name")), "field", "fv" + r.getValue("id"),
                    "align", "center", "classes", "bg-blue-grey-1", "headerStyle", "font-size: 1.2em",
                    "style", "width: 10%"))
        }

        stFv2 = loadSqlMeta("""
            select id, name ${sep}  ${String.join(",", sel)}  from factor where id in (0${setFv2.join(",")}) order by ord
        """, "")

        if (stFv2.size() == 0)
            throw new XError("Нет возраст рыбы")

        stFv2.get(0).set("id", 0)
        String name = "Количество"
        if (codProp == "Prop_WaterFishAverageWeight")
            name = "Вес"
        stFv2.get(0).set("name", name)
        //
        Store stProp1Lev = loadSqlMeta("""
            with mrfv as (
            select meterrate,
                STRING_AGG (cast(factorval as varchar(20)), ',') as fvs,
                ARRAY_LENGTH(STRING_TO_ARRAY(STRING_AGG (cast(factorval as varchar(20)), ','), ','), 1) sz
            from meterratefv
            group by meterrate
            )
            select id, fvs   
            from Prop p, mrfv
            where p.meter=${meter} and p.meterrate=mrfv.meterrate and mrfv.sz=1
        """, "")
        StoreIndex indProp1Lev = stProp1Lev.getIndex("fvs")
        idsPropsAll.addAll(stProp1Lev.getUniqueValues("id"))
        //Проставляем в каждую ячейку prop
        for (StoreRecord r in stFv2) {
            for (StoreField fld in r.getFields()) {
                if (fld.name.startsWith("fv")) {
                    String fvs = ""
                    if (r.getLong("id") == 0) {
                        fvs = "${fld.name.substring(2)}"
                        StoreRecord rec = indProp1Lev.get(fvs)
                        if (rec != null) {
                            r.set("p" + fld.name.substring(2), rec.getLong("id"))
                        }
                    } else {
                        fvs = "${fld.name.substring(2)},${r.getString("id")}"
                        StoreRecord rec = indProp2Lev.get(fvs)
                        if (rec != null) {
                            r.set("p" + fld.name.substring(2), rec.getLong("id"))
                        }
                    }
                }
            }
        }
        // Далее проставляем данные
        String d1 = "1800-01-01"
        String d2 = "3333-12-01"
        if (dependperiod) {
            UtPeriod up = new UtPeriod()
            d1 = up.calcDbeg(XDate.create(dte), periodType, 0).toString(XDateTimeFormatter.ISO_DATE)
            d2 = up.calcDend(XDate.create(dte), periodType, 0).toString(XDateTimeFormatter.ISO_DATE)
        }
        String sql = """
            select d.prop, v.numberval, v.id as idval
            from DataProp d, DataPropVal v
            where d.id=v.dataProp and d.isObj=1 and d.objorrelobj=${own} and d.prop in (${idsPropsAll.join(",")}) and d.periodType is null
        """
        if (dependperiod)
            sql = """
            select d.prop, v.numberval, v.id as idval
            from DataProp d, DataPropVal v
            where d.id=v.dataProp and d.isObj=1 and d.objorrelobj=${own} and d.prop in (${idsPropsAll.join(",")}) and d.periodType=${periodType}
                and v.dbeg='${d1}' and v.dend='${d2}'
        """
        Store stVal = mdb.loadQuery(sql)
        StoreIndex indVal = stVal.getIndex("prop")

        for (StoreRecord r in stFv2) {
            for (StoreField fld in r.getFields()) {
                if (fld.name.startsWith("fv")) {
                    StoreRecord rec = indVal.get(r.getLong("p" + fld.name.substring(2)))
                    if (rec != null) {
                        r.set(fld.name, rec.getDouble("numberval"))
                        r.set("v" + fld.name.substring(2), rec.getDouble("idval"))
                    }
                }
            }
        }

        res.put("cols", cols)

        println("До")
        res.put("store", stFv2)

        ////*******************************************************************

        Store stProp = apiMeta().get(ApiMeta).loadSql("""
            select id from Prop where cod='Prop_WaterNumberFishBio'
        """, "")
        pms.put("cod", "Prop_WaterNumberFishBio")
        pms.put("own", reservoir)
        pms.put("obj2", reservoir)
        pms.put("periodType", 11)

        pms.put("prop", stProp.get(0).getLong("id"))
        Store stBio = loadAlgoNumberFishBio(pms)
        //
        System.out.println("prop = Prop_WaterNumberFishBio" + " - " + stProp.get(0).getLong("id"))
        mdb.outTable(stBio)
/////////////////

        Store stFv2Cpy = mdb.createStore()

        stFv2Cpy.addField("ord", "int");
        stFv2Cpy.addField("id", "long");
        stFv2Cpy.addField("name", "string", 20);

        for (StoreRecord r in stFv1) {
            for (StoreField f : r.getFields()) {
                if (f.getName().equalsIgnoreCase("id")) {
                    stFv2Cpy.addField("v" + r.getString(f.getName()), "long")
                    stFv2Cpy.addField("p" + r.getString(f.getName()), "long")
                    stFv2Cpy.addField("fv" + r.getString(f.getName()), "double")
                }
            }
        }

        stFv2.copyTo(stFv2Cpy)
        stFv2Cpy.get(0).set("name", "1 Kol")
        int ord = 1
        for (StoreRecord r in stFv2Cpy) {
            r.set("ord", ord++)
        }
        Map<String, Double> mapRasn = new HashMap<>()
        mapRasn = stFv2.get(0).getValues() as Map<String, Double>

        println("mapRasn 0")
        mdb.outMap(mapRasn)
        int index = 0
        for (StoreRecord r in stFv2) {
            if (r.getLong("id") == 0) {
                index++
                continue
            }
            for (StoreField fld in r.getFields()) {
                if (fld.name.startsWith("fv") && r.getLong("p" + fld.name.substring(2)) != 0) {
                    if (stFv2.get(0).getDouble(fld.name) != 0 && stBio.get(index).getDouble(fld.name) != 0) {
                        double v = stFv2.get(0).getDouble(fld.name) * stBio.get(index).getDouble(fld.name)
                        r.set(fld.name, round(v))
                        stFv2Cpy.get(index).set(fld.name, v)
                        //
                        double razn = UtCnv.toDouble(mapRasn.get(fld.name)) - r.getDouble(fld.name)
                        mapRasn.put(fld.name, razn)
                    }
                }
            }
            index++
        }

        //
/*
        println("Do")
        mdb.outMap(mapRasn)
        mdb.outTable(stFv2)
        mdb.outTable(stFv2Cpy)
        //

        for (StoreRecord r in stFv2) {
            if (r.getLong("id") == 0) continue
            for (StoreField fld in r.getFields()) {
                if (fld.name.startsWith("fv") && r.getLong("p" + fld.name.substring(2)) != 0) {
                    if (stFv2.get(0).getDouble(fld.name) != 0) {
                        int razn = abs(UtCnv.toInt(mapRasn.get(fld.name)))
                        if (razn == 0) continue
                        double eps = UtCnv.toInt(mapRasn.get(fld.name)) > 0 ? 1 as double : -1 as double
                        stFv2Cpy.sort("ord")
                        stFv2Cpy.sort("*" + fld.name)
                        int i = 1
                        for (StoreRecord rr in stFv2Cpy) {
                            if (rr.getLong("id") == 0) continue
                            rr.set(fld.name, rr.getDouble(fld.name) + eps)
                            if (i == razn) {
                                break
                            }
                            i++
                        }
                    }
                }
            }
        }

        //
        println("После")
        stFv2Cpy.sort("ord")
        mdb.outTable(stFv2Cpy)
*/


        println("Do")
        mdb.outMap(mapRasn)
        mdb.outTable(stFv2)
        mdb.outTable(stFv2Cpy)
//

        //Размазывание

        for (StoreField fld in stFv2.get(0).getFields()) {

            // Нас интересуют только fv-колонки
            if (!fld.name.startsWith("fv")) continue

            // Проверяем, есть ли вообще итог по этой колонке
            if (stFv2.get(0).getDouble(fld.name) == 0) continue

            int razn = abs(UtCnv.toInt(mapRasn.get(fld.name)))
            if (razn == 0) continue

            double eps = UtCnv.toInt(mapRasn.get(fld.name)) > 0 ? 1 as double : -1 as double

            stFv2Cpy.sort("ord")
            stFv2Cpy.sort("*" + fld.name)

            int i = 1
            for (StoreRecord rr in stFv2Cpy) {
                if (rr.getLong("id") == 0) continue // пропускаем Итого
                if (rr.getLong("p" + fld.name.substring(2)) == 0) continue

                rr.set(fld.name, rr.getDouble(fld.name) + eps)

                if (i == razn) {
                    break
                }
                i++
            }
        }

//
        println("После")
        stFv2Cpy.sort("ord") // Возвращаем сортировку по умолчанию для вывода
        mdb.outTable(stFv2Cpy)

        for (StoreRecord r in stFv2Cpy) {
            if (r.getLong("id") == 0) continue
            for (StoreField fld in r.getFields()) {
                if (fld.name.startsWith("fv") && r.getLong("p" + fld.name.substring(2)) != 0) {
                    if (r.getDouble(fld.name) != 0) {
                        double v = r.getDouble(fld.name)
                        r.set(fld.name, round(v))
                    }
                }
            }
        }

        /////
        res.put("store", stFv2Cpy)

        return res
    }


    @DaoMethod
    long saveAlgo(Map<String, Object> rec) {
        rec.put("dependperiod", UtCnv.toInt(rec.get("dependperiod")))
        return saveMeter(rec)
    }

    @DaoMethod
    void saveAlgoMatrix(List<List<Map<String, Object>>> lstlst) {
        for (List<Map<String, Object>> lst in lstlst) {
            for (Map<String, Object> map in lst) {
                map.put("dependperiod", UtCnv.toInt(map.get("dependperiod")))
                saveMeter(map)
            }
        }
    }


    @DaoMethod
    void saveAlgo1Lev(List<Map<String, Object>> lst) {
        for (Map<String, Object> map in lst) {
            map.put("dependperiod", UtCnv.toInt(map.get("dependperiod")))
            saveMeter(map)
        }
    }

    @DaoMethod
    void deleteAlgo(long idVal) {
        mdb.execQueryNative("""
            delete from DataPropVal
            where id=${idVal};
            delete from DataProp where id in (
                select id from dataprop
                except
                select dataProp as id from DataPropVal
            );
        """)
    }

    //---------------- Reservors---------------- //
    @DaoMethod
    Store loadReservoirsFilial(Map<String, Object> params) {
        //
        long filial = UtCnv.toLong(params.get("filial"))
        fromDay2YearForNumberFishCaughtFilial(filial)
        //

        String codTyp = UtCnv.toString(params.get("codTyp"))
        long idObj = UtCnv.toLong(params.get("idObj"))
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "", "Prop_%")
        map.put("filial", filial)
        String whe = "o.id=${idObj}"
        if (idObj == 0) {
            Set<Object> ids = apiMeta().get(ApiMeta).idsChildClses(codTyp)
            whe = "o.cls in (0${ids.join(",")})"
        }
        Store st = mdb.createStore("Obj.reservoirs")
        mdb.loadQuery(st, """
            with ob as (
            select
                o.id, o.cls , v.name
                from Obj o
                    left join ObjVer v on o.id=v.ownerVer and v.lastVer=1                
                where ${whe}
            )
            select
                ob.id as obj, ob.cls, ob.name,
                t2.lstKATO,
                v3.id as idReservoirType, v3.propval as pvReservoirType, null as fvReservoirType,
                v4.id as idReservoirStatus, v4.propval as pvReservoirStatus, null as fvReservoirStatus,
                v5.id as idFishFarmingType, v5.propval as pvFishFarmingType, null as fvFishFarmingType,
                v6.id as idCoordinate, v6.strVal as Coordinate,
                v7.id as idDescription, v7.multiStrVal as Description,
                v8.id as idBranch, v8.obj as objBranch, v8.propval as pvBranch
            from ob                
                left join (
                    select d2.objorrelobj, d2.prop,
                    STRING_AGG (cast(v2.id||'_'||v2.obj||'_'||v2.propval as varchar(2000)), ',' order by v2.id) as lstKATO
                    from ob
                        left join DataProp d2  on d2.isobj=1 and d2.objorrelobj=ob.id and d2.prop=:Prop_KATO
                        left join DataPropVal v2 on d2.id=v2.dataprop
                    where 0=0
                    group by d2.objorrelobj, d2.prop
                    ) t2 on t2.objorrelobj=ob.id and t2.prop=:Prop_KATO
                left join DataProp d3 on d3.isobj=1 and d3.objorrelobj =ob.id and d3.prop=:Prop_ReservoirType
                left join DataPropVal v3 on d3.id=v3.dataprop
                left join DataProp d4 on d4.isobj=1 and d4.objorrelobj=ob.id and d4.prop=:Prop_ReservoirStatus
                left join DataPropVal v4 on d4.id=v4.dataprop
                left join DataProp d5 on d5.isobj=1 and d5.objorrelobj=ob.id and d5.prop=:Prop_FishFarmingType
                left join DataPropVal v5 on d5.id=v5.dataprop
                left join DataProp d6 on d6.isobj=1 and d6.objorrelobj=ob.id and d6.prop=:Prop_Coordinate
                left join DataPropVal v6 on d6.id=v6.dataprop
                left join DataProp d7 on d7.isobj=1 and d7.objorrelobj=ob.id and d7.prop=:Prop_Description
                left join DataPropVal v7 on d7.id=v7.dataprop
                join DataProp d8 on d8.isobj=1 and d8.objorrelobj=ob.id and d8.prop=:Prop_Branch
                join DataPropVal v8 on d8.id=v8.dataprop and v8.obj=:filial
        """, map)
        //mdb.outTable(st)

        Store stFV = apiMeta().get(ApiMeta).storeFVfromPropVal()
        StoreIndex indFV = stFV.getIndex("propval")

        for (StoreRecord r in st) {
            List<String> objKATO = new ArrayList<>()
            String lstKATO = r.getString("lstKATO")
            String[] arr0 = lstKATO.split(",")
            List<Object> idsObj = new ArrayList<>()
            for (String it in arr0) {
                String[] arr1 = it.split("_")
                objKATO.add(arr1[1] + "_" + arr1[2])
                idsObj.add(arr1[1])
            }
            r.set("objKATO", objKATO.join(","))
            Store stObj = loadSqlService("""
                select v.name from Obj o, ObjVer v where o.id=v.ownerVer and v.lastVer=1 and o.id in (${idsObj.join(",")})
            """, "", "nsidata")
            r.set("nameKATO", stObj.getUniqueValues("name").join("; "))
            //
            StoreRecord rec = indFV.get(r.getLong("pvReservoirType"))
            if (rec != null)
                r.set("fvReservoirType", rec.getLong("factorval"))
            rec = indFV.get(r.getLong("pvReservoirStatus"))
            if (rec != null)
                r.set("fvReservoirStatus", rec.getLong("factorval"))
            rec = indFV.get(r.getLong("pvFishFarmingType"))
            if (rec != null)
                r.set("fvFishFarmingType", rec.getLong("factorval"))
        }
        return st

    }

    @DaoMethod
    Map<String, Object> getBranchInfo(long filial) {
        Store st = apiNSIData().get(ApiNSIData).loadSql("""
            select o.cls, v.name, null as pv
            from Obj o, ObjVer v
            where o.id=v.ownerVer and v.lastVer=1 and o.id=${filial}
        """, "")
        if (st.size() == 0)
            throw new XError("Филиал не найден")

        Map<String, Long> mapProp = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "Prop_Branch", "")
        Store stProp = apiMeta().get(ApiMeta).loadSql("""
                select id as propval
                from PropVal
                where prop=${mapProp.get("Prop_Branch")} and cls=${st.get(0).getLong("cls")}
            """, "")
        if (stProp.size() == 0)
            throw new XError("Не найден возможное значения [Prop_Branch]")

        Map<String, Object> rez = new HashMap<>()
        rez.put("name", st.get(0).getString("name"))
        rez.put("pv", stProp.get(0).getLong("propval"))
        //
        return rez
    }

    @DaoMethod
    Store loadReservoirsPage(Map<String, Object> params) {

        String codTyp = UtCnv.toString(params.get("codTyp"))
        long idObj = UtCnv.toLong(params.get("idObj"))
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "", "Prop_%")
        String whe = "o.id=${idObj}"
        if (idObj == 0) {
            Set<Object> ids = apiMeta().get(ApiMeta).idsChildClses(codTyp)
            whe = "o.cls in (0${ids.join(",")})"
        }
        Store st = mdb.createStore("Obj.reservoirs")
        mdb.loadQuery(st, """
            with ob as (
            select
                o.id, o.cls , v.name
                from Obj o
                    left join ObjVer v on o.id=v.ownerVer and v.lastVer=1                
                where ${whe}
            )
            select
                ob.id as obj, ob.cls, ob.name,
                null as lstBranch, t2.lstKATO,
                v3.id as idReservoirType, v3.propval as pvReservoirType, null as fvReservoirType,
                v4.id as idReservoirStatus, v4.propval as pvReservoirStatus, null as fvReservoirStatus,
                v5.id as idFishFarmingType, v5.propval as pvFishFarmingType, null as fvFishFarmingType,
                v6.id as idCoordinate, v6.strVal as Coordinate,
                v7.id as idDescription, v7.multiStrVal as Description
            from ob                
                left join (
                    select d2.objorrelobj, d2.prop,
                    STRING_AGG (cast(v2.id||'_'||v2.obj||'_'||v2.propval as varchar(2000)), ',' order by v2.id) as lstKATO
                    from ob
                        left join DataProp d2  on d2.isobj=1 and d2.objorrelobj=ob.id and d2.prop=:Prop_KATO
                        left join DataPropVal v2 on d2.id=v2.dataprop
                    where 0=0
                    group by d2.objorrelobj, d2.prop
                    ) t2 on t2.objorrelobj=ob.id and t2.prop=:Prop_KATO
                left join DataProp d3 on d3.isobj=1 and d3.objorrelobj =ob.id and d3.prop=:Prop_ReservoirType
                left join DataPropVal v3 on d3.id=v3.dataprop
                left join DataProp d4 on d4.isobj=1 and d4.objorrelobj=ob.id and d4.prop=:Prop_ReservoirStatus
                left join DataPropVal v4 on d4.id=v4.dataprop
                left join DataProp d5 on d5.isobj=1 and d5.objorrelobj=ob.id and d5.prop=:Prop_FishFarmingType
                left join DataPropVal v5 on d5.id=v5.dataprop
                left join DataProp d6 on d6.isobj=1 and d6.objorrelobj=ob.id and d6.prop=:Prop_Coordinate
                left join DataPropVal v6 on d6.id=v6.dataprop
                left join DataProp d7 on d7.isobj=1 and d7.objorrelobj=ob.id and d7.prop=:Prop_Description
                left join DataPropVal v7 on d7.id=v7.dataprop
        """, map)
        //mdb.outTable(st)

        Store stBranch = mdb.loadQuery("""
            select
                t1.objorrelobj as obj, t1.lstBranch
            from (
                    select d1.objorrelobj, d1.prop,
                    STRING_AGG (cast(v1.id||'_'||v1.obj||'_'||v1.propval as varchar(2000)), ',' order by v1.id) as lstBranch
                    from DataProp d1   
                        left join DataPropVal v1 on d1.id=v1.dataprop
                    where d1.isobj=1 and d1.prop=${map.get("Prop_Branch")}
                    group by d1.objorrelobj, d1.prop
                 ) t1
        """)

        StoreIndex indBranch = stBranch.getIndex("obj")
        for (StoreRecord r in st) {
            StoreRecord rec = indBranch.get(r.getLong("obj"))
            if (rec != null)
                r.set("lstBranch", rec.getString("lstBranch"))
        }

        Store stFV = apiMeta().get(ApiMeta).storeFVfromPropVal()
        StoreIndex indFV = stFV.getIndex("propval")

        for (StoreRecord r in st) {
            List<String> objBranch = new ArrayList<>()
            String lstBranch = r.getString("lstBranch")
            String[] arr0 = lstBranch.split(",")
            List<Object> idsObj = new ArrayList<>()
            for (String it in arr0) {
                String[] arr1 = it.split("_")
                objBranch.add(arr1[1] + "_" + arr1[2])
                idsObj.add(arr1[1])
            }
            r.set("objBranch", objBranch.join(","))
            Store stObj = loadSqlService("""
                select v.name from Obj o, ObjVer v where o.id=v.ownerVer and v.lastVer=1 and o.id in (${idsObj.join(",")})
            """, "", "nsidata")
            r.set("nameBranch", stObj.getUniqueValues("name").join("; "))
            //
            List<String> objKATO = new ArrayList<>()
            String lstKATO = r.getString("lstKATO")
            arr0 = lstKATO.split(",")
            idsObj = new ArrayList<>()
            for (String it in arr0) {
                String[] arr1 = it.split("_")
                objKATO.add(arr1[1] + "_" + arr1[2])
                idsObj.add(arr1[1])
            }
            r.set("objKATO", objKATO.join(","))
            stObj = loadSqlService("""
                select v.name from Obj o, ObjVer v where o.id=v.ownerVer and v.lastVer=1 and o.id in (${idsObj.join(",")})
            """, "", "nsidata")
            r.set("nameKATO", stObj.getUniqueValues("name").join("; "))
            //
            StoreRecord rec = indFV.get(r.getLong("pvReservoirType"))
            if (rec != null)
                r.set("fvReservoirType", rec.getLong("factorval"))
            rec = indFV.get(r.getLong("pvReservoirStatus"))
            if (rec != null)
                r.set("fvReservoirStatus", rec.getLong("factorval"))
            rec = indFV.get(r.getLong("pvFishFarmingType"))
            if (rec != null)
                r.set("fvFishFarmingType", rec.getLong("factorval"))
        }
        return st
    }

    @DaoMethod
    Store loadReservoirsMeter(long obj, long prop, String dte, long periodType) {
        String props = "'Prop_WaterArea','Prop_WaterLevel','Prop_WaterLength','Prop_ReservoirWidth','Prop_ReservoirDepth','Prop_WaterFishAverageWeight','Prop_WaterNumberFishBio','Prop_CalcPdy','Prop_ReservoirPdy','Prop_CalcWaterFluct','Prop_NumberFishCaught','Prop_GearCatchabilityNet','Prop_GearCatchabilitySeine'"
        return loadMetersOfOwnerWithPeriod(obj, 1, prop, dte, periodType, props)
    }

    private Store loadMetersOfOwnerWithPeriod(long own, int isObj, long prop,
                                              String dte, long periodType, String props) {
        //String props = "'Prop_WaterArea','Prop_WaterLevel','Prop_WaterLength','Prop_ReservoirWidth','Prop_ReservoirDepth','Prop_WaterFishAverageWeight','Prop_WaterNumberFishBio','Prop_CalcPdy','Prop_ReservoirPdy','Prop_CalcWaterFluct','Prop_NumberFishCaught','Prop_GearCatchabilityNet','Prop_GearCatchabilitySeine'"
        if (prop > 0) {
            return mdb.loadQuery("""
                select d.prop as id, v.numberval, v.dbeg, v.dend, v.id as idval
                from DataProp d
                    left join DataPropVal v on d.id=v.dataProp
                where d.isObj=${isObj} and d.objorrelobj=${own} and d.prop=${prop}
            """)
        } else {
            if (own == 0)
                return mdb.createStore()
            Store st = apiMeta().get(ApiMeta).loadSql("""
                WITH RECURSIVE r AS (
                    SELECT p.id, p.cod, p.parent, p.name || ' ('||m.name||')' as name, p.isdependvalueonperiod as dependperiod, null as dbeg, null as dend, null as numberval, null as idval
                    FROM prop p, Measure m
                    WHERE p.measure=m.id and p.cod in (${props})    
                    UNION ALL    
                    SELECT p1.id, p1.cod, p1.parent, p1.name || ' ('||m1.name||')' as name, p1.isdependvalueonperiod as dependperiod, null as dbeg, null as dend, null as numberval, null as idval
                    FROM  prop p1
                    JOIN Measure m1 ON p1.measure=m1.id
                    JOIN r ON p1.parent = r.id
                )
                SELECT null as obj, id, parent, cod, name, dependperiod, dbeg, dend, numberval, idval
                FROM r;
            """, "")

            Set<Object> idsProp = st.getUniqueValues("id")
            //
            Store stData = mdb.loadQuery("""
                select d.prop as prop, v.numberval, v.dbeg, v.dend, v.id
                from DataProp d
                    left join DataPropVal v on d.id=v.dataProp
                where d.isObj=${isObj} and d.objorrelobj=${own} and d.periodType=${periodType} and 
                    '${dte}' between v.dbeg and v.dend and d.prop in (0${idsProp.join(",")})
                union all
                select d.prop as prop, v.numberval, v.dbeg, v.dend, v.id
                from DataProp d
                    left join DataPropVal v on d.id=v.dataProp
                where d.isObj=${isObj} and d.objorrelobj=${own} and d.periodType is null and '${dte}' between v.dbeg and v.dend 
                    and d.prop in (0${idsProp.join(",")})                
            """)
            StoreIndex indData = stData.getIndex("prop")
            for (StoreRecord r in st) {
                StoreRecord rec = indData.get(r.getLong("id"))
                if (rec != null) {
                    r.set("idval", rec.getLong("id"))
                    r.set("numberval", rec.getDouble("numberval"))
                    r.set("dbeg", rec.getString("dbeg"))
                    r.set("dend", rec.getString("dend"))
                }
            }
            return st
        }
    }

    @DaoMethod
    Store saveReservoirMeter(Map<String, Object> rec) {
        long obj = UtCnv.toLong(rec.get("obj"))
        long prop = UtCnv.toLong(rec.get("prop"))
        long idVal = UtCnv.toLong(rec.get("idval"))
        boolean hasValue = rec.containsKey("numberval")
        double value = UtCnv.toDouble(rec.get("numberval"))
        boolean dependperiod = UtCnv.toInt(rec.get("dependperiod") == 1)
        long pt = UtCnv.toLong(rec.get("pt"))
        String dt = UtCnv.toString(rec.get("dt"))
        String dbeg = "1800-01-01"
        String dend = "3333-12-31"
        if (dependperiod) {
            UtPeriod up = new UtPeriod()
            dbeg = up.calcDbeg(XDate.create(dt), pt, 0).toString(XDateTimeFormatter.ISO_DATE)
            dend = up.calcDend(XDate.create(dt), pt, 0).toString(XDateTimeFormatter.ISO_DATE)
        }

        if (idVal > 0) {
            if (hasValue) {
                String tm = XDateTime.create(new Date()).toString(XDateTimeFormatter.ISO_DATE_TIME)
                mdb.execQueryNative("""
                    update DataPropVal set numberval=${value}, dbeg='${dbeg}', dend='${dend}', timestamp='${tm}'
                    where id=${idVal}
                """)
            } else {
                mdb.execQueryNative("""
                    delete from DataPropVal
                    where dataProp in (select id from DataProp where isobj=1 and objorrelobj=${obj});
                    delete from DataProp where id in (
                            select id from dataprop
                            except
                            select dataProp as id from DataPropVal
                    );
                """)
            }
        } else if (hasValue) {
            StoreRecord recDP = mdb.createStoreRecord("DataProp")
            recDP.set("isObj", 1)
            recDP.set("objorrelobj", obj)
            recDP.set("prop", prop)
            if (dependperiod)
                recDP.set("periodType", pt)
            long idDP = mdb.insertRec("DataProp", recDP)
            StoreRecord recDPV = mdb.createStoreRecord("DataPropVal")
            recDPV.set("dataProp", idDP)
            recDPV.set("numberVal", value)
            long au = getUser()
            recDPV.set("authUser", au)
            recDPV.set("inputType", FD_InputType_consts.app)
            long idDPV = mdb.getNextId("DataPropVal")
            recDPV.set("id", idDPV)
            recDPV.set("ord", idDPV)
            recDPV.set("dbeg", dbeg)
            recDPV.set("dend", dend)
            recDPV.set("timeStamp", XDateTime.create(new Date()).toString(XDateTimeFormatter.ISO_DATE_TIME))
            mdb.insertRec("DataPropVal", recDPV, false)
        }
        return loadReservoirsMeter(obj, 0, dt, pt)
    }

    @DaoMethod
    Store saveReservoirPropertiesRef(Map<String, Object> params) {
        VariantMap pms = new VariantMap(params)

        long own = pms.getLong("obj")
        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        Map<String, Object> par = new HashMap<>(pms)
        par.put("fullName", pms.get("name"))
        if (pms.getString("mode").equalsIgnoreCase("ins")) {
            if (own > 0) {
                throw new XError("ExistsReservoirWithProps")
            }
            own = eu.insertEntity(par)
            pms.put("own", own)
            //
            List<String> lstBranch = UtCnv.toList(pms.get("objBranch"))
            List<String> lstRegion = UtCnv.toList(pms.get("objKATO"))
/*            for (String it in lstBranch) {
                String[] arr = it.split("_")
                pms.put("objBranch", UtCnv.toLong(arr[0]))
                pms.put("pvBranch", UtCnv.toLong(arr[1]))
                fillProperties(true, "Prop_Branch", pms)
            }*/
            for (String it in lstRegion) {
                String[] arr = it.split("_")
                pms.put("objKATO", UtCnv.toLong(arr[0]))
                pms.put("pvKATO", UtCnv.toLong(arr[1]))
                fillProperties(true, "Prop_KATO", pms)
            }
            //
            //2 Prop_Branch
            if (pms.containsKey("objBranch"))
                fillProperties(true, "Prop_Branch", pms)
            else
                throw new XError("Не указан [objBranch]")

            //3 Prop_ReservoirType
            if (pms.containsKey("fvReservoirType"))
                fillProperties(true, "Prop_ReservoirType", pms)
            else
                throw new XError("Не указан [ReservoirType]")

            //4 Prop_ReservoirStatus
            if (pms.containsKey("fvReservoirStatus"))
                fillProperties(true, "Prop_ReservoirStatus", pms)
            else
                throw new XError("Не указан [ReservoirStatus]")

            //5 Prop_FishFarmingType
            if (pms.containsKey("fvFishFarmingType"))
                fillProperties(true, "Prop_FishFarmingType", pms)

            //6 Prop_Coordinate
            if (pms.containsKey("Coordinate"))
                fillProperties(true, "Prop_Coordinate", pms)

            //7 Prop_Description
            if (pms.containsKey("Description"))
                fillProperties(true, "Prop_Description", pms)

        } else {
            if (pms.getString("mode").equalsIgnoreCase("upd")) {
                if (own == 0) {
                    throw new XError("NotFoundOwner")
                }
                par.put("id", own)
                eu.updateEntity(par)
                pms.put("own", own)
                //
/*
                List<String> lstBranch = UtCnv.toList(pms.get("lstBranch"))
                List<String> lstOldBranch = new ArrayList<>()
                List<String> objBranch = UtCnv.toList(pms.get("objBranch"))
                //Delete
                for (String it in lstBranch) {
                    long idVal = UtCnv.toLong(it.substring(0, it.indexOf("_")))
                    String oldBranch = it.substring(it.indexOf("_") + 1)
                    lstOldBranch.add(oldBranch) // obj_pv
                    if (!objBranch.contains(oldBranch)) {
                        //Delete idVal
                        mdb.execQueryNative("""
                        delete from DataPropVal where id=${idVal};
                        with d as (
                        select id from DataProp
                            except
                            select dataProp as id from DataPropVal
                        )
                        delete from DataProp where id in (select id from d);
                    """)
                    }
                }
                //Add
                for (String it in objBranch) {
                    if (!lstOldBranch.contains(it)) {
                        //Add it
                        String[] arr = it.split("_")
                        pms.put("objBranch", UtCnv.toLong(arr[0]))
                        pms.put("pvBranch", UtCnv.toLong(arr[1]))
                        fillProperties(true, "Prop_Branch", pms)
                    }
                }
*/
                // KATO
                List<String> lstKATO = UtCnv.toList(pms.get("lstKATO"))
                List<String> lstOldKATO = new ArrayList<>()
                List<String> objKATO = UtCnv.toList(pms.get("objKATO"))
                //Delete
                for (String it in lstKATO) {
                    long idVal = UtCnv.toLong(it.substring(0, it.indexOf("_")))
                    String oldKATO = it.substring(it.indexOf("_") + 1)
                    if (!objKATO.contains(oldKATO)) {
                        //Delete idVal
                        mdb.execQueryNative("""
                        delete from DataPropVal where id=${idVal};
                        with d as (
                        select id from DataProp
                            except
                            select dataProp as id from DataPropVal
                        )
                        delete from DataProp where id in (select id from d);
                    """)
                    }
                }
                //Add
                for (String it in objKATO) {
                    if (!lstOldKATO.contains(it)) {
                        //Add it
                        String[] arr = it.split("_")
                        pms.put("objKATO", UtCnv.toLong(arr[0]))
                        pms.put("pvKATO", UtCnv.toLong(arr[1]))
                        fillProperties(true, "Prop_KATO", pms)
                    }
                }
                //
                //3 Prop_ReservoirType
                if (pms.getLong("idReservoirType") > 0) {
                    if (pms.containsKey("fvReservoirType"))
                        updateProperties("Prop_ReservoirType", pms)
                    else
                        throw new XError("Не указан [ReservoirType]")
                }

                //4 Prop_ReservoirStatus
                if (pms.getLong("idReservoirStatus") > 0) {
                    if (pms.containsKey("fvReservoirStatus"))
                        updateProperties("Prop_ReservoirStatus", pms)
                    else
                        throw new XError("Не указан [ReservoirStatus]")
                }

                //5 Prop_FishFarmingType
                if (pms.getLong("idFishFarmingType") > 0) {
                    if (pms.containsKey("fvFishFarmingType"))
                        updateProperties("Prop_FishFarmingType", pms)
                } else {
                    if (pms.containsKey("fvFishFarmingType"))
                        fillProperties(true, "Prop_FishFarmingType", pms)
                }

                //6 Prop_Coordinate
                if (pms.getLong("idCoordinate") > 0) {
                    if (pms.containsKey("Coordinate"))
                        updateProperties("Prop_Coordinate", pms)
                } else {
                    if (pms.containsKey("Coordinate"))
                        fillProperties(true, "Prop_Coordinate", pms)
                }

                //7 Prop_Description
                if (pms.getLong("idDescription") > 0) {
                    if (pms.containsKey("Description"))
                        updateProperties("Prop_Description", pms)
                } else if (pms.containsKey("Description"))
                    fillProperties(true, "Prop_Description", pms)

            } else {
                throw new XError("Не известный режим ввода")
            }
        }
        return loadReservoirsFilial([codTyp: "", idObj: own, filial: pms.getLong("objBranch")] as Map<String, Object>)
    }

    @DaoMethod
    void deleteReservoir(long id) {
        checkForExistData(id, 1)
        deleteOwnerWithProperties(id, 1)
    }

    //---------------- 2 SamplingStation ----------------//
    @DaoMethod
    Store loadSamplingStations(Map<String, Object> params) {

        Store stResoir = loadObjCustom("Prop_ReservoirShore")
        StoreIndex indResoir = stResoir.getIndex("id")

        String codCls = UtCnv.toString(params.get("codCls"))
        long idObj = UtCnv.toLong(params.get("idObj"))

        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "", "Prop_%")
        String whe = "o.id=${idObj}"
        if (idObj == 0) {
            Map<String, Long> map1 = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", codCls, "")
            whe = "o.cls = ${map1.get(codCls)}"
        }
        Store st = mdb.createStore("Obj.sampling.station")
        mdb.loadQuery(st, """
            select o.id as obj, o.cls, v.name, 
                v1.id as idCoordinate, v1.strVal as Coordinate, 
                v2.id as idAreaOfTon, v2.numberVal as AreaOfTon,
                v3.id as idDescription, v3.multiStrVal as Description,
                v4.id as idReservoirShore, v4.obj as objReservoirShore, 
                v4.propVal as pvReservoirShore, null as nameReservoirShore
            from Obj o
                join ObjVer v on o.id=v.ownerver and v.lastver=1
                left join DataProp d1 on d1.isObj=1 and d1.objorrelobj=o.id and d1.prop=:Prop_Coordinate
                left join DataPropVal v1 on d1.id=v1.dataprop 
                left join DataProp d2 on d2.isObj=1 and d2.objorrelobj=o.id and d2.prop=:Prop_AreaOfTon
                left join DataPropVal v2 on d2.id=v2.dataprop
                left join DataProp d3 on d3.isObj=1 and d3.objorrelobj=o.id and d3.prop=:Prop_Description
                left join DataPropVal v3 on d3.id=v3.dataprop
                left join DataProp d4 on d4.isObj=1 and d4.objorrelobj=o.id and d4.prop=:Prop_ReservoirShore
                left join DataPropVal v4 on d4.id=v4.dataprop
            where ${whe}
        """, map)
        //
        st.each { StoreRecord r ->
            StoreRecord rec = indResoir.get(r.getLong("objReservoirShore"))
            if (rec != null) {
                r.set("nameReservoirShore", rec.getString("name"))
            }
        }
        return st
    }

    private Store loadObjCustom(String codTypOrProp) {
        Store st = loadObjForSelect(codTypOrProp, "monitoringdata")
        Set<Object> idsCls = st.getUniqueValues("cls")
        Store stCls = apiMeta().get(ApiMeta).loadSql("""
            select c.id, v.name from Cls c, ClsVer v 
            where c.id=v.ownerVer and v.lastVer=1 and c.id in (0${idsCls.join(",")})
        """, "")
        StoreIndex indCls = stCls.getIndex("id")
        for (StoreRecord r in st) {
            StoreRecord rec = indCls.get(r.getLong("cls"))
            if (rec != null) {
                r.set("name", r.getString("name") + " (" + rec.getString("name") + ")")
            }
        }
        return st
    }

    @DaoMethod
    Store saveSamplingStation(Map<String, Object> params) {
        VariantMap pms = new VariantMap(params)
        long own
        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        Map<String, Object> par = new HashMap<>(pms)
        par.put("fullName", pms.get("name"))
        if (pms.getString("mode").equalsIgnoreCase("ins")) {
            Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", "Cls_Station", "")
            if (map.isEmpty()) throw new XError("NotFoundCod@Cls_Station")
            par.put("cls", map.get("Cls_Station"))
            own = eu.insertEntity(par)
            pms.put("own", own)
            //Prop_AreaOfTon
            fillProperties(true, "Prop_AreaOfTon", pms)
            //Prop_Coordinate
            fillProperties(true, "Prop_Coordinate", pms)
            //Prop_ReservoirShore
            fillProperties(true, "Prop_ReservoirShore", pms)
            //Prop_Description
            if (!pms.getString("Description").isEmpty())
                fillProperties(true, "Prop_Description", pms)
        } else {
            own = pms.getLong("obj")
            par.put("id", own)
            eu.updateEntity(par)
            //
            pms.put("own", own)
            //1 Prop_AreaOfTon
            if (pms.containsKey("idAreaOfTon"))
                updateProperties("Prop_AreaOfTon", pms)
            //2 Prop_Coordinate
            if (pms.containsKey("idCoordinate"))
                updateProperties("Prop_Coordinate", pms)
            //3 Prop_Description
            if (pms.containsKey("idDescription"))
                updateProperties("Prop_Description", pms)
            else if (!pms.getString("Description").isEmpty())
                fillProperties(true, "Prop_Description", pms)

            //3 Prop_Description
            if (pms.getLong("idReservoirShore") > 0)
                updateProperties("Prop_ReservoirShore", pms)
            else if (pms.getLong("objReservoirShore") > 0)
                fillProperties(true, "Prop_ReservoirShore", pms)
        }
        return loadSamplingStations([codCls: "", idObj: own] as Map<String, Object>)
    }

    @DaoMethod
    void deleteSamplingStation(long id) {
        checkForExistData(id, 1)
        deleteOwnerWithProperties(id, 1)
    }

    //---------------- 3 TypesFish ----------------//
    @DaoMethod
    Store loadTypesFish(Map<String, Object> params) {
        String codTyp = UtCnv.toString(params.get("codTyp"))
        long idObj = UtCnv.toLong(params.get("idObj"))

        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "", "Prop_%")
        String whe = "o.id=${idObj}"
        if (idObj == 0) {
            Set<Object> idsCls = apiMeta().get(ApiMeta).setIdsOfCls(codTyp)
            whe = "o.cls in (${idsCls.join(",")})"
        }
        Store st = mdb.createStore("Obj.typesFish")
        mdb.loadQuery(st, """
            select o.id as obj, o.cls, v.name, 
                v1.id as idFishFamily, v1.propVal as pvFishFamily, null as fvFishFamily, 
                v2.id as idFishTyp, v2.propVal as pvFishTyp, null as fvFishTyp,
                v3.id as idDescription, v3.multiStrVal as Description
            from Obj o
                left join ObjVer v on o.id=v.ownerver and v.lastver=1
                left join DataProp d1 on d1.isObj=1 and d1.objorrelobj=o.id and d1.prop=:Prop_FishFamily
                left join DataPropVal v1 on d1.id=v1.dataprop 
                left join DataProp d2 on d2.isObj=1 and d2.objorrelobj=o.id and d2.prop=:Prop_FishTyp
                left join DataPropVal v2 on d2.id=v2.dataprop
                left join DataProp d3 on d3.isObj=1 and d3.objorrelobj=o.id and d3.prop=:Prop_Description
                left join DataPropVal v3 on d3.id=v3.dataprop
            where ${whe}
        """, map)

        Store stFV = apiMeta().get(ApiMeta).storeFVfromPropVal()
        StoreIndex indFV = stFV.getIndex("propval")

        for (StoreRecord r in st) {
            StoreRecord rec = indFV.get(r.getLong("pvFishFamily"))
            if (rec != null)
                r.set("fvFishFamily", rec.getLong("factorval"))
            rec = indFV.get(r.getLong("pvFishTyp"))
            if (rec != null)
                r.set("fvFishTyp", rec.getLong("factorval"))
        }
        //mdb.outTable(st)
        return st
    }

    @DaoMethod
    Store loadTypesFishMeters(long own) {
        String props = "Prop_CalcAgeSex,Prop_FishMaxAge,Prop_CalcAgePrey,Prop_FishSpeed,Prop_CalcMaxNumberFry,Prop_CalcEggSurvivalRate,Prop_CalcBaseMortality,Prop_CalcParabolaLeft,Prop_CalcParabolaRight,Prop_CalcBaseEating,Prop_CalcPdyDevCoef"
        return loadMetersWithOutPeriod(own, props)
    }

    @DaoMethod
    Store saveTypesFish(Map<String, Object> params) {
        VariantMap pms = new VariantMap(params)
        long own
        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        Map<String, Object> par = new HashMap<>(pms)
        par.put("fullName", pms.get("name"))
        if (pms.getString("mode").equalsIgnoreCase("ins")) {
            par.put("cls", params.get("cls"))
            own = eu.insertEntity(par)
            pms.put("own", own)
            //Prop_FishFamily
            fillProperties(true, "Prop_FishFamily", pms)
            //Prop_FishTyp
            fillProperties(true, "Prop_FishTyp", pms)
            //Prop_Description
            if (!pms.getString("Description").isEmpty())
                fillProperties(true, "Prop_Description", pms)
        } else {
            own = pms.getLong("obj")
            par.put("id", own)
            eu.updateEntity(par)
            //
            pms.put("own", own)
            //1 Prop_FishFamily
            if (pms.containsKey("idFishFamily"))
                updateProperties("Prop_FishFamily", pms)
            //2 Prop_FishTyp
            if (pms.containsKey("idFishTyp"))
                updateProperties("Prop_FishTyp", pms)
            //3 Prop_Description
            if (pms.containsKey("idDescription"))
                updateProperties("Prop_Description", pms)
            else if (!pms.getString("Description").isEmpty())
                fillProperties(true, "Prop_Description", pms)
        }
        return loadTypesFish([codTyp: "", idObj: own] as Map<String, Object>)
    }

    @DaoMethod
    void deleteTypesFish(long id) {
        checkForExistData(id, 1)
        deleteOwnerWithProperties(id, 1)
    }

    @DaoMethod
    long saveTypesFishMeters(Map<String, Object> rec) {
        rec.put("dependperiod", 0)
        return saveMeter(rec)
    }

    //---------------- 5 PiscesReservoir ----------------//
    @DaoMethod
    Store loadPiscesReservoir(Map<String, Object> params) {
        String codRelTyp = UtCnv.toString(params.get("codRelTyp"))
        long relobj = UtCnv.toLong(params.get("relobj"))
        String whe = "o.id=" + relobj
        if (relobj == 0) {
            Set<Object> idsRelCls = apiMeta().get(ApiMeta).setIdsOfRelCls(codRelTyp)
            whe = "o.relcls in (0" + idsRelCls.join(",") + ")"
        }
        Map<String, Long> mapProps = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "", "Prop_Fish%")
        Store st = mdb.createStore("RelObj.FishReservoir")
        mdb.loadQuery(st, """
            select o.id as relobj, o.relcls, null::bigint as reservoir, null::bigint as cls1, 
                null::bigint as typeOfFish, null::bigint as cls2,
                v1.id as idFishSpawPeriod, v1.strVal as FishSpawPeriod,
                v2.id as idFishStartPuberty, v2.numberVal as FishStartPuberty,
                v3.id as idFishEndPuberty, v3.numberVal as FishEndPuberty,
                v4.id as idFishSpawFrequency, v4.strVal as FishSpawFrequency
            from RelObj o
                left join DataProp d1 on d1.isObj=0 and d1.objorrelobj=o.id and d1.prop=:Prop_FishSpawPeriod
                left join DataPropVal v1 on v1.dataProp=d1.id
                left join DataProp d2 on d2.isObj=0 and d2.objorrelobj=o.id and d2.prop=:Prop_FishStartPuberty
                left join DataPropVal v2 on v2.dataProp=d2.id
                left join DataProp d3 on d3.isObj=0 and d3.objorrelobj=o.id and d3.prop=:Prop_FishEndPuberty
                left join DataPropVal v3 on v3.dataProp=d3.id
                left join DataProp d4 on d4.isObj=0 and d4.objorrelobj=o.id and d4.prop=:Prop_FishSpawFrequency
                left join DataPropVal v4 on v4.dataProp=d4.id
            where ${whe}
        """, mapProps)

        Store stROM = mdb.loadQuery("""
            select relobj, 
                STRING_AGG (cast(cls as varchar(2000)), ',' order by id) as clslist, 
                STRING_AGG (cast(obj as varchar(2000)), ',' order by id) as objlist 
            from relobjmember
            where relobj in (select o.id from RelObj o where ${whe}) 
            group by relobj
        """)
        StoreIndex indROM = stROM.getIndex("relobj")
        //
        for (StoreRecord r in st) {
            StoreRecord rec = indROM.get(r.getLong("relobj"))
            if (rec != null) {
                def clss = rec.getString("clslist").split(",")
                def objs = rec.getString("objlist").split(",")
                long cls1 = UtCnv.toLong(clss[0])
                long cls2 = UtCnv.toLong(clss[1])
                long obj1 = UtCnv.toLong(objs[0])
                long obj2 = UtCnv.toLong(objs[1])

                r.set("reservoir", obj1)
                r.set("cls1", cls1)
                r.set("typeOfFish", obj2)
                r.set("cls2", cls2)
            }
        }
        return st
    }

    @DaoMethod
    void deleteReservoirsMeter(long idDPV) {
        mdb.execQueryNative("""
            delete from DataPropVal
            where id=${idDPV};
            delete from DataProp where id in (
                select id from dataprop
                except
                select dataProp as id from DataPropVal
            );
        """)
    }

    @DaoMethod
    Store savePiscesReservoir(Map<String, Object> params) {
        VariantMap form = new VariantMap(params)
        long relobj = 0
        //
        if (form.getString("mode") == "ins") {
            Store stTmp = mdb.loadQuery("""
                select v.name 
                from RelObjMember r 
                    inner join RelObjMember r2 on r.relobj=r2.relobj and r2.cls=${form.getLong("cls2")} and r2.obj=${form.getLong("typeOfFish")}
                    inner join relobjver v on r.relobj=v.ownerver and v.lastver=1
                where r.cls=${form.getLong("cls1")} and r.obj=${form.getLong("reservoir")}
            """)
            if (stTmp.size() > 0) {
                throw new XError("exists@${stTmp.get(0).getString("name")}")
            }
            //
            long relCls = apiMeta().get(ApiMeta).idRelCls(form.getLong("cls1"), form.getLong("cls2"))
            Map<String, Object> pms = new HashMap<>()
            pms.put("relCls", relCls)
            String n1 = mdb.loadQuery(
                    "select name from ObjVer where ownerVer=:o and lastVer=1", [o: form.getLong("reservoir")])
                    .get(0).getString("name")
            String n2 = mdb.loadQuery(
                    "select name from ObjVer where ownerVer=:o and lastVer=1", [o: form.getLong("typeOfFish")])
                    .get(0).getString("name")
            pms.put("name", n1 + " => " + n2)
            pms.put("fullName", n1 + " => " + n2)
            EntityMdbUtils eu = new EntityMdbUtils(mdb, "RelObj")
            relobj = eu.insertEntity(pms)
            //
            Store stRCM = apiMeta().get(ApiMeta).loadRelClsMember(relCls)

            StoreRecord recROM = mdb.createStoreRecord("RelObjMember")
            recROM.set("relObj", relobj)
            recROM.set("relClsMember", stRCM.get(0).getLong("id"))
            recROM.set("cls", form.getLong("cls1"))
            recROM.set("obj", form.getLong("reservoir"))
            mdb.insertRec("RelObjMember", recROM, true)
            //
            recROM = mdb.createStoreRecord("RelObjMember")
            recROM.set("relObj", relobj)
            recROM.set("relClsMember", stRCM.get(1).getLong("id"))
            recROM.set("cls", form.getLong("cls2"))
            recROM.set("obj", form.getLong("typeOfFish"))
            mdb.insertRec("RelObjMember", recROM, true)
            //
            form.put("own", relobj)
            //Prop_FishSpawPeriod
            if (!form.getString("FishSpawPeriod").isEmpty())
                fillProperties(false, "Prop_FishSpawPeriod", form)
            //Prop_FishStartPuberty
            if (!form.getString("FishStartPuberty").isEmpty())
                fillProperties(false, "Prop_FishStartPuberty", form)
            //Prop_FishEndPuberty
            if (!form.getString("FishEndPuberty").isEmpty())
                fillProperties(false, "Prop_FishEndPuberty", form)
            //Prop_FishSpawFrequency
            if (!form.getString("FishSpawFrequency").isEmpty())
                fillProperties(false, "Prop_FishSpawFrequency", form)
        } else if (form.getString("mode") == "upd") {
            relobj = form.getLong("relobj")
            form.put("own", relobj)
            //Prop_FishSpawPeriod
            if (form.getLong("idFishSpawPeriod") > 0)
                updateProperties("Prop_FishSpawPeriod", form)
            else if (!form.getString("FishSpawPeriod").isEmpty())
                fillProperties(false, "Prop_FishSpawPeriod", form)

            //Prop_FishStartPuberty
            if (form.getLong("idFishStartPuberty") > 0)
                updateProperties("Prop_FishStartPuberty", form)
            else if (!form.getString("FishStartPuberty").isEmpty())
                fillProperties(false, "Prop_FishStartPuberty", form)

            //Prop_FishEndPuberty
            if (form.getLong("idFishEndPuberty") > 0)
                updateProperties("Prop_FishEndPuberty", form)
            else if (!form.getString("FishEndPuberty").isEmpty())
                fillProperties(false, "Prop_FishEndPuberty", form)

            //Prop_FishSpawFrequency
            if (form.getLong("idFishSpawFrequency") > 0)
                updateProperties("Prop_FishSpawFrequency", form)
            else if (!form.getString("FishSpawFrequency").isEmpty())
                fillProperties(false, "Prop_FishSpawFrequency", form)
        } else {
            throw new XError("Не известный режим ввода [${form.getString("mode")}]")
        }
        //
        return loadPiscesReservoir(Map.of("relobj", (Object) relobj))
    }

    @DaoMethod
    void deletePiscesReservoir(long id) {
        checkForExistData(id, 0)
        deleteOwnerWithProperties(id, 0)
    }


    @DaoMethod
    Store loadFishFecundity(long relobj, long prop, String dte, long periodType) {
        String props = "'Prop_FishFecundity','Prop_CalcStartPopulation','Prop_CalcStartPopulationBalance'"
        return loadMetersOfOwnerWithPeriod(relobj, 0, prop, dte, periodType, props)
    }

    @DaoMethod
    Store saveFishFecundity(Map<String, Object> rec) {
        long obj = UtCnv.toLong(rec.get("obj"))
        long prop = UtCnv.toLong(rec.get("prop"))
        long idVal = UtCnv.toLong(rec.get("idval"))
        boolean hasValue = rec.containsKey("numberval")
        double value = UtCnv.toDouble(rec.get("numberval"))
        boolean dependperiod = UtCnv.toInt(rec.get("dependperiod") == 1)
        long pt = UtCnv.toLong(rec.get("pt"))
        String dt = UtCnv.toString(rec.get("dt"))
        String dbeg = "1800-01-01"
        String dend = "3333-12-31"
        if (dependperiod) {
            UtPeriod up = new UtPeriod()
            dbeg = up.calcDbeg(XDate.create(dt), pt, 0).toString(XDateTimeFormatter.ISO_DATE)
            dend = up.calcDend(XDate.create(dt), pt, 0).toString(XDateTimeFormatter.ISO_DATE)
        }

        if (idVal > 0) {
            if (hasValue) {
                String tm = XDateTime.create(new Date()).toString(XDateTimeFormatter.ISO_DATE_TIME)
                mdb.execQueryNative("""
                    update DataPropVal set numberval=${value}, dbeg='${dbeg}', dend='${dend}', timestamp='${tm}'
                    where id=${idVal}
                """)
            } else {
                mdb.execQueryNative("""
                    delete from DataPropVal
                    where dataProp in (select id from DataProp where isobj=1 and objorrelobj=${obj});
                    delete from DataProp where id in (
                            select id from dataprop
                            except
                            select dataProp as id from DataPropVal
                    );
                """)
            }
        } else if (hasValue) {
            StoreRecord recDP = mdb.createStoreRecord("DataProp")
            recDP.set("isObj", 0)
            recDP.set("objorrelobj", obj)
            recDP.set("prop", prop)
            if (dependperiod)
                recDP.set("periodType", pt)
            long idDP = mdb.insertRec("DataProp", recDP)
            StoreRecord recDPV = mdb.createStoreRecord("DataPropVal")
            recDPV.set("dataProp", idDP)
            recDPV.set("numberVal", value)
            long au = getUser()
            recDPV.set("authUser", au)
            recDPV.set("inputType", FD_InputType_consts.app)
            long idDPV = mdb.getNextId("DataPropVal")
            recDPV.set("id", idDPV)
            recDPV.set("ord", idDPV)
            recDPV.set("dbeg", dbeg)
            recDPV.set("dend", dend)
            recDPV.set("timeStamp", XDateTime.create(new Date()).toString(XDateTimeFormatter.ISO_DATE_TIME))
            mdb.insertRec("DataPropVal", recDPV, false)
        }
        return loadFishFecundity(obj, 0, dt, pt)
    }

    @DaoMethod
    void deleteFishFecundity(long idDPV) {
        mdb.execQueryNative("""
            delete from DataPropVal
            where id=${idDPV};
            delete from DataProp where id in (
                select id from dataprop
                except
                select dataProp as id from DataPropVal
            );
        """)
    }

    @DaoMethod
    Store loadFilials() {
        //Cls_Enterprise	1007 предпритие
        //Cls_Branch		1008 филиал
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", "Cls_Enterprise", "")
        if (map.size() == 0)
            throw new XError("Не найден код класса [Cls_Enterprise]")
        Map<String, Long> map1 = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", "Cls_Branch", "")
        if (map1.size() == 0)
            throw new XError("Не найден код класса [Cls_Branch]")
        map.putAll(map1)
        map1 = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "Prop_Branch", "")
        if (map1.size() == 0)
            throw new XError("Не найден код свойств [Prop_Branch]")
        map.putAll(map1)
        Store st = apiNSIData().get(ApiNSIData).loadSqlWithParams("""
            select o.id, v.name || ' (' || t.name || ')' as name, null as cnt 
            from Obj o
                join Objver v on o.id=v.ownerver and v.lastver=1
                left join (
                    select o.id, v.name 
                    from Obj o, Objver v
                    where o.id=v.ownerver and v.lastver=1 and o.cls=:Cls_Enterprise
                ) t on t.id=v.objparent
            where o.cls=:Cls_Branch
            order by o.id
        """, map as Map<String, Object>, "")
        Set<Object> idsFilial = st.getUniqueValues("id")

        Store stCount = mdb.loadQuery("""
            select v.obj, count(*) as cnt
            from datapropval v
                join dataprop d on d.isObj=1 and d.id=v.dataprop and d.prop=${map.get("Prop_Branch")}
            where v.obj in (0${idsFilial.join(",")})
            group by v.obj
        """)
        StoreIndex indCount = stCount.getIndex("obj")
        for (StoreRecord r in st) {
            StoreRecord rec = indCount.get(r.getLong("id"))
            if (rec != null)
                r.set("cnt", rec.getLong("cnt"))
        }

        return st
    }

    @DaoMethod
    Store loadReservoirAll(String codTyp) {
        Store st = loadObjForSelect(codTyp, "monitoringdata")
        return st
    }

    @DaoMethod
    Store loadReservoir(String codTypOrProp) {
        Store st = loadObjForSelect(codTypOrProp, "monitoringdata")
        return st
    }

    @DaoMethod
    Store loadReservoirs(String reservoirs) {
        //Prop_ReservoirShore
        Map<String, Long> mapProp = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "Prop_ReservoirShore", "")
        Store stProp = apiMeta().get(ApiMeta).loadSql("""
                select cls, id as propval
                from PropVal
                where prop=${mapProp.get("Prop_ReservoirShore")} and cls is not null
            """, "")
        StoreIndex indProp = stProp.getIndex("cls")
        //
        Store stObj = mdb.loadQuery("""
            select o.id, o.cls, v.name, null as pv
            from Obj o, ObjVer v
            where o.id=v.ownerVer and v.lastVer=1 and o.id in (${reservoirs})
        """)
        for (StoreRecord r in stObj) {
            StoreRecord rec = indProp.get(r.getLong("cls"))
            if (rec != null) {
                r.set("pv", rec.getLong("propval"))
            }
        }
        //
        Set<Object> idsCls = stObj.getUniqueValues("cls")
        Store stCls = apiMeta().get(ApiMeta).loadSql("""
            select c.id, v.name from Cls c, ClsVer v 
            where c.id=v.ownerVer and v.lastVer=1 and c.id in (0${idsCls.join(",")})
        """, "")
        StoreIndex indCls = stCls.getIndex("id")
        for (StoreRecord r in stObj) {
            StoreRecord rec = indCls.get(r.getLong("cls"))
            if (rec != null) {
                r.set("name", r.getString("name") + " (" + rec.getString("name") + ")")
            }
        }
        return stObj
    }

    @DaoMethod
    Store loadTypeOfFish(String codTyp) {
        return loadObjForSelect(codTyp, "monitoringdata")
    }

    @DaoMethod
    Store loadBranchForSelect(String codTypOrProp) {
        return loadObjForSelectMulti(codTypOrProp, "nsidata")
    }

    @DaoMethod
    Store loadKatoForSelect(String codTypOrProp) {
        return loadObjForTreeSelect(codTypOrProp, "nsidata")
    }

    @DaoMethod
    Store loadObjForSelect(String codTypOrProp, String model) {
        if (codTypOrProp.startsWith("Typ_")) {
            Set<Object> idsCls = apiMeta().get(ApiMeta).setIdsOfCls(codTypOrProp)
            idsCls.add(0)
            return loadSqlService("""
                select o.id, o.cls, v.name
                from Obj o, ObjVer v
                where o.id=v.ownerVer and v.lastVer=1 and o.cls in (${idsCls.join(",")})
            """, "", model)
        } else if (codTypOrProp.startsWith("Prop_")) {
            Map<String, Long> mapProp = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", codTypOrProp, "")
            Store stProp = apiMeta().get(ApiMeta).loadSql("""
                select cls, id as propval
                from PropVal
                where prop=${mapProp.get(codTypOrProp)} and cls is not null
            """, "")
            StoreIndex indProp = stProp.getIndex("cls")

            Set<Object> idsCls = stProp.getUniqueValues("cls")

            Store stObj = loadSqlService("""
                select o.id, o.cls, v.name, null as pv
                from Obj o, ObjVer v
                where o.id=v.ownerVer and v.lastVer=1 and o.cls in (0${idsCls.join(",")})
            """, "", model)
            for (StoreRecord r in stObj) {
                StoreRecord rec = indProp.get(r.getLong("cls"))
                if (rec != null) {
                    r.set("pv", rec.getLong("propval"))
                }
            }
            return stObj
        } else {
            throw new XError("Неверный параметр")
        }
    }

    @DaoMethod
    Store loadObjForSelectMulti(String codProp, String model) {
        Map<String, Long> mapProp = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", codProp, "")
        if (mapProp.size() == 0)
            throw new XError("Не найден код свойств [${codProp}]")

        Store stProp = apiMeta().get(ApiMeta).loadSql("""
                select cls, id as propval
                from PropVal
                where prop=${mapProp.get(codProp)} and cls is not null
            """, "")
        StoreIndex indProp = stProp.getIndex("cls")

        Set<Object> idsCls = stProp.getUniqueValues("cls")

        Store stObj = loadSqlService("""
                select o.id as obj, o.cls, v.name, null as id
                from Obj o, ObjVer v
                where o.id=v.ownerVer and v.lastVer=1 and o.cls in (0${idsCls.join(",")})
            """, "", model)
        for (StoreRecord r in stObj) {
            StoreRecord rec = indProp.get(r.getLong("cls"))
            if (rec != null) {
                r.set("id", r.getLong("obj") + "_" + rec.getLong("propval"))
            }
        }
        return stObj
    }

    @DaoMethod
    Store loadObjForTreeSelect(String codTypOrProp, String model) {
        if (codTypOrProp.startsWith("Typ_")) {
            Set<Object> idsCls = apiMeta().get(ApiMeta).setIdsOfCls(codTypOrProp)
            idsCls.add(0)
            return loadSqlService("""
                select o.id, o.cls, v.name, v.objParent as parent
                from Obj o, ObjVer v
                where o.id=v.ownerVer and v.lastVer=1 and o.cls in (${idsCls.join(",")})
            """, "", model)
        } else if (codTypOrProp.startsWith("Prop_")) {
            Map<String, Long> mapProp = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", codTypOrProp, "")
            Store stProp = apiMeta().get(ApiMeta).loadSql("""
                select cls, id as propval
                from PropVal
                where prop=${mapProp.get(codTypOrProp)} and cls is not null
            """, "")
            StoreIndex indProp = stProp.getIndex("cls")

            Set<Object> idsCls = stProp.getUniqueValues("cls")

            Store stObj = loadSqlService("""
                select o.id, o.cls, v.name, v.objParent as parent, null as key
                from Obj o, ObjVer v
                where o.id=v.ownerVer and v.lastVer=1 and o.cls in (0${idsCls.join(",")})
            """, "", model)
            for (StoreRecord r in stObj) {
                StoreRecord rec = indProp.get(r.getLong("cls"))
                if (rec != null) {
                    r.set("key", r.getString("id") + "_" + rec.getString("propval"))
                }
            }
            return stObj

        } else {
            throw new XError("Неверный параметр")
        }
    }

    @DaoMethod
    Store loadPeriodType() {
        return loadSqlMeta("select id, text from FD_PeriodType where vis=1", "")
    }

    @DaoMethod
    Map<Long, String> loadFvReservoirTypeAsMap(String codProp) {
        return loadFVasMap(codProp)
    }

    @DaoMethod
    Store loadFvReservoirTypeAsStore(String codProp) {
        return loadFVasStore(codProp)
    }

    @DaoMethod
    Map<Long, String> loadFvReservoirStatusAsMap(String codProp) {
        return loadFVasMap(codProp)
    }

    @DaoMethod
    Store loadFvReservoirStatusAsStore(String codProp) {
        return loadFVasStore(codProp)
    }

    @DaoMethod
    Map<Long, String> loadFvFishFarmingTypeAsMap(String codProp) {
        return loadFVasMap(codProp)
    }

    @DaoMethod
    Store loadFvFishFarmingTypeAsStore(String codProp) {
        return loadFVasStore(codProp)
    }

    @DaoMethod
    Store loadFVasStore(String codProp) {
        return apiMeta().get(ApiMeta).loadFVasStore(codProp)
    }

    @DaoMethod
    Map<Long, String> loadFVasMap(String codProp) {
        return apiMeta().get(ApiMeta).loadFVasMap(codProp)
    }

    @DaoMethod
    Store loadCls(String codTyp) {
        Store st = apiMeta().get(ApiMeta).loadCls(codTyp)
        if (st.size() == 0)
            throw new XError("NotFoundCod@${codTyp}")
        return st
    }

    /*
        delete Owner with properties
    */

    @DaoMethod
    void deleteOwnerWithProperties(long id, int isObj) {
        String tableName = isObj == 1 ? "Obj" : "RelObj"
        //
        //checkForExistData(id, isObj)
        //
        EntityMdbUtils eu = new EntityMdbUtils(mdb, tableName)
        mdb.execQueryNative("""
            delete from DataPropVal
            where dataProp in (select id from DataProp where isobj=${isObj} and objorrelobj=${id});
            delete from DataProp where id in (
                select id from dataprop
                except
                select dataProp as id from DataPropVal
            );
        """)
        if (tableName.equalsIgnoreCase("RelObj")) {
            try {
                mdb.execQueryNative("""
                    delete from RelObjMember
                    where relobj=${id};
                """)
            } finally {
                eu.deleteEntity(id)
            }
        } else
            eu.deleteEntity(id)
    }

    private void checkForExistData_new(long id, int isObj, String prop) {
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", prop, "")
        if (map.size() == 0)
            throw new XError("Не найден код [${prop}]")
        long idProp = map.get(prop)
        if (isObj == 1) {
            //1 Объект является значением объекта/отношения?
            Store stTmp = mdb.loadQuery("""
                select  
                    ov.name nm1, d.prop, d.periodType, v.dbeg, v.dend, d.isObj,
                    case when d.isObj = 1 then ov1.name when d.isObj = 0 then rv1.name end as nm2
                from DataProp d
                    inner join DataPropVal v on d.id=v.dataprop
                    left join ObjVer ov1 on d.isObj=1 and d.objorrelobj=ov1.ownerver and ov1.lastver=1
                    left join RelObjVer rv1 on d.isObj=0 and d.objorrelobj=rv1.ownerver and rv1.lastver=1
                    left join ObjVer ov on ov.ownerver=${id} and ov.lastver=1
                where d.prop=${idProp} and v.obj=${id}
            """)
            if (stTmp.size() > 0) {
                String nm = "Объект [" + stTmp.get(0).getString("nm1") + "]"
                String objOrRelObj = stTmp.get(0).getInt("isObj") == 1 ? "объекта" : "отношения"
                String periodName = " за [" + stTmp.get(0).getString("dbeg") + " - " + stTmp.get(0).getString("dend") + "]"
                if (stTmp.get(0).getLong("periodType") > 0) {
                    PeriodGenerator pg = new PeriodGenerator()
                    periodName = " за " + pg.getPeriodName(stTmp.get(0).getDate("dbeg"), stTmp.get(0).getDate("dend"), stTmp.get(0).getLong("periodType"), 3)
                }
                Store stProp = loadSqlMeta("""
                    select name from Prop where id=${stTmp.get(0).getLong("prop")}
                """, "")
                throw new XError(nm + " является значением свойства [" + stProp.get(0).getString("name") + "] " + objOrRelObj + " [" + stTmp.get(0).getString("nm2") + "]" + periodName)
            }
        } else {
            //2 отношение является значением объекта/отношения?
            Store stTmp = mdb.loadQuery("""
                select  
                    rov.name nm1, d.prop, d.periodType, v.dbeg, v.dend, d.isObj,
                    case when d.isObj = 1 then ov1.name when d.isObj = 0 then rv1.name end as nm2
                from DataProp d
                    inner join DataPropVal v on d.id=v.dataprop
                    left join ObjVer ov1 on d.isObj=1 and d.objorrelobj=ov1.ownerver and ov1.lastver=1
                    left join RelObjVer rv1 on d.isObj=0 and d.objorrelobj=rv1.ownerver and rv1.lastver=1
                    left join RelObjVer rov on rov.ownerver=${id} and rov.lastver=1
                where d.prop=${idProp} and v.relobj=${id}
            """)
            if (stTmp.size() > 0) {
                String nm = "Отношение [" + stTmp.get(0).getString("nm1") + "]"
                String objOrRelObj = stTmp.get(0).getInt("isObj") == 1 ? "объекта" : "отношения"
                String periodName = " за [" + stTmp.get(0).getString("dbeg") + " - " + stTmp.get(0).getString("dend") + "]"
                if (stTmp.get(0).getLong("periodType") > 0) {
                    PeriodGenerator pg = new PeriodGenerator()
                    periodName = " за " + pg.getPeriodName(stTmp.get(0).getDate("dbeg"), stTmp.get(0).getDate("dend"), stTmp.get(0).getLong("periodType"), 3)
                }
                Store stProp = loadSqlMeta("""
                    select name from Prop where id=${stTmp.get(0).getLong("prop")}
                """, "")
                throw new XError(nm + " является значением свойства [" + stProp.get(0).getString("name") + "] " + objOrRelObj + " [" + stTmp.get(0).getString("nm2") + "]" + periodName)
            }
        }

    }

    private void checkForExistData(long id, int isObj) {
        if (isObj == 1) {
            // 1 Родитель ?
            Store stTmp = mdb.loadQuery("""
                select distinct ov1.name
                from Obj o
                    left join ObjVer ov on o.id=ov.ownerver and ov.lastver=1
                    left join ObjVer ov1 on ov1.ownerVer=ov.objParent
                where ov.objParent=${id}
            """)
            if (stTmp.size() > 0) {
                throw new XError("Объект [" + stTmp.get(0).getString("name") + "] имеет дочерние элементы")
            }
            //2 Участник ?
            stTmp = mdb.loadQuery("""
                select ov.name as nm1, rv.name as nm
                from relobjmember m
                    inner join ObjVer ov on m.obj=ov.ownerver and ov.lastver=1
                    left join RelObjVer rv on m.relobj=rv.ownerver and rv.lastver=1
                where m.obj=${id}
            """)
            if (stTmp.size() > 0) {
                throw new XError("Объект [" + stTmp.get(0).getString("nm1") + "] является участником отношения [" + stTmp.get(0).getString("nm") + "]")
            }
            //
            //3 Объект имеет значение?
/*
            stTmp = mdb.loadQuery("""
                select ov.name nm1, d.prop, d.periodType, v.dbeg, v.dend
                from DataProp d
                    left join DataPropVal v on d.id=v.dataprop
                    inner join ObjVer ov on d.isObj=1 and d.objorrelobj=ov.ownerver and ov.lastver=1
                where d.isObj=1 and d.objorrelobj=${id} and v.obj is null and v.relobj is null
            """)
            if (stTmp.size() > 0) {
                String periodName = " за [" + stTmp.get(0).getString("dbeg") + " - " + stTmp.get(0).getString("dend") + "]"
                if (stTmp.get(0).getLong("periodType") > 0) {
                    PeriodGenerator pg = new PeriodGenerator()
                    periodName = " за " + pg.getPeriodName(stTmp.get(0).getDate("dbeg"), stTmp.get(0).getDate("dend"), stTmp.get(0).getLong("periodType"), 3)
                }
                Store stProp = loadSqlMeta("""
                    select name from Prop where id=${stTmp.get(0).getLong("prop")}
                """, "")
                throw new XError("Имеется значения свойства [" + stProp.get(0).getString("name") + "] объекта [" + stTmp.get(0).getString("nm1") + "]" + periodName)
            }
*/
            //3 Объект является значением объекта/отношения?
            stTmp = mdb.loadQuery("""
                select  
                    ov.name nm1, d.prop, d.periodType, v.dbeg, v.dend, d.isObj,
                    case when d.isObj = 1 then ov1.name when d.isObj = 0 then rv1.name end as nm2
                from DataProp d
                    inner join DataPropVal v on d.id=v.dataprop
                    left join ObjVer ov1 on d.isObj=1 and d.objorrelobj=ov1.ownerver and ov1.lastver=1
                    left join RelObjVer rv1 on d.isObj=0 and d.objorrelobj=rv1.ownerver and rv1.lastver=1
                    left join ObjVer ov on ov.ownerver=${id} and ov.lastver=1
                where v.obj=${id}
            """)
            if (stTmp.size() > 0) {
                String nm = "Объект [" + stTmp.get(0).getString("nm1") + "]"
                String objOrRelObj = stTmp.get(0).getInt("isObj") == 1 ? "объекта" : "отношения"
                String periodName = " за [" + stTmp.get(0).getString("dbeg") + " - " + stTmp.get(0).getString("dend") + "]"
                if (stTmp.get(0).getLong("periodType") > 0) {
                    PeriodGenerator pg = new PeriodGenerator()
                    periodName = " за " + pg.getPeriodName(stTmp.get(0).getDate("dbeg"), stTmp.get(0).getDate("dend"), stTmp.get(0).getLong("periodType"), 3)
                }
                Store stProp = loadSqlMeta("""
                    select name from Prop where id=${stTmp.get(0).getLong("prop")}
                """, "")
                throw new XError(nm + " является значением свойства [" + stProp.get(0).getString("name") + "] " + objOrRelObj + " [" + stTmp.get(0).getString("nm2") + "]" + periodName)
            }

        } else {
            //1 Отношение имеет значение?
/*
            Store stTmp = mdb.loadQuery("""
                select ov.name nm1, d.prop, d.periodType, v.dbeg, v.dend
                from DataProp d
                    left join DataPropVal v on d.id=v.dataprop
                    inner join RelObjVer ov on d.isObj=0 and d.objorrelobj=ov.ownerver and ov.lastver=1
                where d.isObj=0 and d.objorrelobj=${id} and v.obj is null and v.relobj is null
            """)
            if (stTmp.size() > 0) {
                String periodName = " за [" + stTmp.get(0).getString("dbeg") + " - " + stTmp.get(0).getString("dend") + "]"
                if (stTmp.get(0).getLong("periodType") > 0) {
                    PeriodGenerator pg = new PeriodGenerator()
                    periodName = " за " + pg.getPeriodName(stTmp.get(0).getDate("dbeg"), stTmp.get(0).getDate("dend"), stTmp.get(0).getLong("periodType"), 3)
                }
                Store stProp = loadSqlMeta("""
                    select name from Prop where id=${stTmp.get(0).getLong("prop")}
                """, "")
                throw new XError("Имеется значения свойства [" + stProp.get(0).getString("name") + "] отношения [" + stTmp.get(0).getString("nm1") + "]" + periodName)
            }
*/

            //2 отношение является значением объекта/отношения?
            Store stTmp = mdb.loadQuery("""
                select  
                    rov.name nm1, d.prop, d.periodType, v.dbeg, v.dend, d.isObj,
                    case when d.isObj = 1 then ov1.name when d.isObj = 0 then rv1.name end as nm2
                from DataProp d
                    inner join DataPropVal v on d.id=v.dataprop
                    left join ObjVer ov1 on d.isObj=1 and d.objorrelobj=ov1.ownerver and ov1.lastver=1
                    left join RelObjVer rv1 on d.isObj=0 and d.objorrelobj=rv1.ownerver and rv1.lastver=1
                    left join RelObjVer rov on rov.ownerver=${id} and rov.lastver=1
                where v.relobj=${id}
            """)
            if (stTmp.size() > 0) {
                String nm = "Отношение [" + stTmp.get(0).getString("nm1") + "]"
                String objOrRelObj = stTmp.get(0).getInt("isObj") == 1 ? "объекта" : "отношения"
                String periodName = " за [" + stTmp.get(0).getString("dbeg") + " - " + stTmp.get(0).getString("dend") + "]"
                if (stTmp.get(0).getLong("periodType") > 0) {
                    PeriodGenerator pg = new PeriodGenerator()
                    periodName = " за " + pg.getPeriodName(stTmp.get(0).getDate("dbeg"), stTmp.get(0).getDate("dend"), stTmp.get(0).getLong("periodType"), 3)
                }
                Store stProp = loadSqlMeta("""
                    select name from Prop where id=${stTmp.get(0).getLong("prop")}
                """, "")
                throw new XError(nm + " является значением свойства [" + stProp.get(0).getString("name") + "] " + objOrRelObj + " [" + stTmp.get(0).getString("nm2") + "]" + periodName)
            }
        }
    }

    //---------------- 6 Fishing ----------------//

    @DaoMethod
    Store loadFishGearForSelect(String codTypOrProp) {
        return loadObjForSelect(codTypOrProp, "nsidata")
    }

    @DaoMethod
    Store loadFishLocationForSelect(long resoirvoir) {
        Map<String, Long> mapProp = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "Prop_ReservoirShore", "")
        Map<String, Long> mapCls = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", "Cls_Station", "")

        Store stObj = mdb.loadQuery("""
            select o.id, o.cls, ov.name, null as pv
            from Obj o
                join ObjVer ov on o.id=ov.ownerVer and ov.lastVer=1
                join DataProp d on d.isobj=1 and d.objorrelobj =o.id and d.prop=${mapProp.get("Prop_ReservoirShore")}
                join DataPropVal v on d.id=v.dataprop 
            where o.cls=${mapCls.get("Cls_Station")} and v.obj=${resoirvoir}
        """)

        Store stProp = apiMeta().get(ApiMeta).loadSql("""
            select cls, id as propval
            from PropVal
            where prop=${mapProp.get("Prop_ReservoirShore")} and cls is not null
        """, "")
        StoreIndex indProp = stProp.getIndex("cls")


        for (StoreRecord r in stObj) {
            StoreRecord rec = indProp.get(r.getLong("cls"))
            if (rec != null) {
                r.set("pv", rec.getLong("propval"))
            }
        }

        return stObj

    }

    @DaoMethod
    Store loadFishManagerForSelect(String codTypOrProp) {
        return loadObjForSelect(codTypOrProp, "nsidata")
    }

    @DaoMethod
    Store loadFishParticipantsForSelect(String codProp) {
        return loadObjForSelectMulti(codProp, "nsidata")
    }

    @DaoMethod
    Store loadFishing(long obj, String reservoirs, String dbeg, String dend) {
        String whe = "id=${obj}"
        String wheReservoirs = "0=0"

        if (obj == 0) {
            Set<Object> setCls = apiMeta().get(ApiMeta).setIdsOfCls("Typ_FishCatch")
            if (setCls.isEmpty()) setCls.add(0L)
            whe = "cls in (${setCls.join(",")})"
            wheReservoirs = "v6.obj in (${reservoirs}) and v1.dateTimeVal between '${dbeg}' and '${dend}'"
        }

        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "", "Prop_%")
        Store st = mdb.createStore("Obj.Fishing")
        mdb.loadQuery(st, """
            with ob as (
            select
                id, cls from Obj               
                where ${whe}
            )
            select ob.id as obj, ob.cls, null as nameCls,
                v1.id as idStartDate, v1.dateTimeVal::date as StartDate,
                v2.id as idFishLocation, v2.propVal as pvFishLocation, v2.obj as objFishLocation, ov22.name as nameFishLocation,
                v3.id as idAreaOfTon, v3.numberVal as AreaOfTon,
                v4.id as idFishGear, v4.propVal as pvFishGear, v4.obj as objFishGear,
                v5.id as idFishManager, v5.propVal as pvFishManager, v5.obj as objFishManager,
                t1.lstFishParticipants, 
                v6.id as idReservoirShore, v6.obj as objReservoirShore, v6.propval as pvReservoirShore, ov6.name as nameReservoirShore
            from ob
                join DataProp d1 on d1.isObj=1 and d1.objorrelobj=ob.id and d1.prop=:Prop_StartDate
                join DataPropVal v1 on d1.id=v1.dataprop 
                join DataProp d2 on d2.isObj=1 and d2.objorrelobj=ob.id and d2.prop=:Prop_FishLocation
                join DataPropVal v2 on d2.id=v2.dataprop
                left join ObjVer ov22 on v2.obj=ov22.ownerVer and ov22.lastVer=1
                join DataProp d3 on d3.isObj=1 and d3.objorrelobj=ob.id and d3.prop=:Prop_AreaOfTon
                join DataPropVal v3 on d3.id=v3.dataprop
                join DataProp d4 on d4.isObj=1 and d4.objorrelobj=ob.id and d4.prop=:Prop_FishGear
                join DataPropVal v4 on d4.id=v4.dataprop
                join DataProp d5 on d5.isObj=1 and d5.objorrelobj=ob.id and d5.prop=:Prop_FishManager
                join DataPropVal v5 on d5.id=v5.dataprop
                left join (
                    select d6.objorrelobj, d6.prop,
                    STRING_AGG (cast(v6.id||'_'||v6.obj||'_'||v6.propval as varchar(2000)), ',' order by v6.id) as lstFishParticipants
                    from ob
                        left join DataProp d6  on d6.isobj=1 and d6.objorrelobj=ob.id and d6.prop=:Prop_FishParticipants
                        left join DataPropVal v6 on d6.id=v6.dataprop
                    where 0=0
                    group by d6.objorrelobj, d6.prop
                    ) t1 on t1.objorrelobj=ob.id and t1.prop=:Prop_FishParticipants
                left join DataProp d6 on d6.isObj=1 and d6.objorrelobj=ob.id and d6.prop=:Prop_ReservoirShore
                left join DataPropVal v6 on d6.id=v6.dataprop
                left join ObjVer ov6 on v6.obj=ov6.ownerVer and ov6.lastVer=1
            where ${wheReservoirs}
            order by v1.dateTimeVal::date
        """, map)

        Set<Object> idsCls = st.getUniqueValues("cls")
        Store stCls = apiMeta().get(ApiMeta).loadSql("""
            select c.id, name from Cls c, ClsVer v where c.id=v.ownerVer and v.lastVer=1 and c.id in (0${idsCls.join(",")})
        """, "")
        StoreIndex indCls = stCls.getIndex("id")

        for (StoreRecord r in st) {
            List<String> objFishParticipants = new ArrayList<>()
            String lstParticipants = r.getString("lstFishParticipants")
            String[] arr0 = lstParticipants.split(",")
            List<Object> idsObj = new ArrayList<>()
            for (String it in arr0) {
                if (it.isEmpty()) continue
                String[] arr1 = it.split("_")
                objFishParticipants.add(arr1[1] + "_" + arr1[2])
                idsObj.add(arr1[1])
            }
            r.set("objFishParticipants", objFishParticipants.join(","))
            Store stObj = loadSqlService("""
                select v.name from Obj o, ObjVer v where o.id=v.ownerVer and v.lastVer=1 and o.id in (0${idsObj.join(",")})
            """, "", "nsidata")
            r.set("nameFishParticipants", stObj.getUniqueValues("name").join("; "))
            //
            StoreRecord rec = indCls.get(r.getLong("cls"))
            if (rec != null)
                r.set("nameCls", rec.getString("name"))
        }

        //println(whe)
        //mdb.outTable(st)
        return st

    }

    @DaoMethod
    String getReservoirName(String ids) {
        Store st = mdb.loadQuery("""
            select v.name
            from Obj o, ObjVer v
            where o.id=v.ownerVer and v.lastVer=1 and o.id in (${ids})
        """)
        return st.getUniqueValues("name").join("; ")
    }

    @DaoMethod
    Store saveFishingPropertiesRef(Map<String, Object> params) {
        VariantMap pms = new VariantMap(params)
        long own
        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        Map<String, Object> par = new HashMap<>(pms)
        //
        Store stTmp = loadSqlMeta("""
            select name from ClsVer where ownerVer=${pms.getLong("cls")} and lastVer=1
        """, "")
        String nm = stTmp.get(0).getString("name")
        stTmp = mdb.loadQuery("""
            select name from ObjVer where ownerVer=${pms.getLong("objFishLocation")} and lastVer=1
        """)
        nm = nm + "_" + pms.getString("StartDate") + "_" + stTmp.get(0).getString("name")
        //
        par.put("name", nm)
        par.put("fullName", nm)
        if (pms.getString("mode").equalsIgnoreCase("ins")) {
            par.put("cls", params.get("cls"))
            own = eu.insertEntity(par)
            pms.put("own", own)
            //
            //Prop_StartDate
            if (pms.containsKey("StartDate"))
                fillProperties(true, "Prop_StartDate", pms)
            else
                throw new XError("Не указан [StartDate]")

            //Prop_ReservoirShore
            if (pms.containsKey("objReservoirShore"))
                fillProperties(true, "Prop_ReservoirShore", pms)
            else
                throw new XError("Не указан [objReservoirShore]")

            //Prop_FishLocation
            if (pms.containsKey("objFishLocation"))
                fillProperties(true, "Prop_FishLocation", pms)
            else
                throw new XError("Не указан [objFishLocation]")
            //Prop_AreaOfTon
            if (pms.containsKey("AreaOfTon"))
                fillProperties(true, "Prop_AreaOfTon", pms)
            else
                throw new XError("Не указан [AreaOfTon]")
            //Prop_FishGear
            if (pms.containsKey("objFishGear"))
                fillProperties(true, "Prop_FishGear", pms)
            else
                throw new XError("Не указан [objFishGear]")
            //Prop_FishManager
            if (pms.containsKey("objFishManager"))
                fillProperties(true, "Prop_FishManager", pms)
            else
                throw new XError("Не указан [objFishManager]")
            //Prop_FishParticipants
            List<String> lstFishParticipants = UtCnv.toList(pms.get("FishParticipants"))
            for (String it in lstFishParticipants) {
                String[] arr = it.split("_")
                pms.put("objFishParticipants", UtCnv.toLong(arr[0]))
                pms.put("pvFishParticipants", UtCnv.toLong(arr[1]))
                fillProperties(true, "Prop_FishParticipants", pms)
            }
        } else {
            own = pms.getLong("obj")
            par.put("id", own)
            eu.updateEntity(par)
            //
            pms.put("own", own)
            //Prop_StartDate
            if (pms.containsKey("StartDate") && pms.getLong("idStartDate") > 0)
                updateProperties("Prop_StartDate", pms)

            //Prop_ReservoirShore
            if (pms.getLong("idReservoirShore") > 0)
                updateProperties("Prop_ReservoirShore", pms)

            //Prop_FishLocation
            if (pms.getLong("idFishLocation") > 0)
                updateProperties("Prop_FishLocation", pms)

            //Prop_AreaOfTon
            if (pms.containsKey("AreaOfTon") && pms.getLong("idAreaOfTon") > 0)
                updateProperties("Prop_AreaOfTon", pms)

            //Prop_FishGear
            if (pms.containsKey("objFishGear") && pms.getLong("idFishGear") > 0)
                updateProperties("Prop_FishGear", pms)

            //Prop_FishManager
            if (pms.containsKey("objFishManager") && pms.getLong("idFishManager") > 0)
                updateProperties("Prop_FishManager", pms)

            //Prop_FishParticipants
            String[] oldIds = UtCnv.toString(pms.get("lstFishParticipants")).split(",")
            List<String> FishParticipants = UtCnv.toList(pms.get("FishParticipants"))

            for (String e in oldIds) {
                if (!FishParticipants.contains(e)) {
                    long idDel = UtCnv.toLong(e.split("_")[0])
                    mdb.execQueryNative("""
                                delete from DataPropVal
                                where id=${idDel};
                                delete from DataProp where id in (
                                    select id from dataprop
                                    except
                                    select dataProp as id from DataPropVal
                                );
                            """)
                }
            }
            for (String it in FishParticipants) {
                if (it.split("_").length == 2) {   //ins
                    pms.put("objFishParticipants", UtCnv.toLong(it.split("_")[0]))
                    pms.put("pvFishParticipants", UtCnv.toLong(it.split("_")[1]))
                    fillProperties(true, "Prop_FishParticipants", pms)
                }
            }
        }
        return loadFishing(own, "", "", "")

    }

    @DaoMethod
    void deleteFishing(long id) {
        deleteOwnerWithProperties(id, 1)
    }


    private void fromDay2YearForNumberFishCaught(long reservoir) {
        Map<String, Long> mapProp = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "Prop_ReservoirShore", "")
        Set<Object> setCls = apiMeta().get(ApiMeta).setIdsOfCls("Typ_FishCatch")
        String codProp = "Prop_NumberFishCaught"   //pms.getString("cod")
        long ptFrom = 71L
        long ptTo = 11L
        //
        Store stProp = loadSqlMeta("""
            select id, meter from Prop where cod='${codProp}'
        """, "")
        //
        long meter = stProp.get(0).getLong("meter")

        Store stProp1Lev = loadSqlMeta("""
            with mrfv as (
            select meterrate,
                STRING_AGG (cast(factorval as varchar(200)), ',') as fvs,
                ARRAY_LENGTH(STRING_TO_ARRAY(STRING_AGG (cast(factorval as varchar(200)), ','), ','), 1) sz
            from meterratefv
            group by meterrate
            )
            select id, fvs, null as idval, null as numberval   
            from Prop p, mrfv
            where p.meter=${meter} and p.meterrate=mrfv.meterrate and mrfv.sz=1
        """, "")
        Set<Object> idsPropsAll = stProp1Lev.getUniqueValues("id")
        //
        //mdb.outTable(stProp1Lev)
        //
        String sqlVal = """
            select d.prop, SUM(v.numberval) as numberval, date_part('year', v.dbeg) as year  
            from DataProp d, DataPropVal v
            where d.id=v.dataProp and d.isObj=1 and d.objorrelobj in (
                select d.objorrelobj as own 
                from DataProp d, DataPropVal v
                where d.id=v.dataprop and d.prop=${mapProp.get("Prop_ReservoirShore")} and v.obj=${reservoir}
                and d.objorrelobj in (
                    select id from obj where cls in (0${setCls.join(",")})
                    )
            ) and d.prop in (0${idsPropsAll.join(",")}) and d.periodType=${ptFrom}
            group by d.prop, year
        """
        Store stVal = mdb.loadQuery(sqlVal)
        //
        //mdb.outTable(stVal)
        //
        Map<String, Object> rec = new HashMap()
        rec.put("obj", reservoir)

        for (StoreRecord r in stVal) {
            rec.put("prop", r.getLong("prop"))
            rec.put("numberval", r.getDouble("numberval"))
            rec.put("year", r.getLong("year"))
            saveMeterFishing(rec)
        }
    }

    private void fromDay2YearForNumberFishCaughtFilial(long filial) {
        //Prop_Branch
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "Prop_Branch", "")
        Store st = mdb.loadQuery("""
            select o.id as reservoir
            from Obj o
                join DataProp d1 on d1.isobj=1 and d1.objorrelobj=o.id and d1.prop=${map.get("Prop_Branch")}
                join DataPropVal v1 on d1.id=v1.dataprop and v1.obj=${filial}
        """)

        for (StoreRecord r in st) {
            fromDay2YearForNumberFishCaught(r.getLong("reservoir"))
        }

    }

    private void fromDay2YearForNumberFishCaught2(Map<String, Object> params) {
        Map<String, Long> mapProp = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "Prop_ReservoirShore", "")
        Set<Object> setObj = apiMeta().get(ApiMeta).setIdsOfCls("Typ_FishCatch")
        //Map<String, Object> res = new HashMap<>()
        VariantMap pms = new VariantMap(params)
        long own = pms.getLong("obj")       // Reservoir
        String codProp = "Prop_NumberFishCaught"   //pms.getString("cod")
        boolean dependperiod = true     //pms.getBoolean("dependperiod")
        String dte = pms.getString("dte")
        long periodType = pms.getLong("periodType")
        //
        Store stProp = loadSqlMeta("""
            select id, meter from Prop where cod='${codProp}'
        """, "")
        //
        long meter = stProp.get(0).getLong("meter")
        long prop = stProp.get(0).getLong("id")


        Store stProp1Lev = loadSqlMeta("""
            with mrfv as (
            select meterrate,
                STRING_AGG (cast(factorval as varchar(200)), ',') as fvs,
                ARRAY_LENGTH(STRING_TO_ARRAY(STRING_AGG (cast(factorval as varchar(200)), ','), ','), 1) sz
            from meterratefv
            group by meterrate
            )
            select id, fvs, null as idval, null as numberval   
            from Prop p, mrfv
            where p.meter=${meter} and p.meterrate=mrfv.meterrate and mrfv.sz=1
        """, "")
        Set<Object> idsPropsAll = stProp1Lev.getUniqueValues("id")
        //

//        System.out.println("prop = "+prop)
        mdb.outTable(stProp1Lev)

/*
        Store stOwn = mdb.loadQuery("""
            select id
            from obj
            where cls in (1035,1036,1037)
        """)
        Set<Object> idsOwn = stOwn.getUniqueValues("id")
*/

        // Далее проставляем данные
        String d1 = "1800-01-01"
        String d2 = "3333-12-01"
        if (dependperiod) {
            UtPeriod up = new UtPeriod()
            d1 = up.calcDbeg(XDate.create(dte), periodType, 0).toString(XDateTimeFormatter.ISO_DATE)
            d2 = up.calcDend(XDate.create(dte), periodType, 0).toString(XDateTimeFormatter.ISO_DATE)
        }


        String sql = """
            select d.prop, SUM(v.numberval) as numberval, date_part('year', v.dbeg) as year  
            from DataProp d, DataPropVal v
            where d.id=v.dataProp and d.isObj=1 and d.objorrelobj in (
                select d.objorrelobj as own 
                from DataProp d, DataPropVal v
                where d.id=v.dataprop and d.prop=${mapProp.get("Prop_ReservoirShore")} and v.obj=${own}
                and d.objorrelobj in (
                    select id from obj where cls in (0${setObj.join(",")})
                    )
            ) and d.prop in (0${idsPropsAll.join(",")}) and d.periodType=${periodType}
            group by d.prop, year
        """
        Store stVal = mdb.loadQuery(sql)
        //
//        mdb.outTable(stVal)
        //

        Map<String, Object> rec = new HashMap()
        rec.put("obj", own)
        rec.put("periodType", 11L)
        rec.put("dependperiod", true)

        for (StoreRecord r in stVal) {
            rec.put("prop", r.getLong("prop"))
            rec.put("numberval", r.getDouble("numberval"))
            rec.put("year", r.getLong("year"))
            saveMeterFishing(rec)
        }
    }

    private void saveMeterFishing(Map<String, Object> rec) {
        long obj = UtCnv.toLong(rec.get("obj"))
        long prop = UtCnv.toLong(rec.get("prop"))
        double value = UtCnv.toDouble(rec.get("numberval"))
        Long pt = 11L
        String dt = UtCnv.toString(rec.get("year")) + "-01-01"
        UtPeriod up = new UtPeriod()
        String dbeg = up.calcDbeg(XDate.create(dt), pt, 0).toString(XDateTimeFormatter.ISO_DATE)
        String dend = up.calcDend(XDate.create(dt), pt, 0).toString(XDateTimeFormatter.ISO_DATE)

        String sqlData = """
            select v.id
            from DataProp d, DataPropVal v
            where d.id=v.dataProp and d.prop=${prop} and d.periodType=${pt} and d.objorrelobj=${obj}
                and v.dbeg='${dbeg}' and v.dend='${dend}'
        """
        Store stData = mdb.loadQuery(sqlData)
        long idVal = 0
        if (stData.size() > 0)
            idVal = stData.get(0).getLong("id")


        if (idVal > 0) {
            String tm = XDateTime.create(new Date()).toString(XDateTimeFormatter.ISO_DATE_TIME)
            mdb.execQueryNative("""
                update DataPropVal set numberval=${value}, dbeg='${dbeg}', dend='${dend}', timestamp='${tm}'
                where id=${idVal}
            """)

        } else {
            StoreRecord recDP = mdb.createStoreRecord("DataProp")
            recDP.set("isObj", 1)
            recDP.set("objorrelobj", obj)
            recDP.set("prop", prop)
            recDP.set("periodType", pt)
            long idDP = mdb.insertRec("DataProp", recDP)
            StoreRecord recDPV = mdb.createStoreRecord("DataPropVal")
            recDPV.set("dataProp", idDP)
            recDPV.set("numberVal", value)
            long au = getUser()
            recDPV.set("authUser", au)
            recDPV.set("inputType", FD_InputType_consts.app)
            long idDPV = mdb.getNextId("DataPropVal")
            recDPV.set("id", idDPV)
            recDPV.set("ord", idDPV)
            recDPV.set("dbeg", dbeg)
            recDPV.set("dend", dend)
            recDPV.set("timeStamp", XDateTime.create(new Date()).toString(XDateTimeFormatter.ISO_DATE_TIME))
            mdb.insertRec("DataPropVal", recDPV, false)
        }
        //return idVal
    }


    @DaoMethod
    Store loadFishingMeters(long obj, long prop, String dte, long periodType, long reservoir) {
        if (obj == 0)
            return mdb.createStore()
        String props = "'Prop_NumberFishCaught','Prop_NumberEggs','Prop_FishArea','Prop_WorkDuration','Prop_NumberNet'"

        // Svae for Prop_NumberFishCaught PeriodType(day) => PeriodType(year)

        //fromDay2YearForNumberFishCaught(Map.of("obj", reservoir, "prop", prop, "dte", dte, "periodType", periodType) as Map<String, Object>)
        //
        return loadMetersOfOwnerWithPeriod(obj, 1, prop, dte, periodType, props)
    }


    @DaoMethod
    void deleteFishingMeters(long idDPV) {
        mdb.execQueryNative("""
            delete from DataPropVal
            where id=${idDPV};
            delete from DataProp where id in (
                select id from dataprop
                except
                select dataProp as id from DataPropVal
            );
        """)
    }

    @DaoMethod
    long saveFishingMeters(Map<String, Object> rec) {
        if (UtCnv.toString(rec.get("cod")).contains("Prop_NumberFishCaught"))
            rec.put("dependperiod", 1)
        else
            rec.put("dependperiod", 0)
        return saveMeter(rec)
    }

    private long saveMeter(Map<String, Object> rec) {
        long obj = UtCnv.toLong(rec.get("obj"))
        long prop = UtCnv.toLong(rec.get("prop"))
        long idVal = UtCnv.toLong(rec.get("idval"))
        double value = UtCnv.toDouble(rec.get("numberval"))
        boolean dependperiod = UtCnv.toBoolean(rec.get("dependperiod"))
        Long pt = null
        String dbeg = "1800-01-01"
        String dend = "3333-12-31"
        if (dependperiod) {
            pt = UtCnv.toLong(rec.get("periodType"))
            String dt = XDate.create(new Date()).toString(XDateTimeFormatter.ISO_DATE)
            if (rec.containsKey("year")) {
                dt = UtCnv.toString(rec.get("year")) + "-01-01"
            } else if (rec.containsKey("dte")) {
                dt = UtCnv.toString(UtCnv.toString(rec.get("dte")))
            } else {
                throw new XError("Не известно [year|dte]")
            }
            UtPeriod up = new UtPeriod()
            dbeg = up.calcDbeg(XDate.create(dt), pt, 0).toString(XDateTimeFormatter.ISO_DATE)
            dend = up.calcDend(XDate.create(dt), pt, 0).toString(XDateTimeFormatter.ISO_DATE)
        }

        if (idVal > 0) {
            String tm = XDateTime.create(new Date()).toString(XDateTimeFormatter.ISO_DATE_TIME)
            mdb.execQueryNative("""
                update DataPropVal set numberval=${value}, dbeg='${dbeg}', dend='${dend}', timestamp='${tm}'
                where id=${idVal}
            """)

        } else {
            StoreRecord recDP = mdb.createStoreRecord("DataProp")
            recDP.set("isObj", 1)
            recDP.set("objorrelobj", obj)
            recDP.set("prop", prop)
            if (dependperiod)
                recDP.set("periodType", pt)
            long idDP = mdb.insertRec("DataProp", recDP)
            StoreRecord recDPV = mdb.createStoreRecord("DataPropVal")
            recDPV.set("dataProp", idDP)
            recDPV.set("numberVal", value)
            long au = getUser()
            recDPV.set("authUser", au)
            recDPV.set("inputType", FD_InputType_consts.app)
            long idDPV = mdb.getNextId("DataPropVal")
            recDPV.set("id", idDPV)
            recDPV.set("ord", idDPV)
            recDPV.set("dbeg", dbeg)
            recDPV.set("dend", dend)
            recDPV.set("timeStamp", XDateTime.create(new Date()).toString(XDateTimeFormatter.ISO_DATE_TIME))
            idVal = mdb.insertRec("DataPropVal", recDPV, false)
        }
        return idVal
    }

    private Store loadMetersWithOutPeriod(long own, String props) {
        Set<Object> idsPropAll = new HashSet<>()
        for (String cod in props.split(",")) {
            Store stTmp = loadSqlMeta("""
                WITH RECURSIVE r AS (
                    SELECT id
                    FROM prop
                    WHERE cod='${cod}'    
                    UNION ALL    
                    SELECT c.id
                    FROM prop c
                    JOIN r ON c.parent = r.id
                )
                SELECT * FROM r;
            """, "")
            Set<Object> setIds = stTmp.getUniqueValues("id")
            idsPropAll.addAll(setIds)
        }
        //
        Store st = loadSqlMeta("""
            select id, parent, cod, name, null as idval, null as numberval
            from Prop 
            where id in (${idsPropAll.join(",")})
        """, "")
        // Value
        Store stVal = mdb.loadQuery("""
            select  d1.prop, v1.id as idval, v1.numberval
            from Obj o
                join DataProp d1 on d1.isObj=1 and d1.objOrRelObj=o.id and d1.periodtype is null
                    and d1.prop in (${idsPropAll.join(",")})
                join DataPropVal v1 on v1.dataprop=d1.id 
            where o.id=${own}
        """)

        StoreIndex indStVal = stVal.getIndex("prop")
        for (StoreRecord r in st) {
            StoreRecord rec = indStVal.get(r.getLong("id"))
            if (rec != null) {
                r.set("idval", rec.getLong("idval"))
                r.set("numberval", rec.getDouble("numberval"))
            }
        }
        return st
    }

    /*private Store loadMetersWithPeriod(long own, String props) {
        Map<String, Object> map = apiMeta().get(ApiMeta).getIdsFromCodsOfEntityAsMap("Prop", props)
        map.put("own", own)
        //year1 & year2
        Map<String, Long> mapY = getYears(own)
        long year1 = mapY.get("year1")
        long year2 = mapY.get("year2")
        //
        long count = UtCnv.toLong(year2) - UtCnv.toLong(year1)
        List<String> sel = new ArrayList<>();
        for (long i in 0..count) {
            String year = UtCnv.toString(year1 + i)
            sel.add("null as id" + year + ",  null  as v" + year)
        }
        //
        Set<Object> idsPropAll = new HashSet<>()
        for (String cod in props.split(",")) {
            Store stTmp = loadSqlMeta("""
                WITH RECURSIVE r AS (
                    SELECT id
                    FROM prop
                    WHERE cod='${cod}'
                    UNION ALL
                    SELECT c.id
                    FROM prop c
                    JOIN r ON c.parent = r.id
                )
                SELECT * FROM r;
            """, "")
            Set<Object> setIds = stTmp.getUniqueValues("id")
            idsPropAll.addAll(setIds)
        }

        Store st = loadSqlMeta("""
            select p.id, p.parent, p.name, ${sel.join(",")}
            from prop p
            where p.id in (${idsPropAll.join(",")})
        """, "")

        // sql for value
        String sqlVal = """
            select v1.id, v1.numberval, d1.prop || '_' || 'v'||date_part('year', v1.dbeg) as key
            from Obj o
                join DataProp d1 on d1.isObj=1 and d1.objOrRelObj=o.id and d1.prop in (${idsPropAll.join(",")}) and d1.periodType is not null
                join DataPropVal v1 on v1.dataprop=d1.id and v1.numberval is not null
            where o.id=${own}
        """
        Store stVal = mdb.loadQuery(sqlVal)
        StoreIndex indVal = stVal.getIndex("key")
        //mdb.outTable(stVal)
        for (StoreRecord r in st) {
            for (StoreField fld in r.fields) {
                if (fld.name.startsWith("v")) {
                    StoreRecord rec = indVal.get(r.getString("id") + "_" + fld.name)
                    if (rec != null) {
                        r.set("id" + fld.name.substring(1), rec.get("id"))
                        r.set(fld.name, rec.get("numberval"))
                    }
                }
            }
        }
        //mdb.outTable(st)
        return st
    }*/


//************************************************************************//


    private long getIdDataProp(Store stProp, boolean isObj, long own, long prop, long periodType) {
        StoreRecord recDP = mdb.createStoreRecord("DataProp")
        recDP.set("isObj", isObj)
        recDP.set("objOrRelObj", own)
        recDP.set("prop", prop)
        if (stProp.get(0).getLong("statusFactor") > 0) {
            long fv = apiMeta().get(ApiMeta).getDefaultStatus(prop)
            recDP.set("status", fv)
        }
        if (stProp.get(0).getLong("providerTyp") > 0) {
            //todo
            // provider
            //
        }
        if (stProp.get(0).getBoolean("dependPeriod")) {
            recDP.set("periodType", periodType)
        }
        long idDP = mdb.insertRec("DataProp", recDP, true)
        return idDP
    }

    @DaoMethod
    void fillProperties(boolean isObj, String cod, Map<String, Object> params) {
        long own = UtCnv.toLong(params.get("own"))
        long au = getUser()
        String keyValue = cod.split("_")[1]
        long objRef = UtCnv.toLong(params.get("obj" + keyValue))
        long propVal = UtCnv.toLong(params.get("pv" + keyValue))

        Store stProp = apiMeta().get(ApiMeta).getPropInfo(cod)
        //
        long prop = stProp.get(0).getLong("id")
        long propType = stProp.get(0).getLong("propType")
        long attribValType = stProp.get(0).getLong("attribValType")
        Integer digit = null
        double koef = stProp.get(0).getDouble("koef")
        if (koef == 0) koef = 1
        if (!stProp.get(0).isValueNull("digit"))
            digit = stProp.get(0).getInt("digit")

        long idDP
        StoreRecord recDP = mdb.createStoreRecord("DataProp")
        String whe = isObj ? "and isObj=1 " : "and isObj=0 "
        if (stProp.get(0).getLong("statusFactor") > 0) {
            long fv = apiMeta().get(ApiMeta).getDefaultStatus(prop)
            whe += "and status = ${fv} "
        } else {
            whe += "and status is null "
        }
        //todo if (stProp.get(0).getLong("providerTyp") > 0)
        whe += "and provider is null "
        if (stProp.get(0).getBoolean("dependPeriod")) {
            whe += "and periodType=${FD_PeriodType_consts.month} "
        } else {
            whe += "and periodType is null "
        }
        Store stDP = mdb.loadQuery("""
            select * from DataProp
            where objOrRelObj=${own} and prop=${prop} ${whe}
        """)
        if (stDP.size() > 0) {
            idDP = stDP.get(0).getLong("id")
            recDP.setValues(stDP.get(0))
        } else {
            recDP.set("isObj", isObj)
            recDP.set("objOrRelObj", own)
            recDP.set("prop", prop)
            if (stProp.get(0).getLong("statusFactor") > 0) {
                long fv = apiMeta().get(ApiMeta).getDefaultStatus(prop)
                recDP.set("status", fv)
            }
            if (stProp.get(0).getLong("providerTyp") > 0) {
                //todo
                // provider
                //
            }
            if (stProp.get(0).getBoolean("dependperiod")) {
                recDP.set("periodType", FD_PeriodType_consts.month)
            }
            idDP = mdb.insertRec("DataProp", recDP, true)
        }
        //
        StoreRecord recDPV = mdb.createStoreRecord("DataPropVal")
        recDPV.set("dataProp", idDP)
        // For Attrib
        if ([FD_AttribValType_consts.str].contains(attribValType)) {
            if (cod.equalsIgnoreCase("Prop_Coordinate") ||
                    cod.equalsIgnoreCase("Prop_FishSpawPeriod") ||
                    cod.equalsIgnoreCase("Prop_FishSpawFrequency")) {
                if (params.get(keyValue) != null || params.get(keyValue) != "") {
                    recDPV.set("strVal", UtCnv.toString(params.get(keyValue)))
                }
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }
        if ([FD_AttribValType_consts.dt].contains(attribValType)) {
            if (cod.equalsIgnoreCase("Prop_StartDate")) {
                if (params.get(keyValue) != null || params.get(keyValue) != "") {
                    recDPV.set("dateTimeVal", UtCnv.toString(params.get(keyValue)))
                }
            } else
                throw new XError("for dev: [${cod}] отсутствует в реализации")
        }
        if ([FD_AttribValType_consts.multistr].contains(attribValType)) {
            if (cod.equalsIgnoreCase("Prop_Description")) {
                if (params.get(keyValue) != null || params.get(keyValue) != "") {
                    recDPV.set("multiStrVal", UtCnv.toString(params.get(keyValue)))
                }
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }
        // For Typ
        if ([FD_PropType_consts.typ].contains(propType)) {
            if (cod.equalsIgnoreCase("Prop_KATO") ||
                    cod.equalsIgnoreCase("Prop_Branch") ||
                    cod.equalsIgnoreCase("Prop_FishLocation") ||
                    cod.equalsIgnoreCase("Prop_ReservoirShore") ||
                    cod.equalsIgnoreCase("Prop_FishGear") ||
                    cod.equalsIgnoreCase("Prop_FishManager") ||
                    cod.equalsIgnoreCase("Prop_FishParticipants")) {
                if (objRef > 0) {
                    recDPV.set("propVal", propVal)
                    recDPV.set("obj", objRef)
                }
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }
        // For FV
        if ([FD_PropType_consts.factor].contains(propType)) {
            if (cod.equalsIgnoreCase("Prop_ReservoirType") ||
                    cod.equalsIgnoreCase("Prop_ReservoirStatus") ||
                    cod.equalsIgnoreCase("Prop_FishFarmingType") ||
                    cod.equalsIgnoreCase("Prop_FishFamily") ||
                    cod.equalsIgnoreCase("Prop_FishTyp")) {
                if (propVal > 0) {
                    recDPV.set("propVal", propVal)
                }
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }
        // For Meter
        if ([FD_PropType_consts.meter, FD_PropType_consts.rate].contains(propType)) {
            if (cod.equalsIgnoreCase("Prop_AreaOfTon") ||
                    cod.equalsIgnoreCase("Prop_FishStartPuberty") ||
                    cod.equalsIgnoreCase("Prop_FishEndPuberty")) {
                if (params.get(keyValue) != null || params.get(keyValue) != "") {
                    double v = UtCnv.toDouble(params.get(keyValue))
                    v = v / koef
                    if (digit) v = v.round(digit)
                    recDPV.set("numberVal", v)
                }
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }
        //
        if (recDP.getLong("periodType") > 0) {
            if (!params.containsKey("dte"))
                params.put("dte", XDateTime.create(new Date()).toString(XDateTimeFormatter.ISO_DATE))
            tofi.api.mdl.utils.UtPeriod utPeriod = new tofi.api.mdl.utils.UtPeriod()
            XDate d1 = utPeriod.calcDbeg(UtCnv.toDate(params.get("dte")), recDP.getLong("periodType"), 0)
            XDate d2 = utPeriod.calcDend(UtCnv.toDate(params.get("dte")), recDP.getLong("periodType"), 0)
            recDPV.set("dbeg", d1.toString(XDateTimeFormatter.ISO_DATE))
            recDPV.set("dend", d2.toString(XDateTimeFormatter.ISO_DATE))
        } else {
            recDPV.set("dbeg", "1800-01-01")
            recDPV.set("dend", "3333-12-31")
        }

        recDPV.set("authUser", au)
        recDPV.set("inputType", FD_InputType_consts.app)
        long idDPV = mdb.getNextId("DataPropVal")
        recDPV.set("id", idDPV)
        recDPV.set("ord", idDPV)
        recDPV.set("timeStamp", XDateTime.create(new Date()).toString(XDateTimeFormatter.ISO_DATE_TIME))
        mdb.insertRec("DataPropVal", recDPV, false)
    }

    void updateProperties(String cod, Map<String, Object> params) {
        VariantMap mapProp = new VariantMap(params)
        long au = getUser()
        String keyValue = cod.split("_")[1]
        long idVal = mapProp.getLong("id" + keyValue)
        //
        StoreRecord recDPV = mdb.createStoreRecord("DataPropVal")
        mdb.loadQueryRecord(recDPV, "select * from DataPropVal where id=${idVal}")
        StoreRecord recDP = mdb.createStoreRecord("DataProp")
        mdb.loadQueryRecord(recDP, """
            select d.* from DataPropVal v, DataProp d where v.id=${idVal} and v.dataProp=d.id
        """)
        long idDP = recDP.getLong("id")
        //
        long objRef = mapProp.getLong("obj" + keyValue)
        long propVal = mapProp.getLong("pv" + keyValue)
        Store stProp = apiMeta().get(ApiMeta).getPropInfo(cod)
        //
        long propType = stProp.get(0).getLong("propType")
        long attribValType = stProp.get(0).getLong("attribValType")
        Integer digit = null
        double koef = stProp.get(0).getDouble("koef")
        if (koef == 0) koef = 1
        if (!stProp.get(0).isValueNull("digit"))
            digit = stProp.get(0).getInt("digit")

        def tmst = XDateTime.create(new Date()).toString(XDateTimeFormatter.ISO_DATE_TIME)
        def strValue = mapProp.getString(keyValue)
        recDPV.set("authUser", au)
        recDPV.set("timeStamp", tmst)
        // For Attrib (str)
        if ([FD_AttribValType_consts.str].contains(attribValType)) {
            if (cod.equalsIgnoreCase("Prop_Coordinate") ||
                    cod.equalsIgnoreCase("Prop_FishSpawPeriod") ||
                    cod.equalsIgnoreCase("Prop_FishSpawFrequency")) {
                if (!mapProp.keySet().contains(keyValue) || strValue.trim() == "") {
                    mdb.deleteRec("DataPropVal", idVal)
                    //
                    mdb.execQueryNative("""
                        delete from DataProp where id in (
                            select id from DataProp
                            except
                            select dataProp as id from DataPropVal
                        )
                    """)
                } else {
                    recDPV.set("strVal", strValue)
                    mdb.updateRec("DataPropVal", recDPV)
                }
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }
        // For Attrib (multistr)
        if ([FD_AttribValType_consts.multistr].contains(attribValType)) {
            if (cod.equalsIgnoreCase("Prop_Description")) {
                if (!mapProp.keySet().contains(keyValue) || strValue.trim() == "") {
                    mdb.deleteRec("DataPropVal", idVal)
                    //
                    mdb.execQueryNative("""
                        delete from DataProp where id in (
                            select id from DataProp
                            except
                            select dataProp as id from DataPropVal
                        )
                    """)
                } else {
                    recDPV.set("multiStrVal", strValue)
                    mdb.updateRec("DataPropVal", recDPV)
                }
            } else
                throw new XError("for dev: [${cod}] отсутствует в реализации")
        }
        // For Attrib (dt)
        if ([FD_AttribValType_consts.dt].contains(attribValType)) {
            if (cod.equalsIgnoreCase("Prop_StartDate")) {
                if (!mapProp.keySet().contains(keyValue) || strValue.trim() == "") {
                    mdb.deleteRec("DataPropVal", idVal)
                    //
                    mdb.execQueryNative("""
                        delete from DataProp where id in (
                            select id from DataProp
                            except
                            select dataProp as id from DataPropVal
                        )
                    """)
                } else {
                    recDPV.set("dateTimeVal", strValue)
                    mdb.updateRec("DataPropVal", recDPV)
                }
            } else
                throw new XError("for dev: [${cod}] отсутствует в реализации")
        }
        // For FV
        if ([FD_PropType_consts.factor].contains(propType)) {
            if (cod.equalsIgnoreCase("Prop_ReservoirType") ||
                    cod.equalsIgnoreCase("Prop_ReservoirStatus") ||
                    cod.equalsIgnoreCase("Prop_FishFarmingType") ||
                    cod.equalsIgnoreCase("Prop_FishFamily") ||
                    cod.equalsIgnoreCase("Prop_FishTyp")) {
                if (propVal > 0) {
                    recDPV.set("propVal", propVal)
                    mdb.updateRec("DataPropVal", recDPV)
                } else {
                    mdb.deleteRec("DataPropVal", idVal)
                    //
                    mdb.execQueryNative("""
                        delete from DataProp where id in (
                            select id from DataProp
                            except
                            select dataProp as id from DataPropVal
                        )
                    """)
                }
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }
        // For Measure
        if ([FD_PropType_consts.measure].contains(propType)) {
            if (cod.equalsIgnoreCase("Prop_Measure")) {
                if (propVal > 0) {
                    recDPV.set("propVal", propVal)
                    mdb.updateRec("DataPropVal", recDPV)
                    //
                } else {
                    mdb.deleteRec("DataPropVal", idVal)
                    //
                    mdb.execQueryNative("""
                        delete from DataProp where id in (
                            select id from DataProp
                            except
                            select dataProp as id from DataPropVal
                        )
                    """)
                }
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }
        // For Meter
        if ([FD_PropType_consts.meter, FD_PropType_consts.rate].contains(propType)) {
            if (cod.equalsIgnoreCase("Prop_AreaOfTon") ||
                    cod.equalsIgnoreCase("Prop_FishStartPuberty") ||
                    cod.equalsIgnoreCase("Prop_FishEndPuberty")) {
                if (mapProp.keySet().contains(keyValue) && mapProp[keyValue] != "") {
                    def v = mapProp.getDouble(keyValue)
                    v = v / koef
                    if (digit) v = v.round(digit)
                    recDPV.set("numberVal", v)
                    mdb.updateRec("DataPropVal", recDPV)
                    //
                } else {
                    mdb.deleteRec("DataPropVal", idVal)
                    //
                    mdb.execQueryNative("""
                        delete from DataProp where id in (
                            select id from DataProp
                            except
                            select dataProp as id from DataPropVal
                        )
                    """)
                }
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }
        // For Typ
        if ([FD_PropType_consts.typ].contains(propType)) {
            if (cod.equalsIgnoreCase("Prop_KATO") ||
                    cod.equalsIgnoreCase("Prop_Branch") ||
                    cod.equalsIgnoreCase("Prop_FishLocation") ||
                    cod.equalsIgnoreCase("Prop_ReservoirShore") ||
                    cod.equalsIgnoreCase("Prop_FishGear") ||
                    cod.equalsIgnoreCase("Prop_FishManager") ||
                    cod.equalsIgnoreCase("Prop_FishParticipants")) {
                if (objRef > 0) {
                    recDPV.set("propVal", propVal)
                    recDPV.set("obj", objRef)
                    mdb.updateRec("DataPropVal", recDPV)
                } else {
                    mdb.deleteRec("DataPropVal", idVal)
                    //
                    mdb.execQueryNative("""
                        delete from DataProp where id in (
                            select id from DataProp
                            except
                            select dataProp as id from DataPropVal
                        )
                    """)
                }
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }
    }

    private void execSql(String sql, String model) {
        if (model.equalsIgnoreCase("nsidata"))
            apiNSIData().get(ApiNSIData).execSql(sql)
        else if (model.equalsIgnoreCase("monitoringdata"))
            apiMonitoringData().get(ApiMonitoringData).execSql(sql)
        else
            throw new XError("Unknown model [${model}]")
    }

    private Store loadSqlService(String sql, String domain, String model) {
        if (model.equalsIgnoreCase("nsidata"))
            return apiNSIData().get(ApiNSIData).loadSql(sql, domain)
        else if (model.equalsIgnoreCase("monitoringdata"))
            return apiMonitoringData().get(ApiMonitoringData).loadSql(sql, domain)
        else
            throw new XError("Unknown model [${model}]")
    }

    @DaoMethod
    String getPathFile(long id) {

        DbFileStorageService dfsrv = apiMeta().get(ApiMeta).getDbFileStorageService()
        dfsrv.setModelName(UtCnv.toString("monitoringdata"))
        DbFileStorageItem dfsi = dfsrv.getFile(id)

        String pdf_dir = getApp().getAppdir() + File.separator + "frontend" + File.separator + "pdf"
        //String pdf_dir = getApp().getAppdir() + File.separator + "pdf"
        File fle = dfsi.getFile()

        File file = new File(UtFile.join(pdf_dir, dfsi.originalFilename))
        if (UtFile.exists(file))
            file.delete()

        try (InputStream ins = new FileInputStream(fle)) {
            Files.copy(ins, Paths.get(pdf_dir, dfsi.originalFilename))
        } catch (Exception e) {
            e.printStackTrace()
        }
        String pathFile = '/pdf/' + dfsi.originalFilename

        return pathFile

    }

    private Store loadSqlMeta(String sql, String domain) {
        return apiMeta().get(ApiMeta).loadSql(sql, domain)
    }

    private long getUser() throws Exception {
        AuthService authSvc = mdb.getApp().bean(AuthService.class)
        long au = authSvc.getCurrentUser().getAttrs().getLong("id")
        if (au == 0)
            throw new XError("notLoginned")
        return au
    }


}
