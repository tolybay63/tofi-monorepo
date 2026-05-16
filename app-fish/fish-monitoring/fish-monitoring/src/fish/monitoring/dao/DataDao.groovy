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
import jandcode.core.store.StoreIndex
import jandcode.core.store.StoreRecord
import tofi.api.dta.ApiUserData
import tofi.api.dta.model.utils.EntityMdbUtils
import tofi.api.dta.model.utils.PeriodGenerator
import tofi.api.dta.model.utils.UtPeriod
import tofi.api.mdl.ApiMeta
import tofi.api.mdl.model.consts.FD_AttribValType_consts
import tofi.api.mdl.model.consts.FD_InputType_consts
import tofi.api.mdl.model.consts.FD_PropType_consts
import tofi.api.mdl.utils.dbfilestorage.DbFileStorageItem
import tofi.api.mdl.utils.dbfilestorage.DbFileStorageService
import tofi.apinator.ApinatorApi
import tofi.apinator.ApinatorService

import java.nio.file.Files
import java.nio.file.Paths

@CompileStatic
class DataDao extends BaseMdbUtils {

    ApinatorApi apiMeta() { return app.bean(ApinatorService).getApi("meta") }
    ApinatorApi apiUserData() { return app.bean(ApinatorService).getApi("userdata") }


    //-----------------------------------------------------------------------------------------------//

    //---------------- Branch ---------------- //

    @DaoMethod
    Store loadClsTree(Map<String, Object> params) {
        return apiMeta().get(ApiMeta).loadClsTree(params)
    }

    @DaoMethod
    Store loadObj(long cls) {
        Store st = mdb.createStore("Obj.full")
        mdb.loadQuery(st, """
            select o.*, v.name, v.fullName, v.objParent as parent from Obj o, ObjVer v
            where o.id=v.ownerVer and v.lastVer=1 and o.cls=:c
        """, [c: cls])
        return st
    }

    @DaoMethod
    Map<String, Object> idNameParent(long cls) {
        Map<String, Object> res = new HashMap<>()
        Store st = mdb.loadQuery("""
            select o.id, v.name from Obj o, ObjVer v where o.id=v.ownerVer and v.lastVer=1 and o.cls=:cls
        """, [cls: cls])
        if (st.size() == 0) {
            res.put("id", 0) as Map<String, Object>
            res.put("name", "") as Map<String, Object>
        } else {
            res.put("id", st.get(0).getLong("id"))
            res.put("name", st.get(0).getString("name"))
        }
        return res
    }

    @DaoMethod
    Map<String, Long> getClsIds(String codCls) {
        if (codCls == "")
            return apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", "", "Cls_%")
        else
            return apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", codCls, "")
    }

    @DaoMethod
    StoreRecord insertBranch(Map<String, Object> rec) {
        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        long own = eu.insertEntity(rec)
        return loadObjRec(own)
    }

    @DaoMethod
    StoreRecord updateBranch(Map<String, Object> rec) {
        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        long id = UtCnv.toLong(rec.get("id"))
        eu.updateEntity(rec)
        return loadObjRec(id)
    }

    private StoreRecord loadObjRec(long obj) {
        StoreRecord st = mdb.createStoreRecord("Obj.full")
        mdb.loadQueryRecord(st, """
            select o.*, v.name, v.fullName, v.objParent as parent from Obj o, ObjVer v
            where o.id=v.ownerVer and v.lastVer=1 and o.id=:o
        """, [o: obj])
        return st
    }

    @DaoMethod
    void deleteBranch(long id) {
        checkForExistData(id, 1)
        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        eu.deleteEntity(id)
    }

    //---------------- KATO ---------------- //
    @DaoMethod
    Store loadKATO() {
        Set<Object> setCls = apiMeta().get(ApiMeta).setIdsOfCls("Typ_KATO")
        if (setCls.isEmpty()) setCls.add(0L)
        String whe = setCls.join(",")
        Store st = mdb.createStore("Obj.cust")
        mdb.loadQuery(st, """
            select o.id, v.objParent as parent, o.cls, v.name, o.cmt, v.fullName, o.ord 
            from Obj o
                left join ObjVer v on o.id=v.ownerver and v.lastver=1
            where o.cls in (${whe})
            order by o.ord
        """)
        //
        return st
    }

    @DaoMethod
    void deleteKATO(long id) {
        checkForExistData(id, 1)
        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        eu.deleteEntity(id)
    }

