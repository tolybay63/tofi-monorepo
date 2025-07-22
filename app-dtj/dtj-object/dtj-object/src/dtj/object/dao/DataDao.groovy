package dtj.object.dao

import groovy.transform.CompileStatic
import jandcode.commons.error.XError
import jandcode.core.dao.DaoMethod
import jandcode.core.dbm.mdb.BaseMdbUtils
import jandcode.core.store.Store
import tofi.api.dta.ApiNSIData
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




    @DaoMethod
    Store loadTyp() {
        String sql = """
            select * from Typ t, TypVer v
            where t.id=v.ownerVer and v.lastVer=1
        """
        return loadSqlMeta(sql, "Typ.object")
    }


        @DaoMethod
    Store loadObj(String codCls, String model) {
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", codCls, "")

        String sql = """
            select * from Obj o, ObjVer v
            where o.id=v.ownerVer and v.lastVer=1 and o.cls=${map.get(codCls)}
        """

        Store st = loadSqlService(sql, "Obj.object", model)

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
            return apiNSIData().get(ApiNSIData).loadSql(sql, domain)

        else
            throw new XError("Unknown model [${model}]")
    }



}
