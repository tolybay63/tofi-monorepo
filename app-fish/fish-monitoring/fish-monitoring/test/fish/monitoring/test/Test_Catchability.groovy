package fish.monitoring.test

import fish.monitoring.dao.DataDao
import fish.monitoring.dao.utils.CatchabilityOptimizer
import jandcode.commons.UtCnv
import jandcode.commons.datetime.XDate
import jandcode.commons.datetime.XDateTimeFormatter
import jandcode.core.apx.test.Apx_Test
import jandcode.core.store.Store
import jandcode.core.store.StoreIndex
import jandcode.core.store.StoreRecord
import org.junit.jupiter.api.Test
import tofi.api.dta.model.utils.UtPeriod
import tofi.api.mdl.ApiMeta
import tofi.apinator.ApinatorApi
import tofi.apinator.ApinatorService

class Test_Catchability extends Apx_Test {

    ApinatorApi apiMeta() { return app.bean(ApinatorService).getApi("meta") }

    ApinatorApi apiNSIData() { return app.bean(ApinatorService).getApi("nsidata") }

    ApinatorApi apiMonitoringData() { return app.bean(ApinatorService).getApi("monitoringdata") }
    //-----------------------------------------------------------------------------------------------//



    @Test
    void test1() {
        // 1. Подготавливаем данные
/*
        double[][] catchesData = [
                [30d, 120d, 450d, 600d, 350d, 200d],    //Date 1
                [15d, 80d, 310d, 520d, 410d, 180d],     //Date 2
                [50d, 200d, 500d, 480d, 300d, 150d],    //Date 3
                [100d, 300d, 600d, 400d, 200d, 50d],    //Date 4
                [10d, 50d, 200d, 450d, 500d, 300d]      //Date 5
        ] as double[][]
*/

        List<List<Double>> catchesData = getDataBream()
        double[] qKnown = [0.35d, 0.35d, 0.35d, 0.35d, 0.35d,
                           0.35d, 0.35d, 0.35d, 0.35d, 0.35d,
                           0.35d, 0.35d, 0.35d, 0.35d, 0.35d] as double[]


        double[] initGuess = [0.60d, 3.0d, 1.0d] as double[]
        double[] lower = [0.35d, 0.5d, 0.1d] as double[]
        double[] upper = [1.00d, 10.0d, 5.0d] as double[]

        // 2. Инициализируем и вызываем
        CatchabilityOptimizer optimizer = new CatchabilityOptimizer()
        Map result = optimizer.optimize(catchesData as double[][], qKnown, initGuess, lower, upper)

        // 3. Работаем с результатом
        println "Минимальная ошибка МНК: ${result.error}"
        println "q_max (макс. уловистость): ${result.qMax}"
        println "a50 (возраст 50% отлова): ${result.a50}"
        println "k (крутизна сигмоиды): ${result.k}"

        println "Рассчитанные уловистости по возрастам:"
        result.qByAge.each { age, qVal ->
            println "Возраст ${age}: q = ${String.format('%.4f', qVal)}"
        }

    }


    //@Test
    List<List<Double>> getDataBream() {
        //DataDao dao = mdb.createDao(DataDao.class)
        //def prop = 1049L
        def reservoir = 1000L
        def periodType = 71L
        //def dependperiod = true
        def dbeg = "2015-01-01"
        def dend = "2015-12-31"

        Set<Object> setCls = apiMeta().get(ApiMeta).setIdsOfCls("Typ_FishCatch")
        if (setCls.isEmpty()) setCls.add(0L)
        String whe = "cls in (${setCls.join(",")})"
        String wheReservoirs = "v6.obj in (${reservoir}) and v1.dateTimeVal between '${dbeg}' and '${dend}'"

        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "", "Prop_%")
        Store st = mdb.loadQuery("""
            with ob as (
            select
                id, cls from Obj               
                where ${whe}
            )
            select ob.id as obj, ob.cls,
                v1.dateTimeVal::date as StartDate,
                v6.obj as objReservoirShore
            from ob
                join DataProp d1 on d1.isObj=1 and d1.objorrelobj=ob.id and d1.prop=:Prop_StartDate
                join DataPropVal v1 on d1.id=v1.dataprop
                left join DataProp d6 on d6.isObj=1 and d6.objorrelobj=ob.id and d6.prop=:Prop_ReservoirShore
                left join DataPropVal v6 on d6.id=v6.dataprop
            where ${wheReservoirs}
            order by v1.dateTimeVal
        """, map)
        //
        //mdb.outTable(st)
        //
        Map<String, Object> mapParamCatch = new HashMap<>()
        mapParamCatch.put("dependperiod", true)
        mapParamCatch.put("periodType", 71)
        mapParamCatch.put("prop", map.get("Prop_NumberFishCaught"))
        mapParamCatch.put("cod", "Prop_NumberFishCaught")

        List<List<Double>> lstData = new ArrayList<>()
        int indexAll = 0
        int index = 0
        for (StoreRecord r in st) {
            List<Double> lst = new ArrayList<>()
            long obj = r.getLong("obj")
            String dte = r.getString("StartDate")
            mapParamCatch.put("own", obj)
            mapParamCatch.put("obj2", reservoir)
            mapParamCatch.put("dte", dte)
            //
            Set<Object> idsProp = getIdsProp()
            Store stData = mdb.loadQuery("""
                select d.prop, v.numberval, v.id as idval
                from DataProp d, DataPropVal v
                where d.id=v.dataProp and d.isObj=1 and d.objorrelobj=${obj} and d.prop in (${idsProp.join(",")}) and d.periodType=${periodType}
                    and v.dbeg='${dte}' and v.dend='${dte}'
            """)

            //mdb.outTable(stData)

            stData.forEach {StoreRecord it -> {
                lst.add(it.getDouble("numberval"))
            }}
            if (lst.size()==15) {
                lstData.add(index, lst)
                index++
            }
            indexAll++
        }

        println("countAll: ${indexAll}")
        println("count: ${index}")
        println(lstData)
        //
        return lstData
    }

    /////////////////////
    Set<Object> getIdsProp() {
        Store st = loadSqlMeta("""
            with mrfv as (
            select meterrate,
                STRING_AGG (cast(factorval as varchar(200)), ',') as fvs,
                string_to_array(STRING_AGG (cast(factorval as varchar(4000)), ','), ',') as arr,
                ARRAY_LENGTH(STRING_TO_ARRAY(STRING_AGG (cast(factorval as varchar(200)), ','), ','), 1) sz
            from meterratefv
            group by meterrate
            )
            select id, name, fvs   
            from Prop p, mrfv
            where p.meter=1006 and p.meterrate=mrfv.meterrate and mrfv.sz=2 and ARRAY[mrfv.arr] @> '{1025}'
                and p.id<>8666
        """, "")
        return st.getUniqueValues("id")

    }


    ////////////////
    private Store loadSqlMeta(String sql, String domain) {
        return apiMeta().get(ApiMeta).loadSql(sql, domain)
    }


}
