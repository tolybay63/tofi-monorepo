package tofi.api.adm;

import jandcode.core.store.Store;

import java.util.Map;

public interface ApiAdm {

    Map<String, Object> getUserInfo(String login);

    void saveRefreshToken(Object authUserId, String tokenHash, jandcode.commons.datetime.XDateTime expiresAt);

    // Находит user_id по хэшу токена, если токен еще не протух
    Long findUserIdByRefreshToken(String tokenHash);

    // Удаляет рефреш-токен из базы (нужно для логаута и ротации)
    void deleteRefreshToken(String tokenHash);

    void updatePasswordAndAlgo(String login, String passwd, String passwd_algo);

    void updateAuthStats(String login, boolean success);

    void forceChangePsw(String login, String passwd);

    Store loadAuthUser(long id);

    long regUser(Map<String, Object> rec);

    void changePasswd(long user, String newPasswd);

    void forgetPasswd(String login);

    void send(String subject, String infoWithLink, String email);

    void deleteAuthUser(long id);

    Store loadSql(String sql, String domain);

    Store loadSqlWithParams(String sql, Map<String, Object> params);

    void execSqlWithParams(String sql, Map<String, Object> params);

    void updateEmailAndPhone(Map<String, Object> rec);

}
