package tofi.auth;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import jandcode.commons.UtCnv;
import jandcode.commons.datetime.XDateTime;
import jandcode.commons.error.XError;
import jandcode.core.BaseComp;
import jandcode.core.auth.*;
import jandcode.core.std.CfgService;
import tofi.api.adm.ApiAdm;
import tofi.apinator.ApinatorService;

import java.util.HashMap;
import java.util.Map;

public class BaseTofiAuthProcessor extends BaseComp implements AuthProcessor {

    // Инициализируем фабрику Argon2id
    private final Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);

    @Override
    public boolean isSupportedAuthToken(AuthToken authToken) {
        return authToken instanceof UserPasswdAuthToken;
    }

    @Override
    public AuthUser login(AuthToken authToken) {
        UserPasswdAuthToken token = (UserPasswdAuthToken) authToken;
        String rawLogin = token.getUsername();
        String providedPasswd = token.getPasswd();

        String login = rawLogin;
        String clientType = "legacy";

        if (rawLogin != null && rawLogin.contains(":::")) {
            String[] parts = rawLogin.split(":::");
            if (parts.length >= 2) {
                login = parts[0];
                clientType = parts[1];
            }
        }

        var admApi = getApp().bean(ApinatorService.class).getApi("adm");
        var z = admApi.get(ApiAdm.class);

        Map<String, Object> fullAttrs = z.getUserInfo(login);

        if (fullAttrs == null || fullAttrs.isEmpty()) {
            throw new XError("invalid_user_passwd");
        }

        XDateTime lockedUntilX = UtCnv.toDateTime(fullAttrs.get("locked_until"));
        if (lockedUntilX != null) {
            java.time.LocalDateTime lockedUntil = lockedUntilX.toJavaLocalDateTime();
            java.time.LocalDateTime now = XDateTime.now().toJavaLocalDateTime();
            if (lockedUntil.isAfter(now)) {
                throw new XError("login_temporarily_blocked");
            }
        }

        String dbPasswdHash = (String) fullAttrs.get("passwd");
        String passwdAlgo = (String) fullAttrs.get("passwd_algo");
        boolean passwordValid = false;

        if ("md5".equals(passwdAlgo) || jandcode.commons.UtString.empty(passwdAlgo)) {
            String providedHash = jandcode.commons.UtString.md5Str(providedPasswd);
            passwordValid = providedHash.equals(dbPasswdHash);
        } else if ("argon2id".equals(passwdAlgo)) {
            try {
                passwordValid = argon2.verify(dbPasswdHash, providedPasswd.toCharArray());
            } catch (Exception e) {
                passwordValid = false;
            }
        }

        if (passwordValid) {
            z.updateAuthStats(login, true);

            // Чистый стандартный JWT без лишних полей (чтобы не было 500 ошибки при refresh)
            Map<String, Object> jwtAttrs = new HashMap<>();
            jwtAttrs.put("id", fullAttrs.get("id"));
            jwtAttrs.put("login", fullAttrs.get("login"));
            jwtAttrs.put("name", fullAttrs.get("name"));
            jwtAttrs.put("accesslevel", fullAttrs.get("accesslevel"));
            jwtAttrs.put("target", fullAttrs.get("target"));
            jwtAttrs.put("metamodel", fullAttrs.get("metamodel"));

            CfgService cfgSvc = getApp().bean(CfgService.class);
            String secret = cfgSvc.getConf().getString("auth/main/jwt");

            String accessToken;
            String refreshTokenPlain = null;

            if ("admin-quasar".equals(clientType) || "vue-app".equals(clientType)) {
                accessToken = JwtUtils.createAccessToken(jwtAttrs, secret);
                refreshTokenPlain = JwtUtils.createRefreshToken(fullAttrs.get("id"), secret);

                String tokenHash = JwtUtils.hashToken(refreshTokenPlain);
                XDateTime expiresAt = XDateTime.now().addDays(7);
                z.saveRefreshToken(fullAttrs.get("id"), tokenHash, expiresAt);
            } else {
                accessToken = JwtUtils.createLegacyToken(jwtAttrs, secret);
            }

            // Читаем флаг принудительной смены пароля из БД (1 или 0)
            boolean forceChange = jandcode.commons.UtCnv.toBoolean(fullAttrs.get("force_change"));

            // Устанавливаем пользователя в контекст JAndCode сессии сервера
            AuthUser fullUser = new DefaultAuthUser(fullAttrs);
            getApp().bean(AuthService.class).setCurrentUser(fullUser);

            // Формируем атрибуты ответа, которые заберет TofiAuthAction
            Map<String, Object> responseAttr = new HashMap<>();
            responseAttr.put("token", accessToken);
            if (refreshTokenPlain != null) {
                responseAttr.put("refresh_token_plain", refreshTokenPlain);
            }

            // Записываем флаг принудительной смены пароля в attrs возвращаемого объекта
            responseAttr.put("forcePasswordChange", forceChange);


            // 1. Создаем мапу на базе ВСЕХ атрибутов из БД (id, login, target)
            Map<String, Object> finalServerAttrs = new HashMap<>(fullAttrs);

            // 2. Домешиваем туда токен и новые параметры для фронтенда
            finalServerAttrs.put("token", accessToken);
            if (refreshTokenPlain != null) {
                finalServerAttrs.put("refresh_token_plain", refreshTokenPlain);
            }
            finalServerAttrs.put("forcePasswordChange", forceChange);

            // 3. Кладем в сессию сервера ПОЛНОЦЕННОГО пользователя
            fullUser = new DefaultAuthUser(finalServerAttrs);
            getApp().bean(AuthService.class).setCurrentUser(fullUser);

            return fullUser;

        } else {
            z.updateAuthStats(login, false);
            throw new XError("invalid_user_passwd");
        }
    }
}