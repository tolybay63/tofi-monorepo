package dtj.nsi.dao

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
import jandcode.core.std.CfgService
import jandcode.core.std.DataDirService
import jandcode.core.store.Store
import jandcode.core.store.StoreIndex
import jandcode.core.store.StoreRecord
import tofi.api.dta.ApiMonitoringData
import tofi.api.dta.ApiNSIData
import tofi.api.dta.ApiObjectData
import tofi.api.dta.ApiOrgStructureData
import tofi.api.dta.ApiPersonnalData
import tofi.api.dta.ApiPlanData
import tofi.api.dta.ApiUserData
import tofi.api.dta.model.utils.EntityMdbUtils
import tofi.api.dta.model.utils.UtPeriod
import tofi.api.mdl.ApiMeta
import tofi.api.mdl.ApiMetaFish
import tofi.api.mdl.model.consts.FD_AttribValType_consts
import tofi.api.mdl.model.consts.FD_InputType_consts
import tofi.api.mdl.model.consts.FD_PeriodType_consts
import tofi.api.mdl.model.consts.FD_PropType_consts
import tofi.api.mdl.utils.CartesianProduct
import tofi.api.mdl.utils.dbfilestorage.DbFileStorageItem
import tofi.api.mdl.utils.dbfilestorage.DbFileStorageService
import tofi.api.mdl.utils.tree.DataTreeNode
import tofi.api.mdl.utils.tree.ITreeNodeVisitor
import tofi.api.mdl.utils.tree.UtData
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
    ApinatorApi apiObjectData() {
        return app.bean(ApinatorService).getApi("objectdata")
    }
    ApinatorApi apiPlanData() {
        return app.bean(ApinatorService).getApi("plandata")
    }
    ApinatorApi apiPersonnalData() {
        return app.bean(ApinatorService).getApi("personnaldata")
    }
    ApinatorApi apiOrgStructureData() {
        return app.bean(ApinatorService).getApi("orgstructuredata")
    }


    //-------------------------

    void is_exist_owner_as_data(long owner, int isObj, String modelMeta) {
        Map<Long, Long> mapPV
        if (isObj==1)
            mapPV = apiMeta().get(ApiMeta).mapEntityIdFromPV("cls", false)
        else
            mapPV = apiMeta().get(ApiMeta).mapEntityIdFromPV("relcls", false)

        List<String> lstApp = new ArrayList<>()
        long clsORrelcls
        if (isObj == 1) {
            clsORrelcls = apiUserData().get(ApiUserData).getClsOrRelCls(owner, isObj)
            if (mapPV.containsKey(clsORrelcls)) {
                boolean b = apiUserData().get(ApiUserData).is_exist_entity_as_data(owner, "obj", mapPV.get(clsORrelcls))
                if (b) lstApp.add("userdata")
            }
            //
            clsORrelcls = apiNSIData().get(ApiNSIData).getClsOrRelCls(owner, isObj)
            if (mapPV.containsKey(clsORrelcls)) {
                boolean b = apiNSIData().get(ApiNSIData).is_exist_entity_as_data(owner, "obj", mapPV.get(clsORrelcls))
                if (b) lstApp.add("nsidata")
            }
            //
            if (modelMeta=="fish") {
                clsORrelcls = apiMonitoringData().get(ApiMonitoringData).getClsOrRelCls(owner, isObj)
                if (mapPV.containsKey(clsORrelcls)) {
                    boolean b = apiMonitoringData().get(ApiMonitoringData).is_exist_entity_as_data(owner, "obj", mapPV.get(clsORrelcls))
                    if (b) lstApp.add("monitoringdata")
                }
            }
            if (modelMeta=="dtj") {
                clsORrelcls = apiPersonnalData().get(ApiPersonnalData).getClsOrRelCls(owner, isObj)
                if (mapPV.containsKey(clsORrelcls)) {
                    boolean b = apiPersonnalData().get(ApiPersonnalData).is_exist_entity_as_data(owner, "obj", mapPV.get(clsORrelcls))
                    if (b) lstApp.add("personnaldata")
                }
                //
                clsORrelcls = apiPlanData().get(ApiPlanData).getClsOrRelCls(owner, isObj)
                if (mapPV.containsKey(clsORrelcls)) {
                    boolean b = apiPlanData().get(ApiPlanData).is_exist_entity_as_data(owner, "obj", mapPV.get(clsORrelcls))
                    if (b) lstApp.add("plandata")
                }
                //
                clsORrelcls = apiOrgStructureData().get(ApiOrgStructureData).getClsOrRelCls(owner, isObj)
                if (mapPV.containsKey(clsORrelcls)) {
                    boolean b = apiOrgStructureData().get(ApiOrgStructureData).is_exist_entity_as_data(owner, "obj", mapPV.get(clsORrelcls))
                    if (b) lstApp.add("orgstructuredata")
                }
                //
                clsORrelcls = apiObjectData().get(ApiObjectData).getClsOrRelCls(owner, isObj)
                if (mapPV.containsKey(clsORrelcls)) {
                    boolean b = apiObjectData().get(ApiObjectData).is_exist_entity_as_data(owner, "obj", mapPV.get(clsORrelcls))
                    if (b) lstApp.add("objectdata")
                }
            }
        } else {
            clsORrelcls = apiUserData().get(ApiUserData).getClsOrRelCls(owner, isObj)
            if (mapPV.containsKey(clsORrelcls)) {
                boolean b = apiUserData().get(ApiUserData).is_exist_entity_as_data(owner, "relobj", mapPV.get(clsORrelcls))
                if (b) lstApp.add("userdata")
            }
            //
            clsORrelcls = apiNSIData().get(ApiNSIData).getClsOrRelCls(owner, isObj)
            if (mapPV.containsKey(clsORrelcls)) {
                boolean b = apiNSIData().get(ApiNSIData).is_exist_entity_as_data(owner, "relobj", mapPV.get(clsORrelcls))
                if (b) lstApp.add("nsidata")
            }
            //
            if (modelMeta=="fish") {
                clsORrelcls = apiMonitoringData().get(ApiMonitoringData).getClsOrRelCls(owner, isObj)
                if (mapPV.containsKey(clsORrelcls)) {
                    boolean b = apiMonitoringData().get(ApiMonitoringData).is_exist_entity_as_data(owner, "relobj", mapPV.get(clsORrelcls))
                    if (b) lstApp.add("monitoringdata")
                }
            }
            if (modelMeta=="dtj") {
                clsORrelcls = apiPersonnalData().get(ApiPersonnalData).getClsOrRelCls(owner, isObj)
                if (mapPV.containsKey(clsORrelcls)) {
                    boolean b = apiPersonnalData().get(ApiPersonnalData).is_exist_entity_as_data(owner, "relobj", mapPV.get(clsORrelcls))
                    if (b) lstApp.add("personnaldata")
                }
                //
                clsORrelcls = apiPlanData().get(ApiPlanData).getClsOrRelCls(owner, isObj)
                if (mapPV.containsKey(clsORrelcls)) {
                    boolean b = apiPlanData().get(ApiPlanData).is_exist_entity_as_data(owner, "relobj", mapPV.get(clsORrelcls))
                    if (b) lstApp.add("plandata")
                }
                //
                clsORrelcls = apiOrgStructureData().get(ApiOrgStructureData).getClsOrRelCls(owner, isObj)
                if (mapPV.containsKey(clsORrelcls)) {
                    boolean b = apiOrgStructureData().get(ApiOrgStructureData).is_exist_entity_as_data(owner, "relobj", mapPV.get(clsORrelcls))
                    if (b) lstApp.add("orgstructuredata")
                }
                //
                clsORrelcls = apiObjectData().get(ApiObjectData).getClsOrRelCls(owner, isObj)
                if (mapPV.containsKey(clsORrelcls)) {
                    boolean b = apiObjectData().get(ApiObjectData).is_exist_entity_as_data(owner, "relobj", mapPV.get(clsORrelcls))
                    if (b) lstApp.add("objectdata")
                }
            }
        }
        //...
        String msg = lstApp.join(", ")
        if (lstApp.size() > 0)
            throw new XError("UseInApp@"+msg)
    }

    void validateForDeleteOwner(long owner, int isObj) {
        //---< check data in other DB
        CfgService cfgSvc = mdb.getApp().bean(CfgService.class)
        String modelMeta = cfgSvc.getConf().getString("dbsource/meta/id")
        if (modelMeta.isEmpty())
            throw new XError("Не найден id мета модели")
        //-->
        is_exist_owner_as_data(owner, isObj, modelMeta)
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
        if (setIdsCls.size()==0)
            throw new XError("NotFoundCod@${codTyp}")
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
    Store loadDefects(long obj) {
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", "Cls_Defects", "")
        if (map.isEmpty())
            throw new XError("NotFoundCod@Cls_Defects")
        String whe = "o.id=${obj}"
        if (obj==0)
            whe = "o.cls=${map.get("Cls_Defects")}"
        map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "", "Prop_%")
        Store st = mdb.createStore("Obj.Defects")
        mdb.loadQuery(st, """
            select o.id, o.cls, v.name,
                v1.id as idDefectsComponent, v1.propVal as pvDefectsComponent, v1.obj as objDefectsComponent, ov1.name as nameDefectsComponent,
                v2.id as idDefectsCategory, v2.propVal as pvDefectsCategory, null as fvDefectsCategory,
                v3.id as idDefectsIndex, v3.strVal as DefectsIndex,
                v4.id as idDefectsNote, v4.strVal as DefectsNote
            from Obj o 
                left join ObjVer v on o.id=v.ownerver and v.lastver=1
                left join DataProp d1 on d1.objorrelobj=o.id and d1.prop=:Prop_DefectsComponent --1072
                left join DataPropVal v1 on d1.id=v1.dataprop
                left join ObjVer ov1 on v1.obj=ov1.ownerver and ov1.lastver=1
                left join DataProp d2 on d2.objorrelobj=o.id and d2.prop=:Prop_DefectsCategory   --1074
                left join DataPropVal v2 on d2.id=v2.dataprop
                left join DataProp d3 on d3.objorrelobj=o.id and d3.prop=:Prop_DefectsIndex --1073
                left join DataPropVal v3 on d3.id=v3.dataprop
                left join DataProp d4 on d4.objorrelobj=o.id and d4.prop=:Prop_DefectsNote   --1075
                left join DataPropVal v4 on d4.id=v4.dataprop
            where ${whe}
        """, map)

        Map<Long, Long> mapPV = apiMeta().get(ApiMeta).mapEntityIdFromPV("factorVal", true)

        for (StoreRecord record in st) {
            record.set("fvDefectsCategory", mapPV.get(record.getLong("pvDefectsCategory")))
        }

        //mdb.outTable(st)
        return st
    }

    @DaoMethod
    Store loadComponents(long obj) {
        Set<Object> idsCls = apiMeta().get(ApiMeta).setIdsOfCls("Typ_Components")
        if (idsCls.size()==0)
            throw new XError("NotFoundCod@Typ_Components")
        String whe = "o.cls in (${idsCls.join(",")})"
        if (obj>0) whe = "o.id=${obj}"

        Store st = mdb.createStore("Obj.Components")
        mdb.loadQuery(st, """
            select o.id, o.cls, v.name
            from Obj o 
                left join ObjVer v on o.id=v.ownerver and v.lastver=1
            where ${whe}
        """)
        return st
    }

    @DaoMethod
    Store loadOrgStructure(long obj) {
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", "Cls_Location", "")
        if (map.isEmpty())
            throw new XError("NotFoundCod@Cls_Location")
        String whe = "o.id=${obj}"
        if (obj==0)
            whe = "o.cls=${map.get("Cls_Location")}"
        map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "", "Prop_%")
        Store st = mdb.createStore("Obj.ProdArea")
        mdb.loadQuery(st, """
            select o.id, o.cls, v.name, v.objParent as parent,
                v1.id as idStartKm, v1.numberVal as StartKm,
                v3.id as idFinishKm, v3.numberVal as FinishKm,
                v5.id as idStageLength, v5.numberVal as StageLength
            from Obj o 
                left join ObjVer v on o.id=v.ownerver and v.lastver=1
                left join DataProp d1 on d1.objorrelobj=o.id and d1.prop=:Prop_StartKm --1007
                left join DataPropVal v1 on d1.id=v1.dataprop
                left join DataProp d3 on d3.objorrelobj=o.id and d3.prop=:Prop_FinishKm --1008
                left join DataPropVal v3 on d3.id=v3.dataprop
                left join DataProp d5 on d5.objorrelobj=o.id and d5.prop=:Prop_StageLength   --1011
                left join DataPropVal v5 on d5.id=v5.dataprop
            where ${whe}
        """, map)

        return st
    }

    @DaoMethod
    Store loadParameters(long obj) {
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", "Cls_Params", "")
        if (map.isEmpty())
            throw new XError("NotFoundCod@Cls_Params")
        String whe = "o.id=${obj}"
        if (obj==0)
            whe = "o.cls=${map.get("Cls_Params")}"
        map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "", "Prop_%")
        Store st = mdb.createStore("Obj.Parameters")
        mdb.loadQuery(st, """
            select o.id, o.cls, v.name,
                v1.id as idParamsMeasure, v1.propVal as pvParamsMeasure, null as meaParamsMeasure,
                v2.id as idCollections, v2.propVal as pvCollections, v2.obj as objCollections,
                v3.id as idParamsDescription, v3.strVal as ParamsDescription
            from Obj o 
                left join ObjVer v on o.id=v.ownerver and v.lastver=1
                left join DataProp d1 on d1.isObj=1 and d1.objorrelobj=o.id and d1.prop=:Prop_ParamsMeasure   --1105
                left join DataPropVal v1 on d1.id=v1.dataprop
                left join DataProp d2 on d2.isObj=1 and d2.objorrelobj=o.id and d2.prop=:Prop_Collections --1081
                left join DataPropVal v2 on d2.id=v2.dataprop
                left join DataProp d3 on d3.isObj=1 and d3.objorrelobj=o.id and d3.prop=:Prop_ParamsDescription   --1106
                left join DataPropVal v3 on d3.id=v3.dataprop
            where ${whe}
            order by o.id
        """, map)

        Map<Long, Long> mapPV = apiMeta().get(ApiMeta).mapEntityIdFromPV("measure", true)

        for (StoreRecord record in st) {
            record.set("meaParamsMeasure", mapPV.get(record.getLong("pvParamsMeasure")))
        }
        return st
    }


    @DaoMethod
    Store loadSourceCollections(long obj) {
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", "Cls_Collections", "")
        if (map.isEmpty())
            throw new XError("NotFoundCod@Cls_Collections")
        String whe = "o.id=${obj}"
        if (obj==0)
            whe = "o.cls=${map.get("Cls_Collections")}"
        map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "", "Prop_%")
        Store st = mdb.createStore("Obj.SourceCollections")
        mdb.loadQuery(st, """
            select o.id, o.cls, v.name,
                v1.id as idDocumentNumber, v1.strVal as DocumentNumber,
                v2.id as idDocumentApprovalDate, v2.datetimeVal as DocumentApprovalDate,
                v3.id as idDocumentAuthor, v3.strVal as DocumentAuthor,
                v4.id as idDocumentStartDate, v4.datetimeVal as DocumentStartDate,
                v5.id as idDocumentEndDate, v5.datetimeVal as DocumentEndDate
            from Obj o 
                left join ObjVer v on o.id=v.ownerver and v.lastver=1
                left join DataProp d1 on d1.isObj=1 and d1.objorrelobj=o.id and d1.prop=:Prop_DocumentNumber   --1082
                left join DataPropVal v1 on d1.id=v1.dataprop
                left join DataProp d2 on d2.isObj=1 and d2.objorrelobj=o.id and d2.prop=:Prop_DocumentApprovalDate --1083
                left join DataPropVal v2 on d2.id=v2.dataprop
                left join DataProp d3 on d3.isObj=1 and d3.objorrelobj=o.id and d3.prop=:Prop_DocumentAuthor   --1086
                left join DataPropVal v3 on d3.id=v3.dataprop
                left join DataProp d4 on d4.isObj=1 and d4.objorrelobj=o.id and d4.prop=:Prop_DocumentStartDate    --1084
                left join DataPropVal v4 on d4.id=v4.dataprop
                left join DataProp d5 on d5.isObj=1 and d5.objorrelobj=o.id and d5.prop=:Prop_DocumentEndDate  --1085
                left join DataPropVal v5 on d5.id=v5.dataprop
            where ${whe}
            order by o.id
        """, map)
        return st
    }

    @DaoMethod
    Map<String, Object> loadDepartmentsWithFile(long obj) {
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "", "Prop_Document%")
        Store st = mdb.loadQuery("""
            select v.obj
            from DataProp d
            left join DataPropVal v on d.id=v.dataprop
            where d.isObj=1 and d.objOrRelObj=${obj} and d.prop=${map.get("Prop_DocumentLinkToDepartment")} --1080
        """)
        Set<Object> ids = st.getUniqueValues("obj")

        Map<String, Object> mapRez = new HashMap<>()
        mapRez.put("departments", ids.join(","))
        //Files
        Store stDBFS = loadAttachedFiles(obj, "Prop_DocumentFiles")
        //
        mapRez.put("files", stDBFS)

        return mapRez
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
    void saveDepartment(Map<String, Object> params) {
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", "Cls_Location", "")
        if (map.isEmpty())
            throw new XError("NotFoundCod@Cls_Location")
        long cls = map.get("Cls_Location")
        map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "Prop_DocumentLinkToDepartment", "")
        if (map.isEmpty())
            throw new XError("NotFoundCod@Prop_DocumentLinkToDepartment")
        map.put("obj", UtCnv.toLong(params.get("obj")))


        Store stOld = mdb.loadQuery("""
            select v.id, v.obj
            from DataProp d
                left join DataPropVal v on d.id=v.dataprop
            where d.isObj=1 and d.objOrRelObj=:obj and d.prop=:Prop_DocumentLinkToDepartment --1080            
        """, map)
        Set<Long> idsOld = stOld.getUniqueValues("obj") as Set<Long>
        Set<Long> idsNew = UtCnv.toList(params.get("ids")) as Set<Long>
        Set<Long> idsNewLong = new HashSet<>()
        idsNew.forEach {idsNewLong.add(UtCnv.toLong(it))}

        Set<Long> idsOldVal = new HashSet<>()
        //Deleting
        for (StoreRecord r in stOld) {
            if (!idsNewLong.contains(r.getLong("obj"))) {
                idsOldVal.add(r.getLong("id"))
            }
        }
        if (idsOldVal.size() > 0) {
            mdb.execQuery("""
                delete from DataPropVal where id in (${idsOldVal.join(",")});
                delete from DataProp
                where id in (
                    select id from DataProp
                    except
                    select dataprop as id from DataPropVal
                )
            """)
        }
        //
        //Adding
        Map<String, Object> pms = new HashMap<>()
        pms.put("own", UtCnv.toLong(params.get("obj")))
        //cls ?
        Map<Long, Long> mapPV = apiMeta().get(ApiMeta).mapEntityIdFromPV("cls", false)
        //
        for (long obj in idsNewLong) {
            if (!idsOld.contains(obj)) {
                pms.put("objDocumentLinkToDepartment", obj)
                pms.put("pvDocumentLinkToDepartment", mapPV.get(cls))
                fillProperties(true, "Prop_DocumentLinkToDepartment", pms)
            }
        }
    }

    @DaoMethod
    Store loadStage(long obj) {
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", "Cls_Stage", "")
        if (map.isEmpty())
            throw new XError("NotFoundCod@Cls_Stage")
        String whe = "o.id=${obj}"
        if (obj==0)
            whe = "o.cls=${map.get("Cls_Stage")}"
        map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "", "Prop_%")
        Store st = mdb.createStore("Obj.Stage")
        mdb.loadQuery(st, """
            select o.id, o.cls, v.name,
                v1.id as idStartKm, v1.numberVal as StartKm,
                v2.id as idStartPicket, v2.numberVal as StartPicket,
                v3.id as idFinishKm, v3.numberVal as FinishKm,
                v4.id as idFinishPicket, v4.numberVal as FinishPicket,
                v5.id as idStageLength, v5.numberVal as StageLength
            from Obj o 
                left join ObjVer v on o.id=v.ownerver and v.lastver=1
                left join DataProp d1 on d1.objorrelobj=o.id and d1.prop=:Prop_StartKm --1007
                left join DataPropVal v1 on d1.id=v1.dataprop
                left join DataProp d2 on d2.objorrelobj=o.id and d2.prop=:Prop_StartPicket   --1009
                left join DataPropVal v2 on d2.id=v2.dataprop
                left join DataProp d3 on d3.objorrelobj=o.id and d3.prop=:Prop_FinishKm --1008
                left join DataPropVal v3 on d3.id=v3.dataprop
                left join DataProp d4 on d4.objorrelobj=o.id and d4.prop=:Prop_FinishPicket   --1010
                left join DataPropVal v4 on d4.id=v4.dataprop
                left join DataProp d5 on d5.objorrelobj=o.id and d5.prop=:Prop_StageLength   --1011
                left join DataPropVal v5 on d5.id=v5.dataprop
            where ${whe}
        """, map)

        return st
    }

    @DaoMethod
    Store loadStation(long obj) {
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", "Cls_Station", "")
        if (map.isEmpty())
            throw new XError("NotFoundCod@Cls_Station")
        String whe = "o.id=${obj}"
        if (obj==0)
            whe = "o.cls=${map.get("Cls_Station")}"
        map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "", "Prop_%")
        Store st = mdb.createStore("Obj.Station")
        mdb.loadQuery(st, """
            select o.id, o.cls, v.name,
                v1.id as idStartKm, v1.numberVal as StartKm,
                v2.id as idStartPicket, v2.numberVal as StartPicket,
                v3.id as idFinishKm, v3.numberVal as FinishKm,
                v4.id as idFinishPicket, v4.numberVal as FinishPicket
            from Obj o 
                left join ObjVer v on o.id=v.ownerver and v.lastver=1
                left join DataProp d1 on d1.objorrelobj=o.id and d1.prop=:Prop_StartKm --1007
                left join DataPropVal v1 on d1.id=v1.dataprop
                left join DataProp d2 on d2.objorrelobj=o.id and d2.prop=:Prop_StartPicket   --1009
                left join DataPropVal v2 on d2.id=v2.dataprop
                left join DataProp d3 on d3.objorrelobj=o.id and d3.prop=:Prop_FinishKm --1008
                left join DataPropVal v3 on d3.id=v3.dataprop
                left join DataProp d4 on d4.objorrelobj=o.id and d4.prop=:Prop_FinishPicket   --1010
                left join DataPropVal v4 on d4.id=v4.dataprop
            where ${whe}
        """, map)

        return st
    }

    @DaoMethod
    Store loadTypesObjects(long obj) {
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "", "Prop_%")
        Set<Object> idsCls = apiMeta().get(ApiMeta).setIdsOfCls("Typ_ObjectTyp")
        if (idsCls.size()==0)
            throw new XError("NotFoundCod@Typ_ObjectTyp")
        String whe = "o.id=${obj}"
        if (obj==0)
            whe = "o.cls in (${idsCls.join(",")})"
        Store st = mdb.createStore("Obj.TypesObjects")
        mdb.loadQuery(st, """
            select o.id, v.objparent as parent, o.cls, v.name, null as nameCls,
                v1.id as idNumberOt, v1.strVal as NumberOt,
                v2.id as idShape, v2.propVal as pvShape, null as fvShape
            from Obj o 
                left join ObjVer v on o.id=v.ownerver and v.lastver=1
                left join DataProp d1 on d1.objorrelobj=o.id and d1.prop=:Prop_NumberOt --1005
                left join DataPropVal v1 on d1.id=v1.dataprop
                left join DataProp d2 on d2.objorrelobj=o.id and d2.prop=:Prop_Shape   --1006
                left join DataPropVal v2 on d2.id=v2.dataprop
            where ${whe}
        """, map)
        Map<Long, Long> mapPV = apiMeta().get(ApiMeta).mapEntityIdFromPV("factorVal", true)

        for (StoreRecord record in st) {
            record.set("fvShape", mapPV.get(record.getLong("pvShape")))
        }

        DataTreeNode dtn = UtData.createTreeIdParent(st, "id", "parent")
        long ind = 1
        UtData.scanTree(dtn, false, new ITreeNodeVisitor() {
            @Override
            void visitNode(DataTreeNode node) {
                node.record.set("number", ind++)
            }
        } as ITreeNodeVisitor)
        //mdb.outTable(st)
        return st
    }

    @DaoMethod
    Store loadProcessCharts(long obj) {
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "", "Prop_%")
        Set<Object> idsCls = apiMeta().get(ApiMeta).setIdsOfCls("Typ_Work")
        if (idsCls.size()==0)
            throw new XError("NotFoundCod@Typ_Work")
        String whe = "o.cls in (${idsCls.join(",")})"
        if (obj>0) whe = "o.id=${obj}"
        Store st = mdb.createStore("Obj.ProcessCharts")
        mdb.loadQuery(st, """
            select o.id as obj, o.cls, v.name, null as nameCls,
                v1.id as idTechCard, v1.strVal as TechCard,
                v2.id as idNumberSource, v2.strVal as NumberSource,
                v3.id as idCollections, v3.propVal as pvCollections, v3.obj as objCollections, ov3.name as nameCollections,
                v4.id as idPeriodicity, v4.propVal as pvPeriodicity, null as fvPeriodicity
            from Obj o 
                left join ObjVer v on o.id=v.ownerver and v.lastver=1
                left join DataProp d1 on d1.objorrelobj=o.id and d1.prop=:Prop_TechCard --1002
                left join DataPropVal v1 on d1.id=v1.dataprop
                left join DataProp d2 on d2.objorrelobj=o.id and d2.prop=:Prop_NumberSource   --1001
                left join DataPropVal v2 on d2.id=v2.dataprop
                left join DataProp d3 on d3.objorrelobj=o.id and d3.prop=:Prop_Collections --1081
                left join DataPropVal v3 on d3.id=v3.dataprop
                left join ObjVer ov3 on ov3.ownerVer=v3.obj and ov3.lastVer=1
                left join DataProp d4 on d4.objorrelobj=o.id and d4.prop=:Prop_Periodicity   --1003
                left join DataPropVal v4 on d4.id=v4.dataprop
            where ${whe}
            order by o.id
        """, map)

        Map<Long, Long> mapPV = apiMeta().get(ApiMeta).mapEntityIdFromPV("factorVal", true)

        for (StoreRecord record in st) {
            record.set("fvPeriodicity", mapPV.get(record.getLong("pvPeriodicity")))
        }

        //mdb.outTable(st)
        return st
    }

    @DaoMethod
    Store saveParams(String mode, Map<String, Object> params) {
        VariantMap pms = new VariantMap(params)
        long own
        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", "Cls_Params", "")
        Map<String, Object> par = new HashMap<>(pms)
        par.put("cls", map.get("Cls_Params"))
        par.put("fullName", pms.get("name"))
        if (mode.equalsIgnoreCase("ins")) {
            own = eu.insertEntity(par)
            pms.put("own", own)
            //1 Prop_ParamsMeasure
            if (pms.getLong("meaParamsMeasure") > 0)
                fillProperties(true, "Prop_ParamsMeasure", pms)
            //2 Prop_Collections
            if (pms.getLong("objCollections") > 0)
                fillProperties(true, "Prop_Collections", pms)
            //3 Prop_ParamsDescription
            if (pms.containsKey("ParamsDescription"))
                fillProperties(true, "Prop_ParamsDescription", pms)
        } else {
            own = pms.getLong("id")
            eu.updateEntity(par)
            //
            pms.put("own", own)
            //1 Prop_ParamsMeasure
            if (params.containsKey("idParamsMeasure"))
                updateProperties("Prop_ParamsMeasure", pms)
            else
                fillProperties(true, "Prop_ParamsMeasura", pms)
            //2 Prop_Collections
            if (params.containsKey("idCollections"))
                updateProperties("Prop_Collections", pms)
            else
                fillProperties(true, "Prop_Collections", pms)
            //3 Prop_ParamsDescription
            if (pms.getLong("idParamsDescription") > 0)
                updateProperties("Prop_ParamsDescription", pms)
            else
                fillProperties(true, "Prop_ParamsDescription", pms)
        }

        return loadParameters(own)
    }

    @DaoMethod
    double saveParamComponentValue(Map<String, Object> params) {

        params.put(UtCnv.toString(params.get("codProp")).split("_")[1], UtCnv.toDouble(params.get("val")))

        if (params.get("mode")== "ins") {
            fillProperties(false, UtCnv.toString(params.get("codProp")), params)
        } else {
            updateProperties(UtCnv.toString(params.get("codProp")), params)
        }
        return UtCnv.toDouble(params.get("val"))
    }

    @DaoMethod
    void deletePropOfParamComponent(long id) {
        mdb.execQuery("""
            delete from DataPropVal where id=${id};
            with d as (
                select id from DataProp
                except
                select dataProp as id from DataPropVal
            )
            delete from DataProp where id in (select id from d);
        """)
    }


    @DaoMethod
    Store saveDefects(String mode, Map<String, Object> params) {
        VariantMap pms = new VariantMap(params)
        long own
        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", "Cls_Defects", "")
        Map<String, Object> par = new HashMap<>(pms)
        par.put("cls", map.get("Cls_Defects"))
        par.put("fullName", pms.get("name"))
        if (mode.equalsIgnoreCase("ins")) {
            own = eu.insertEntity(par)
            pms.put("own", own)
            //1 Prop_DefectsIndex
            if (pms.getString("DefectsIndex") && pms.getString("DefectsIndex") != "")
                fillProperties(true, "Prop_DefectsIndex", pms)
            //2 Prop_DefectsNote
            if (pms.getString("DefectsNote") && pms.getString("DefectsNote") != "")
                fillProperties(true, "Prop_DefectsNote", pms)
            //3 Prop_DefectsCategory
            if (pms.getLong("fvDefectsCategory") > 0)
                fillProperties(true, "Prop_DefectsCategory", pms)
            //4 Prop_DefectsComponent
            if (pms.getLong("objDefectsComponent") > 0)
                fillProperties(true, "Prop_DefectsComponent", pms)
        } else {
            own = pms.getLong("id")
            eu.updateEntity(par)
            //
            pms.put("own", own)
            //1 Prop_DefectsIndex
            if (params.containsKey("idDefectsIndex"))
                updateProperties("Prop_DefectsIndex", pms)
            else
                fillProperties(true, "Prop_DefectsIndex", pms)
            //2 Prop_DefectsNote
            if (params.containsKey("idDefectsNote"))
                updateProperties("Prop_DefectsNote", pms)
            else
                fillProperties(true, "Prop_DefectsNote", pms)
            //3 Prop_DefectsCategory
            if (pms.getLong("idDefectsCategory") > 0)
                updateProperties("Prop_DefectsCategory", pms)
            else
                fillProperties(true, "Prop_DefectsCategory", pms)
            //4 Prop_DefectsComponent
            if (pms.getLong("idDefectsComponent") > 0)
                updateProperties("Prop_DefectsComponent", pms)
            else
                fillProperties(true, "Prop_DefectsComponent", pms)
        }

        return loadDefects(own)
    }

    @DaoMethod
    Store saveComponents(String mode, Map<String, Object> params) {
        VariantMap pms = new VariantMap(params)
        long own
        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        params.put("fullName", pms.get("name"))
        if (mode.equalsIgnoreCase("ins"))
            own = eu.insertEntity(params)
        else {
            own = pms.getLong("id")
            eu.updateEntity(params)
        }

        return loadComponents(own)
    }

    @DaoMethod
    Store saveStation(String mode, Map<String, Object> params) {
        VariantMap pms = new VariantMap(params)
        long own
        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", "Cls_Station", "")
        Map<String, Object> par = new HashMap<>(pms)
        par.put("cls", map.get("Cls_Station"))
        par.put("fullName", pms.get("name"))
        if (mode.equalsIgnoreCase("ins")) {
            own = eu.insertEntity(par)
            pms.put("own", own)
            //1 Prop_StartKm
            if (pms.getString("StartKm") && pms.getString("StartKm") != "")
                fillProperties(true, "Prop_StartKm", pms)
            //2 StartPicket
            if (pms.getString("StartPicket") && pms.getString("StartPicket") != "")
                fillProperties(true, "Prop_StartPicket", pms)
            //3 FinishKm
            if (pms.getString("FinishKm") && pms.getString("FinishKm") != "")
                fillProperties(true, "Prop_FinishKm", pms)
            //4 FinishPicket
            if (pms.getString("FinishPicket") && pms.getString("FinishPicket") != "")
                fillProperties(true, "Prop_FinishPicket", pms)
        } else {
            own = pms.getLong("id")
            eu.updateEntity(par)
            //
            pms.put("own", own)
            //1 Prop_StartKm
            if (params.containsKey("idStartKm"))
                updateProperties("Prop_StartKm", pms)
            else
                fillProperties(true, "Prop_StartKm", pms)
            //2 Prop_StartPicket
            if (params.containsKey("idStartPicket"))
                updateProperties("Prop_StartPicket", pms)
            else
                fillProperties(true, "Prop_StartPicket", pms)
            //3 Prop_FinishKm
            if (params.containsKey("idFinishKm"))
                updateProperties("Prop_FinishKm", pms)
            else
                fillProperties(true, "Prop_FinishKm", pms)
            //4 Prop_FinishPicket
            if (params.containsKey("idFinishPicket"))
                updateProperties("Prop_FinishPicket", pms)
            else
                fillProperties(true, "Prop_FinishPicket", pms)
        }
        return loadStation(own)
    }

    @DaoMethod
    Store saveStage(String mode, Map<String, Object> params) {
        VariantMap pms = new VariantMap(params)
        long own
        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", "Cls_Stage", "")
        Map<String, Object> par = new HashMap<>(pms)
        par.put("cls", map.get("Cls_Stage"))
        par.put("fullName", pms.get("name"))
        if (mode.equalsIgnoreCase("ins")) {
            own = eu.insertEntity(par)
            pms.put("own", own)
            //1 Prop_StartKm
            if (pms.getString("StartKm") && pms.getString("StartKm") != "")
                fillProperties(true, "Prop_StartKm", pms)
            //2 StartPicket
            if (pms.getString("StartPicket") && pms.getString("StartPicket") != "")
                fillProperties(true, "Prop_StartPicket", pms)
            //3 FinishKm
            if (pms.getString("FinishKm") && pms.getString("FinishKm") != "")
                fillProperties(true, "Prop_FinishKm", pms)
            //4 FinishPicket
            if (pms.getString("FinishPicket") && pms.getString("FinishPicket") != "")
                fillProperties(true, "Prop_FinishPicket", pms)
            //5 StageLength
            if (pms.getString("StageLength") && pms.getString("StageLength") != "")
                fillProperties(true, "Prop_StageLength", pms)

        } else {
            own = pms.getLong("id")
            eu.updateEntity(par)
            //
            pms.put("own", own)
            //1 Prop_StartKm
            if (params.containsKey("idStartKm"))
                updateProperties("Prop_StartKm", pms)
            else
                fillProperties(true, "Prop_StartKm", pms)
            //2 Prop_StartPicket
            if (params.containsKey("idStartPicket"))
                updateProperties("Prop_StartPicket", pms)
            else
                fillProperties(true, "Prop_StartPicket", pms)
            //3 Prop_FinishKm
            if (params.containsKey("idFinishKm"))
                updateProperties("Prop_FinishKm", pms)
            else
                fillProperties(true, "Prop_FinishKm", pms)
            //4 Prop_FinishPicket
            if (params.containsKey("idFinishPicket"))
                updateProperties("Prop_FinishPicket", pms)
            else
                fillProperties(true, "Prop_FinishPicket", pms)
            //5 Prop_StageLength
            if (params.containsKey("idStageLength"))
                updateProperties("Prop_StageLength", pms)
            else
                fillProperties(true, "Prop_StageLength", pms)
        }
        return loadStage(own)
    }

    @DaoMethod
    Store saveSourceCollections(String mode, Map<String, Object> params) {
        VariantMap pms = new VariantMap(params)
        long own
        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", "Cls_Collections", "")
        Map<String, Object> par = new HashMap<>(pms)
        par.put("cls", map.get("Cls_Collections"))
        par.put("fullName", pms.get("name"))
        if (mode.equalsIgnoreCase("ins")) {
            own = eu.insertEntity(par)
            pms.put("own", own)
            //1 Prop_DocumentNumber
            if (pms.containsKey("DocumentNumber"))
                fillProperties(true, "Prop_DocumentNumber", pms)
            //2 Prop_DocumentApprovalDate
            if (pms.containsKey("DocumentApprovalDate"))
                fillProperties(true, "Prop_DocumentApprovalDate", pms)

            //3 Prop_DocumentAuthor
            if (pms.containsKey("DocumentAuthor"))
                fillProperties(true, "Prop_DocumentAuthor", pms)

            //4 Prop_DocumentStartDate
            if (pms.containsKey("DocumentStartDate"))
                fillProperties(true, "Prop_DocumentStartDate", pms)

            //5 Prop_DocumentEndDate
            if (pms.containsKey("DocumentEndDate"))
                fillProperties(true, "Prop_DocumentEndDate", pms)

        } else {
            own = pms.getLong("id")
            eu.updateEntity(par)
            //
            pms.put("own", own)
            //1 Prop_DocumentNumber
            if (pms.containsKey("idDocumentNumber"))
                updateProperties("Prop_DocumentNumber", pms)
            //2 Prop_DocumentApprovalDate
            if (pms.containsKey("idDocumentApprovalDate"))
                updateProperties("Prop_DocumentApprovalDate", pms)
            //3 Prop_DocumentAuthor
            if (pms.containsKey("idDocumentAuthor"))
                updateProperties("Prop_DocumentAuthor", pms)

            //4 Prop_DocumentStartDate
            if (pms.containsKey("idDocumentStartDate"))
                updateProperties("Prop_DocumentStartDate", pms)
            else {
                if (pms.containsKey("DocumentStartDate"))
                    fillProperties(true,"Prop_DocumentStartDate", pms)
            }
            //5 Prop_DocumentEndDate
            if (pms.containsKey("idDocumentEndDate"))
                updateProperties("Prop_DocumentEndDate", pms)
            else {
                if (pms.containsKey("DocumentEndDate"))
                    fillProperties(true,"Prop_DocumentEndDate", pms)
            }
        }

        return loadSourceCollections(own)
    }

    @DaoMethod
    Store saveOrgStructure(String mode, Map<String, Object> params) {
        VariantMap pms = new VariantMap(params)
        long own
        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", "Cls_Location", "")
        Map<String, Object> par = new HashMap<>(pms)
        par.put("cls", map.get("Cls_Location"))
        par.put("fullName", pms.get("name"))
        if (mode.equalsIgnoreCase("ins")) {
            own = eu.insertEntity(par)
            pms.put("own", own)
            //1 Prop_StartKm
            if (pms.getString("StartKm") && pms.getString("StartKm") != "")
                fillProperties(true, "Prop_StartKm", pms)
            //3 FinishKm
            if (pms.getString("FinishKm") && pms.getString("FinishKm") != "")
                fillProperties(true, "Prop_FinishKm", pms)
            //5 StageLength
            if (pms.getString("StageLength") && pms.getString("StageLength") != "")
                fillProperties(true, "Prop_StageLength", pms)

        } else {
            own = pms.getLong("id")
            eu.updateEntity(par)
            //
            pms.put("own", own)
            //1 Prop_StartKm
            if (params.containsKey("idStartKm"))
                updateProperties("Prop_StartKm", pms)
            else
                fillProperties(true, "Prop_StartKm", pms)
            //3 Prop_FinishKm
            if (params.containsKey("idFinishKm"))
                updateProperties("Prop_FinishKm", pms)
            else
                fillProperties(true, "Prop_FinishKm", pms)
            //5 Prop_StageLength
            if (params.containsKey("idStageLength"))
                updateProperties("Prop_StageLength", pms)
            else
                fillProperties(true, "Prop_StageLength", pms)
        }
        return loadOrgStructure(own)
    }

    @DaoMethod
    Store saveTypesObjects(String mode, Map<String, Object> params) {
        VariantMap pms = new VariantMap(params)
        long own
        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        Map<String, Object> par = new HashMap<>(pms)
        par.put("fullName", pms.get("name"))
        if (mode.equalsIgnoreCase("ins")) {
            own = eu.insertEntity(par)
            pms.put("own", own)
            //1 Prop_NumberOt
            if (pms.getString("NumberOt") && pms.getString("NumberOt") != "")
                fillProperties(true, "Prop_NumberOt", pms)
            //2 Prop_Shape
            if (pms.getLong("fvShape") > 0)
                fillProperties(true, "Prop_Shape", pms)

        } else {
            own = pms.getLong("id")
            eu.updateEntity(par)
            //
            pms.put("own", own)
            //1 Prop_NumberOt
            if (params.containsKey("idNumberOt"))
                updateProperties("Prop_NumberOt", pms)
            else
                fillProperties(true, "Prop_NumberOt", pms)
            //2 Prop_Shape
            if (params.containsKey("idShape"))
                updateProperties("Prop_Shape", pms)
            else
                fillProperties(true, "Prop_Shape", pms)
        }

        return loadTypesObjects(own)
    }

    @DaoMethod
    Store saveProcessCharts(String mode, Map<String, Object> params) {
        VariantMap pms = new VariantMap(params)
        long own
        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        Map<String, Object> par = new HashMap<>(pms)
        par.put("fullName", pms.get("name"))
        if (mode.equalsIgnoreCase("ins")) {
            own = eu.insertEntity(par)
            pms.put("own", own)
            //1 Prop_TechCard
            fillProperties(true, "Prop_TechCard", pms)
            //2 Prop_NumberSource
            fillProperties(true, "Prop_NumberSource", pms)
            //3 Prop_Source
            fillProperties(true, "Prop_Collections", pms)
            //4 Prop_Periodicity
            fillProperties(true, "Prop_Periodicity", pms)
        } else {
            own = pms.getLong("obj")
            par.put("id", own)
            eu.updateEntity(par)
            //
            pms.put("own", own)
            //1 Prop_TechCard
            if (params.containsKey("idTechCard"))
                updateProperties("Prop_TechCard", pms)
            else
                fillProperties(true, "Prop_TechCard", pms)
            //2 Prop_NumberSource
            if (params.containsKey("idNumberSource"))
                updateProperties("Prop_NumberSource", pms)
            else
                fillProperties(true, "Prop_NumberSource", pms)
            //3 Prop_Source
            if (params.containsKey("idCollections"))
                updateProperties("Prop_Collections", pms)
            else
                fillProperties(true, "Prop_Collections", pms)
            //4 Prop_Periodicity
            if (params.containsKey("idPeriodicity"))
                updateProperties("Prop_Periodicity", pms)
            else
                fillProperties(true, "Prop_Periodicity", pms)
        }
        return loadProcessCharts(own)
    }

    @DaoMethod
    long getIdRelTyp(codRelTyp) {
        Store st = loadSqlMeta("""
            select id from RelTyp where cod like '${codRelTyp}' 
        """, "")
        if (st.size()==0)
            throw new XError("NotFoundCod@${codRelTyp}")

        return st.get(0).getLong("id")
    }

    protected List<List<Object>> combAll(long relTyp) throws Exception {
        Store stRelCls = loadSqlMeta("""
            select id from RelCls where reltyp=${relTyp} order by ord
        """, "")

        List<List<Object>> lists = new ArrayList<>()

        for (StoreRecord r : stRelCls) {
            Store stMembCls = loadSqlMeta("""
                select * from relclsmember where relcls=${r.getLong("id")}
            """, "")

            List<Object> lst = new ArrayList<>()
            for (StoreRecord rr : stMembCls ) {
               lst.add(rr.getLong("cls"))
            }
            lists.add(lst)
        }

        return lists

    };

    @DaoMethod
    void createGroupRelObj(long relTyp, List<List<List<Map<String, Object>>>> lists) {
        Store stRelCls = loadSqlMeta("""
            select id from RelCls where reltyp=${relTyp}
        """, "")

        Store stRelObj = mdb.loadQuery("""
            select id from relobj
            where relcls in (0${stRelCls.getUniqueValues("id").join(",")})
        """)

        Store stUch = mdb.loadQuery("""
            select relobj, obj 
            from relobjmember where relobj in (0${stRelObj.getUniqueValues("id").join(",")})
        """)
        List<List<Long>> allUch = new ArrayList<>()

        for (Object obj in stUch.getUniqueValues("relobj")) {
            long relobj = UtCnv.toLong(obj)
            Store stCur = stUch.findAll { it-> {
                it['relobj'] ==relobj
            }} as Store
            List<Long> lsCur = new ArrayList<>()
            for(StoreRecord rrr in stCur) {
                lsCur.add(rrr.getLong("obj"))
            }
            allUch.add(lsCur)
        }


        lists.forEach((List<List<Map<String, Object>>> ll) -> {
            List<List<Map<String, Object>>> lstUch = CartesianProduct.result(ll)
            lstUch.forEach((List<Map<String, Object>> uch) -> {
                List<Long> curUch = new ArrayList<>()
                curUch.add(uch.get(0).ent as Long)
                curUch.add(uch.get(1).ent as Long)

                if (!allUch.contains(curUch)) {
                    Map<String, Object> rec = new HashMap<>()
                    rec.put("relcls", uch.get(0).relcls)
                    String nm = "${uch.get(0).name} <=> ${uch.get(1).name}"
                    rec.put("name", nm)
                    rec.put("fullName", rec.get("name"))
                    EntityMdbUtils ue = new EntityMdbUtils(mdb, "RelObj")
                    long idRelObj = ue.insertEntity(rec)
                    //
                    rec = new HashMap<>()
                    rec.put("relobj", idRelObj)
                    rec.put("relclsmember", uch.get(0).rcm)
                    rec.put("cls", uch.get(0).cls)
                    rec.put("obj", uch.get(0).ent)
                    mdb.insertRec("RelObjMember", rec, true)
                    //
                    rec = new HashMap<>()
                    rec.put("relobj", idRelObj)
                    rec.put("relclsmember", uch.get(1).rcm)
                    rec.put("cls", uch.get(1).cls)
                    rec.put("obj", uch.get(1).ent)
                    mdb.insertRec("RelObjMember", rec, true)
                }
            })
        })
    }

    @DaoMethod
    Store loadObjRecOfROM(long rom) {
        Store st = mdb.createStore("Obj.ComponentsObject")
        return mdb.loadQuery(st, """
            select o.*, v.*
            from RelObjMember r
                left join Obj o on o.id=r.obj
                left join ObjVer v on o.id=v.ownerver and v.lastver=1
            where r.id=${rom}
        """)
    }

    @DaoMethod
    Store loadRelObjRec(long id) {
        Store st = mdb.createStore("RelObj.full")
        return mdb.loadQuery(st, """
            select o.id, v.name, v.cmtVer as cmt
            from RelObj o, RelObjVer v
            where o.id=v.ownerver and v.lastver=1 and o.id=${id}
        """)
    }

    @DaoMethod
    Store editRelObj(Map<String, Object> rec) {
        mdb.execQuery("""
            update RelObjVer set name='${rec.get("name")}',fullName='${rec.get("name")}',cmtVer='${rec.get("cmt")}'
            where ownerver=${rec.get("id")} and lastVer=1
        """)
        return loadRelObjRec(UtCnv.toLong(rec.get("id")))
    }

    @DaoMethod
    Store editObj(Map<String, Object> rec) {
        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        eu.updateEntity(rec)

        return mdb.loadQuery("""
            select ro.id, rv.name, rv.cmtver as cmt
            from Obj ro
            left join ObjVer rv on ro.id=rv.ownerver and rv.lastver=1
            where ro.id=${UtCnv.toLong(rec.get("id"))}
        """)
    }

    @DaoMethod
    Store loadComponentsObject_(long reltyp) {

        Store stRelCls = loadSqlMeta("""
            select id from RelCls where reltyp=${reltyp}
        """, "")
        String wheRelCls = "ro.relcls in (0," +stRelCls.getUniqueValues("id").join(",")+")"

        return mdb.loadQuery("""
            select 
                ROW_NUMBER() OVER() AS number,
                ro.id, rv.name, rv.cmtver as cmt
            from RelObj ro
            left join RelObjVer rv on ro.id=rv.ownerver and rv.lastver=1
            where ${wheRelCls}
        """)
    }

    @DaoMethod
    Store loadAllRelObj(long relTyp) {
        Store st = mdb.createStore("Obj.ComponentsObject.full")
        Store stTmp = loadSqlMeta("""
            select id from RelCls where reltyp=${relTyp}
        """, "")
        String idsRelCls = stTmp.getUniqueValues("id").join(",")

        stTmp = mdb.loadQuery("""
            select id, relCls from RelObj where relcls in (0${idsRelCls})
        """)
        idsRelCls = stTmp.getUniqueValues("relCls").join(",")
        String idsRelObj = stTmp.getUniqueValues("id").join(",")

        Store stRelCls = loadSqlMeta("""
            select -c.id as id, null as parent, v.name, null as cmt, c.ord
            from RelCls c, RelClsVer v
            where c.id in (0${idsRelCls}) and c.id=v.ownerVer and v.lastVer=1
            order by c.ord
        """, "")

        st.add(stRelCls)

        Store stRelObj = mdb.loadQuery("""
            select ro.id, -ro.relcls as parent, rv.name, rv.cmtver as cmt, ro.ord
            from RelObj ro
            left join RelObjVer rv on ro.id=rv.ownerver and rv.lastver=1
            where ro.id in (0${idsRelObj})
        """)
        st.add(stRelObj)
        return st
    }

    @DaoMethod
    void deleteGroupRelObj(List<Long> params) {
        String idsRelObj = params.join(",")
        Store stROM = mdb.loadQuery("""
            select id from RelObjMember where relobj in (${idsRelObj})
        """)
        String idsROM = stROM.getUniqueValues("id").join(",")
        mdb.execQuery("""
            delete from RelObjMember where id in (${idsROM});
            delete from RelObjVer where ownerVer in (${idsRelObj});
            delete from RelObj where id in (${idsRelObj});
            delete from SysCod where entityType=2 and entityId in (${idsRelObj});
        """)
    }


    @DaoMethod
    Store loadParamsComponent(long relTyp) {
        Store st = mdb.createStore("Obj.Parameters.component")
        Store stTmp = loadSqlMeta("""
            select id from RelCls where reltyp=${relTyp}
        """, "")
        String idsRelCls = stTmp.getUniqueValues("id").join(",")

        stTmp = mdb.loadQuery("""
            select id, relCls from RelObj where relcls in (0${idsRelCls})
        """)
        idsRelCls = stTmp.getUniqueValues("relCls").join(",")
        String idsRelObj = stTmp.getUniqueValues("id").join(",")

        Store stRelCls = loadSqlMeta("""
            select -c.id as id, null as parent, v.name, null as cmt, c.ord
            from RelCls c, RelClsVer v
            where c.id in (0${idsRelCls}) and c.id=v.ownerVer and v.lastVer=1
            order by c.ord
        """, "")

        st.add(stRelCls)
        Store stRelObj = mdb.createStore("Obj.Parameters.component")
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "", "Prop_ParamsLimit%")
        mdb.loadQuery(stRelObj, """
            select 
                ro.id, -ro.relcls as parent, rv.name, 
                v1.id as idParamsLimitMax, v1.numberval as ParamsLimitMax,
                v2.id as idParamsLimitMin, v2.numberval as ParamsLimitMin,
                v3.id as idParamsLimitNorm, v3.numberval as ParamsLimitNorm,
                rv.cmtver as cmt, ro.ord
            from RelObj ro
            left join RelObjVer rv on ro.id=rv.ownerver and rv.lastver=1
            left join DataProp d1 on d1.isObj=0 and d1.objorrelobj=ro.id and d1.prop=:Prop_ParamsLimitMax   --1102
            left join DataPropVal v1 on d1.id=v1.dataprop
            left join DataProp d2 on d2.isObj=0 and d2.objorrelobj=ro.id and d2.prop=:Prop_ParamsLimitMin   --1103
            left join DataPropVal v2 on d2.id=v2.dataprop
            left join DataProp d3 on d3.isObj=0 and d3.objorrelobj=ro.id and d3.prop=:Prop_ParamsLimitNorm  --1104
            left join DataPropVal v3 on d3.id=v3.dataprop
            where ro.id in (0${idsRelObj})
        """, map)
        st.add(stRelObj)
        DataTreeNode dtn = UtData.createTreeIdParent(st, "id", "parent")
        long ind = 1
        UtData.scanTree(dtn, false, new ITreeNodeVisitor() {
            @Override
            void visitNode(DataTreeNode node) {
                node.record.set("number", ind++)
            }
        } as ITreeNodeVisitor)

        return st
    }

    @DaoMethod
    Store loadComponentsObject(long relTyp) {
        Store st = mdb.createStore("Obj.ComponentsObject.full")
        Store stTmp = loadSqlMeta("""
            select id from RelCls where reltyp=${relTyp}
        """, "")
        String idsRelCls = stTmp.getUniqueValues("id").join(",")

        stTmp = mdb.loadQuery("""
            select id, relCls from RelObj where relcls in (0${idsRelCls})
        """)
        idsRelCls = stTmp.getUniqueValues("relCls").join(",")
        String idsRelObj = stTmp.getUniqueValues("id").join(",")

        Store stRelCls = loadSqlMeta("""
            select -c.id as id, null as parent, v.name, null as cmt, c.ord
            from RelCls c, RelClsVer v
            where c.id in (0${idsRelCls}) and c.id=v.ownerVer and v.lastVer=1
            order by c.ord
        """, "")

        st.add(stRelCls)

        Store stRelObj = mdb.loadQuery("""
            select 
                ro.id, -ro.relcls as parent, rv.name, rv.cmtver as cmt, ro.ord
            from RelObj ro
            left join RelObjVer rv on ro.id=rv.ownerver and rv.lastver=1
            where ro.id in (0${idsRelObj})
        """)
        st.add(stRelObj)

        DataTreeNode dtn = UtData.createTreeIdParent(st, "id", "parent")
        long ind = 1
        UtData.scanTree(dtn, false, new ITreeNodeVisitor() {
            @Override
            void visitNode(DataTreeNode node) {
                node.record.set("number", ind++)
            }
        } as ITreeNodeVisitor)

        return st
    }

    @DaoMethod
    Store loadRelObjMember(long relobj) {
        //Store st = mdb.createStore("Obj.ComponentsObject")
        return mdb.loadQuery("""
            select r.id, v.name, v.cmtVer as cmt, o.id as obj
            from RelObjMember r
                left join Obj o on o.id=r.obj
                left join ObjVer v on o.id=v.ownerver and v.lastver=1
            where relobj=${relobj}
        """)
    }

    @DaoMethod
    Store loadAllMembers(Map<String, Object> params) throws Exception {
        long reltyp = UtCnv.toLong(params.get("relTyp"))
        Store stRes = mdb.createStore("RelObjMember.all")

        Store stRC = loadSqlMeta("""
            select 'rc_'||c.id as id, null as parent, v.name, 'rc_'||c.id as cod, --null as cod, 
                null as cls, c.id as relcls, 0 as ent, c.ord
            from RelCls c, RelClsVer v
            where c.reltyp=${reltyp} and c.id=v.ownerver and v.lastver=1
            order by c.ord
        """, "")
        stRes.add(stRC)

        int i = 0
        for (StoreRecord r : stRC) {
            i++
            Store stCls = loadSqlMeta("""
                select 'c_'||r.cls||'_'||${i} as id, 'rc_'||c.id as parent, r.name, 'c_'||r.cls||'_'||${i} as cod,--null as cod, 
                    r.cls as cls, c.id as relcls, 0 as ent, r.id as ord
                from RelClsMember r, Relcls c
                where r.relcls=c.id and c.id=${r.getLong("relcls")}
                order by r.id
            """, "")
            stRes.add(stCls)
            for (StoreRecord rr : stCls) {
                Store stRO = mdb.createStore("RelObjMember.all")
                String prn = rr.getString("id")
                long rc = rr.getLong("ord")
                mdb.loadQuery(stRO, """
                    select 'o_'||o.id||'_'||${i} as id, '${prn}' as parent, v.name, o.cod, o.cls, ${rr.getLong("relcls")} as relcls, o.id as ent, o.ord,
                        '${rr.getString("parent")}_${rc}' as membs, ${rc} as rcm
                    from Obj o
                    left join ObjVer v on o.id=v.ownerver and v.lastver=1
                    where o.cls=${rr.getLong("cls")}
                    order by o.ord
                """)
                stRes.add(stRO)
            }
        }

        return stRes
    }

    @DaoMethod
    Store loadComponentDefect(String codTyp, String codProp) {
        Set<Object> idsCls = apiMeta().get(ApiMeta).setIdsOfCls(codTyp)
        Store st = mdb.loadQuery("""
            select o.id, o.cls, v.name as name, 0 as pv
            from Obj o, ObjVer v
            where o.id=v.ownerVer and v.lastVer=1 and o.cls in (0${idsCls.join(",")})
        """)

        Store stPV = loadSqlMeta("""
            select pv.id, pv.cls from PropVal pv, Prop p 
            where pv.prop=p.id and p.cod like '${codProp}'
        """, "")
        StoreIndex indPV = stPV.getIndex("cls")
        for (StoreRecord r in st) {
            StoreRecord rec = indPV.get(r.getLong("cls"))
            if (rec != null)
                r.set("pv", rec.getLong("id"))
        }

        return st
    }

    @DaoMethod
    Store loadMeasure(String codProp) {
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", codProp, "")
        if (map.isEmpty())
            throw new XError("NotFoundCod@${codProp}")

        return loadSqlMeta("""
            select m.id, m.name, p.id as pv
            from PropVal p, Measure m
            where p.measure=m.id and p.prop=${map.get(codProp)}
        """, "")
    }


    @DaoMethod
    Store loadDepartments(String codCls, String codProp) {
        return loadObjTreeForSelect(codCls, codProp)
    }

    @DaoMethod
    Store loadCollections(String codCls, String codProp) {
        return loadObjForSelect(codCls, codProp)
    }


    private Store loadObjTreeForSelect(String codCls, String codProp) {
        return apiNSIData().get(ApiNSIData).loadObjTreeForSelect(codCls, codProp)
    }


    private Store loadObjForSelect(String codCls, String codProp) {
        return apiNSIData().get(ApiNSIData).loadObjForSelect(codCls, codProp)
    }

    @DaoMethod
    Store loadFvCategory(String codFactor) {
        return loadFvForSelect(codFactor)
    }

    @DaoMethod
    Store loadFvSource(String codFactor) {
        return loadFvForSelect(codFactor)
    }

    @DaoMethod
    Store loadFvPeriodicity(String codFactor) {
        return loadFvForSelect(codFactor)
    }

    @DaoMethod
    Store loadFvOt(String codFactor) {
        return loadFvForSelect(codFactor)
    }

    @DaoMethod
    long getCls(long obj) {
        return loadObjRec(obj).getLong("cls")
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
            if ( cod.equalsIgnoreCase("Prop_TechCard") ||
                    cod.equalsIgnoreCase("Prop_NumberSource") ||
                    cod.equalsIgnoreCase("Prop_NumberOt") ||
                    cod.equalsIgnoreCase("Prop_DefectsIndex") ||
                    cod.equalsIgnoreCase("Prop_DefectsNote") ||
                        cod.equalsIgnoreCase("Prop_DocumentNumber") ||
                        cod.equalsIgnoreCase("Prop_DocumentAuthor") ||
                            cod.equalsIgnoreCase("Prop_ParamsDescription")) {
                if (params.get(keyValue) != null) {
                    recDPV.set("strVal", UtCnv.toString(params.get(keyValue)))
                }
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }

        if ([FD_AttribValType_consts.multistr].contains(attribValType)) {
            if ( cod.equalsIgnoreCase("Prop_Description")) {
                if (params.get(keyValue) != null) {
                    recDPV.set("multiStrVal", UtCnv.toString(params.get(keyValue)))
                }
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }

        if ([FD_AttribValType_consts.dt].contains(attribValType)) {
            if (cod.equalsIgnoreCase("Prop_DocumentApprovalDate") ||
                    cod.equalsIgnoreCase("Prop_DocumentStartDate") ||
                    cod.equalsIgnoreCase("Prop_DocumentEndDate")) {
                if (params.get(keyValue) != null) {
                    recDPV.set("dateTimeVal", UtCnv.toString(params.get(keyValue)))
                }
            } else
                throw new XError("for dev: [${cod}] отсутствует в реализации")
        }

        // For FV
        if ([FD_PropType_consts.factor].contains(propType)) {
            if ( cod.equalsIgnoreCase("Prop_Source") ||
                    cod.equalsIgnoreCase("Prop_Periodicity") ||
                        cod.equalsIgnoreCase("Prop_Shape") ||
                            cod.equalsIgnoreCase("Prop_DefectsCategory")) {
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
            if (cod.equalsIgnoreCase("Prop_StartKm") ||
                    cod.equalsIgnoreCase("Prop_StartPicket") ||
                    cod.equalsIgnoreCase("Prop_FinishKm") ||
                    cod.equalsIgnoreCase("Prop_FinishPicket") ||
                    cod.equalsIgnoreCase("Prop_StageLength") ||
                        cod.equalsIgnoreCase("Prop_ParamsLimitMax") ||
                        cod.equalsIgnoreCase("Prop_ParamsLimitMin") ||
                        cod.equalsIgnoreCase("Prop_ParamsLimitNorm")) {
                if (params.get(keyValue) != null) {
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
            if (cod.equalsIgnoreCase("Prop_DefectsComponent") ||
                        cod.equalsIgnoreCase("Prop_Collections")) {
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
            if (cod.equalsIgnoreCase("Prop_TechCard") ||
                    cod.equalsIgnoreCase("Prop_NumberSource") ||
                        cod.equalsIgnoreCase("Prop_NumberOt") ||
                    cod.equalsIgnoreCase("Prop_DefectsIndex") ||
                    cod.equalsIgnoreCase("Prop_DefectsNote") ||
                        cod.equalsIgnoreCase("Prop_DocumentNumber") ||
                        cod.equalsIgnoreCase("Prop_DocumentAuthor") ||
                            cod.equalsIgnoreCase("Prop_ParamsDescription")) {
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
            if ( cod.equalsIgnoreCase("Prop_DocumentApprovalDate") ||
                    cod.equalsIgnoreCase("Prop_DocumentStartDate") ||
                    cod.equalsIgnoreCase("Prop_DocumentEndDate") ) {
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
            if ( cod.equalsIgnoreCase("Prop_Source") ||
                    cod.equalsIgnoreCase("Prop_Periodicity") ||
                        cod.equalsIgnoreCase("Prop_Shape") ||
                            cod.equalsIgnoreCase("Prop_DefectsCategory")) {
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
            if (cod.equalsIgnoreCase("Prop_StartKm") ||
                    cod.equalsIgnoreCase("Prop_StartPicket") ||
                    cod.equalsIgnoreCase("Prop_FinishKm") ||
                    cod.equalsIgnoreCase("Prop_FinishPicket") ||
                    cod.equalsIgnoreCase("Prop_StageLength") ||
                        cod.equalsIgnoreCase("Prop_ParamsLimitMax") ||
                        cod.equalsIgnoreCase("Prop_ParamsLimitMin") ||
                        cod.equalsIgnoreCase("Prop_ParamsLimitNorm")) {
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
            if (cod.equalsIgnoreCase("Prop_DefectsComponent") ||
                    cod.equalsIgnoreCase("Prop_Collections")) {
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


    private Store loadFvForSelect(String codFactor) {
        return apiMetaFish().get(ApiMetaFish).loadFvForSelect(codFactor)
    }

    @DaoMethod
    Store loadClsTree(Map<String, Object> params) {
        return apiMeta().get(ApiMeta).loadClsTree(params)
    }

    //-------------------------

    private Store loadSqlMeta(String sql, String domain) {
        return apiMeta().get(ApiMeta).loadSql(sql, domain)
    }


    @DaoMethod
    public Map<String, Object> getCurUserInfo() {
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
