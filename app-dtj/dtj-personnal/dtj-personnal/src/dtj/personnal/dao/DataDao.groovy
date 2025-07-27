package dtj.personnal.dao

import groovy.transform.CompileStatic
import jandcode.commons.error.XError
import jandcode.core.dao.DaoMethod
import jandcode.core.dbm.mdb.BaseMdbUtils
import jandcode.core.store.Store
import jandcode.core.store.StoreIndex
import jandcode.core.store.StoreRecord
import tofi.api.dta.ApiNSIData
import tofi.api.dta.ApiObjectData
import tofi.api.dta.ApiOrgStructureData
import tofi.api.dta.ApiPersonnalData
import tofi.api.dta.ApiPlanData
import tofi.api.dta.ApiUserData
import tofi.api.mdl.ApiMeta
import tofi.apinator.ApinatorApi
import tofi.apinator.ApinatorService


@CompileStatic
class DataDao extends BaseMdbUtils {

    ApinatorApi apiAdm() {
        return app.bean(ApinatorService).getApi("adm")
    }
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
    Store loadPersonnal(long id) {
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", "Cls_Personnel", "")

        String whe = "o.id=${id}"
        if (id==0)
            whe = "o.cls=(${map.get("Cls_Personnel")})"

        Store st = mdb.createStore("Obj.Personnal")
        mdb.loadQuery(st, """
            select o.id, o.cls, v.name, v.fullName,
                v1.id as idTabNumber, v1.strVal as TabNumber,
                v2.id as idUserSecondName, v2.strVal as UserSecondName,
                v4.id as idUserFirstName, v4.strVal as UserFirstName,
                v5.id as idUserMiddleName, v5.strVal as UserMiddleName,
                v6.id as idUserEmail, v6.strVal as UserEmail,
                v7.id as idUserPhone, v7.strVal as UserPhone,
                v8.id as idUserDateBirth, v8.dateTimeVal as UserDateBirth,
                v9.id as idDateEmployment, v9.dateTimeVal as DateEmployment,
                v10.id as idDateDateDismissal, v10.dateTimeVal as DateDismissal,
                v11.id as idCreatedAt, v11.dateTimeVal as CreatedAt,
                v12.id as idUpdatedAt, v12.dateTimeVal as UpdatedAt,
                v13.id as idUserSex, v13.propVal as pvUserSex, null as fvUserSex,
                v14.id as idPosition, v14.propVal as pvPosition, null as fvPosition,
                v15.id as idLocation, v15.obj as objLocation, v15.propVal as pvLocation, ov15.name as nameLocation
            from Obj o 
                left join ObjVer v on o.id=v.ownerver and v.lastver=1
                left join DataProp d1 on d1.objorrelobj=o.id and d1.prop=:Prop_TabNumber
                left join DataPropVal v1 on d1.id=v1.dataprop
                left join DataProp d2 on d2.objorrelobj=o.id and d2.prop=:Prop_UserSecondName
                left join DataPropVal v2 on d2.id=v2.dataprop
                left join DataProp d4 on d4.objorrelobj=o.id and d4.prop=:Prop_UserFirstName
                left join DataPropVal v4 on d4.id=v4.dataprop
                left join DataProp d5 on d5.objorrelobj=o.id and d5.prop=:Prop_UserMiddleName
                left join DataPropVal v5 on d5.id=v5.dataprop
                left join DataProp d6 on d6.objorrelobj=o.id and d6.prop=:Prop_UserEmail
                left join DataPropVal v6 on d6.id=v6.dataprop
                left join DataProp d7 on d7.objorrelobj=o.id and d7.prop=:Prop_UserPhone
                left join DataPropVal v7 on d7.id=v7.dataprop
                left join DataProp d8 on d8.objorrelobj=o.id and d8.prop=:Prop_UserDateBirth
                left join DataPropVal v8 on d8.id=v8.dataprop
                left join DataProp d9 on d9.objorrelobj=o.id and d9.prop=:Prop_DateEmployment
                left join DataPropVal v9 on d9.id=v9.dataprop
                left join DataProp d10 on d10.objorrelobj=o.id and d10.prop=:Prop_DateDismissal
                left join DataPropVal v10 on d10.id=v10.dataprop
                left join DataProp d11 on d11.objorrelobj=o.id and d11.prop=:Prop_CreatedAt
                left join DataPropVal v11 on d11.id=v11.dataprop
                left join DataProp d12 on d12.objorrelobj=o.id and d12.prop=:Prop_UpdatedAt
                left join DataPropVal v12 on d12.id=v12.dataprop
                left join DataProp d13 on d13.objorrelobj=o.id and d13.prop=:Prop_UserSex
                left join DataPropVal v13 on d13.id=v13.dataprop
                left join DataProp d14 on d14.objorrelobj=o.id and d14.prop=:Prop_Position
                left join DataPropVal v14 on d14.id=v14.dataprop     
                left join DataProp d15 on d15.objorrelobj=o.id and d15.prop=:Prop_Location
                left join DataPropVal v15 on d15.id=v15.dataprop
                left join ObjVer ov15 on v15.obj=ov15.ownerVer and ov15.lastVer=1
            where ${whe}
        """, map)

        Map<Long, Long> mapPV = apiMeta().get(ApiMeta).mapEntityIdFromPV("factorVal", true)

        for (StoreRecord record in st) {
            record.set("fvUserSex", mapPV.get(record.getLong("pvUserSex")))
            record.set("fvPosition", mapPV.get(record.getLong("pvPosition")))
        }
        //
        return st
    }

    private void vaidatePersonal(Map<String, Object> params) {
        /*
login   passwd  Prop_TabNumber  Prop_UserSecondName Prop_UserFirstName   Prop_UserDateBirth
Prop_UserSex    Prop_Position  Prop_Location
         */
        if (params.containsKey("login"))
            throw new XError("login not specified")
        if (params.containsKey("passwd"))
            throw new XError("passwd not specified")
        if (params.containsKey("TabNumber"))
            throw new XError("TabNumber not specified")
        if (params.containsKey("UserSecondName"))
            throw new XError("UserSecondName not specified")
        if (params.containsKey("UserFirstName"))
            throw new XError("UserFirstName not specified")
        if (params.containsKey("UserDateBirth"))
            throw new XError("UserDateBirth not specified")
        if (params.containsKey("fvUserSex"))
            throw new XError("UserSex not specified")
        if (params.containsKey("fvPosition"))
            throw new XError("Position not specified")
        if (params.containsKey("objLocation"))
            throw new XError("Location not specified")
    }

    @DaoMethod
    Store savePersonnal(String mode, Map<String, Object> params) {




        loadPersonnal(0)
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
        else if (model.equalsIgnoreCase("objectdata"))
            return apiObjectData().get(ApiObjectData).loadSql(sql, domain)
        else if (model.equalsIgnoreCase("orgstructuredata"))
            return apiOrgStructureData().get(ApiOrgStructureData).loadSql(sql, domain)
        else if (model.equalsIgnoreCase("plandata"))
            return apiPlanData().get(ApiPlanData).loadSql(sql, domain)
        else if (model.equalsIgnoreCase("personnaldata"))
            return apiPersonnalData().get(ApiPersonnalData).loadSql(sql, domain)
        else
            throw new XError("Unknown model [${model}]")
    }



}
