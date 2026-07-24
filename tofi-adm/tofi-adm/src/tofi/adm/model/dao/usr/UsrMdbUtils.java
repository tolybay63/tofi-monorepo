package tofi.adm.model.dao.usr;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import jandcode.commons.UtCnv;
import jandcode.commons.UtString;
import jandcode.commons.error.XError;
import jandcode.core.auth.AuthService;
import jandcode.core.auth.AuthUser;
import jandcode.core.dao.DaoMethod;
import jandcode.core.dbm.mdb.BaseMdbUtils;
import jandcode.core.std.CfgService;
import jandcode.core.store.Store;
import jandcode.core.store.StoreRecord;
import tofi.api.adm.utils.PasswordGenerator;
import tofi.api.dta.ApiPersonnelData;
import tofi.api.dta.ApiUserData;
import tofi.api.mdl.ApiMeta;
import tofi.api.mdl.model.consts.FD_AccessLevel_consts;
import tofi.apinator.ApinatorApi;
import tofi.apinator.ApinatorService;

import java.util.*;

public class UsrMdbUtils extends BaseMdbUtils {

    ApinatorApi apiMeta() {
        return getApp().bean(ApinatorService.class).getApi("meta");
    }
    ApinatorApi apiUserData() {
        return getApp().bean(ApinatorService.class).getApi("userdata");
    }
    ApinatorApi apiPersonnelData() {
        return getApp().bean(ApinatorService.class).getApi("personneldata");
    }

