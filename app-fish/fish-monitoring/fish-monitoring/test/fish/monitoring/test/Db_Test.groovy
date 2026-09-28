package fish.monitoring.test

import fish.monitoring.dao.DataDao
import jandcode.core.apx.test.Apx_Test
import jandcode.core.store.Store
import jandcode.core.store.StoreRecord
import org.junit.jupiter.api.Test

class Db_Test extends Apx_Test {

    @Test
    void fishstock_test() {
        DataDao dao = mdb.createDao(DataDao.class)

        dao.fishStock(1000L as String, "2015-01-01", "2015-12-31")

    }

    @Test
    void test1() {
        Store st = mdb.loadQuery("""
            select * from DataPropVal where dbeg is null
        """)

        for (StoreRecord r in st) {
            mdb.execQuery("""
                update DataPropVal set dbeg='1800-01-01', dend='3333-12-31' where id=${r.getLong("id")}
            """)
        }
    }

    @Test
    void test2() {

        for (;;) {
            mdb.execQuery("""
                delete 
                from datapropval where id in (
                    select id from (
                        select d.prop, d.periodtype, v.numberval, v.dbeg, count(*) as cnt, max(v.id) as id   
                        from dataprop d, datapropval v
                        where d.id=v.dataprop and d.isobj=1 and d.objorrelobj=1000 and v.numberval is not null and d.periodtype is not null
                        group by d.prop, d.periodtype, v.numberval, v.dbeg
                        having count(*) > 1
                    ) t
                )
            """)
            Store st = mdb.loadQuery("""
                select d.prop, d.periodtype, v.numberval, v.dbeg, count(*) as cnt   
                from dataprop d, datapropval v
                where d.id=v.dataprop and d.isobj=1 and d.objorrelobj=1000 and v.numberval is not null and d.periodtype is not null
                group by d.prop, d.periodtype, v.numberval, v.dbeg
                having count(*) > 1
            """)
            if (st.size() == 0) break
        }


    }



}