    Store loadObj(String codTyp, long idObj) {
        String whe = "o.id=${idObj}"
        if (idObj == 0) {
            Set<Object> ids = apiMeta().get(ApiMeta).setIdsOfCls(codTyp)
            whe = "o.cls in (0${ids.join(",")})"
        }
        Store st = mdb.createStore("ObjAndObjVer")
        mdb.loadQuery(st, """
            select
                o.id, o.cls, v.name, v.fullName, null as nameCls, o.cmt
            from Obj o
                left join ObjVer v on o.id=v.ownerVer and v.lastVer=1
            where ${whe}
        """)
        Set<Object> idsCls = st.getUniqueValues("cls")
        Store stCls = apiMeta().get(ApiMeta).loadSql("""
            select c.id, v.name
            from Cls c, ClsVer v where c.id=v.ownerVer and v.lastVer=1 and c.id in (0${idsCls.join(",")})
        """, "")
        StoreIndex indCls = stCls.getIndex("id")
        for (StoreRecord r in st) {
            StoreRecord rec = indCls.get(r.getLong("cls"))
            if (rec != null)
                r.set("nameCls", rec.getString("name"))
        }
        return st
    }

    //---------------- Reservors---------------- //
    @DaoMethod
    Store loadReservors(Map<String, Object> params) {

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
                t1.lstBranch, t2.lstKATO,
                v3.id as idReservoirType, v3.propval as pvReservoirType, null as fvReservoirType,
                v4.id as idReservoirStatus, v4.propval as pvReservoirStatus, null as fvReservoirStatus,
                v5.id as idFishFarmingType, v5.propval as pvFishFarmingType, null as fvFishFarmingType,
                v6.id as idCoordinate, v6.strVal as Coordinate,
                v7.id as idDescription, v7.multiStrVal as Description
            from ob                
                left join (
                    select d1.objorrelobj, d1.prop,
                    STRING_AGG (cast(v1.id||'_'||v1.obj||'_'||v1.propval as varchar(2000)), ',' order by v1.id) as lstBranch
                    from ob
                        left join DataProp d1  on d1.isobj=1 and d1.objorrelobj=ob.id and d1.prop=:Prop_Branch
                        left join DataPropVal v1 on d1.id=v1.dataprop
                    where 0=0
                    group by d1.objorrelobj, d1.prop
                    ) t1 on t1.objorrelobj=ob.id and t1.prop=:Prop_Branch
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
            Store stObj = mdb.loadQuery("""
                select v.name from Obj o, ObjVer v where o.id=v.ownerVer and v.lastVer=1 and o.id in (${idsObj.join(",")})
            """)
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
            stObj = mdb.loadQuery("""
                select v.name from Obj o, ObjVer v where o.id=v.ownerVer and v.lastVer=1 and o.id in (${idsObj.join(",")})
            """)
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
        if (prop > 0) {
            return mdb.loadQuery("""
                select d.prop as id, v.numberval, v.dbeg, v.dend, v.id as idval
                from DataProp d
                    left join DataPropVal v on d.id=v.dataProp
                where d.isObj=0 and d.objorrelobj=${obj} and d.prop=${prop}
            """)
        } else {
            if (obj == 0)
                return mdb.createStore()
            Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "", "Prop_Water%")
            Map<String, Long> map1 = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "", "Prop_Reservoir%")
            map.putAll(map1)
            Store st = apiMeta().get(ApiMeta).loadSqlWithParams("""
                select p.id, p.parent, p.name || ' ('||m.name||')' as name, p.isdependvalueonperiod as dependperiod, null as dbeg, null as dend, null as numberval, null as idval
                from Prop p, Measure m
                where p.id=:Prop_WaterArea and p.measure=m.id
                union all
                select p.id, p.parent, p.name || ' ('||m.name||')' as name, p.isdependvalueonperiod as dependperiod, null as dbeg, null as dend, null as numberval, null as idval
                from Prop p, Measure m
                where p.id=:Prop_WaterLevel and p.measure=m.id
                union all
                select p.id, p.parent, p.name || ' ('||m.name||')' as name, p.isdependvalueonperiod as dependperiod, null as dbeg, null as dend, null as numberval, null as idval
                from Prop p, Measure m
                where p.id=:Prop_WaterLength and p.measure=m.id
                union all
                select p.id, p.parent, p.name || ' ('||m.name||')' as name, p.isdependvalueonperiod as dependperiod, null as dbeg, null as dend, null as numberval, null as idval
                from Prop p, Measure m
                where p.id=:Prop_ReservoirWidth and p.measure=m.id
                union all
                select p.id, p.parent, p.name || ' ('||m.name||')' as name, p.isdependvalueonperiod as dependperiod, null as dbeg, null as dend, null as numberval, null as idval
                from Prop p, Measure m
                where p.parent=:Prop_ReservoirWidth and p.measure=m.id
                union all
                select p.id, p.parent, p.name || ' ('||m.name||')' as name, p.isdependvalueonperiod as dependperiod, null as dbeg, null as dend, null as numberval, null as idval
                from Prop p, Measure m
                where p.id=:Prop_ReservoirDepth and p.measure=m.id
                union all
                select p.id, p.parent, p.name || ' ('||m.name||')' as name, p.isdependvalueonperiod as dependperiod, null as dbeg, null as dend, null as numberval, null as idval
                from Prop p, Measure m
                where p.parent=:Prop_ReservoirDepth and p.measure=m.id
            """, "", map as Map<String, Object>)
            Set<Object> idsProp = st.getUniqueValues("id")
            //

