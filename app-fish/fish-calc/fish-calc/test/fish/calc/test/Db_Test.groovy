package fish.calc.test

import jandcode.commons.error.XError
import jandcode.core.apx.test.Apx_Test
import jandcode.core.store.Store
import jandcode.core.store.StoreRecord
import org.junit.jupiter.api.Test
import tofi.api.dta.ApiCalcData
import tofi.api.dta.ApiMonitoringData
import tofi.api.dta.ApiNSIData
import tofi.api.mdl.ApiMeta
import tofi.apinator.ApinatorApi
import tofi.apinator.ApinatorService

class Db_Test extends Apx_Test {

    ApinatorApi apiMeta() { return app.bean(ApinatorService).getApi("meta") }
    ApinatorApi apiNSIData() { return app.bean(ApinatorService).getApi("nsidata") }
    ApinatorApi apiMonitoringData() { return app.bean(ApinatorService).getApi("monitoringdata") }
    ApinatorApi apiCalcData() { return app.bean(ApinatorService).getApi("calcdata") }

    @Test
    void test1() {
        Store st = getPropsOfCalc(1039)
        mdb.outTable(st)
    }


    Store getPropsOfCalc(long idCalc) {

     //Запрос на Мета
        Store stTmp = loadSqlMeta("""
            select id, cod from Prop where cod in ('Prop_ReservoirShore','Prop_CalcStartYear','Prop_CalcEndYear') 
        """, "")
        Map<String, Long> mapProp = new HashMap<>()
        for (StoreRecord r in stTmp) {
            mapProp.put(r.getString("cod"), r.getLong("id"))
        }
        // Здесь запрос на базу сервиса calc
        Store st = mdb.loadQuery("""
            select o.id as idCalc,
                v1.strVal::integer as start_year,
                v2.strVal::integer as end_year,    
                v3.obj as waterbody_id
            from Obj o
                join ObjVer v on o.id=v.ownerVer and v.lastVer=1
                join DataProp d1 on d1.isObj=1 and d1.objOrRelObj=o.id and d1.prop=:Prop_CalcStartYear
                join DataPropVal v1 on v1.dataprop=d1.id
                join DataProp d2 on d2.isObj=1 and d2.objOrRelObj=o.id and d2.prop=:Prop_CalcEndYear
                join DataPropVal v2 on v2.dataprop=d2.id
                join DataProp d3 on d3.isObj=1 and d3.objOrRelObj=o.id and d3.prop=:Prop_ReservoirShore
                join DataPropVal v3 on v3.dataprop=d3.id    
            where o.id=${idCalc}
        """, mapProp)
        return st
    }


    //--------------------
    private Store loadSqlService(String sql, String domain, String model) {
        if (model.equalsIgnoreCase("nsidata"))
            return apiNSIData().get(ApiNSIData).loadSql(sql, domain)
        else if (model.equalsIgnoreCase("monitoringdata"))
            return apiMonitoringData().get(ApiMonitoringData).loadSql(sql, domain)
        else if (model.equalsIgnoreCase("calcdata"))
            return apiCalcData().get(ApiCalcData).loadSql(sql, domain)
        else
            throw new XError("Unknown model [${model}]")
    }

    private Store loadSqlMeta(String sql, String domain) {
        return apiMeta().get(ApiMeta).loadSql(sql, domain)
    }
}
