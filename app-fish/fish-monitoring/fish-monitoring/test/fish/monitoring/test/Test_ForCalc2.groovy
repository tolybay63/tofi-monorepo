package fish.monitoring.test

import jandcode.core.apx.test.Apx_Test
import jandcode.core.store.Store
import jandcode.core.store.StoreIndex
import jandcode.core.store.StoreRecord
import org.junit.jupiter.api.Test
import tofi.api.mdl.ApiMeta
import tofi.apinator.ApinatorApi
import tofi.apinator.ApinatorService

class Test_ForCalc2 extends Apx_Test {
    ApinatorApi apiMeta() { return app.bean(ApinatorService).getApi("meta") }
    ApinatorApi apiNSIData() { return app.bean(ApinatorService).getApi("nsidata") }
    ApinatorApi apiMonitoringData() { return app.bean(ApinatorService).getApi("monitoringdata") }



    /*
Prop_CalcAgeSex						Возраст половой зрелости рыбы
Prop_FishMaxAge						Максимальный возраст рыбы, лет
Prop_FishSpeed						Крейсерская скорость рыбы
     */

    @Test
    void test0() {
        Store st = loadFishProps(1005)
        mdb.outTable(st)
    }
    /* Result
    load-app: completed in 0.531 sec
+----+------+-----+--------------+-----+--------------+-----+-------------+
| id | name |prop1|val_calcagesex|prop2|val_fishmaxage|prop3|val_fishspeed|
+----+------+-----+--------------+-----+--------------+-----+-------------+
|1001|лещ   | 7122|           4.0| 7277|          16.0| 7280|         0.05|
|1008|судак | 7122|           3.0| 7277|          11.0| 7280|         0.13|
|1010|плотва| 7122|           2.0| 7277|          10.0| 7280|         0.05|
|1013|сазан | 7122|           3.0| 7277|          10.0| 7280|         0.06|
|1012|карась| 7122|           4.0| 7277|          15.0| 7280|         0.04|
|1014|язь   | 7122|           3.0| 7277|          13.0| 7280|          0.1|
|1009|окунь | 7122|           3.0| 7277|          13.0| 7280|         0.04|
|1011|щука  | 7122|           3.0| 7277|          10.0| 7280|         0.04|
+----+------+-----+--------------+-----+--------------+-----+-------------+
records: 8

     */



