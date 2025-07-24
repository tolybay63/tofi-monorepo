package dtj.orgstructuredata.dao

import groovy.transform.CompileStatic
import jandcode.commons.error.XError
import jandcode.commons.variant.VariantMap
import jandcode.core.dao.DaoMethod
import jandcode.core.dbm.mdb.BaseMdbUtils
import jandcode.core.store.Store
import jandcode.core.store.StoreIndex
import jandcode.core.store.StoreRecord
import tofi.api.dta.ApiNSIData
import tofi.api.dta.ApiUserData
import tofi.api.dta.model.utils.EntityMdbUtils
import tofi.api.mdl.ApiMeta
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

    /* =================================================================== */

    @DaoMethod
    Store loadLocation(long id) {

        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Typ", "Typ_Location", "")
        Store st = mdb.createStore("Obj.Location")

        Store stCls = loadSqlMeta("""
            select id from Cls where typ=${map.get("Typ_Location")}
        """, "")
        Set<Object> idsCls = stCls.getUniqueValues("id")

        String whe = "o.id=${id}"
        if (id==0)
            whe = "o.cls in (${idsCls.join(",")})"

        map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "", "Prop_")
        mdb.loadQuery(st, """
            select o.id, v.objParent as parent, o.cls, v.name, v.fullName, null as nameCls,
                v1.id as idAddress, v1.strVal as Address,
                v2.id as idPhone, v2.numberVal as Phone,
                null as ObjectTypeMulti,
                v4.id as idStartKm, v4.numberVal as StartKm,
                v5.id as idFinishKm, v5.numberVal as FinishKm,
                v6.id as idStageLength, v6.numberVal as StageLength,
                v7.id as idRegion, v7.propVal as pvRegion, null as fvRegion,
                v8.id as idIsActive, v8.propVal as pvIsActive, null as fvIsActive,
                v9.id as idCreatedAt, v9.dateTimeVal as CreatedAt,
                v10.id as idUpdatedAt, v10.dateTimeVal as UpdatedAt,
                v11.id as idDescription, v11.multiStrVal as Description
            from Obj o 
                left join ObjVer v on o.id=v.ownerver and v.lastver=1
                left join DataProp d1 on d1.objorrelobj=o.id and d1.prop=:Prop_Address
                left join DataPropVal v1 on d1.id=v1.dataprop
                left join ObjVer ov1 on v1.obj=ov1.ownerver and ov1.lastver=1
                left join DataProp d2 on d2.objorrelobj=o.id and d2.prop=:Prop_Phone
                left join DataPropVal v2 on d2.id=v2.dataprop
                --left join DataProp d3 on d3.objorrelobj=o.id and d3.prop=:Prop_ObjectTypeMulti
                --left join DataPropVal v3 on d3.id=v3.dataprop
                left join DataProp d4 on d4.objorrelobj=o.id and d4.prop=:Prop_StartKm
                left join DataPropVal v4 on d4.id=v4.dataprop
                left join DataProp d5 on d5.objorrelobj=o.id and d5.prop=:Prop_FinishKm
                left join DataPropVal v5 on d5.id=v5.dataprop
                left join DataProp d6 on d6.objorrelobj=o.id and d6.prop=:Prop_StageLength
                left join DataPropVal v6 on d6.id=v6.dataprop
                left join DataProp d7 on d7.objorrelobj=o.id and d7.prop=:Prop_Region
                left join DataPropVal v7 on d7.id=v7.dataprop
                left join DataProp d8 on d8.objorrelobj=o.id and d8.prop=:Prop_IsActive
                left join DataPropVal v8 on d8.id=v8.dataprop
                left join DataProp d9 on d9.objorrelobj=o.id and d9.prop=:Prop_CreatedAt
                left join DataPropVal v9 on d9.id=v9.dataprop
                left join DataProp d10 on d10.objorrelobj=o.id and d10.prop=:Prop_UpdatedAt
                left join DataPropVal v10 on d10.id=v10.dataprop
                left join DataProp d11 on d11.objorrelobj=o.id and d11.prop=:Prop_Description
                left join DataPropVal v11 on d11.id=v11.dataprop
            where ${whe}
        """, map)

        Store stMulti = mdb.loadQuery("""
            select o.id,
                string_agg (cast(v1.obj as varchar(3000)), ',' order by v1.obj) as arr
            from Obj o 
                left join DataProp d1 on d1.objorrelobj=o.id and d1.prop=:Prop_ObjectTypeMulti
                left join DataPropVal v1 on d1.id=v1.dataprop
            where ${whe}
            group by o.id
        """, map)
        StoreIndex indMulti = stMulti.getIndex("id")

        Map<Long, Long> mapPV = apiMeta().get(ApiMeta).mapEntityIdFromPV("factorVal", true)

        for (StoreRecord record in st) {
            StoreRecord rec = indMulti.get(record.getLong("id"))
            if (rec != null) {
                record.set("ObjectTypeMulti", rec.getString("arr"))
            }
            record.set("fvRegion", mapPV.get(record.getLong("pvRegion")))
            record.set("fv_IsActive", mapPV.get(record.getLong("pvIsActive")))
        }
        //
        return st
    }

    @DaoMethod
    Store saveLocation(String mode, Map<String, Object> params) {
        VariantMap pms = new VariantMap(params)
        //
        long own
        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        Map<String, Object> par = new HashMap<>(pms)
        if (mode.equalsIgnoreCase("ins")) {
            //
            own = eu.insertEntity(par)
            pms.put("own", own)
            //1 Prop_Address
            if (pms.containsKey("Address"))
                fillProperties(true, "Prop_Address", pms)
            //2 Prop_Phone
            if (pms.containsKey("Phone"))
                fillProperties(true, "Prop_Phone", pms)

            //3 Prop_StartKm
            if (pms.containsKey("FinishKm"))
                fillProperties(true, "Prop_FinishKm", pms)

            //4 Prop_FinishKm
            if (pms.containsKey("StartKm"))
                fillProperties(true, "Prop_StartKm", pms)

            //5 Prop_StartPicket
            if (pms.containsKey("StageLength"))
                fillProperties(true, "Prop_StageLength", pms)


            //6 Prop_Region
            if (pms.getLong("fvRegion") > 0)
                fillProperties(true, "Prop_Region", pms)

            //7 Prop_IsActive
            if (pms.getLong("fvIsActive") > 0)
                fillProperties(true, "Prop_IsActive", pms)
            //8 Prop_CreatedAt
            if (pms.containsKey("CreatedAt"))
                fillProperties(true, "Prop_CreatedAt", pms)
            //9 Prop_UpdatedAt
            if (pms.containsKey("UpdatedAt"))
                fillProperties(true, "Prop_UpdatedAt", pms)
            //10 Prop_Description
            if (pms.containsKey("Description"))
                fillProperties(true, "Prop_Description", pms)
        } else if (mode.equalsIgnoreCase("upd")) {
            own = pms.getLong("id")
            eu.updateEntity(par)
            //
            pms.put("own", own)

            //1 Prop_Address
            if (pms.containsKey("idAddress"))
                updateProperties("Prop_Address", pms)
            else {
                if (!pms.getString("Address").isEmpty())
                    fillProperties(true, "Prop_ObjectType", pms)
            }
            //2 Prop_Phone
            if (pms.containsKey("idPhone"))
                updateProperties("Prop_Phone", pms)
            else {
                if (!pms.getString("Phone").isEmpty())
                    fillProperties(true, "Prop_Phone", pms)
            }
            //3 Prop_StartKm
            if (pms.containsKey("idStartKm"))
                updateProperties("Prop_StartKm", pms)
            else {
                if (!pms.getString("StartKm").isEmpty())
                    fillProperties(true, "Prop_StartKm", pms)
            }
            //4 Prop_FinishKm
            if (pms.containsKey("idFinishKm"))
                updateProperties("Prop_FinishKm", pms)
            else {
                if (!pms.getString("FinishKm").isEmpty())
                    fillProperties(true, "Prop_FinishKm", pms)
            }
            //5 Prop_StartPicket
            if (pms.containsKey("idStageLength"))
                updateProperties("Prop_StageLength", pms)
            else {
                if (!pms.getString("StageLength").isEmpty())
                    fillProperties(true, "Prop_StageLength", pms)
            }
            //6 Prop_Region
            if (pms.containsKey("idRegion"))
                updateProperties("Prop_Region", pms)
            else {
                if (pms.getLong("fvRegion") > 0)
                    fillProperties(true, "Prop_Region", pms)
            }
            //7 Prop_IsActive
            if (pms.containsKey("idIsActive"))
                updateProperties("Prop_IsActive", pms)
            else {
                if (pms.getLong("fvIsActive") > 0)
                    fillProperties(true, "Prop_IsActive", pms)
            }
            //8 Prop_CreatedAt
            if (pms.containsKey("idCreatedAt"))
                updateProperties("Prop_CreatedAt", pms)
            else {
                if (!pms.getString("CreatedAt").isEmpty())
                    fillProperties(true, "Prop_CreatedAt", pms)
            }

            //9 Prop_UpdatedAt
            if (pms.containsKey("idUpdatedAt"))
                updateProperties("Prop_UpdatedAt", pms)
            else {
                if (!pms.getString("UpdatedAt").isEmpty())
                    fillProperties(true, "Prop_UpdatedAt", pms)
            }
            //10 Prop_Description
            if (pms.containsKey("idDescription"))
                updateProperties("Prop_Description", pms)
            else {
                if (!pms.getString("Description").isEmpty())
                    fillProperties(true, "Prop_Description", pms)
            }
        } else {
            throw new XError("Нейзвестный режим сохранения ('ins', 'upd')")
        }

        return loadLocation(own)
    }



    private void fillProperties(boolean isObj, String cod, Map<String, Object> params) {

    }

    private void updateProperties(String cod, Map<String, Object> params) {

    }


    //-------------------------

    private Store loadSqlMeta(String sql, String domain) {
        return apiMeta().get(ApiMeta).loadSql(sql, domain)
    }

    private Store loadSqlService(String sql, String domain, String model) {
        if (model.equalsIgnoreCase("userdata"))
            return apiUserData().get(ApiUserData).loadSql(sql, domain)
        else if (model.equalsIgnoreCase("nsidata"))
            return apiNSIData().get(ApiNSIData).loadSql(sql, domain)
        else
            throw new XError("Unknown model [${model}]")
    }



}
