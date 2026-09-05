package fish.calc.dao

import groovy.transform.CompileStatic
import jandcode.commons.UtCnv
import jandcode.commons.datetime.XDate
import jandcode.commons.datetime.XDateTime
import jandcode.commons.datetime.XDateTimeFormatter
import jandcode.commons.error.XError
import jandcode.commons.variant.VariantMap
import jandcode.core.auth.AuthService
import jandcode.core.auth.AuthUser
import jandcode.core.dao.DaoMethod
import jandcode.core.dbm.mdb.BaseMdbUtils
import jandcode.core.store.Store
import jandcode.core.store.StoreField
import jandcode.core.store.StoreIndex
import jandcode.core.store.StoreRecord
import tofi.api.dta.ApiMonitoringData
import tofi.api.dta.ApiNSIData
import tofi.api.dta.model.utils.EntityMdbUtils
import tofi.api.mdl.ApiMeta
import tofi.api.mdl.model.consts.FD_AttribValType_consts
import tofi.api.mdl.model.consts.FD_InputType_consts
import tofi.api.mdl.model.consts.FD_PeriodType_consts
import tofi.api.mdl.model.consts.FD_PropType_consts
import tofi.api.mdl.utils.UtPeriod
import tofi.apinator.ApinatorApi
import tofi.apinator.ApinatorService

@CompileStatic
class DataDao extends BaseMdbUtils {

    ApinatorApi apiMeta() { return app.bean(ApinatorService).getApi("meta") }

    ApinatorApi apiNSIData() { return app.bean(ApinatorService).getApi("nsidata") }

    ApinatorApi apiMonitoringData() { return app.bean(ApinatorService).getApi("monitoringdata") }
    //-----------------------------------------------------------------------------------------------//


    @DaoMethod
    long getCls(String codCls) {
        Store st = apiMeta().get(ApiMeta).loadSql("""
            select id from Cls where cod like '${codCls}'
        """, "")
        if (st.size() == 0)
            throw new XError("Не найден код класса [Cls_CalcDeterm/Cls_CalcBayes]")
        return st.get(0).getLong("id")
    }

    @DaoMethod
    Store loadCalc(long id) {
        return mdb.loadQuery("""
            select o.id, 
                v.objParent as parent,
                v.name, o.cls as cls 
            from Obj o, ObjVer v
            where o.id=v.ownerVer and v.lastVer=1 and o.cls=${id}
        """)
    }

    @DaoMethod
    void insertCalc(Map<String, Object> rec) throws Exception {
        // Права доступа
        //checkTarget("calc")
        //
        // Check Props if child
        long objParent = UtCnv.toLong(rec.get("parent"))
        if (objParent > 0) {
            if (!hasProps(objParent)) {
                throw new XError("Для данного расчета не указаны необходимые свойства")
            }
        }
        //
        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        long obj = eu.insertEntity(rec)
        // Create prop CalcCreatDate
        Map<String, Object> map = new HashMap<>()
        map.put("own", obj)
        map.put("CalcCreatDate", XDate.create(new Date()).toString(XDateTimeFormatter.ISO_DATE))
        fillProperties(true, "Prop_CalcCreatDate", map)
        // Наследуем свойства if child objParent => obj
        if (objParent > 0) {
            parent2childProps(objParent, obj)
        }
    }

