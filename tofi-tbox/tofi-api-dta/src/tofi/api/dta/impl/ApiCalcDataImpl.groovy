package tofi.api.dta.impl

import jandcode.commons.datetime.XDateTime
import jandcode.commons.datetime.XDateTimeFormatter
import jandcode.commons.error.XError
import jandcode.commons.variant.IVariantMap
import jandcode.commons.variant.VariantMap
import jandcode.core.auth.AuthService
import jandcode.core.dbm.mdb.BaseMdbUtils
import jandcode.core.store.Store
import jandcode.core.store.StoreRecord
import tofi.api.dta.ApiCalcData
import tofi.api.dta.ApiNSIData
import tofi.api.mdl.ApiMeta
import tofi.api.mdl.model.consts.FD_InputType_consts
import tofi.apinator.ApinatorApi
import tofi.apinator.ApinatorService

class ApiCalcDataImpl extends BaseMdbUtils implements ApiCalcData {

    ApinatorApi apiMeta() { return app.bean(ApinatorService).getApi("meta") }

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
        mdb.deleteRec(tableName, entId)
    }

    @Override
    void attachFile(Map<String, Object> rec) {
        IVariantMap par = new VariantMap(rec)
        par.putIfAbsent("dbeg", "1800-01-01")
        par.putIfAbsent("dend", "3333-12-31")
        if (par.isValueNull("fileVal")) {
            throw new XError("filevalue is required")
        }
        String codProp = par.getString("codProp")

        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", codProp, "")
        if (map.isEmpty())
            throw new XError("NotFoundCod@${codProp}")
        Map<String, Object> params = new HashMap<>()
        params.put("isObj", par.getLong("isObj"))
        params.put("dbeg", par.getString("dbeg"))
        params.put("dend", par.getString("dend"))
        params.put("objorrelobj", par.getLong("objorrelobj"))
        params.put("prop", map.get(codProp))
        params.put("fileVal", par.getLong("fileVal"))
        AuthService authSvc = mdb.getApp().bean(AuthService.class)
        long au = par.getLong("user_id")
        if (au == 0) {
            au = authSvc.getCurrentUser().getAttrs().getLong("id")
        }
        if (au == 0) {
            throw new XError("notLoginned")
        }

        Store stDP = mdb.loadQuery("""
            select id from DataProp
            where isObj=1 and objorrelobj=${par.getLong("objorrelobj")} 
                and prop=${map.get(codProp)} and periodType is null
        """)
        long id
        if (stDP.size() > 0)
            id = stDP.get(0).getLong("id")
        else
            id = insertRecToTable("DataProp", params, true)

        params.put("dataProp", id)
        params.put("authUser", au)
        params.put("inputType", FD_InputType_consts.app)
        insertRecToTable("DataPropVal", params, false)
    }

    @Override
    long insertRecToTable(String tableName, Map<String, Object> params, boolean generateId) {
        Store st = mdb.createStore(tableName)
        StoreRecord r = st.add(params)
        if (generateId)
            return mdb.insertRec(tableName, r, generateId)
        else {
            long id = mdb.getNextId(tableName)
            r.set("id", id)
            r.set("ord", id)
            r.set("timeStamp", XDateTime.create(new Date()).toString(XDateTimeFormatter.ISO_DATE_TIME))
            //
            return mdb.insertRec(tableName, r, false);
        }
    }
}
