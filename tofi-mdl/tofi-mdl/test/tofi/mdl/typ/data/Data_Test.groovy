package tofi.mdl.typ.data

import jandcode.core.apx.test.Apx_Test
import jandcode.core.store.StoreRecord
import org.junit.jupiter.api.Test
import tofi.mdl.model.utils.CartesianProduct

class Data_Test extends Apx_Test {


    @Test
    void test1() {
        List<String> lst1 = List.of("f1", "f2", "f3")
        List<String> lst2 = List.of("g1", "g2")
        List<String> lst3 = List.of("p1", "p2", "p3")
        //
        List<List<String>> lstlst = new ArrayList<>()
        lstlst.add(lst1)
        lstlst.add(lst2)
        lstlst.add(lst3)
        //
        List<List<String>> res = CartesianProduct.result(lstlst)

        println(res)
        println(res.size())
    }

}
