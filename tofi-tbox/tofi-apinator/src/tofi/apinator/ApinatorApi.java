package tofi.apinator;

import jandcode.commons.conf.IConfLink;
import jandcode.commons.named.NamedList;
import jandcode.core.Comp;

public interface ApinatorApi extends Comp, IConfLink {

    ApinatorInvoker getInvoker();

    NamedList<ApinatorApiItem> getItems();

    /**
     * Получить экземпляр для интерфейса
     *
     * @param clsIntfName имя интерфейса
     */
    Object get(String clsIntfName);

    /**
     * Получить экземпляр для интерфейса
     *
     * @param clsIntf имя интерфейса
     */
    <A> A get(Class<A> clsIntf);

}
