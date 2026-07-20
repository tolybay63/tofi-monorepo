package tofi.adm.model.dao.auth;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import jandcode.commons.UtCnv;
import jandcode.commons.UtString;
import jandcode.commons.error.XError;
import jandcode.core.auth.AuthService;
import jandcode.core.auth.AuthUser;
import jandcode.core.dao.DaoMethod;
import jandcode.core.dbm.mdb.BaseMdbUtils;
import jandcode.core.store.Store;
import jandcode.core.store.StoreRecord;
import tofi.api.adm.ApiAdm;
import tofi.apinator.ApinatorApi;
import tofi.apinator.ApinatorService;

import java.util.Arrays;
import java.util.Map;

public class AuthMdbUtils extends BaseMdbUtils {

    ApinatorApi apiAdm() { return getApp().bean(ApinatorService.class).getApi("adm"); }
    private final Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);

    @DaoMethod
    public void forceChangePsw(String login, String passwd) {
        apiAdm().get(ApiAdm.class).forceChangePsw(login, passwd);
    }

    @DaoMethod
    public Map<String, Object> getUserInfo() {
        AuthService authSvc = getMdb().getApp().bean(AuthService.class);
        AuthUser au = authSvc.getCurrentUser();
        if (au==null) {
            throw new XError("notLoginned");
        }
        return au.getAttrs();
    }

    @DaoMethod
    public Store loadProfile(long id) throws Exception {
        checkTarget("default-target");
        Store st = getMdb().createStore("AuthUser");
        getMdb().loadQuery(st, "select * from AuthUser where id=:id", Map.of("id", id));
        st.get(0).set("passwd", null);
        return st;
    }

    @DaoMethod
    public void saveProfile(Map<String, Object> rec) throws Exception {
        checkTarget("default-target");
        if (rec.size() > 1)
            getMdb().updateRec("AuthUser", rec);
    }

    @DaoMethod
    public void savePsw(Map<String, Object> rec) throws Exception {
        checkTarget("default-target");
        String po;
        try {
            po = argon2.hash(3, 65536, 4, UtCnv.toString(rec.get("passwdold")).toCharArray());
        } catch (Exception ignored) {
            throw new XError("Ошибка хэширования");
        }
        Store st = getMdb().loadQuery("select passwd from AuthUser where id=:id",
                Map.of("id", UtCnv.toLong(rec.get("id"))));

        if (po.equals(st.get(0).getString("passwd"))) {
            String p;
            try {
                p = argon2.hash(3, 65536, 4, UtCnv.toString(rec.get("passwd")).toCharArray());
            } catch (Exception ignored) {
                throw new XError("Ошибка хэширования");
            }

            getMdb().updateRec("AuthUser",
                    Map.of("id", UtCnv.toLong(rec.get("id")), "passwd", p));
        } else {
            throw new XError("invalidOldPasswd");
        }
    }

    @DaoMethod
    public void regUser(Map<String, Object> rec) throws Exception {
        String psw = UtCnv.toString(rec.get("passwd"));
        String login = UtString.toString(rec.get("login")).trim();
        Store st = getMdb().loadQuery("""
                    select id from AuthUser where login like :l
                """, Map.of("l", login));
        if (st.size() > 0) {
            throw new XError("loginExists");
        }

        String newHash;
        try {
            newHash = argon2.hash(3, 65536, 4, psw.toCharArray());
        } catch (Exception ignored) {
            throw new XError("Ошибка хэширования");
        }
        rec.put("passwd", newHash);
        rec.put("passwd_algo", "argon2id");
        rec.put("force_change", 0);
        //
        st = getMdb().createStore("AuthUser");
        StoreRecord r = st.add(rec);
        r.set("authUserGr", 2);
        r.set("locked", 0);
        getMdb().insertRec("AuthUser", r, true);
    }

    @DaoMethod
    public void forgetPasswd(String login) {
        apiAdm().get(ApiAdm.class).forgetPasswd(login);
    }

    @DaoMethod
    public void checkTarget(String target) {
        AuthService authService = getModel().getApp().bean(AuthService.class);
        AuthUser usr = authService.getCurrentUser();

        if (getMdb().getApp().getEnv().isDev()) {
            System.out.println("--- DEBUG ---");
            System.out.println("Target: " + target);
            System.out.println("User ID from Attrs: " + usr.getAttrs().getLong("id"));
            System.out.println("User Login: " + usr.getAttrs().getString("login"));
            System.out.println("-------------");
        }

        if (usr.getAttrs().getLong("id") == 1) return;

        if (usr.getAttrs().getLong("id") == 0)
            throw new XError("notLoginned");

        String userTargets = usr.getAttrs().getString("target", "");
        String [] targets = userTargets.trim().split("\\s*,\\s*");
        if (!Arrays.asList(targets).contains(target)) {
            if (target.equals("adm")) {
                throw new XError("notAccessService");
            }
            throw new XError("notAccess");
        }
    }


}
