package tofi.nsi.test_obj

import jandcode.core.apx.test.Apx_Test
import jandcode.core.store.Store
import org.junit.jupiter.api.Test

class Obj_Test extends Apx_Test {

    @Test
    public void jsonrpc1() throws Exception {
        Map<String, Store>  map = apx.execJsonRpc("api", "data/loadObj", [1000]) as Map<String, Store>
        mdb.outMap(map)
        map.result.records.forEach {
            mdb.outTable(it)
        }

    }

}
