package tofi.mdl.model.dao.factor;

import jandcode.commons.UtCnv;
import jandcode.commons.UtString;
import jandcode.core.dbm.mdb.Mdb;
import jandcode.core.store.Store;
import jandcode.core.store.StoreField;
import jandcode.core.store.StoreRecord;

import java.util.*;

public class FactorRelMdbUtils {
    Mdb mdb;

    FactorRelMdbUtils(Mdb mdb) throws Exception {
        this.mdb = mdb;
    }

    /**
     * Загрузка зависимых факторов для указанного фактора
     *
     * @param params factor
     * @return
     * @throws Exception
     */
    public Store load(Map<String, Object> params) throws Exception {
        String sql = """
                    with fvs1 as (
                        select id from factor where parent=:factor
                    ),
                    r as (
                        select distinct f2.parent as f2
                        from FactorValRel r, factor f1, factor f2
                        where
                            (r.factor1 in (select id from fvs1) and r.factor1=f1.id and r.factor2=f2.id )
                            or
                            (r.factor2 in (select id from fvs1) and r.factor1=f2.id and r.factor2=f1.id )
                    )
                    select f.*
                    from r
                    left join factor f on r.f2=f.id
                """;
        Store st = mdb.createStore("Factor");

        return mdb.loadQuery(st, sql, params);
    }

    /**
     * Выбор зависимого фактора, для комбобокса
     *
     * @param params
     * @return
     * @throws Exception
     */
    public Store factor2(Map<String, Object> params) throws Exception {
        return mdb.loadQuery("""
                    with fvs1 as (
                        select id from factor where parent=:factor
                    ),
                    r as (
                        select distinct f2.parent as f2
                        from FactorValRel r, factor f1, factor f2
                        where
                            (r.factor1 in (select id from fvs1) and r.factor1=f1.id and r.factor2=f2.id )
                            or
                            (r.factor2 in (select id from fvs1) and r.factor1=f2.id and r.factor2=f1.id )
                    ),
                    zav as (
                        select f.id
                        from r
                        left join factor f on r.f2=f.id
                    )
                    select id, name from factor where parent is null
                        and id<>:factor and id not in (select id from zav)
                """, params);
    }

    public Map<String, Object> factorValRel(Map<String, Object> params) throws Exception {
        /*
        Установка зависимости двух факторов factor1 и factor2. В начале все пары значений не совместные.
        В базу записываются несовместные пары.
        */

        Store stFv2 = mdb.loadQuery("select id, name from factor where parent=:factor2 order by ord", params);

        List<Map<String, Object>> cols = new ArrayList<>();
        cols.add(Map.of("name", "name", "label", "factor1/factor2", "field", "name",
                "align", "left", "classes", "bg-blue-grey-1", "headerStyle", "font-size: 1.3em", "style", "width: 30%"));

        Store stFv1 = mdb.createStore();
        stFv1.addField("id", "long");
        stFv1.addField("name", "string", 200);

        List<String> sel = new ArrayList<>();
        String sep = "";
        for (StoreRecord r : stFv2) {
            for (StoreField f : r.getFields()) {
                if (f.getName().equalsIgnoreCase("id")) {
                    stFv1.addField("fv" + r.getString(f.getName()), "string", 20);
                    sel.add(0 + " as " + "fv" + r.getString(f.getName()));
                }
            }
            sep = (!sel.isEmpty()) ? ", " : "";
            cols.add(Map.of("name", "fv" + r.getValue("id"),
                    "label", UtCnv.toString(r.getValue("name")), "field", "fv" + r.getValue("id"),
                    "align", "center", "classes", "bg-blue-grey-1", "headerStyle", "font-size: 1.2em",
                    "style", "width: 10%"));
        }

        String sql = "select id, name as name" + sep + String.join(",", sel) + " from factor where parent=:factor1 order by ord";

        mdb.loadQuery(stFv1, sql, params);

        Store stFvRel = mdb.loadQuery("""
                            select r.id,
                            case when f1.parent = :factor1 then r.factor1 else r.factor2 end as factor1,
                            case when f1.parent = :factor1 then r.factor2 else r.factor1 end as factor2,
                            r.cmt
                            from factorvalrel r, factor f1, factor f2
                            where r.factor1=f1.id and r.factor2=f2.id and (
                                r.factor1 in (select id from factor where parent=:factor1) and r.factor2 in (select id from factor where parent=:factor2)
                                or
                                r.factor1 in (select id from factor where parent=:factor2) and r.factor2 in (select id from factor where parent=:factor1)
                            )
                        """
                , params);

        //Проставляем значения из базы
        Map<String, String> cmt = new HashMap<>();
        for (StoreRecord r : stFv1) {
            List<StoreRecord> lst =
                    stFvRel.getRecords().stream().filter(rec ->
                            UtCnv.toString(rec.getValue("factor1"))
                                    .equals(r.getString("id"))).toList();
            for (StoreRecord rr : lst) {
                String fld = "fv" + rr.getValue("factor2");
                r.set(fld, rr.getLong("factor2"));
                cmt.put(r.getString("id") + "_" + rr.getString("factor2"), rr.getString("cmt"));
            }
        }
        Map<String, Object> rez = new HashMap<>();
        rez.put("rows", stFv1);
        rez.put("cols", cols);
        rez.put("cmt", cmt);

/*
        mdb.outTable(stFv1);
        for (Map m : cols)
            mdb.outMap(m);
        mdb.outMap(cmt);
*/

        return rez;
    }

