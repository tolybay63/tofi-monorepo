package tofi.mdl.model.dao.prop.multi;

import jandcode.commons.UtCnv;
import jandcode.commons.UtString;
import jandcode.commons.error.XError;
import jandcode.core.dao.DaoMethod;
import jandcode.core.dbm.dict.DictService;
import jandcode.core.dbm.mdb.BaseMdbUtils;
import jandcode.core.store.Store;
import jandcode.core.store.StoreRecord;
import tofi.mdl.consts.FD_AccessLevel_consts;
import tofi.mdl.consts.FD_PeriodType_consts;
import tofi.mdl.model.utils.EntityMdbUtils;

import java.util.*;


public class MultiPropMdbUtils extends BaseMdbUtils {

    @DaoMethod
    public Store newRecMultiProp(long propGr) throws Exception {
        Store st = getMdb().createStore("MultiProp");
        StoreRecord r = st.add();
        r.set("multiPropGr", propGr);
        r.set("accessLevel", FD_AccessLevel_consts.common);
        r.set("isUniq", true);
        r.set("isDependValueOnPeriod", true);
        r.set("fillMore", 1);
        getMdb().resolveDicts(st);
        return st;
    }

    @DaoMethod
    public Store loadMultiProp(long propGr) throws Exception {

        Store st = getMdb().createStore("MultiProp");
        String sql = """
                    select *
                    from MultiProp
                    where multiPropGr=:gr
                    order by ord
                """;
        getMdb().loadQuery(st, sql, Map.of("gr", propGr));
        getMdb().resolveDicts(st);
        return st;
    }

    @DaoMethod
    public Store loadRec(long id) throws Exception {
        Store st = getMdb().createStore("MultiProp");
        String sql = """
                    select *
                    from MultiProp where id=:id
                """;
        getMdb().loadQuery(st, sql, Map.of("id", id));
        getMdb().resolveDicts(st);
        return st;
    }

