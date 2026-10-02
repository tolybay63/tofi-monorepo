package fish.monitoring.test

import fish.monitoring.dao.DataDao
import jandcode.commons.UtCnv
import jandcode.commons.datetime.XDate
import jandcode.commons.datetime.XDateTimeFormatter
import jandcode.core.apx.test.Apx_Test
import jandcode.core.dao.DaoMethod
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

    @Test
    void test_fish() {
        Store st = loadTypesFish()

        mdb.outTable(st)
    }

    Store loadTypesFish() {
        String codTyp = "Typ_Fish"

        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "Prop_FishTyp", "")

        Set<Object> idsCls = apiMeta().get(ApiMeta).setIdsOfCls(codTyp)
        String whe = "o.cls in (${idsCls.join(",")})"

        //Store st = mdb.createStore("Obj.typesFish")
        Store st = mdb.loadQuery("""
            select o.id as obj, o.cls, v.name,
                v2.propVal as pvFishTyp, null as fvFishTyp, null as nameFishTyp
            from Obj o
                left join ObjVer v on o.id=v.ownerver and v.lastver=1
                left join DataProp d2 on d2.isObj=1 and d2.objorrelobj=o.id and d2.prop=:Prop_FishTyp
                left join DataPropVal v2 on d2.id=v2.dataprop
            where ${whe}
        """, map)

        Store stFV = apiMeta().get(ApiMeta).storeFVfromPropVal()
        StoreIndex indFV = stFV.getIndex("propval")

        for (StoreRecord r in st) {
            StoreRecord rec = indFV.get(r.getLong("pvFishTyp"))
            if (rec != null) {
                r.set("fvFishTyp", rec.getLong("factorval"))
                r.set("nameFishTyp", rec.getString("name"))
            }
        }
        return st
    }


    @Test
    void test1() {
        Store st = loadWaterNumberFishBio(1000, "Prop_WaterNumberFishBio", 2015)

        mdb.outTable(st)

    }

    //Prop_WaterNumberFishBio
    Store loadWaterNumberFishBio(long reservoir, String codProp, int year) {
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


    //------------------------------------------
    @Test
    void test2() {
        Store st = loadMetersOfOwnerWithPeriod(1000, 1, "Prop_WaterNumberFishBio", 2015, 2016)

        mdb.outTable(st)

    }


    Store loadMetersOfOwnerWithPeriod(long own, int isObj, String codProp, int start_year, int end_year) {

        long periodType = 11L
        String dte = start_year + "-01-01"

        Store st = apiMeta().get(ApiMeta).loadSql("""
                WITH RECURSIVE r AS (
                    SELECT p.id, p.cod, p.parent, p.name || ' ('||m.name||')' as name, p.isdependvalueonperiod as dependperiod, null as dbeg, null as dend, null as numberval, null as idval, m.kfrombase
                    FROM prop p, Measure m
                    WHERE p.measure=m.id and p.cod = '${codProp}'    
                    UNION ALL    
                    SELECT p1.id, p1.cod as cod, p1.parent, p1.name || ' ('||m1.name||')' as name, p1.isdependvalueonperiod as dependperiod, null as dbeg, null as dend, null as numberval, null as idval, m1.kfrombase
                    FROM  prop p1
                    JOIN Measure m1 ON p1.measure=m1.id
                    JOIN r ON p1.parent = r.id
                )
                SELECT id, parent, cod, name, dependperiod, dbeg, dend, numberval, idval, kfrombase
                FROM r;
            """, "")

        Set<Object> idsProp = st.getUniqueValues("id")
        //
        Store stData = mdb.loadQuery("""
                select d.prop as prop, v.numberval, v.dbeg, v.dend, v.id
                from DataProp d
                    left join DataPropVal v on d.id=v.dataProp
                where d.isObj=${isObj} and d.objorrelobj=${own} and d.periodType=${periodType} and 
                    '${dte}' between v.dbeg and v.dend and d.prop in (0${idsProp.join(",")})
                union all
                select d.prop as prop, v.numberval, v.dbeg, v.dend, v.id
                from DataProp d
                    left join DataPropVal v on d.id=v.dataProp
                where d.isObj=${isObj} and d.objorrelobj=${own} and d.periodType is null and '${dte}' between v.dbeg and v.dend 
                    and d.prop in (0${idsProp.join(",")})                
            """)

        StoreIndex indData = stData.getIndex("prop")
        for (StoreRecord r in st) {
            StoreRecord rec = indData.get(r.getLong("id"))
            if (rec != null) {
                double kf = r.getDouble("kfrombase")
                if (kf == 0) kf = 1
                r.set("idval", rec.getLong("id"))
                r.set("numberval", rec.getDouble("numberval") * kf)
                r.set("dbeg", rec.getString("dbeg"))
                r.set("dend", rec.getString("dend"))
            }
        }
        return st
    }


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