    public Store saveFactorValRel(Map<String, Object> factors, Map<String, Object> data) throws Exception {
        //Deleting old values
        Store st = mdb.loadQuery("""
                    select r.id
                    from factorvalrel r, factor f1, factor f2
                    where r.factor1=f1.id and r.factor2=f2.id and (
                        r.factor1 in (select id from factor where parent=:factor1) and
                            r.factor2 in (select id from factor where parent=:factor2)
                        or
                        r.factor1 in (select id from factor where parent=:factor2) and
                            r.factor2 in (select id from factor where parent=:factor1)
                    )
                """, factors);
        Set<Object> ids = st.getUniqueValues("id");
        String whe = UtString.join(ids, ",");

        if (!whe.isEmpty())
            mdb.execQuery("delete from factorvalrel where id in (" + whe + ")");
        //
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String[] f1f2 = UtCnv.toString(entry.getKey()).split("_");
            String cmt = UtCnv.toString(entry.getValue());
            long factor1 = UtCnv.toLong(f1f2[0]);
            long factor2 = UtCnv.toLong(f1f2[1]);
            long id = mdb.getNextId("factorvalrel");
            Map<String, Object> rec = new HashMap<>();
            rec.put("id", id);
            rec.put("factor1", factor1);
            rec.put("factor2", factor2);
            if (!cmt.isEmpty()) {
                rec.put("cmt", cmt);
            }
            mdb.insertRec("factorvalrel", rec, false);
        }
        //
        String sql = """
                    with fvs1 as (
                        select id from factor where parent=:factor1
                    ),
                    r as (
                        select distinct f2.parent as f2
                        from FactorValRel r, factor f1, factor f2
                        where
                            (r.factor1 in (select id from fvs1) and r.factor1=f1.id and r.factor2=f2.id )
                            or
                            (r.factor2 in (select id from fvs1) and r.factor1=f2.id and r.factor2=f1.id )
                    )
                    select f.*
                    from r
                    left join factor f on r.f2=f.id
                    where f.id=:factor2
                """;
        Store rez = mdb.loadQuery(sql, factors);
        mdb.resolveDicts(rez);
        if (rez.size() > 0) {
            return rez;
        } else {
            return mdb.createStore();
        }
    }
    //


}

