package dtj.orgstructuredata.test_obj

import jandcode.core.apx.test.Apx_Test
import jandcode.core.store.Store
import jandcode.core.store.StoreRecord
import org.junit.jupiter.api.Test

class Obj_Test extends Apx_Test {

    @Test
    void jsonrpc1() throws Exception {
        Map<String, Object> map = apx.execJsonRpc("api", "data/loadObj", ["Cls_WorkCheckInspect", "nsidata"])
        mdb.outMap(map.get("result") as Map)


/*
        Map<String, Object> st = apx.execJsonRpc("api", "data/loadObj", ["Cls_WorkCheckInspect", "nsidata"])
        List<Object> sr = st["result"]["store"] as List<Object>
        for (Object r in sr) {
            println((r["records"] as ArrayList).get(0))
        }
        //
        mdb.outMap(st)
*/


    }

}
