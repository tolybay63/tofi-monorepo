package tofi.mdl.model.dao.typ;

import jandcode.commons.UtCnv;
import jandcode.commons.UtString;
import jandcode.commons.error.XError;
import jandcode.core.dao.DaoMethod;
import jandcode.core.dbm.mdb.BaseMdbUtils;
import jandcode.core.std.CfgService;
import jandcode.core.store.Store;
import jandcode.core.store.StoreRecord;
import tofi.api.dta.*;
import tofi.apinator.ApinatorApi;
import tofi.apinator.ApinatorService;
import tofi.mdl.consts.FD_PropType_consts;
import tofi.mdl.model.utils.EntityMdbUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ClsMdbUtils extends BaseMdbUtils {
    ApinatorApi apiUserData() { return getMdb().getApp().bean(ApinatorService.class).getApi("userdata"); }
    ApinatorApi apiNSIData() {
        return getMdb().getApp().bean(ApinatorService.class).getApi("nsidata");
    }
    ApinatorApi apiMonitoringData() {
        return getMdb().getApp().bean(ApinatorService.class).getApi("monitoringdata");
    }


    @DaoMethod
    public Store loadCls(long typ) throws Exception {
        Store st = getMdb().createStore("Cls.full");
        return getMdb().loadQuery(st, """
                    select * from Cls c, ClsVer v where c.id=v.ownerVer and v.lastVer=1 and c.typ=:typ
                """, Map.of("typ", typ));
    }

    @DaoMethod
    public StoreRecord loadRecCls(long id) throws Exception {
        StoreRecord st = getMdb().createStoreRecord("Cls.full");
        return getMdb().loadQueryRecord(st, """
                    select * from Cls c, ClsVer v where c.id=v.ownerVer and v.lastVer=1 and c.id=:id
                """, Map.of("id", id));
    }


    @DaoMethod
    public Store loadClsFVforUpd(long typ, long cls) throws Exception {
        Store st = getMdb().createStore("TypClusterFactor.tree");
        TypMdbUtils typDao = getMdb().createDao(TypMdbUtils.class);
        long typParent = typDao.loadRec(typ).getLong("parent");

        getMdb().loadQuery(st, """
                    select a.*, case when c.id > 0 then 1 else 0 end as checked
                    from (
                        select id, name, fullName, cod, cmt, isReq, isUniq, parent, isOwn, ord
                        from
                            (
                                select -tcf.factor as id, f.name, f.fullName, f.cod, f.cmt, tcf.isReq, tcf.isUniq, null::numeric as parent,
                                    case when tcf.typ=:typ then 1 else 0 end as isOwn, f.ord
                                from TypClusterFactor tcf, Factor f
                                where (tcf.typ=:typ or tcf.typ=:typParent) and tcf.factor = f.id
                            ) t
                        union all
                        select id, name, fullName, cod, cmt, isReq, isUniq, parent, isOwn, ord
                        from
                        (
                           select fv.id, fv.name, fv.fullName, fv.cod, fv.cmt, tcf.isReq, tcf.isUniq, -fv.parent as parent,
                               case when tcf.typ=:typ then 1 else 0 end as isOwn, fv.ord
                           from TypClusterFactor tcf, Factor f, Factor fv
                           where (tcf.typ=:typ or tcf.typ=:typParent) and tcf.factor = f.id and f.id=fv.parent
                        ) t
                    ) a left join clsfactorval c on c.factorval=a.id and c.cls=:cls
                order by isown desc, ord
                """, Map.of("typ", typ, "typParent", typParent, "cls", cls));
        return st;
    }

    @DaoMethod
    public Store loadClsFV(long typ, long cls) throws Exception {
        Store st = getMdb().createStore("TypClusterFactor.full");
        getMdb().loadQuery(st, """
                select * from (
                  select distinct id, name, fullName, cod, parent, isOwn, ord
                  from
                      (
                          select -f.id as id, f.name, f.fullName, f.cod, null::numeric as parent,
                              case when f.id in (select factor from TypClusterFactor where typ=:t) then 1 else 0 end as isOwn, f.ord
                          from Factor f, ClsFactorVal cf, factor fv
                          where cf.cls=:c and cf.factorval = fv.id and fv.parent=f.id
                      ) t
                  union all
                  select id, name, fullName, cod, parent, isOwn, ord
                  from
                      (
                          select fv.id, fv.name, fv.fullName, fv.cod, -fv.parent as parent,
                          case when fv.parent in (select factor from TypClusterFactor where typ=:t) then 1 else 0 end as isOwn, fv.ord
                          from
                          Factor fv, ClsFactorVal cf where cf.cls=:c and fv.parent is not null and cf.factorval = fv.id
                      ) t
                ) a
                order by ord
                """, Map.of("t", typ, "c", cls));

        return st;
    }

    @DaoMethod
    public Store insertCls(Map<String, Object> params) throws Exception {

        Map<String, Object> rec = UtCnv.toMap(params.get("rec"));
        long typ = UtCnv.toLong(rec.get("typ"));
        List<String> lstFvs = UtCnv.toList(params.get("ids"));
        // Проверка уаказанных значении классов на повторность
        int cnt = lstFvs.size();
        String fvs = lstFvs.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",", "", ""));

        testForDubleCls(fvs, cnt, 0, typ, "ins");

        //add Cls
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), "Cls");
        long cls = eu.insertEntity(rec);
        //add to PropVal
        Store rTmp = getMdb().loadQuery("select id, allItem from Prop where typ=:typ and proptype=:pt",
                Map.of("typ", typ, "pt", FD_PropType_consts.typ));
        if (rTmp.size() > 0) {
            if (rTmp.get(0).getBoolean("allItem")) {
                long prop = rTmp.get(0).getLong("id");
                getMdb().insertRec("PropVal", Map.of("prop", prop, "cls", cls), true);
            }
        }

        //add ClsFactorVal
        for (String sfv : lstFvs) {
            long f = UtCnv.toLong(sfv);
            getMdb().insertRec("ClsFactorVal", Map.of("cls", cls, "factorVal", f), true);
        }
        //
        Store st = getMdb().createStore("Cls.full");
        getMdb().loadQuery(st, """
                    select * from Cls c, ClsVer v where c.id=v.ownerVer and v.lastver=1 and c.id=:id
                """, Map.of("id", cls));
        //getMdb().resolveDicts(st);
        //
        return st;
    }

    protected void testForDubleCls(String fvs, int cnt, long cls, long typ, String mode) throws Exception {
        if (fvs.isEmpty())
            throw new XError("notClustFactorVal");

        Store ds;
        if (mode.equals("ins")) {
            ds = getMdb().loadQuery("select cls from clsfactorval cl, Cls c where factorval in (" + fvs + ") and " +
                            "cl.cls=c.id  and c.typ=" + typ + " group by cls having count(*)=:cnt and count(*)=(select count(*) from clsfactorval where cls=cl.cls group by cls)",
                    Map.of("fvs", fvs, "cnt", cnt));
        } else {
            ds = getMdb().loadQuery("select cls from clsfactorval cl, Cls c where factorval in (" + fvs + ") and cls<>:cls and " +
                            "cl.cls=c.id and c.typ=" + typ + " group by cls having count(*)=:cnt and count(*)=(select count(*) from clsfactorval where cls=cl.cls group by cls)",
                    Map.of("fvs", fvs, "cls", cls, "cnt", cnt));
        }

        if (ds.size() > 0) {
            Store st = getMdb().loadQuery("""
                        select cod, name from Cls c, ClsVer v where c.id=v.ownerver and v.lastver=1 and c.id=:cls
                    """, Map.of("cls", ds.get(0).getLong("cls")));
            String msg = "Существует класс, образованный от указанных значений кластерных факторов: ";//NLS
            throw new XError(msg + "[" + st.get(0).get("cod") + ", " + st.get(0).get("name") + "]");
        }

    }

    @DaoMethod
    public Store updateCls(Map<String, Object> params) throws Exception {
        Map<String, Object> rec = UtCnv.toMap(params.get("rec"));
        long cls = UtCnv.toLong(rec.get("id"));
        long typ = UtCnv.toLong(rec.get("typ"));
        List<String> fvs = UtCnv.toList(params.get("ids"));
        // Проверка уаказанных значении классов на повторность
        int cnt = fvs.size();
        String whFvs = fvs.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",", "", ""));
        //
        testForDubleCls(whFvs, cnt, cls, typ, "upd");
        //

        //upd Cls
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), "Cls");
        eu.updateEntity(rec);

        Store dsOld = getMdb().loadQuery("select id,factorVal from clsfactorval where cls=:cls", Map.of("cls", cls));
        Set<Object> oldFvs = dsOld.getUniqueValues("factorVal");
        // Deleting
        for (StoreRecord r : dsOld) {
            if (!fvs.contains(r.getString("factorVal"))) {
                System.out.println("del " + r.getString("id"));
                getMdb().execQuery("delete from ClsFactorVal where id=:id", Map.of("id", r.getLong("id")));
            }
        }
        //Adding
        for (String sfv : fvs) {
            long f = UtCnv.toLong(sfv);
            if (!oldFvs.contains(f)) {
                getMdb().insertRec("ClsFactorVal", Map.of("cls", cls, "factorVal", f), true);
            }
        }
        //
        Store st = getMdb().createStore("Cls.full");
        getMdb().loadQuery(st, """
                    select * from Cls c, ClsVer v where c.id=v.ownerVer and v.lastver=1 and c.id=:id
                """, Map.of("id", cls));
        getMdb().resolveDicts(st);
        //
        return st;
    }

    protected void checkExistOwners(long cls, String modelMeta) {
        List<String> lstApp = new ArrayList<>();
        if (modelMeta.equalsIgnoreCase("fish")) {
            //1
            boolean b = apiUserData().get(ApiUserData.class).checkExistOwners(cls, true);
            if (b) lstApp.add("userdata");
            //2
            b = apiNSIData().get(ApiNSIData.class).checkExistOwners(cls, true);
            if (b) lstApp.add("nsidata");
            //3
            b = apiMonitoringData().get(ApiMonitoringData.class).checkExistOwners(cls, true);
            if (b) lstApp.add("monitoringdata");
        }

        if (!lstApp.isEmpty()) {
            throw new XError("ExistObjInApp@" + UtString.join(lstApp, ", "));
        }

    }

    @DaoMethod
    public void deleteCls(Map<String, Object> params) throws Exception {
        Map<String, Object> rec = UtCnv.toMap(params.get("rec"));
        long cls = UtCnv.toLong(rec.get("ent"));

        //---< check data in other DB
        CfgService cfgSvc = getMdb().getApp().bean(CfgService.class);
        String modelMeta = cfgSvc.getConf().getString("dbsource/default/id");
        if (modelMeta.isEmpty())
            throw new XError("Не найден id мета модели");

        checkExistOwners(cls, modelMeta);
        //--->

        // delete ClsFactorVal
        getMdb().execQuery("""
                    delete from ClsFactorVal where cls=:cls
                """, Map.of("cls", cls));

        // delete Cls
        rec.put("id", cls);
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), "Cls");
        eu.deleteEntity(rec);
    }


    @DaoMethod
    public Store loadClsTreeForSelect() throws Exception {
        String sql = """
                    select -t.id as id, v.name, null as parent
                    from Typ t, TypVer v where t.id=v.ownerVer and v.lastVer=1
                    union all
                    select t.id, v.name, -t.typ as parent
                    from Cls t, ClsVer v where t.id=v.ownerVer and v.lastVer=1
                """;
        return getMdb().loadQuery(sql);
    }

    @DaoMethod
    public Store loadClsVer(long cls) throws Exception {
        Store st = getMdb().createStore("ClsVer");
        getMdb().loadQuery(st, """
                    select * from clsver where ownerver=:cls order by dend desc
                """, Map.of("cls", cls));

        getMdb().outTable(st);


        return st;
    }

    @DaoMethod
    public Store insertClsVer(Map<String, Object> rec) throws Exception {
        //
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), "Cls");
        long id = eu.insertEntityVer(rec);
        //
        Store st = getMdb().createStore("ClsVer");
        getMdb().loadQuery(st, "select * from clsver where id=:id", Map.of("id", id));

        return st;
    }

    @DaoMethod
    public Store updateClsVer(Map<String, Object> rec) throws Exception {
        long id = UtCnv.toLong(rec.get("id"));
        if (id == 0) {
            throw new XError("Поле id должно иметь не нулевое значение");
        }
        //
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), "Cls");
        eu.updateEntityVer(rec);
        // Загрузка записи
        Store st = getMdb().createStore("ClsVer");
        getMdb().loadQuery(st, "select * from clsver where id=:id", Map.of("id", id));
        return st;
    }

    @DaoMethod
    public void deleteClsVer(Map<String, Object> rec) throws Exception {
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), "Cls");
        eu.deleteEntityVer(rec);

    }


}
