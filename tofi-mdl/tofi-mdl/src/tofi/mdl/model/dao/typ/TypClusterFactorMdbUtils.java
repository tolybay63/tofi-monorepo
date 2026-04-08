package tofi.mdl.model.dao.typ;

import jandcode.commons.UtCnv;
import jandcode.commons.error.XError;
import jandcode.core.dao.DaoMethod;
import jandcode.core.dbm.mdb.BaseMdbUtils;
import jandcode.core.store.Store;
import jandcode.core.store.StoreRecord;

import java.util.Map;


public class TypClusterFactorMdbUtils extends BaseMdbUtils {

    @DaoMethod
    public Store loadTypClusterFactor(long typ) throws Exception {
        Store st = getMdb().createStore("TypClusterFactor.full");
        TypMdbUtils typDao = getMdb().createDao(TypMdbUtils.class);
        long typParent = typDao.loadRec(typ).getLong("parent");

        getMdb().loadQuery(st, """
                  select * from (
                      select t.*, f.name, f.fullName, case when t.typ=:t then 1 else 0 end as isOwn, f.ord
                      from TypClusterFactor t, Factor f
                      where t.factor=f.id and (t.typ=:t or t.typ=:tp)
                  ) a
                  order by isOwn desc, ord
                """, Map.of("t", typ, "tp", typParent));

        return st;
    }

    @DaoMethod
    public Store insertTypClusterFactor(Map<String, Object> params) throws Exception {
        Map<String, Object> rec = UtCnv.toMap(params.get("rec"));
        Store st = getMdb().createStore("TypClusterFactor");
        StoreRecord r = st.add(rec);
        //
        long id = getMdb().insertRec("TypClusterFactor", r, true);
        //
        st = getMdb().createStore("TypClusterFactor.full");
        getMdb().loadQuery(st, """
                  select t.*, f.name, f.fullName, 1 as isOwn from TypClusterFactor t, Factor f
                  where t.factor=f.id and t.id=:id
                """, Map.of("id", id));
        return st;
    }

    @DaoMethod
    public Store updateTypClusterFactor(Map<String, Object> params) throws Exception {
        Map<String, Object> rec = UtCnv.toMap(params.get("rec"));
        long id = UtCnv.toLong(rec.get("id"));
        Store st = getMdb().createStore("TypClusterFactor");
        StoreRecord r = st.add(rec);
        //
        getMdb().updateRec("TypClusterFactor", r);

        //
        st = getMdb().createStore("TypClusterFactor.full");
        getMdb().loadQuery(st, """
                  select t.*, f.name, f.fullName, 1 as isOwn from TypClusterFactor t, Factor f
                  where t.factor=f.id and t.id=:id
                """, Map.of("id", id));
        return st;
    }

    @DaoMethod
    public void deleteTypClusterFactor(Map<String, Object> rec) throws Exception {
        long id = UtCnv.toLong(rec.get("id"));
        Store stTmp = getMdb().loadQuery("""
            select t.id,
            STRING_AGG (cast(v.name as varchar(1000)), ', ' order by v.id) as nms
            from cls c
            left join clsver v on c.id=v.ownerver and v.lastver=1
            left join typclusterfactor t on t.typ=c.typ
            where t.id=:id
            group by t.id
        """,  Map.of("id", id));
        if (stTmp.size() > 0) {
            throw new XError("clsExists");
            //throw new XError("Существует(ют) класс(ы) {0}", stTmp.get(0).getString("nms"));
        }


        getMdb().deleteRec("TypClusterFactor", id);
    }

    @DaoMethod
    public Store loadFactors(long typ, String mode) throws Exception {
        if (mode.equals("ins"))
            return getMdb().loadQuery("""
                        select id, name from Factor where parent is null
                            and id not in (
                            select factor from TypClusterFactor where typ=:typ
                        )
                        order by ord
                    """, Map.of("typ", typ));
        else
            return getMdb().loadQuery("""
                        select id, name from Factor where parent is null order by ord
                    """, Map.of("typ", typ));

    }

}
