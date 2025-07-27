package tofi.api.adm;

import jandcode.core.store.Store;

import java.util.*;

public interface ApiAdm {

   Map<String, Object> getUserInfo(String login, String passwd, String app);

    /**
     *
     * @param id AuthUser
     * @return StoreRecord AuthUser
     */
    Store loadAuthUser(long id);

    public void regUser(Map<String, Object> rec);

}
