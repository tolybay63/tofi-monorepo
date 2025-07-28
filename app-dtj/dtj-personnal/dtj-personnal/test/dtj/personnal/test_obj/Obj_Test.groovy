package dtj.personnal.test_obj

import dtj.personnal.dao.DataDao
import jandcode.core.apx.test.Apx_Test
import jandcode.core.store.Store
import jandcode.core.store.StoreRecord
import org.junit.jupiter.api.Test

class Obj_Test extends Apx_Test {

    @Test
    void loadPersonnal() {
        DataDao dao = mdb.createDao(DataDao.class)
        Store st = dao.loadPersonnal(0)
        mdb.outTable(st)
    }

    @Test
    void savePersonnalIns() {
        Map<String, Object> map = new HashMap<>()
        map.put("login", "user_dtj")
        map.put("passwd", "111")
        map.put("UserEmail", "user_dtj@gmail.com")
        map.put("UserPhone", "7773334455")
        map.put("UserFirstName", "Иван")
        map.put("UserSecondName", "Иванов")
        map.put("TabNumber", "123456789")
        map.put("CreatedAt", "2025-07-27")
        map.put("UserDateBirth", "2020-07-27")
        map.put("fvUserSex", 1088)
        map.put("pvUserSex", 1085)
        map.put("fvPosition", 1130)
        map.put("pvPosition", 1246)
        map.put("objLocation", 1001)
        map.put("pvLocation", 1040)

        //
        savePersonnal("ins", map)
    }

    @Test
    void savePersonnalUpd() {
        DataDao dao = mdb.createDao(DataDao.class)
        Store st = dao.loadPersonnal(0)
        Map<String, Object> map = st.get(0).getValues()
        map.put("UserSecondName", "New Value")
        map.put("UserMiddleName", "UserMiddleName update")
        map.put("TabNumber", "987654321 upd")
        //...

        savePersonnal("upd", map)
    }

    private void savePersonnal(String mode, Map<String, Object> params) {
        DataDao dao = mdb.createDao(DataDao.class)
        Store st = dao.savePersonnal(mode, params)
        mdb.outTable(st)
    }

    @Test
    void delectPersonnal() {
        DataDao dao = mdb.createDao(DataDao.class)
        dao.deleteObjWithProperties(1009)
    }



    @Test
    void jsonrpc1() throws Exception {
        Map<String, Object> map = apx.execJsonRpc("api", "data/loadPersonnal", [0])
        mdb.outMap(map.get("result") as Map)
    }

}
