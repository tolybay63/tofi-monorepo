package dtj.plan.dao

import groovy.transform.CompileStatic
import jandcode.commons.UtCnv
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
import tofi.api.dta.ApiNSIData
import tofi.api.dta.ApiObjectData
import tofi.api.dta.ApiOrgStructureData
import tofi.api.dta.ApiPersonnalData
import tofi.api.dta.ApiUserData
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

    ApinatorApi apiMeta() {
        return app.bean(ApinatorService).getApi("meta")
    }
    ApinatorApi apiUserData() {
        return app.bean(ApinatorService).getApi("userdata")
    }
    ApinatorApi apiNSIData() {
        return app.bean(ApinatorService).getApi("nsidata")
    }
    ApinatorApi apiPersonnalData() {
        return app.bean(ApinatorService).getApi("personnaldata")
    }
    ApinatorApi apiOrgStructureData() {
        return app.bean(ApinatorService).getApi("orgstructuredata")
    }
    ApinatorApi apiObjectData() {
        return app.bean(ApinatorService).getApi("objectdata")
    }


    @DaoMethod
    Store getPersonnalInfo(long userId) {

        Store st = loadSqlService("""
            select o.id
            from Obj o
            left join DataProp d on d.isObj=1 and d.prop=:Prop_UserId
            left join DataPropVal v on d.id=v.dataProp and v.strVal='${userId}'
        """, "", "personnaldata")

        if (st.size()==0)
            throw new XError("Not found")
        long own = st.get(0).getLong("id")
        return apiPersonnalData().get(ApiPersonnalData).loadPersonnal(own)

    }

    private Set<Object> getIdsObjLocation(long obj) {
        Store st = loadSqlService("""
           WITH RECURSIVE r AS (
               SELECT o.id, v.objParent as parent
               FROM Obj o, ObjVer v
               WHERE o.id=v.ownerver and v.lastver=1 and v.objParent=${obj}
               UNION ALL
               SELECT t.*
               FROM ( SELECT o.id, v.objParent as parent
                      FROM Obj o, ObjVer v
                      WHERE o.id=v.ownerver and v.lastver=1
                    ) t
                  JOIN r
                      ON t.parent = r.id
           ),
           o as (
           SELECT o.id, v.objParent as parent
           FROM Obj o, ObjVer v
           WHERE o.id=v.ownerver and v.lastver=1 and o.id=${obj}
           )
           SELECT * FROM o
           UNION ALL
           SELECT * FROM r
           where 0=0
        """, "", "orgstructuredata")

        return st.getUniqueValues("id")
    }

    @DaoMethod
    Store loadPlan(Map<String, Object> params) {
        Store st = mdb.createStore("Obj.plan")

        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Typ", "Typ_WorkPlan", "")
        Store stCls = loadSqlMeta("""
            select c.id , v.name
            from Cls c, ClsVer v
            where c.id=v.ownerVer and v.lastVer=1 and typ=${map.get("Typ_WorkPlan")}
        """, "")
        Set<Object> idsCls = stCls.getUniqueValues("id")
        StoreIndex indClsWork = stCls.getIndex("id")
        String whe
        String wheV1 = ""
        String wheV7 = ""
        if (params.containsKey("id"))
                whe = "o.id=${UtCnv.toLong(params.get("id"))}"
        else {
            whe = "o.cls in (${idsCls.join(",")})"
            //
            Map<String, Long> mapCls = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", "Cls_LocationSection", "")
            long clsLocation = loadSqlService("""
                select cls from Obj where id=${UtCnv.toLong(params.get("objLocation"))}
            """, "", "orgstructuredata").get(0).getLong("cls")
            if (clsLocation == mapCls.get("Cls_LocationSection")) {
                Set<Object> idsObjLocation = getIdsObjLocation(UtCnv.toLong(params.get("objLocation")))
                wheV1 = "and v1.obj in (${idsObjLocation.join(",")})"
            }
            long pt = UtCnv.toLong(params.get("periodType"))
            String dte = UtCnv.toString(params.get("date"))
            UtPeriod utPeriod = new UtPeriod()
            XDate d1 = utPeriod.calcDbeg(UtCnv.toDate(dte), pt, 0)
            XDate d2 = utPeriod.calcDend(UtCnv.toDate(dte), pt, 0)
            wheV7 = "and v7.dateTimeVal between '${d1}' and '${d2}'"
        }

        map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "", "Prop_")



        mdb.loadQuery(st, """
            select o.id, o.cls, v.name, null as nameCls,
                v1.id as idLocationClsSection, v1.propVal as pvLocationClsSection, 
                    v1.obj as objLocationClsSection, null as nameLocationClsSection,
                v2.id as idObject, v2.propVal as pvObject, v2.obj as objObject, 
                    null as nameClsObject, null as fullNameObject,
                v3.id as idStartKm, v3.numberVal as StartKm,
                v4.id as idFinishKm, v4.numberVal as FinishKm,
                v5.id as idStartPicket, v5.numberVal as StartPicket,
                v6.id as idFinishPicket, v6.numberVal as FinishPicket,
                v7.id as idPlanDateEnd, v7.dateTimeVal as PlanDateEnd,
                v8.id as idUser, v8.propVal as pvUser, v8.obj as objUser, null as nameUser,
                v9.id as idCreatedAt, v9.dateTimeVal as CreatedAt,
                v10.id as idUpdatedAt, v10.dateTimeVal as UpdatedAt,
                v11.id as idWork, v11.propVal as pvWork, v11.obj as objWork,
                    null as nameClsWork, null as fullNameWork
            from Obj o 
                left join ObjVer v on o.id=v.ownerver and v.lastver=1
                left join DataProp d1 on d1.objorrelobj=o.id and d1.prop=:Prop_LocationClsSection
                inner join DataPropVal v1 on d1.id=v1.dataprop ${wheV1}
                left join DataProp d2 on d2.objorrelobj=o.id and d2.prop=:Prop_Object
                left join DataPropVal v2 on d2.id=v2.dataprop
                left join DataProp d3 on d3.objorrelobj=o.id and d3.prop=:Prop_StartKm
                left join DataPropVal v3 on d3.id=v3.dataprop
                left join DataProp d4 on d4.objorrelobj=o.id and d4.prop=:Prop_FinishKm
                left join DataPropVal v4 on d4.id=v4.dataprop
                left join DataProp d5 on d5.objorrelobj=o.id and d5.prop=:Prop_StartPicket
                left join DataPropVal v5 on d5.id=v5.dataprop
                left join DataProp d6 on d6.objorrelobj=o.id and d6.prop=:Prop_FinishPicket
                left join DataPropVal v6 on d6.id=v6.dataprop
                left join DataProp d7 on d7.objorrelobj=o.id and d7.prop=:Prop_PlanDateEnd
                inner join DataPropVal v7 on d7.id=v7.dataprop ${wheV7}
                left join DataProp d8 on d8.objorrelobj=o.id and d8.prop=:Prop_User
                left join DataPropVal v8 on d8.id=v8.dataprop
                left join DataProp d9 on d9.objorrelobj=o.id and d9.prop=:Prop_CreatedAt
                left join DataPropVal v9 on d9.id=v9.dataprop
                left join DataProp d10 on d10.objorrelobj=o.id and d10.prop=:Prop_UpdatedAt
                left join DataPropVal v10 on d10.id=v10.dataprop
                left join DataProp d11 on d11.objorrelobj=o.id and d11.prop=:Prop_Work
                left join DataPropVal v11 on d11.id=v11.dataprop
            where ${whe}
        """, map)
        //... Пересечение
        map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Typ", "Typ_Location", "")
        stCls = loadSqlMeta("""
            select c.id from Cls c where typ=${map.get("Typ_Location")}
        """, "")
        idsCls = stCls.getUniqueValues("id")
        Store stLocation = loadSqlService("""
            select o.id, v.name
            from Obj o, ObjVer v where o.id=v.ownerVer and v.lastVer=1 and o.cls in (${idsCls.join(",")})
        """, "", "orgstructuredata")
        StoreIndex indLocation = stLocation.getIndex("id")
        //
        map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Typ", "Typ_Work", "")
        stCls = loadSqlMeta("""
            select c.id, v.name from Cls c, ClsVer v where c.id=v.ownerVer and v.lastVer=1 and typ=${map.get("Typ_Work")}
        """, "")
        StoreIndex indCls = stCls.getIndex("id")
        idsCls = stCls.getUniqueValues("id")
        Store stWork = loadSqlService("""
            select o.id, o.cls, v.fullName, null as nameClsWork
            from Obj o, ObjVer v where o.id=v.ownerVer and v.lastVer=1 and o.cls in (${idsCls.join(",")})
        """, "", "orgstructuredata")
        for (StoreRecord r in stWork) {
            StoreRecord rec = indCls.get(r.getLong("cls"))
            if (rec != null)
                r.set("nameClsWork", rec.getString("name"))
        }
        StoreIndex indWork = stLocation.getIndex("id")
        //

        map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Typ", "Typ_Object", "")
        stCls = loadSqlMeta("""
            select c.id, v.name from Cls c, ClsVer v where c.id=v.ownerVer and v.lastVer=1 and typ=${map.get("Typ_Object")}
        """, "")
        indCls = stCls.getIndex("id")
        idsCls = stCls.getUniqueValues("id")
        Store stObject = loadSqlService("""
            select o.id, o.cls, v.fullName, null as nameClsObject
            from Obj o, ObjVer v where o.id=v.ownerVer and v.lastVer=1 and o.cls in (${idsCls.join(",")})
        """, "", "objectdata")

        for (StoreRecord r in stObject) {
            StoreRecord rec = indCls.get(r.getLong("cls"))
            if (rec != null)
                r.set("nameClsObject", rec.getString("name"))
        }
        StoreIndex indObject = stObject.getIndex("id")
        //
        for (StoreRecord r in st) {
            StoreRecord rCls = indClsWork.get(r.getLong("cls"))
            if (rCls != null)
                r.set("nameCls", rCls.getString("name"))
            StoreRecord rObj = indLocation.get(r.getLong("objLocationClsSection"))
            if (rObj != null)
                r.set("nameLocationClsSection", rObj.getString("name"))
            StoreRecord rWork = indWork.get(r.getLong("objWork"))
            if (rWork != null) {
                r.set("nameClsWork", rWork.getString("nameCls"))
                r.set("fullNameWork", rWork.getString("fullName"))
            }
            StoreRecord rObject = indObject.get(r.getLong("objObject"))
            if (rObject != null) {
                r.set("fullNameObject", rObject.getString("fullName"))
            }
        }
        //...
        return st
    }

    @DaoMethod
    Store savePlan(String mode, Map<String, Object> params) {
        VariantMap pms = new VariantMap(params)
        //
        long own
        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        Map<String, Object> par = new HashMap<>(pms)
        if (mode.equalsIgnoreCase("ins")) {
            // find cls(linkCls)
            long linkCls = pms.getLong("linkCls")
            Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Typ", "Typ_WorkType", "")
            if (map.isEmpty())
                throw new XError("NotFoundCod@Typ_WorkType")
            map.put("linkCls", linkCls)
            Store stTmp = loadSqlMeta("""
                with fv as (
                    select cls,
                    string_agg (cast(factorval as varchar(2000)), ',' order by factorval) as fvlist
                    from clsfactorval
                    where cls=${linkCls}
                    group by cls
                )
                select * from (
                    select c.cls,
                    string_agg (cast(c.factorval as varchar(1000)), ',' order by factorval) as fvlist
                    from clsfactorval c, factor f  
                    where c.factorval =f.id and c.cls in (
                        select id from Cls where typ=${map.get("Typ_WorkType")}
                    )
                    group by c.cls
                ) t where t.fvlist in (select fv.fvlist from fv)
            """, "")

            long cls
            if (stTmp.size() > 0)
                cls = stTmp.get(0).getLong("cls")
            else {
                throw new XError("Не найден класс сответствующий классу {0}", linkCls)
            }

            par.put("cls", cls)
            own = eu.insertEntity(par)
            pms.put("own", own)
            //1 Prop_LocationClsSection
            if (pms.containsKey("objLocationClsSection"))
                fillProperties(true, "Prop_LocationClsSection", pms)
            //2 Prop_Work
            if (pms.containsKey("objWork"))
                fillProperties(true, "Prop_Work", pms)
            //3 Prop_Object
            if (pms.containsKey("objObject"))
                fillProperties(true, "Prop_Object", pms)
            //4 Prop_User
            if (pms.containsKey("objUser"))
                fillProperties(true, "Prop_User", pms)
            //5 Prop_StartKm
            if (pms.containsKey("StartKm"))
                fillProperties(true, "Prop_StartKm", pms)
            //6 Prop_FinishKm
            if (pms.containsKey("FinishKm"))
                fillProperties(true, "Prop_FinishKm", pms)
            //7 Prop_StartPicket
            if (pms.containsKey("StartPicket"))
                fillProperties(true, "Prop_StartPicket", pms)
            //8 Prop_FinishPicket
            if (pms.containsKey("FinishPicket"))
                fillProperties(true, "Prop_FinishPicket", pms)
            //9 Prop_PlanDateEnd
            if (pms.containsKey("PlanDateEnd"))
                fillProperties(true, "Prop_PlanDateEnd", pms)
            //10 Prop_CreatedAt
            if (pms.containsKey("CreatedAt"))
                fillProperties(true, "Prop_CreatedAt", pms)
            //11 Prop_UpdatedAt
            if (pms.containsKey("UpdatedAt"))
                fillProperties(true, "Prop_UpdatedAt", pms)
        } else if (mode.equalsIgnoreCase("upd")) {
            own = pms.getLong("id")
            eu.updateEntity(par)
            //
            pms.put("own", own)

            //1 Prop_ObjectType
            if (pms.containsKey("idLocationClsSection"))
                updateProperties("Prop_LocationClsSection", pms)
            else {
                if (pms.containsKey("objLocationClsSection"))
                    fillProperties(true, "Prop_ObjectType", pms)
            }
            //2 Prop_Work
            if (pms.containsKey("idWork"))
                updateProperties("Prop_Work", pms)
            else {
                if (pms.containsKey("objWork"))
                    fillProperties(true, "Prop_Work", pms)
            }
            //3 Prop_Object
            if (pms.containsKey("idObject"))
                updateProperties("Prop_Object", pms)
            else {
                if (pms.containsKey("objObject"))
                    fillProperties(true, "Prop_Object", pms)
            }
            //4 Prop_User
            if (pms.containsKey("idUser"))
                updateProperties("Prop_User", pms)
            else {
                if (pms.containsKey("objUser"))
                    fillProperties(true, "Prop_User", pms)
            }
            //5 Prop_StartKm
            if (pms.containsKey("idStartKm"))
                updateProperties("Prop_StartKm", pms)
            else {
                if (pms.containsKey("StartKm"))
                    fillProperties(true, "Prop_StartKm", pms)
            }
            //6 Prop_FinishKm
            if (pms.containsKey("idFinishKm"))
                updateProperties("Prop_FinishKm", pms)
            else {
                if (pms.containsKey("FinishKm"))
                    fillProperties(true, "Prop_FinishKm", pms)
            }
            //7 Prop_StartPicket
            if (pms.containsKey("idStartPicket"))
                updateProperties("Prop_StartPicket", pms)
            else {
                if (pms.containsKey("StartPicket"))
                    fillProperties(true, "Prop_StartPicket", pms)
            }
            //8 Prop_FinishPicket
            if (pms.containsKey("idFinishPicket"))
                updateProperties("Prop_FinishPicket", pms)
            else {
                if (pms.containsKey("FinishPicket"))
                    fillProperties(true, "Prop_FinishPicket", pms)
            }
            //9 Prop_PlanDateEnd
            if (pms.containsKey("idPlanDateEnd"))
                updateProperties("Prop_PlanDateEnd", pms)
            else {
                if (pms.containsKey("PlanDateEnd"))
                    fillProperties(true, "Prop_PlanDateEnd", pms)
            }
            //10 Prop_CreatedAt
            if (pms.containsKey("idCreatedAt"))
                updateProperties("Prop_CreatedAt", pms)
            else {
                if (pms.containsKey("CreatedAt"))
                    fillProperties(true, "Prop_CreatedAt", pms)
            }
            //11 Prop_UpdatedAt
            if (pms.containsKey("idUpdatedAt"))
                updateProperties("Prop_UpdatedAt", pms)
            else {
                if (pms.containsKey("UpdatedAt"))
                    fillProperties(true, "Prop_UpdatedAt", pms)
            }
        } else {
            throw new XError("Нейзвестный режим сохранения ('ins', 'upd')")
        }

        Map<String, Object> mapRez = new HashMap<>()
        mapRez.put("id", own)
        return loadPlan(mapRez)
    }

    /**
     *
     * @param id Id Obj
     * Delete object with properties
     */
    @DaoMethod
    void deleteObjWithProperties(long id) {
        validateForDeleteOwner(id)
        //
        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        mdb.execQueryNative("""
            delete from DataPropVal
            where dataProp in (select id from DataProp where isobj=1 and objorrelobj=${id});
            delete from DataProp where id in (
                select id from dataprop
                except
                select dataProp as id from DataPropVal
            );
        """)
        //
        eu.deleteEntity(id)
    }

    @DaoMethod
    Store loadWorkForSelect(long id) {
        //id: from OrgStructure idLocation (Typ_Location)

        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "Prop_LocationMulti", "")

        Store stNSI = loadSqlService("""
            select d.objOrRelObj as owner
            from DataProp d, DataPropval v
            where d.id=v.dataProp and d.prop=${map.get("Prop_LocationMulti")} and v.obj=${id}
        """, "", "nsidata")
        Set<Object> ids = stNSI.getUniqueValues("owner")
        map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "Prop_Collections", "")
        stNSI = loadSqlService("""
            select d.objOrRelObj as owner
            from DataProp d, DataPropval v
            where d.id=v.dataProp and d.prop=${map.get("Prop_Collections")} and v.obj in (0${ids.join(",")})
        """, "", "nsidata")
        Set<Object> idsWork = stNSI.getUniqueValues("owner")
        Store st = loadSqlService("""
            select o.id, o.cls, v.fullName, null as pv
            from Obj o, ObjVer v
            where o.id=v.ownerVer and v.lastVer=1 and o.id in (0${idsWork.join(",")})
        """, "", "nsidata")

        for (StoreRecord r in st) {
            long pv = apiMeta().get(ApiMeta).idPV("cls", r.getLong("cls"), "Prop_Work")
            r.set("pv", pv)
        }
        return st
    }

    @DaoMethod
    Store loadObjectServedForSelect(long id) {
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("RelTyp", "RT_Works", "")
        Store stTyp = loadSqlMeta("""
            select typ from reltypmember
            where reltyp=${map.get("RT_Works")}
            order by ord
        """, "")
        long typ1 = stTyp.get(0).getLong("typ")
        long typ2 = stTyp.get(1).getLong("typ")

        Store stRCM1 = loadSqlMeta("""
            select distinct cls 
            from relclsmember
            where cls in (
                select id from Cls where typ=${typ1}
            )
        """, "")

        Set<Object> idsCls1 = stRCM1.getUniqueValues("cls")
        Store stRCM2 = loadSqlMeta("""
            select distinct cls 
            from relclsmember
            where cls in (
                select id from Cls where typ=${typ2}
            )
        """, "")
        Set<Object> idsCls2 = stRCM2.getUniqueValues("cls")

        Store stTmp = loadSqlService("""
            select obj from relobjmember
            where cls in (${idsCls2.join(",")})
                and relobj in (
                    select relobj from relobjmember
                    where cls in (${idsCls1.join(",")}) and obj=${id}
                )
        """, "", "nsidata")

        Set<Object> idsObj = stTmp.getUniqueValues("obj")

        map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "Prop_ObjectType", "")
        stTmp = loadSqlService("""
            select d.objOrRelObj as owner
            from DataProp d, DataPropval v
            where d.id=v.dataProp and d.prop=${map.get("Prop_ObjectType")} and v.obj in (0${idsObj.join(",")})
        """, "", "objectdata")
        Set<Object> owners = stTmp.getUniqueValues("owner")




    }

    private void validateForDeleteOwner(long owner) {
        //---< check data in other DB
        Store stObj = mdb.loadQuery("""
            select o.cls, v.name from Obj o, ObjVer v where o.id=v.ownerVer and v.lastVer=1 and o.id=${owner}
        """)
        if (stObj.size() > 0) {
            //
            List<String> lstService = new ArrayList<>()
            long cls = stObj.get(0).getLong("cls")
            String name = stObj.get(0).getString("name")
            Store stPV = loadSqlMeta("""
                select id from PropVal where cls=${cls}
            """, "")
            Set<Object> idsPV = stPV.getUniqueValues("id")
            if (stPV.size() > 0) {
                Store stData = loadSqlService("""
                    select id from DataPropVal
                    where propval in (${idsPV.join(",")}) and obj=${owner}
                """, "", "nsidata")
                if (stData.size() > 0)
                    lstService.add("nsidata")
                //
                stData = loadSqlService("""
                    select id from DataPropVal
                    where propval in (${idsPV.join(",")}) and obj=${owner}
                """, "", "objectdata")
                if (stData.size() > 0)
                    lstService.add("objectdata")
                //
                stData = loadSqlService("""
                    select id from DataPropVal
                    where propval in (${idsPV.join(",")}) and obj=${owner}
                """, "", "orgstructuredata")
                if (stData.size() > 0)
                    lstService.add("orgstructuredata")
                //
                stData = loadSqlService("""
                    select id from DataPropVal
                    where propval in (${idsPV.join(",")}) and obj=${owner}
                """, "", "personnaldata")
                if (stData.size() > 0)
                    lstService.add("personnaldata")
                //
                stData = loadSqlService("""
                    select id from DataPropVal
                    where propval in (${idsPV.join(",")}) and obj=${owner}
                """, "", "plandata")
                if (stData.size() > 0)
                    lstService.add("plandata")
                if (lstService.size()>0) {
                    throw new XError("${name} используется в ["+ lstService.join(", ") + "]")
                }

            }
        }
    }

    //-------------------------
    private void fillProperties(boolean isObj, String cod, Map<String, Object> params) {
        long own = UtCnv.toLong(params.get("own"))
        String keyValue = cod.split("_")[1]
        def objRef = UtCnv.toLong(params.get("obj"+keyValue))
        def propVal = UtCnv.toLong(params.get("pv"+keyValue))

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

        //
        long idDP
        StoreRecord recDP = mdb.createStoreRecord("DataProp")
        String whe = isObj ? "and isObj=1 " : "and isObj=0 "
        if (stProp.get(0).getLong("statusFactor") > 0) {
            long fv = apiMeta().get(ApiMeta).getDefaultStatus(prop)
            whe += "and status = ${fv} "
        } else {
            whe += "and status is null "
        }
        whe += "and provider is null "
        //todo if (stProp.get(0).getLong("providerTyp") > 0)

        if (stProp.get(0).getLong("providerTyp") > 0) {
            whe += "and periodType is not null "
        } else {
            whe += "and periodType is null"
        }
        Store stDP = mdb.loadQuery("""
            select * from DataProp
            where objOrRelObj=${own} and prop=${prop} ${whe}
        """)
        if (stDP.size() > 0) {
            idDP = stDP.get(0).getLong("id")
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
                recDP.set("periodType", FD_PeriodType_consts.year)
            }
            idDP = mdb.insertRec("DataProp", recDP, true)
        }
        //
        StoreRecord recDPV = mdb.createStoreRecord("DataPropVal")
        recDPV.set("dataProp", idDP)
        // Attrib
        if ([FD_AttribValType_consts.str].contains(attribValType)) {
            if ( cod.equalsIgnoreCase("Prop_TabNumber") ||
                    cod.equalsIgnoreCase("Prop_UserSecondName") ||
                    cod.equalsIgnoreCase("Prop_UserFirstName") ||
                    cod.equalsIgnoreCase("Prop_UserMiddleName") ||
                    cod.equalsIgnoreCase("Prop_UserEmail") ||
                    cod.equalsIgnoreCase("Prop_UserPhone") ||
                    cod.equalsIgnoreCase("Prop_UserId")) {
                if (params.get(keyValue) != null || params.get(keyValue) != "") {
                    recDPV.set("strVal", UtCnv.toString(params.get(keyValue)))
                }
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }
        //
        if ([FD_AttribValType_consts.multistr].contains(attribValType)) {
            if ( cod.equalsIgnoreCase("Prop_Description")) {
                if (params.get(keyValue) != null || params.get(keyValue) != "") {
                    recDPV.set("multiStrVal", UtCnv.toString(params.get(keyValue)))
                }
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }

        if ([FD_AttribValType_consts.dt].contains(attribValType)) {
            if (cod.equalsIgnoreCase("Prop_CreatedAt") ||
                    cod.equalsIgnoreCase("Prop_UpdatedAt") ||
                    cod.equalsIgnoreCase("Prop_DateEmployment") ||
                    cod.equalsIgnoreCase("Prop_DateDismissal") ||
                    cod.equalsIgnoreCase("Prop_UserDateBirth")) {
                if (params.get(keyValue) != null || params.get(keyValue) != "") {
                    recDPV.set("dateTimeVal", UtCnv.toString(params.get(keyValue)))
                }
            } else
                throw new XError("for dev: [${cod}] отсутствует в реализации")
        }

        // For FV
        if ([FD_PropType_consts.factor].contains(propType)) {
            if ( cod.equalsIgnoreCase("Prop_UserSex") ||
                    cod.equalsIgnoreCase("Prop_Position")) {
                if (propVal > 0) {
                    recDPV.set("propVal", propVal)
                }
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }

        // For Measure
        if ([FD_PropType_consts.measure].contains(propType)) {
            if ( cod.equalsIgnoreCase("Prop_ParamsMeasure")) {
                if (propVal > 0) {
                    recDPV.set("propVal", propVal)
                }
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }

        // For Meter
        if ([FD_PropType_consts.meter, FD_PropType_consts.rate].contains(propType)) {
            if (cod.equalsIgnoreCase("Prop_StartKm")) { // template
                if (params.get(keyValue) != null || params.get(keyValue) != "") {
                    double v = UtCnv.toDouble(params.get(keyValue))
                    v = v / koef
                    if (digit) v = v.round(digit)
                    recDPV.set("numberval", v)
                }
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }
        // For Typ
        if ([FD_PropType_consts.typ].contains(propType)) {
            if (cod.equalsIgnoreCase("Prop_Location")) {
                if (objRef > 0) {
                    recDPV.set("propVal", propVal)
                    recDPV.set("obj", objRef)
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

        long au = getUser()
        recDPV.set("authUser", au)
        recDPV.set("inputType", FD_InputType_consts.app)
        long idDPV = mdb.getNextId("DataPropVal")
        recDPV.set("id", idDPV)
        recDPV.set("ord", idDPV)
        recDPV.set("timeStamp", XDateTime.create(new Date()).toString(XDateTimeFormatter.ISO_DATE_TIME))
        mdb.insertRec("DataPropVal", recDPV, false)

    }

    private void updateProperties(String cod, Map<String, Object> params) {
        VariantMap mapProp = new VariantMap(params)
        String keyValue = cod.split("_")[1]
        long idVal = mapProp.getLong("id" + keyValue)
        long propVal = mapProp.getLong("pv" + keyValue)
        long objRef = mapProp.getLong("obj" + keyValue)
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
        def strValue = mapProp.getString(keyValue)
        // For Attrib
        if ([FD_AttribValType_consts.str].contains(attribValType)) {
            if (cod.equalsIgnoreCase("Prop_TabNumber") ||
                    cod.equalsIgnoreCase("Prop_UserSecondName") ||
                    cod.equalsIgnoreCase("Prop_UserFirstName") ||
                    cod.equalsIgnoreCase("Prop_UserMiddleName") ||
                    cod.equalsIgnoreCase("Prop_UserEmail") ||
                    cod.equalsIgnoreCase("Prop_UserPhone") ||
                    cod.equalsIgnoreCase("Prop_UserId")) {
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

        if ([FD_AttribValType_consts.multistr].contains(attribValType)) {
            if ( cod.equalsIgnoreCase("Prop_Description")) {
                if (params.get(keyValue) != null) {
                    sql = "update DataPropval set multiStrVal='${strValue}', timeStamp='${tmst}' where id=${idVal}"
                }
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }

        if ([FD_AttribValType_consts.dt].contains(attribValType)) {
            if (cod.equalsIgnoreCase("Prop_CreatedAt") ||
                    cod.equalsIgnoreCase("Prop_UpdatedAt") ||
                    cod.equalsIgnoreCase("Prop_DateEmployment") ||
                    cod.equalsIgnoreCase("Prop_DateDismissal") ||
                    cod.equalsIgnoreCase("Prop_UserDateBirth")) {
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
                    sql = "update DataPropval set dateTimeVal='${strValue}', timeStamp='${tmst}' where id=${idVal}"
                }
            } else
                throw new XError("for dev: [${cod}] отсутствует в реализации")
        }

        // For FV
        if ([FD_PropType_consts.factor].contains(propType)) {
            if ( cod.equalsIgnoreCase("Prop_UserSex") ||
                    cod.equalsIgnoreCase("Prop_Position")) {
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

        // For Measure
        if ([FD_PropType_consts.measure].contains(propType)) {
            if ( cod.equalsIgnoreCase("Prop_ParamsMeasure") ) {
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
            if (cod.equalsIgnoreCase("Prop_StartKm")) {
                if (mapProp.keySet().contains(keyValue) && mapProp[keyValue] != 0) {
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
        // For Typ
        if ([FD_PropType_consts.typ].contains(propType)) {
            if (cod.equalsIgnoreCase("Prop_Location")) {
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

        mdb.execQueryNative(sql)
    }

    private Store loadSqlMeta(String sql, String domain) {
        return apiMeta().get(ApiMeta).loadSql(sql, domain)
    }

    private Store loadSqlService(String sql, String domain, String model) {
        if (model.equalsIgnoreCase("userdata"))
            return apiUserData().get(ApiUserData).loadSql(sql, domain)
        else if (model.equalsIgnoreCase("nsidata"))
            return apiNSIData().get(ApiNSIData).loadSql(sql, domain)
        else if (model.equalsIgnoreCase("personnaldata"))
            return apiPersonnalData().get(ApiPersonnalData).loadSql(sql, domain)
        else if (model.equalsIgnoreCase("orgstructuredata"))
            return apiOrgStructureData().get(ApiOrgStructureData).loadSql(sql, domain)
        else if (model.equalsIgnoreCase("objectdata"))
            return apiObjectData().get(ApiObjectData).loadSql(sql, domain)
        else
            throw new XError("Unknown model [${model}]")
    }

    private long getUser() throws Exception {
        AuthService authSvc = mdb.getApp().bean(AuthService.class)
        long au = authSvc.getCurrentUser().getAttrs().getLong("id")
        if (au == 0)
            au = 1//throw new XError("notLogined")
        return au
    }


}
