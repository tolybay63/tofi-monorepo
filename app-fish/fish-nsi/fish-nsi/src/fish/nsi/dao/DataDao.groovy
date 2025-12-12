package fish.nsi.dao

import groovy.transform.CompileStatic
import jandcode.commons.UtCnv
import jandcode.commons.UtFile
import jandcode.commons.conf.Conf
import jandcode.commons.datetime.XDate
import jandcode.commons.datetime.XDateTime
import jandcode.commons.datetime.XDateTimeFormatter
import jandcode.commons.error.XError
import jandcode.commons.variant.VariantMap
import jandcode.core.auth.AuthService
import jandcode.core.auth.AuthUser
import jandcode.core.dao.DaoMethod
import jandcode.core.dbm.mdb.BaseMdbUtils
import jandcode.core.std.DataDirService
import jandcode.core.store.Store
import jandcode.core.store.StoreIndex
import jandcode.core.store.StoreRecord
import tofi.api.dta.ApiMonitoringData
import tofi.api.dta.ApiNSIData
import tofi.api.dta.ApiUserData
import tofi.api.dta.model.utils.EntityMdbUtils
import tofi.api.dta.model.utils.UtPeriod
import tofi.api.mdl.ApiMeta
import tofi.api.mdl.ApiMetaFish
import tofi.api.mdl.model.consts.FD_AttribValType_consts
import tofi.api.mdl.model.consts.FD_InputType_consts
import tofi.api.mdl.model.consts.FD_PeriodType_consts
import tofi.api.mdl.model.consts.FD_PropType_consts
import tofi.api.mdl.utils.dbfilestorage.DbFileStorageItem
import tofi.api.mdl.utils.dbfilestorage.DbFileStorageService
import tofi.apinator.ApinatorApi
import tofi.apinator.ApinatorService

