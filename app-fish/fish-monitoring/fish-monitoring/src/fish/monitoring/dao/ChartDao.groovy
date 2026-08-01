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
        String name = mdb.loadQuery("""
            select name from ObjVer where ownerVer=${owner} and lastVer=1
        """).get(0).getString("name")
        rez.put("ownerName", name)

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
        """, "")
        rez.put("fishType", st)

        st = apiMeta().get(ApiMeta).loadSql("""
            select id as value, name as label
            from factor
            where parent=${stDim.get(1).getLong("factor")}
        """, "")
        rez.put("age", st)
        st = apiMeta().get(ApiMeta).loadSql("""
            select id as value, name as label
            from factor
            where parent=${stDim.get(2).getLong("factor")}
        """, "")
        rez.put("sex", st)
        return rez
    }

    @DaoMethod
    Store loadCubeData(Map<String, Object> params) {
        long owner = UtCnv.toLong(params.get("owner"))
        long meter = UtCnv.toLong(params.get("meter"))
        long param1 = UtCnv.toLong(params.get("param1"))

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
                select meterrate, name[1] as fishtype, name[2] as fishyear, coalesce(name[3], '') as fishsex  from gr
                where arrFv @> '{${param1}}' and sz>=2
            ) t on p.meterrate=t.meterrate
        """, "")

        StoreIndex indProp = stProp.getIndex("id")
        //
        Set<Object> idsProp = stProp.getUniqueValues("id")
        if (idsProp.empty) idsProp.add(0L)
        Store stData = mdb.loadQuery("""
            select d.prop as prop, substring(v.dbeg::text, 0, 5) as year, null as age, null as ageord, 
                null as sex, v.numberval as value    
            from DataProp d
            join DataPropVal v on d.id=v.dataProp  
            where d.isObj=1 and d.objorrelobj=${owner} and d.periodType=11 --and v.dbeg='2015-01-01'
                and d.prop in (${idsProp.join(",")})
            order by substring(v.dbeg::text, 0, 5)                
        """)

        for (StoreRecord r in stData) {
            StoreRecord rec = indProp.get(r.getLong("prop"))
            if (rec != null) {
                r.set("age", rec.getString("fishyear"))
                r.set("sex", rec.getString("fishsex"))
                String ord = UtString.padLeft(rec.getString("fishyear").split(" ")[0], 2, "0")
                r.set("ageord", ord)
            }
        }
        stData.sort("year, ageord")

        mdb.outTable(stData)

        return stData
    }

}
