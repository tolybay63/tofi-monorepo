package tofi.apinator;

import jandcode.core.test.App_Test;
import org.junit.jupiter.api.Test;
import tofi.apinator.api1.Task1;

public class ApinatorApi_Test extends App_Test {

    @Test
    public void test1() throws Exception {
        utils.logOn();
        
        ApinatorApi api = app.bean(ApinatorService.class).getApi("api1");

        Task1 t = api.get(Task1.class);

        t.m1();
    }

}