    /**
     *
     !Все свойства не зависит от периода!
         Prop_CalcAgeSex	Возраст половой зрелости рыбы
         Prop_FishMaxAge	Максимальный возраст рыбы, лет
         Prop_FishSpeed		Крейсерская скорость рыбы
     *
     * @param reservoir
     * @return
     */
    Store loadFishProps(long reservoir) {
        Store stTmp = loadSqlMeta("""
            select c.id from Cls c, Typ t
            where c.typ=t.id and t.cod='Typ_WaterBodies'
        """, "")
        Set<Object> setCls1 = stTmp.getUniqueValues("id")
        stTmp = loadSqlMeta("""
            select c.id from Cls c, Typ t
            where c.typ=t.id and t.cod='Typ_Fish'
        """, "")
        Set<Object> setCls2 = stTmp.getUniqueValues("id")
        //
        // если mdb то запрос к БД текущего сервиса, т.е. в данном случае к monitoring
        stTmp = mdb.loadQuery("""
            select r2.obj as objFish
            from RelObj ro
                join relobjmember r1 on r1.relobj=ro.id and r1.cls in (${setCls1.join(",")})
                join relobjmember r2 on r2.relobj=ro.id and r2.cls in (${setCls2.join(",")})
            where r1.obj=${reservoir}
        """)

        //Объекты Fish водоема reservoir
        Set<Object> setObjFish = stTmp.getUniqueValues("objFish")

        Store stProp = loadSqlMeta("""
            SELECT p.id, p.cod, p.name, m.kfrombase 
            FROM prop p, Measure m
            WHERE p.measure=m.id and p.cod in ('Prop_CalcAgeSex', 'Prop_FishMaxAge','Prop_FishSpeed')
        """, "")
        StoreIndex indProp = stProp.getIndex("id")

        Map<String, Long> mapProp = new HashMap<>()
        for (StoreRecord r in stProp) {
            mapProp.put(r.getString("cod"), r.getLong("id"))
        }
        // Value
        Store stVal = mdb.loadQuery("""
            select  o.id, ov.name, 
                    d1.prop as prop1, v1.numberval as val_CalcAgeSex,
                    d2.prop as prop2, v2.numberval as val_FishMaxAge,
                    d3.prop as prop3, v3.numberval as val_FishSpeed
            from Obj o
                join ObjVer ov on o.id=ov.ownerVer and ov.lastVer=1
                join DataProp d1 on d1.isObj=1 and d1.objOrRelObj=o.id and d1.periodtype is null and d1.prop = :Prop_CalcAgeSex
                join DataPropVal v1 on v1.dataprop=d1.id 
                join DataProp d2 on d2.isObj=1 and d2.objOrRelObj=o.id and d2.periodtype is null and d2.prop = :Prop_FishMaxAge
                join DataPropVal v2 on v2.dataprop=d2.id 
                join DataProp d3 on d3.isObj=1 and d3.objOrRelObj=o.id and d3.periodtype is null and d3.prop = :Prop_FishSpeed
                join DataPropVal v3 on v3.dataprop=d3.id
            where o.id in (${setObjFish.join(",")})
        """, mapProp)

        for (StoreRecord r in stVal) {
            StoreRecord rec1 = indProp.get(r.getLong("prop1"))
            StoreRecord rec2 = indProp.get(r.getLong("prop2"))
            StoreRecord rec3 = indProp.get(r.getLong("prop3"))
            if (rec1 != null) {
                double kf = rec1.getDouble("kfrombase")
                if (kf == 0) kf = 1
                r.set("val_CalcAgeSex", r.getDouble("val_CalcAgeSex") * kf)
            }
            if (rec2 != null) {
                double kf = rec2.getDouble("kfrombase")
                if (kf == 0) kf = 1
                r.set("val_CalcAgeSex", r.getDouble("val_CalcAgeSex") * kf)
            }
            if (rec3 != null) {
                double kf = rec1.getDouble("kfrombase")
                if (kf == 0) kf = 1
                r.set("val_CalcAgeSex", r.getDouble("val_CalcAgeSex") * kf)
            }
        }
        return stVal
    }

    //************************
    @Test
    void test_fish() {
        Store st = loadTypesFish()
        mdb.outTable(st)
    }

    //-------------------------------------

    /**
     +----+----+---------+---------+---------+-----------+
     |obj |cls |  name   |pvfishtyp|fvfishtyp|namefishtyp|
     +----+----+---------+---------+---------+-----------+
     |1001|1012|лещ      |     1046|1072     |мирная рыба|
     |1008|1013|судак    |     1045|1071     |хищная рыба|
     * @return
     */
    Store loadTypesFish() {
        String codTyp = "Typ_Fish"
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "Prop_FishTyp", "")

        Set<Object> idsCls = apiMeta().get(ApiMeta).setIdsOfCls(codTyp)
        String whe = "o.cls in (${idsCls.join(",")})"

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


    //Объекты Fish водоема
    Set<Object> getObjFishFromReservoir(long reservoir) {
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
            select r2.obj
            from RelObj ro
                join relobjmember r1 on r1.relobj=ro.id and r1.cls in (${setCls1.join(",")})
                join relobjmember r2 on r2.relobj=ro.id and r2.cls in (${setCls2.join(",")})
            where r1.obj=${reservoir}
        """)
        return st.getUniqueValues("obj") as Set<Object>

    }


    //=============================================================

    private Store loadSqlMeta(String sql, String domain) {
        return apiMeta().get(ApiMeta).loadSql(sql, domain)
    }


}
