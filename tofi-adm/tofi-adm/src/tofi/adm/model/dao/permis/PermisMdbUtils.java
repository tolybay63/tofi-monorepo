package tofi.adm.model.dao.permis;

import jandcode.commons.UtCnv;
import jandcode.commons.error.XError;
import jandcode.core.auth.AuthService;
import jandcode.core.auth.AuthUser;
import jandcode.core.dao.DaoMethod;
import jandcode.core.dbm.dao.BaseModelDao;
import jandcode.core.dbm.mdb.BaseMdbUtils;
import jandcode.core.dbm.mdb.Mdb;
import jandcode.core.store.Store;
import jandcode.core.store.StoreIndex;
import jandcode.core.store.StoreRecord;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PermisMdbUtils extends BaseMdbUtils {
    @DaoMethod
    public Store load() throws Exception {
        checkTarget("adm:tml");
        String sql = "select * from Permis where 0=0 order by ord";
        Store st = getMdb().createStore("Permis");

        getMdb().loadQuery(st, sql);

        return st;
    }


    private void validateRec(String id) throws Exception {
        Store stTmp = getMdb().loadQuery("""
                select r.name
                from AuthRolePermis p
                	left join authrole r on p.authrole=r.id
                where p.permis=:id
        """, Map.of("id", id));
        if (stTmp.size() > 0) {
            throw new XError("Используется в роли [{0}]", stTmp.get(0).getString("name"));
        }

        stTmp = getMdb().loadQuery("""
            select r.fullname
            from AuthUserPermis p
            left join authuser r on p.authuser=r.id
            where p.permis =:id
        """, Map.of("id", id));
        if (stTmp.size() > 0) {
            throw new XError("Используется в привилегии пользователя [{0}]", stTmp.get(0).getString("fullname"));
        }

        stTmp = getMdb().loadQuery("""
            select *
            from Permis
            where parent =:id
        """, Map.of("id", id));
        if (stTmp.size() > 0) {
            throw new XError("hasChild");
        }

    }

    @DaoMethod
    public void delete(Map<String, Object> rec) throws Exception {
        checkTarget("adm:tml:del");
        validateRec(UtCnv.toString(rec.get("id")));
        String sql = """
            delete from Permis where id=:id;
        """;
        getMdb().execQuery(sql, Map.of("id", UtCnv.toString(rec.get("id"))));
    }

    @DaoMethod
    public Store update(Map<String, Object> params) throws Exception {
        checkTarget("adm:tml:upd");

        Map<String, Object> rec = UtCnv.toMap(params.get("rec"));
        String sql = """
            update Permis set text=:text where id=:id;
        """;
        getMdb().execQuery(sql, rec);
        //
        Store st = getMdb().createStore("Permis");
        return getMdb().loadQuery(st, "select * from Permis where id=:id", Map.of("id",
                rec.get("id")));
    }

    @DaoMethod
    public Store insert(Map<String, Object> params) throws Exception {
        checkTarget("adm:tml:ins");
        Map<String, Object> rec = UtCnv.toMap(params.get("rec"));
        String sql = """
            insert into Permis (id, parent, text, ord)
            values (:id, :parent, :text, :ord)
        """;
        int ord = getMdb().loadQuery("select max(ord) as max from Permis").get(0).getInt("max");
        rec.put("ord", ord+1);
        getMdb().execQuery(sql, rec);
        //
        Store st = getMdb().createStore("Permis");
        getMdb().loadQuery(st, "select * from Permis where id=:id", Map.of("id", rec.get("id")));
        //
        //getMdb().outTable(st);
        return st;
    }

    @DaoMethod
    public Set<String> getLeaf(String id) throws Exception {
        Store st = getMdb().createStore("Permis");
        getMdb().loadQuery(st, """
            select * from Permis
        """);
        StoreIndex stInd = st.getIndex("id");
        Set<String> leaf = new HashSet<>();

        StoreRecord record = stInd.get(id);
        while (true) {
            if (record != null) {
                leaf.add(record.getString("id"));
                record = stInd.get(record.getString("parent"));
            } else {
                break;
            }
        }
        return leaf;
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
