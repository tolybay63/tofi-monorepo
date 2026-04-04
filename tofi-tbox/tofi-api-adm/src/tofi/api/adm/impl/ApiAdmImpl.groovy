package tofi.api.adm.impl

import jandcode.commons.UtCnv
import jandcode.commons.UtString
import jandcode.commons.error.XError
import jandcode.core.dbm.mdb.BaseMdbUtils
import jandcode.core.store.Store
import jandcode.core.store.StoreRecord
import tofi.api.adm.ApiAdm
import tofi.api.adm.ApiMeta
import tofi.apinator.ApinatorApi
import tofi.apinator.ApinatorService

class ApiAdmImpl extends BaseMdbUtils implements ApiAdm {
    ApinatorApi apiMeta() { return app.bean(ApinatorService).getApi("meta") }

    @Override
    Map<String, Object> getUserInfo(String login, String passwd) {
        Map<String, Object> attrs = [:]
        String p1 = UtString.md5Str(passwd)

        Store st = mdb.loadQuery("""
                    select id, login, accesslevel, fullname as name, email, phone from authuser
                    where login=:l and passwd=:p
                """, Map.of("l", login, "p", p1))
        if (st.size() != 0) {
            attrs.putAll(st.get(0).getValues())
            Store stPer = mdb.loadQuery("""
                select distinct permis
                from (
                    select permis 
                    from authuserpermis up
                    where up.authuser=:u
                    union all
                    select up.permis  
                    from authroleuser ru 
                    inner join authrolepermis up on ru.authrole=up.authrole  
                    where ru.authuser=:u
                ) t
            """, Map.of("u", UtCnv.toLong(st.get(0).getLong("id"))))

            Set<Object> sp = stPer.getUniqueValues("permis")
            String target = UtString.join(sp, ",")
            attrs.put("target", target)
            //
            String mm = apiMeta().get(ApiMeta).getIdMetaModel()
            attrs.put("metamodel", mm)
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
    long regUser(Map<String, Object> rec) {
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
        return mdb.insertRec("AuthUser", r, true);
    }

    @Override
    void deleteAuthUser(long id) {
        mdb.execQuery("""
            delete from AuthUser where id=:id
        """, Map.of("id", id))
    }

    @Override
    Store loadSql(String sql, String domain) {
        if (domain=="")
            return mdb.loadQuery(sql)
        else {
            Store st = mdb.createStore(domain)
            return mdb.loadQuery(st, sql)
        }
    }


}
