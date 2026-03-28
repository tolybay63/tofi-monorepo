package tofi.adm.model.dao.role;

import jandcode.commons.UtCnv;
import jandcode.commons.UtString;
import jandcode.commons.error.XError;
import jandcode.core.auth.AuthService;
import jandcode.core.auth.AuthUser;
import jandcode.core.dao.DaoMethod;
import jandcode.core.dbm.mdb.BaseMdbUtils;
import jandcode.core.dbm.mdb.Mdb;
import jandcode.core.dbm.sql.SqlText;
import jandcode.core.store.Store;
import jandcode.core.store.StoreRecord;

import java.util.*;

public class RoleMdbUtils extends BaseMdbUtils {

    @DaoMethod
    public Map<String, Object> loadRolePaginate(Map<String, Object> params) throws Exception {
        checkTarget("adm:role");
        String filter = UtCnv.toString(params.get("filter")).trim();
        //count
        String sql = "select count(*) as cnt from AuthRole where 0=0";
        SqlText sqlText = getMdb().createSqlText(sql);
        sqlText.setSql(sql);
        if (!filter.isEmpty())
            sqlText = sqlText.addWhere("name like '%" + filter + "%' or fullName like '%" + filter + "%' or cmt like '%" + filter + "%'");
        int total = getMdb().loadQuery(sqlText).get(0).getInt("cnt");
        //

        sql = "select * from AuthRole where 0=0 order by id";
        sqlText = getMdb().createSqlText(sql);
        Map<String, Object> par = new HashMap<>();
        int pg = UtCnv.toInt(params.get("page"));
        int limit = UtCnv.toInt(params.get("limit"));
        limit = limit==0 ? total : limit;
        int offset = (pg - 1) * limit;
        par.put("offset", offset);
        par.put("limit", limit);
        sqlText.setSql(sql);
        sqlText.paginate(true);

        if (!UtCnv.toString(params.get("orderBy")).trim().isEmpty())
            sqlText = sqlText.replaceOrderBy(UtCnv.toString(params.get("orderBy")));


        if (!filter.isEmpty())
            sqlText = sqlText.addWhere("(name like '%" + filter + "%' or fullName like '%" + filter + "%'  or cmt like '%" + filter + "%')");
        Store st = getMdb().createStore("AuthRole");
        getMdb().loadQuery(st, sqlText, par);

        Map<String, Object> meta = new HashMap<>();

        meta.put("total", total);
        meta.put("page", pg);
        meta.put("limit", limit);

        return Map.of("store", st, "meta", meta);
    }

    @DaoMethod
    public void delete(Map<String, Object> rec) throws Exception {
        checkTarget("adm:role:del");
        getMdb().deleteRec("AuthRole", UtCnv.toLong(rec.get("id")));
    }

    @DaoMethod
    public Store update(Map<String, Object> params) throws Exception {
        checkTarget("adm:role:upd");
        Map<String, Object> rec = UtCnv.toMap(params.get("rec"));
        getMdb().updateRec("AuthRole", rec);
        //
        Store st = getMdb().createStore("AuthRole");
        return getMdb().loadQuery(st, "select * from AuthRole where id=:id", Map.of("id",
                rec.get("id")));
    }

    @DaoMethod
    public Store insert(Map<String, Object> params) throws Exception {
        checkTarget("adm:role:ins");
        Map<String, Object> rec = UtCnv.toMap(params.get("rec"));
        long id = getMdb().insertRec("AuthRole", rec, true);
        //
        Store st = getMdb().createStore("AuthRole");
        getMdb().loadQuery(st, "select * from AuthRole where id=:id", Map.of("id", id));
        //
        //getMdb().outTable(st);
        return st;
    }

    @DaoMethod
    public Store loadRec(long id) throws Exception {
        Store st = getMdb().createStore("AuthRole");
        return getMdb().loadQuery(st, """
                    select * from AuthRole where id=:id
                """, Map.of("id", id));
    }

