package tofi.apinator.impl;

import jandcode.commons.UtClass;
import jandcode.commons.UtString;
import jandcode.commons.conf.Conf;
import jandcode.commons.error.XError;
import jandcode.commons.named.DefaultNamedList;
import jandcode.commons.named.NamedList;
import jandcode.core.BaseComp;
import tofi.apinator.ApinatorApi;
import tofi.apinator.ApinatorApiItem;
import tofi.apinator.ApinatorMethod;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class ApinatorApiItemImpl extends BaseComp implements ApinatorApiItem {

    private final ApinatorApi api;
    private final Class clsIntf;
    private final Class clsImpl;
    private final NamedList<ApinatorMethod> methods = new DefaultNamedList<>();
    private final Set<String> methodNames = new HashSet<>();
    private final Conf conf;

    public ApinatorApiItemImpl(ApinatorApi api, Conf conf, Set<String> excludeMethods) {
        this.api = api;
        this.conf = conf;
        String clsIntfName = conf.getName();
        String clsImplName = conf.getString("class");

        this.clsIntf = UtClass.getClass(clsIntfName);
        if (UtString.empty(clsImplName)) {
            this.clsImpl = this.clsIntf;
        } else {
            this.clsImpl = UtClass.getClass(clsImplName);
        }

        if (!this.clsIntf.isAssignableFrom(this.clsImpl)) {
            throw new XError("Несовместимые классы: {0} и {1}", this.clsIntf.getName(), this.clsImpl.getName());
        }

        setName(clsIntfName);

        // методы
        MethodGrabber mg = new MethodGrabber(this.clsIntf, excludeMethods);
        for (Method m : mg.getMethods().values()) {
            ApinatorMethod it = new ApinatorMethodImpl(m);
            this.methods.add(it);
            this.methodNames.add(it.getName());
        }
    }

    public Conf getConf() {
        return conf;
    }

    public ApinatorApi getApi() {
        return api;
    }

    public Class getClsIntf() {
        return clsIntf;
    }

    public Class getClsImpl() {
        return clsImpl;
    }

    public NamedList<ApinatorMethod> getMethods() {
        return methods;
    }

    public Set<String> getMethodNames() {
        return methodNames;
    }

    @Override
    public String toString() {
        return "ApinatorApiItem{" +
                "clsIntf=" + clsIntf +
                ", clsImpl=" + clsImpl +
                '}';
    }
}
