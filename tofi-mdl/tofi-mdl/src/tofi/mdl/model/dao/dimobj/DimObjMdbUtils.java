package tofi.mdl.model.dao.dimobj;

import jandcode.commons.UtCnv;
import jandcode.commons.error.XError;
import jandcode.core.dao.DaoMethod;
import jandcode.core.dbm.mdb.BaseMdbUtils;
import jandcode.core.dbm.mdb.Mdb;
import jandcode.core.store.Store;
import jandcode.core.store.StoreRecord;
import tofi.mdl.consts.FD_AccessLevel_consts;
import tofi.mdl.model.utils.EntityMdbUtils;

import java.util.Map;

public class DimObjMdbUtils extends BaseMdbUtils {

    /**
     * @param dimObjGr id
     * @return Store
     */
    @DaoMethod
    public Store loadDimObj(long dimObjGr) throws Exception {
        Store st = getMdb().createStore("DimObj");
        return getMdb().loadQuery(st, """
                    select * from DimObj where dimObjGr=:g
                    order by ord
                """, Map.of("g", dimObjGr));
    }

    /**
     * @param dimObjGroup id
     * @return Store
     */
    @DaoMethod
    public StoreRecord newRec(long dimObjGroup) throws Exception {
        Store st = getMdb().createStore("DimObj");
        StoreRecord r = st.add();
        r.set("dimObjGr", dimObjGroup);
        r.set("accessLevel", FD_AccessLevel_consts.common);
        return r;
    }

    /**
     * @param id
     * @return
     * @throws Exception
     */
    public StoreRecord loadRec(long id) throws Exception {
        Store st = getMdb().createStore("DimObj");
        StoreRecord rec = st.add();
        return getMdb().loadQueryRecord(rec, """
                    select * from DimObj where id=:id
                """, Map.of("id", id));
    }

    @DaoMethod
    public Store insert(Map<String, Object> params) throws Exception {
        Map<String, Object> rec = UtCnv.toMap(params.get("rec"));
        //
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), "DimObj");
        long id = eu.insertEntity(rec);
        //
        Store st = getMdb().createStore("DimObj");
        getMdb().loadQuery(st, "select * from DimObj where id=:id", Map.of("id", id));
        getMdb().resolveDicts(st);
        return st;
    }

    @DaoMethod
    public Store update(Map<String, Object> params) throws Exception {
        Map<String, Object> rec = (UtCnv.toMap(params.get("rec")));
        long id = UtCnv.toLong(rec.get("id"));
        if (id == 0) {
            throw new XError("Поле id должно иметь не нулевое значение");
        }
        //
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), "DimObj");
        eu.updateEntity(rec);
        //
        // Загрузка записи
        Store st = getMdb().createStore("DimObj");
        getMdb().loadQuery(st, "select * from DimObj where id=:id", Map.of("id", id));
        getMdb().resolveDicts(st);
        return st;
    }

    @DaoMethod
    public void delete(Map<String, Object> rec) throws Exception {
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), "DimObj");
        eu.deleteEntity(rec);
    }


}
