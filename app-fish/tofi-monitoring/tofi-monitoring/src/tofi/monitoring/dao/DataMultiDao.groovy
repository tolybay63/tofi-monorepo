package tofi.monitoring.dao

import jandcode.commons.UtCnv
import jandcode.commons.datetime.XDateTime
import jandcode.commons.error.XError
import jandcode.core.auth.AuthService
import jandcode.core.dao.DaoMethod
import jandcode.core.dbm.mdb.BaseMdbUtils
import jandcode.core.store.Store
import jandcode.core.store.StoreIndex
import jandcode.core.store.StoreRecord
import tofi.api.mdl.ApiMeta
import tofi.apinator.ApinatorApi
import tofi.apinator.ApinatorService

class DataMultiDao extends BaseMdbUtils {
    ApinatorApi apiMeta() {
        return app.bean(ApinatorService).getApi("meta")
    }

    Store getStoreDim(String fields, Map<String, Object> params) {
        String sqlDMPI = """
            select  
                dmpi.id as dimMultiPropItem, mpd.id as multiPropDim, dmpi.name ${fields}
            from typchargr t
                inner join typchargrprop p on t.id=p.typchargr and p.multiprop is not null
                inner join Cls c on t.typ=c.typ
                inner join clsfactorval cf on cf.cls=c.id and cf.factorval=t.factorval
                left join MultiPropDim mpd on mpd.multiprop=p.multiprop
                left join DimMultiProp dmp on dmp.id=mpd.dimmultiprop
                left join DimMultiPropItem dmpi on dmpi.dimmultiprop=dmp.id
            where c.id=:cls and mpd.dimMultiProp=:dimMultiProp
            order by dmpi.id
        """
        return loadSqlMetaWithParams(sqlDMPI, "", params)
    }

    @DaoMethod
    Map<String, Object> colsZooplankton(String codMP, long cls) {
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("MultiProp", codMP, "")
        if (map.isEmpty())
            throw new XError("NotFoundCod@${codMP}")

        String dimName = "Группа зоопланктона"
        if (codMP=="MP_Zoobenthos")
            dimName = "Группа бентоса"


        Store stDim = loadSqlMeta("""
            select dimMultiProp from MultiPropDim
            where multiProp=${map.get(codMP)} order by dimnumber
        """, "")
        String flds = ""
        def params = [cls: cls, dimMultiProp: stDim.get(1).getLong("dimMultiProp")]
        Store stCol = getStoreDim(flds, params)

        List<Map<String, String>> lstCols = new ArrayList<>()
        lstCols.add([
            name: "name", field: "name", label:dimName,
            align: "left", classes: "bg-blue-grey-1", headerStyle: "font-size: 1.2em; width:40%"
        ])
        for (StoreRecord r in stCol) {
            Map<String, String> m = new HashMap<>()
            m.put("name", "fld_"+r.getString("dimMultiPropItem")+"_"+r.getString("multiPropDim"))
            m.put("field", "fld_"+r.getString("dimMultiPropItem")+"_"+r.getString("multiPropDim"))
            m.put("label", r.getString("name"))
            m.put("align", "left")
            m.put("classes", "bg-blue-grey-1")
            m.put("headerStyle", "font-size: 1.2em; width:20%",)
            lstCols.add(m)
        }

        Map<String, Object> res = new HashMap<>()
        res.put("cols", lstCols)
        //
        return res
    }

    @DaoMethod
    Store loadZooplankton(String codMP, long cls, long obj) {
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("MultiProp", codMP, "")
        if (map.isEmpty())
            throw new XError("NotFoundCod@${codMP}")

        Store stDim = loadSqlMeta("""
            select dimMultiProp from MultiPropDim
            where multiProp=${map.get(codMP)} order by dimnumber
        """, "")

        String flds = ""
        def params = [cls: cls, dimMultiProp: stDim.get(1).getLong("dimMultiProp")]
        Store stCol = getStoreDim(flds, params)

        List<String> lstFields = new ArrayList<>()
        for (StoreRecord r in stCol) {
            lstFields.add("0 as id_"+r.getString("dimMultiPropItem")+"_"+r.getString("multiPropDim"))
            lstFields.add("null as fld_"+r.getString("dimMultiPropItem")+"_"+r.getString("multiPropDim"))
        }
        flds = "," + lstFields.join(",")
        //
        params = [cls: cls, dimMultiProp: stDim.get(0).getLong("dimMultiProp")]
        Store st = getStoreDim(flds, params)
        // Data
        Store stData = mdb.loadQuery("""
            select c.id as idCell, p.objorrelobj as obj, c.numberval as val, c.measure, t.ind
            from DataMultiProp p
                left join DataMultiPropCell c on p.id=c.datamultiprop 
                left join (
                    select dataMultiPropCell,
                        STRING_AGG (cast(dimmultipropitem as varchar(1000)), '_') as ind
                    from DataMultiPropCellCoord
                    group by dataMultiPropCell
                ) t on t.dataMultiPropCell=c.id
            where p.isobj=1 and p.objorrelobj=${obj} and p.multiprop=${map.get(codMP)}
        """)
        StoreIndex stIndex = stData.getIndex("ind")

        def cols = []
        int idx = 0
        for (StoreRecord r in st) {
            for (def fld in r.getStore().fields) {
                if (fld.name.startsWith("fld_")) {
                    cols.add(idx++, fld.name)
                    def ind2 = fld.name.split("_")[1]
                    def ind = r.getString("dimMultiPropItem") + "_"+ind2
                    StoreRecord rec = stIndex.get(ind)
                    if (rec != null) {
                        r.set(fld.name, rec.getDouble("val"))
                        def id = fld.name.replace("fld", "id")
                        r.set(id, rec.getLong("idCell"))
                    }
                }
            }
        }

        double s1=0
        double s2=0
        double s3=0
        boolean b1=false
        boolean b2=false
        boolean b3=false

        for (StoreRecord r in st) {
            if (r.getString("name")=="Всего")
                continue
            if (r.get(cols.get(0) as String)) {
                s1 = s1 + r.getDouble(cols.get(0) as String)
                b1 = true
            }
            if (r.get(cols.get(1) as String)) {
                s2 = s2 + r.getDouble(cols.get(1) as String)
                b2 = true
            }
            if (r.get(cols.get(2) as String)) {
                s3 = s3 + r.getDouble(cols.get(2) as String)
                b3 = true
            }
        }

        if (b1)
            st.get(st.size()-1).set(cols.get(0) as String, s1)
        if (b2)
            st.get(st.size()-1).set(cols.get(1) as String, s2)
        if (b3)
            st.get(st.size()-1).set(cols.get(2) as String, s3)

        //mdb.outTable(st)

        return st
    }

