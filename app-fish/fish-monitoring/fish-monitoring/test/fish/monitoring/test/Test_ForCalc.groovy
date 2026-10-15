package fish.monitoring.test

import fish.monitoring.dao.DataDao
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

class Test_ForCalc extends Apx_Test {

    ApinatorApi apiMeta() { return app.bean(ApinatorService).getApi("meta") }

    ApinatorApi apiNSIData() { return app.bean(ApinatorService).getApi("nsidata") }

    ApinatorApi apiMonitoringData() { return app.bean(ApinatorService).getApi("monitoringdata") }


    @Test
    void test0() {
        DataDao dao = mdb.createDao(DataDao.class)
        Set<Object> setFvs = dao.getFvs(1000)
        println(setFvs)
    }

    ////////////////////////////////////////


    //1. Prop_NumberFishCaught		Количество пойманных рыб
    @Test
    void testCatch() {
        Store st = loadPropsCathBioWeightNetSein(1000, "Prop_NumberFishCaught", 2015)
        mdb.outTable(st)
    }

    //2. Prop_GearCatchabilityNet		Коэффициент уловистости сети
    @Test
    void testNet() {
        Store st = loadPropsCathBioWeightNetSein(1000, "Prop_GearCatchabilityNet", 2015)
        mdb.outTable(st)
    }

    //3. Prop_GearCatchabilitySeine		Коэффициент уловистости невода
    @Test
    void testSeine() {
        Store st = loadPropsCathBioWeightNetSein(1000, "Prop_GearCatchabilitySeine", 2015)
        mdb.outTable(st)
    }

    //4. Prop_WaterFishAverageWeight			Средний вес одной рыбы
    @Test
    void testAverageWeight() {
        Store st = loadPropsCathBioWeightNetSein(1000, "Prop_WaterFishAverageWeight", 2015)
        mdb.outTable(st)
    }

    //----------------------------------
    //5. Количество рыб, подвергнутых биологической обработке
    @Test
    void testBio() {
        Store st = loadPropsCathBioWeightNetSein(1000, "Prop_WaterNumberFishBio", 2015)
        mdb.outTable(st)
    }


    //
    Store loadPropsCathBioWeightNetSein(long reservoir, String codProp, int year) {
        long periodType = 11L
        String dte = year + "-01-01"

        Set<Object> fvs1 = getFvs(reservoir) //Виды рыб водоема
        String strFvs1 = "'" + fvs1.join("','") + "'" // Добавлены закрывающие кавычки для безопасности
        //
        Store stFV = loadSqlMeta("""
            select id, parent from Factor where cod='FV_Age1'
        """, "")
        long Factor_Age = stFV.get(0).getLong("parent")
        long fvFishAge_1 = stFV.get(0).getLong("id")
        Store stFv2 = loadSqlMeta("""
            select id ,name 
            from Factor where parent=${Factor_Age} and id<>${fvFishAge_1} order by ord   
        """, "")

        // Собираем общий список разрешенных ID в виде строки ('1025','1026',...)
        Set<String> arrFvsStr = new HashSet<>()
        arrFvsStr.addAll(fvs1.collect { it.toString() })
        arrFvsStr.addAll(stFv2.getUniqueValues("id").collect { it.toString() })
        String strFv1Fvs2 = "'" + arrFvsStr.join("','") + "'"

        long meter = loadSqlMeta("""
            select meter from Prop where cod='${codProp}'
        """, "").get(0).getLong("meter")

        // Переписанный запрос без массивов PostgreSQL (нет ARRAY и string_to_array)
        Store stProp = loadSqlMeta("""
            with mrfv as (
                select meterrate,
                    STRING_AGG (cast(factorval as varchar(200)), ',') as fvs,
                    COUNT(factorval) as sz
                from meterratefv
                group by meterrate
            )
            select * from (
              select p.id, p.name, m.kFromBase, fvs, null as numberval   
              from Prop p, mrfv, measure m
              where p.meter=${meter} and p.measure=m.id and p.meterrate=mrfv.meterrate 
                and mrfv.sz=1 and fvs in (${strFvs1})
              union all
              select p.id, p.name, m.kFromBase, fvs, null as numberval
              from Prop p, mrfv, measure m
              where p.meter=${meter} and p.measure=m.id and p.meterrate=mrfv.meterrate 
                and mrfv.sz=2 
                and not exists (
                    select 1 from meterratefv m_sub 
                    where m_sub.meterrate = mrfv.meterrate 
                    and cast(m_sub.factorval as varchar) not in (${strFv1Fvs2})
                )
            ) t order by id
        """, "")

        Set<Object> idsProp = stProp.getUniqueValues("id")

        // Далее проставляем данные

        UtPeriod up = new UtPeriod()
        String d1 = up.calcDbeg(XDate.create(dte), periodType, 0).toString(XDateTimeFormatter.ISO_DATE)
        String d2 = up.calcDend(XDate.create(dte), periodType, 0).toString(XDateTimeFormatter.ISO_DATE)


        String sql = """
            select d.prop, v.numberval
            from DataProp d, DataPropVal v
            where d.id=v.dataProp and d.isObj=1 and d.objorrelobj=${reservoir} and d.prop in (${idsProp.join(",")}) and d.periodType=${periodType}
                and v.dbeg='${d1}' and v.dend='${d2}'
        """
        Store stVal = mdb.loadQuery(sql)
        StoreIndex indVal = stVal.getIndex("prop")
        //
        for (StoreRecord r in stProp) {
            StoreRecord rec = indVal.get(r.getLong("id"))
            if (rec != null)
                r.set("numberval", rec.getDouble("numberval") * r.getDouble("kFromBase"))
        }
        return stProp
    }



    //=============================================================

    private Store loadSqlMeta(String sql, String domain) {
        return apiMeta().get(ApiMeta).loadSql(sql, domain)
    }

    /**
     * fvs второго участника Typ_Reservoir <=> Typ_Fish
     * @param reservoir
     * @return set oj fvs
     */

    Set<Object> getFvs(long reservoir) {
        Store st = loadSqlMeta("""
            select c.id from Cls c, Typ t
            where c.typ=t.id and t.cod='Typ_WaterBodies'
        """, "")
        Set<Object> setCls1 = st.getUniqueValues("id")
        st = loadSqlMeta("""
            select c.id from Cls c, Typ t
            where c.typ=t.id and t.cod='Typ_Fish'
        """, "")
        Set<Object> setCls2 = st.getUniqueValues("id")
        //
        // если mdb то запрос к БД текущего сервиса, т.е. в данном случае к monitoring
        st = mdb.loadQuery("""
            select r2.cls, null as factorval
            from RelObj ro
                join relobjmember r1 on r1.relobj=ro.id and r1.cls in (${setCls1.join(",")})
                join relobjmember r2 on r2.relobj=ro.id and r2.cls in (${setCls2.join(",")})
            where r1.obj=${reservoir}
        """)
        Store stCls = loadSqlMeta("""
            select c.cls, c.factorval  
            from clsfactorval c, factor f 
            where c.factorval=f.id and f.cod <> 'FV_Fictive' and
                c.cls in (${setCls2.join(",")})
        """, "")
        StoreIndex indCls = stCls.getIndex("cls")
        for (StoreRecord r in st) {
            StoreRecord rec = indCls.get(r.getLong("cls"))
            if (rec != null) {
                r.set("factorval", rec.getLong("factorval"))
            }
        }
        return st.getUniqueValues("factorval") as Set<Object>
    }

}
