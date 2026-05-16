package tofi.apinator;

import jandcode.commons.named.NamedList;
import jandcode.core.Comp;

public interface ApinatorService extends Comp {

    NamedList<ApinatorApi> getApis();

    ApinatorApi getApi(String name);

    ApinatorLogger getLogger();

}
