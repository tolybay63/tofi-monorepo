package fish.monitoring.dao

import jandcode.commons.UtCnv
import jandcode.commons.UtString
import jandcode.core.dao.DaoMethod
import jandcode.core.dbm.mdb.BaseMdbUtils
import jandcode.core.store.Store
import jandcode.core.store.StoreIndex
import jandcode.core.store.StoreRecord
import tofi.api.mdl.ApiMeta
import tofi.apinator.ApinatorApi
import tofi.apinator.ApinatorService

class ChartDao extends BaseMdbUtils {

    ApinatorApi apiMeta() { return app.bean(ApinatorService).getApi("meta") }
    ApinatorApi apiMonitoringData() { return app.bean(ApinatorService).getApi("monitoringdata") }
    ApinatorApi apiPersonnelData() { return app.bean(ApinatorService).getApi("personneldata") }
    //-----------------------------------------------------------------------------------------------//

    @DaoMethod
    Map<String, Object> loadCubeMetaData(long owner, long meter) {
        Map<String, Object> rez = new HashMap<>()

        String name = apiMeta().get(ApiMeta).loadSql("""
            select name from meter where id=${meter}
        """, "").get(0).getString("name")
        rez.put("meterName", name)

        Store stDim = apiMeta().get(ApiMeta).loadSql("""
            select m.factor, f.name 
            from meterfactor m, factor f 
            where meter=${meter} and m.factor=f.id
            order by orddim
        """, "")
        rez.put("dims", stDim)

        Store st = apiMeta().get(ApiMeta).loadSql("""
            select id as value, name as label
            from factor
            where parent=${stDim.get(0).getLong("factor")}
            order by ord
        """, "")
        rez.put("fishtype", st)

        st = apiMeta().get(ApiMeta).loadSql("""
            select id as value, name as label
            from factor
            where parent=${stDim.get(1).getLong("factor")}
            order by ord
        """, "")
        rez.put("age", st)
        st = apiMeta().get(ApiMeta).loadSql("""
            select id as value, name as label
            from factor
            where parent=${stDim.get(2).getLong("factor")}
            order by ord
        """, "")
        rez.put("sex", st)
        return rez
    }

    @DaoMethod
    Store loadCubeData(Map<String, Object> params) {
        def owner = UtCnv.toLong(params.get("owner"))
        def meter = UtCnv.toLong(params.get("meter"))
        String param1Key = params.get("param1Key")
        def param1 = params.get("param1")
        if (param1Key == "fishtype" || param1Key == "age" || param1Key == "sex")
            param1 = UtCnv.toLong(params.get("param1"))

        String param2Key = UtCnv.toString(params.get("param2Key"))
        def param2 = params.get("param2")
        if (param2Key == "fishtype" || param2Key == "age" || param2Key == "sex")
            param2 = UtCnv.toLong(params.get("param2"))

        def xAxisField = params.get("xAxisField")
        def seriesField = params.get("seriesField")

        def periodType = 11L
        def periodDbeg = null
        if (param1Key.contains("year") || param2Key.contains("year")) {
            if (param1Key.contains("year")) periodDbeg = param1
            if (param2Key.contains("year")) periodDbeg = param2
        }
        //
        def factorDims = []
        if (["fishtype", "age", "sex"].contains(param1Key))
            factorDims.add(param1)
        if (["fishtype", "age", "sex"].contains(param2Key))
            factorDims.add(param2)

        def lstFvs = factorDims.join(",")

        def level = 2
        if (factorDims.size()==2)
            level = 3
        else {
            if (["fishtype", "age", "sex"].contains(xAxisField) &&
                ["fishtype", "age", "sex"].contains(seriesField) &&
                    xAxisField != seriesField)
                level=3
        }

        Store stProp = apiMeta().get(ApiMeta).loadSql("""
            select id, t.fishtype, t.fishyear, t.fishsex
            from Prop p
            inner join (
                with gr as (
                    select m.meterrate,
                    string_to_array(STRING_AGG (cast(factorval as varchar(2000)), ',' order by fv.ord), ',') as arrFv,
                    array_length(string_to_array(STRING_AGG (cast(factorval as varchar(2000)), ','), ','), 1) sz,
                    string_to_array(STRING_AGG (cast(fv.name as varchar(2000)), ',' order by fv.ord), ',') as name
                    from prop p, meterratefv m, factor fv
                    where
                        p.meterrate=m.meterrate and 
                        m.factorval=fv.id and
                        p.meter=${meter} and p.meterrate is not null
                    group by m.meterrate
                )
                select meterrate, name[1] as fishtype, name[2] as fishyear, name[3] as fishsex from gr
                where arrFv @> '{${lstFvs}}' and sz=${level}
            ) t on p.meterrate=t.meterrate
        """, "")

        StoreIndex indProp = stProp.getIndex("id")
        //
        Set<Object> idsProp = stProp.getUniqueValues("id")
        if (idsProp.empty) idsProp.add(0L)
        def wheDbeg = periodDbeg ? "and v.dbeg='${periodDbeg}-01-01'" : ""
        Store stData = mdb.loadQuery("""
            select d.prop as prop, substring(v.dbeg::text, 0, 5) as year, null as age, 
                null as fishtype, null as ageord, null as sex, v.numberval as value    
            from DataProp d
            join DataPropVal v on d.id=v.dataProp  
            where d.isObj=1 and d.objorrelobj=${owner} and d.periodType=11 ${wheDbeg}
                and d.prop in (${idsProp.join(",")})
            order by substring(v.dbeg::text, 0, 5)                
        """)

        for (StoreRecord r in stData) {
            StoreRecord rec = indProp.get(r.getLong("prop"))
            if (rec != null) {
                r.set("fishType", rec.getString("fishType"))
                r.set("age", rec.getString("fishyear"))
                r.set("sex", rec.getString("fishsex"))
                String ord = UtString.padLeft(rec.getString("fishyear").split(" ")[0], 2, "0")
                r.set("ageord", ord)
            }
        }
        stData.sort("year, ageord")

        //mdb.outTable(stData)

        return stData
    }

}
