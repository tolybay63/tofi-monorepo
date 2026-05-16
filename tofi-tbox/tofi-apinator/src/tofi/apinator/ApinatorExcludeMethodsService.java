package tofi.apinator;

import jandcode.core.Comp;

import java.util.Set;

public interface ApinatorExcludeMethodsService extends Comp {

    /**
     * Набор имен методов, которые исключаются из api
     */
    Set<String> getMethods();

}
