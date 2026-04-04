package tofi.mdl.model.dao.measure;

import jandcode.commons.UtCnv;
import jandcode.core.auth.AuthService;
import jandcode.core.auth.AuthUser;
import jandcode.core.dao.DaoMethod;
import jandcode.core.dbm.mdb.BaseMdbUtils;
import jandcode.core.store.Store;
import tofi.mdl.consts.FD_PropType_consts;
import tofi.mdl.model.utils.EntityMdbUtils;

import java.util.Map;


public class MeasureMdbUtils extends BaseMdbUtils {

    @DaoMethod
    public Store load(Map<String, Object> params) throws Exception {
        long measureBase = UtCnv.toLong(params.get("measureBase"));
        long meter = UtCnv.toLong(params.get("meter"));

        String whe = "0=0";
        if (measureBase > 0) {
            whe = "id=" + measureBase + " or parent=" + measureBase;
        } else {
            if (meter > 0) {
                measureBase = getMdb().loadQuery("select measure from Meter where id=:id ",
                        Map.of("id", meter)).get(0).getLong("measure");
                whe = "id=" + measureBase + " or parent=" + measureBase;
            }
        }
        //
        AuthService authSvc = getMdb().getApp().bean(AuthService.class);
        AuthUser au = authSvc.getCurrentUser();
        //todo AuthUser
        long al = 10; //au.getAttrs().getLong("accesslevel");
        whe += " and accessLevel <= " + al;
        //
        Store st = getMdb().createStore("Measure");
        String sql = "select * from Measure where " + whe;
        getMdb().loadQuery(st, sql);
        getMdb().resolveDicts(st);
        //
        return st;
    }

    @DaoMethod
    public Store loadBase(Map<String, Object> params) throws Exception {
        AuthService authSvc = getMdb().getApp().bean(AuthService.class);
        AuthUser au = authSvc.getCurrentUser();
        //todo AuthUser
        long al = 10; //au.getAttrs().getLong("accesslevel");

        Store st = getMdb().createStore("Measure");
        String sql = "select id, name from Measure where parent is null and accessLevel <= " + al;
        getMdb().loadQuery(st, sql);
        //
        return st;
    }

    protected Store loadRec(long id) throws Exception {
        Store st = getMdb().createStore("Measure");
        return getMdb().loadQuery(st, """
                    select * from Measure where id=:id
                """, Map.of("id", id));
    }

    @DaoMethod
    public Store insert(Map<String, Object> params) throws Exception {
        Map<String, Object> rec = UtCnv.toMap(params.get("rec"));
        //
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), "Measure");
        long id = eu.insertEntity(rec);

        //add to PropVal
        Store rTmp = getMdb().loadQuery("select id, allItem from Prop where measure=:m and proptype=:pt",
                Map.of("m", id, "pt", FD_PropType_consts.measure));
        if (rTmp.size() > 0) {
            if (rTmp.get(0).getBoolean("allItem")) {
                long prop = rTmp.get(0).getLong("id");
                getMdb().insertRec("PropVal", Map.of("prop", prop, "measure", id), true);
            }
        }
        return loadRec(id);
    }

    @DaoMethod
    public Store update(Map<String, Object> params) throws Exception {
        Map<String, Object> rec = UtCnv.toMap(params.get("rec"));
        //
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), "Measure");
        eu.updateEntity(rec);
        //
        return loadRec(UtCnv.toLong(rec.get("id")));
    }

    @DaoMethod
    public void delete(Map<String, Object> rec) throws Exception {
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), "Measure");
        eu.deleteEntity(rec);
    }


}
