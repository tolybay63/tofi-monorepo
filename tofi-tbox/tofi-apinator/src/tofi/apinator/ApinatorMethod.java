package tofi.apinator;

import jandcode.commons.named.INamed;
import jandcode.commons.named.NamedList;

import java.lang.reflect.Method;

/**
 * Описание метода
 */
public interface ApinatorMethod extends INamed {

    /**
     * Ссылка на реальный java-метод
     */
    Method getMethod();

    /**
     * Параметры метода
     */
    NamedList<ApinatorMethodParam> getParams();

}
