package tofi.api.adm.impl

import jandcode.commons.UtCnv
import jandcode.commons.UtString
import jandcode.commons.error.XError
import jandcode.core.dbm.mdb.BaseMdbUtils
import jandcode.core.store.Store
import jandcode.core.store.StoreRecord
import tofi.api.adm.ApiAdm

class ApiAdmImpl extends BaseMdbUtils implements ApiAdm {
    @Override
    Map<String, Object> getUserInfo(String login, String passwd, String app) {
        Map<String, Object> attrs = [:]
        String p1 = UtString.md5Str(passwd)

        Store st = mdb.loadQuery("""
                    select * from authuser where login=:l and passwd=:p
                """, Map.of("l", login, "p", p1))
        if (st.size() != 0) {
            attrs.putAll(st.get(0).getValues())
            Store stPer = mdb.loadQuery("""
                WITH RECURSIVE r AS (
                   SELECT *
                   FROM permis
                   WHERE parent=:app
                   union ALL
                   SELECT t.*
                   FROM ( SELECT * FROM permis ) t JOIN r ON t.parent = r.id
                ),
                o as (
                    SELECT * FROM permis WHERE id=:app
                ),
                perm as (
                    SELECT * FROM o
                    union ALL
                    SELECT * FROM r where 0=0
                )
                SELECT distinct p.id as permis
                FROM perm p
                    inner join (
                        select distinct * from (
                            select a2.permis
                            from authroleuser a
                                left join authrolepermis a2  on a2.authrole=a.authrole
                            where a.authuser=:u
                            union all
                            select permis
                            from authuserpermis
                            where authuser=:u
                        ) e where 0=0
                    ) t on t.permis=p.id
            """, Map.of("u", UtCnv.toLong(st.get(0).getLong("id")),
            "app", app))

            Set<Object> sp = stPer.getUniqueValues("permis")
            String target = UtString.join(sp, ",")
            attrs.put("target", target)
        }
        return attrs
    }

    @Override
    Store loadAuthUser(long id) {
        Store st = mdb.createStore("AuthUser")
        mdb.loadQuery(st, """
                select u.id, u.login, u.email, u.phone, u.name, u.fullname, g.name as userGroup
                from authuser u, authusergr g
                where u.authusergr=g.id and u.id=:id
        """, Map.of("id", id))

        return st
    }

    @Override
    void regUser(Map<String, Object> rec) {
        String psw = UtString.md5Str(UtCnv.toString(rec.get("passwd")));
        String login = UtString.toString(rec.get("login")).trim();
        Store st = mdb.loadQuery("""
                    select id from AuthUser where login like :l
                """, Map.of("l", login));
        if (st.size() > 0) {
            throw new XError("loginExists");
        }

        rec.put("passwd", psw);

        //
        st = mdb.createStore("AuthUser");
        StoreRecord r = st.add(rec);
        r.set("authUserGr", 2);
        r.set("locked", 0);
        mdb.insertRec("AuthUser", r, true);
    }
}
