package tofi.mdl.model.dao.stock;

import jandcode.commons.UtCnv;
import jandcode.commons.error.XError;
import jandcode.core.dao.DaoMethod;
import jandcode.core.dbm.mdb.BaseMdbUtils;
import jandcode.core.store.Store;
import jandcode.core.store.StoreRecord;
import tofi.mdl.consts.FD_AccessLevel_consts;
import tofi.mdl.consts.FD_SourceStockType_consts;
import tofi.mdl.model.utils.EntityMdbUtils;

import java.util.Map;

public class StockMdbUtils extends BaseMdbUtils {

    @DaoMethod
    public Store loadStocks(long stockGr) throws Exception {
        Store st = getMdb().createStore("SourceStock");
        getMdb().loadQuery(st, """
            select * from SourceStock where parent=:p
        """, Map.of("p", stockGr));
        return st;
    }

    @DaoMethod
    public StoreRecord newRec(long stockGr) {
        Store st = getMdb().createStore("SourceStock");
        StoreRecord r = st.add();
        r.set("parent", stockGr);
        r.set("accessLevel", FD_AccessLevel_consts.common);
        r.set("sourceStockType", FD_SourceStockType_consts.file);
        return  r;
    }

    @DaoMethod
    public Store insert(Map<String, Object> rec) throws Exception {
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), "SourceStock");
        long id = eu.insertEntity(rec);

        Store st = getMdb().createStore("SourceStock");
        getMdb().loadQuery(st, "select * from SourceStock where id=:id", Map.of("id", id));
        return st;
    }

    @DaoMethod
    public Store update(Map<String, Object> rec) throws Exception {
        long id = UtCnv.toLong(rec.get("id"));
        if (id == 0) {
            throw new XError("Поле id должно иметь не нулевое значение");
        }
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), "SourceStock");
        eu.updateEntity(rec);

        Store st = getMdb().createStore("SourceStock");
        getMdb().loadQuery(st, "select * from SourceStock where id=:id", Map.of("id", id));
        return st;
    }

    @DaoMethod
    public void delete(Map<String, Object> rec) throws Exception {
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), "SourceStock");
        eu.deleteEntity(rec);
    }

    @DaoMethod
    public Store loadStockForSelect() throws Exception {
        return getMdb().loadQuery("""
            select id, name from SourceStock where 0=0
        """);
    }

}
