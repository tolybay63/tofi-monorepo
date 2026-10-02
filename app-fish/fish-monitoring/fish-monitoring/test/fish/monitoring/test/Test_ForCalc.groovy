package fish.monitoring.test

import fish.monitoring.dao.DataDao
import jandcode.core.apx.test.Apx_Test
import org.junit.jupiter.api.Test

class Test_ForCalc extends Apx_Test {


    @Test
    void test1() {
        DataDao dao = mdb.createDao(DataDao.class)
        Set<Object> setFvs = dao.getFvs(1000)
        println(setFvs)
    }

}
