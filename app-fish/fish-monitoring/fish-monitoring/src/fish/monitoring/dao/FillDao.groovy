package fish.monitoring.dao

import jandcode.core.dao.DaoMethod
import jandcode.core.dbm.mdb.BaseMdbUtils
import jandcode.core.store.Store

class FillDao  extends BaseMdbUtils {

    @DaoMethod
    Store loadLog() {
        return mdb.loadQuery("""
            select * from log
        """)
    }


    @DaoMethod
    void fillFishing_1(File file, boolean fill) {

        int o= 0

    }


}
