package tofi.mdl.model.dao.prop.multi.dim;

import jandcode.commons.UtCnv;
import jandcode.commons.UtString;
import jandcode.commons.error.XError;
import jandcode.core.dao.DaoMethod;
import jandcode.core.dbm.mdb.BaseMdbUtils;
import jandcode.core.store.Store;
import jandcode.core.store.StoreField;
import jandcode.core.store.StoreIndex;
import jandcode.core.store.StoreRecord;
import tofi.mdl.consts.FD_AccessLevel_consts;
import tofi.mdl.consts.FD_DimMultiPropType_consts;
import tofi.mdl.consts.FD_MultiValEntityType_consts;
import tofi.mdl.model.utils.EntityMdbUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class DimMultiPropMdbUtils extends BaseMdbUtils {

    @DaoMethod
    public Store newRec(long propGr) throws Exception {
        Store st = getMdb().createStore("DimMultiProp");
        StoreRecord r = st.add();
        r.set("dimMultiPropGr", propGr);
        r.set("accessLevel", FD_AccessLevel_consts.common);
        r.set("dimMultiPropType", FD_DimMultiPropType_consts.stat);
        getMdb().resolveDicts(st);
        return st;
    }

    @DaoMethod
    public Store loadDimMultiProp(long propGr) throws Exception {

        Store st = getMdb().createStore("DimMultiProp");
        String sql = "select * from DimMultiProp where dimMultiPropGr=:gr";
        getMdb().loadQuery(st, sql, Map.of("gr", propGr));
        getMdb().resolveDicts(st);
        return st;
    }

    @DaoMethod
    public Store insert(Map<String, Object> params) throws Exception {
        Map<String, Object> rec = UtCnv.toMap(params.get("rec"));
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), "DimMultiProp");
        long id = eu.insertEntity(rec);
        Store st = getMdb().createStore("DimMultiProp");
        getMdb().loadQuery(st, "select * from DimMultiProp where id=:id", Map.of("id", id));
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
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), "DimMultiProp");
        eu.updateEntity(rec);
        // Загрузка записи
        Store st = getMdb().createStore("DimMultiProp");

        getMdb().loadQuery(st, "select * from DimMultiProp where id=:id", Map.of("id", id));
        getMdb().resolveDicts(st);
        return st;
    }

    @DaoMethod
    public void delete(Map<String, Object> rec) throws Exception {
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), "DimMultiProp");
        eu.deleteEntity(rec);
    }

    //DimMultiPropItem
    @DaoMethod
    public Map<String, Object> loadDimMultiPropItem(long dimMultiProp) throws Exception {
        Store st = getMdb().createStore("DimMultiPropItem.full");

        Store stTitle = getMdb().loadQuery("""
                    select id, title from DimMultiPropName where dimMultiProp=:dmp order by ord
                """, Map.of("dmp", dimMultiProp));

        boolean isDopCols = stTitle.size() > 0;


        String sql = """
                    select *,
                    case
                        when multiEntityType = 1 then 'meter'
                        when multiEntityType = 2 then 'factorval'
                        when multiEntityType = 3 then 'obj'
                        when multiEntityType = 4 then 'relobj'
                        when multiEntityType = 5 then 'formula'
                        when multiEntityType = 6 then 'alg'
                        when multiEntityType = 7 then 'attr_str'
                        when multiEntityType = 8 then 'attr_mask'
                        when multiEntityType = 9 then 'attr_date'
                    end as entityType
                    from DimMultiPropItem where dimMultiProp=:dmp
                """;
        getMdb().loadQuery(st, sql, Map.of("dmp", dimMultiProp));

        if (!isDopCols) {
            return Map.of("rows", st, "titles", stTitle);
        }
        //
        Set<Object> ids = st.getUniqueValues("id");
        String where = "(0" + UtString.join(ids, ",") + ")";
        Store stDMPIN = getMdb().loadQuery("""
                    select *, dimMultiPropItem || '_' || dimMultiPropName as key
                    from DimMultiPropItemName where dimMultiPropItem in
                """ + where);
        StoreIndex indDMPIN = stDMPIN.getIndex("key");

        for (Object o : ids) {
            long id = UtCnv.toLong(o);
            for (StoreRecord r : stTitle) {
                String key = id + "_" + r.getString("id");
                StoreRecord rec = indDMPIN.get(key);
                if (rec == null) {  //add
                    getMdb().insertRec("DimMultiPropItemName",
                            Map.of("dimMultiPropItem", id, "dimMultiPropName", r.getLong("id")),
                            true);
                }
            }
        }
        //
        for (StoreRecord r : stTitle) {
            StoreField fld = st.addField("col_" + r.getString("id"), "string", 150);
            fld.setTitle(r.getString("title"));
        }

        for (StoreRecord r : st) {
            for (StoreField fld : r.getFields()) {
                if (fld.getName().startsWith("col_")) {
                    String idDMPIN = fld.getName().split("_")[1];
                    StoreRecord record = indDMPIN.get(r.getString("id") + "_" + idDMPIN);
                    String nm = null;
                    if (record != null) {
                        nm = record.getString("name");
                    }
                    r.set(fld.getName(), nm);
                }
            }
        }

        //
        return Map.of("rows", st, "titles", stTitle);
    }

    private Store loadDimMultiPropItemRec(long id, long dmp) throws Exception {
        Store st = getMdb().createStore("DimMultiPropItem.full");

        Store stTitle = getMdb().loadQuery("""
                    select id, title from DimMultiPropName where dimMultiProp=:dmp order by ord
                """, Map.of("dmp", dmp));

        boolean isDopCols = stTitle.size() > 0;


        for (StoreRecord r : stTitle) {
            StoreField fld = st.addField("col_" + r.getString("id"), "string", 150);
            fld.setTitle(r.getString("title"));
        }

        String sql = """
                    select *,
                    case
                        when multiEntityType = 1 then 'meter'
                        when multiEntityType = 2 then 'factorval'
                        when multiEntityType = 3 then 'obj'
                        when multiEntityType = 4 then 'relobj'
                        when multiEntityType = 5 then 'formula'
                        when multiEntityType = 6 then 'alg'
                        when multiEntityType = 7 then 'attr_str'
                        when multiEntityType = 8 then 'attr_mask'
                        when multiEntityType = 9 then 'attr_date'
                    end as entityType
                    from DimMultiPropItem where id=:id
                """;
        getMdb().loadQuery(st, sql, Map.of("id", id));
        //
        Store stDMPIN = getMdb().loadQuery("""
                    select *, dimMultiPropItem || '_' || dimMultiPropName as key
                    from DimMultiPropItemName where dimMultiPropItem=:id
                """, Map.of("id", id));
        StoreIndex indDMPIN = stDMPIN.getIndex("key");
        //
        for (StoreRecord r : st) {
            for (StoreField fld : r.getFields()) {
                if (fld.getName().startsWith("col_")) {
                    String idDMPIN = fld.getName().split("_")[1];
                    StoreRecord record = indDMPIN.get(r.getString("id") + "_" + idDMPIN);
                    String nm = null;
                    if (record != null) {
                        nm = record.getString("name");
                    }
                    r.set(fld.getName(), nm);
                }
            }
        }
        //
        return st;
    }

    @DaoMethod
    public Store newRecDimMultiPropItem(long propDim) throws Exception {
        Store st = getMdb().createStore("DimMultiPropItem.full");
        StoreRecord r = st.add();
        r.set("dimMultiProp", propDim);
        r.set("multiEntityType", FD_MultiValEntityType_consts.meter);
        return st;
    }


    @DaoMethod
    public Store insertDimItem(Map<String, Object> params) throws Exception {
        Map<Long, String> mapCols = new HashMap<>();
        for (String c : params.keySet()) {
            if (c.startsWith("col_")) {
                mapCols.put(UtCnv.toLong(c.split("_")[1]), UtCnv.toString(params.get(c)));
            }
        }

        for (Long c : mapCols.keySet()) {
            params.remove("col_" + c);
        }
        //
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), "DimMultiPropItem");
        long id = eu.insertEntity(params);
        //
        long dmp = UtCnv.toLong(params.get("dimMultiProp"));
        for (Map.Entry<Long, String> entry : mapCols.entrySet()) {
            getMdb().insertRec("DimMultiPropItemName", Map.of("dimMultiPropItem", id,
                    "dimMultiPropName", entry.getKey(), "name", entry.getValue()), true);
        }
        //
        return loadDimMultiPropItemRec(id, dmp);
    }

    @DaoMethod
    public Store updateDimItem(Map<String, Object> params) throws Exception {
        long id = UtCnv.toLong(params.get("id"));
        long dmp = UtCnv.toLong(params.get("dimMultiProp"));
        Map<Long, String> mapCols = new HashMap<>();
        for (String c : params.keySet()) {
            if (c.startsWith("col_")) {
                mapCols.put(UtCnv.toLong(c.split("_")[1]), UtCnv.toString(params.get(c)));
            }
        }

        for (Long c : mapCols.keySet()) {
            params.remove("col_" + c);
        }
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), "DimMultiPropItem");
        eu.updateEntity(params);
        for (Map.Entry<Long, String> entry : mapCols.entrySet()) {
            getMdb().execQuery("""
                        update DimMultiPropItemName set name=:nm where dimMultiPropItem=:dmpi and dimMultiPropName=:dmpn
                    """, Map.of("nm", entry.getValue(), "dmpi", id, "dmpn", entry.getKey()));
        }
        return loadDimMultiPropItemRec(id, dmp);
    }

    @DaoMethod
    public void deleteDimItem(Map<String, Object> params) throws Exception {
        Store stTmp = getMdb().loadQuery("select id from DimMultiPropItemMeter where dimMultiPropItem=:id",
                Map.of("id", UtCnv.toLong(params.get("id"))));
        if (stTmp.size() == 0) {
            getMdb().execQuery("delete from DimMultiPropItemName where dimMultiPropItem=:id",
                    Map.of("id", UtCnv.toLong(params.get("id"))));
        }
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), "DimMultiPropItem");
        eu.deleteEntity(params);
    }

    /// /

    @DaoMethod
    public Store loadClsForSelect() throws Exception {
        return getMdb().loadQuery("""
                    select 't_'||t.id as id, null as parent, v.name, v.fullname, -t.id as cls, null as ent
                    from Typ t, TypVer v
                    where t.id=v.ownerVer and v.lastVer=1
                    union all
                    select 'c_'||c.id as id, 't_'||c.typ as parent, v.name, v.fullname, c.id as cls, 'cls' as ent
                    from Cls c, ClsVer v
                    where c.id=v.ownerVer and v.lastVer=1
                """);

    }

    @DaoMethod
    public Store loadObjForSelect(long cls) throws Exception {
        throw new XError("Запрос на другую базу");

//todo Запрос на данные
/*        return getMdb().loadQuery("""
            select 't_'||t.id as id, null as parent, v.name, null::bigint as obj
            from Typ t, TypVer v
            where t.id=v.ownerVer and v.lastVer=1
            union all
            select 'c_'||c.id as id, 't_'||c.typ as parent, v.name, null::bigint as obj
            from Cls c, ClsVer v
            where c.id=v.ownerVer and v.lastVer=1
            union all
            select 'o_'||o.id as id, 'c_'||o.cls as parent, v.name, o.id as obj
            from Obj o, ObjVer v
            where o.id=v.ownerVer and v.lastVer=1
        """);*/

    }

    @DaoMethod
    public Store loadRelClsForSelect() throws Exception {
        return getMdb().loadQuery("""
                    select 't_'||t.id as id, null as parent, v.name, -t.id as relcls, null as ent
                    from RelTyp t, RelTypVer v
                    where t.id=v.ownerVer and v.lastVer=1
                    union all
                    select 'c_'||c.id as id, 't_'||c.reltyp as parent, v.name, c.id as relcls, 'relcls' as ent
                    from RelCls c, RelClsVer v
                    where c.id=v.ownerVer and v.lastVer=1
                """);
    }


    @DaoMethod
    public Store loadRelObjForSelect(long relCls) throws Exception {
        throw new XError("Запрос на другую базу");
//todo Запрос на данные
/*
        return getMdb().loadQuery("""
            select 'r_'||r.id as id, null as parent, v.name, null as relobj
            from RelTyp r, RelTypVer v
            where r.id=v.ownerVer and v.lastVer=1
            union all
            select 'o_'||o.id as id, 'r_'||o.reltyp as parent, v.name, o.id as relobj
            from RelObj o, RelObjVer v
            where o.id=v.ownerVer and v.lastVer=1
        """);
*/

    }

    @DaoMethod
    public Store loadProp() throws Exception {
        return getMdb().loadQuery("""
                    select id, name from prop
                    where proptype in (1,5,6)
                """);
    }

    @DaoMethod
    public Store loadDimMultiPropItemAttrib(long dimMultiPropItem) throws Exception {
        Store st = getMdb().createStore("DimMultiPropItemAttrib");
        return getMdb().loadQuery(st, """
                    select * from DimMultiPropItemAttrib
                    where dimMultiPropItem=:id
                """, Map.of("id", dimMultiPropItem));
    }

    private Store loadDimMultiPropItemAttribRec(long id) throws Exception {
        Store st = getMdb().createStore("DimMultiPropItemAttrib");
        return getMdb().loadQuery(st, """
                    select * from DimMultiPropItemAttrib
                    where id=:id
                """, Map.of("id", id));
    }

    @DaoMethod
    public Store insDimMultiPropItemAttrib(Map<String, Object> params) throws Exception {
        Store st = getMdb().createStore("DimMultiPropItemAttrib");
        StoreRecord r = st.add(params);
        long id = getMdb().insertRec("DimMultiPropItemAttrib", r, true);
        return loadDimMultiPropItemAttribRec(id);
    }

    @DaoMethod
    public Store updDimMultiPropItemAttrib(Map<String, Object> params) throws Exception {
        Store st = getMdb().createStore("DimMultiPropItemAttrib");
        StoreRecord r = st.add(params);
        long id = r.getLong("id");
        getMdb().updateRec("DimMultiPropItemAttrib", r);
        return loadDimMultiPropItemAttribRec(id);
    }

    @DaoMethod
    public void deleteDimMultiPropItemAttrib(long id) throws Exception {
        getMdb().deleteRec("DimMultiPropItemAttrib", id);
    }

    //
    @DaoMethod
    public Store loadDimMultiPropItemMeter(long dimMultiPropItem) throws Exception {
        Store st = getMdb().createStore("DimMultiPropItemMeter.full");
        return getMdb().loadQuery(st, """
                    select d.*, mea.name as measureName
                    from DimMultiPropItemMeter d
                        left join Measure mea on d.measure=mea.id
                    where d.dimMultiPropItem=:id
                """, Map.of("id", dimMultiPropItem));
    }

    private Store loadDimMultiPropItemMeterRec(long id) throws Exception {
        Store st = getMdb().createStore("DimMultiPropItemMeter.full");
        return getMdb().loadQuery(st, """
                    select d.*, m.name as measureName from DimMultiPropItemMeter d
                    left join Measure m on d.measure=m.id
                    where d.id=:id
                """, Map.of("id", id));
    }

    @DaoMethod
    public Store insDimMultiPropItemMeter(Map<String, Object> params) throws Exception {
        Store st = getMdb().createStore("DimMultiPropItemMeter");
        StoreRecord r = st.add(params);
        long id = getMdb().insertRec("DimMultiPropItemMeter", r, true);
        return loadDimMultiPropItemMeterRec(id);
    }

    @DaoMethod
    public Store updDimMultiPropItemMeter(Map<String, Object> params) throws Exception {
        Store st = getMdb().createStore("DimMultiPropItemMeter");
        StoreRecord r = st.add(params);
        long id = r.getLong("id");
        getMdb().updateRec("DimMultiPropItemMeter", r);
        return loadDimMultiPropItemMeter(id);
    }

    @DaoMethod
    public void deleteDimMultiPropItemMeter(long id) throws Exception {
        getMdb().deleteRec("DimMultiPropItemMeter", id);
    }

    //
    @DaoMethod
    public Store loadMeasure() throws Exception {
        Store st = getMdb().createStore("Measure");
        return getMdb().loadQuery(st, """
                    select * from Measure where 0=0
                """);
    }

    //Factor
    @DaoMethod
    public Store loadDimMultiPropItemFactor(long dimMultiPropItem) throws Exception {
        Store st = getMdb().createStore("DimMultiPropItemFactor.full");
        return getMdb().loadQuery(st, """
                    select d.*, f.name
                    from DimMultiPropItemFactor d
                        left join Factor f on d.factor=f.id and f.parent is null
                    where d.dimMultiPropItem=:id
                """, Map.of("id", dimMultiPropItem));
    }

    private Store loadDimMultiPropItemFactorRec(long id) throws Exception {
        Store st = getMdb().createStore("DimMultiPropItemFactor.full");
        return getMdb().loadQuery(st, """
                    select d.*, f.name
                    from DimMultiPropItemFactor d
                        left join Factor f on d.factor=f.id and f.parent is null
                    where d.id=:id
                """, Map.of("id", id));
    }

    @DaoMethod
    public Store insDimMultiPropItemFactor(Map<String, Object> params) throws Exception {
        Store st = getMdb().createStore("DimMultiPropItemFactor");
        StoreRecord r = st.add(params);
        long id = getMdb().insertRec("DimMultiPropItemFactor", r, true);
        return loadDimMultiPropItemFactorRec(id);
    }

    @DaoMethod
    public Store updDimMultiPropItemFactor(Map<String, Object> params) throws Exception {
        Store st = getMdb().createStore("DimMultiPropItemFactor");
        StoreRecord r = st.add(params);
        long id = r.getLong("id");
        getMdb().updateRec("DimMultiPropItemFactor", r);
        return loadDimMultiPropItemFactor(id);
    }

    @DaoMethod
    public void deleteDimMultiPropItemFactor(long id) throws Exception {
        getMdb().deleteRec("DimMultiPropItemFactor", id);
    }

    @DaoMethod
    public Store loadFactors() throws Exception {
        Store st = getMdb().createStore("Factor.select");
        return getMdb().loadQuery(st, """
                    select * from Factor where parent is null
                """);
    }

    //Cls
    @DaoMethod
    public Store loadDimMultiPropItemCls(long dimMultiPropItem) throws Exception {
        Store st = getMdb().createStore("DimMultiPropItemCls.full");
        return getMdb().loadQuery(st, """
                    select d.*, c.name
                    from DimMultiPropItemCls d
                        left join ClsVer c on d.cls=c.ownerVer and c.lastVer=1
                    where d.dimMultiPropItem=:id
                """, Map.of("id", dimMultiPropItem));
    }

    private Store loadDimMultiPropItemClsRec(long id) throws Exception {
        Store st = getMdb().createStore("DimMultiPropItemCls.full");
        return getMdb().loadQuery(st, """
                    select d.*, c.name
                    from DimMultiPropItemCls d
                        left join ClsVer c on d.cls=c.ownerVer and c.lastVer=1
                    where d.id=:id
                """, Map.of("id", id));
    }

    @DaoMethod
    public Store insDimMultiPropItemCls(Map<String, Object> params) throws Exception {
        Store st = getMdb().createStore("DimMultiPropItemCls");
        StoreRecord r = st.add(params);
        long id = getMdb().insertRec("DimMultiPropItemCls", r, true);
        return loadDimMultiPropItemClsRec(id);
    }

    @DaoMethod
    public Store updDimMultiPropItemCls(Map<String, Object> params) throws Exception {
        Store st = getMdb().createStore("DimMultiPropItemCls");
        StoreRecord r = st.add(params);
        long id = r.getLong("id");
        getMdb().updateRec("DimMultiPropItemCls", r);
        return loadDimMultiPropItemCls(id);
    }

    @DaoMethod
    public void deleteDimMultiPropItemCls(long id) throws Exception {
        getMdb().deleteRec("DimMultiPropItemCls", id);
    }

    //RelCls
    @DaoMethod
    public Store loadDimMultiPropItemRelCls(long dimMultiPropItem) throws Exception {
        Store st = getMdb().createStore("DimMultiPropItemRelCls.full");
        return getMdb().loadQuery(st, """
                    select d.*, c.name
                    from DimMultiPropItemRelCls d
                        left join RelClsVer c on d.relCls=c.ownerVer and c.lastVer=1
                    where d.dimMultiPropItem=:id
                """, Map.of("id", dimMultiPropItem));
    }

    private Store loadDimMultiPropItemRelClsRec(long id) throws Exception {
        Store st = getMdb().createStore("DimMultiPropItemRelCls.full");
        return getMdb().loadQuery(st, """
                    select d.*, c.name
                    from DimMultiPropItemRelCls d
                        left join RelClsVer c on d.relCls=c.ownerVer and c.lastVer=1
                    where d.id=:id
                """, Map.of("id", id));
    }

    @DaoMethod
    public Store insDimMultiPropItemRelCls(Map<String, Object> params) throws Exception {
        Store st = getMdb().createStore("DimMultiPropItemRelCls");
        StoreRecord r = st.add(params);
        long id = getMdb().insertRec("DimMultiPropItemRelCls", r, true);
        return loadDimMultiPropItemRelClsRec(id);
    }

    @DaoMethod
    public Store updDimMultiPropItemRelCls(Map<String, Object> params) throws Exception {
        Store st = getMdb().createStore("DimMultiPropItemRelCls");
        StoreRecord r = st.add(params);
        long id = r.getLong("id");
        getMdb().updateRec("DimMultiPropItemRelCls", r);
        return loadDimMultiPropItemRelClsRec(id);
    }

    @DaoMethod
    public void deleteDimMultiPropItemRelCls(long id) throws Exception {
        getMdb().deleteRec("DimMultiPropItemRelCls", id);
    }

    //Measure
    @DaoMethod
    public Store loadDimMultiPropItemMeasure(long dimMultiPropItem) throws Exception {
        Store st = getMdb().createStore("DimMultiPropItemMeasure.full");
        return getMdb().loadQuery(st, """
                    select d.*, c.name
                    from DimMultiPropItemMeasure d
                        left join Measure c on c.parent is null and d.measure=c.id
                    where d.dimMultiPropItem=:id
                """, Map.of("id", dimMultiPropItem));
    }

    private Store loadDimMultiPropItemMeasureRec(long id) throws Exception {
        Store st = getMdb().createStore("DimMultiPropItemMeasure.full");
        return getMdb().loadQuery(st, """
                    select d.*, c.name
                    from DimMultiPropItemMeasure d
                        left join Measure c on c.parent is null and d.measure=c.id
                    where d.id=:id
                """, Map.of("id", id));
    }

    @DaoMethod
    public Store insDimMultiPropItemMeasure(Map<String, Object> params) throws Exception {
        Store st = getMdb().createStore("DimMultiPropItemMeasure");
        StoreRecord r = st.add(params);
        long id = getMdb().insertRec("DimMultiPropItemMeasure", r, true);
        return loadDimMultiPropItemMeasureRec(id);
    }

    @DaoMethod
    public Store updDimMultiPropItemMeasure(Map<String, Object> params) throws Exception {
        Store st = getMdb().createStore("DimMultiPropItemMeasure");
        StoreRecord r = st.add(params);
        long id = r.getLong("id");
        getMdb().updateRec("DimMultiPropItemMeasure", r);
        return loadDimMultiPropItemMeasureRec(id);
    }

    @DaoMethod
    public void deleteDimMultiPropItemMeasure(long id) throws Exception {
        getMdb().deleteRec("DimMultiPropItemMeasure", id);
    }

    @DaoMethod
    public Store loadDimMultiPropMoreCols(long dimMultiProp) throws Exception {
        return getMdb().loadQuery("""
                    select id, title from DimMultiPropName where dimMultiProp=:dim order by ord
                """, Map.of("dim", dimMultiProp));
    }

    @DaoMethod
    public Store insertMoreCols(Map<String, Object> params) throws Exception {
        long id = getMdb().insertRec("DimMultiPropName", params, true);
        getMdb().execQuery("""
                    update DimMultiPropName set ord=:id where id=:id
                """, Map.of("id", id));
        Store st = getMdb().createStore("DimMultiPropName");
        getMdb().loadQuery(st, "select id, title from DimMultiPropName where id=:id", Map.of("id", id));
        return st;
    }

    @DaoMethod
    public Store updateMoreCols(Map<String, Object> params) throws Exception {
        long id = UtCnv.toLong(params.get("id"));
        getMdb().updateRec("DimMultiPropName", params);
        Store st = getMdb().createStore("DimMultiPropName");
        getMdb().loadQuery(st, "select id, title from DimMultiPropName where id=:id", Map.of("id", id));
        return st;
    }

    @DaoMethod
    public void deleteMoreCols(long id) throws Exception {
        try {
            getMdb().execQuery("""
                        delete from DimMultiPropItemName where dimMultiPropName=:id;
                    """, Map.of("id", id));
        } finally {
            getMdb().deleteRec("DimMultiPropName", id);
        }

    }

    @DaoMethod
    public void changeOrdMoreCols(Map<String, Object> params) throws Exception {
        Map<String, Object> rec = UtCnv.toMap(params.get("rec"));
        boolean up = UtCnv.toBoolean(params.get("up"));
        long dmp = UtCnv.toLong(params.get("dimMultiProp"));
        long id1 = UtCnv.toLong(rec.get("id"));
        long ord1 = UtCnv.toLong(rec.get("ord"));
        long id2;
        long ord2;

        Store st = getMdb().loadQuery("""
                    select * from DimMultiPropName where dimMultiProp=:dmp order by ord
                """, Map.of("dmp", dmp));
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
                    update DimMultiPropName set ord=:ord2 where id=:id1;
                    update DimMultiPropName set ord=:ord1 where id=:id2;
                """, Map.of("id1", id1, "id2", id2, "ord1", ord1, "ord2", ord2));
    }

    @DaoMethod
    public Store loadPropForMultiPropItem() throws Exception {
        return getMdb().loadQuery("""
                    select id, name, parent, 0 as prop
                    from PropGr
                    where id in (
                        select distinct propgr
                        from prop
                        where proptype in (1,5,6)
                    )
                    union all
                    select id, name, propgr as parent, id as prop
                    from Prop
                    where propGr in (
                        select id
                        from PropGr
                        where id in (
                            select distinct propgr
                            from prop
                            where prop.proptype in (1,5,6)
                            )
                    ) and proptype in (1,5,6)
                """);
    }

}