            Store stData = mdb.loadQuery("""
                select d.prop as prop, v.numberval, v.dbeg, v.dend, v.id
                from DataProp d
                    left join DataPropVal v on d.id=v.dataProp
                where d.isObj=1 and d.objorrelobj=${obj} and d.periodType=${periodType} and 
                    '${dte}' between v.dbeg and v.dend and d.prop in (0${idsProp.join(",")})
                union all
                select d.prop as prop, v.numberval, v.dbeg, v.dend, v.id
                from DataProp d
                    left join DataPropVal v on d.id=v.dataProp
                where d.isObj=1 and d.objorrelobj=${obj} and d.periodType is null and '${dte}' between v.dbeg and v.dend 
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
            for (String it in lstBranch) {
                String[] arr = it.split("_")
                pms.put("objBranch", UtCnv.toLong(arr[0]))
                pms.put("pvBranch", UtCnv.toLong(arr[1]))
                fillProperties(true, "Prop_Branch", pms)
            }
            for (String it in lstRegion) {
                String[] arr = it.split("_")
                pms.put("objKATO", UtCnv.toLong(arr[0]))
                pms.put("pvKATO", UtCnv.toLong(arr[1]))
                fillProperties(true, "Prop_KATO", pms)
            }
            //
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
                // KATO
                List<String> lstKATO = UtCnv.toList(pms.get("lstKATO"))
                List<String> lstOldKATO = new ArrayList<>()
                List<String> objKATO = UtCnv.toList(pms.get("objKATO"))
                //Delete
                for (String it in lstKATO) {
                    long idVal = UtCnv.toLong(it.substring(0, it.indexOf("_")))
                    String oldKATO = it.substring(it.indexOf("_") + 1)
                    lstOldBranch.add(oldKATO) // obj_pv
                    if (!objBranch.contains(oldKATO)) {
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
        return loadReservors([codTyp: "", idObj: own] as Map<String, Object>)
    }

    //---------------- 2 SamplingStation ----------------//
    @DaoMethod
    Store loadSamplingStations(Map<String, Object> params) {
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
                v3.id as idDescription, v3.multiStrVal as Description
            from Obj o
                left join ObjVer v on o.id=v.ownerver and v.lastver=1
                left join DataProp d1 on d1.objorrelobj=o.id and d1.prop=:Prop_Coordinate
                left join DataPropVal v1 on d1.id=v1.dataprop 
                left join DataProp d2 on d2.objorrelobj=o.id and d2.prop=:Prop_AreaOfTon
                left join DataPropVal v2 on d2.id=v2.dataprop
                left join DataProp d3 on d3.objorrelobj=o.id and d3.prop=:Prop_Description
                left join DataPropVal v3 on d3.id=v3.dataprop
            where ${whe}
        """, map)
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
                left join DataProp d1 on d1.objorrelobj=o.id and d1.prop=:Prop_FishFamily
                left join DataPropVal v1 on d1.id=v1.dataprop 
                left join DataProp d2 on d2.objorrelobj=o.id and d2.prop=:Prop_FishTyp
                left join DataPropVal v2 on d2.id=v2.dataprop
                left join DataProp d3 on d3.objorrelobj=o.id and d3.prop=:Prop_Description
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

