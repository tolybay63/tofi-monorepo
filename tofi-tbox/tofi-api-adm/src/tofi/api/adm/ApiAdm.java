package tofi.api.adm;

import jandcode.core.store.Store;

import java.util.Map;

public interface ApiAdm {

    Map<String, Object> getUserInfo(String login, String passwd);

    /**
     *
     * @param id AuthUser
     * @return StoreRecord AuthUser
     */
    Store loadAuthUser(long id);

    long regUser(Map<String, Object> rec);

    void deleteAuthUser(long id);

    Store loadSql(String sql, String domain);


}
