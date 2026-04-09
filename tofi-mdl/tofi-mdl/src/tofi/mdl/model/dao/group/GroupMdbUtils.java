package tofi.mdl.model.dao.group;

import jandcode.commons.UtCnv;
import jandcode.commons.error.XError;
import jandcode.core.dao.DaoMethod;
import jandcode.core.dbm.mdb.BaseMdbUtils;
import jandcode.core.store.Store;
import jandcode.core.store.StoreRecord;
import tofi.mdl.model.utils.EntityMdbUtils;

import java.util.Map;

public class GroupMdbUtils extends BaseMdbUtils {

    @DaoMethod
    public Store loadGroup(Map<String, Object> params) throws Exception {
        String tableName = UtCnv.toString(params.get("tableName"));
        Store st = getMdb().createStore(tableName);
        return getMdb().loadQuery(st, "select * from " + tableName);
    }

    @DaoMethod
    public Store loadGroupForSelect(Map<String, Object> params) throws Exception {
        long id = UtCnv.toLong(params.get("id"));
        String tableName = UtCnv.toString(params.get("tableName"));
        Store st = getMdb().createStore(tableName);
        getMdb().loadQuery(st, "select * from " + tableName + " where id <> :id", Map.of("id", id));

        return st;
    }

    @DaoMethod
    public Store loadRec(Map<String, Object> params) throws Exception {
        String tableName = UtCnv.toString(params.get("tableName"));
        long id = UtCnv.toLong(params.get("id"));
        Store st = getMdb().createStore(tableName);
        getMdb().loadQuery(st, "select * from " + tableName + " where id=" + id);
        return st;
    }

    @DaoMethod
    public StoreRecord newRec(String tableName) throws Exception {
        Store st = getMdb().createStore(tableName);
        StoreRecord r = st.add();
        r.set("accessLevel", 1L);
        return r;
    }

    @DaoMethod
    public Store insert(Map<String, Object> params) throws Exception {
        Map<String, Object> rec = UtCnv.toMap(params.get("rec"));
        String tableName = UtCnv.toString(params.get("tableName"));
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), tableName);
        long id = eu.insertEntity(rec);
        Store st = getMdb().createStore(tableName);
        getMdb().loadQuery(st, "select * from " + tableName + " where id=:id", Map.of("id", id));
        getMdb().resolveDicts(st);
        return st;
    }

    @DaoMethod
    public Store update(Map<String, Object> params) throws Exception {
        Map<String, Object> rec = (UtCnv.toMap(params.get("rec")));
        String tableName = UtCnv.toString(rec.get("tableName"));
        long id = UtCnv.toLong(rec.get("id"));
        if (id == 0) {
            throw new XError("Поле id должно иметь не нулевое значение");
        }
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), tableName);
        eu.updateEntity(rec);
        // Загрузка записи
        Store st = getMdb().createStore(tableName);
        getMdb().loadQuery(st, "select * from " + tableName + " where id=:id", Map.of("id", id));
        return st;
    }

    @DaoMethod
    public void delete(Map<String, Object> rec) throws Exception {
        String tableName = UtCnv.toString(rec.get("tableName"));
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), tableName);
        eu.deleteEntity(rec);

    }


}
