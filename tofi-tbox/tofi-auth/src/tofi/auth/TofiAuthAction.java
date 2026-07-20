package tofi.auth;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import jandcode.commons.UtCnv;
import jandcode.commons.UtString;
import jandcode.commons.datetime.XDateTime;
import jandcode.commons.error.XError;
import jandcode.core.apx.auth.AuthAction;
import jandcode.core.apx.auth.AuthConsts;
import jandcode.core.auth.AuthService;
import jandcode.core.auth.AuthUser;
import jandcode.core.auth.std.DefaultUserPasswdAuthToken;
import jandcode.core.std.CfgService;
import jandcode.core.store.Store;
import tofi.api.adm.ApiAdm;
import tofi.apinator.ApinatorApi;
import tofi.apinator.ApinatorService;

import javax.servlet.http.Cookie;
import java.util.HashMap;
import java.util.Map;

public class TofiAuthAction extends AuthAction {

    // Инициализируем фабрику Argon2id для сохранения новых паролей
    private final Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);

    ApinatorApi apiAdm() { return getApp().bean(ApinatorService.class).getApi("adm"); }

    @Override
    public void login() throws Exception {
        AuthService authSvc = getApp().bean(AuthService.class);
        var req = getReq();

        String username = req.getParams().getString("username");
        String password = req.getParams().getString("password");

        String clientType = "legacy";

        if (username != null && username.contains(":::")) {
            String[] parts = username.split(":::");
            if (parts.length >= 2) {
                clientType = parts[1];
            }
        }

        DefaultUserPasswdAuthToken authToken = new DefaultUserPasswdAuthToken(username, password);

        AuthUser u = authSvc.login(authToken);

        if ("admin-quasar".equals(clientType) || "vue-app".equals(clientType)) {
            String refreshTokenPlain = (String) u.getAttrs().get("refresh_token_plain");
            if (refreshTokenPlain != null) {
                setRefreshCookie(refreshTokenPlain, 7 * 24 * 60 * 60);
            }
        }

        req.getSession().put(AuthConsts.SESSION_KEY_USER, u);

        Map<String, Object> clientResponse = new HashMap<>();
        clientResponse.put("token", u.getAttrs().get("token"));

        boolean forceChange = UtCnv.toBoolean(u.getAttrs().get("forcePasswordChange"));
        clientResponse.put("forcePasswordChange", forceChange);

        Map<String, Object> resultWrapper = new HashMap<>();
        resultWrapper.put("result", clientResponse);

        req.render(resultWrapper);
    }

    /**
     * НОВЫЙ МЕТОД: Принудительное хэширование и сохранение нового пароля с фронтенда
     */
    public void forcechangepsw() throws Exception {
        var req = getReq();

        String login = req.getParams().getString("login");
        String rawPassword = req.getParams().getString("passwd");

        if (UtString.empty(login) || UtString.empty(rawPassword)) {
            throw new XError("invalid_params");
        }

        String newHash;
        try {
            newHash = argon2.hash(3, 65536, 4, rawPassword.toCharArray());
        } catch (Exception e) {
            throw new XError("Хэширование пароля прервано безопасностью системы");
        }

        var z = apiAdm().get(ApiAdm.class);
        z.execSqlWithParams("""
            update authuser
            set passwd = :passwd,
                passwd_algo = 'argon2id',
                force_change = 0
            where login = :login
        """, Map.of("passwd", newHash, "login", login));

        req.render(new HashMap<>());
    }

    public void refresh() throws Exception {
        var req = getReq();

        Cookie[] cookies = req.getHttpRequest().getCookies();
        String refreshTokenPlain = null;
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("fish_refresh".equals(cookie.getName())) {
                    refreshTokenPlain = cookie.getValue();
                    break;
                }
            }
        }

        if (UtString.empty(refreshTokenPlain)) {
            throw new XError("notLoginned");
        }

        CfgService cfgSvc = getApp().bean(CfgService.class);
        String secret = cfgSvc.getConf().getString("auth/main/jwt");

        String userIdFromToken = JwtUtils.getUserIdFromRefreshToken(refreshTokenPlain, secret);
        if (userIdFromToken == null) {
            throw new XError("lifetime_expired");
        }

        String tokenHash = JwtUtils.hashToken(refreshTokenPlain);
        var z = apiAdm().get(ApiAdm.class);
        Long authUserId = z.findUserIdByRefreshToken(tokenHash);

        if (authUserId == null) {
            throw new XError("lifetime_expired");
        }

        z.deleteRefreshToken(tokenHash);

        Store userRows = apiAdm().get(ApiAdm.class).loadSqlWithParams("""
                 select login from authuser where id = :id
        """, Map.of("id", authUserId));

        if (userRows.size()==0) {
            throw new XError("invalid_user_passwd");
        }
        String userLogin = userRows.get(0).getString("login");

        Map<String, Object> fullAttrs = z.getUserInfo(userLogin);

        if (fullAttrs == null || fullAttrs.isEmpty()) {
            throw new XError("invalid_user_passwd");
        }

        Map<String, Object> jwtAttrs = new HashMap<>();
        jwtAttrs.put("id", fullAttrs.get("id"));
        jwtAttrs.put("login", fullAttrs.get("login"));
        jwtAttrs.put("name", fullAttrs.get("name"));
        jwtAttrs.put("accesslevel", fullAttrs.get("accesslevel"));
        jwtAttrs.put("target", fullAttrs.get("target"));
        jwtAttrs.put("metamodel", fullAttrs.get("metamodel"));

        String newAccessToken = JwtUtils.createAccessToken(jwtAttrs, secret);
        String newRefreshTokenPlain = JwtUtils.createRefreshToken(authUserId, secret);

        String newTokenHash = JwtUtils.hashToken(newRefreshTokenPlain);
        XDateTime expiresAt = XDateTime.now().addDays(7);
        z.saveRefreshToken(authUserId, newTokenHash, expiresAt);

        setRefreshCookie(newRefreshTokenPlain, 7 * 24 * 60 * 60);

        Map<String, Object> clientResponse = new HashMap<>();
        clientResponse.put("token", newAccessToken);

        Map<String, Object> resultWrapper = new HashMap<>();
        resultWrapper.put("result", clientResponse);

        req.render(resultWrapper);
    }

    @Override
    public void logout() throws Exception {
        var req = getReq();

        Cookie[] cookies = req.getHttpRequest().getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("fish_refresh".equals(cookie.getName())) {
                    String tokenHash = JwtUtils.hashToken(cookie.getValue());
                    var z = apiAdm().get(ApiAdm.class);
                    z.deleteRefreshToken(tokenHash);
                    break;
                }
            }
        }

        setRefreshCookie("", 0);

        req.getSession().remove(AuthConsts.SESSION_KEY_USER);
        req.render(new HashMap<>());
    }

    private void setRefreshCookie(String token, int maxAgeSec) {
        Cookie cookie = new Cookie("fish_refresh", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(getReq().getHttpRequest().isSecure());
        cookie.setPath("/fish/");
        cookie.setMaxAge(maxAgeSec);
        getReq().getHttpResponse().addCookie(cookie);
    }

}