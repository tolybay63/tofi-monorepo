package tofi.auth;

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
        // Выполняем проверку только ПЕРЕД выполнением действия (action)
        if (type != FilterType.beforeAction) {
            return;
        }

        String path = request.getPathInfo();
        if (path == null) path = "";
        String normPath = path.startsWith("/") ? path.substring(1) : path;

        // 1. Пропускаем публичные пути (логин, статика и т.д.)
        if (normPath.isEmpty() || normPath.startsWith("auth/login") ||
                normPath.startsWith("auth/logout") || normPath.startsWith("data")) {
            return;
        }

        AuthService authService = getApp().bean(AuthService.class);

        // 2. Пытаемся восстановить пользователя из токена для текущего запроса
        AuthUser user = restoreUserFromToken(request);

        if (user != null) {
            // Если токен валидный, сохраняем его в ThreadLocal текущего потока
            // Теперь CheckTargets(target) найдет все права внутри этого объекта
            authService.setCurrentUser(user);
            return;
        }

        // 3. Если пользователь не опознан и путь защищенный — блокируем доступ
        if (normPath.startsWith("api") || normPath.startsWith("meta") ||
                normPath.startsWith("nsi") || normPath.startsWith("admin") ||
                normPath.startsWith("report")) {
            // Возвращаем 401 ошибку, чтобы фронтенд (Quasar) мог сделать редирект на логин
            //throw new XError("401: Unauthorized", 401);
            throw new XError("lifetime_expired");
        }
    }

    /**
     * Извлекает JWT из заголовка Authorization и превращает его в AuthUser
     */
    private AuthUser restoreUserFromToken(Request request) {
        String authHeader = request.getHttpRequest().getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;

        String token = authHeader.substring(7);
        CfgService cfgSvc = getApp().bean(CfgService.class);

        String secret = cfgSvc.getConf().getString("auth/main/jwt", "default-key-change-me");

        try {
            Map<String, Object> userAttrs = JwtUtils.decode(token, secret);

            if (userAttrs == null || userAttrs.isEmpty()) {
                return null;
            }

            return new DefaultAuthUser(userAttrs);
        } catch (Exception e) {
            return null;
        }
    }
}