package tofi.mdl.model.dao.reltyp;

import jandcode.commons.UtCnv;
import jandcode.core.dbm.mdb.BaseMdbUtils;
import jandcode.core.store.Store;
import jandcode.core.store.StoreRecord;

import java.util.Map;

public class RelTypRoleMdbUtils extends BaseMdbUtils {

    public Store loadRelTypRole(long reltyp) throws Exception {
        Store st = getMdb().createStore("RelTypRole.full");

        getMdb().loadQuery(st, """
                  select r.*, v.name, v.fullName from RelTypRole r, Role v
                  where r.role=v.id and r.reltyp=:t
                """, Map.of("t", reltyp));

        return st;
    }

    public Store selectRelTypRole(long reltyp) throws Exception {
        return getMdb().loadQuery("""
                  select id, name from Role where id not in (select role from RelTypRole where reltyp=:t)
                """, Map.of("t", reltyp));
    }

    public Store insertRelTypRole(Map<String, Object> params) throws Exception {
        Map<String, Object> rec = UtCnv.toMap(params.get("rec"));
        Store st = getMdb().createStore("RelTypRole");
        StoreRecord r = st.add(rec);
        //
        long id = getMdb().insertRec("RelTypRole", r, true);

        //
        st = getMdb().createStore("RelTypRole.full");
        getMdb().loadQuery(st, """
                  select r.*, v.name, v.fullName from RelTypRole r, Role v where r.role=v.id and r.id=:id
                """, Map.of("id", id));
        return st;
    }

    public Store updateRelTypRole(Map<String, Object> params) throws Exception {
        Map<String, Object> rec = UtCnv.toMap(params.get("rec"));
        long id = UtCnv.toLong(rec.get("id"));
        Store st = getMdb().createStore("RelTypRole");
        StoreRecord r = st.add(rec);
        //
        getMdb().updateRec("RelTypRole", r);
        //
        st = getMdb().createStore("RelTypRole.full");
        getMdb().loadQuery(st, """
                  select r.*, v.name, v.fullName from RelTypRole r, Role v where r.role=v.id and r.id=:id
                """, Map.of("id", id));
        return st;
    }


    public void deleteRelTypRole(Map<String, Object> rec) throws Exception {
        long id = UtCnv.toLong(rec.get("id"));
        getMdb().execQuery("delete from RelTypRoleLifeInterval where reltypRole=:tr", Map.of("tr", id));
        getMdb().deleteRec("RelTypRole", id);
    }

    /////
    public Store loadRelTypRoleLife(long reltypRole) throws Exception {
        Store st = getMdb().createStore("RelTypRoleLifeInterval");
        getMdb().loadQuery(st, "select * from RelTypRoleLifeInterval where reltypRole=:tr", Map.of("tr", reltypRole));
        return st;
    }

    public Store insertRelTypRoleLife(Map<String, Object> params) throws Exception {
        Map<String, Object> rec = UtCnv.toMap(params.get("rec"));
        Store st = getMdb().createStore("RelTypRoleLifeInterval");
        StoreRecord r = st.add(rec);
        if (r.getString("dbeg").equals("0000-01-01"))
            r.set("dbeg", "1800-01-01");
        if (r.getString("dend").equals("0000-01-01"))
            r.set("dend", "3333-12-31");
        long id = getMdb().insertRec("RelTypRoleLifeInterval", r, true);
        //
        st = getMdb().createStore("RelTypRoleLifeInterval");
        getMdb().loadQuery(st, """
                  select * from RelTypRoleLifeInterval where id=:id
                """, Map.of("id", id));
        return st;
    }

    public Store updateRelTypRoleLife(Map<String, Object> params) throws Exception {
        Map<String, Object> rec = UtCnv.toMap(params.get("rec"));
        long id = UtCnv.toLong(rec.get("id"));
        Store st = getMdb().createStore("RelTypRoleLifeInterval");
        StoreRecord r = st.add(rec);
        if (r.getString("dbeg").equals("0000-01-01"))
            r.set("dbeg", "1800-01-01");
        if (r.getString("dend").equals("0000-01-01"))
            r.set("dend", "3333-12-31");
        getMdb().updateRec("RelTypRoleLifeInterval", r);
        //
        st = getMdb().createStore("RelTypRoleLifeInterval");
        getMdb().loadQuery(st, """
                  select * from RelTypRoleLifeInterval where id=:id
                """, Map.of("id", id));
        return st;
    }

    public void deleteRelTypRoleLife(Map<String, Object> rec) throws Exception {
        long id = UtCnv.toLong(rec.get("id"));
        getMdb().deleteRec("RelTypRoleLifeInterval", id);
    }
}
