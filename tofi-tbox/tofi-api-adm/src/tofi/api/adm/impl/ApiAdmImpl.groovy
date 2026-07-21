package tofi.api.adm.impl

import de.mkammerer.argon2.Argon2
import de.mkammerer.argon2.Argon2Factory
import jandcode.commons.UtCnv
import jandcode.commons.UtString
import jandcode.commons.datetime.XDateTime
import jandcode.commons.error.XError
import jandcode.core.dbm.mdb.BaseMdbUtils
import jandcode.core.std.CfgService
import jandcode.core.store.Store
import jandcode.core.store.StoreRecord
import tofi.api.adm.ApiAdm
import tofi.api.adm.ApiMeta
import tofi.api.adm.utils.MailSender
import tofi.api.adm.utils.PasswordGenerator
import tofi.apinator.ApinatorApi
import tofi.apinator.ApinatorService

class ApiAdmImpl extends BaseMdbUtils implements ApiAdm {

    ApinatorApi apiMeta() { return app.bean(ApinatorService).getApi("meta") }
    private final Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id)

    @Override
    Map<String, Object> getUserInfo(String login) {
        Map<String, Object> attrs = [:]

        // 1. Ищем пользователя только по логину
        Store st = mdb.loadQuery("""
            select id, login, passwd_algo, passwd, force_change, accesslevel, fullname as name, email, phone,
                   failed_attempts, locked_until, lockout_count, temp_passwd, temp_passwd_expired
            from authuser
            where login=:l
        """, Map.of("l", login))

        if (st.size() == 0) return null // Пользователь не найден

        attrs.putAll(st.get(0).getValues())

        Store stPer = mdb.loadQuery("""
            select distinct permis from (
                select permis from authuserpermis where authuser=:u
                union all
                select up.permis from authroleuser ru 
                inner join authrolepermis up on ru.authrole=up.authrole where ru.authuser=:u
            ) t
        """, Map.of("u", UtCnv.toLong(attrs.get("id"))))

        Set<Object> sp = stPer.getUniqueValues("permis")
        attrs.put("target", UtString.join(sp, ","))
        attrs.put("metamodel", apiMeta().get(ApiMeta).getIdMetaModel())
        return attrs
    }

    @Override
    void saveRefreshToken(Object authUserId, String tokenHash, XDateTime expiresAt) {
        Map<String, Object> params = new HashMap<>()
        params.put("authuser", authUserId)
        params.put("tokenhash", tokenHash)
        params.put("expiresat", expiresAt)
        params.put("createdat", XDateTime.now())

        getMdb().execQuery("insert into userrefreshtoken (id, authuser, tokenhash, expiresat, createdat) " +
                "values (nextval('g_userrefreshtoken'), :authuser, :tokenhash, :expiresat, :createdat)", params)
    }

    Long findUserIdByRefreshToken(String tokenHash) {
        Map<String, Object> params = new HashMap<>()
        params.put("tokenhash", tokenHash)
        params.put("now", XDateTime.now())

        // Ищем запись в таблице. Запрос вернет authuser, если хэш совпал и срок не истек
        var rows = getMdb().loadQuery(
                "select authuser from userrefreshtoken " +
                        "where tokenhash = :tokenhash and expiresat > :now",
                params
        )

        if (rows.isEmpty()) {
            return null
        }

        // Возвращаем ID пользователя (в Jc-2 это long/Long)
        return rows.get(0).getLong("authuser")
    }

    void deleteRefreshToken(String tokenHash) {
        Map<String, Object> params = new HashMap<>()
        params.put("tokenhash", tokenHash)

        // Удаляем конкретную сессию из базы
        getMdb().execQuery("delete from userrefreshtoken where tokenhash = :tokenhash", params)
    }

    void updatePasswordAndAlgo(String login, String passwd, String passwd_algo) {
        mdb.startTran() // Открываем транзакцию
        try {
            mdb.execQuery("""
                UPDATE AuthUser 
                SET passwd = :passwd, passwd_algo = :passwd_algo 
                WHERE login = :login
            """, Map.of("passwd", passwd, "passwd_algo", passwd_algo, "login", login))

            mdb.commit() // Фиксируем изменения, снимаем блокировку со строки
        } catch (Exception e) {
            mdb.rollback() // В случае ошибки откатываемся, чтобы не запереть строку
            throw e
        }
    }

    @Override
    void updateAuthStats(String login, boolean success) {
        if (success) {
            // Сброс при успешном входе
            mdb.execQuery("update authuser set failed_attempts=0, locked_until=null, lockout_count=0 where login=:l", [l: login])
        } else {
            // Читаем текущее состояние
            Store st = mdb.loadQuery("select failed_attempts, lockout_count from authuser where login=:l", [l: login])
            if (st.size() == 0) return

            int attempts = st.get(0).getInt("failed_attempts") + 1
            int lockouts = st.get(0).getInt("lockout_count")

            if (attempts >= 3) {
                // Рассчитываем время блокировки (5, 15 или 60 мин)
                int minutes = (lockouts == 0) ? 5 : (lockouts == 1 ? 15 : 60)
                mdb.execQuery("""
                        update authuser set 
                            locked_until = now() + interval '1 minute' * :m, 
                            lockout_count = lockout_count + 1, 
                            failed_attempts = 0 
                        where login = :l
                    """, [m: minutes, l: login])
            } else {
                // Просто прибавляем попытку
                mdb.execQuery("update authuser set failed_attempts = :a where login = :l", [a: attempts, l: login])
            }
        }
    }

    @Override
    void forceChangePsw(String login, String passwd) {

        if (!PasswordGenerator.checkPasswd(passwd))
            throw new XError("Пароль должен состоять не менее чем из 8 знаков, содержать цифры, заглавные и прописные буквы латинского алфавита и специальные знаки (!@#^&_)");

        String newHash;
        try {
            newHash = argon2.hash(3, 65536, 4, passwd.toCharArray());
        } catch (Exception ignored) {
            throw new XError("Ошибка хэширования");
        }

        mdb.execQuery("""
            update authuser 
            set passwd = :passwd, 
                passwd_algo = 'argon2id', 
                force_change = 0 
            where login = :login
        """, Map.of("passwd", newHash, "login", login));
    }

    @Override
    Store loadAuthUser(long id) {
        Store st = mdb.createStore("AuthUser")
        mdb.loadQuery(st, """
                select u.id, u.login, u.accesslevel, u.email, u.phone, u.name, u.fullname, g.name as userGroup
                from authuser u, authusergr g
                where u.authusergr=g.id and u.id=:id
        """, Map.of("id", id))

        return st
    }

    @Override
    long regUser(Map<String, Object> rec) {
        String psw = UtCnv.toString(rec.get("passwd"))
        if (psw.isEmpty())
            throw new XError("Пароль пустой")

        String argon2Hash = argon2.hash(3, 65536, 4, psw.toCharArray())
        String login = UtString.toString(rec.get("login")).trim()
        Store st = mdb.loadQuery("""
                    select id from AuthUser where login like :l
                """, Map.of("l", login))
        if (st.size() > 0) {
            throw new XError("loginExists")
        }
        rec.put("passwd", argon2Hash)
        //
        st = mdb.createStore("AuthUser")
        StoreRecord r = st.add(rec)
        r.set("authUserGr", 2)
        r.set("locked", 0)
        return mdb.insertRec("AuthUser", r, true)
    }

    @Override
    void changePasswd(long user, String newPasswd) {
        Store st = mdb.loadQuery("""
            select passwd_algo from AuthUser where id=${user}
        """)
        if (st.size()==0)
            throw new XError("Пользователь не найден")
        String psw = argon2.hash(3, 65536, 4, newPasswd.toCharArray())
        mdb.execQuery("update AuthUser set passwd='${psw}' where id=${user}")
    }

    @Override
    void forgetPasswd(String login) {
        Store st = mdb.loadQuery("""
            select * from AuthUser where login like :l
        """, Map.of("l", login.trim()))
        if (st.size() == 0) {
            throw new XError("Не найден пользователь с логиом [{0}]", login)
        }

        StoreRecord newRec = mdb.createStoreRecord("AuthUser", st.get(0))
        //
        String newPasswd = PasswordGenerator.generateTemporaryPassword(8)
        //
        try {
            XDateTime expiredAt = XDateTime.now().addDays(1);
            // 2. Хэшируем через Argon2id с параметрами под сервер (3 итерации, 64МБ памяти, 4 потока)
            String argon2Hash = argon2.hash(3, 65536, 4, newPasswd.toCharArray());

            // 3. Записываем новый хэш и маркер алгоритма в запись
            newRec.set("temp_passwd", argon2Hash);
            newRec.set("temp_passwd_expired", expiredAt);
        } finally {
            // Очищаем массив символов в памяти в целях безопасности
            argon2.wipeArray(newPasswd.toCharArray())
        }
        mdb.updateRec("AuthUser", newRec)

        String url = "http://127.0.0.1";

        if (!getMdb().getApp().getEnv().isDev()) {
            CfgService cfgSvc = getApp().bean(CfgService.class);
            url = cfgSvc.getConf().getString("auth/main/url");
        }

        String confirmLink = url + ":8181/admin/#/confirm-pws?login=" + login;

        if (!getMdb().getApp().getEnv().isDev())
            confirmLink = url + "/fish/admin/#/confirm-pws?login=" + login;

        send("Сброс пароля",
                "Ваш разовый пароль: " + newPasswd +
                        "\nДля активации перейдите по ссылке: " + confirmLink, newRec.getString("email"))
    }

    @Override
    void send(String subject, String infoWithLink, String email) {
        MailSender mailSender = new MailSender(mdb)
        mailSender.send(subject, infoWithLink, email)
    }

    @Override
    void deleteAuthUser(long id) {
        mdb.execQuery("""
            delete from AuthUser where id=:id
        """, Map.of("id", id))
    }

    @Override
    Store loadSql(String sql, String domain) {
        if (domain == "")
            return mdb.loadQuery(sql)
        else {
            Store st = mdb.createStore(domain)
            return mdb.loadQuery(st, sql)
        }
    }

    @Override
    Store loadSqlWithParams(String sql, Map<String, Object> params) {
        return mdb.loadQuery(sql, params)
    }

    @Override
    void execSqlWithParams(String sql, Map<String, Object> params) {
        mdb.execQuery(sql, params)
    }

    @Override
    void updateEmailAndPhone(Map<String, Object> rec) {
        Store st = mdb.loadQuery("""
            select * from AuthUser where login='${rec.get("login")}' 
        """)
        if (st.size() == 0)
            throw new XError("Не найден пользователь с логиом [${rec.get('login')}]")

        String set = ""
        if (rec.containsKey("phone"))
            set = """, phone='${rec.get("phone")}'"""

        mdb.execQuery("""
            update AuthUser set email='${rec.get("email")}'${set} where login='${rec.get('login')}'
        """)
    }


}