import java.nio.file.Files
import java.nio.file.Paths


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

    ApinatorApi apiMonitoringData() {
        return app.bean(ApinatorService).getApi("monitoringdata")
    }

    ApinatorApi apiMetaFish() {
        return app.bean(ApinatorService).getApi("meta")
    }

    //-------------------------

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

    void is_exist_owner_as_data(long owner, int isObj, long propVal) {
        List<String> lstApp = new ArrayList<>()
        if (isObj == 1) {
            boolean b = apiUserData().get(ApiUserData).is_exist_entity_as_dataOld(owner, "obj", propVal)
            if (b) lstApp.add("userdata")
            b = apiNSIData().get(ApiNSIData).is_exist_entity_as_dataOld(owner, "obj", propVal)
            if (b) lstApp.add("nsidata")
            b = apiMonitoringData().get(ApiMonitoringData).is_exist_entity_as_dataOld(owner, "obj", propVal)
            if (b) lstApp.add("monitoringdata")
        } else {
            boolean b = apiUserData().get(ApiUserData).is_exist_entity_as_dataOld(owner, "relobj", propVal)
            if (b) lstApp.add("userdata")
            b = apiNSIData().get(ApiNSIData).is_exist_entity_as_dataOld(owner, "relobj", propVal)
            if (b) lstApp.add("nsidata")
            b = apiMonitoringData().get(ApiMonitoringData).is_exist_entity_as_dataOld(owner, "relobj", propVal)
            if (b) lstApp.add("monitoringdata")
        }
        //...
        String msg = lstApp.join(", ")
        if (lstApp.size() > 0)
            throw new XError("UseInApp@"+msg)
    }

    void validateForDeleteOwner(long owner, int isObj) {
        long clsORrelcls = apiNSIData().get(ApiNSIData).getClsOrRelCls(owner, isObj)
        Map<Long, Long> mapPV
        if (isObj==1)
            mapPV = apiMeta().get(ApiMeta).mapEntityIdFromPV("cls", false)
        else
            mapPV = apiMeta().get(ApiMeta).mapEntityIdFromPV("relcls", false)
        if (mapPV.containsKey(clsORrelcls))
            is_exist_owner_as_data(owner, isObj, mapPV.get(clsORrelcls))
    }

    /*
        delete Owner without properties
    */
    @DaoMethod
    void deleteOwner(long id, int isObj) {
        //
        validateForDeleteOwner(id, isObj)
        //
        String tableName = isObj==1 ? "Obj" : "RelObj"
        EntityMdbUtils eu = new EntityMdbUtils(mdb, tableName)
        eu.deleteEntity(id)
    }

    /*
        delete Owner with properties
    */
    @DaoMethod
    void deleteOwnerWithProperties(long id, int isObj) {
        //
        validateForDeleteOwner(id, isObj)
        //
        String tableName = isObj==1 ? "Obj" : "RelObj"
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

    @DaoMethod
    StoreRecord insertKATO(Map<String, Object> rec) {
        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        long own = eu.insertEntity(rec)
        return loadObjRec(own)
    }

    @DaoMethod
    StoreRecord updateKATO(Map<String, Object> rec) {
        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        long id = UtCnv.toLong(rec.get("id"))
        eu.updateEntity(rec)
        return loadObjRec(id)
    }

    StoreRecord loadObjRec(long obj) {
        StoreRecord st = mdb.createStoreRecord("Obj.full")
        mdb.loadQueryRecord(st, """
            select o.*, v.name, v.fullName, v.objParent as parent from Obj o, ObjVer v
            where o.id=v.ownerVer and v.lastVer=1 and o.id=:o
        """, [o: obj])
        return st
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
    Map<String, Long> getClsIds(String codCls) {
        if (codCls=="")
            return apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", "", "Cls_%")
        else
            return apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", codCls, "")
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
    Store loadObjWithCls(String codTyp) {
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Typ", codTyp, "")
        if (map.isEmpty())
            throw new XError("NotFoundCod@${codTyp}")

        Set<Object> setIdsCls = apiMeta().get(ApiMeta).setIdsOfCls(codTyp)
        if (setIdsCls.empty) setIdsCls.add(0L)
        String whe = setIdsCls.join(",")
        Store st = mdb.createStore("Obj.cust")
        mdb.loadQuery(st, """
            select o.id, v.objParent as parent, o.accesslevel, o.cls, v.name, null as nameCls, v.cmtver as cmt
            from Obj o, ObjVer v
            where o.id=v.ownerver and v.lastver=1 and o.cls in (${whe})
            order by o.ord
        """)
        Store stCls = apiMeta().get(ApiMeta).loadCls(codTyp)
        StoreIndex indCls = stCls.getIndex("id")
        for (StoreRecord r in st) {
            StoreRecord rec = indCls.get(r.getLong("cls"))
            if (rec != null) {
                r.set("nameCls", rec.getString("name"))
            }
        }
        return st
    }

    @DaoMethod
    Store insertObj(Map<String, Object> rec) {
        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        long own = eu.insertEntity(rec)
        return loadObjWithClsRec(own)
    }

    @DaoMethod
    Store updateObj(Map<String, Object> rec) {
        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        long id = UtCnv.toLong(rec.get("id"))
        eu.updateEntity(rec)
        return loadObjWithClsRec(id)
    }

    Store loadObjWithClsRec(long obj) {
        Store st = mdb.createStore("Obj.cust")
        mdb.loadQuery(st, """
            select o.id, v.objParent as parent, o.accesslevel, o.cls, v.name, null as nameCls, v.cmtver as cmt
            from Obj o, ObjVer v
            where o.id=v.ownerver and v.lastver=1 and o.id=${obj}
            order by o.ord
        """)

        Store stCls = apiMeta().get(ApiMeta).recEntity("Cls", st.get(0).getLong("cls"))
        String nameCls = stCls.get(0).getString("name")
        for (StoreRecord r in st) {
            r.set("nameCls", nameCls)
        }
        return st
    }

    @DaoMethod
    Store loadClsForSelect(String codTyp) {
        return apiMeta().get(ApiMeta).loadClsForSelect(codTyp)
    }

    @DaoMethod
    Store loadRegDocumentsCls(String codTyp) {
        Store st = apiMeta().get(ApiMeta).loadCls(codTyp)
        if (st.size()==0)
            throw new XError("NotFoundCod@${codTyp}")
        return st
    }

    @DaoMethod
    Store loadRegDocuments(long objORcls, boolean isRec) {
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "", "Prop_RegDocuments%")
        map.put("objORcls", objORcls)
        String whe = "o.cls=:objORcls"
        if (isRec) whe = "o.id=:objORcls"
        Store st = mdb.createStore("Obj.regDocs")
        return mdb.loadQuery(st, """
            select o.id as obj, 
                v1.id as idRegDocumentsNameDoc, v1.strVal as RegDocumentsNameDoc,
                v2.id as idRegDocumentsAuthorDoc, v2.strVal as RegDocumentsAuthorDoc,
                v3.id as idRegDocumentsNumberOrder, v3.strVal as RegDocumentsNumberOrder,
                v4.id as idRegDocumentsNumberDoc, v4.strVal as RegDocumentsNumberDoc,
                v5.id as idRegDocumentsDateApproval, v5.datetimeVal::date as RegDocumentsDateApproval,
                v6.id as idRegDocumentsLifeDoc, v6.datetimeVal::date as RegDocumentsLifeDoc,
                (select count(v.fileVal) from DataProp d left join DataPropVal v on d.id=v.dataprop
                                where d.isobj=1 and d.objorrelobj=o.id and d.prop=:Prop_RegDocumentsFile
                                ) as hasFile
            from Obj o
                left join DataProp d1 on d1.isobj=1 and d1.objorrelobj=o.id and d1.prop=:Prop_RegDocumentsNameDoc
                left join DataPropVal v1 on d1.id=v1.dataprop
                left join DataProp d2 on d2.isobj=1 and d2.objorrelobj=o.id and d2.prop=:Prop_RegDocumentsAuthorDoc
                left join DataPropVal v2 on d2.id=v2.dataprop
                left join DataProp d3 on d3.isobj=1 and d3.objorrelobj=o.id and d3.prop=:Prop_RegDocumentsNumberOrder
                left join DataPropVal v3 on d3.id=v3.dataprop
                left join DataProp d4 on d4.isobj=1 and d4.objorrelobj=o.id and d4.prop=:Prop_RegDocumentsNumberDoc
                left join DataPropVal v4 on d4.id=v4.dataprop
                left join DataProp d5 on d5.isobj=1 and d5.objorrelobj=o.id and d5.prop=:Prop_RegDocumentsDateApproval
                left join DataPropVal v5 on d5.id=v5.dataprop
                left join DataProp d6 on d6.isobj=1 and d6.objorrelobj=o.id and d6.prop=:Prop_RegDocumentsLifeDoc
                left join DataPropVal v6 on d6.id=v6.dataprop
            where ${whe}
        """, map)
    }

    @DaoMethod
    Store loadAttachedFiles(long obj, String propCod) {
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", propCod, "")
        if (map.isEmpty())
            throw new XError("NotFoundCod@${propCod}")
        map.put("id", obj)
        Store st = mdb.createStore("Obj.file")
        mdb.loadQuery(st, """
            select d.objorrelobj as obj, v.id as idDPV, v.fileVal, null as fileName, v.cmt
            from DataProp d, DataPropVal v 
            where d.id=v.dataprop and d.isobj=1 and d.objorrelobj=:id and d.prop=:${propCod}
        """, map)
        Set<Object> ids = st.getUniqueValues("fileVal")
        if (ids.isEmpty()) ids.add(0L)
        String whe = ids.join(",")
        Store stFS = apiMeta().get(ApiMeta).loadSql("""
            select id, originalfilename as filename from DbFileStorage where id in (${whe})
        """, "")
        StoreIndex indFS = stFS.getIndex("id")
        for (StoreRecord r : st) {
            StoreRecord rr = indFS.get(r.getLong("fileVal"))
            if (rr != null) {
                r.set("fileName", rr.getString("filename"))
            }
        }
        return st
    }

    @DaoMethod
    void deleteFileValue(Map<String, Object> rec) {
        String path
        try {
            path = mdb.getApp().bean(DataDirService.class).getPath("dbfilestorage")
        } catch (Exception e) {
            path = ""
            e.printStackTrace()
        }

        String bucketName = ""
        if (path == "") {
            try {
                Conf conf2 = mdb.getApp().getConf().getConf("datadir/minio")
                bucketName = conf2.getString("bucketName")
            } catch (Exception e) {
                bucketName = ""
                e.printStackTrace()
            }
        }

        if (path != "" && bucketName == "") {
            deleteFileValueFS(rec)
        } else if (path == "" && bucketName != "") {
            deleteFileValueMinio(rec)
        } else {
            throw new XError("FileStorage не настроен!")
        }
    }

    private void deleteFileValueFS(Map<String, Object> params) {
        long fileId = UtCnv.toLong(params.get("fileVal"))
        long id = UtCnv.toLong(params.get("idDPV"))

        try {
            DbFileStorageService dfsrv = apiMeta().get(ApiMeta).getDbFileStorageService()
            dfsrv.setModelName(UtCnv.toString(params.get("model")))
            dfsrv.removeFile(fileId)
        } finally {
            String sql = """
                delete from DataPropVal where id=${id};
                with d as (
                    select id from DataProp
                    except
                    select dataProp as id from DataPropVal
                )
                delete from DataProp where id in (select id from d);
            """
            execSql(sql, UtCnv.toString(params.get("model")))
        }
    }

    private static void deleteFileValueMinio(Map<String, Object> params) {
        throw new XError("MinIO не настроен!")
    }

    @DaoMethod
    Store saveObjProperties(Map<String, Object> params) {
        VariantMap pms = new VariantMap(params)
        long own
        pms.putIfAbsent("RegDocumentsDateApproval", "1800-01-01")
        pms.putIfAbsent("RegDocumentsLifeDoc", "3333-12-31")
        if (pms["RegDocumentsDateApproval"] == "")
            pms.put("RegDocumentsDateApproval", "1800-01-01")
        if (pms["RegDocumentsLifeDoc"] == "")
            pms.put("RegDocumentsLifeDoc", "3333-12-31")

        if (pms.getDate("RegDocumentsDateApproval").toJavaLocalDate()
                .isAfter(pms.getDate("RegDocumentsLifeDoc").toJavaLocalDate())) {
            throw new XError("invalidLifeDoc")
        }

        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        Map<String, Object> par = new HashMap<>(pms)
        par.put("name", pms.get("RegDocumentsNameDoc"))
        par.put("fullName", pms.get("RegDocumentsNameDoc"))
        if (pms.getString("mode").equalsIgnoreCase("ins")) {
            own = eu.insertEntity(par)
            pms.put("own", own)
            //1 RegDocumentsNameDoc
            fillProperties(true, "Prop_RegDocumentsNameDoc", pms)
            //2 RegDocumentsNumberDoc
            fillProperties(true, "Prop_RegDocumentsNumberDoc", pms)
            //3 RegDocumentsAuthorDoc
            fillProperties(true, "Prop_RegDocumentsAuthorDoc", pms)
            //4 RegDocumentsNumberOrder
            fillProperties(true, "Prop_RegDocumentsNumberOrder", pms)
            //5 RegDocumentsDateApproval
            fillProperties(true, "Prop_RegDocumentsDateApproval", pms)
            //6 RegDocumentsLifeDoc
            fillProperties(true, "Prop_RegDocumentsLifeDoc", pms)
        } else {
            own = pms.getLong("obj")
            par.put("id", own)
            eu.updateEntity(par)
            //
            pms.put("own", own)
            //1 RegDocumentsNameDoc
            if (params.containsKey("idRegDocumentsNameDoc"))
                updateProperties("Prop_RegDocumentsNameDoc", pms)
            else
                fillProperties(true, "Prop_RegDocumentsNameDoc", pms)
            //2 RegDocumentsNumberDoc
            if (params.containsKey("idRegDocumentsNumberDoc"))
                updateProperties("Prop_RegDocumentsNumberDoc", pms)
            else
                fillProperties(true, "Prop_RegDocumentsNumberDoc", pms)
            //3 RegDocumentsAuthorDoc
            if (params.containsKey("idRegDocumentsAuthorDoc"))
                updateProperties("Prop_RegDocumentsAuthorDoc", pms)
            else
                fillProperties(true, "Prop_RegDocumentsAuthorDoc", pms)
            //4 RegDocumentsNumberOrder
            if (params.containsKey("idRegDocumentsNumberOrder"))
                updateProperties("Prop_RegDocumentsNumberOrder", pms)
            else
                fillProperties(true, "Prop_RegDocumentsNumberOrder", pms)
            //5 RegDocumentsDateApproval
            if (params.containsKey("idRegDocumentsDateApproval"))
                updateProperties("Prop_RegDocumentsDateApproval", pms)
            else
                fillProperties(true, "Prop_RegDocumentsDateApproval", pms)
            //6 RegDocumentsDateApproval
            if (params.containsKey("idRegDocumentsLifeDoc"))
                updateProperties("Prop_RegDocumentsLifeDoc", pms)
            else
                fillProperties(true, "Prop_RegDocumentsLifeDoc", pms)
        }
        return loadRegDocuments(own, true)
    }

    private void fillProperties(boolean isObj, String cod, Map<String, Object> params) {
        long own = UtCnv.toLong(params.get("own"))
        String keyValue = cod.split("_")[1]
        //def objRef = UtCnv.toLong(params.get("obj"+keyValue))
        def propVal = UtCnv.toLong(params.get("pv"+keyValue))

        Store stProp = apiMeta().get(ApiMeta).getPropInfo(cod)
        //
        long prop = stProp.get(0).getLong("id")
        long propType = stProp.get(0).getLong("propType")
        long attribValType = stProp.get(0).getLong("attribValType")
        Integer digit
        double koef = stProp.get(0).getDouble("koef")
        if (koef == 0) koef = 1
        if (stProp.get(0).isValueNull("digit"))
            digit = null
        else
            digit = stProp.get(0).getInt("digit")

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
        if (stProp.get(0).getBoolean("dependperiod")) {
            recDP.set("periodType", FD_PeriodType_consts.year)
        }

        long idDP = mdb.insertRec("DataProp", recDP, true)
        //
        StoreRecord recDPV = mdb.createStoreRecord("DataPropVal")
        recDPV.set("dataProp", idDP)
        // Attrib
        if ([FD_AttribValType_consts.str].contains(attribValType)) {
            if ( cod.equalsIgnoreCase("Prop_RegDocumentsNameDoc") ||
                    cod.equalsIgnoreCase("Prop_RegDocumentsNumberDoc") ||
                        cod.equalsIgnoreCase("Prop_RegDocumentsAuthorDoc") ||
                            cod.equalsIgnoreCase("Prop_RegDocumentsNumberOrder") )
            {
                recDPV.set("strVal", UtCnv.toString(params.get(keyValue)))
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }
        //
        if ([FD_AttribValType_consts.dt].contains(attribValType)) {
            if ( cod.equalsIgnoreCase("Prop_RegDocumentsDateApproval") ||
                    cod.equalsIgnoreCase("Prop_RegDocumentsLifeDoc") ||
                        cod.equalsIgnoreCase("Prop_MethodApprovalDate") ) {
                recDPV.set("dateTimeVal", UtCnv.toString(params.get(keyValue)))
            } else
                throw new XError("for dev: [${cod}] отсутствует в реализации")
        }
        // For FV
        if ([FD_PropType_consts.factor].contains(propType)) {
            if ( cod.equalsIgnoreCase("Prop_MethodStatus") ) {
                recDPV.set("propVal", propVal)
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

        Store stProp = apiMeta().get(ApiMeta).getPropInfo(cod)
        //
        long propType = stProp.get(0).getLong("propType")
        long attribValType = stProp.get(0).getLong("attribValType")

        String sql = ""
        if ([FD_AttribValType_consts.str].contains(attribValType)) {
            if ( cod.equalsIgnoreCase("Prop_RegDocumentsNameDoc") ||
                    cod.equalsIgnoreCase("Prop_RegDocumentsNumberDoc") ||
                    cod.equalsIgnoreCase("Prop_RegDocumentsAuthorDoc") ||
                    cod.equalsIgnoreCase("Prop_RegDocumentsNumberOrder") )
            {
                def strValue = mapProp.getString(keyValue)
                sql = "update DataPropval set strVal='${strValue}' where id=${idVal}"
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }
        if ([FD_AttribValType_consts.dt].contains(attribValType)) {
            if ( cod.equalsIgnoreCase("Prop_RegDocumentsDateApproval") ||
                    cod.equalsIgnoreCase("Prop_RegDocumentsLifeDoc") ||
                        cod.equalsIgnoreCase("Prop_MethodApprovalDate")) {
                def dtValue = mapProp.getString(keyValue)
                sql = "update DataPropval set dateTimeVal='${dtValue}' where id=${idVal}"
            } else
                throw new XError("for dev: [${cod}] отсутствует в реализации")
        }
        // For FV
        if ([FD_PropType_consts.factor].contains(propType)) {
            if ( cod.equalsIgnoreCase("Prop_MethodStatus")) {
                if (mapProp.getLong("fv"+keyValue) > 0)
                    sql = "update DataPropval set propVal=${propVal} where id=${idVal}"
                else
                    sql = "update DataPropval set propVal=null where id=${idVal}"
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }


        mdb.execQueryNative(sql)

    }

    private void execSql(String sql, String model) {
        if (model.equalsIgnoreCase("userdata"))
            apiUserData().get(ApiUserData).execSql(sql)
        else if (model.equalsIgnoreCase("nsidata"))
            apiNSIData().get(ApiNSIData).execSql(sql)
        else
            throw new XError("Unknown model [${model}]")
    }

    @DaoMethod
    String getPathFile(long id) {

        DbFileStorageService dfsrv = apiMeta().get(ApiMeta).getDbFileStorageService()
        dfsrv.setModelName(UtCnv.toString("nsidata"))
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
        String pathFile = '/pdf/'+ dfsi.originalFilename

        return pathFile

    }

    @DaoMethod
    Store loadFvMethodologyForSelect(String codFactor) {
        return loadFvForSelect(codFactor)
    }

    private Store loadFvForSelect(String codFactor) {
        return apiMetaFish().get(ApiMetaFish).loadFvForSelect(codFactor)
    }

    @DaoMethod
    Map<Long, String> mapFvNameFromId() {
        return apiMeta().get(ApiMeta).mapFvNameFromId()
    }

    @DaoMethod
    Map<String, Long> mapFvApproved() {
        return apiMeta().get(ApiMeta).getIdFromCodOfEntity("Factor", "FV_Approved", "")
    }


    @DaoMethod
    Store loadMethodology(String codCls, boolean isRec, long idObj) {
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "", "Prop_Method%")
        if (map.isEmpty())
            throw new XError("NotFoundCod@Prop_Method%")
        String whe = "o.cls = :${codCls}"
        if (!isRec) {
            Map<String, Long> map2 = apiMeta().get(ApiMeta).getIdsFromCodOfEntity("Cls", "'${codCls}'")
            if (map2.isEmpty())
                throw new XError("NotFoundCod@${codCls}")
            map.put(codCls, map2.get(codCls))

        } else {
            map.put("obj", idObj)
            whe = "o.id = :obj"
        }

        Store st = mdb.createStore("Obj.methodology")
        mdb.loadQuery(st, """
            select o.id as obj, o.cls, v.name, null as nameCls,
                v1.id as idMethodStatus, v1.propval as pvMethodStatus, null as fvMethodStatus,
                v2.id as idMethodApprovalDate, v2.dateTimeVal as MethodApprovalDate,
                (select count(v.fileVal) from DataProp d left join DataPropVal v on d.id=v.dataprop
                where d.isobj=1 and d.objorrelobj=o.id and d.prop=:Prop_MethodDiscription
                ) as hasFile
            from Obj o
                left join ObjVer v on o.id=v.ownerVer and v.lastVer=1
                left join DataProp d1 on d1.isobj=1 and d1.objorrelobj=o.id and d1.prop=:Prop_MethodStatus
                left join DataPropVal v1 on d1.id=v1.dataprop
                left join DataProp d2 on d2.isobj=1 and d2.objorrelobj=o.id and d2.prop=:Prop_MethodApprovalDate
                left join DataPropVal v2 on d2.id=v2.dataprop
            where ${whe}
        """, map)

        Map<Long, Long> mapPV = apiMeta().get(ApiMeta).mapEntityIdFromPV("factorVal", true)

        for (StoreRecord record in st) {
            record.set("fvMethodStatus", mapPV.get(record.getLong("pvMethodStatus")))
        }
        return st
    }

    @DaoMethod
    Store saveMethodologyProperties(Map<String, Object> params) {
        VariantMap pms = new VariantMap(params)
        long own
        pms.putIfAbsent("MethodApprovalDate", "1800-01-01")

        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        Map<String, Object> par = new HashMap<>(pms)
        par.put("fullName", pms.get("name"))
        if (pms.getString("mode").equalsIgnoreCase("ins")) {
            own = eu.insertEntity(par)
            pms.put("own", own)
            //1 MethodStatus
            fillProperties(true, "Prop_MethodStatus", pms)
            //2 MethodApprovalDate
            fillProperties(true, "Prop_MethodApprovalDate", pms)
        } else {
            own = pms.getLong("obj")
            par.put("id", own)
            eu.updateEntity(par)
            //
            pms.put("own", own)
            //1 MethodStatus
            if (params.containsKey("idMethodStatus"))
                updateProperties("Prop_MethodStatus", pms)
            else
                fillProperties(true, "Prop_MethodStatus", pms)
            //2 MethodApprovalDate
            if (params.containsKey("idMethodApprovalDate"))
                updateProperties("Prop_MethodApprovalDate", pms)
            else
                fillProperties(true, "Prop_MethodApprovalDate", pms)
        }
        return loadMethodology("", true, own)
    }

    @DaoMethod
    Store loadClsTree(Map<String, Object> params) {
        return apiMeta().get(ApiMeta).loadClsTree(params)
    }

    //-------------------------

    private Store loadSqlMeta(String sql, String domain) {
        return apiMeta().get(ApiMeta).loadSql(sql, domain)
    }

    private long insertRecToTable(String tableName, Map<String, Object> params, String model, String metamodel, boolean generateId) {
        if (metamodel=="fish") {
            if (model.equalsIgnoreCase("userdata"))
                return apiUserData().get(ApiUserData).insertRecToTable(tableName, params, generateId)
            else if (model.equalsIgnoreCase("nsidata"))
                return apiNSIData().get(ApiNSIData).insertRecToTable(tableName, params, generateId)
            else
                throw new XError("Unknown model [${model}]")
        }
        throw new XError("Unknown id metamodel")
    }

    private Store loadSql(String sql, String domain, String model, String metamodel) {
        if (metamodel=="fish") {
            if (model.equalsIgnoreCase("userdata"))
                return apiUserData().get(ApiUserData).loadSql(sql, domain)
            else if (model.equalsIgnoreCase("nsidata"))
                return apiNSIData().get(ApiNSIData).loadSql(sql, domain)
            else
                throw new XError("Unknown model [${model}]")
        }
        throw new XError("Unknown id metamodel")
    }

    @DaoMethod
    Map<String, Object> getCurUserInfo() {
        AuthService authSvc = mdb.getApp().bean(AuthService.class)
        AuthUser au = authSvc.getCurrentUser()
        if (au == null) {
            throw new XError("NotLogined")
        }
        return au.getAttrs()
    }

    private long getUser() throws Exception {
        AuthService authSvc = mdb.getApp().bean(AuthService.class)
        long au = authSvc.getCurrentUser().getAttrs().getLong("id")
        if (au == 0)
            throw new XError("notLogined")
        return au
    }


}
