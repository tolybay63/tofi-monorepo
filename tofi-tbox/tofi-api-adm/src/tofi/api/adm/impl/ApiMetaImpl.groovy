package tofi.api.adm.impl


import jandcode.core.dbm.mdb.BaseMdbUtils
import jandcode.core.std.CfgService
import tofi.api.adm.ApiMeta
import tofi.apinator.ApinatorApi
import tofi.apinator.ApinatorService

class ApiMetaImpl extends BaseMdbUtils implements ApiMeta {
    ApinatorApi apiMeta() { return app.bean(ApinatorService).getApi("meta") }

    @Override
    String getIdMetaModel() {
        CfgService cfgSvc = getMdb().getApp().bean(CfgService.class);
        return cfgSvc.getConf().getString("dbsource/meta/id");
    }


}