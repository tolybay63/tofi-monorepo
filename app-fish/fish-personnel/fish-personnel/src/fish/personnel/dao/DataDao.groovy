package fish.personnel.dao

import groovy.transform.CompileStatic
import jandcode.commons.UtCnv
import jandcode.commons.UtString
import jandcode.commons.datetime.XDate
import jandcode.commons.datetime.XDateTime
import jandcode.commons.datetime.XDateTimeFormatter
import jandcode.commons.error.XError
import jandcode.commons.variant.VariantMap
import jandcode.core.auth.AuthService
import jandcode.core.auth.AuthUser
import jandcode.core.dao.DaoMethod
import jandcode.core.dbm.mdb.BaseMdbUtils
import jandcode.core.dbm.sql.SqlText
import jandcode.core.store.Store
import jandcode.core.store.StoreIndex
import jandcode.core.store.StoreRecord
import tofi.api.adm.ApiAdm
import tofi.api.dta.ApiMonitoringData
import tofi.api.dta.ApiPersonnelData
import tofi.api.dta.ApiUserData
import tofi.api.dta.model.utils.EntityMdbUtils
import tofi.api.dta.model.utils.PeriodGenerator
import tofi.api.dta.model.utils.UtPeriod
import tofi.api.mdl.ApiMeta
import tofi.api.mdl.model.consts.FD_AttribValType_consts
import tofi.api.mdl.model.consts.FD_InputType_consts
import tofi.api.mdl.model.consts.FD_PeriodType_consts
import tofi.api.mdl.model.consts.FD_PropType_consts
import tofi.apinator.ApinatorApi
import tofi.apinator.ApinatorService

@CompileStatic
class DataDao extends BaseMdbUtils {

    ApinatorApi apiMeta() { return app.bean(ApinatorService).getApi("meta") }
    ApinatorApi apiAdm() { return app.bean(ApinatorService).getApi("adm") }
    ApinatorApi apiUserData() { return app.bean(ApinatorService).getApi("userdata") }
    ApinatorApi apiPersonnelData() { return app.bean(ApinatorService).getApi("personneldata") }
    ApinatorApi apiMonitoringData() { return app.bean(ApinatorService).getApi("monitoringdata") }

    //----------------------------------------------------------------------------//

    //---------------------------------- Branch --------------------------------- //
    @DaoMethod
    Store loadClsTree(Map<String, Object> params) {
        return apiMeta().get(ApiMeta).loadClsTree(params)
    }

