package tofi.mdl.model.dao.factor;

import jandcode.commons.UtCnv;
import jandcode.commons.UtString;
import jandcode.commons.error.XError;
import jandcode.commons.variant.VariantMap;
import jandcode.core.auth.AuthService;
import jandcode.core.auth.AuthUser;
import jandcode.core.dao.DaoMethod;
import jandcode.core.dbm.mdb.BaseMdbUtils;
import jandcode.core.dbm.sql.SqlText;
import jandcode.core.std.CfgService;
import jandcode.core.store.Store;
import jandcode.core.store.StoreRecord;
import tofi.api.dta.*;
import tofi.apinator.ApinatorApi;
import tofi.apinator.ApinatorService;
import tofi.mdl.consts.FD_PropType_consts;
import tofi.mdl.model.utils.EntityMdbUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class FactorMdbUtils extends BaseMdbUtils {

    ApinatorApi apiUserData() {
        return  getMdb().getApp().bean(ApinatorService.class).getApi("userdata");
    }
    ApinatorApi apiNSIData() {
        return getMdb().getApp().bean(ApinatorService.class).getApi("nsidata");
    }
    ApinatorApi apiMonitoringData() {
        return getMdb().getApp().bean(ApinatorService.class).getApi("monitoringdata");
    }


    /**
     * Загрузка FactorVal без пагинацией
    */
    @DaoMethod
    public Store loadFactorVal(Map<String, Object> params) throws Exception {
        //AuthService authSvc = getMdb().getApp().bean(AuthService.class);
        //AuthUser au = authSvc.getCurrentUser();
        //todo AuthUser
        //long al = 10; //au.getAttrs().getLong("accesslevel");
        //params.put("al", al);
        Store st = getMdb().createStore("Factor");
        getMdb().loadQuery(st, "select * from factor where parent=:factor order by ord", params);
        return st;
    }

    /**
     * Загрузка Factor с пагинацией
     */
    @DaoMethod
    public Map<String, Object> loadFactorPaginate(Map<String, Object> params) throws Exception {
        //AuthService authSvc = getMdb().getApp().bean(AuthService.class);
        //AuthUser au = authSvc.getCurrentUser();
        //long al = au.getAttrs().getLong("accesslevel");
        //if (al==0) throw new XError("notLoginned");

        String sql0 = "select * from factor where parent is null order by ord";
        SqlText sqlText = getMdb().createSqlText(sql0);
        Map<String, Object> par = new HashMap<>();
        String filter = UtCnv.toString(params.get("filter")).trim();

        //count
        String sql = "select count(*) as cnt from factor where parent is null";
        sqlText.setSql(sql);
        if (!filter.isEmpty())
            sqlText = sqlText.addWhere("name like '%" + filter + "%' or fullName like '%" + filter + "%' or cod like '%" + filter + "%'");
        int total = getMdb().loadQuery(sqlText).get(0).getInt("cnt");
        int lm = UtCnv.toInt(params.get("limit")) == 0 ? total : UtCnv.toInt(params.get("limit"));
        Map<String, Object> meta = new HashMap<String, Object>();
        meta.put("total", total);
        meta.put("page", UtCnv.toInt(params.get("page")));
        meta.put("limit", lm);
        //

        int offset = (UtCnv.toInt(params.get("page")) - 1) * lm;
        par.put("offset", offset);
        par.put("limit", lm);
        sqlText.setSql(sql0);
        sqlText.paginate(true);

        if (!UtCnv.toString(params.get("orderBy")).trim().isEmpty())
            sqlText = sqlText.replaceOrderBy(UtCnv.toString(params.get("orderBy")));


        if (!filter.isEmpty())
            sqlText = sqlText.addWhere("(cod like '%" + filter + "%' or name like '%" + filter + "%' or " +
                    "fullName like '%" + filter + "%')");
        Store st = getMdb().createStore("Factor");
        getMdb().loadQuery(st, sqlText, par);
        getMdb().resolveDicts(st);

        return Map.of("store", st, "meta", meta);
    }


    void is_exist_entity_as_data(long propVal, String metaModel) {
        List<String> lstApp = new ArrayList<>();
        if (metaModel.equalsIgnoreCase("fish")) {
            boolean b = apiUserData().get(ApiUserData.class).is_exist_entity_as_dataOld(0, "factorVal", propVal);
            if (b) lstApp.add("userdata");
            b = apiNSIData().get(ApiNSIData.class).is_exist_entity_as_dataOld(0, "factorVal", propVal);
            if (b) lstApp.add("nsidata");
            b = apiMonitoringData().get(ApiMonitoringData.class).is_exist_entity_as_dataOld(0, "factorVal", propVal);
            if (b) lstApp.add("monitoringdata");
        }

        String msg = UtString.join(lstApp, ", ");
        if (!lstApp.isEmpty())
            throw new XError("UseInApp@"+msg);
    }


    /**
     * Delete Factor
     *
     * @param params record Factor
     */
    @DaoMethod
    public void delete(Map<String, Object> params) throws Exception {
        Map<String, Object> rec = UtCnv.toMap(params);
        VariantMap map = new VariantMap(rec);
        if (map.getLong("parent") > 0) {
            //---< check data in other DB
            CfgService cfgSvc = getMdb().getApp().bean(CfgService.class);
            String modelMeta = cfgSvc.getConf().getString("dbsource/default/id");
            if (modelMeta.isEmpty())
                throw new XError("Не найден id мета модели");

            long fv = UtCnv.toLong(rec.get("id"));
            Store stTmp = getMdb().loadQuery("select id from PropVal where factorVal=:fv", Map.of("fv", fv));
            if (stTmp.size() > 0) {
                long propVal = stTmp.get(0).getLong("propVal");
                if (propVal > 0)
                    is_exist_entity_as_data(propVal, modelMeta);
            }
            //-->
        }

        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), "Factor");
        eu.deleteEntity(rec);
    }

    /**
     * Update Factor & FactorVal
     */
    @DaoMethod
    public Store update(Map<String, Object> params) throws Exception {
        Map<String, Object> rec = (UtCnv.toMap(params.get("rec")));

        long id = UtCnv.toLong(rec.get("id"));
        if (id == 0) {
            throw new XError("Поле id должно иметь не нулевое значение");
        }
        //
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), "Factor");
        eu.updateEntity(rec);
        //
        // Загрузка записи
        Store st = getMdb().createStore("Factor");

        getMdb().loadQuery(st, "select * from Factor where id=:id", Map.of("id", id));
        getMdb().resolveDicts(st);
        //getMdb().outTable(st);
        return st;
    }

    /**
     * Insert Factor & FactorVal
     */
    @DaoMethod
    public Store insert(Map<String, Object> params) throws Exception {
        Map<String, Object> rec = UtCnv.toMap(params.get("rec"));
        //
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), "Factor");
        long id = eu.insertEntity(rec);
        //add to PropVal
        long fac = UtCnv.toLong(rec.get("parent"));
        if (fac > 0) {
            Store rTmp = getMdb().loadQuery("select id, allItem from Prop where factor=:f and proptype=:pt",
                    Map.of("f", fac, "pt", FD_PropType_consts.factor));
            if (rTmp.size() > 0) {
                if (rTmp.get(0).getBoolean("allItem")) {
                    long prop = rTmp.get(0).getLong("id");
                    getMdb().insertRec("PropVal", Map.of("prop", prop, "factorVal", id), true);
                }
            }
        }
        //

        Store st = getMdb().createStore("Factor");
        getMdb().loadQuery(st, "select * from factor where id=:id", Map.of("id", id));
        getMdb().resolveDicts(st);
        return st;
    }

    @DaoMethod
    public StoreRecord loadRec(Map<String, Object> params) throws Exception {
        long id = UtCnv.toLong(params.get("id"));
        StoreRecord st = getMdb().createStoreRecord("Factor");
        getMdb().loadQueryRecord(st, "select * from factor where id=:id", Map.of("id", id));
        //getMdb().resolveDicts(st);
        return st;
    }

    @DaoMethod
    public void changeOrdFV(Map<String, Object> params) throws Exception {
        //AuthService authSvc = getMdb().getApp().bean(AuthService.class);
        //AuthUser au = authSvc.getCurrentUser();
        //long al = au.getAttrs().getLong("accesslevel");

        Map<String, Object> rec = UtCnv.toMap(params.get("rec"));
        boolean up = UtCnv.toBoolean(params.get("up"));
        long factor = UtCnv.toLong(rec.get("parent"));
        long id1 = UtCnv.toLong(rec.get("id"));
        long ord1 = UtCnv.toLong(rec.get("ord"));
        long id2;
        long ord2;

        Store st = getMdb().loadQuery("""
                    select * from Factor where parent=:factor order by ord
                """, Map.of("factor", factor));
        int k = 0;  //искомая позиция
        for (int i = 0; i < st.size(); i++) {
            if (st.get(i).getLong("id") == id1) {
                k = i;
                break;
            }
        }
        if (up) {
            id2 = st.get(k - 1).getLong("id");
            ord2 = st.get(k - 1).getLong("ord");
        } else {
            id2 = st.get(k + 1).getLong("id");
            ord2 = st.get(k + 1).getLong("ord");
        }
        //
        getMdb().execQuery("""
                    update Factor set ord=:ord2 where id=:id1;
                    update Factor set ord=:ord1 where id=:id2;
                """, Map.of("id1", id1, "id2", id2, "ord1", ord1, "ord2", ord2));
    }

    @DaoMethod
    public void changeOrdF(Map<String, Object> params) throws Exception {
        //AuthService authSvc = getMdb().getApp().bean(AuthService.class);
        //AuthUser au = authSvc.getCurrentUser();
        //long al = au.getAttrs().getLong("accesslevel");

        Map<String, Object> rec = UtCnv.toMap(params.get("rec"));
        boolean up = UtCnv.toBoolean(params.get("up"));
        long id1 = UtCnv.toLong(rec.get("id"));
        long ord1 = UtCnv.toLong(rec.get("ord"));
        long id2 = 0;
        long ord2 = 0;

        Store st = getMdb().loadQuery("""
                    select * from Factor where parent is null order by ord
                """);
        int k = 0;  //искомая позиция
        for (int i = 0; i < st.size(); i++) {
            if (st.get(i).getLong("id") == id1) {
                k = i;
                break;
            }
        }
        if (up) {
            id2 = st.get(k - 1).getLong("id");
            ord2 = st.get(k - 1).getLong("ord");
        } else {
            id2 = st.get(k + 1).getLong("id");
            ord2 = st.get(k + 1).getLong("ord");
        }
        //
        getMdb().execQuery("""
                    update Factor set ord=:ord2 where id=:id1;
                    update Factor set ord=:ord1 where id=:id2;
                """, Map.of("id1", id1, "id2", id2, "ord1", ord1, "ord2", ord2));
    }

    @DaoMethod
    public Store loadFactorTree(Map<String, Object> params) throws Exception {
        //
        long node = UtCnv.toLong(params.get("node"));
        String sql = "select * from factor where parent is null";
        if (node > 0)
            sql = "select * from factor where parent=" + node;

        Store st = getMdb().createStore("Factor");
        getMdb().loadQuery(st, sql);
        getMdb().resolveDicts(st);
        return st;
    }

    @DaoMethod
    public Store loadForSelect() throws Exception {
        //AuthService authSvc = getMdb().getApp().bean(AuthService.class);
        //AuthUser au = authSvc.getCurrentUser();
        //todo AuthUser
        //long al = 10; //au.getAttrs().getLong("accesslevel");

        Store st = getMdb().createStore("Factor.select");
        return getMdb().loadQuery(st, """
                    select id, name from Factor where parent is null order by ord
                """);
    }

    @DaoMethod
    public Store getFactor(long fv) throws Exception {
        return getMdb().loadQuery("""
                    with f as (
                        select parent as id from Factor where id=:id
                    )
                    select id, name from Factor where id in (select id from f)
                """, Map.of("id", fv));

    }

    @DaoMethod
    public Store loadFvForSelect(Map<String, Object> params) throws Exception {
        //AuthService authSvc = getMdb().getApp().bean(AuthService.class);
        //AuthUser au = authSvc.getCurrentUser();
        //todo AuthUser
        //long al = 10; //au.getAttrs().getLong("accesslevel");

        return getMdb().loadQuery("""
                    select id, name, fullName, parent,
                        case when parent is null then -id else id end as factorval,
                        case when parent is null then null else 'factorval' end as ent
                    from Factor where 0=0 order by ord
                """);
    }

}
