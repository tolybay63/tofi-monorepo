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
    Store loadCalc(String codTyp) {
        Set<Object> setCls = apiMeta().get(ApiMeta).setIdsOfCls(codTyp)
        if (setCls.size() == 0) {
            throw new XError("Не найден код [Typ_Stock]")
        }
        Store st = apiMeta().get(ApiMeta).loadSql("""
            select -c.id as id, null as parent, v.name, c.id as cls, true as iscls,
            case when c.id=${setCls[0]} then 1 else 2 end as ind
            from Cls c, ClsVer v
            where c.id=v.ownerVer and v.lastVer=1 and c.id in (0${setCls.join(",")})
        """, "")
        Store stObj = mdb.loadQuery("""
            select o.id, 
                case when v.objParent is null then -o.cls else v.objParent end as parent,
                v.name, o.cls as cls, false as iscls, 
                case when o.cls=${setCls[0]} then 1 else 2 end as ind
            from Obj o, ObjVer v
            where o.id=v.ownerVer and v.lastVer=1 and o.cls in (0${setCls.join(",")})
        """)
        st.add(stObj)
        return st
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