    private final Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);

    String checkAccount(long id) throws Exception {
        Map<String, Long> mapCods = apiMeta().get(ApiMeta.class).getIdFromCodOfEntity("Prop", "", "Prop_");
        Set<Object> setCls = apiMeta().get(ApiMeta.class).setIdsOfCls("Typ_Users");
        if (setCls.isEmpty()) setCls.add(0L);

        Store st = apiUserData().get(ApiUserData.class).infoUser(mapCods, id, UtString.join(setCls, ","), "0");
        if (st.size() > 0)
            return st.get(0).getString("name");
        else
            return "";
    }

    @DaoMethod
    public Store loadGroup(Map<String, Object> params) throws Exception {
        checkTarget("adm:usr");
        //
        Store st = getMdb().createStore("AuthUserGr");
        AuthService authSvc = getMdb().getApp().bean(AuthService.class);
        AuthUser au = authSvc.getCurrentUser();
        long al = au.getAttrs().getLong("accesslevel");
        getMdb().loadQuery(st, """
                    select * from authusergr
                    where id in (
                        select authusergr from authuser
                        where accesslevel <= :al
                    ) or id not in (
                        select authusergr from authuser
                        where accesslevel <= :al
                    )
                """, Map.of("al", al));
        return st;
    }

    @DaoMethod
    public Store loadGroupForSelect(long gr) throws Exception {
        Store st = getMdb().createStore("AuthUserGr");
        AuthService authSvc = getMdb().getApp().bean(AuthService.class);
        AuthUser au = authSvc.getCurrentUser();
        long al = au.getAttrs().getLong("accesslevel");

        getMdb().loadQuery(st, """
                    select * from authusergr
                    where (id in (
                        select authusergr from authuser
                        where accesslevel <= :al
                    ) or id not in (
                        select authusergr from authuser
                        where accesslevel <= :al
                    )) and id <> :id
                """, Map.of("al", al, "id", gr));

        Set<Object> ids = st.getUniqueValues("id");
        st.forEach(r -> {
            if (!ids.contains(r.getLong("parent")))
                r.set("parent", null);
        });
        return st;
    }

    @DaoMethod
    public Store load(long gr) throws Exception {
        Store st = getMdb().createStore("AuthUser");
        return getMdb().loadQuery(st, "select * from AuthUser where authUserGr=:gr", Map.of("gr", gr));
    }

    @DaoMethod
    public Store loadUser(long id) throws Exception {
        Store st = getMdb().createStore("AuthUser");
        return getMdb().loadQuery(st, "select * from AuthUser where id=:id", Map.of("id", id));
    }

    @DaoMethod
    public Store insertGr(Map<String, Object> params) throws Exception {
        Map<String, Object> rec = UtCnv.toMap(params.get("rec"));
        Store st = getMdb().createStore("AuthUserGr");
        StoreRecord record = st.add(rec);
        record.set("id", null);
        long id = getMdb().insertRec("AuthUserGr", record, true);
        st = getMdb().createStore("AuthUserGr");
        getMdb().loadQuery(st, "select * from AuthUserGr where id=:id", Map.of("id", id));
        return st;
    }

    @DaoMethod
    public Store updateGr(Map<String, Object> params) throws Exception {
        Map<String, Object> rec = UtCnv.toMap(params.get("rec"));
        Store st = getMdb().createStore("AuthUserGr");
        StoreRecord record = st.add(rec);
        long id = UtCnv.toLong(rec.get("id"));
        if (id == 0) {
            throw new XError("Поле id должно иметь не нулевое значение");
        }
        long parentNew = UtCnv.toLong(rec.get("parent"));
        if (parentNew > 0) {
            StoreRecord reNew = getMdb().loadQueryRecord("select * from AuthUserGr where id=:id", Map.of("id", parentNew));
            if (reNew.getLong("id") == UtCnv.toLong(rec.get("parent")) &&
                    reNew.getLong("parent") == UtCnv.toLong(rec.get("id"))) {
                throw new XError("Циклическая ссылка id-parent");
            }
        }

        getMdb().updateRec("AuthUserGr", record);
        st = getMdb().createStore("AuthUserGr");
        getMdb().loadQuery(st, "select * from AuthUserGr where id=:id", Map.of("id", id));
        return st;
    }

    @DaoMethod
    public void deleteGr(long gr) throws Exception {
        getMdb().deleteRec("AuthUserGr", gr);
    }

    @DaoMethod
    public Store insert(Map<String, Object> rec) throws Exception {
        checkTarget("adm:usr:gr:usr:ins");
        Store st = getMdb().createStore("AuthUser");
        StoreRecord record = st.add(rec);

        // 1. Получаем сырой пароль
        String rawPassword = record.getString("passwd").replaceAll("", "");

        if (UtString.empty(rawPassword)) {
            throw new XError("password_cannot_be_empty");
        }
        //
        if (!PasswordGenerator.checkPasswd(rawPassword))
            throw new XError("Пароль должен состоять не менее чем из 8 знаков, содержать цифры, заглавные и прописные буквы латинского алфавита и специальные знаки (!@#^&_)");

        //
        try {
            // 2. Хэшируем через Argon2id с параметрами под сервер (3 итерации, 64МБ памяти, 4 потока)
            String argon2Hash = argon2.hash(3, 65536, 4, rawPassword.toCharArray());

            // 3. Записываем новый хэш и маркер алгоритма в запись
            record.set("passwd", argon2Hash);
            record.set("passwd_algo", "argon2id");
        } finally {
            // Очищаем массив символов в памяти в целях безопасности
            argon2.wipeArray(rawPassword.toCharArray());
        }

        record.set("id", null);
        long id = getMdb().insertRec("AuthUser", record, true);

        st = getMdb().createStore("AuthUser");
        getMdb().loadQuery(st, "select * from AuthUser where id=:id", Map.of("id", id));
        return st;

    }

    @DaoMethod
    public Store update(Map<String, Object> rec) throws Exception {
        Store st = getMdb().createStore("AuthUser");
        StoreRecord record = st.add(rec);
        long id = UtCnv.toLong(rec.get("id"));
        if (id == 0) {
            throw new XError("Поле id должно иметь не нулевое значение");
        }
        getMdb().updateRec("AuthUser", record);
        st = getMdb().createStore("AuthUser");
        getMdb().loadQuery(st, "select * from AuthUser where id=:id", Map.of("id", id));
        getMdb().resolveDicts(st);
        return st;
    }

    @DaoMethod
    public void delete(long id) throws Exception {
        checkTarget("adm:usr:gr:usr:del");
        CfgService cfgSvc = getApp().bean(CfgService.class);
        String idmodel = cfgSvc.getConf().getString("dbsource/meta/id");
        if (Objects.equals(idmodel, "test")) {
            getMdb().deleteRec("AuthUser", id);
        } else {
            // Check Personnel
            String wheUser = "v1.strval like '"+id+".0'";
            Map<String, Long> mapProp = apiMeta().get(ApiMeta.class).getIdFromCodOfEntity("Prop", "Prop_UserId", "");
            Store st = apiPersonnelData().get(ApiPersonnelData.class).loadSql("""
                    SELECT o.id, v.name
                    FROM Obj o
                        JOIN ObjVer v ON o.id = v.ownerVer AND v.lastVer = 1
                        JOIN DataProp d1 ON d1.objorrelobj = o.id AND d1.prop=
                    """+ mapProp.get("Prop_UserId") + """
                        JOIN DataPropVal v1 ON d1.id = v1.dataProp
                    WHERE
            """ + wheUser, "");

            if (st.size() > 0) {
                throw new XError("Пользователь привязан сотруднику "+st.get(0).getString("name"));
            }
            //
            String str = checkAccount(id);
            if (!str.isEmpty()) {
                throw new XError("Существует аккаунт пользователя [" + str + "]");
            }

            try {
                getMdb().execQuery("""
                    delete from AuthRoleUser where authUser=:id;
                    delete from AuthUserPermis where authUser=:id;
                    delete from UserRefreshToken where authUser=:id;
                """, Map.of("id", id));
            } finally {
                getMdb().deleteRec("AuthUser", id);
            }
        }
    }

    @DaoMethod
    public StoreRecord newRec(long gr) throws Exception {
        Store st = getMdb().createStore("AuthUser");
        StoreRecord r = st.add();
        r.set("authUserGr", gr);
        r.set("locked", 0);
        r.set("accessLevel", FD_AccessLevel_consts.common);
        return r;
    }

    @DaoMethod
    public Store loadUserPermis(long user) throws Exception {
        Store st = getMdb().createStore("Permis.full");
        return getMdb().loadQuery(st, """
                        with a as (
                            select permis, accessLevel from authuserpermis where authuser=:user
                        )
                        select p.*, a.accessLevel  from permis p, a
                        where p.id=a.permis
                        order by p.ord
                """, Map.of("user", user));
    }

    @DaoMethod
    public Store loadUserPermisForUpd(long user) throws Exception {
        Store st = getMdb().createStore("Permis.full");
        return getMdb().loadQuery(st, """
                    select p.*, a.accessLevel, a.id as idInTable, case when a.id is null then false else true end as checked
                    from permis p
                        left join authuserpermis a on p.id=a.permis and a.authuser=:user
                    order by p.ord
                """, Map.of("user", user));
    }

    @DaoMethod
    public void saveUserPermis(Map<String, Object> params) throws Exception {
        long user = UtCnv.toLong(params.get("user"));
        List<Map<String, Object>> lstData = (List<Map<String, Object>>) params.get("data");
        //Old ids
        Store oldSt = getMdb().loadQuery("select id from AuthUserPermis where authUser=:u", Map.of("u", user));
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
                    getMdb().deleteRec("AuthUserPermis", r.getLong("id"));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        // Saving
        Store st = getMdb().createStore("AuthUserPermis");
        for (Map<String, Object> map : lstData) {
            if (!oldIds.contains(UtCnv.toLong(map.get("idInTable")))) {
                StoreRecord r = st.add(map);
                r.set("id", null);
                r.set("authUser", user);
                r.set("permis", UtCnv.toString(map.get("id")));
                if (UtCnv.toLong(map.get("accessLevel")) > 0)
                    r.set("accessLevel", UtCnv.toLong(map.get("accessLevel")));
                getMdb().insertRec("AuthUserPermis", r, true);
            } else {
                StoreRecord r = st.add(map);
                r.set("id", UtCnv.toLong(map.get("idInTable")));
                r.set("authUser", user);
                r.set("permis", UtCnv.toString(map.get("id")));
                r.set("accessLevel", UtCnv.toLong(map.get("accessLevel")));
                getMdb().updateRec("AuthUserPermis", r);
            }
        }
    }

    @DaoMethod
    public Store loadUserRoles(long user) throws Exception {
        Store st = getMdb().createStore("AuthRole");
        return getMdb().loadQuery(st, """
                    select r.* from authroleuser u
                        left join authrole r on u.authrole=r.id
                    where u.authuser=:user
                """, Map.of("user", user));
    }

    @DaoMethod
    public Store loadUserRolesForUpd(long user) throws Exception {
        Store st = getMdb().createStore("AuthRole.full");
        return getMdb().loadQuery(st, """
                    select r.*,
                        case when u.id is null then false else true end as checked
                    from authrole r
                        left join authroleuser u on r.id=u.authrole and u.authuser=:user
                """, Map.of("user", user));
    }

    @DaoMethod
    public void saveUserRole(long user, List<Map<String, Long>> data) throws Exception {

        //Old ids : id(AuthRoleUser)
        Store oldSt = getMdb().loadQuery("select id, authrole from AuthRoleUser where authUser=:u", Map.of("u", user));
        Set<Object> oldIds = oldSt.getUniqueValues("authrole");

        //New ids
        Set<Object> newIds = new HashSet<>();
        for (Map<String, Long> map : data) {
            newIds.add(UtCnv.toLong(map.get("id")));
        }

        //Deleting
        for (StoreRecord r : oldSt) {
            if (!newIds.contains(r.getLong("authrole"))) {
                try {
                    getMdb().deleteRec("AuthRoleUser", r.getLong("id"));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        // Saving
        Store st = getMdb().createStore("AuthRoleUser");
        for (Map<String, Long> map : data) {
            if (!oldIds.contains(UtCnv.toLong(map.get("id")))) {
                StoreRecord r = st.add(map);
                long id = getMdb().getNextId("AuthRoleUser");
                r.set("id", id);
                r.set("authUser", user);
                r.set("authRole", UtCnv.toLong(map.get("id")));
                r.set("ord", id);
                getMdb().insertRec("AuthRoleUser", r, false);
            }
        }
    }

    @DaoMethod
    public void checkTarget(String target) {
        AuthService authService = getModel().getApp().bean(AuthService.class);
        AuthUser usr = authService.getCurrentUser();

        if (getApp().getEnv().isDev()) {
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
            if (Objects.equals(target, "adm")) {
                throw new XError("notAccessService");
            }
            throw new XError("notAccess");
        }
    }


}
