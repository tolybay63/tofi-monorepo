package tofi.apinator.impl;

import jandcode.commons.UtClass;
import jandcode.commons.conf.Conf;
import jandcode.core.BaseComp;
import jandcode.core.BeanConfig;
import tofi.apinator.ApinatorExcludeMethodsService;

import java.util.Set;

public class ApinatorExcludeMethodsServiceImpl extends BaseComp implements ApinatorExcludeMethodsService {

    private final ExcludeMethodsHolder excludeMethodsHolder = new ExcludeMethodsHolder();

    protected void onConfigure(BeanConfig cfg) throws Exception {
        super.onConfigure(cfg);
        //
        Conf rootConf = getApp().getConf().getConf("apinator");

        //
        for (Conf x : rootConf.getConfs("excludeMethods")) {
            String nm = x.getName();
            if (nm.contains(".")) {
                try {
                    this.excludeMethodsHolder.addClass(UtClass.getClass(nm));
                } catch (Exception e) {
                    // ignore
                    // возможно такого класса и нет, если не подключена какая нибудь jar
                }
            } else {
                this.excludeMethodsHolder.addMethod(nm);
            }
        }

    }

    public Set<String> getMethods() {
        return excludeMethodsHolder.getMethods();
    }
}
