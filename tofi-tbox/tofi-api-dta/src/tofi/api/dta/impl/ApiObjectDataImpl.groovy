package tofi.api.dta.impl

import jandcode.core.dbm.mdb.BaseMdbUtils
import jandcode.core.store.Store
import tofi.api.dta.ApiObjectData
import tofi.apinator.ApinatorApi
import tofi.apinator.ApinatorService

class ApiObjectDataImpl extends BaseMdbUtils implements ApiObjectData {

    ApinatorApi apiMeta() {
        return app.bean(ApinatorService).getApi("meta")
    }


    @Override
    Store loadSql(String sql, String domain) {
        if (domain.isEmpty())
            return mdb.loadQuery(sql)
        else {
            Store st = mdb.createStore(domain)
            return mdb.loadQuery(st, sql)
        }
    }

    @Override
    Store loadSqlWithParams(String sql, Map<String, Object> params, String domain) {
        if (domain.isEmpty())
            return mdb.loadQuery(sql, params)
        else {
            Store st = mdb.createStore(domain)
            return mdb.loadQuery(st, sql, params)
        }
    }

}
