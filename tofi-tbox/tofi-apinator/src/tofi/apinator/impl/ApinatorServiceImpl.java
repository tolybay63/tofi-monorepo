package tofi.apinator.impl;

import jandcode.commons.UtConf;
import jandcode.commons.conf.Conf;
import jandcode.commons.named.DefaultNamedList;
import jandcode.commons.named.NamedList;
import jandcode.core.BaseComp;
import jandcode.core.BeanConfig;
import tofi.apinator.ApinatorApi;
import tofi.apinator.ApinatorInvoker;
import tofi.apinator.ApinatorLogger;
import tofi.apinator.ApinatorService;

public class ApinatorServiceImpl extends BaseComp implements ApinatorService {

    private final NamedList<ApinatorApi> apis = new DefaultNamedList<>("api not found: {0}");
    private ApinatorLogger logger;

    protected void onConfigure(BeanConfig cfg) throws Exception {
        super.onConfigure(cfg);

        //
        Conf rootConf = getApp().getConf().getConf("apinator");

        // logger
        this.logger = (ApinatorLogger) getApp().create(rootConf.getConf("logger/default"));

        // api
        for (Conf apiConf : rootConf.getConfs("api")) {
            // invoker
            String invokerName = apiConf.getString("invoker", "base");
            Conf invokerConf = rootConf.getConf("invoker/" + invokerName);
            Conf invokerConf2 = UtConf.expandInclude(rootConf, invokerConf, "invoker");
            ApinatorInvoker invoker = getApp().create(invokerConf2, ApinatorInvokerImpl.class, true);
            // api
            ApinatorApiImpl it = getApp().create(apiConf, ApinatorApiImpl.class);
            it.setInvoker(invoker);
            apis.add(it);
        }

    }

    public NamedList<ApinatorApi> getApis() {
        return apis;
    }

    public ApinatorApi getApi(String name) {
        return apis.get(name);
    }

    public ApinatorLogger getLogger() {
        return logger;
    }

}