    //---------------- 4 TypesFishGear ----------------//
    @DaoMethod
    Store loadFishGear(Map<String, Object> params) {
        String codTyp = UtCnv.toString(params.get("codTyp"))
        long idObj = UtCnv.toLong(params.get("idObj"))

        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "", "Prop_%")
        String whe = "o.id=${idObj}"
        if (idObj == 0) {
            Set<Object> idsCls = apiMeta().get(ApiMeta).setIdsOfCls(codTyp)
            whe = "o.cls in (${idsCls.join(",")})"
        }
        Store st = mdb.createStore("Obj.FishGear")
        mdb.loadQuery(st, """
            select o.id as obj, o.cls, v.name, null as nameCls, 
                v3.id as idDescription, v3.multiStrVal as Description
            from Obj o
                left join ObjVer v on o.id=v.ownerver and v.lastver=1
                left join DataProp d1 on d1.objorrelobj=o.id and d1.prop=:Prop_FishFamily
                left join DataPropVal v1 on d1.id=v1.dataprop 
                left join DataProp d2 on d2.objorrelobj=o.id and d2.prop=:Prop_FishTyp
                left join DataPropVal v2 on d2.id=v2.dataprop
                left join DataProp d3 on d3.objorrelobj=o.id and d3.prop=:Prop_Description
                left join DataPropVal v3 on d3.id=v3.dataprop
            where ${whe}
        """, map)
        Set<Object> idsCls = st.getUniqueValues("cls")
        Store stCls = apiMeta().get(ApiMeta).loadSql("""
            select c.id, name from Cls c, ClsVer v where c.id=v.ownerVer and v.lastVer=1 and c.id in (0${idsCls.join(",")})
        """, "")
        StoreIndex indCls = stCls.getIndex("id")