    @DaoMethod
    Store loadObj(long cls) {
        Store st = mdb.createStore("Obj.full")
        mdb.loadQuery(st, """
            select o.*, v.name, v.fullName, v.objParent as parent, ov1.name as namePerent
            from Obj o
            join ObjVer v on o.id=v.ownerVer and v.lastVer=1
            left join ObjVer ov1 on ov1.ownerVer=v.objParent and ov1.lastVer=1 
            where o.cls=:c
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

    @DaoMethod
    void deletePersonnel(long id) {
        //checkForExistData(id, 1)
        Store st = loadSqlService("""
            select distinct v.name 
            from Obj o
            join ObjVer v on o.id=v.ownerVer and v.lastVer=1
            join DataProp d1 on d1.objorrelobj=o.id-- and d1.prop in (1047,1048)
            join DataPropVal v1 on d1.id=v1.dataProp
            where v1.obj=${id}
        """, "", "monitoringdata")

        if (st.size() > 0) {
            String msg = st.getUniqueValues("name").join(";\n\r")
            throw new XError("Cотрудник используется в следующих ловлах:\n\r${msg}")
        }

        deleteOwnerWithProperties(id, 1)
    }

    private void deleteOwnerWithProperties(long id, int isObj) {
        if (id == 0)
            throw new XError("Не указан [id]")
        if (isObj > 1)
            throw new XError("Некорректный [isObj]")

        String tableName = isObj == 1 ? "Obj" : "RelObj"
        EntityMdbUtils eu = new EntityMdbUtils(mdb, tableName)
        //
        mdb.execQueryNative("""
            delete from DataPropVal
            where dataProp in (select id from DataProp where isobj=${isObj} and objorrelobj=${id});
            delete from DataProp where id in (
                select id from dataprop
                except
                select dataProp as id from DataPropVal
            );
        """)
        //
        if (tableName.equalsIgnoreCase("RelObj")) {
            try {
                mdb.execQueryNative("""
                    delete from RelObjMember
                    where relobj=${id};
                """)
            } finally {
                eu.deleteEntity(id)
            }
        } else {
            eu.deleteEntity(id)
        }
    }

    //---------------------------------- Personnel --------------------------------- //
    @DaoMethod
    Map<String, Object> loadPersonnel(Map<String, Object> params) throws Exception {
        checkTarget("personnel");
        String filter = UtCnv.toString(params.get("filter")).trim()
        Map<String, Long> mapProp = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "", "Prop_User%")
        Map<String, Long> mapCls = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", "Cls_Personnel", "")
        mapProp.putAll(mapCls)
        //count
        String sql = """
            select count(*) as cnt 
            from Obj o
                join DataProp d1 on d1.isObj=1 and d1.objOrRelobj=o.id and d1.prop=:Prop_UserSecondName
                join DataPropVal v1 on d1.id=v1.dataProp
                join DataProp d2 on d2.isObj=1 and d2.objOrRelobj=o.id and d2.prop=:Prop_UserFirstName
                join DataPropVal v2 on d2.id=v2.dataProp
                join DataProp d4 on d4.isObj=1 and d4.objOrRelobj=o.id and d4.prop=:Prop_UserSex
                join DataPropVal v4 on d4.id=v4.dataProp
                join DataProp d5 on d5.isObj=1 and d5.objOrRelobj=o.id and d5.prop=:Prop_UserPosition
                join DataPropVal v5 on d5.id=v5.dataProp
                join DataProp d6 on d6.isObj=1 and d6.objOrRelobj=o.id and d6.prop=:Prop_UserOrg
                join DataPropVal v6 on d6.id=v6.dataProp
            where o.cls=:Cls_Personnel
        """
        SqlText sqlText = getMdb().createSqlText(sql)
        sqlText.setSql(sql)
        if (!filter.isEmpty())
            sqlText = sqlText.addWhere("""
                v1.strVal like '%${filter}%' or v2.strVal like '%${filter}%'
            """)
        int total = mdb.loadQuery(sqlText as String, mapProp as Map<String, Object>).get(0).getInt("cnt")
        //

        sql = """
            select
                (v1.strVal || ' ' || v2.strVal || case when coalesce(v3.strVal, '')='' then '' else ' ' || v3.strVal end) as fio,            
                o.id as own, o.cls, 
                v1.strVal as UserSecondName, v1.id as idUserSecondName,  
                v2.strVal as UserFirstName, v2.id as idUserFirstName,
                v3.strVal as UserMiddleName, v3.id as idUserMiddleName,
                v4.id as idUserSex, v4.propVal as pvUserSex, null as fvUserSex, null as nameUserSex,
                v5.id as idUserPosition, v5.propVal as pvUserPosition, null as fvUserPosition, null as nameUserPosition,
                v6.id as idUserOrg, v6.obj as objUserOrg, null as nameUserOrg,
                v7.dateTimeVal::date as UserDateBirth, v7.id as idUserDateBirth,
                v8.strVal as UserEmail, v8.id as idUserEmail,
                v9.strVal as UserPhone, v9.id as idUserPhone,
                v10.strVal as UserId, v10.id as idUserId
            from Obj o
                join DataProp d1 on d1.isObj=1 and d1.objOrRelobj=o.id and d1.prop=:Prop_UserSecondName
                join DataPropVal v1 on d1.id=v1.dataProp
                join DataProp d2 on d2.isObj=1 and d2.objOrRelobj=o.id and d2.prop=:Prop_UserFirstName
                join DataPropVal v2 on d2.id=v2.dataProp
                left join DataProp d3 on d3.isObj=1 and d3.objOrRelobj=o.id and d3.prop=:Prop_UserMiddleName
                left join DataPropVal v3 on d3.id=v3.dataProp
                join DataProp d4 on d4.isObj=1 and d4.objOrRelobj=o.id and d4.prop=:Prop_UserSex
                join DataPropVal v4 on d4.id=v4.dataProp
                join DataProp d5 on d5.isObj=1 and d5.objOrRelobj=o.id and d5.prop=:Prop_UserPosition
                join DataPropVal v5 on d5.id=v5.dataProp
                join DataProp d6 on d6.isObj=1 and d6.objOrRelobj=o.id and d6.prop=:Prop_UserOrg
                join DataPropVal v6 on d6.id=v6.dataProp
                left join DataProp d7 on d7.isObj=1 and d7.objOrRelobj=o.id and d7.prop=:Prop_UserDateBirth
                left join DataPropVal v7 on d7.id=v7.dataProp
                left join DataProp d8 on d8.isObj=1 and d8.objOrRelobj=o.id and d8.prop=:Prop_UserEmail
                left join DataPropVal v8 on d8.id=v8.dataProp
                left join DataProp d9 on d9.isObj=1 and d9.objOrRelobj=o.id and d9.prop=:Prop_UserPhone
                left join DataPropVal v9 on d9.id=v9.dataProp
                left join DataProp d10 on d10.isObj=1 and d10.objOrRelobj=o.id and d10.prop=:Prop_UserId
                left join DataPropVal v10 on d10.id=v10.dataProp
            where o.cls=:Cls_Personnel
            order by v1.strVal
        """

        sqlText = getMdb().createSqlText(sql)
        Map<String, Object> par = new HashMap<>()
        int pg = UtCnv.toInt(params.get("page"))
        int limit = UtCnv.toInt(params.get("limit"))
        limit = limit == 0 ? total : limit
        int offset = (pg - 1) * limit
        par.put("offset", offset)
        par.put("limit", limit)
        sqlText.setSql(sql)
        sqlText.paginate(true)

        if (!UtCnv.toString(params.get("orderBy")).trim().isEmpty())
            sqlText = sqlText.replaceOrderBy(UtCnv.toString(params.get("orderBy")))


        if (!filter.isEmpty())
            sqlText = sqlText.addWhere("""
                v1.strVal like '%${filter}%' or v2.strVal like '%${filter}%'
            """)

        Store st = getMdb().createStore("Personnel")
        mdb.loadQuery(st, sqlText as String, mapProp as Map<String, Object>)
        //
        Store stFV = apiMeta().get(ApiMeta).storeFVfromPropVal()
        StoreIndex indFV = stFV.getIndex("propval")
        Set<Object> objIds = st.getUniqueValues("objUserOrg")
        Store stObj = mdb.loadQuery("""
            select o.id, v.name from Obj o, ObjVer v
            where o.id=v.ownerVer and v.lastVer=1 and o.id in (0${objIds.join(",")})
        """)
        StoreIndex indObj = stObj.getIndex("id")
        //
        for (StoreRecord r in st) {
            StoreRecord rec = indFV.get(r.getLong("pvUserSex"))
            if (rec != null) {
                r.set("fvUserSex", rec.getLong("factorval"))
                r.set("nameUserSex", rec.getString("name"))
            }
            rec = indFV.get(r.getLong("pvUserPosition"))
            if (rec != null) {
                r.set("fvUserPosition", rec.getLong("factorval"))
                r.set("nameUserPosition", rec.getString("name"))
            }
            rec = indObj.get(r.getLong("objUserOrg"))
            if (rec != null)
                r.set("nameUserOrg", rec.getString("name"))
        }
        //
        Map<String, Object> meta = new HashMap<>()
        meta.put("total", total)
        meta.put("page", pg)
        meta.put("limit", limit)

        Map<String, Object> mapRes = new HashMap<>()
        mapRes.put("store", st)
        mapRes.put("meta", meta)
        return mapRes
    }

    Store loadPersonnelRec(long id) throws Exception {
        Map<String, Long> mapProp = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "", "Prop_User%")
        mapProp.put("obj", id)
        String sql = """
            select
                (v1.strVal || ' ' || v2.strVal || case when coalesce(v3.strVal, '')='' then '' else ' ' || v3.strVal end) as fio,
                o.id as own, o.cls, 
                v1.strVal as UserSecondName, v1.id as idUserSecondName,  
                v2.strVal as UserFirstName, v2.id as idUserFirstName,
                v3.strVal as UserMiddleName, v3.id as idUserMiddleName,
                v4.id as idUserSex, v4.propVal as pvUserSex, null as fvUserSex, null as nameUserSex,
                v5.id as idUserPosition, v5.propVal as pvUserPosition, null as fvUserPosition, null as nameUserPosition,
                v6.id as idUserOrg, v6.obj as objUserOrg, null as nameUserOrg,
                v7.dateTimeVal::date as UserDateBirth, v7.id as idUserDateBirth,
                v8.strVal as UserEmail, v8.id as idUserEmail,
                v9.strVal as UserPhone, v9.id as idUserPhone,
                v10.strVal as UserId, v10.id as idUserId
            from Obj o
                join DataProp d1 on d1.isObj=1 and d1.objOrRelobj=o.id and d1.prop=:Prop_UserSecondName
                join DataPropVal v1 on d1.id=v1.dataProp
                join DataProp d2 on d2.isObj=1 and d2.objOrRelobj=o.id and d2.prop=:Prop_UserFirstName
                join DataPropVal v2 on d2.id=v2.dataProp
                left join DataProp d3 on d3.isObj=1 and d3.objOrRelobj=o.id and d3.prop=:Prop_UserMiddleName
                left join DataPropVal v3 on d3.id=v3.dataProp
                join DataProp d4 on d4.isObj=1 and d4.objOrRelobj=o.id and d4.prop=:Prop_UserSex
                join DataPropVal v4 on d4.id=v4.dataProp
                join DataProp d5 on d5.isObj=1 and d5.objOrRelobj=o.id and d5.prop=:Prop_UserPosition
                join DataPropVal v5 on d5.id=v5.dataProp
                join DataProp d6 on d6.isObj=1 and d6.objOrRelobj=o.id and d6.prop=:Prop_UserOrg
                join DataPropVal v6 on d6.id=v6.dataProp
                left join DataProp d7 on d7.isObj=1 and d7.objOrRelobj=o.id and d7.prop=:Prop_UserDateBirth
                left join DataPropVal v7 on d7.id=v7.dataProp
                left join DataProp d8 on d8.isObj=1 and d8.objOrRelobj=o.id and d8.prop=:Prop_UserEmail
                left join DataPropVal v8 on d8.id=v8.dataProp
                left join DataProp d9 on d9.isObj=1 and d9.objOrRelobj=o.id and d9.prop=:Prop_UserPhone
                left join DataPropVal v9 on d9.id=v9.dataProp
                left join DataProp d10 on d10.isObj=1 and d10.objOrRelobj=o.id and d10.prop=:Prop_UserId
                left join DataPropVal v10 on d10.id=v10.dataProp                 
            where o.id=:obj
        """
        Store st = getMdb().createStore("Personnel")
        mdb.loadQuery(st, sql, mapProp)
        //
        Store stFV = apiMeta().get(ApiMeta).storeFVfromPropVal()
        StoreIndex indFV = stFV.getIndex("propval")
        Set<Object> setObj = st.getUniqueValues("objUserOrg")
        Store stObj = mdb.loadQuery("""
            select o.id, v.name from Obj o, ObjVer v
            where o.id=v.ownerVer and v.lastVer=1 and o.id in (0${setObj.join(",")})
        """)
        StoreIndex indObj = stObj.getIndex("id")
        for (StoreRecord r in st) {
            StoreRecord rec = indFV.get(r.getLong("pvUserSex"))
            if (rec != null) {
                r.set("fvUserSex", rec.getLong("factorval"))
                r.set("nameUserSex", rec.getString("name"))
            }
            rec = indFV.get(r.getLong("pvUserPosition"))
            if (rec != null) {
                r.set("fvUserPosition", rec.getLong("factorval"))
                r.set("nameUserPosition", rec.getString("name"))
            }
            //
            rec = indObj.get(r.getLong("objUserOrg"))
            if (rec != null)
                r.set("nameUserOrg", rec.getString("name"))
        }

        return st
    }

    @DaoMethod
    StoreRecord newRec() {
        //checkTarget("default-target")
        Store st = getMdb().createStore("Personnel")
        return st.add()
    }

    private void checkUser(long user, long own) {
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", "Cls_Personnel", "")
        long cls = map.get("Cls_Personnel")
        map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "Prop_UserId", "")
        map.put("cls", cls)
        Store st = mdb.loadQuery("""
            select v1.strVal::numeric::bigint as user, o.id as own
            from Obj o
                left join DataProp d1 on d1.isObj=1 and d1.objOrRelobj=o.id and d1.prop=:Prop_UserId
                left join DataPropVal v1 on d1.id=v1.dataProp
            where o.cls=:cls
        """, map)
        for (StoreRecord r in st) {
            if (r.getLong("user") == user && r.getLong("own") != own) {
                throw new XError("Указанный пользователь уже назначен другому сотруднику")
            }
        }
    }

    @DaoMethod
    Store savePersonnel(String mode, Map<String, Object> params) {
        VariantMap pms = new VariantMap(params)
        long own = pms.getLong("own")
        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        Map<String, Object> par = new HashMap<>(pms)
        String fn = pms.getString("UserSecondName").trim() + " " + pms.getString("UserFirstName").trim()
        if (!pms.getString("UserMiddleName").trim().isEmpty())
            fn = fn + " " + pms.getString("UserMiddleName").trim()


        String mn = pms.getString("UserMiddleName").trim()
        String nm = pms.getString("UserSecondName").trim() + " " + UtString.capFirst(pms.getString("UserFirstName").trim()).substring(0,1) + "."
        if (!mn.isEmpty())
            nm = nm + UtString.capFirst(mn).substring(0,1) + "."
        par.put("name", nm)
        par.put("fullName", fn)
        Map<String, Long> mapCls = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", "Cls_Personnel", "")
        par.put("cls", mapCls.get("Cls_Personnel"))
        if (own > 0 && pms.getLong("UserId") > 0)
            checkUser(pms.getLong("UserId"), own)
        if (mode == "ins") {
            own = eu.insertEntity(par)
            pms.put("own", own)
            //
            //1 Prop_UserSecondName
            fillProperties(true, "Prop_UserSecondName", pms)
            //2 Prop_UserFirstName
            fillProperties(true, "Prop_UserFirstName", pms)
            //3 UserMiddleName
            if (pms.containsKey("UserMiddleName"))
                fillProperties(true, "Prop_UserMiddleName", pms)

            //4 Prop_UserSex
            fillProperties(true, "Prop_UserSex", pms)
            //5 Prop_UserPosition
            fillProperties(true, "Prop_UserPosition", pms)
            //6 Prop_UserOrg
            fillProperties(true, "Prop_UserOrg", pms)
            //7 UserMiddleName
            if (pms.containsKey("UserDateBirth"))
                fillProperties(true, "Prop_UserDateBirth", pms)
            //8 UserMiddleName
            if (pms.containsKey("UserEmail"))
                fillProperties(true, "Prop_UserEmail", pms)
            //9 UserMiddleName
            if (pms.containsKey("UserPhone"))
                fillProperties(true, "Prop_UserPhone", pms)
            //10 UserMiddleName
            if (pms.containsKey("UserId"))
                fillProperties(true, "Prop_UserId", pms)

        } else if (mode == "upd") {
            par.put("id", own)
            eu.updateEntity(par)
            pms.put("own", own)
            //
            //1 Prop_UserSecondName
            updateProperties("Prop_UserSecondName", pms)
            //2 Prop_UserFirstName
            updateProperties("Prop_UserFirstName", pms)
            //3 UserMiddleName
            if (pms.getLong("idUserMiddleName") > 0)
                updateProperties("Prop_UserMiddleName", pms)
            else {
                if (!pms.getString("UserMiddleName").isEmpty())
                    fillProperties(true, "Prop_UserMiddleName", pms)
            }
            //4 Prop_UserSex
            updateProperties("Prop_UserSex", pms)
            //5 Prop_UserPosition
            updateProperties("Prop_UserPosition", pms)
            //6 Prop_UserOrg
            updateProperties("Prop_UserOrg", pms)
            //7 UserDateBirth
            if (pms.getLong("idUserDateBirth") > 0)
                updateProperties("Prop_UserDateBirth", pms)
            else {
                if (!pms.getString("UserDateBirth").isEmpty())
                    fillProperties(true, "Prop_UserDateBirth", pms)
            }
            //8 UserEmail
            if (pms.getLong("idUserEmail") > 0)
                updateProperties("Prop_UserEmail", pms)
            else {
                if (!pms.getString("UserEmail").isEmpty())
                    fillProperties(true, "Prop_UserEmail", pms)
            }
            //9 UserPhone
            if (pms.getLong("idUserPhone") > 0)
                updateProperties("Prop_UserPhone", pms)
            else {
                if (!pms.getString("UserPhone").isEmpty())
                    fillProperties(true, "Prop_UserPhone", pms)
            }
            //10 UserId
            if (pms.getLong("idUserId") > 0)
                updateProperties("Prop_UserId", pms)
            else {
                if (!pms.getString("UserId").isEmpty())
                    fillProperties(true, "Prop_UserId", pms)
            }
        } else {
            throw new XError("Не известный режим ввода [${mode}]")
        }


        return loadPersonnelRec(own)
    }

    @DaoMethod
    Store selectFV(String codProp) {
        return apiMeta().get(ApiMeta).storePropValForSelectFV(codProp)
    }

    @DaoMethod
    Store selectObj(String codProp) {
        Set<Object> setCls = apiMeta().get(ApiMeta).setIdsOfClsFromPV(codProp)
        Map<Long, Long> mapCls = apiMeta().get(ApiMeta).mapEntityIdFromPV("cls", false)
        Store st = mdb.loadQuery("""
            select o.id, o.cls, v.name, v.objParent as parent, null as pv
            from Obj o, ObjVer v
            where o.id=v.ownerVer and v.lastVer=1 and o.cls in (${setCls.join(",")})
        """)
        for (StoreRecord r in st) {
            r.set("pv", mapCls.get(r.getLong("cls")))
        }
        return st
    }

    @DaoMethod
    Store selectUser() {
        return apiAdm().get(ApiAdm).loadSql("""
            select id, fullName as name
            from AuthUser
            where id<>1 and locked<>1
        """, "")
    }

    @DaoMethod
    Map<Long, String> mapFvNameFromId() {
        return apiMeta().get(ApiMeta).mapFvNameFromId()
    }

    //------------------
    @DaoMethod
    Store loadEnterprise(String codTyp) {
        Set<Object> idsCls = apiMeta().get(ApiMeta).setIdsOfCls(codTyp)
        if (idsCls.size() == 0)
            idsCls.add(0L)

        Store stCls = apiMeta().get(ApiMeta).loadSql("""
            select c.id, v.name
            from Cls c, ClsVer v
            where c.id=v.ownerVer and v.lastVer=1 and c.id in (${idsCls.join(",")})
        """, "")

        StoreIndex indCls = stCls.getIndex("id")

        Store st = mdb.loadQuery("""
            select o.id, v.objparent as parent, v.name, v.fullname, o.cls, null as namecls, v.cmtver as cmt 
            from obj o, objver v  
            where o.id=v.ownerver and v.lastVer=1 and o.cls in (${idsCls.join(",")})
            order by o.ord
        """)

        for (StoreRecord r in st) {
            StoreRecord rec = indCls.get(r.getLong("cls"))
            if (rec != null) {
                r.set("namecls", rec.getString("name"))
            }
        }
        return st
    }

    @DaoMethod
    Store loadCls(String codTyp, String flag) {
        Set<Object> idsCls = apiMeta().get(ApiMeta).setIdsOfCls(codTyp)
        if (idsCls.size() == 0)
            idsCls.add(0L)

        String sql = """
            select cls
            from clsfactorval
            where cls in (${idsCls.join(",")})
            group by cls
            having count(*) = 1
        """
        if (flag.equalsIgnoreCase("childs")) {
            sql = """
                    select cls
                    from clsfactorval
                    where cls in (${idsCls.join(",")})
                    group by cls
                    having count(*) > 1
                """
        }
        Store stCls = apiMeta().get(ApiMeta).loadSql(sql, "")
        Set<Object> setCls = stCls.getUniqueValues("cls")
        return apiMeta().get(ApiMeta).loadSql("""
            select c.id, v.name
            from Cls c, ClsVer v
            where c.id=v.ownerVer and v.lastVer=1 and c.id in (${setCls.join(",")})        
        """, "")
    }

    @DaoMethod
    void insertEnterprise(Map<String, Object> rec) {
        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        long id = eu.insertEntity(rec)
    }

    @DaoMethod
    void updateEnterprise(Map<String, Object> rec) {
        long id = UtCnv.toLong(rec.get("id"))
        if (id==0)
            throw new XError("id=0")
        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        eu.updateEntity(rec)
    }

    @DaoMethod
    void deleteEnterprise(long id) {
        checkForExistData(id, 1)
        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        eu.deleteEntity(id)
    }


//-----------------------------------------------------------------------------------------------//
    private void fillProperties(boolean isObj, String cod, Map<String, Object> params) {
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
            if (cod.equalsIgnoreCase("Prop_UserSecondName") ||
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
        if ([FD_AttribValType_consts.dt].contains(attribValType)) {
            if (cod.equalsIgnoreCase("Prop_UserDateBirth")) {
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
            if (cod.equalsIgnoreCase("Prop_UserOrg")) {
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
            if (cod.equalsIgnoreCase("Prop_UserSex") ||
                    cod.equalsIgnoreCase("Prop_UserPosition")) {
                if (propVal > 0) {
                    recDPV.set("propVal", propVal)
                }
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }
        // For Meter
        if ([FD_PropType_consts.meter, FD_PropType_consts.rate].contains(propType)) {
            if (cod.equalsIgnoreCase("Prop_WaterArea")) {
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

    private void updateProperties(String cod, Map<String, Object> params) {
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
            if (cod.equalsIgnoreCase("Prop_UserSecondName") ||
                    cod.equalsIgnoreCase("Prop_UserFirstName") ||
                    cod.equalsIgnoreCase("Prop_UserMiddleName") ||
                    cod.equalsIgnoreCase("Prop_UserEmail") ||
                    cod.equalsIgnoreCase("Prop_UserPhone") ||
                    cod.equalsIgnoreCase("Prop_UserId")) {
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
            if (cod.equalsIgnoreCase("Prop_UserDateBirth")) {
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
            if (cod.equalsIgnoreCase("Prop_UserSex") ||
                    cod.equalsIgnoreCase("Prop_UserPosition")) {
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
            if (cod.equalsIgnoreCase("Prop_WaterArea")) {
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
            if (cod.equalsIgnoreCase("Prop_UserOrg")) {
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
        if (model.equalsIgnoreCase("userdata"))
            return apiUserData().get(ApiUserData).loadSql(sql, domain)
        else if (model.equalsIgnoreCase("personneldata"))
            return apiPersonnelData().get(ApiPersonnelData).loadSql(sql, domain)
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


    @DaoMethod
    void checkTarget(String target) {
        AuthService authService = getModel().getApp().bean(AuthService.class);
        AuthUser usr = authService.getCurrentUser();

        if (getApp().getEnv().isDev()) {
            System.out.println("--- DEBUG ---");
            System.out.println("Target: " + target);
            System.out.println("User ID from Attrs: " + usr.getAttrs().getLong("id"));
            System.out.println("User Login: " + usr.getAttrs().getString("login"));
            System.out.println("-------------");
        }

        if (usr.getAttrs().getLong("id") == 1) return;

        if (usr.getAttrs().getLong("id") == 0)
            throw new XError("notLoginned");

        String userTargets = usr.getAttrs().getString("target", "");
        String[] targets = userTargets.trim().split("\\s*,\\s*");
        if (!Arrays.asList(targets).contains(target)) {
            if (Arrays.asList("dtj", "adm", "meta", "nsi").contains(target)) {
                throw new XError("notAccessService");
            }
            throw new XError("notAccess");
        }
    }


}
