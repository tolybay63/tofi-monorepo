package tofi.auth;

import jandcode.commons.error.XError;
import jandcode.core.BaseComp;
import jandcode.core.auth.*;
import jandcode.core.std.CfgService;
import tofi.api.adm.ApiAdm;
import tofi.apinator.ApinatorService;

import java.util.Map;


public abstract class BaseTofiAuthProcessor extends BaseComp implements AuthProcessor {

    /**
     * Каждый сервис (nsi, plan, mdl) вернет свою строку: "nsi", "dtj", "mdl"
     */
    //protected abstract String getServiceTargetGroup();

    @Override
    public boolean isSupportedAuthToken(AuthToken authToken) {
        return authToken instanceof UserPasswdAuthToken;
    }

    @Override
    public AuthUser login(AuthToken authToken) throws Exception {
        UserPasswdAuthToken token = (UserPasswdAuthToken) authToken;
        // 1. Идем в базу админки через Apinator
        var admApi = getApp().bean(ApinatorService.class).getApi("adm");
        // Предполагаем, что интерфейс ApiAdm доступен
        var z = admApi.get(ApiAdm.class);
        // 2. Получаем данные пользователя и таргеты
        Map<String, Object> attrs = z.getUserInfo(token.getUsername(), token.getPasswd());

        if (attrs == null || attrs.isEmpty()) {
            throw new XError("invalid_user_passwd");
        }

        // 3. Генерируем JWT токен. Секрет берем из общего конфига
        CfgService cfgSvc = getApp().bean(CfgService.class);
        String secret = cfgSvc.getConf().getString("auth/main/jwt", "default-key-change-me");

        String jwtToken = JwtUtils.createToken(attrs, secret);

        // Добавляем токен в атрибуты, чтобы он ушел на фронтенд
        attrs.put("token", jwtToken);

        AuthUser usr = new DefaultAuthUser(attrs);
        getApp().bean(AuthService.class).setCurrentUser(usr);

        return usr;
    }
}

