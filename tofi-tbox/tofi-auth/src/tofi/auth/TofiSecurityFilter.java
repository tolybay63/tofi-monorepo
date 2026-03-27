package tofi.auth;

import jandcode.commons.UtCnv;
import jandcode.commons.error.XError;
import jandcode.core.auth.AuthService;
import jandcode.core.auth.AuthUser;
import jandcode.core.auth.DefaultAuthUser;
import jandcode.core.std.CfgService;
import jandcode.core.web.Request;
import jandcode.core.web.filter.BaseFilter;
import jandcode.core.web.filter.FilterType;

import java.util.Map;

public class TofiSecurityFilter extends BaseFilter {

    @Override
    public void execFilter(FilterType type, Request request) {
        if (type != FilterType.beforeAction) {
            return;
        }

        String path = request.getPathInfo();
        if (path == null) path = "";
        String normPath = path.startsWith("/") ? path.substring(1) : path;

        // 1. Публичные пути
        if (normPath.isEmpty() || normPath.startsWith("auth/login") ||
                normPath.startsWith("auth/logout") || normPath.startsWith("data")) {
            return;
        }

        AuthService authService = getApp().bean(AuthService.class);
        AuthUser user = authService.getCurrentUser();

        // 2. Если движок (вес -110) уже опознал юзера — выходим успешно
        if (user != null && user.getAttrs().getLong("id") != 0) {
            return;
        }

        // 3. Иначе пробуем восстановить из JWT вручную
        user = restoreUserFromToken(request);
        if (user != null && user.getAttrs().getLong("id") != 0) {
            authService.setCurrentUser(user);
            return;
        }

        // 4. Если всё еще аноним — блокируем доступ к API
        if (normPath.startsWith("api") || normPath.startsWith("meta") ||
                normPath.startsWith("nsi") || normPath.startsWith("admin")) {
            throw new XError("401: Unauthorized", 401);
        }
    }

    private AuthUser restoreUserFromToken(Request request) {
        String authHeader = request.getHttpRequest().getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;

        String token = authHeader.substring(7);
        CfgService cfgSvc = getApp().bean(CfgService.class);
        String secret = cfgSvc.getConf().getString("auth/main/jwt", "default-secret-key");

        try {
            Map<String, Object> payload = JwtUtils.decode(token, secret);
            if (payload == null) return null;

            // ПРАВКА ТУТ:
            // Если в токене есть вложенный attrs - берем его,
            // если нет (как на вашем скрине) - берем весь payload целиком.
            Object attrsObj = payload.get("attrs");
            Map<String, Object> userAttrs = (attrsObj != null) ? UtCnv.toMap(attrsObj) : payload;

            return new DefaultAuthUser(userAttrs);
        } catch (Exception e) {
            return null;
        }
    }
}