    private boolean hasProps(long obj) {
        String props = "'Prop_ReservoirShore','Prop_CalcStartYear','Prop_CalcEndYear','Prop_CalcFishSpec','Prop_CalcStatus','Prop_CalcDescription'"
        Map<String, Object> map = apiMeta().get(ApiMeta).getIdsFromCodsOfEntity("Prop", props)
        map.put("obj", obj)

        Store st = mdb.loadQuery("""
            select o.id 
            from Obj o
                join ObjVer v on o.id=v.ownerVer and v.lastVer=1
                join DataProp d1 on d1.isObj=1 and d1.objOrRelObj=o.id and d1.prop=:Prop_CalcStartYear
                join DataPropVal v1 on v1.dataprop=d1.id
                join DataProp d2 on d2.isObj=1 and d2.objOrRelObj=o.id and d2.prop=:Prop_CalcEndYear
                join DataPropVal v2 on v2.dataprop=d2.id
                join DataProp d5 on d5.isObj=1 and d5.objOrRelObj=o.id and d5.prop=:Prop_CalcFishSpec
                join DataPropVal v5 on v5.dataprop=d5.id
                join DataProp d6 on d6.isObj=1 and d6.objOrRelObj=o.id and d6.prop=:Prop_CalcStatus
                join DataPropVal v6 on v6.dataprop=d6.id
                join DataProp d8 on d8.isObj=1 and d8.objOrRelObj=o.id and d8.prop=:Prop_ReservoirShore
                join DataPropVal v8 on v8.dataprop=d8.id
                join DataProp d9 on d9.isObj=1 and d9.objOrRelObj=o.id and d9.prop=:Prop_CalcDescription
                join DataPropVal v9 on v9.dataprop=d9.id                
            where o.id=:obj
        """, map)
        return st.size() > 0
    }

    private void parent2childProps(long parent, long id) {
        String props = "'Prop_ReservoirShore','Prop_CalcStartYear','Prop_CalcEndYear','Prop_CalcFishSpec','Prop_CalcStatus','Prop_CalcDescription'"
        Map<String, Object> map = apiMeta().get(ApiMeta).getIdsFromCodsOfEntity("Prop", props)
        map.put("obj", parent)
        Store stPrt = mdb.createStore("Calc.main.props.copy")
        mdb.loadQuery(stPrt, """
            select
                v1.strVal as CalcStartYear,
                v2.strVal as CalcEndYear,
                v5.propVal as pvCalcFishSpec,
                v6.propVal as pvCalcStatus,
                v8.propVal as pvReservoirShore, v8.obj as objReservoirShore,
                v9.multiStrVal as CalcDescription
            from Obj o
                join ObjVer v on o.id=v.ownerVer and v.lastVer=1
                join DataProp d1 on d1.isObj=1 and d1.objOrRelObj=o.id and d1.prop=:Prop_CalcStartYear
                join DataPropVal v1 on v1.dataprop=d1.id
                join DataProp d2 on d2.isObj=1 and d2.objOrRelObj=o.id and d2.prop=:Prop_CalcEndYear
                join DataPropVal v2 on v2.dataprop=d2.id
                join DataProp d5 on d5.isObj=1 and d5.objOrRelObj=o.id and d5.prop=:Prop_CalcFishSpec
                join DataPropVal v5 on v5.dataprop=d5.id
                join DataProp d6 on d6.isObj=1 and d6.objOrRelObj=o.id and d6.prop=:Prop_CalcStatus
                join DataPropVal v6 on v6.dataprop=d6.id
                join DataProp d8 on d8.isObj=1 and d8.objOrRelObj=o.id and d8.prop=:Prop_ReservoirShore
                join DataPropVal v8 on v8.dataprop=d8.id
                join DataProp d9 on d9.isObj=1 and d9.objOrRelObj=o.id and d9.prop=:Prop_CalcDescription
                join DataPropVal v9 on v9.dataprop=d9.id
            where o.id=:obj
        """, map)

        //mdb.outTable(stPrt)
        if (stPrt.size() == 1) {
            Map<String, Object> mapProp = stPrt.get(0).getValues()
            mapProp.put("own", id)
            String props_ = props.replaceAll("'", "")
            for (final def prop in props_.split(",")) {
                fillProperties(true, prop, mapProp)
            }
        }
    }

    @DaoMethod
    void updateCalc(Map<String, Object> rec) {
        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        UtCnv.toLong(rec.get("id"))
        eu.updateEntity(rec)
    }

    @DaoMethod
    void deleteCalc(long id) {
        deleteOwnerWithProperties(id, 1)
    }

