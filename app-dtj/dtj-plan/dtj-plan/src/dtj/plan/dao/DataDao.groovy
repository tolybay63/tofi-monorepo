package dtj.plan.dao

import groovy.transform.CompileStatic
import jandcode.commons.error.XError
import jandcode.core.dao.DaoMethod
import jandcode.core.dbm.mdb.BaseMdbUtils
import jandcode.core.store.Store
import tofi.api.dta.ApiNSIData
import tofi.api.dta.ApiPersonnalData
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
    ApinatorApi apiPersonnalData() {
        return app.bean(ApinatorService).getApi("personnaldata")
    }


    @DaoMethod
    Store getPersonnalId(long userId) {

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







    //-------------------------


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
        else
            throw new XError("Unknown model [${model}]")
    }



}
