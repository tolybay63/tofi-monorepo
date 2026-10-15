package tofi.auth;

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

        // Извлекаем метод только штатными средствами
        String rpcMethod = request.getParams().getString("method", "");
        if (rpcMethod == null) rpcMethod = "";

        // Определяем, относится ли запрос к экшену сброса пароля
        boolean isResetPasswdAction = normPath.contains("psw/") || rpcMethod.contains("psw/");

        // ПУБЛИЧНЫЕ ЭНДПОИНТЫ: Пропускаем без проверки токенов
        boolean isPublicRpc = rpcMethod.contains("forgetPasswd") || rpcMethod.contains("confirmPasswd");

        if (normPath.isEmpty() || isResetPasswdAction ||
                normPath.startsWith("auth/login") || normPath.startsWith("api/auth/login") ||
                normPath.startsWith("auth/forgetPasswd") || normPath.startsWith("api/auth/forgetPasswd") ||
                "auth/forceChangePsw".equals(rpcMethod) || isPublicRpc) {

            if (isResetPasswdAction || isPublicRpc) {
                jandcode.core.auth.AuthUser guestUser = new jandcode.core.auth.DefaultAuthUser(new java.util.HashMap<>());
                getApp().bean(AuthService.class).setCurrentUser(guestUser);
            }
            return;
        }

        // ЗАЩИЩЕННЫЕ ЭНДПОИНТЫ: Восстановление пользователя из токена
        AuthService authService = getApp().bean(AuthService.class);
        AuthUser user = restoreUserFromToken(request);

        if (user != null) {
            authService.setCurrentUser(user);
            return;
        }

        // АВТОМАТИЧЕСКАЯ ОЧИСТКА JSESSIONID ПРИ ПРОТУХАНИИ ТОКЕНА
        String authHeader = request.getHttpRequest().getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            System.out.println("=== JWT SECURITY: Токен просрочен! Очищаем контекст пользователя ===");

            // 1. Стираем пользователя из текущего потока JAndCode
            authService.setCurrentUser(null);

            // 2. МЯГКОЕ РЕШЕНИЕ: Вместо invalidate() просто удаляем пользователя из сессии
            try {
                var httpSession = request.getHttpRequest().getSession(false);
                if (httpSession != null) {
                    // JAndCode хранит пользователя в атрибутах сессии.
                    // Удаляем всё, что связано с авторизацией, чтобы JSESSIONID стал "пустым"
                    java.util.Enumeration<String> attrNames = httpSession.getAttributeNames();
                    while (attrNames.hasMoreElements()) {
                        String attrName = attrNames.nextElement();
                        if (attrName.toLowerCase().contains("auth") || attrName.toLowerCase().contains("user")) {
                            httpSession.removeAttribute(attrName);
                        }
                    }
                }
            } catch (Exception e) {
                // Игнорируем возможные проблемы с многопоточностью
            }

            // 3. Выходим. Рантайм JAndCode увидит анонима и вернет валидный JSON-ответ (200 OK),
            // фронтенд Quasar без проблем запустит рефреш.
            return;
        }
    }

    private AuthUser restoreUserFromToken(Request request) {
        String authHeader = request.getHttpRequest().getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;

        String token = authHeader.substring(7);
        CfgService cfgSvc = getApp().bean(CfgService.class);
        String secret = cfgSvc.getConf().getString("auth/main/jwt");

        try {
            Map<String, Object> tokenData = JwtUtils.decode(token, secret);
            if (tokenData == null || tokenData.isEmpty()) {
                return null;
            }

            // Достаем мапу, которую JwtUtils упаковал под именем "attrs"
            Map<String, Object> userAttrs = (Map<String, Object>) tokenData.get("attrs");

            if (userAttrs == null) {
                userAttrs = tokenData;
            }

            return new DefaultAuthUser(userAttrs);

        } catch (Exception e) {
            return null;
        }
    }
}