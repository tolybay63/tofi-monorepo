package dtj.plan.test_obj

import dtj.plan.dao.DataDao
import jandcode.core.apx.test.Apx_Test
import jandcode.core.store.Store
import jandcode.core.store.StoreRecord
import org.junit.jupiter.api.Test

class Obj_Test extends Apx_Test {

    @Test
    void testPersonnalId() {
        DataDao dao = mdb.createDao(DataDao.class)
        long id = dao.getPersonnalId(1013)
        println("id = "+id )

    }


    @Test
    void jsonrpc1() throws Exception {
        Map<String, Object> map = apx.execJsonRpc("api", "data/getPersonnalId", [1013])
        mdb.outMap(map)
    }

}
