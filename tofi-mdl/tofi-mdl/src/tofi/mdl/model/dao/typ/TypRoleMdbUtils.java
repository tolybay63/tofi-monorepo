package tofi.mdl.model.dao.typ;

import jandcode.commons.UtCnv;
import jandcode.core.dao.DaoMethod;
import jandcode.core.dbm.mdb.BaseMdbUtils;
import jandcode.core.store.Store;
import jandcode.core.store.StoreRecord;

import java.util.Map;


public class TypRoleMdbUtils extends BaseMdbUtils {

    @DaoMethod
    public Store loadTypRole(long typ) throws Exception {
        Store st = getMdb().createStore("TypRole.full");

        getMdb().loadQuery(st, """
                  select r.*, v.name, v.fullName from TypRole r, Role v
                  where r.role=v.id and r.typ=:t
                """, Map.of("t", typ));

        return st;
    }

    @DaoMethod
    public Store selectTypRole(long typ) throws Exception {
        return getMdb().loadQuery("""
                  select id, name from Role where id not in (select role from TypRole where typ=:t)
                """, Map.of("t", typ));
    }

    @DaoMethod
    public Store insertTypRole(Map<String, Object> params) throws Exception {
        Map<String, Object> rec = UtCnv.toMap(params.get("rec"));
        Store st = getMdb().createStore("TypRole");
        StoreRecord r = st.add(rec);
        //
        long id = getMdb().insertRec("TypRole", r, true);

        //
        st = getMdb().createStore("TypRole.full");
        getMdb().loadQuery(st, """
                  select r.*, v.name, v.fullName from TypRole r, Role v where r.role=v.id and r.id=:id
                """, Map.of("id", id));
        return st;
    }

    @DaoMethod
    public Store updateTypRole(Map<String, Object> params) throws Exception {
        Map<String, Object> rec = UtCnv.toMap(params.get("rec"));
        long id = UtCnv.toLong(rec.get("id"));
        Store st = getMdb().createStore("TypRole");
        StoreRecord r = st.add(rec);
        //
        getMdb().updateRec("TypRole", r);

        //
        st = getMdb().createStore("TypRole.full");
        getMdb().loadQuery(st, """
                  select r.*, v.name, v.fullName from TypRole r, Role v where r.role=v.id and r.id=:id
                """, Map.of("id", id));
        return st;
    }

    @DaoMethod
    public void deleteTypRole(Map<String, Object> rec) throws Exception {
        long id = UtCnv.toLong(rec.get("id"));
        getMdb().execQuery("delete from TypRoleLifeInterval where typRole=:tr", Map.of("tr", id));
        getMdb().deleteRec("TypRole", id);
    }

    /// //
    @DaoMethod
    public Store loadTypRoleLife(long typRole) throws Exception {
        Store st = getMdb().createStore("TypRoleLifeInterval");
        getMdb().loadQuery(st, "select * from TypRoleLifeInterval where typRole=:tr", Map.of("tr", typRole));
        return st;
    }

    @DaoMethod
    public Store insertTypRoleLife(Map<String, Object> params) throws Exception {
        Map<String, Object> rec = UtCnv.toMap(params.get("rec"));
        Store st = getMdb().createStore("TypRoleLifeInterval");
        StoreRecord r = st.add(rec);
        if (r.getString("dbeg").equals("0000-01-01"))
            r.set("dbeg", "1800-01-01");
        if (r.getString("dend").equals("0000-01-01"))
            r.set("dend", "3333-12-31");
        long id = getMdb().insertRec("TypRoleLifeInterval", r, true);
        //
        st = getMdb().createStore("TypRoleLifeInterval");
        getMdb().loadQuery(st, """
                  select * from TypRoleLifeInterval where id=:id
                """, Map.of("id", id));
        return st;
    }

    @DaoMethod
    public Store updateTypRoleLife(Map<String, Object> params) throws Exception {
        Map<String, Object> rec = UtCnv.toMap(params.get("rec"));
        long id = UtCnv.toLong(rec.get("id"));
        Store st = getMdb().createStore("TypRoleLifeInterval");
        StoreRecord r = st.add(rec);
        if (r.getString("dbeg").equals("0000-01-01"))
            r.set("dbeg", "1800-01-01");
        if (r.getString("dend").equals("0000-01-01"))
            r.set("dend", "3333-12-31");
        getMdb().updateRec("TypRoleLifeInterval", r);
        //
        st = getMdb().createStore("TypRoleLifeInterval");
        getMdb().loadQuery(st, """
                  select * from TypRoleLifeInterval where id=:id
                """, Map.of("id", id));
        return st;
    }

    @DaoMethod
    public void deleteTypRoleLife(Map<String, Object> rec) throws Exception {
        long id = UtCnv.toLong(rec.get("id"));
        getMdb().deleteRec("TypRoleLifeInterval", id);
    }
}
