package tofi.adm.model.dao.permis;

import jandcode.commons.UtCnv;
import jandcode.core.dbm.dao.BaseModelDao;
import jandcode.core.store.Store;

import java.util.Map;
import java.util.Set;

public class PermisDao extends BaseModelDao {

    public Store load(Map<String, Object> params) throws Exception {
        PermisMdbUtils ut = new PermisMdbUtils(getMdb());
        return ut.load(params);
    }

    public void delete(Map<String, Object> params) throws Exception {
        PermisMdbUtils ut = new PermisMdbUtils(getMdb());
        ut.delete(UtCnv.toMap(params.get("rec")));
    }

    public Store update(Map<String, Object> params) throws Exception {
        PermisMdbUtils ut = new PermisMdbUtils(getMdb());
        return ut.update(params);
    }

    public Store insert(Map<String, Object> params) throws Exception {
        PermisMdbUtils ut = new PermisMdbUtils(getMdb());
        return ut.insert(params);
    }

    public Set<String> getLeaf(String id) throws Exception {
        PermisMdbUtils ut = new PermisMdbUtils(getMdb());
        return ut.getLeaf(id);
    }

}
