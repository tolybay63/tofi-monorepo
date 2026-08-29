package fish.calc.dao

import groovy.transform.CompileStatic
import jandcode.commons.UtCnv
import jandcode.commons.error.XError
import jandcode.core.auth.AuthService
import jandcode.core.dao.DaoMethod
import jandcode.core.dbm.mdb.BaseMdbUtils
import jandcode.core.store.Store
import jandcode.core.store.StoreRecord
import tofi.api.dta.model.utils.EntityMdbUtils
import tofi.api.mdl.ApiMeta
import tofi.apinator.ApinatorApi
import tofi.apinator.ApinatorService

@CompileStatic
class DataDao extends BaseMdbUtils {

    ApinatorApi apiMeta() { return app.bean(ApinatorService).getApi("meta") }
    //-----------------------------------------------------------------------------------------------//
    @DaoMethod
    long getCls(String codCls) {
        Store st = apiMeta().get(ApiMeta).loadSql("""
            select id from Cls where cod like '${codCls}'
        """, "")
        if (st.size() == 0)
            throw new XError("Не найден код класса [Cls_CalcDeterm/Cls_CalcBayes]")
        return st.get(0).getLong("id")
    }

    @DaoMethod
    Store loadCalc(long id) {
        return mdb.loadQuery("""
            select o.id, 
                case when v.objParent is null then -o.cls else v.objParent end as parent,
                v.name, o.cls as cls 
            from Obj o, ObjVer v
            where o.id=v.ownerVer and v.lastVer=1 and o.cls=${id}
        """)
    }

    @DaoMethod
    void insertCalc(Map<String, Object> rec) throws Exception {
        //checkTarget("adm:tml:ins")
        StoreRecord r = mdb.createStoreRecord("Obj.full", rec)

        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        eu.insertEntity(rec)
    }

    @DaoMethod
    void updateCalc(Map<String, Object> rec) {
        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        long id = UtCnv.toLong(rec.get("id"))
        eu.updateEntity(rec)
    }

    @DaoMethod
    void deleteCalc(long id) {
        deleteOwnerWithProperties(id, 1)
    }

    private void deleteOwnerWithProperties(long id, int isObj) {
        String tableName = isObj == 1 ? "Obj" : "RelObj"
        //
        //checkForExistData(id, isObj)
        //
        EntityMdbUtils eu = new EntityMdbUtils(mdb, tableName)
        mdb.execQueryNative("""
            delete from DataPropVal
            where dataProp in (select id from DataProp where isobj=${isObj} and objorrelobj=${id});
            delete from DataProp where id in (
                select id from dataprop
                except
                select dataProp as id from DataPropVal
            );
        """)
        if (tableName.equalsIgnoreCase("RelObj")) {
            try {
                mdb.execQueryNative("""
                    delete from RelObjMember
                    where relobj=${id};
                """)
            } finally {
                eu.deleteEntity(id)
            }
        } else
            eu.deleteEntity(id)
    }



/*
    private StoreRecord loadObjRec(long obj) {
        StoreRecord st = mdb.createStoreRecord("Obj.full")
        mdb.loadQueryRecord(st, """
            select o.*, v.name, v.fullName, v.objParent as parent from Obj o, ObjVer v
            where o.id=v.ownerVer and v.lastVer=1 and o.id=:o
        """, [o: obj])
        return st
    }
*/

    private Store loadSqlMeta(String sql, String domain) {
        return apiMeta().get(ApiMeta).loadSql(sql, domain)
    }

    private long getUser() throws Exception {
        AuthService authSvc = mdb.getApp().bean(AuthService.class)
        long au = authSvc.getCurrentUser().getAttrs().getLong("id")
        if (au == 0)
            throw new XError("notLoginned")
        return au
    }


}
