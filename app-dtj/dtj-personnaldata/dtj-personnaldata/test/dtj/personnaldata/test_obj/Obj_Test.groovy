package dtj.personnaldata.test_obj

import jandcode.core.apx.test.Apx_Test
import jandcode.core.store.Store
import jandcode.core.store.StoreRecord
import org.junit.jupiter.api.Test

class Obj_Test extends Apx_Test {

    @Test
    void jsonrpc1() throws Exception {
        Map<String, Object> map = apx.execJsonRpc("api", "data/loadObj", ["Cls_WorkCheckInspect", "nsidata"])
        mdb.outMap(map.get("result") as Map)

    }

}
