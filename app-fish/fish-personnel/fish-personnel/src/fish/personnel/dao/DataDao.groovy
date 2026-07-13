package fish.personnel.dao

import groovy.transform.CompileStatic
import jandcode.commons.UtCnv
import jandcode.commons.error.XError
import jandcode.core.auth.AuthService
import jandcode.core.dao.DaoMethod
import jandcode.core.dbm.mdb.BaseMdbUtils
import jandcode.core.dbm.sql.SqlText
import jandcode.core.store.Store
import tofi.api.mdl.ApiMeta
import tofi.apinator.ApinatorApi
import tofi.apinator.ApinatorService

@CompileStatic
class DataDao extends BaseMdbUtils {

    ApinatorApi apiMeta() { return app.bean(ApinatorService).getApi("meta") }
    //-----------------------------------------------------------------------------------------------//

    @DaoMethod
    Map<String, Object> loadPersonnel(Map<String, Object> params) throws Exception {
        //checkTarget("adm:role");
        String filter = UtCnv.toString(params.get("filter")).trim();
        Map<String, Long> mapProp = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "", "Prop_User%")
        Map<String, Long> mapCls = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", "Cls_Personnel", "")
        mapProp.putAll(mapCls)
        //count
        String sql = """
            select count(*) as cnt 
            from Obj o
                join DataProp d1 on d1.isObj=1 and d1.objOrRelobj=o.id and d1.prop=:Prop_UserSecondName
                join DataProp d2 on d2.isObj=1 and d2.objOrRelobj=o.id and d2.prop=:Prop_UserFirstName
                left join DataProp d3 on d3.isObj=1 and d3.objOrRelobj=o.id and d3.prop=:Prop_UserMiddleName
                join DataProp d4 on d4.isObj=1 and d4.objOrRelobj=o.id and d4.prop=:Prop_UserSex
                join DataProp d5 on d5.isObj=1 and d5.objOrRelobj=o.id and d5.prop=:Prop_UserPosition
                join DataProp d6 on d6.isObj=1 and d6.objOrRelobj=o.id and d6.prop=:Prop_UserOrg 
            where o.cls=:Cls_Personnel
        """
        SqlText sqlText = getMdb().createSqlText(sql)
        sqlText.setSql(sql)
        if (!filter.isEmpty())
            sqlText = sqlText.addWhere("""
                v1.strVal like '%" + filter + "%' or v2.strVal like '%" + filter + "%' or v3.strVal like '%" + filter + "%' or
                v5.strVal like '%" + filter + "%' or v6.strVal like '%" + filter + "%'
            """)
        int total = mdb.loadQuery(sqlText as String, mapProp as Map<String, Object>).get(0).getInt("cnt")
        //

        sql = """
            select
                o.id as own, o.cls, 
                v1.strVal as UserSecondName, v1.id as idUserSecondName,  
                v2.strVal as UserFirstName, v2.id as idUserFirstName,
                v3.strVal as UserMiddleName, v3.id as idUserMiddleName,
                v4.id as idUserSex, v4.propVal as pvUserSex, null as fvUserSex, null as nameUserSex,
                v5.id as idUserPosition, v5.propVal as pvUserPosition, null as fvUserPosition, null as nameUserPosition,
                v6.id as idUserOrg, v6.obj as objUserOrg, ov6.name as nameUserOrg
            from Obj o
                join DataProp d1 on d1.isObj=1 and d1.objOrRelobj=o.id and d1.prop=:Prop_UserSecondName
                join DataPropVal v1 on d1.id=v1.dataProp
                join DataProp d2 on d2.isObj=1 and d2.objOrRelobj=o.id and d2.prop=:Prop_UserFirstName
                join DataPropVal v2 on d2.id=v2.dataProp
                left join DataProp d3 on d3.isObj=1 and d3.objOrRelobj=o.id and d3.prop=:Prop_UserMiddleName
                join DataPropVal v3 on d3.id=v3.dataProp
                join DataProp d4 on d4.isObj=1 and d4.objOrRelobj=o.id and d4.prop=:Prop_UserSex
                join DataPropVal v4 on d4.id=v4.dataProp
                join DataProp d5 on d5.isObj=1 and d5.objOrRelobj=o.id and d5.prop=:Prop_UserPosition
                join DataPropVal v5 on d5.id=v5.dataProp
                join DataProp d6 on d6.isObj=1 and d6.objOrRelobj=o.id and d6.prop=:Prop_UserOrg
                join DataPropVal v6 on d6.id=v6.dataProp
                join ObjVer ov6 on ov6.ownerVer=v6.obj and ov6.lastVer=1 
            where o.cls=:ClsPersonnel
            order by v1.strVal
        """;
        sqlText = getMdb().createSqlText(sql);
        Map<String, Object> par = new HashMap<>();
        int pg = UtCnv.toInt(params.get("page"));
        int limit = UtCnv.toInt(params.get("limit"));
        limit = limit==0 ? total : limit;
        int offset = (pg - 1) * limit;
        par.put("offset", offset);
        par.put("limit", limit);
        sqlText.setSql(sql);
        sqlText.paginate(true);

        if (!UtCnv.toString(params.get("orderBy")).trim().isEmpty())
            sqlText = sqlText.replaceOrderBy(UtCnv.toString(params.get("orderBy")));


        if (!filter.isEmpty())
            sqlText = sqlText.addWhere("""
                v1.strVal like '%" + filter + "%' or v2.strVal like '%" + filter + "%' or v3.strVal like '%" + filter + "%' or
                v5.strVal like '%" + filter + "%' or v6.strVal like '%" + filter + "%'
            """)

        Store st = getMdb().createStore("Personnel");
        mdb.loadQuery(st, sqlText as String, mapProp as Map<String, Object>)

        Map<String, Object> meta = new HashMap<>()

        meta.put("total", total)
        meta.put("page", pg)
        meta.put("limit", limit)

        Map<String, Object> mapRes = new HashMap<>()
        mapRes.put("store", st)
        mapRes.put("meta", meta)
        return mapRes
    }



//-----------------------------------------------------------------------------------------------//
    private Store loadSqlMeta(String sql, String domain) {
        return apiMeta().get(ApiMeta).loadSql(sql, domain)
    }

    private Store loadSqlMetaWithParams(String sql, String domain, Map<String, Object> params) {
        return apiMeta().get(ApiMeta).loadSqlWithParams(sql, domain, params)
    }

    private long getUser() throws Exception {
        AuthService authSvc = mdb.getApp().bean(AuthService.class)
        long au = authSvc.getCurrentUser().getAttrs().getLong("id")
        if (au == 0)
            throw new XError("notLoginned")
        return au
    }


}
