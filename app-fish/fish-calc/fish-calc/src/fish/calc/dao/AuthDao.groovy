package fish.calc.dao

import jandcode.commons.error.XError
import jandcode.core.auth.AuthService
import jandcode.core.auth.AuthUser
import jandcode.core.dao.DaoMethod
import jandcode.core.dbm.mdb.BaseMdbUtils
import tofi.api.adm.ApiAdm
import tofi.apinator.ApinatorApi
import tofi.apinator.ApinatorService

class AuthDao extends BaseMdbUtils {

    ApinatorApi apiAdm() { return getApp().bean(ApinatorService.class).getApi("adm"); }

    @DaoMethod
    Map<String, Object> getUserInfo() {
        AuthService authSvc = getMdb().getApp().bean(AuthService.class)
        AuthUser au = authSvc.getCurrentUser()
        if (au==null) {
            throw new XError("notLoginned")
        }
        return au.getAttrs()
    }

    @DaoMethod
    void forgetPasswd(String login) {
        apiAdm().get(ApiAdm.class).forgetPasswd(login);
    }

    @DaoMethod
    void checkTarget(String target) {
        AuthService authService = getModel().getApp().bean(AuthService.class)
        AuthUser usr = authService.getCurrentUser()

        if (getMdb().getApp().getEnv().isDev()) {
            System.out.println("--- DEBUG ---")
            System.out.println("Target: " + target)
            System.out.println("User ID from Attrs: " + usr.getAttrs().getLong("id"))
            System.out.println("User Login: " + usr.getAttrs().getString("login"))
            System.out.println("-------------")
        }

        if (usr.getAttrs().getLong("id") == 1) return

        if (usr.getAttrs().getLong("id") == 0)
            throw new XError("notLoginned")

        String userTargets = usr.getAttrs().getString("target", "")
        String[] targets = userTargets.trim().split("\\s*,\\s*")
        if (!Arrays.asList(targets).contains(target)) {
            if (target == "calc") {
                throw new XError("notAccessService")
            }
            throw new XError("notAccess")
        }
    }
}