    @DaoMethod
    public String getRolePermis(long id) throws Exception {

        Store st = getMdb().loadQuery("""
                    select p.text from AuthRolePermis r, Permis p where r.authRole=:id and r.permis=p.id
                    order by p.ord
                """, Map.of("id", id));

        Set<Object> set = st.getUniqueValues("text");

        return UtString.join(set, "; ");
    }

    @DaoMethod
    public Store loadRolePermis(long role) throws Exception {
        Store st = getMdb().createStore("Permis.full");
        return getMdb().loadQuery(st, """
                        with a as (
                            select permis, accessLevel from authrolepermis where authrole=:role
                        )
                        select p.*, a.accessLevel  from permis p, a
                        where p.id=a.permis
                        order by p.ord
                """, Map.of("role", role));
    }

    @DaoMethod
    public Store loadRolePermisForUpd(long role) throws Exception {
        Store st = getMdb().createStore("Permis.full");
        return getMdb().loadQuery(st, """
                    select p.*, a.accessLevel, a.id as idInTable, case when a.id is null then false else true end as checked
                    from permis p
                    left join authrolepermis a on p.id=a.permis and a.authrole=:role
                    order by p.ord
                """, Map.of("role", role));
    }

    @DaoMethod
    public void saveRolePermis(Map<String, Object> params) throws Exception {
        long role = UtCnv.toLong(params.get("role"));
        List<Map<String, Object>> lstData = (List<Map<String, Object>>) params.get("data");

        //Old ids
        Store oldSt = getMdb().loadQuery("select id from AuthRolePermis where authRole=:r", Map.of("r", role));
        Set<Object> oldIds = oldSt.getUniqueValues("id");

        //New ids
        Set<Object> newIds = new HashSet<>();
        for (Map<String, Object> map : lstData) {
            newIds.add(UtCnv.toLong(map.get("idInTable")));
        }
        //Deleting
        for (StoreRecord r : oldSt) {
            if (!newIds.contains(r.getLong("id"))) {
                try {
                    getMdb().deleteRec("AuthRolePermis", r.getLong("id"));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        // Saving
        Store st = getMdb().createStore("AuthRolePermis");
        for (Map<String, Object> map : lstData) {
            if (!oldIds.contains(UtCnv.toLong(map.get("idInTable")))) {
                StoreRecord r = st.add(map);
                r.set("id", null);
                r.set("authRole", role);
                r.set("permis", UtCnv.toString(map.get("id")));
                if (UtCnv.toLong(map.get("accessLevel")) > 0)
                    r.set("accessLevel", UtCnv.toLong(map.get("accessLevel")));
                getMdb().insertRec("AuthRolePermis", r, true);
            } else {
                StoreRecord r = st.add(map);
                r.set("id", UtCnv.toLong(map.get("idInTable")));
                r.set("authRole", role);
                r.set("permis", UtCnv.toString(map.get("id")));
                r.set("accessLevel", UtCnv.toLong(map.get("accessLevel")));
                getMdb().updateRec("AuthRolePermis", r);
            }
        }
    }

    @DaoMethod
    public void checkTarget(String target) {
        AuthService authService = getModel().getApp().bean(AuthService.class);
        AuthUser usr = authService.getCurrentUser();

        // ЛОГ: Сразу увидим, кто стучится
        System.out.println("--- DEBUG ---");
        System.out.println("Target: " + target);
        System.out.println("User ID from Attrs: " + usr.getAttrs().getLong("id"));
        System.out.println("User Login: " + usr.getAttrs().getString("login"));
        System.out.println("-------------");

        if (usr.getAttrs().getLong("id") == 1) return;

        if (usr.getAttrs().getLong("id") == 0)
            throw new XError("notLoginned");

        String userTargets = usr.getAttrs().getString("target", "");
        String [] targets = userTargets.trim().split("\\s*,\\s*");
        if (!Arrays.asList(targets).contains(target)) {
            if (Arrays.asList("dtj", "adm", "meta", "nsi").contains(target)) {
                throw new XError("notAccessService");
            }
            throw new XError("notAccess");
        }
    }

}
