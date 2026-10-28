package tofi.api.dta;

import jandcode.core.store.Store;

import java.util.Map;

public interface ApiCalcData {

    /**
     * @param sql text of Sql
     * @return Store
     */
    Store loadSql(String sql, String domain);

    /**
     * @param sql    text sql
     * @param params params Map
     * @param domain domain
     * @return Store
     */
    Store loadSqlWithParams(String sql, Map<String, Object> params, String domain);

    /**
     * @param sql text sql
     */
    void execSql(String sql);

    void deleteEntity(long entId, String tableName);

    /**
     * @param rec Props of File
     */
    void attachFile(Map<String, Object> rec);

    /**
     * @param tableName name of table
     * @param params    params
     * @return id of table
     */
    long insertRecToTable(String tableName, Map<String, Object> params, boolean generateId);

}
