package tofi.apinator;

import jandcode.commons.conf.IConfLink;
import jandcode.commons.named.NamedList;
import jandcode.core.Comp;

import java.util.Set;

public interface ApinatorApiItem extends Comp, IConfLink {

    /**
     * Какому api принадлежит
     */
    ApinatorApi getApi();

    Class getClsIntf();

    Class getClsImpl();

    NamedList<ApinatorMethod> getMethods();

    Set<String> getMethodNames();

}
