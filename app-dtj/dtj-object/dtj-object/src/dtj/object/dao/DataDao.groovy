package dtj.object.dao

import groovy.transform.CompileStatic
import jandcode.commons.error.XError
import jandcode.core.dao.DaoMethod
import jandcode.core.dbm.mdb.BaseMdbUtils
import jandcode.core.store.Store
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

    /**
     *
     * @param codFactor: код фактора
     * @return  Набор значений указанного фактора
     */
    @DaoMethod
    Store loadFactorValForSelect(String codPropOrFactor) {
        Map<String, Long> map
        String sql
        if (codPropOrFactor.startsWith("Prop_")) {
            map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", codPropOrFactor, "")
            if (map.isEmpty())
                throw new XError("NotFoundCod@${codPropOrFactor}")
            sql = """
                select f.id, f.name, p.id as pv
                from propval p, factor f
                where p.factorval=f.id and p.prop=${map.get(codPropOrFactor)}
            """
        } else if (codPropOrFactor.startsWith("Factor_")) {
            map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Factor", codPropOrFactor, "")
            if (map.isEmpty())
                throw new XError("NotFoundCod@${codPropOrFactor}")
            sql = """
                select f.id, f.name, p.id as pv, f.parent
                from propval p, factor f 
                where p.factorval=f.id and f.parent=${map.get(codPropOrFactor)}
            """
        } else {
            throw new XError("Неисвезстная сущность")
        }
        return loadSqlMeta(sql, "")
    }


    /**
     *
     * @param codClsOr: Код класса или код типа
     * @param model:    Идентификатор сервиса (nsidata,plandata,personnaldata,...)
     * @return  Набор записей
     */
    @DaoMethod
    Store loadObjList(String codClsOrTyp, String model) {
        Map<String, Long> map
        String sql
        if (codClsOrTyp.startsWith("Cls_")) {
            map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", codClsOrTyp, "")
            if (map.isEmpty())
                throw new XError("NotFoundCod@${codClsOrTyp}")
            sql = """
                select o.id, o.cls, v.name 
                from Obj o, ObjVer v
                where o.id=v.ownerVer and v.lastVer=1 and o.cls=${map.get(codClsOrTyp)}
            """
        } else if (codClsOrTyp.startsWith("Typ_")) {
            map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Typ", codClsOrTyp, "")
            if (map.isEmpty())
                throw new XError("NotFoundCod@${codClsOrTyp}")
            sql = """
                select o.id, o.cls, v.name 
                from Obj o, ObjVer v
                where o.id=v.ownerVer and v.lastVer=1 and o.cls in (
                  select id from Cls where typ=${map.get(codClsOrTyp)}
                )
            """
        } else
            throw new XError("Неисвезстная сущность")

        Store st = loadSqlService(sql, "", model)
        return st
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
        else if (model.equalsIgnoreCase("plandata"))
            return apiPlanData().get(ApiPlanData).loadSql(sql, domain)
        else if (model.equalsIgnoreCase("personnaldata"))
            return apiPersonnalData().get(ApiPersonnalData).loadSql(sql, domain)
        else if (model.equalsIgnoreCase("orgstructuredata"))
            return apiOrgStructureData().get(ApiOrgStructureData).loadSql(sql, domain)
        else
            throw new XError("Unknown model [${model}]")
    }



}
