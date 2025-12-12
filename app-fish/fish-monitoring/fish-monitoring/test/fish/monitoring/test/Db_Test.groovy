package fish.monitoring.test

import jandcode.core.apx.test.Apx_Test
import jandcode.core.store.Store
import jandcode.core.store.StoreRecord
import org.junit.jupiter.api.Test

class Db_Test extends Apx_Test {


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

}
