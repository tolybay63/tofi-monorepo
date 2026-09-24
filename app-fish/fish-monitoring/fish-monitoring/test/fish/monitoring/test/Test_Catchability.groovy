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
        double[][] catchesData = [
                [30d, 120d, 450d, 600d, 350d, 200d],    //Date 1
                [15d, 80d, 310d, 520d, 410d, 180d],     //Date 2
                [50d, 200d, 500d, 480d, 300d, 150d],    //Date 3
                [100d, 300d, 600d, 400d, 200d, 50d],    //Date 4
                [10d, 50d, 200d, 450d, 500d, 300d]      //Date 5
        ] as double[][]

        double[] qKnown = [0.35d, 0.35d, 0.35d, 0.35d, 0.35d] as double[]

        // Опционально: можно задать свои границы или оставить те, что по умолчанию
        double[] initGuess = [0.60d, 3.0d, 1.0d] as double[]
        double[] lower = [0.35d, 0.5d, 0.1d] as double[]
        double[] upper = [1.00d, 10.0d, 5.0d] as double[]

        // 2. Инициализируем и вызываем
        CatchabilityOptimizer optimizer = new CatchabilityOptimizer()
        Map result = optimizer.optimize(catchesData, qKnown, initGuess, lower, upper)
        // Если устраивают дефолтные границы, достаточно передать только данные:
        // Map result = optimizer.optimize(catchesData, qKnown)

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


    @Test
    void test2() {
        DataDao dao = mdb.createDao(DataDao.class)
        def prop = 1049L
        def reservoir = 1000L
        def periodType = 71L
        def dependperiod = true
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

        mdb.outTable(st)

        Map<String, Object> mapParamCatch = new HashMap<>()
        mapParamCatch.put("dependperiod", true)
        mapParamCatch.put("periodType", 71)
        mapParamCatch.put("prop", map.get("Prop_NumberFishCaught"))
        mapParamCatch.put("cod", "Prop_NumberFishCaught")

        for (StoreRecord r in st) {
            long obj = r.getLong("obj")
            String dte = r.getString("StartDate")

            mapParamCatch.put("own", obj)
            mapParamCatch.put("obj2", reservoir)
            mapParamCatch.put("dte", dte)
            //
            Map<String, Store> mapCatch = dao.loadAlgoMatrix(mapParamCatch)
            Store stCatch = mapCatch.get("stMatrix")         //сторе с данными

            mdb.outTable(stCatch)


            def oooo = 0


        }






/*
        def meter = loadSqlMeta("""
            select meter from Prop where id=${prop}
        """, "").get(0).getLong("meter")
        //
        Store stProp2Lev = loadSqlMeta("""
            with mrfv as (
            select meterrate,
                STRING_AGG (cast(factorval as varchar(200)), ',') as fvs,
                ARRAY_LENGTH(STRING_TO_ARRAY(STRING_AGG (cast(factorval as varchar(200)), ','), ','), 1) sz
            from meterratefv
            group by meterrate
            )
            select id, name, fvs
            from Prop p, mrfv
            where p.meter=${meter} and p.meterrate=mrfv.meterrate and mrfv.sz=2
        """, "")
        Set<Object> idsPropsAll = stProp2Lev.getUniqueValues("id")
        StoreIndex indProp2Lev = stProp2Lev.getIndex("fvs")
        //
        Set<Object> fvsFromRelObj = dao.getFvs(reservoir)
        //
        Set<Long> setFv1 = new HashSet<>()
        Set<Long> setFv2 = new HashSet<>()
        for (StoreRecord r in stProp2Lev) {
            String[] arr = r.getString("fvs").split(",")
            if (fvsFromRelObj.contains(arr[0]))
                setFv1.add(UtCnv.toLong(arr[0]))
            setFv2.add(UtCnv.toLong(arr[1]))
        }
        //
        Store stFv1 = loadSqlMeta("""
            select id, name
            from factor
            where id in (0${setFv1.join(",")})
            order by ord
        """, "")

        Store stFv2 = mdb.createStore()
        stFv2.addField("id", "long")
        stFv2.addField("name", "string", 20)


        String d1 = "1800-01-01"
        String d2 = "3333-12-01"
        if (dependperiod) {
            UtPeriod up = new UtPeriod()
            d1 = up.calcDbeg(XDate.create(dte), periodType, 0).toString(XDateTimeFormatter.ISO_DATE)
            d2 = up.calcDend(XDate.create(dte), periodType, 0).toString(XDateTimeFormatter.ISO_DATE)
        }
        String sql = """
            select d.prop, v.numberval, v.id as idval
            from DataProp d, DataPropVal v
            where d.id=v.dataProp and d.isObj=1 and d.objorrelobj=${own} and d.prop in (${idsPropsAll.join(",")}) and d.periodType is null
        """
        if (dependperiod)
            sql = """
            select d.prop, v.numberval, v.id as idval
            from DataProp d, DataPropVal v
            where d.id=v.dataProp and d.isObj=1 and d.objorrelobj=${own} and d.prop in (${idsPropsAll.join(",")}) and d.periodType=${periodType}
                and v.dbeg='${d1}' and v.dend='${d2}'
        """
        Store stVal = mdb.loadQuery(sql)
        // Has data Lev1
        mdb.outTable(stVal)
*/










    }
    ////////////////
    private Store loadSqlMeta(String sql, String domain) {
        return apiMeta().get(ApiMeta).loadSql(sql, domain)
    }


}
