package dtj.object.test_obj

import dtj.object.dao.DataDao
import jandcode.core.apx.test.Apx_Test
import jandcode.core.store.Store
import jandcode.core.store.StoreRecord
import org.junit.jupiter.api.Test

import javax.xml.crypto.Data

class Obj_Test extends Apx_Test {


    @Test
    void test_FV() {
        DataDao dao = mdb.createDao(DataDao.class)
        Store st = dao.loadFactorValForSelect("Factor_Periodicity")
        mdb.outTable(st)
    }

    @Test
    void test_Obj() {
        DataDao dao = mdb.createDao(DataDao.class)
        Store st = dao.loadObjList("Cls_Collections", "Prop_Collections",  'nsidata')
        mdb.outTable(st)
    }

    @Test
    void jsonrpc1() throws Exception {
        Map<String, Object> map = apx.execJsonRpc("api", "data/loadObjList", ["Cls_Collections", "Prop_Collections", "nsidata"])
        mdb.outMap(map.get("result") as Map)
    }

}
