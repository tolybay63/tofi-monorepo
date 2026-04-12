package tofi.mdl.model.dao.syscoding;

import jandcode.commons.UtCnv;
import jandcode.commons.error.XError;
import jandcode.core.dao.DaoMethod;
import jandcode.core.dbm.mdb.BaseMdbUtils;
import jandcode.core.store.Store;
import jandcode.core.store.StoreRecord;
import tofi.mdl.consts.FD_AccessLevel_consts;
import tofi.mdl.consts.FD_SysCodingType_consts;
import tofi.mdl.model.utils.EntityMdbUtils;

import java.util.Map;

public class SysCodingMdbUtils extends BaseMdbUtils {

    @DaoMethod
    public Store load() throws Exception {
        Store st = getMdb().createStore("SysCoding");
        getMdb().loadQuery(st, """
                    select * from SysCoding where 0=0
                """);
        return st;
    }

    @DaoMethod
    public StoreRecord newRec() {
        Store st = getMdb().createStore("SysCoding");
        StoreRecord r = st.add();
        r.set("accessLevel", FD_AccessLevel_consts.common);
        r.set("sysCodingType", FD_SysCodingType_consts.reg);
        return r;
    }

    @DaoMethod
    public Store insert(Map<String, Object> rec) throws Exception {
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), "SysCoding");
        long id = eu.insertEntity(rec);

        Store st = getMdb().createStore("SysCoding");
        getMdb().loadQuery(st, "select * from SysCoding where id=:id", Map.of("id", id));
        return st;
    }

    @DaoMethod
    public Store update(Map<String, Object> rec) throws Exception {
        long id = UtCnv.toLong(rec.get("id"));
        if (id == 0) {
            throw new XError("Поле id должно иметь не нулевое значение");
        }
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), "SysCoding");
        eu.updateEntity(rec);

        Store st = getMdb().createStore("SysCoding");
        getMdb().loadQuery(st, "select * from SysCoding where id=:id", Map.of("id", id));
        return st;
    }

    @DaoMethod
    public void delete(Map<String, Object> rec) throws Exception {
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), "SysCoding");
        eu.deleteEntity(rec);
    }


}
