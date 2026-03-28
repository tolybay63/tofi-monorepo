package tofi.mdl.model.dao.database;

import jandcode.commons.UtCnv;
import jandcode.commons.error.XError;
import jandcode.core.auth.AuthService;
import jandcode.core.auth.AuthUser;
import jandcode.core.dao.DaoMethod;
import jandcode.core.dbm.mdb.BaseMdbUtils;
import jandcode.core.dbm.mdb.Mdb;
import jandcode.core.std.CfgService;
import jandcode.core.store.Store;
import jandcode.core.store.StoreRecord;
import tofi.mdl.consts.FD_DataBaseType_consts;
import tofi.mdl.model.utils.EntityMdbUtils;

import java.util.Arrays;
import java.util.Map;

public class DataBaseMdbUtils extends BaseMdbUtils {

    //todo Delete!
    @DaoMethod
    public String getIdMetaModel() {
        CfgService cfgSvc = getMdb().getApp().bean(CfgService.class);
        return cfgSvc.getConf().getString("dbsource/default/id");
    }

    @DaoMethod
    public Store load() throws Exception {
        checkTarget("meta:db");
        Store st = getMdb().createStore("DataBase");
        getMdb().loadQuery(st, """
                    select * from DataBase where 0=0
                """);
        return st;
    }

    @DaoMethod
    public StoreRecord newRec() {
        Store st = getMdb().createStore("DataBase");
        return st.add();
    }

    @DaoMethod
    public Store insert(Map<String, Object> rec) throws Exception {
        checkTarget("meta:db:ins");
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), "DataBase");
        long id = eu.insertEntity(rec);

        Store st = getMdb().createStore("DataBase");
        getMdb().loadQuery(st, "select * from DataBase where id=:id", Map.of("id", id));
        return st;
    }

    @DaoMethod
    public Store update(Map<String, Object> rec) throws Exception {
        checkTarget("meta:db:upd");
        long id = UtCnv.toLong(rec.get("id"));
        if (id == 0) {
            throw new XError("Поле id должно иметь не нулевое значение");
        }
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), "DataBase");
        eu.updateEntity(rec);

        Store st = getMdb().createStore("DataBase");
        getMdb().loadQuery(st, "select * from DataBase where id=:id", Map.of("id", id));
        return st;
    }

    @DaoMethod
    public void delete(Map<String, Object> rec) throws Exception {
        checkTarget("meta:db:del");
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), "DataBase");
        eu.deleteEntity(rec);
    }

    @DaoMethod
    public Store loadDbForSelect() throws Exception {
        return getMdb().loadQuery("""
                    select id, name from DataBase where dataBaseType=:db
                """, Map.of("db", FD_DataBaseType_consts.data));
    }

    @DaoMethod
    public void checkTarget(String target) {
        AuthService authService = getModel().getApp().bean(AuthService.class);
        AuthUser usr = authService.getCurrentUser();

        //if (getMdb().getApp().getEnv().isDev()) {
        System.out.println("--- DEBUG ---");
        System.out.println("Target: " + target);
        System.out.println("User ID from Attrs: " + usr.getAttrs().getLong("id"));
        System.out.println("User Login: " + usr.getAttrs().getString("login"));
        System.out.println("-------------");
        //}


        if (usr.getAttrs().getLong("id") == 1) return;

        if (usr.getAttrs().getLong("id") == 0)
            throw new XError("notLoginned");

        String userTargets = usr.getAttrs().getString("target", "");
        String [] targets = userTargets.trim().split("\\s*,\\s*");
        if (!Arrays.asList(targets).contains(target)) {
            if (Arrays.asList("dtj", "adm", "meta", "nsi").contains(target)) {
                throw new XError("notAccessService");
            }
            throw new XError("notAccess");
        }
    }

}
