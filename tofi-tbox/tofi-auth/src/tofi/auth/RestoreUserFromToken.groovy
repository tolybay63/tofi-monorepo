package tofi.auth

import jandcode.commons.error.XError
import jandcode.core.dbm.mdb.Mdb
import jandcode.core.std.CfgService

class RestoreUserFromToken {
    static Map<String, Object> attrsUserFromToken(Mdb mdb, String token) {
        CfgService cfgSvc = mdb.getApp().bean(CfgService.class)
        String secret = cfgSvc.getConf().getString("auth/main/jwt", "default-key")
        if (secret == "default-key")
            throw new XError("Неверный ключ в cfg")
        return JwtUtils.decode(token, secret)
    }


}
