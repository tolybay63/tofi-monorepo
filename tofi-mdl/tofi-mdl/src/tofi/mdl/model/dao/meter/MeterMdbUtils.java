package tofi.mdl.model.dao.meter;

import jandcode.commons.UtCnv;
import jandcode.commons.error.XError;
import jandcode.core.dao.DaoMethod;
import jandcode.core.dbm.dict.DictService;
import jandcode.core.dbm.mdb.BaseMdbUtils;
import jandcode.core.dbm.sql.SqlText;
import jandcode.core.store.Store;
import jandcode.core.store.StoreRecord;
import tofi.mdl.consts.*;
import tofi.mdl.model.utils.EntityMdbUtils;

import java.util.HashMap;
import java.util.Map;


public class MeterMdbUtils extends BaseMdbUtils {

    /**
     * Загрузка Meter с пагинацией
     *
     * @param params
     * @return
     * @throws Exception
     */
    @DaoMethod
    public Map<String, Object> loadMeterPaginate(Map<String, Object> params) throws Exception {
        String sql0 = "select f.* from Meter f where 0=0 order by f.id";
        SqlText sqlText = getMdb().createSqlText(sql0);
        String filter = UtCnv.toString(params.get("filter")).trim();

        //count
        String sql = "select count(*) as cnt from Meter f where 0=0";
        sqlText.setSql(sql);
        if (!filter.isEmpty())
            sqlText = sqlText.addWhere("name like '%" + filter + "%' or fullName like '%" + filter + "%' or cod like '%" + filter + "%'");
        int total = getMdb().loadQuery(sqlText).get(0).getInt("cnt");
        int lm = UtCnv.toInt(params.get("rowsPerPage")) == 0 ? total : UtCnv.toInt(params.get("rowsPerPage"));
        Map<String, Object> meta = new HashMap<String, Object>();
        meta.put("total", total);
        meta.put("page", UtCnv.toInt(params.get("page")));
        meta.put("limit", lm);

        Map<String, Object> par = new HashMap<>();
        int offset = (UtCnv.toInt(params.get("page")) - 1) * lm;
        par.put("offset", offset);
        par.put("limit", lm);
        sqlText.setSql(sql0);
        sqlText.paginate(true);

        if (!UtCnv.toString(params.get("sortBy")).trim().isEmpty()) {
            String orderBy = UtCnv.toString(params.get("sortBy"));
            if (UtCnv.toBoolean(params.get("descending"))) {
                orderBy = orderBy + " desc";
            }
            sqlText = sqlText.replaceOrderBy(orderBy);
        }

        if (!filter.isEmpty())
            sqlText = sqlText.addWhere("(cod like '%" + filter + "%' or f.name like '%" + filter + "%' or " +
                    "f.fullName like '%" + filter + "%')");
        Store st = getMdb().createStore("Meter");
        getMdb().loadQuery(st, sqlText, par);

        return Map.of("store", st, "meta", meta);
    }

    /**
     * Delete Meter
     *
     * @param
     * @throws Exception
     */

    @DaoMethod
    public void delete(Map<String, Object> rec) throws Exception {
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), "Meter");
        eu.deleteEntity(rec);
    }

    /**
     * Update Meter
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
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), "Meter");
        eu.updateEntity(rec);
        //
        // Загрузка записи
        Store st = getMdb().createStore("Meter");
        getMdb().loadQuery(st, "select * from Meter where id=:id", Map.of("id", id));

        //getMdb().outTable(st);
        return st;
    }

    /**
     * Insert Meter
     *
     * @param params
     * @return
     * @throws Exception
     */
    @DaoMethod
    public Store insert(Map<String, Object> params) throws Exception {
        Map<String, Object> rec = UtCnv.toMap(params.get("rec"));
        //
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), "Meter");
        long id = eu.insertEntity(rec);
        //
        Store st = getMdb().createStore("Meter");

        getMdb().loadQuery(st, "select * from Meter where id=:id", Map.of("id", id));

        return st;
    }

    @DaoMethod
    public StoreRecord loadRec(Map<String, Object> params) throws Exception {
        long id = UtCnv.toLong(params.get("id"));
        StoreRecord st = getMdb().createStoreRecord("Meter");
        getMdb().loadQueryRecord(st, "select * from Meter where id=:id", Map.of("id", id));
        return st;
    }

    @DaoMethod
    public StoreRecord newRec(Map<String, Object> params) throws Exception {
        DictService dictSvc = getMdb().getModel().bean(DictService.class);
        Store st = getMdb().createStore("Meter");
        StoreRecord rec = st.add();
        rec.set("accessLevel", FD_AccessLevel_consts.common);
        rec.set("meterStruct", FD_MeterStruct_consts.soft);
        rec.set("meterDeterm", FD_MeterDeterm_consts.determ);
        rec.set("distributionLaw", FD_DistributionLaw_consts.uniform);
        rec.set("meterTypeByRate", FD_MeterType_consts.integral);
        rec.set("meterTypeByPeriod", FD_MeterType_consts.integral);
        rec.set("meterTypeByMember", FD_MeterType_consts.integral);
        rec.set("meterBehavior", FD_MeterBehavior_consts.positive);

        return rec;
    }

    @DaoMethod
    public Store loadForSelect() throws Exception {
        Store st = getMdb().createStore("Meter.select");
        return getMdb().loadQuery(st, "select id, name, meterStruct, measure from Meter where 0=0");
    }


}
