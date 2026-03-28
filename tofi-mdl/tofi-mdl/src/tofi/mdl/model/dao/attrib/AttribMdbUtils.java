package tofi.mdl.model.dao.attrib;

import jandcode.commons.UtCnv;
import jandcode.commons.error.XError;
import jandcode.core.auth.AuthService;
import jandcode.core.auth.AuthUser;
import jandcode.core.dao.DaoMethod;
import jandcode.core.dbm.mdb.BaseMdbUtils;
import jandcode.core.dbm.mdb.Mdb;
import jandcode.core.dbm.sql.SqlText;
import jandcode.core.store.Store;
import jandcode.core.store.StoreRecord;
import tofi.mdl.model.utils.EntityMdbUtils;

import java.util.HashMap;
import java.util.Map;

public class AttribMdbUtils extends BaseMdbUtils {

    /**
     * Загрузка Attrib с пагинацией
     *
     * @param params Map
     * @return Map
     */
    @DaoMethod
    public Map<String, Object> loadAttribPaginate(Map<String, Object> params) throws Exception {
        //AuthService authSvc = getMdb().getApp().bean(AuthService.class);
        //AuthUser au = authSvc.getCurrentUser();
        //todo AuthUser
        //long al = au.getAttrs().getLong("accesslevel");

        String sql = "select * from Attrib where 0=0 order by id";
        SqlText sqlText = getMdb().createSqlText(sql);
        Map<String, Object> par = new HashMap<>();
        int offset = (UtCnv.toInt(params.get("page")) - 1) * UtCnv.toInt(params.get("limit"));
        par.put("offset", offset);
        par.put("limit", UtCnv.toInt(params.get("limit")));
        sqlText.setSql(sql);
        sqlText.paginate(true);

        if (!UtCnv.toString(params.get("orderBy")).trim().isEmpty())
            sqlText = sqlText.replaceOrderBy(UtCnv.toString(params.get("orderBy")));

        String filter = UtCnv.toString(params.get("filter")).trim();
        if (!filter.isEmpty())
            sqlText = sqlText.addWhere("(cod like '%" + filter + "%' or name like '%" + filter + "%' or " +
                    "fullName like '%" + filter + "%')");
        Store st = getMdb().createStore("Attrib");

        getMdb().loadQuery(st, sqlText, par);
        getMdb().resolveDicts(st);

        //count
        sql = "select count(*) as cnt from Attrib where 0=0";
        sqlText.setSql(sql);
        if (!filter.isEmpty())
            sqlText = sqlText.addWhere("name like '%" + filter + "%' or fullName like '%" + filter + "%' or cod like '%" + filter + "%'");
        int total = getMdb().loadQuery(sqlText).get(0).getInt("cnt");
        Map<String, Object> meta = new HashMap<String, Object>();
        meta.put("total", total);
        meta.put("page", UtCnv.toInt(params.get("page")));
        meta.put("limit", UtCnv.toInt(params.get("limit")));

        return Map.of("store", st, "meta", meta);
    }


    /**
     * Delete Attrib
     *
     * @param
     * @throws Exception
     */
    @DaoMethod
    public void delete(Map<String, Object> params) throws Exception {
        Map<String, Object> rec = UtCnv.toMap(params.get("rec"));
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), "Attrib");
        eu.deleteEntity(rec);
    }

    /**
     * Update Factor & FactorVal
     *
     * @param params
     * @return
     * @throws Exception
     */
    @DaoMethod
    public Store update(Map<String, Object> params) throws Exception {
        Map<String, Object> rec = (UtCnv.toMap(params.get("rec")));

        long id = UtCnv.toLong(rec.get("id"));
        if (id == 0) {
            throw new XError("Поле id должно иметь не нулевое значение");
        }
        //
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), "Attrib");
        eu.updateEntity(rec);
        //
        // Загрузка записи
        Store st = getMdb().createStore("Attrib");

        getMdb().loadQuery(st, "select * from Attrib where id=:id", Map.of("id", id));
        return st;
    }

    /**
     * Insert Attrib
     *
     * @param params
     * @return
     * @throws Exception
     */
    @DaoMethod
    public Store insert(Map<String, Object> params) throws Exception {
        Map<String, Object> rec = UtCnv.toMap(params.get("rec"));
        //
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), "Attrib");
        long id = eu.insertEntity(rec);
        //
        Store st = getMdb().createStore("Attrib");
        getMdb().loadQuery(st, "select * from Attrib where id=:id", Map.of("id", id));

        return st;
    }

    public StoreRecord loadRec(Map<String, Object> params) throws Exception {
        long id = UtCnv.toLong(params.get("id"));
        StoreRecord st = getMdb().createStoreRecord("Attrib");
        getMdb().loadQueryRecord(st, "select * from Attrib where id=:id", Map.of("id", id));

        return st;
    }

    @DaoMethod
    public Store loadAttribChar(Map<String, Object> params) throws Exception {
        Store st = getMdb().createStore("AttribChar");
        getMdb().loadQuery(st, "select * from AttribChar where attrib=:attrib", params);

        return st;
    }

    @DaoMethod
    public Store insertAttribChar(Map<String, Object> params) throws Exception {
        Map<String, Object> rec = UtCnv.toMap(params.get("rec"));
        long id = getMdb().insertRec("AttribChar", rec);
        //
        Store st = getMdb().createStore("AttribChar");
        getMdb().loadQuery(st, "select * from AttribChar where id=:id", Map.of("id", id));

        return st;
    }

    @DaoMethod
    public Store updateAttribChar(Map<String, Object> params) throws Exception {
        Map<String, Object> rec = (UtCnv.toMap(params.get("rec")));
        long id = UtCnv.toLong(rec.get("id"));
        if (id == 0) {
            throw new XError("Поле id должно иметь не нулевое значение");
        }
        //
        getMdb().updateRec("AttribChar", rec);
        //
        // Загрузка записи
        Store st = getMdb().createStore("AttribChar");

        getMdb().loadQuery(st, "select * from AttribChar where id=:id", Map.of("id", id));

        //getMdb().outTable(st);
        return st;
    }

    @DaoMethod
    public void deleteAttribChar(Map<String, Object> rec) throws Exception {
        long id = UtCnv.toLong(rec.get("id"));
        getMdb().deleteRec("AttribChar", id);
    }

    @DaoMethod
    public Store loadForSelect() throws Exception {
        Store st = getMdb().createStore("Attrib");
        return getMdb().loadQuery(st, "select * from Attrib where 0=0");
    }


}