    private void deleteOwnerWithProperties(long id, int isObj) {
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

    //*************************** Props Main *****************************//
    @DaoMethod
    Store loadDesc(long obj) {
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "Prop_CalcDescription", "")
        Store st = mdb.createStore("Calc.desc")
        mdb.loadQuery(st, """
            select o.id as obj, v.objParent as parent, o.cls, 
                v1.id as idCalcDescription, v1.multiStrVal as CalcDescription
            from Obj o
                join ObjVer v on o.id=v.ownerVer and v.lastVer=1
                join DataProp d1 on d1.isObj=1 and d1.objOrRelObj=o.id and d1.prop=${map.get("Prop_CalcDescription")}
                join DataPropVal v1 on v1.dataprop=d1.id
            where o.id=${obj}
        """)
        return st
    }

    @DaoMethod
    void saveDesc(Map<String, Object> rec) {
        long idVal = UtCnv.toLong(rec.get("idCalcDescription"))
        if (idVal == 0)
            fillProperties(true, "Prop_CalcDescription", rec)
        else
            updateProperties("Prop_CalcDescription", rec)
    }

    @DaoMethod
    Store loadMainProps(long obj) {
        Map<String, Object> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "Prop_ReservoirShore", "") as Map<String, Object>
        Map<String, Object> mapProp = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "", "Prop_Calc%") as Map<String, Object>
        mapProp.putAll(map)
        Store st = mdb.createStore("Calc.main.props")
        mdb.loadQuery(st, """
            select o.id as own, v.objParent as parent, o.cls,
                v1.id as idCalcStartYear, v1.strVal as CalcStartYear,
                v2.id as idCalcEndYear, v2.strVal as CalcEndYear,    
                v3.id as idCalcCreatDate, v3.dateTimeVal as CalcCreatDate,
                v4.id as idCalcLastDate, v4.dateTimeVal as CalcLastDate,    
                v5.id as idCalcFishSpec, v5.propVal as pvCalcFishSpec, null as fvCalcFishSpec,
                v6.id as idCalcStatus, v6.propVal as pvCalcStatus, null as fvCalcStatus,
                v7.id as idCalcUser, v7.propVal as pvCalcUser, v7.obj as objCalcUser,
                v8.id as idReservoirShore, v8.propVal as pvReservoirShore, v8.obj as objReservoirShore
            from Obj o
                join ObjVer v on o.id=v.ownerVer and v.lastVer=1
                join DataProp d1 on d1.isObj=1 and d1.objOrRelObj=o.id and d1.prop=:Prop_CalcStartYear
                join DataPropVal v1 on v1.dataprop=d1.id
                join DataProp d2 on d2.isObj=1 and d2.objOrRelObj=o.id and d2.prop=:Prop_CalcEndYear
                join DataPropVal v2 on v2.dataprop=d2.id
                join DataProp d3 on d3.isObj=1 and d3.objOrRelObj=o.id and d3.prop=:Prop_CalcCreatDate
                join DataPropVal v3 on v3.dataprop=d3.id
                left join DataProp d4 on d4.isObj=1 and d4.objOrRelObj=o.id and d4.prop=:Prop_CalcLastDate
                left join DataPropVal v4 on v4.dataprop=d4.id
                join DataProp d5 on d5.isObj=1 and d5.objOrRelObj=o.id and d5.prop=:Prop_CalcFishSpec
                join DataPropVal v5 on v5.dataprop=d5.id
                join DataProp d6 on d6.isObj=1 and d6.objOrRelObj=o.id and d6.prop=:Prop_CalcStatus
                join DataPropVal v6 on v6.dataprop=d6.id
                left join DataProp d7 on d7.isObj=1 and d7.objOrRelObj=o.id and d7.prop=:Prop_CalcUser
                left join DataPropVal v7 on v7.dataprop=d7.id
                join DataProp d8 on d8.isObj=1 and d8.objOrRelObj=o.id and d8.prop=:Prop_ReservoirShore
                join DataPropVal v8 on v8.dataprop=d8.id    
            where o.id=${obj}
        """, mapProp)

        Store stFV = apiMeta().get(ApiMeta).storeFVfromPropVal()
        StoreIndex indFV = stFV.getIndex("propval")
        for (StoreRecord r in st) {
            StoreRecord rec = indFV.get(r.getLong("pvCalcFishSpec"))
            if (rec != null)
                r.set("fvCalcFishSpec", rec.getLong("factorval"))
            //
            rec = indFV.get(r.getLong("pvCalcStatus"))
            if (rec != null)
                r.set("fvCalcStatus", rec.getLong("factorval"))

        }
        return st
    }

    @DaoMethod
    Store newRecMainProps(long obj) {
        Store st = mdb.createStore("Calc.main.props")
        StoreRecord rec = mdb.createStoreRecord("Calc.main.props")
        rec.set("own", obj)
        AuthService authService = getModel().getApp().bean(AuthService.class);
        AuthUser usr = authService.getCurrentUser();
        rec.set("objCalcUser", usr.getAttrs().getLong("id"))
        //rec.set("pvCalcUser", 1102L) //todo
        rec.set("CalcCreatDate", XDate.create(new Date()).toString(XDateTimeFormatter.ISO_DATE))
        st.add(rec)
        //mdb.outTable(st)
        return st
    }

    @DaoMethod
    void saveMainProps(Map<String, Object> rec) {
        VariantMap params = new VariantMap(rec)
        //Attr
        //1
/*
        if (params.getLong("idCalcCreatDate") == 0) {
            if (params.getString("CalcCreatDate").isEmpty())
                throw new XError("CalcCreatDate is required")
            else
                fillProperties(true, "Prop_CalcCreatDate", params)
        } else {
            if (params.getString("CalcCreatDate").isEmpty())
                throw new XError("CalcCreatDate is required")
            else
                updateProperties("Prop_CalcCreatDate", params)
        }
*/

        //2 !req
        if (params.getLong("idCalcLastDate") == 0) {
            if (!params.getString("CalcLastDate").isEmpty())
                fillProperties(true, "Prop_CalcLastDate", params)
        } else {
            if (params.getString("CalcLastDate").isEmpty())
                updateProperties("Prop_CalcLastDate", params)
        }
        //3
        if (params.getLong("idCalcStartYear") == 0) {
            if (params.getString("CalcStartYear").isEmpty())
                throw new XError("CalcStartYear is required")
            else
                fillProperties(true, "Prop_CalcStartYear", params)
        } else {
            if (params.getString("CalcStartYear").isEmpty())
                throw new XError("CalcStartYear is required")
            else
                updateProperties("Prop_CalcStartYear", params)
        }

        //4
        if (params.getLong("idCalcEndYear") == 0) {
            if (params.getString("CalcEndYear").isEmpty())
                throw new XError("CalcEndYear is required")
            else
                fillProperties(true, "Prop_CalcEndYear", params)
        } else {
            if (params.getString("CalcEndYear").isEmpty())
                throw new XError("CalcEndYear is required")
            else
                updateProperties("Prop_CalcEndYear", params)
        }
        //5
        if (params.getLong("idCalcFishSpec") == 0) {
            if (params.getLong("fvCalcFishSpec") > 0)
                fillProperties(true, "Prop_CalcFishSpec", params)
            else
                throw new XError("CalcFishSpec is required")
        } else {
            if (params.getLong("fvCalcFishSpec") > 0)
                updateProperties("Prop_CalcFishSpec", params)
            else
                throw new XError("CalcFishSpec is required")
        }
        //6
        if (params.getLong("idCalcStatus") == 0) {
            if (params.getLong("fvCalcStatus") > 0)
                fillProperties(true, "Prop_CalcStatus", params)
            else
                throw new XError("CalcStatus is required")
        } else {
            if (params.getLong("fvCalcStatus") > 0)
                updateProperties("Prop_CalcStatus", params)
            else
                throw new XError("CalcStatus is required")
        }
        //7

        if (params.getLong("idReservoirShore") == 0) {
            if (params.getLong("objReservoirShore") > 0)
                fillProperties(true, "Prop_ReservoirShore", params)
            else
                throw new XError("ReservoirShore is required")
        } else {
            if (params.getLong("objReservoirShore") > 0)
                updateProperties("Prop_ReservoirShore", params)
            else
                throw new XError("ReservoirShore is required")
        }
        //8 !req
/*
        if (params.getLong("idCalcUser") == 0) {
            if (params.getLong("objCalcUser") > 0)
                fillProperties(true, "Prop_CalcUser", params)
        } else {
            updateProperties("Prop_CalcUser", params)
        }
*/
    }

    @DaoMethod
    Map<Long, String> loadFvAsMap(String codProp) {
        return apiMeta().get(ApiMeta).loadFVasMap(codProp)
    }

    @DaoMethod
    Store loadFVasStore(String codProp) {
        return apiMeta().get(ApiMeta).storePropValForSelectFV(codProp)
    }

    @DaoMethod
    Store loadReservoirs(String codTypOrProp) {
        return loadObjForSelect(codTypOrProp, "monitoringdata")
    }

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
    //**************************************  Bayes Calc **************************************//
    //**************************************  Tab Reservoir **************************************//
    private Map<String, Long> getYears(long own) {
        Map<String, Long> res = new HashMap<>()
        String props = "'Prop_CalcStartYear','Prop_CalcEndYear'"
        Map<String, Object> map = apiMeta().get(ApiMeta).getIdsFromCodsOfEntity("Prop", props)
        map.put("own", own)
        Store stYear = mdb.loadQuery("""
            select 
                v1.strVal as year1, v2.strVal as year2    
            from Obj o
                join DataProp d1 on d1.isObj=1 and d1.objOrRelObj=o.id and d1.prop=:Prop_CalcStartYear
                join DataPropVal v1 on v1.dataprop=d1.id
                join DataProp d2 on d2.isObj=1 and d2.objOrRelObj=o.id and d2.prop=:Prop_CalcEndYear
                join DataPropVal v2 on v2.dataprop=d2.id
            where o.id=${own}
        """, map)
        res.put("year1", stYear.get(0).getLong("year1"))
        res.put("year2", stYear.get(0).getLong("year2"))
        return res
    }

    @DaoMethod
    List<Map<String, Object>> getCols(long own) throws Exception {
        Map<String, Long> mapY = getYears(own)
        long year1 = mapY.get("year1")
        long year2 = mapY.get("year2")
        long count = UtCnv.toLong(year2) - UtCnv.toLong(year1)
        long w1 = 50
        double w = 50 / count
        if (count > 8) {
            w1 = 30
            w = 70 / count
        }
        String w1Str = UtCnv.toString(w1)
        String wStr = UtCnv.toString(w)
        List<Map<String, Object>> cols = new ArrayList<>()
        Map<String, Object> map = new HashMap<>()
        map.put("name", "name")
        map.put("label", "Наименование")
        map.put("field", "name")
        map.put("align", "left")
        map.put("style", "font-size: 1.2em; width: " + w1Str + "%")
        cols.add(map)

        for (long i in 0..count) {
            long y = year1 + i
            String yStr = UtCnv.toString(y)
            map = new HashMap<>()
            map.put("name", "v" + yStr)
            map.put("label", yStr)
            map.put("field", "v" + yStr)
            map.put("align", "left")
            map.put("style", "font-size: 1.2em; width: " + wStr + "%")
            cols.add(map)
        }
        return cols
    }

    @DaoMethod
    Store loadReservoirPage(long own) {
        /* Prop_WaterArea		1008    Prop_CalcWaterFluct	7224 */
        String props = "'Prop_WaterArea','Prop_CalcWaterFluct'"
        Map<String, Object> map = apiMeta().get(ApiMeta).getIdsFromCodsOfEntity("Prop", props)
        map.put("own", own)
        //year1 & year2
        Map<String, Long> mapY = getYears(own)
        long year1 = mapY.get("year1")
        long year2 = mapY.get("year2")
        //
        //String d1 = "${year1}-01-01"
        //String d2 = "${year2}-01-01"

        long count = UtCnv.toLong(year2) - UtCnv.toLong(year1)
        List<String> sel = new ArrayList<>();
        for (long i in 0..count) {
            String year = UtCnv.toString(year1 + i)
            sel.add("null as id" + year + ",  null  as v" + year)
        }
        // sql for value
        String sqlVal = """
            select v1.id, v1.numberval, d1.prop || '_' || 'v'||date_part('year', v1.dbeg) as key   
            from Obj o
                join DataProp d1 on d1.isObj=1 and d1.objOrRelObj=o.id-- and d1.prop=1008
                join DataPropVal v1 on v1.dataprop=d1.id and v1.numberval is not null
            where o.id=${own}
        """
        Store stVal = mdb.loadQuery(sqlVal)
        StoreIndex indVal = stVal.getIndex("key")

        mdb.outTable(stVal)
        //
        Store st = loadSqlMetaWithParams("""
            select p.id, p.parent, p.name, ${sel.join(",")}
            from prop p
            where p.id=:Prop_WaterArea
            union all
            select p.id, p.parent, p.name, ${sel.join(",")}
            from prop p
            where p.id=:Prop_CalcWaterFluct
            union all 
            select p.id, p.parent, p.name, ${sel.join(",")}
            from prop p
            where p.parent=:Prop_CalcWaterFluct
        """, "", map)

        for (StoreRecord r in st) {
            for (StoreField fld in r.fields) {
                if (fld.name.startsWith("v")) {
                    StoreRecord rec = indVal.get(r.getString("id")+"_"+fld.name)
                    if (rec != null) {
                        r.set("id"+fld.name.substring(1), rec.get("id"))
                        r.set(fld.name, rec.get("numberval"))
                    }
                }
            }

        }
        mdb.outTable(st)
        return st
    }

    @DaoMethod
    long saveReservoirPage(Map<String, Object> rec) {
        long obj = UtCnv.toLong(rec.get("obj"))
        long prop = UtCnv.toLong(rec.get("prop"))
        long idVal = UtCnv.toLong(rec.get("idval"))
        boolean hasValue = rec.containsKey("numberval")
        double value = UtCnv.toDouble(rec.get("numberval"))
        boolean dependperiod = 1
        long pt = 11L
        String dt = UtCnv.toString(rec.get("year")) + "-01-01"
        UtPeriod up = new UtPeriod()
        String dbeg = up.calcDbeg(XDate.create(dt), pt, 0).toString(XDateTimeFormatter.ISO_DATE)
        String dend = up.calcDend(XDate.create(dt), pt, 0).toString(XDateTimeFormatter.ISO_DATE)

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
            idVal = mdb.insertRec("DataPropVal", recDPV, false)
        }
        return idVal
    }

    @DaoMethod
    void deleteReservoirPage(long idDPV) {
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
    //**************************************  Tab Fish **************************************//



    //*****************************************************************************************//
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
        Long periodType = null
        if (stProp.get(0).getBoolean("dependPeriod")) {
            if (params.containsKey("periodType"))
                periodType = UtCnv.toLong("periodType")
            else
                periodType = FD_PeriodType_consts.year
        }

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
        //
        if (stProp.get(0).getBoolean("dependPeriod")) {
            whe += "and periodType=${periodType}"
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
                recDP.set("periodType", periodType)
            }
            idDP = mdb.insertRec("DataProp", recDP, true)
        }
        //
        StoreRecord recDPV = mdb.createStoreRecord("DataPropVal")
        recDPV.set("dataProp", idDP)
        // For Attrib
        if ([FD_AttribValType_consts.str].contains(attribValType)) {
            if (cod.equalsIgnoreCase("Prop_CalcStartYear") ||
                    cod.equalsIgnoreCase("Prop_CalcEndYear")) {
                if (params.get(keyValue) != null || params.get(keyValue) != "") {
                    recDPV.set("strVal", UtCnv.toString(params.get(keyValue)))
                }
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }

        if ([FD_AttribValType_consts.dt].contains(attribValType)) {
            if (cod.equalsIgnoreCase("Prop_CalcCreatDate") ||
                    cod.equalsIgnoreCase("Prop_CalcLastDate")) {
                if (params.get(keyValue) != null || params.get(keyValue) != "") {
                    recDPV.set("dateTimeVal", UtCnv.toString(params.get(keyValue)))
                }
            } else
                throw new XError("for dev: [${cod}] отсутствует в реализации")
        }
        if ([FD_AttribValType_consts.multistr].contains(attribValType)) {
            if (cod.equalsIgnoreCase("Prop_CalcDescription")) {
                if (params.get(keyValue) != null || params.get(keyValue) != "") {
                    recDPV.set("multiStrVal", UtCnv.toString(params.get(keyValue)))
                }
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }
        // For Typ
        if ([FD_PropType_consts.typ].contains(propType)) {
            if (cod.equalsIgnoreCase("Prop_ReservoirShore") ||
                    cod.equalsIgnoreCase("Prop_CalcUser")) {
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
            if (cod.equalsIgnoreCase("Prop_CalcFishSpec") ||
                    cod.equalsIgnoreCase("Prop_CalcStatus")) {
                if (propVal > 0) {
                    recDPV.set("propVal", propVal)
                }
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }

        // For Meter
        if ([FD_PropType_consts.meter, FD_PropType_consts.rate].contains(propType)) {
            if (cod.equalsIgnoreCase("Prop_CalcWaterFluct") ||
                    cod.equalsIgnoreCase("Prop_WaterArea")) {
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
            UtPeriod utPeriod = new UtPeriod()
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
            if (cod.equalsIgnoreCase("Prop_CalcStartYear") ||
                    cod.equalsIgnoreCase("Prop_CalcEndYear")) {
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
            if (cod.equalsIgnoreCase("Prop_CalcDescription")) {
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
            if (cod.equalsIgnoreCase("Prop_CalcCreatDate") ||
                    cod.equalsIgnoreCase("Prop_CalcLastDate")) {
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
            if (cod.equalsIgnoreCase("Prop_CalcFishSpec") ||
                    cod.equalsIgnoreCase("Prop_CalcStatus")) {
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
            if (cod.equalsIgnoreCase("Prop_CalcWaterFluct") ||
                    cod.equalsIgnoreCase("Prop_WaterArea")) {
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
            if (cod.equalsIgnoreCase("Prop_ReservoirShore") ||
                    cod.equalsIgnoreCase("Prop_CalcUser")) {
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

    private Store loadSqlService(String sql, String domain, String model) {
        if (model.equalsIgnoreCase("nsidata"))
            return apiNSIData().get(ApiNSIData).loadSql(sql, domain)
        else if (model.equalsIgnoreCase("monitoringdata"))
            return apiMonitoringData().get(ApiMonitoringData).loadSql(sql, domain)
        else
            throw new XError("Unknown model [${model}]")
    }

    private Store loadSqlMeta(String sql, String domain) {
        return apiMeta().get(ApiMeta).loadSql(sql, domain)
    }

    private Store loadSqlMetaWithParams(String sql, String domain, Map<String, Object> params) {
        return apiMeta().get(ApiMeta).loadSqlWithParams(sql, domain, params)
    }

    private long getUser() throws Exception {
        AuthService authSvc = mdb.getApp().bean(AuthService.class)
        long au = authSvc.getCurrentUser().getAttrs().getLong("id")
        if (au == 0)
            throw new XError("notLoginned")
        return au
    }


}
