package tofi.auth;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import jandcode.commons.UtCnv;
import jandcode.commons.UtString;
import jandcode.commons.datetime.XDateTime;
import jandcode.commons.error.XError;
import jandcode.core.dao.DaoMethod;
import jandcode.core.dbm.mdb.BaseMdbUtils;
import jandcode.core.std.CfgService;
import tofi.api.adm.ApiAdm;
import tofi.api.adm.utils.PasswordGenerator;
import tofi.apinator.ApinatorService;

import java.util.Map;

public class TofiPasswordResetAction extends BaseMdbUtils {

    private final Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);

    private ApiAdm getAdmApi() {
        return getApp().bean(ApinatorService.class).getApi("adm").get(ApiAdm.class);
    }

    /**
     * 1. МЕТОД: Запрос на восстановление пароля
     */
    @DaoMethod
    public void forgetPasswd(Map<String, Object> params) throws Exception {
        if (params == null || !params.containsKey("login")) {
            throw new XError("invalid_params");
        }

        String login = UtCnv.toString(params.get("login"));
        if (UtString.empty(login)) {
            throw new XError("invalid_params");
        }

        Map<String, Object> userInfo = getAdmApi().getUserInfo(login);
        if (userInfo == null || userInfo.isEmpty()) {
            return;
        }

        String rawTempPassword = PasswordGenerator.generateTemporaryPassword(8);
        String tempHash = argon2.hash(3, 65536, 4, rawTempPassword.toCharArray());
        XDateTime expiredAt = XDateTime.now().addDays(1);

        getAdmApi().execSqlWithParams("""
            update authuser
            set temp_passwd = :temp_passwd,
                temp_passwd_expired = :expired
            where login = :login
        """, Map.of("temp_passwd", tempHash, "expired", expiredAt, "login", login));


        String url = "http://127.0.0.1";

        if (!getMdb().getApp().getEnv().isDev()) {
            CfgService cfgSvc = getApp().bean(CfgService.class);
            url = cfgSvc.getConf().getString("auth/main/url");
        }

        String confirmLink = url + ":8181/admin/#/confirm-pws?login=" + login;

        if (!getMdb().getApp().getEnv().isDev())
            confirmLink = url + "/fish/admin/#/confirm-pws?login=" + login;

        getAdmApi().send("Сброс пароля",
                "Ваш разовый пароль: " + rawTempPassword +
                        "\nДля активации перейдите по ссылке: " + confirmLink,
                UtCnv.toString(userInfo.get("email")));
    }

    /**
     * 2. МЕТОД: Активация пароля при переходе по ссылке
     */
    @DaoMethod // И сюда тоже ставим
    public void confirmPasswd(Map<String, Object> params) throws Exception {
        if (params == null || !params.containsKey("login")) {
            throw new XError("Ссылка недействительна или повреждена");
        }

        String login = UtCnv.toString(params.get("login"));
        if (UtString.empty(login)) {
            throw new XError("Ссылка недействительна или повреждена");
        }

        Map<String, Object> userInfo = getAdmApi().getUserInfo(login);
        if (userInfo == null || userInfo.isEmpty()) {
            throw new XError("Пользователь не найден");
        }

        String tempPasswd = (String) userInfo.get("temp_passwd");
        XDateTime expiredAt = (XDateTime) userInfo.get("temp_passwd_expired");

        if (UtString.empty(tempPasswd) || expiredAt == null) {
            throw new XError("Запрос на сброс пароля не создавался или уже активирован");
        }

        if (expiredAt.toJavaLocalDateTime().isBefore(XDateTime.now().toJavaLocalDateTime())) {
            throw new XError("Срок действия ссылки истек. Запросите сброс заново");
        }

        getAdmApi().execSqlWithParams("""
            update authuser
            set passwd = temp_passwd,
                passwd_algo = 'argon2id',
                force_change = 1,
                temp_passwd = null,
                temp_passwd_expired = null
            where login = :login
        """, Map.of("login", login));
    }
}