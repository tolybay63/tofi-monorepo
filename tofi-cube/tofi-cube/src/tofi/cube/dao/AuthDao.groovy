package tofi.cube.dao

import jandcode.commons.error.XError
import jandcode.core.auth.AuthService
import jandcode.core.auth.AuthUser
import jandcode.core.dao.DaoMethod
import jandcode.core.dbm.mdb.BaseMdbUtils

class AuthDao extends BaseMdbUtils {

    @DaoMethod
    Map<String, Object> getUserInfo() {
        AuthService authSvc = getMdb().getApp().bean(AuthService.class)
        AuthUser au = authSvc.getCurrentUser()
        if (au == null) {
            throw new XError("notLoginned")
        }
        return au.getAttrs()
    }

}
