package tofi.auth;

import jandcode.core.apx.auth.AuthAction;
import jandcode.core.apx.auth.AuthConsts;
import jandcode.core.auth.AuthService;
import jandcode.core.auth.AuthUser;
import jandcode.core.auth.std.DefaultUserPasswdAuthToken;

public class TofiAuthAction extends AuthAction {
    @Override
    public void login() throws Exception {
        AuthService authSvc = getApp().bean(AuthService.class);
        var req = getReq();

        String username = req.getParams().getString("username");
        String password = req.getParams().getString("password");

        // 1. Вызываем ваш BaseTofiAuthProcessor.login
        // Он уже подготовил токен и положил его в атрибуты
        AuthUser u = authSvc.login(new DefaultUserPasswdAuthToken(username, password));

        // 2. Для совместимости со старыми частями Jc2 оставляем сессию
        req.getSession().put(AuthConsts.SESSION_KEY_USER, u);

        // 3. ФИНАЛЬНЫЙ ШТРИХ: Вместо render("ok") отдаем JSON с токеном
        // Это отправит клиенту {"token": "eyJ..."}
        req.render(u.getAttrs());
    }


}