    @DaoMethod
    public void deleteMultiProp(Map<String, Object> params) throws Exception {
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), "MultiProp");
        eu.deleteEntity(params);
    }

    @DaoMethod
    public Store insertMultiProp(Map<String, Object> params) throws Exception {
        Map<String, Object> rec = UtCnv.toMap(params.get("rec"));
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), "MultiProp");
        long id = eu.insertEntity(rec);
        //
        if (UtCnv.toBoolean(rec.get("isDependValueOnPeriod"))) {
            getMdb().insertRec("MultiPropPeriodType", Map.of("multiProp", id, "periodType", FD_PeriodType_consts.year));
        }
        long factor = UtCnv.toLong(rec.get("statusFactor"));
        if (factor > 0) {
            Store stFv = getMdb().loadQuery("select id from Factor where parent=:p", Map.of("p", factor));
            Store stS = getMdb().createStore("MultiPropStatus");
            int i = 0;
            for (StoreRecord r : stFv) {
                StoreRecord record = stS.add();
                record.set("multiProp", id);
                record.set("factorVal", r.getLong("id"));
                if (i == 0) record.set("isDefault", true);
                getMdb().insertRec("MultiPropStatus", record, true);
                i++;
            }
        }
        long typ = UtCnv.toLong(rec.get("providerTyp"));
        if (typ > 0) {
            Store stCls = getMdb().loadQuery("select id from Cls where typ=:t limit 1", Map.of("t", typ));
            Store stQ = getMdb().createStore("MultiPropProvider");
            for (StoreRecord r : stCls) {
                StoreRecord record = stQ.add();
                record.set("multiProp", id);
                record.set("cls", r.getLong("id"));
                record.set("isDefault", false);
                getMdb().insertRec("MultiPropProvider", record, true);
            }
        }
        //
        Store st = getMdb().createStore("MultiProp");
        getMdb().loadQuery(st, """
                    select *
                    from MultiProp where id=:id
                """, Map.of("id", id));
        getMdb().resolveDicts(st);
        return st;
    }

    @DaoMethod
    public Store updateMultiProp(Map<String, Object> params) throws Exception {
        Map<String, Object> rec = UtCnv.toMap(params.get("rec"));
        long id = UtCnv.toLong(rec.get("id"));
        //
        if (!UtCnv.toBoolean(rec.get("isDependValueOnPeriod"))) {
            Store stTmp = getMdb().loadQuery("""
                        select 0 as id, periodType from MultiPropPeriodType where multiProp=:pm;
                    """, Map.of("pm", id));
            List<Map<String, Object>> lstTmp = new ArrayList<>();
            stTmp.forEach(r -> {
                lstTmp.add(r.getValues());
            });
            String msg = saveMultiPropPeriodType(id, lstTmp);
            if (!msg.isEmpty()) {
                if (msg.contains(","))
                    msg = msg + " используются в данных";
                else
                    msg = msg + " используется в данных";
                throw new XError(msg);
            }
        }
        //
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), "MultiProp");
        eu.updateEntity(rec);
        //
        Store st = getMdb().createStore("MultiProp");
        getMdb().loadQuery(st, """
                    select *
                    from MultiProp where id=:id
                """, Map.of("id", id));
        getMdb().resolveDicts(st);
        return st;
    }


    @DaoMethod
    public boolean checkStatus(long multiProp) throws Exception {
        Store st = getMdb().loadQuery("""
                          select id from MultiPropStatus where multiProp=:p
                """, Map.of("p", multiProp));
        return st.size() > 0;
    }

    @DaoMethod
    public boolean checkProvider(long multiProp) throws Exception {
        Store st = getMdb().loadQuery("""
                          select id from MultiPropProvider where multiProp=:p
                """, Map.of("p", multiProp));
        return st.size() > 0;
    }

    @DaoMethod
    public boolean checkCondition(long multiProp) throws Exception {
        Store st = getMdb().loadQuery("""
                          select id from MultiPropCond where multiProp=:p
                """, Map.of("p", multiProp));
        return st.size() > 0;
    }

    @DaoMethod
    public Store loadStatus(long fk) throws Exception {
        Store st = getMdb().createStore("MultiPropStatus.full");
        getMdb().loadQuery(st, """
                    select p.*, f.name, f.cod
                    from MultiPropStatus p
                        left join Factor f on f.parent is not null and p.factorval=f.id
                    where multiProp=:p
                    order by f.ord
                """, Map.of("p", fk));

        return st;
    }

    @DaoMethod
    public Store loadStatusForUpd(long prop, long factor) throws Exception {
        Store st = getMdb().createStore("MultiPropStatus.full");
        getMdb().loadQuery(st, """
                    select p.id, f.cod, f.name, p.multiProp, f.id as factorVal, p.isDefault,
                        case when p.factorval is not null then 1 else null end as checked
                    from Factor f
                        left join MultiPropStatus p on p.multiProp=:p and f.id=p.factorval
                    where f.parent=:f
                    order by f.ord
                """, Map.of("p", prop, "f", factor));

        return st;
    }

    private Set<Object> getStatusInData(long multiProp, Set<Object> oldFvs) throws Exception {
        // todo запрос на данные
        String wheFvs = "(" + UtString.join(oldFvs, ",") + ")";
        if (wheFvs.equals("()")) wheFvs = "(0)";

/*
        Store stFvsData = getMdb().loadQuery("""
                    select distinct fv.id, fv.cod
                    from DataMultiProp d, DataMultiPropCell v, MultiPropStatus p, Factor fv
                    where d.multiProp=:pm and d.id=v.dataMultiProp and d.multiProp=p.multiProp and
                        p.factorVal=d.status and d.status =fv.id and d.status in
                """ + wheFvs, Map.of("pm", multiProp));
        Set<Object> oldStatusData = stFvsData.getUniqueValues("id");
*/

        return Set.of(0);
    }

    @DaoMethod
    public String saveStatus(long multiProp, List<Map<String, Object>> params) throws Exception {
        //Old values
        Store stOld = getMdb().createStore("MultiPropStatus.full");
        getMdb().loadQuery(stOld, """
                    select p.id, p.factorVal, f.cod from MultiPropStatus p, Factor f
                    where p.multiProp=:p and p.factorVal=f.id
                """, Map.of("p", multiProp));

        //OldFvsData
        Set<Object> oldIds = stOld.getUniqueValues("id");
        Set<Object> oldFvs = stOld.getUniqueValues("factorVal");

        Set<Object> oldStatusData = getStatusInData(multiProp, oldFvs);

        //New values
        Store stNew = getMdb().createStore("MultiPropStatus");
        for (Map<String, Object> map : params) {
            StoreRecord r = stNew.add(map);
            r.set("multiProp", multiProp);
        }
        Set<Object> newIds = stNew.getUniqueValues("id");

        Set<String> codsFvsForInfo = new HashSet<>();
        //Deleting
        for (StoreRecord r : stOld) {
            if (!newIds.contains(r.getLong("id"))) {
                if (oldStatusData.contains(r.getLong("factorVal"))) {
                    codsFvsForInfo.add(r.getString("cod"));
                } else {
                    try {
                        getMdb().deleteRec("MultiPropStatus", r.getLong("id"));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
        //Saving
        for (StoreRecord r : stNew) {
            if (!oldIds.contains(r.getLong("id"))) {
                getMdb().insertRec("MultiPropStatus", r, true);
            } else {
                getMdb().updateRec("MultiPropStatus", r);
            }
        }

        return UtString.join(codsFvsForInfo, ",");
    }


    @DaoMethod
    public Store loadProvider(long prop) throws Exception {
        Store st = getMdb().createStore("MultiPropProvider.full");

        getMdb().loadQuery(st, """
                    select p.*, c.name as nameCls, '' as nameObj from MultiPropProvider p
                        left join ClsVer c on p.cls=c.ownerVer and c.lastVer=1
                    where p.multiProp=:p
                """, Map.of("p", prop));

        //todo Запрос к Db data

        return st;
    }

    @DaoMethod
    public Store loadProviderClsForSelect(long prop, long typ, String mode) throws Exception {

        String whe = "";
        if (mode.equals("ins"))
            whe = " and c.id not in (select cls from MultiPropProvider where multiProp=:p)";

        String sql = """
                    select c.id, name from Cls c, ClsVer v where c.id=v.ownerVer and v.lastVer=1 and c.typ=:t
                """ + whe;

        return getMdb().loadQuery(sql, Map.of("t", typ, "p", prop));
        //

    }

    //todo Delete!
    @DaoMethod
    public Store loadProviderForUpd(long prop, long typ) throws Exception {
        Store st = getMdb().createStore("MultiPropProvider.full");
        getMdb().loadQuery("""
                    select * from MultiPropProvider where 0=0
                """);


        //todo Запрос к Db data

/*
        getMdb().loadQuery(st, """
                    with c as (
                        select id as cls from Cls where typ=:t
                    )
                    select * from (
                        select 'c_'||c.id  as id, '' as parent, c.cod, v.name, null as isDefault,
                        case when exists (select obj from MultiPropProvider where cls=c.id and multiProp=:p) then 1 else null end as checked,
                        -1 as ord, c.id as cls, null as obj
                        from Cls c, ClsVer v
                        where c.id=v.ownerVer and v.lastVer=1 and c.id in (select cls from c)
                        union all
                        select 'o_'||o.id as id, 'c_'||o.cls as parent, o.cod, v.name, p.isDefault,
                            case when p.obj is not null then 1 else null end as checked, o.ord, o.cls, o.id as obj
                        from Obj o
                            left join ObjVer v on o.id=v.ownerVer and v.lastVer=1
                            left join MultiPropProvider p on p.multiProp=:p and p.obj=o.id
                        where o.cls in (select cls from c)
                    ) t order by ord
                """, Map.of("p", prop, "t", typ));
*/
        return st;
    }

    @DaoMethod
    public StoreRecord saveProvider(Map<String, Object> rec, String mode) throws Exception {
        StoreRecord record = getMdb().createStoreRecord("MultiPropProvider", rec);
        long id;
        if (mode.equals("ins")) {
            id = getMdb().insertRec("MultiPropProvider", record, true);
        } else {
            id = UtCnv.toLong(rec.get("id"));
            getMdb().updateRec("MultiPropProvider", record);
        }
        //
        return loadProviderRec(id);
    }

    private StoreRecord loadProviderRec(long id) throws Exception {
        StoreRecord st = getMdb().createStoreRecord("MultiPropProvider.full");

        getMdb().loadQueryRecord(st, """
                    select p.*, c.name as nameCls, '' as nameObj
                    from MultiPropProvider p
                        left join ClsVer c on p.cls=c.ownerVer and c.lastVer=1
                    where p.id=:id
                """, Map.of("id", id));

        //todo Запрос к Db data

        return st;
    }


    @DaoMethod
    public void deleteProvider(long id) throws Exception {
        getMdb().deleteRec("MultiPropProvider", id);
    }

    @DaoMethod
    public Store loadMultiPropDim(long pm) throws Exception {
        Store st = getMdb().createStore("MultiPropDim.full");
        getMdb().loadQuery(st, """
                    select p.*, d.name, d.fullName, d.cod
                    from MultiPropDim p
                        left join DimMultiProp d on d.id=p.dimMultiProp
                    where p.multiProp=:pm
                    order by p.ord
                """, Map.of("pm", pm));

        return st;
    }

    @DaoMethod
    public Store loadMultiPropDimForUpd(long multiProp) throws Exception {
        Store st = getMdb().createStore("MultiPropDim.full");
        getMdb().loadQuery(st, """
                    select d.id as dimId, d.name, d.fullName, d.cod, p.*,
                        case when p.id is null then false else true end as checked
                    from DimMultiProp d
                        left join MultiPropDim p on d.id=p.dimMultiProp and p.multiProp=:pm
                    where 0=0
                    order by p.ord
                """, Map.of("pm", multiProp));

        return st;
    }

    @DaoMethod
    public void saveMultiPropDim(long multiProp, List<Map<String, Object>> params) throws Exception {
        Set<Object> oldIds = getMdb().loadQuery("select id from MultiPropDim where multiProp=:pm",
                Map.of("pm", multiProp)).getUniqueValues("id");

        Store st = getMdb().createStore("MultiPropDim");
        for (Map<String, Object> map : params) {
            StoreRecord r = st.add(map);
            //getMdb().outMap(map);
        }

        //getMdb().outTable(st);
        Set<Object> newIds = st.getUniqueValues("id");
        // Deleteing
        oldIds.forEach(id -> {
            if (!newIds.contains(id)) {
                try {
                    getMdb().deleteRec("MultiPropDim", UtCnv.toLong(id));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        //Saving
        for (StoreRecord r : st) {
            if (!oldIds.contains(r.getLong("id"))) {
                long id = getMdb().getNextId("MultiPropDim");
                r.set("id", id);
                r.set("ord", id);
                r.set("multiProp", multiProp);
                getMdb().insertRec("MultiPropDim", r, false);
            } else {
                r.set("ord", r.getLong("id"));
                r.set("multiProp", multiProp);

                getMdb().updateRec("MultiPropDim", r);
            }
        }

        //getMdb().outTable(st);
    }

    @DaoMethod
    public void changeOrdMultiPropDim(Map<String, Object> params) throws Exception {
        Map<String, Object> rec = UtCnv.toMap(params.get("rec"));
        boolean up = UtCnv.toBoolean(params.get("up"));
        long multiProp = UtCnv.toLong(rec.get("multiProp"));
        long id1 = UtCnv.toLong(rec.get("id"));
        long ord1 = UtCnv.toLong(rec.get("ord"));
        long id2;
        long ord2;

        Store st = getMdb().loadQuery("""
                    select * from MultiPropDim where multiProp=:pm order by ord
                """, Map.of("pm", multiProp));
        int k = 0;  //искомая позиция
        for (int i = 0; i < st.size(); i++) {
            if (st.get(i).getLong("id") == id1) {
                k = i;
                break;
            }
        }
        if (up) {
            id2 = st.get(k - 1).getLong("id");
            ord2 = st.get(k - 1).getLong("ord");
        } else {
            id2 = st.get(k + 1).getLong("id");
            ord2 = st.get(k + 1).getLong("ord");
        }
        //
        getMdb().execQuery("""
                    update MultiPropDim set ord=:ord2 where id=:id1;
                    update MultiPropDim set ord=:ord1 where id=:id2;
                """, Map.of("id1", id1, "id2", id2, "ord1", ord1, "ord2", ord2));
    }

    @DaoMethod
    public Store loadMultiPropPeriodType(long multiProp) throws Exception {
        Store st = getMdb().createStore("MultiPropPeriodType");
        return getMdb().loadQuery(st, "select * from MultiPropPeriodType where multiProp=:p", Map.of("p", multiProp));
    }

    @DaoMethod
    public Store loadMultiPropPeriodTypeForUpd(long multiProp) throws Exception {
        Store st = getMdb().createStore("MultiPropPeriodType.full");
        String sql = """
                    select p.id, p.text as name, t.id as idInTable,
                        case when t.periodType is null then false else true end as checked
                    from FD_PeriodType p
                        left join MultiPropPeriodType t on t.multiProp=:p and p.id=t.periodType
                    where 0=0
                    order by p.ord
                """;
        return getMdb().loadQuery(st, sql, Map.of("p", multiProp));
    }

    @DaoMethod
    public String saveMultiPropPeriodType(long multiProp, List<Map<String, Object>> params) throws Exception {
        //Old Data
        Store stOld = getMdb().loadQuery("select id, periodType from MultiPropPeriodType where multiProp=:p",
                Map.of("p", multiProp));
        Set<Object> idsOld = stOld.getUniqueValues("id");
        //period in data
        //todo Db Data
/*
        Store stTmp = getMdb().loadQuery("select periodType from DataPropMatrix where multiProp=:pm",
                Map.of("pm", multiProp));
        Set<Object> periodsOldInData = stTmp.getUniqueValues("periodType");
*/
        Set<Object> periodsOldInData = Set.of(0);


        DictService dc = getMdb().getModel().bean(DictService.class);
        Store dd = dc.loadDictData("FD_PeriodType").getData();

        //New values
        Store stNew = getMdb().createStore("MultiPropPeriodType");
        for (Map<String, Object> map : params) {
            StoreRecord r = stNew.add(map);
            r.set("multiProp", multiProp);
        }
        Set<Object> idsNew = stNew.getUniqueValues("id");
        Set<String> codsPeriodForInfo = new HashSet<>();

        //Deleting
        for (StoreRecord r : stOld) {
            if (!idsNew.contains(r.getLong("id"))) {
                if (periodsOldInData.contains(r.getLong("periodType"))) {
                    codsPeriodForInfo.add(dc.loadDictData("FD_PeriodType")
                            .getData().getById(r.get("periodType")).getString("text"));
                } else {
                    try {
                        getMdb().deleteRec("MultiPropPeriodType", r.getLong("id"));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }

        for (StoreRecord r : stNew) {
            if (!idsOld.contains(r.getLong("id"))) {
                getMdb().insertRec("MultiPropPeriodType", r, true);
            }
        }

        return UtString.join(codsPeriodForInfo, ",");
    }

    @DaoMethod
    public Store loadMultiPropCond(long multiProp) throws Exception {
        Store st = getMdb().createStore("MultiPropCond.full");
        String sql = """
                    select p.*,
                        case when p.factor is not null then f.name
                            when p.typ is not null then vt.name else vr.name end as name,
                        case when p.factor is not null then f.cod
                            when p.typ is not null then t.cod else r.cod end as cod,
                        case when p.factor is not null then 'factor'
                            when p.typ is not null then 'typ' else 'reltyp' end as flag
                    from MultiPropCond p left join Factor f on p.factor=f.id
                        left join Typ t on p.typ=t.id left join TypVer vt on t.id=vt.ownerVer and vt.lastVer=1
                        left join RelTyp r on p.relTyp=r.id left join RelTypVer vr on r.id=vr.ownerVer and vr.lastVer=1
                    where p.multiProp=:pm
                """;
        return getMdb().loadQuery(st, sql, Map.of("pm", multiProp));
    }

    @DaoMethod
    public void deleteMultiPropCond(long id) throws Exception {
        getMdb().deleteRec("MultiPropCond", id);
    }

    @DaoMethod
    public Store loadFactorsForSelect(long multiProp, String mode) throws Exception {
        String sql = """
                    with ids as (
                        select id from Factor where parent is null
                        except
                        select factor as id from MultiPropCond where multiProp=:pm
                    )
                    select id, name from Factor where parent is null and id in (select id from ids)
                """;
        if (mode.equals("upd")) {
            sql = """
                        select id, name from Factor where parent is null
                            and id in (select factor from MultiPropCond where multiProp=:pm)
                    """;
        }
        return getMdb().loadQuery(sql, Map.of("pm", multiProp));
    }

    @DaoMethod
    public Store loadTypsForSelect(long multiProp, String mode) throws Exception {
        String sql = """
                    with ids as (
                        select id from Typ where 0=0
                        except
                        select typ as id from MultiPropCond where multiProp=:pm
                    )
                    select t.id, v.name from Typ t, TypVer v where t.id=v.ownerVer and v.lastVer=1
                        and t.id in (select id from ids)
                """;
        if (mode.equals("upd")) {
            sql = """
                        select t.id, v.name from Typ t, TypVer v where t.id=v.ownerVer and v.lastVer=1
                            and t.id in (select typ from MultiPropCond where multiProp=:pm)
                    """;
        }
        return getMdb().loadQuery(sql, Map.of("pm", multiProp));
    }

    @DaoMethod
    public Store loadRelTypsForSelect(long multiProp, String mode) throws Exception {
        String sql = """
                    with ids as (
                        select id from RelTyp where 0=0
                        except
                        select relTyp as id from MultiPropCond where multiProp=:pm
                    )
                    select t.id, v.name from RelTyp t, RelTypVer v where t.id=v.ownerVer and v.lastVer=1
                        and t.id in (select id from ids)
                """;
        if (mode.equals("upd")) {
            sql = """
                        select t.id, v.name from RelTyp t, RelTypVer v where t.id=v.ownerVer and v.lastVer=1
                            and t.id in (select relTyp from MultiPropCond where multiProp=:pm)
                    """;
        }
        return getMdb().loadQuery(sql, Map.of("pm", multiProp));
    }

    @DaoMethod
    public void insertMultiPropCond(Map<String, Object> rec) throws Exception {
        Store st = getMdb().createStore("MultiPropCond");
        StoreRecord r = st.add(rec);
        getMdb().insertRec("MultiPropCond", r, true);
    }

    @DaoMethod
    public void updateMultiPropCond(Map<String, Object> rec) throws Exception {
        Store st = getMdb().createStore("MultiPropCond");
        StoreRecord r = st.add(rec);
        getMdb().updateRec("MultiPropCond", r);
    }


}
