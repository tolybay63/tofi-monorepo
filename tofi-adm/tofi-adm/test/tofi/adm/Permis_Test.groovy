package tofi.adm

import de.mkammerer.argon2.Argon2
import de.mkammerer.argon2.Argon2Factory
import jandcode.core.apx.test.Apx_Test
import org.junit.jupiter.api.Test

import java.awt.font.TextMeasurer

class Permis_Test extends Apx_Test {

    private final Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);

    @Test
    void test1() {
        String psw = "Fish@2026"
        //String psw = "Qwerty123!"
        String pswHash = argon2.hash(3, 65536, 4, psw.toCharArray());
        println(pswHash)


    }

}