    @DaoMethod
    void saveResultZooplankton(Map<String, Object> params) {
        String codMP = UtCnv.toString(params.get("codMP"))
        params.putIfAbsent("dbeg", "1800-01-01")
        params.putIfAbsent("dend", "3333-12-01")
        String d1 = UtCnv.toString(params.get("dbeg"))
        String d2 = UtCnv.toString(params.get("dbeg"))
        //Double val = null
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("MultiProp", codMP, "")
        if (map.isEmpty())
            throw new XError("NotFoundCod@${codMP}")
        def dmpcell = UtCnv.toLong(params.get("dmpcell"))
        //todo ??? kfrombase ???
        def koef = UtCnv.toDouble(params.get("kfrombase"))
        if (koef == 0) koef = 1


        def upd = dmpcell > 0

        if (!upd) {
            StoreRecord recDMP = mdb.createStoreRecord("DataMultiProp")
            recDMP.set("isObj", 1)
            recDMP.set("objOrRelObj", params.get("obj"))
            recDMP.set("multiProp", map.get(codMP))
            long idDMP = mdb.insertRec("DataMultiProp", recDMP, true)
            //
            StoreRecord recDMPC = mdb.createStoreRecord("DataMultiPropCell")
            def au = mdb.getApp().bean(AuthService.class).currentUser.attrs.getLong("id")
            if (au==0)
                throw new XError("notLogined")
            def inputType = 2L
            def measure = params.get("measure")
            recDMPC.set("dataMultiProp", idDMP)
            recDMPC.set("entityType", 1L)
            recDMPC.set("measure", measure)
            def val = UtCnv.toDouble(params.get("val")) / koef
            if (params.containsKey("digit")) {
                val = val.round(UtCnv.toInt(params.get("digit")))
            }
            recDMPC.set("numberVal", val)
            recDMPC.set("inputType", inputType)
            recDMPC.set("authUser", au)
            recDMPC.set("dbeg", d1)
            recDMPC.set("dend", d2)
            recDMPC.set("timeStamp", XDateTime.now())

            dmpcell = mdb.insertRec("DataMultiPropCell", recDMPC, true)
            //

            StoreRecord recDMPCC = mdb.createStoreRecord("DataMultiPropCellCoord")
            recDMPCC.set("dataMultiPropCell", dmpcell)
            recDMPCC.set("multiPropDim", params.get("r_mpd")) //? multiPropDim
            recDMPCC.set("dimMultiPropItem", params.get("r_dmpi"))
            mdb.insertRec("DataMultiPropCellCoord", recDMPCC, true)
            //
            recDMPCC = mdb.createStoreRecord("DataMultiPropCellCoord")
            recDMPCC.set("dataMultiPropCell", dmpcell)
            recDMPCC.set("multiPropDim", params.get("c_mpd")) //? multiPropDim
            recDMPCC.set("dimMultiPropItem", params.get("c_dmpi"))
            mdb.insertRec("DataMultiPropCellCoord", recDMPCC, true)
        } else {
            def val = UtCnv.toDouble(params.get("val")) / koef
            if (params.containsKey("digit")) {
                val = val.round(UtCnv.toInt(params.get("digit")))
            }
            StoreRecord recDMPC = mdb.loadQueryRecord(""" 
                select * from DataMultiPropCell where id=${dmpcell}
            """)
            recDMPC.set("numberVal", val)
            recDMPC.set("timeStamp", XDateTime.now())
            mdb.updateRec("DataMultiPropCell", recDMPC)
        }
    }

    @DaoMethod
    void deleteZooplankton(long idCell) {
        mdb.execQuery("""
            delete from DataMultiPropCellCoord where dataMultiPropCell=${idCell};
            delete from DataMultiPropCell where id=${idCell};
            delete from DataMultiProp where id in (
                select id from DataMultiProp
                except
                select DataMultiProp as id from DataMultiPropCell
            );
        """)
    }

    private Store loadSqlMeta(String sql, String domain) {
        return apiMeta().get(ApiMeta).loadSql(sql, domain)
    }

    private Store loadSqlMetaWithParams(String sql, String domain, Map<String, Object> params) {
        return apiMeta().get(ApiMeta).loadSqlWithParams(sql, domain, params)
    }
}
