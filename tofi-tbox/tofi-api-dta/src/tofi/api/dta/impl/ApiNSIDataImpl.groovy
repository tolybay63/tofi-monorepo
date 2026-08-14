package tofi.api.dta.impl

import jandcode.core.dbm.mdb.BaseMdbUtils
import jandcode.core.store.Store
import tofi.api.dta.ApiNSIData

class ApiNSIDataImpl extends BaseMdbUtils implements ApiNSIData {

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

    @Override
    void execSql(String sql) {
        mdb.execQuery(sql)
    }

    @Override
    void deleteEntity(long entId, String tableName) {
        mdb.deleteRec(tableName, id)
    }
}
