package fish.calc.dao

import groovy.transform.CompileStatic
import jandcode.commons.error.XError
import jandcode.core.auth.AuthService
import jandcode.core.dbm.mdb.BaseMdbUtils
import jandcode.core.store.Store
import tofi.api.mdl.ApiMeta
import tofi.apinator.ApinatorApi
import tofi.apinator.ApinatorService

@CompileStatic
class DataDao extends BaseMdbUtils {

    ApinatorApi apiMeta() { return app.bean(ApinatorService).getApi("meta") }
    //-----------------------------------------------------------------------------------------------//


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
