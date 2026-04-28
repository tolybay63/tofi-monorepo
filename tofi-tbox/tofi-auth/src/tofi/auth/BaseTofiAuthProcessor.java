package tofi.auth;

import jandcode.commons.error.XError;
import jandcode.core.BaseComp;
import jandcode.core.auth.*;
import jandcode.core.std.CfgService;
import tofi.api.adm.ApiAdm;
import tofi.apinator.ApinatorService;

import java.util.HashMap;
import java.util.Map;


public class BaseTofiAuthProcessor extends BaseComp implements AuthProcessor {

    @Override
    public boolean isSupportedAuthToken(AuthToken authToken) {
        return authToken instanceof UserPasswdAuthToken;
    }

    @Override
    public AuthUser login(AuthToken authToken) throws Exception {
        UserPasswdAuthToken token = (UserPasswdAuthToken) authToken;

        // 1. Получаем ПОЛНЫЕ данные (включая targets) из базы админки
        var admApi = getApp().bean(ApinatorService.class).getApi("adm");
        var z = admApi.get(ApiAdm.class);
        Map<String, Object> fullAttrs = z.getUserInfo(token.getUsername(), token.getPasswd());

        if (fullAttrs == null || fullAttrs.isEmpty()) {
            throw new XError("invalid_user_passwd");
        }

        // 2. Генерируем JWT, упаковывая туда ВСЕ атрибуты
        CfgService cfgSvc = getApp().bean(CfgService.class);
        String secret = cfgSvc.getConf().getString("auth/main/jwt", "default-key");
        String jwtToken = JwtUtils.createToken(fullAttrs, secret);

        // 3. Устанавливаем ПОЛНОГО пользователя в ThreadLocal сервера
        // Это нужно, чтобы CheckTargets сработал прямо сейчас, если нужно.
        AuthUser fullUser = new DefaultAuthUser(fullAttrs);
        getApp().bean(AuthService.class).setCurrentUser(fullUser);

        // 4. ВОЗВРАЩАЕМ КЛИЕНТУ ТОЛЬКО ТОКЕН
        // Создаем "легкий" объект пользователя только для ответа
        Map<String, Object> responseAttr = new HashMap<>();
        responseAttr.put("token", jwtToken);
        return new DefaultAuthUser(responseAttr);
    }
}

