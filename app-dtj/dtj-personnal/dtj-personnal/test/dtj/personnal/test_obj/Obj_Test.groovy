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
        map.put("login", "user_dtj2")
        map.put("passwd", "111")
        map.put("UserEmail", "user_dtj2@gmail.com")
        map.put("UserPhone", "7773334455")
        map.put("UserFirstName", "Иван2")
        map.put("UserSecondName", "Иванов2")
        map.put("TabNumber", "123456789")
        map.put("CreatedAt", "2025-07-27")
        map.put("UserDateBirth", "2020-07-27")
        map.put("fvUserSex", 1088)
        map.put("pvUserSex", 1085)
        map.put("fvPosition", 1130)
        map.put("pvPosition", 1246)
        map.put("objLocation", 1000)
        map.put("pvLocation", 1040)

        //
        savePersonnal("ins", map)
    }


    @Test
    void savePersonnalUpd() {
        Store st = loadPersonnal()
        Map<String, Object> map = st.get(0).getValues()
        map.put("UserSecondName", "New Value")
        savePersonnal("upd", map)
    }


    private void savePersonnal(String mode, Map<String, Object> params) {
        DataDao dao = mdb.createDao(DataDao.class)
        Store st = dao.savePersonnal(mode, params)
        mdb.outTable(st)
    }



    @Test
    void jsonrpc1() throws Exception {
        Map<String, Object> map = apx.execJsonRpc("api", "data/loadPersonnal", [0])
        mdb.outMap(map.get("result") as Map)
    }

}
