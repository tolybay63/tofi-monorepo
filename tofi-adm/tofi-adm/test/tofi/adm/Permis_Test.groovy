package tofi.adm

import de.mkammerer.argon2.Argon2
import de.mkammerer.argon2.Argon2Factory
import jandcode.commons.UtString
import jandcode.core.apx.test.Apx_Test
import org.junit.jupiter.api.Test

class Permis_Test extends Apx_Test {

    private final Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);

    @Test
    void test1() {
        String psw = "Fish@2026"
        //String psw = "Qwerty123!"
        String pswHash = argon2.hash(3, 65536, 4, psw.toCharArray());
        boolean pswValid = argon2.verify(pswHash, psw.toCharArray());
        println(pswHash)
        println(pswValid)
        //
        String tst = '$argon2id$v=19$m=65536,t=3,p=4$IRaAm4sXLb4MNFuDR9ouzw$o8ohVPblvfwvBr3Fz79IrF9d2wuf/qqLiqp19vTRa1Y'

        pswValid = argon2.verify(tst, psw.toCharArray());
        println(pswValid)

//$argon2id$v=19$m=65536,t=3,p=4$IRaAm4sXLb4MNFuDR9ouzw$o8ohVPblvfwvBr3Fz79IrF9d2wuf/qqLiqp19vTRa1Y
//$argon2id$v=19$m=65536,t=3,p=4$II2ZubTT5wObDQmI5LCD0w$YnJTvpAx3iRbVNP0ogCMv430VPMa8HYRVAbykoS1STI

    }

    @Test
    void test2() {
        String psw = "Qwerty123!"
        String pswHash = UtString.md5Str(psw)
        println(pswHash)

    }


}