        for (StoreRecord r in st) {
            StoreRecord rec = indCls.get(r.getLong("cls"))
            if (rec != null)
                r.set("nameCls", rec.getString("name"))
        }
        //mdb.outTable(st)
        return st
    }

    @DaoMethod
    Store saveFishGear(Map<String, Object> params) {
        VariantMap pms = new VariantMap(params)
        long own
        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        Map<String, Object> par = new HashMap<>(pms)
        par.put("fullName", pms.get("name"))
        if (pms.getString("mode").equalsIgnoreCase("ins")) {
            par.put("cls", params.get("cls"))
            own = eu.insertEntity(par)
            pms.put("own", own)
            //Prop_Description
            if (!pms.getString("Description").isEmpty())
                fillProperties(true, "Prop_Description", pms)
        } else {
            own = pms.getLong("obj")
            par.put("id", own)
            eu.updateEntity(par)
            //
            pms.put("own", own)
            //Prop_Description
            if (pms.containsKey("idDescription"))
                updateProperties("Prop_Description", pms)
            else if (!pms.getString("Description").isEmpty())
                fillProperties(true, "Prop_Description", pms)
        }
        return loadFishGear([codTyp: "", idObj: own] as Map<String, Object>)
    }

    @DaoMethod
    void deleteFishGear(long id) {
        checkForExistData(id, 1)
        deleteOwnerWithProperties(id, 1)
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
    Store loadFishFecundity(long relobj, long prop) {
        if (prop > 0) {
            return mdb.loadQuery("""
                select d.prop as id, v.numberval, v.id as idval
                from DataProp d
                    left join DataPropVal v on d.id=v.dataProp
                where d.isObj=0 and d.objorrelobj=${relobj} and d.prop=${prop}
            """)
        } else {
            if (relobj == 0)
                return mdb.createStore()
            Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "Prop_FishFecundity", "")
            Store st = loadSqlMeta("""
                select id, parent, name, null as numberval, null as idval 
                from Prop where id=${map.get("Prop_FishFecundity")} or parent=${map.get("Prop_FishFecundity")}
                order by id
            """, "")
            Set<Object> idsProp = st.getUniqueValues("id")

            Store stData = mdb.loadQuery("""
                select d.prop as id, v.numberval, v.id as idval
                from DataProp d
                    left join DataPropVal v on d.id=v.dataProp
                where d.isObj=0 and d.objorrelobj=${relobj} and d.prop in (0${idsProp.join(",")})
            """)
            StoreIndex indData = stData.getIndex("id")
            for (StoreRecord r in st) {
                StoreRecord rec = indData.get(r.getLong("id"))
                if (rec != null) {
                    r.set("numberval", rec.getDouble("numberval"))
                    r.set("idval", rec.getLong("idval"))
                }
            }
            return st
        }
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
    Store saveFishFecundiry(Map<String, Object> rec) {
        long relobj = UtCnv.toLong(rec.get("relobj"))
        long prop = UtCnv.toLong(rec.get("prop"))
        long idVal = UtCnv.toLong(rec.get("idval"))
        boolean hasValue = rec.containsKey("numberval")
        double value = UtCnv.toDouble(rec.get("numberval"))
        if (idVal > 0) {
            if (hasValue) {
                String tm = XDateTime.create(new Date()).toString(XDateTimeFormatter.ISO_DATE_TIME)
                mdb.execQueryNative("""
                    update DataPropVal set numberval=${value}, timestamp='${tm}' where id=${idVal}
                """)
            } else {
                mdb.execQueryNative("""
                    delete from DataPropVal
                    where dataProp in (select id from DataProp where isobj=0 and objorrelobj=${relobj});
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
            recDP.set("objorrelobj", relobj)
            recDP.set("prop", prop)
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
            recDPV.set("dbeg", "1800-01-01")
            recDPV.set("dend", "3333-12-31")
            recDPV.set("timeStamp", XDateTime.create(new Date()).toString(XDateTimeFormatter.ISO_DATE_TIME))
            mdb.insertRec("DataPropVal", recDPV, false)
        }
        return loadFishFecundity(relobj, prop)
    }

    @DaoMethod
    Store loadReservoir(String codTyp) {
        Store st = loadObjForSelect(codTyp)
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
    Store loadTypeOfFish(String codTyp) {
        return loadObjForSelect(codTyp)
    }

    @DaoMethod
    Store loadBranchForSelect(String codTypOrProp) {
        return loadObjForSelect(codTypOrProp)
    }

    @DaoMethod
    Store loadKatoForSelect(String codTypOrProp) {
        return loadObjForSelect(codTypOrProp)
    }

    @DaoMethod
    Store loadObjForSelect(String codTypOrProp) {
        if (codTypOrProp.startsWith("Typ_")) {
            Set<Object> idsCls = apiMeta().get(ApiMeta).setIdsOfCls(codTypOrProp)
            idsCls.add(0)
            return mdb.loadQuery("""
                select o.id, o.cls, v.name
                from Obj o, ObjVer v
                where o.id=v.ownerVer and v.lastVer=1 and o.cls in (${idsCls.join(",")})
            """)
        } else if (codTypOrProp.startsWith("Prop_")) {
            Map<String, Long> mapProp = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", codTypOrProp, "")
            Store stProp = apiMeta().get(ApiMeta).loadSql("""
                select cls, id as propval
                from PropVal
                where prop=${mapProp.get(codTypOrProp)} and cls is not null
            """, "")
            StoreIndex indProp = stProp.getIndex("cls")

            Set<Object> idsCls = stProp.getUniqueValues("cls")

            Store stObj = mdb.loadQuery("""
                select o.id as obj, o.cls, null as id, v.name
                from Obj o, ObjVer v
                where o.id=v.ownerVer and v.lastVer=1 and o.cls in (0${idsCls.join(",")})
            """)
            for (StoreRecord r in stObj) {
                StoreRecord rec = indProp.get(r.getLong("cls"))
                if (rec != null) {
                    r.set("id", r.getString("obj") + "_" + rec.getString("propval"))
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
        checkForExistData(id, isObj)
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

    void fillProperties(boolean isObj, String cod, Map<String, Object> params) {
        long own = UtCnv.toLong(params.get("own"))
        String keyValue = cod.split("_")[1]
        long objRef = UtCnv.toLong(params.get("obj" + keyValue))
        long propVal = UtCnv.toLong(params.get("pv" + keyValue))

        Store stProp = apiMeta().get(ApiMeta).getPropInfo(cod)
        //
        long prop = stProp.get(0).getLong("id")
        long propType = stProp.get(0).getLong("propType")
        long attribValType = stProp.get(0).getLong("attribValType")
        boolean dependPeriod = stProp.get(0).getInt("dependPeriod") == 1
        long periodType = 0
        if (dependPeriod) {
            periodType = UtCnv.toLong(params.get("periodType"))
        }
        Integer digit = null
        double koef = stProp.get(0).getDouble("koef")
        if (koef == 0) koef = 1
        if (!stProp.get(0).isValueNull("digit"))
            digit = stProp.get(0).getInt("digit")

        long idDP = 0
        String dend = "3333-12-31"
        //
        String whePeriod = "and d.periodType is null"
        if (periodType > 0) {
            whePeriod = "and d.periodType=${periodType}"
        }
        Store stDP = mdb.loadQuery("""
            select d.id 
            from DataProp d, DataPropVal v
            where d.id=v.dataprop and d.isobj=1 and d.prop=${prop} and d.objorrelobj=${own} ${whePeriod}
            order by dbeg desc, dend
        """)
        if (stDP.size() > 0)
            idDP = stDP.get(0).getLong("id")
        else
            idDP = getIdDataProp(stProp, isObj, own, prop, periodType)

        //Изменился условия, при повторном использовании надо анализировать
        if (periodType = 0) {
            if (params.keySet().contains("isFirst")) {
                if (UtCnv.toInt(params.get("isFirst")) == 1) {
                    Store st = mdb.loadQuery("""
                        select v.id, d.id as did, v.dbeg, v.dend 
                        from DataProp d, DataPropVal v
                        where d.id=v.dataprop and d.isobj=1 and d.prop=${prop} and d.objorrelobj=${own}
                        order by dbeg desc, dend
                    """)
                    if (st.size() > 0) {
                        String dte = UtCnv.toString(params.get("dte"))
                        for (StoreRecord r in st) {
                            if (XDate.create(dte).toJavaLocalDate().isBefore(r.getDate("dbeg").toJavaLocalDate())) {
                                dend = r.getDate("dbeg").toJavaLocalDate().minusDays(1)
                                idDP = r.getLong("did")
                            }
                        }
                    } else {
                        idDP = getIdDataProp(stProp, isObj, own, prop, periodType)
                    }
                } else {
                    idDP = UtCnv.toLong(params.get("did" + keyValue))
                }
            }
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
            if (cod.equalsIgnoreCase("Prop_RegDocumentsDateApproval") ||
                    cod.equalsIgnoreCase("Prop_RegDocumentsLifeDoc") ||
                    cod.equalsIgnoreCase("Prop_SampleDate") ||
                    cod.equalsIgnoreCase("Prop_ResearchDate")) {
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
                    cod.equalsIgnoreCase("Prop_Branch") /*||
                    cod.equalsIgnoreCase("Prop_ReservoirShore") ||
                    cod.equalsIgnoreCase("Prop_SampleExecutor") ||
                    cod.equalsIgnoreCase("Prop_StationSampling") ||
                    cod.equalsIgnoreCase("Prop_LinkToSample") ||
                    cod.equalsIgnoreCase("Prop_ResearchExecutor") ||
                    cod.equalsIgnoreCase("Prop_FishTyp") ||
                    cod.equalsIgnoreCase("Prop_FishGear") ||
                    cod.equalsIgnoreCase("Prop_FishZone")*/) {
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
                    /*cod.equalsIgnoreCase("Prop_FishGender") ||*/
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
            if (cod.equalsIgnoreCase("Prop_FishStartPuberty") ||
                    cod.equalsIgnoreCase("Prop_FishEndPuberty") ||
                    cod.equalsIgnoreCase("Prop_FishFecundity") ||
                    cod.equalsIgnoreCase("Prop_AreaOfTon") ||
                    cod.equalsIgnoreCase("Prop_WaterArea") ||
                    cod.equalsIgnoreCase("Prop_WaterLevel") ||
                    cod.equalsIgnoreCase("Prop_WaterLength") ||
                    cod.equalsIgnoreCase("Prop_ReservoirWidth") ||
                    cod.equalsIgnoreCase("Prop_ReservoirDepth")) {
                if (params.get(keyValue) != null) {
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
        if (dependPeriod) {
            if (!params.containsKey("dte"))
                params.put("dte", XDateTime.create(new Date()).toString(XDateTimeFormatter.ISO_DATE))
            UtPeriod utPeriod = new UtPeriod()
            XDate d1 = utPeriod.calcDbeg(UtCnv.toDate(params.get("dte")), periodType, 0)
            XDate d2 = utPeriod.calcDend(UtCnv.toDate(params.get("dte")), periodType, 0)
            recDPV.set("dbeg", d1.toString(XDateTimeFormatter.ISO_DATE))
            recDPV.set("dend", d2.toString(XDateTimeFormatter.ISO_DATE))
        } else {
            if (params.keySet().contains("isFirst")) {
                if (UtCnv.toInt(params.get("isFirst")) == 1) {
                    recDPV.set("dbeg", UtCnv.toString(params.get("dte")))
                    recDPV.set("dend", dend)
                } else {
                    updateDbegDend(UtCnv.toInt(isObj), prop, cod, params, recDPV)
                }
            } else {
                params.putIfAbsent("dbeg", "1800-01-01")
                params.putIfAbsent("dend", "3333-12-31")
                recDPV.set("dbeg", params.get("dbeg"))
                recDPV.set("dend", params.get("dend"))
            }
        }

        long au = getUser()
        recDPV.set("authUser", au)
        recDPV.set("inputType", FD_InputType_consts.app)
        long idDPV = mdb.getNextId("DataPropVal")
        recDPV.set("id", idDPV)
        recDPV.set("ord", idDPV)
        recDPV.set("timeStamp", XDateTime.create(new Date()).toString(XDateTimeFormatter.ISO_DATE_TIME))
        mdb.insertRec("DataPropVal", recDPV, false)
    }

    private void updateDbegDend(int isObj, long prop, String codProp, Map<String, Object> params, StoreRecord rec) {
        String cod = codProp.split("_")[1]
        long own = UtCnv.toLong(params.get("own"))
        Store st = mdb.loadQuery("""
            select v.id, v.dbeg, v.dend 
            from DataProp d, DataPropVal v
            where d.id=v.dataprop and d.isobj=${isObj} and d.prop=${prop} and d.objorrelobj=${own}
            order by dbeg desc, dend
        """)
        String dte = UtCnv.toString(params.get("dte"))

        if (st.size() > 0) {
            for (StoreRecord rr in st) {
                String dbeg = rr.getString("dbeg")
                String dend = rr.getString("dend")
                String dendNew = rr.getString("dend")
                long idv = rr.getLong("id")
                if (XDate.create(dte).toJavaLocalDate().plusDays(1).isAfter(XDate.create(dbeg).toJavaLocalDate()) &&
                        XDate.create(dte).toJavaLocalDate().minusDays(1).isBefore(XDate.create(dend).toJavaLocalDate())) {
                    if (dbeg == dte) {
                        throw new XError("ExistsValueOnDate@" + cod)
                    }
                    String dendOLd = XDate.create(dte).toJavaLocalDate().minusDays(1)
                    mdb.execQuery("""
                        update DataPropVal set dend='${dendOLd}' where id=${idv}
                    """)
                    rec.set("dbeg", dte)
                    rec.set("dend", dendNew)
                    break
                }
            }
        } else {
            rec.set("dbeg", dte)
        }
    }

    void updateProperties(String cod, Map<String, Object> params) {
        VariantMap mapProp = new VariantMap(params)
        String keyValue = cod.split("_")[1]
        long idVal = mapProp.getLong("id" + keyValue)
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

        String sql = ""
        def tmst = XDateTime.create(new Date()).toString(XDateTimeFormatter.ISO_DATE_TIME)
        // For Attrib
        if ([FD_AttribValType_consts.str].contains(attribValType)) {
            if (cod.equalsIgnoreCase("Prop_Coordinate") ||
                    cod.equalsIgnoreCase("Prop_FishSpawPeriod") ||
                    cod.equalsIgnoreCase("Prop_FishSpawFrequency")) {
                def strValue = mapProp.getString(keyValue)
                if (!mapProp.keySet().contains(keyValue) || strValue.trim() == "") {
                    sql = """
                        delete from DataPropVal where id=${idVal};
                        delete from DataProp where id in (
                            select id from DataProp
                            except
                            select dataProp as id from DataPropVal
                        );
                    """
                } else {
                    sql = "update DataPropval set strVal='${strValue}', timeStamp='${tmst}' where id=${idVal}"
                }
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }
        if ([FD_AttribValType_consts.dt].contains(attribValType)) {
            if (cod.equalsIgnoreCase("Prop_RegDocumentsDateApproval") ||
                    cod.equalsIgnoreCase("Prop_SampleDate") ||
                    cod.equalsIgnoreCase("Prop_ResearchDate")) {
                def dtValue = mapProp.getString(keyValue)
                if (!mapProp.keySet().contains(keyValue) || dtValue.trim() == "") {
                    sql = """
                        delete from DataPropVal where id=${idVal};
                        delete from DataProp where id in (
                            select id from DataProp
                            except
                            select dataProp as id from DataPropVal
                        );
                    """
                } else {
                    sql = "update DataPropval set dateTimeVal='${dtValue}', timeStamp='${tmst}' where id=${idVal}"
                }
            } else
                throw new XError("for dev: [${cod}] отсутствует в реализации")
        }
        if ([FD_AttribValType_consts.multistr].contains(attribValType)) {
            if (cod.equalsIgnoreCase("Prop_Description")) {
                def str = mapProp.getString(keyValue)
                if (!mapProp.keySet().contains(keyValue) || str.trim() == "") {
                    sql = """
                        delete from DataPropVal where id=${idVal};
                        delete from DataProp where id in (
                            select id from DataProp
                            except
                            select dataProp as id from DataPropVal
                        );
                    """
                } else {
                    sql = "update DataPropval set multiStrVal='${str}', timeStamp='${tmst}' where id=${idVal}"
                }
            } else
                throw new XError("for dev: [${cod}] отсутствует в реализации")
        }
        // For Typ
        if ([FD_PropType_consts.typ].contains(propType)) {
            if (cod.equalsIgnoreCase("Prop_Region") ||
                    cod.equalsIgnoreCase("Prop_Branch") ||
                    cod.equalsIgnoreCase("Prop_District") ||
                    cod.equalsIgnoreCase("Prop_ReservoirShore") ||
                    cod.equalsIgnoreCase("Prop_SampleExecutor") ||
                    cod.equalsIgnoreCase("Prop_LinkToSample") ||
                    cod.equalsIgnoreCase("Prop_ResearchExecutor") ||
                    cod.equalsIgnoreCase("Prop_FishTyp") ||
                    cod.equalsIgnoreCase("Prop_FishGear") ||
                    cod.equalsIgnoreCase("Prop_FishZone")) {
                if (objRef > 0)
                    sql = "update DataPropval set propVal=${propVal}, obj=${objRef}, timeStamp='${tmst}' where id=${idVal}"
                else {
                    sql = """
                        delete from DataPropVal where id=${idVal};
                        delete from DataProp where id in (
                            select id from DataProp
                            except
                            select dataProp as id from DataPropVal
                        );
                    """
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
                    /*cod.equalsIgnoreCase("Prop_FishGender") ||*/
                    cod.equalsIgnoreCase("Prop_FishFamily") ||
                    cod.equalsIgnoreCase("Prop_FishTyp")) {
                if (propVal > 0)
                    sql = "update DataPropval set propVal=${propVal}, timeStamp='${tmst}' where id=${idVal}"
                else {
                    sql = """
                        delete from DataPropVal where id=${idVal};
                        delete from DataProp where id in (
                            select id from DataProp
                            except
                            select dataProp as id from DataPropVal
                        );
                    """
                }
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }
        // For Meter
        if ([FD_PropType_consts.meter, FD_PropType_consts.rate].contains(propType)) {
            if (cod.equalsIgnoreCase("Prop_FishStartPuberty") ||
                    cod.equalsIgnoreCase("Prop_FishEndPuberty") ||
                    cod.equalsIgnoreCase("Prop_FishFecundity") ||
                    cod.equalsIgnoreCase("Prop_AreaOfTon") ||
                    cod.equalsIgnoreCase("Prop_WaterArea") ||
                    cod.equalsIgnoreCase("Prop_WaterLevel") ||
                    cod.equalsIgnoreCase("Prop_WaterLength") ||
                    cod.equalsIgnoreCase("Prop_ReservoirWidth") ||
                    cod.equalsIgnoreCase("Prop_ReservoirDepth")) {
                if (mapProp.keySet().contains(keyValue) && mapProp[keyValue] != "") {
                    def v = mapProp.getDouble(keyValue)
                    v = v / koef
                    if (digit) v = v.round(digit)
                    sql = "update DataPropval set numberVal=${v}, timeStamp='${tmst}' where id=${idVal}"
                } else {
                    sql = """
                        delete from DataPropVal where id=${idVal};
                        delete from DataProp where id in (
                            select id from DataProp
                            except
                            select dataProp as id from DataPropVal
                        );
                    """
                }
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }
        mdb.execQueryNative(sql)

    }

    private void execSql(String sql, String model) {
        if (model.equalsIgnoreCase("userdata"))
            apiUserData().get(ApiUserData).execSql(sql)
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
