package fish.monitoring.dao

import groovy.transform.CompileStatic
import jandcode.commons.UtCnv
import jandcode.commons.UtFile
import jandcode.commons.conf.Conf
import jandcode.commons.datetime.XDate
import jandcode.commons.datetime.XDateTime
import jandcode.commons.datetime.XDateTimeFormatter
import jandcode.commons.error.XError
import jandcode.commons.variant.IVariantMap
import jandcode.commons.variant.VariantMap
import jandcode.core.auth.AuthService
import jandcode.core.auth.AuthUser
import jandcode.core.dao.DaoMethod
import jandcode.core.dbm.mdb.BaseMdbUtils
import jandcode.core.dbm.mdb.Mdb
import jandcode.core.std.DataDirService
import jandcode.core.store.Store
import jandcode.core.store.StoreField
import jandcode.core.store.StoreIndex
import jandcode.core.store.StoreRecord
import tofi.api.dta.ApiMonitoringData
import tofi.api.dta.ApiNSIData
import tofi.api.dta.ApiUserData
import tofi.api.dta.model.utils.EntityMdbUtils
import tofi.api.dta.model.utils.PeriodGenerator
import tofi.api.dta.model.utils.UtPeriod
import tofi.api.mdl.ApiMeta
import tofi.api.mdl.ApiMetaFish
import tofi.api.mdl.model.consts.*
import tofi.api.mdl.utils.dbfilestorage.DbFileStorageItem
import tofi.api.mdl.utils.dbfilestorage.DbFileStorageService
import tofi.apinator.ApinatorApi
import tofi.apinator.ApinatorService

import java.nio.file.Files
import java.nio.file.Paths

@CompileStatic
class DataDao extends BaseMdbUtils {

    DataDao() {

    }

    Mdb mdb

    DataDao(Mdb mdb) {
        this.mdb = mdb
    }

    ApinatorApi apiMeta() {
        return app.bean(ApinatorService).getApi("meta")
    }

    ApinatorApi apiMetaFish() {
        return app.bean(ApinatorService).getApi("meta")
    }

    ApinatorApi apiUserData() {
        return app.bean(ApinatorService).getApi("userdata")
    }

    ApinatorApi apiNSIData() {
        return app.bean(ApinatorService).getApi("nsidata")
    }

    ApinatorApi apiMonitoringData() {
        return app.bean(ApinatorService).getApi("monitoringdata")
    }

    //-------------------------

    private void checkForExistData(long id, int isObj) {
        if (isObj == 1) {
            // 1 Родитель ?
            Store stTmp = mdb.loadQuery("""
                select distinct ov1.name
                from Obj o
                    left join ObjVer ov on o.id=ov.ownerver and ov.lastver=1
                    left join ObjVer ov1 on ov1.ownerVer=ov.objParent
                where ov.objParent=${id}
            """)
            if (stTmp.size() > 0) {
                throw new XError("Объект [" + stTmp.get(0).getString("name") + "] имеет дочерные элементы")
            }
            //2 Участник ?
            stTmp = mdb.loadQuery("""
                select ov.name as nm1, rv.name as nm
                from relobjmember m
                    inner join ObjVer ov on m.obj=ov.ownerver and ov.lastver=1
                    left join RelObjVer rv on m.relobj=rv.ownerver and rv.lastver=1
                where m.obj=${id}
            """)
            if (stTmp.size() > 0) {
                throw new XError("Объект [" + stTmp.get(0).getString("nm1") + "] является участником отношения [" + stTmp.get(0).getString("nm") + "]")
            }
            //
            //3 Объект имеет значение?

/*
            stTmp = mdb.loadQuery("""
                select ov.name nm1, d.prop, d.periodType, v.dbeg, v.dend
                from DataProp d
                    left join DataPropVal v on d.id=v.dataprop
                    inner join ObjVer ov on d.isObj=1 and d.objorrelobj=ov.ownerver and ov.lastver=1
                where d.isObj=1 and d.objorrelobj=${id} and v.obj is null and v.relobj is null
            """)
            if (stTmp.size() > 0) {
                String periodName = " за [" + stTmp.get(0).getString("dbeg") + " - " + stTmp.get(0).getString("dend") + "]"
                if (stTmp.get(0).getLong("periodType") > 0) {
                    PeriodGenerator pg = new PeriodGenerator()
                    periodName = " за " + pg.getPeriodName(stTmp.get(0).getDate("dbeg"), stTmp.get(0).getDate("dend"), stTmp.get(0).getLong("periodType"), 3)
                }
                Store stProp = loadSqlMeta("""
                    select name from Prop where id=${stTmp.get(0).getLong("prop")}
                """, "")
                throw new XError("Имеется значения свойства [" + stProp.get(0).getString("name") + "] объекта [" + stTmp.get(0).getString("nm1") + "]" + periodName)
            }
*/

            //3 Объект/отношение является значением объекта?
            stTmp = mdb.loadQuery("""
                select  
                    ov1.name nm1, d.prop, v.obj, v.relobj, d.periodType, v.dbeg, v.dend,
                    case when v.obj is not null then ov2.name when v.relobj is not null then rv2.name end as nm2
                from DataProp d
                    left join DataPropVal v on d.id=v.dataprop
                    inner join ObjVer ov1 on d.isObj=1 and d.objorrelobj=ov1.ownerver and ov1.lastver=1
                    left join ObjVer ov2 on v.obj is not null and v.obj=ov2.ownerver and ov2.lastver=1
                    left join RelObjVer rv2 on v.relobj is not null and v.relobj=rv2.ownerver and rv2.lastver=1
                where d.isObj=1 and (v.obj=${id} or v.relobj=${id})
            """)
            if (stTmp.size() > 0) {
                String nmOR = stTmp.get(0).getLong("obj") > 0 ? "Объект [" : "Отношение ["
                String periodName = " за [" + stTmp.get(0).getString("dbeg") + " - " + stTmp.get(0).getString("dend") + "]"
                if (stTmp.get(0).getLong("periodType") > 0) {
                    PeriodGenerator pg = new PeriodGenerator()
                    periodName = " за " + pg.getPeriodName(stTmp.get(0).getDate("dbeg"), stTmp.get(0).getDate("dend"), stTmp.get(0).getLong("periodType"), 3)
                }
                Store stProp = loadSqlMeta("""
                    select name from Prop where id=${stTmp.get(0).getLong("prop")}
                """, "")
                throw new XError(nmOR + stTmp.get(0).getString("nm2") + "] является значением свойства [" + stProp.get(0).getString("name") + "] объекта [" + stTmp.get(0).getString("nm1") + "]" + periodName)
            }

        } else {
            //1 Отношение имеет значение?
            Store stTmp = mdb.loadQuery("""
                select ov.name nm1, d.prop, d.periodType, v.dbeg, v.dend
                from DataProp d
                    left join DataPropVal v on d.id=v.dataprop
                    inner join RelObjVer ov on d.isObj=0 and d.objorrelobj=ov.ownerver and ov.lastver=1
                where d.isObj=0 and d.objorrelobj=${id} and v.obj is null and v.relobj is null
            """)
            if (stTmp.size() > 0) {
                String periodName = " за [" + stTmp.get(0).getString("dbeg") + " - " + stTmp.get(0).getString("dend") + "]"
                if (stTmp.get(0).getLong("periodType") > 0) {
                    PeriodGenerator pg = new PeriodGenerator()
                    periodName = " за " + pg.getPeriodName(stTmp.get(0).getDate("dbeg"), stTmp.get(0).getDate("dend"), stTmp.get(0).getLong("periodType"), 3)
                }
                Store stProp = loadSqlMeta("""
                    select name from Prop where id=${stTmp.get(0).getLong("prop")}
                """, "")
                throw new XError("Имеется значения свойства [" + stProp.get(0).getString("name") + "] отношения [" + stTmp.get(0).getString("nm1") + "]" + periodName)
            }
            //2 Объект/отношение является значением отношения?
            stTmp = mdb.loadQuery("""
                select rv1.name nm1, d.prop, v.obj, v.relobj, d.periodType, v.dbeg, v.dend, 
                    case when v.obj is not null then ov2.name when v.relobj is not null then rv2.name end as nm2
                from DataProp d
                    left join DataPropVal v on d.id=v.dataprop
                    inner join RelObjVer rv1 on d.isObj=0 and d.objorrelobj=rv1.ownerver and rv1.lastver=1
                    left join ObjVer ov2 on v.obj is not null and v.obj=ov2.ownerver and ov2.lastver=1
                    left join RelObjVer rv2 on v.relobj is not null and v.relobj=rv2.ownerver and rv2.lastver=1
                where d.isObj=0 and d.objorrelobj is not null and (v.obj=${id} or v.relobj=${id})
            """)
            if (stTmp.size() > 0) {
                String nmOR = stTmp.get(0).getLong("obj") > 0 ? "Объект [" : "Отношение ["
                String periodName = " за [" + stTmp.get(0).getString("dbeg") + " - " + stTmp.get(0).getString("dend") + "]"
                if (stTmp.get(0).getLong("periodType") > 0) {
                    PeriodGenerator pg = new PeriodGenerator()
                    periodName = " за " + pg.getPeriodName(stTmp.get(0).getDate("dbeg"), stTmp.get(0).getDate("dend"), stTmp.get(0).getLong("periodType"), 3)
                }
                Store stProp = loadSqlMeta("""
                    select name from Prop where id=${stTmp.get(0).getLong("prop")}
                """, "")
                throw new XError(nmOR + stTmp.get(0).getString("nm2") + "] является значением свойства [" + stProp.get(0).getString("name") + "] отношения [" + stTmp.get(0).getString("nm1") + "]" + periodName)
            }
        }
    }

    /*
        delete Owner with properties
    */

    @DaoMethod
    void deleteOwnerWithProperties(long id, int isObj) {
        String tableName = isObj == 1 ? "Obj" : "RelObj"
        //
        checkForExistData(id, isObj)
        //
        EntityMdbUtils eu = new EntityMdbUtils(mdb, tableName)
        mdb.execQueryNative("""
            delete from DataPropVal
            where dataProp in (select id from DataProp where isobj=${isObj} and objorrelobj=${id});
            delete from DataProp where id in (
                select id from dataprop
                except
                select dataProp as id from DataPropVal
            );
        """)
        if (tableName.equalsIgnoreCase("RelObj")) {
            try {
                mdb.execQueryNative("""
                    delete from RelObjMember
                    where relobj=${id};
                """)
            } finally {
                eu.deleteEntity(id)
            }
        } else
            eu.deleteEntity(id)
    }


/*
    @DaoMethod
    Map<String, Long> getClsIds(String codCls) {
        if (codCls == "")
            return apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", "", "Cls_%")
        else
            return apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", codCls, "")
    }

    @DaoMethod
    Store loadObjWithCls(String codTyp) {
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Typ", codTyp, "")
        if (map.isEmpty())
            throw new XError("NotFoundCod@${codTyp}")

        Set<Object> setIdsCls = apiMeta().get(ApiMeta).setIdsOfCls(codTyp)
        if (setIdsCls.empty) setIdsCls.add(0L)
        String whe = setIdsCls.join(",")
        Store st = mdb.createStore("Obj.cust")
        mdb.loadQuery(st, """
            select o.id, v.objParent as parent, o.accesslevel, o.cls, v.name, null as nameCls, v.cmtver as cmt
            from Obj o, ObjVer v
            where o.id=v.ownerver and v.lastver=1 and o.cls in (${whe})
            order by o.ord
        """)
        Store stCls = apiMeta().get(ApiMeta).loadCls(codTyp)
        StoreIndex indCls = stCls.getIndex("id")
        for (StoreRecord r in st) {
            StoreRecord rec = indCls.get(r.getLong("cls"))
            if (rec != null) {
                r.set("nameCls", rec.getString("name"))
            }
        }
        return st
    }

    Store loadObjWithClsRec(long obj) {
        Store st = mdb.createStore("Obj.cust")
        mdb.loadQuery(st, """
            select o.id, v.objParent as parent, o.accesslevel, o.cls, v.name, null as nameCls, v.cmtver as cmt
            from Obj o, ObjVer v
            where o.id=v.ownerver and v.lastver=1 and o.id=${obj}
            order by o.ord
        """)

        Store stCls = apiMeta().get(ApiMeta).recEntity("Cls", st.get(0).getLong("cls"))
        String nameCls = stCls.get(0).getString("name")
        for (StoreRecord r in st) {
            r.set("nameCls", nameCls)
        }
        return st
    }

*/

    @DaoMethod
    Store loadClsForSelect(String codTyp) {
        return apiMeta().get(ApiMeta).loadClsForSelect(codTyp)
    }


    @DaoMethod
    Store loadAttachedFiles(long obj) {
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "Prop_RegDocumentsFile", "")
        if (map.isEmpty())
            throw new XError("NotFoundCod@Prop_RegDocumentsFile")
        map.put("id", obj)
        Store st = mdb.createStore("Obj.file")
        mdb.loadQuery(st, """
            select d.objorrelobj as obj, v.id as idDPV, v.fileVal, null as fileName, v.cmt
            from DataProp d, DataPropVal v 
            where d.id=v.dataprop and d.isObj=1 and d.objorrelobj=:id and d.prop=:Prop_RegDocumentsFile
        """, map)
        Set<Object> ids = st.getUniqueValues("fileVal")
        if (ids.isEmpty()) ids.add(0L)
        String whe = ids.join(",")
        Store stFS = apiMeta().get(ApiMeta).loadSql("""
            select id, originalfilename as filename from DbFileStorage where id in (${whe})
        """, "")
        StoreIndex indFS = stFS.getIndex("id")
        for (StoreRecord r : st) {
            StoreRecord rr = indFS.get(r.getLong("fileVal"))
            if (rr != null) {
                r.set("fileName", rr.getString("filename"))
            }
        }
        return st
    }

    @DaoMethod
    void deleteFileValue(Map<String, Object> rec) {
        String path
        try {
            path = mdb.getApp().bean(DataDirService.class).getPath("dbfilestorage")
        } catch (Exception e) {
            path = ""
            e.printStackTrace()
        }

        String bucketName = ""
        if (path == "") {
            try {
                Conf conf2 = mdb.getApp().getConf().getConf("datadir/minio")
                bucketName = conf2.getString("bucketName")
            } catch (Exception e) {
                bucketName = ""
                e.printStackTrace()
            }
        }

        if (path != "" && bucketName == "") {
            deleteFileValueFS(rec)
        } else if (path == "" && bucketName != "") {
            deleteFileValueMinio(rec)
        } else {
            throw new XError("FileStorage не настроен!")
        }
    }

    private void deleteFileValueFS(Map<String, Object> params) {
        long fileId = UtCnv.toLong(params.get("fileVal"))
        long id = UtCnv.toLong(params.get("idDPV"))

        try {
            DbFileStorageService dfsrv = apiMeta().get(ApiMeta).getDbFileStorageService()
            dfsrv.setModelName(UtCnv.toString(params.get("model")))
            dfsrv.removeFile(fileId)
        } finally {
            String sql = """
                delete from DataPropVal where id=${id};
                with d as (
                    select id from DataProp
                    except
                    select dataProp as id from DataPropVal
                )
                delete from DataProp where id in (select id from d);
            """
            execSql(sql, UtCnv.toString(params.get("model")))
        }
    }

    private static void deleteFileValueMinio(Map<String, Object> params) {
        throw new XError("MinIO не настроен!")
    }

    @DaoMethod
    Store loadFishFamily(Map<String, Object> params) {
        String codCls = UtCnv.toString(params.get("codCls"))
        boolean isRec = UtCnv.toBoolean(params.get("isRec"))
        long idObj = UtCnv.toLong(params.get("idObj"))
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "Prop_FishFamily", "")
        if (map.isEmpty())
            throw new XError("NotFoundCod@Prop_FishFamily")
        String whe = "o.cls = :${codCls}"
        if (!isRec) {
            Map<String, Long> map2 = apiMeta().get(ApiMeta).getIdsFromCodOfEntity("Cls", "'${codCls}'")
            if (map2.isEmpty())
                throw new XError("NotFoundCod@${codCls}")
            map.put(codCls, map2.get(codCls))

        } else {
            map.put("obj", idObj)
            whe = "o.id = :obj"
        }

        Store st = mdb.createStore("Obj.FishFamily")
        mdb.loadQuery(st, """
            select o.id as obj, o.cls, v.name, null as nameCls, o.cmt, o.id as id, v.objParent as parent, o.ord,
                v1.id as idFishFamily, v1.propval as pvFishFamily, null as fvFishFamily
            from Obj o
                left join ObjVer v on o.id=v.ownerVer and v.lastVer=1
                left join DataProp d1 on d1.isobj=1 and d1.objorrelobj=o.id and d1.prop=:Prop_FishFamily
                left join DataPropVal v1 on d1.id=v1.dataprop
            where ${whe}
            order by o.ord
        """, map)

        Set<Object> idsCls = st.getUniqueValues("cls")
        if (idsCls.isEmpty()) idsCls.add(0L)
        Store stCls = loadSqlMeta("""
            select c.id, v.name from Cls c, ClsVer v
            where c.id=v.ownerVer and v.lastVer=1 and c.id in (${idsCls.join(",")})
        """, "")
        StoreIndex indStCls = stCls.getIndex("id")
        Map<Long, Long> mapPV = apiMeta().get(ApiMeta).mapEntityIdFromPV("factorVal", true)

        for (StoreRecord record in st) {
            StoreRecord rec = indStCls.get(record.get("cls"))
            if (rec != null)
                record.set("nameCls", rec.getString("name"))
            record.set("fvFishFamily", mapPV.get(record.getLong("pvFishFamily")))
        }
        return st
    }

    @DaoMethod
    Store loadTypeOfFish(String codCls, String codProp) {
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", codCls, "")
        if (map.isEmpty())
            throw new XError("NotFoundCod@" + codCls)

        Store st = mdb.loadQuery("""
            select o.id, o.cls, v.name, 0 as pv
            from Obj o, ObjVer v
            where o.id=v.ownerVer and v.lastVer=1 and o.cls=:Cls_FishTypes
        """, map)

        if (codProp) {
            Store stPV = loadSqlMeta("""
                select pv.id, pv.cls from PropVal pv, Prop p where pv.prop=p.id and p.cod like '${codProp}'
            """, "")
            StoreIndex indPV = stPV.getIndex("cls")
            for (StoreRecord r in st) {
                StoreRecord rec = indPV.get(r.getLong("cls"))
                if (rec != null)
                    r.set("pv", rec.getLong("id"))
            }
        }
        return st
    }

    @DaoMethod
    Store loadTypeOfFishForRes(long objResSampling, String codProp) {
        String whe1 = "o.id=${objResSampling}"
        String whe2 = "r1.obj=0"
        if (objResSampling == 0) {
            Store stCls = loadSqlMeta("""
                select id from Cls where typ in (select id from Typ where cod='Typ_Research')
            """, "")
            Set<Object> idsCls = stCls.getUniqueValues("id")
            Store stObj = mdb.loadQuery("""
                select o.id from Obj o, ObjVer v
                where o.id=v.ownerVer and v.lastVer=1 and o.cls in (0${idsCls.join(",")}) and v.objParent is null
            """)
            Set<Object> idsObj = stObj.getUniqueValues("id")
            whe1 = "o.id in (0${idsObj.join(",")})"
            //whe2 = "r1.obj in (0${idsObj.join(",")})"
        }

        Map<String, Long> map = apiMeta().get(ApiMeta)
                .getIdFromCodOfEntity("Prop", "", "Prop_%")

        //objResSampling
        Store stResoirvor = mdb.loadQuery("""
            select o.id as obj, v51.obj as objReservoirShore
            from Obj o
                inner join ObjVer v on o.id=v.ownerVer and v.lastVer=1
                inner join DataProp d1 on d1.objorrelobj=o.id and d1.prop=:Prop_LinkToSample     --1056
                left join DataPropVal v1 on d1.id=v1.dataprop
                left join DataProp d5 on d5.objorrelobj=v1.obj and d5.prop=:Prop_StationSampling  --1053
                left join DataPropVal v5 on d5.id=v5.dataprop
                left join DataProp d51 on d51.objorrelobj=v5.obj and d51.prop=:Prop_ReservoirShore  --1049
                left join DataPropVal v51 on d51.id=v51.dataprop    
            where ${whe1}
        """, map)


        if (objResSampling != 0) {
            if (stResoirvor.size() > 0)
                whe2 = "r1.obj=${stResoirvor.get(0).getLong('objReservoirShore')}"
        } else {
            Set<Object> idsResoirVoir = stResoirvor.getUniqueValues("objReservoirShore")
            whe2 = "r1.obj in (0${idsResoirVoir.join(",")})"
        }

        Store stRelCls = loadSqlMeta("""
            select id from RelCls where relTyp in (select id from RelTyp where cod='RelTyp_FishType')
        """, "")
        Set<Object> idsRelCls = stRelCls.getUniqueValues("id")

        //objResoirvor
        Store stObjFish = mdb.loadQuery("""
            with rom1 as (
                select min(r.id) as rom 
                from relobjmember r
                where r.relobj in (select id from RelObj where relcls in (0${idsRelCls.join(",")}))
                group by r.relobj
            ),
            obj1 as (
                select obj as obj1 from relobjmember r 
                inner join rom1 on r.id=rom1.rom
            ),
            rom2 as (
                select max(r.id) as rom 
                from relobjmember r
                where r.relobj in (select id from RelObj where relcls in (0${idsRelCls.join(",")}))
                group by r.relobj
            ),
            obj2 as (
                select obj as obj2 from relobjmember r 
                inner join rom2 on r.id=rom2.rom
            ),
            r1 as (
                select r1.relobj, r1.obj as obj1 
                from obj1
                left join relobjmember r1 on r1.obj=obj1.obj1
                where ${whe2}
            ),
            r2 as (
                select r1.relobj, r1.obj as obj2 
                from obj2
                left join relobjmember r1 on r1.obj=obj2.obj2
            )
            select distinct r1.relobj, r2.obj2
            from r1
            left join r2 on r1.relobj=r2.relobj
        """)

        Set<Object> idsFish = stObjFish.getUniqueValues("obj2")

        Store st = mdb.loadQuery("""
            select o.id, o.cls, v.name, 0 as pv 
            from Obj o, ObjVer v
            where o.id=v.ownerVer and v.lastVer=1 and o.id in (0${idsFish.join(",")}) 
        """)

        if (codProp) {
            Store stPV = loadSqlMeta("""
                select pv.id, pv.cls from PropVal pv, Prop p where pv.prop=p.id and p.cod like '${codProp}'
            """, "")
            StoreIndex indPV = stPV.getIndex("cls")
            for (StoreRecord r in st) {
                StoreRecord rec = indPV.get(r.getLong("cls"))
                if (rec != null)
                    r.set("pv", rec.getLong("id"))
            }
        }

        return st
    }

    @DaoMethod
    Store loadTypeOfFishForSelect(String codCls) {
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", codCls, "")
        if (map.isEmpty())
            throw new XError("NotFoundCod@" + codCls)

        return mdb.loadQuery("""
            select o.id, v.objParent as parent, o.cls, v.name, 0 as pv
            from Obj o, ObjVer v
            where o.id=v.ownerVer and v.lastVer=1 and o.cls=:Cls_FishTypes
        """, map)
    }

    @DaoMethod
    Store loadTypeOfFishFirstLevel(String codCls) {
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", codCls, "")
        if (map.isEmpty())
            throw new XError("NotFoundCod@" + codCls)

        return mdb.loadQuery("""
            select o.id, v.name
            from Obj o, ObjVer v
            where o.id=v.ownerver and v.lastver=1 and o.cls=:Cls_FishTypes and v.objparent is null
            order by v.name
        """, map)
    }


    @DaoMethod
    Store loadSamplingStations(Map<String, Object> params) {
        String codCls = UtCnv.toString(params.get("codCls"))
        boolean isRec = UtCnv.toBoolean(params.get("isRec"))
        long idObj = UtCnv.toLong(params.get("idObj"))

        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "", "Prop_%")
        String whe = "o.id=${idObj}"
        if (!isRec) {
            Map<String, Long> map1 = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", codCls, "")
            whe = "o.cls = ${map1.get(codCls)}"
        }
        Store st = mdb.createStore("Obj.Stations")
        mdb.loadQuery(st, """
            select o.id as obj, o.cls, v.name, o.cmt, 
                v1.id as idReservoirShore, v1.propval as pvReservoirShore, v1.obj as objReservoirShore, 
                v2.id as idBranch, v2.propval as pvBranch, v2.obj as objBranch
            from Obj o
                left join ObjVer v on o.id=v.ownerver and v.lastver=1
                left join DataProp d1 on d1.objorrelobj=o.id and d1.prop=:Prop_ReservoirShore
                left join DataPropVal v1 on d1.id=v1.dataprop 
                left join DataProp d2 on d2.objorrelobj=v1.obj and d2.prop=:Prop_Branch
                left join DataPropVal v2 on d2.id=v2.dataprop
            where ${whe}
        """, map)
        return st
    }

    @DaoMethod
    Store loadFishingArea(Map<String, Object> params) {
        String codCls = UtCnv.toString(params.get("codCls"))
        boolean isRec = UtCnv.toBoolean(params.get("isRec"))
        long idObj = UtCnv.toLong(params.get("idObj"))

        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "", "Prop_%")
        String whe = "o.id=${idObj}"
        if (!isRec) {
            Map<String, Long> map1 = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", codCls, "")
            whe = "o.cls = ${map1.get(codCls)}"
        }
        Store st = mdb.createStore("Obj.Stations")
        mdb.loadQuery(st, """
            select o.id as obj, o.cls, v.name, o.cmt, 
                v1.id as idReservoirShore, v1.propval as pvReservoirShore, v1.obj as objReservoirShore, 
                v2.id as idBranch, v2.propval as pvBranch, v2.obj as objBranch
            from Obj o
                left join ObjVer v on o.id=v.ownerver and v.lastver=1
                left join DataProp d1 on d1.objorrelobj=o.id and d1.prop=:Prop_ReservoirShore
                left join DataPropVal v1 on d1.id=v1.dataprop 
                left join DataProp d2 on d2.objorrelobj=v1.obj and d2.prop=:Prop_Branch
                left join DataPropVal v2 on d2.id=v2.dataprop
            where ${whe}
        """, map)
        return st
    }

    @DaoMethod
    Store loadReservors(Map<String, Object> params) {

        String codTyp = UtCnv.toString(params.get("codTyp"))
        boolean isRec = UtCnv.toBoolean(params.get("isRec"))
        long idObj = UtCnv.toLong(params.get("idObj"))
        String dte = UtCnv.toString(params.get("dte"))
        long periodType = UtCnv.toLong(params.get("periodType"))
        UtPeriod utPeriod = new UtPeriod()
        XDate d1 = utPeriod.calcDbeg(UtCnv.toDate(dte), periodType, 0)
        XDate d2 = utPeriod.calcDend(UtCnv.toDate(dte), periodType, 0)

        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "", "Prop_%")
        String whe = "o.id=${idObj}"
        if (!isRec) {
            Set<Object> ids = apiMetaFish().get(ApiMetaFish).idsChildClses(codTyp)
            whe = "o.cls in (${ids.join(",")})"
        }
        Store st = mdb.createStore("Obj.reservoirs")
        mdb.loadQuery(st, """
            select distinct 
                o.id as obj, v.id, o.cls, v.name, null as nameCls,
                v1.id as idRegion, v1.propval as pvRegion, v1.obj as objRegion, null as nameRegion,
                v1_1.id as idDistrict, v1_1.propval as pvDistrict, v1_1.obj as objDistrict, null as nameDistrict,
                v2.id as idBranch, v2.propval as pvBranch, v2.obj as objBranch, null as nameBranch,
                v3.id as idReservoirType, v3.propval as pvReservoirType, null as fvReservoirType,
                v4.id as idReservoirStatus, v4.propval as pvReservoirStatus, null as fvReservoirStatus,
                v4_1.id as idFishFarmingType, v4_1.propval as pvFishFarmingType, null as fvFishFarmingType
            from Obj o
                left join ObjVer v on o.id=v.ownerVer and v.lastVer=1
                left join DataProp d1 on d1.isobj=1 and d1.objorrelobj=o.id and d1.prop=:Prop_Region   --1000
                left join DataPropVal v1 on d1.id=v1.dataprop and '${dte}' between v1.dbeg and v1.dend     --inner
                left join DataProp d2 on d2.isobj=1 and d2.objorrelobj=o.id and d2.prop=:Prop_Branch   --1013
                left join DataPropVal v2 on d2.id=v2.dataprop and '${dte}' between v2.dbeg and v2.dend     --inner
                left join DataProp d1_1 on d1_1.isobj=1 and d1_1.objorrelobj=o.id and d1_1.prop=:Prop_District   --1044
                left join DataPropVal v1_1 on d1_1.id=v1_1.dataprop and '${dte}' between v1_1.dbeg and v1_1.dend
                left join DataProp d3 on d3.isobj=1 and d3.objorrelobj=o.id and d3.prop=:Prop_ReservoirType    --1002
                left join DataPropVal v3 on d3.id=v3.dataprop and '${dte}' between v3.dbeg and v3.dend
                left join DataProp d4 on d4.isobj=1 and d4.objorrelobj=o.id and d4.prop=:Prop_ReservoirStatus   --1003
                left join DataPropVal v4 on d4.id=v4.dataprop and '${dte}' between v4.dbeg and v4.dend
                left join DataProp d4_1 on d4_1.isobj=1 and d4_1.objorrelobj=o.id and d4_1.prop=:Prop_FishFarmingType   --1045
                left join DataPropVal v4_1 on d4_1.id=v4_1.dataprop and '${dte}' between v4_1.dbeg and v4_1.dend
            where '${dte}' between v.dbeg and v.dend and ${whe}
        """, map)
        //mdb.outTable(st)
        Store st2 = mdb.createStore("Obj.reservoirsMeter")
        mdb.loadQuery(st2, """
            select o.id as obj, 
                d8.id as didWaterArea, v8.id as idWaterArea, v8.numberval as WaterArea, v8.numberval as oldWaterArea,
                d16.id as didWaterAreaFishing, v16.id as idWaterAreaFishing, v16.numberval as WaterAreaFishing, v16.numberval as oldWaterAreaFishing,
                d17.id as didWaterAreaLittoral, v17.id as idWaterAreaLittoral, v17.numberval as WaterAreaLittoral, v17.numberval as oldWaterAreaLittoral,
                d18.id as didReservoirHydroLevel, v18.id as idReservoirHydroLevel, v18.numberval as ReservoirHydroLevel, v18.numberval as oldReservoirHydroLevel
            from Obj o
                left join ObjVer v on o.id=v.ownerVer and v.lastVer=1
                left join DataProp d8 on d8.isobj=1 and d8.objorrelobj=o.id and d8.periodType=${periodType} and d8.prop=:Prop_WaterArea --1004
                left join DataPropVal v8 on d8.id=v8.dataprop and v8.dbeg='${d1}' and v8.dend='${d2}' and v8.numberVal is not null
                left join DataProp d16 on d16.isobj=1 and d16.objorrelobj=o.id and d16.periodType=${periodType} and d16.prop=:Prop_WaterAreaFishing  --1076
                left join DataPropVal v16 on d16.id=v16.dataprop and v16.dbeg='${d1}' and v16.dend='${d2}' and v16.numberVal is not null
                left join DataProp d17 on d17.isobj=1 and d17.objorrelobj=o.id and d17.periodType=${periodType} and d17.prop=:Prop_WaterAreaLittoral --1077
                left join DataPropVal v17 on d17.id=v17.dataprop and v17.dbeg='${d1}' and v17.dend='${d2}' and v17.numberVal is not null
                left join DataProp d18 on d18.isobj=1 and d18.objorrelobj=o.id and d18.periodType=${periodType} and d18.prop=:Prop_ReservoirHydroLevel   --1078
                left join DataPropVal v18 on d18.id=v18.dataprop and v18.dbeg='${d1}' and v18.dend='${d2}' and v18.numberVal is not null
            where '${dte}' between v.dbeg and v.dend and ${whe}                   
        """, map)
        //mdb.outTable(st2)
        Store st3 = mdb.createStore("Obj.reservoirsMeter")
        for (StoreRecord r in st2) {
            if ( r.getLong("idWaterArea")>0 || r.getLong("idWaterAreaFishing")>0
                    || r.getLong("idWaterAreaLittoral") >0 || r.getLong("idReservoirHydroLevel") >0 ) {
                st3.add(r)
            }
        }
        //mdb.outTable(st3)

        //
        StoreIndex indSt3 = st3.getIndex("obj")
        for (StoreRecord r in st) {
            StoreRecord rec = indSt3.get(r.getLong("obj"))
            if (rec != null) {
                r.set("didWaterArea", rec.get("didWaterArea"))
                r.set("idWaterArea", rec.get("idWaterArea"))
                if (rec.getLong("idWaterArea") > 0) {
                    r.set("WaterArea", rec.getDouble("WaterArea"))
                    r.set("oldWaterArea", rec.getDouble("WaterArea"))
                }

                r.set("didWaterAreaFishing", rec.get("didWaterAreaFishing"))
                r.set("idWaterAreaFishing", rec.get("idWaterAreaFishing"))
                if (rec.getLong("idWaterAreaFishing") > 0) {
                    r.set("WaterAreaFishing", rec.getDouble("WaterAreaFishing"))
                    r.set("oldWaterAreaFishing", rec.getDouble("WaterAreaFishing"))
                }

                r.set("didWaterAreaLittoral", rec.get("didWaterAreaLittoral"))
                r.set("idWaterAreaLittoral", rec.get("idWaterAreaLittoral"))
                if (rec.getLong("idWaterAreaLittoral") > 0) {
                    r.set("WaterAreaLittoral", rec.getDouble("WaterAreaLittoral"))
                    r.set("oldWaterAreaLittoral", rec.getDouble("WaterAreaLittoral"))
                }

                r.set("didReservoirHydroLevel", rec.get("didReservoirHydroLevel"))
                r.set("idReservoirHydroLevel", rec.get("idReservoirHydroLevel"))
                if (rec.getLong("idReservoirHydroLevel") > 0) {
                    r.set("ReservoirHydroLevel", rec.getDouble("ReservoirHydroLevel"))
                    r.set("oldReservoirHydroLevel", rec.getDouble("ReservoirHydroLevel"))
                }
            }
        }
        //
        //mdb.outTable(st)
        //
        Set<Object> idsCls = st.getUniqueValues("cls")
        if (idsCls.isEmpty()) idsCls.add(0L)

        Set<Object> idsRegion = st.getUniqueValues("objRegion")
        if (idsRegion.isEmpty()) idsRegion.add(0L)

        Set<Object> idsDistrict = st.getUniqueValues("objDistrict")
        if (idsDistrict.isEmpty()) idsDistrict.add(0L)
        idsRegion.addAll(idsDistrict)
        Set<Object> idsBranch = st.getUniqueValues("objBranch")
        idsRegion.addAll(idsBranch)

        Store stObj = loadSql("""
            select o.id, v.name from Obj o, ObjVer v
            where o.id=v.ownerVer and v.lastVer=1 and o.id in (${idsRegion.join(",")}) 
        """, "", "nsidata")
        StoreIndex indStObj = stObj.getIndex("id")

        Store stCls = loadSqlMeta("""
            select c.id, v.name from Cls c, ClsVer v
            where c.id=v.ownerVer and v.lastVer=1 and c.id in (${idsCls.join(",")})
        """, "")
        StoreIndex indStCls = stCls.getIndex("id")

        Map<Long, Long> mapPV = apiMeta().get(ApiMeta).mapEntityIdFromPV("factorVal", true)

        for (StoreRecord record in st) {
            StoreRecord rec = indStObj.get(record.get("objRegion"))
            if (rec != null)
                record.set("nameRegion", rec.getString("name"))
            rec = indStObj.get(record.get("objDistrict"))
            if (rec != null)
                record.set("nameDistrict", rec.getString("name"))
            rec = indStObj.get(record.get("objBranch"))
            if (rec != null)
                record.set("nameBranch", rec.getString("name"))
            rec = indStCls.get(record.get("cls"))
            if (rec != null)
                record.set("nameCls", rec.getString("name"))
            record.set("fvReservoirType", mapPV.get(record.getLong("pvReservoirType")))
            record.set("fvReservoirStatus", mapPV.get(record.getLong("pvReservoirStatus")))
            record.set("fvFishFarmingType", mapPV.get(record.getLong("pvFishFarmingType")))
        }
        st.sort("nameRegion,nameDistrict")
        return st
    }

    @DaoMethod
    Map<Long, String> mapFvNameFromId() {
        return apiMeta().get(ApiMeta).mapFvNameFromId()
    }

    @DaoMethod
    Store loadChildClsForSelect(String codTyp) {
        return apiMetaFish().get(ApiMetaFish).loadChildClsForSelect(codTyp)
    }

    @DaoMethod
    Store loadRegion(String codCls) {
        return loadObjForSelect(codCls, "Prop_Region")
    }

    @DaoMethod
    Store loadDistrict(long region) {
        Store stDistrict = loadSql("""
            select o.id, v.name, o.cls, 0 as pv
            from Obj o, ObjVer v
            where o.id=v.ownerVer and v.lastVer=1 and v.objParent=${region}
        """, "", "nsidata")
        Set<Object> idsCls = stDistrict.getUniqueValues("cls")
        Map<String, Object> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "Prop_District", "") as Map<String, Object>

        Store stPV = apiMeta().get(ApiMeta).loadSqlWithParams("""
            select id, cls from PropVal where prop=:Prop_District and cls in (0${idsCls.join(",")})
        """, "", map)
        StoreIndex indexPV = stPV.getIndex("cls")
        for (StoreRecord r in stDistrict) {
            StoreRecord rec = indexPV.get(r.getLong("cls"))
            if (rec != null)
                r.set("pv", rec.getLong("id"))
        }
        return stDistrict
    }

    @DaoMethod
    Store loadBranchName(String codCls) {
        return loadObjForSelect(codCls, "Prop_Branch")
    }

    @DaoMethod
    Store loadFishGearName(String codCls) {
        return loadObjForSelect(codCls, "Prop_FishGear")
    }

    @DaoMethod
    Store loadReservoirName() {
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "Prop_Branch", "")
        if (map.isEmpty())
            throw new XError("NotFoundCod@Prop_Branch")
        return mdb.loadQuery("""
            select d.objorrelobj as id, o.cls, ov.name
            from DataProp d
                left join DataPropVal v on d.id=v.dataprop
                left join Obj o on o.id=d.objorrelobj
                left join ObjVer ov on o.id=ov.ownerVer and ov.lastVer=1
            where d.isObj=1 and d.prop=:Prop_Branch
        """, map)
    }


    private Store loadObjForSelect(String codCls, String codProp) {
        return apiNSIData().get(ApiNSIData).loadObjForSelect(codCls, codProp)
    }

    @DaoMethod
    Store loadFvReservoirType(String codFactor) {
        return loadFvForSelect(codFactor)
    }

    @DaoMethod
    Store loadFvReservoirStatus(String codFactor) {
        return loadFvForSelect(codFactor)
    }

    @DaoMethod
    Store loadFvFishFamilyForSelect(String codFactor) {
        return loadFvForSelect(codFactor)
    }

    @DaoMethod
    Store loadFvFishFarmingType(String codFactor) {
        return loadFvForSelect(codFactor)
    }


    private Store loadFvForSelect(String codFactor) {
        return apiMetaFish().get(ApiMetaFish).loadFvForSelect(codFactor)
    }

    private static void validatePropertiesRef(VariantMap pms) {


    }

    private long getReservoir(Map<String, Object> params) {
        String name = UtCnv.toString(params.get("name")).toLowerCase().trim()
        long region = UtCnv.toLong(params.get("objRegion"))
        long branch = UtCnv.toLong(params.get("objBranch"))
        long cls = UtCnv.toLong(params.get("cls"))
        String dte = UtCnv.toString(params.get("dte"))
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "", "Prop_%")
        Store st = mdb.loadQuery("""
            select o.id as obj
            from Obj o
                inner join ObjVer v on o.id=v.ownerVer and v.lastVer=1 and trim(lower(v.name))='${name}'
                inner join DataProp d1 on d1.isobj=1 and d1.objorrelobj=o.id and d1.prop=:Prop_Region
                inner join DataPropVal v1 on d1.id=v1.dataprop and '${dte}' between v1.dbeg and v1.dend and v1.obj=${region}
                inner join DataProp d2 on d2.isobj=1 and d2.objorrelobj=o.id and d2.prop=:Prop_Branch
                inner join DataPropVal v2 on d2.id=v2.dataprop and '${dte}' between v2.dbeg and v2.dend and v2.obj=${branch}
            where o.cls=${cls}
        """, map)
        if (st.size() == 0)
            return 0
        else if (st.size() == 1)
            return st.get(0).getLong("obj")
        else {
            throw new Error("The reservoir is duplicated")
        }
    }

    private void checkForExistReservoir(Map<String, Object> params) {
        String name = UtCnv.toString(params.get("name")).toLowerCase().trim()
        long own = UtCnv.toLong(params.get("obj"))
        long objRegion = UtCnv.toLong(params.get("objRegion"))
        long objBranch = UtCnv.toLong(params.get("objBranch"))
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "", "Prop_%")
        Store st = mdb.loadQuery("""
            select o.id
            from Obj o
                inner join ObjVer v on o.id=v.ownerVer and v.lastVer=1 and trim(lower(v.name))='${name}'
                left join DataProp d1 on d1.isobj=1 and d1.objorrelobj=o.id and d1.prop=:Prop_Region
                inner join DataPropVal v1 on d1.id=v1.dataprop and v1.obj=${objRegion}
                left join DataProp d2 on d2.isobj=1 and d2.objorrelobj=o.id and d2.prop=:Prop_Branch
                inner join DataPropVal v2 on d2.id=v2.dataprop and v2.obj=${objBranch}
            where o.id<>${own}
        """, map)

        if (st.size() > 0) {
            throw new XError("Такой водоем уже существует")
        }
    }

    @DaoMethod
    Store saveReservoirPropertiesRef(Map<String, Object> params) {
        VariantMap pms = new VariantMap(params)
        validatePropertiesRef(pms)
        long own = getReservoir(params)
        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        Map<String, Object> par = new HashMap<>(pms)
        par.put("fullName", pms.get("name"))
        par.put("dbeg", pms.get("dte"))
        if (pms.getString("mode").equalsIgnoreCase("ins")) {
            if (own > 0) {
                throw new XError("ExistsReservoirWithProps")
            }
            own = eu.insertEntity(par)
            pms.put("own", own)
            pms.put("isFirst", 1)

            //1 Prop_Region *
            fillProperties(true, "Prop_Region", pms)
            //2 Prop_Branch *
            fillProperties(true, "Prop_Branch", pms)
            //1_1 Prop_District
            if (pms.containsKey("objDistrict"))
                fillProperties(true, "Prop_District", pms)
            //3 Prop_ReservoirType
            if (pms.containsKey("fvReservoirType"))
                fillProperties(true, "Prop_ReservoirType", pms)
            //4 Prop_ReservoirStatus
            if (pms.containsKey("fvReservoirStatus"))
                fillProperties(true, "Prop_ReservoirStatus", pms)
            //4_1 Prop_FishFarmingType
            if (pms.containsKey("fvFishFarmingType"))
                fillProperties(true, "Prop_FishFarmingType", pms)
/*
            //7 Prop_WaterArea
            if (pms.containsKey("WaterArea"))
                fillProperties(true, "Prop_WaterArea", pms)

            //16 Prop_WaterAreaFishing
            if (pms.containsKey("WaterAreaFishing"))
                fillProperties(true, "Prop_WaterAreaFishing", pms)
            //17 Prop_WaterAreaLittoral
            if (pms.containsKey("WaterAreaLittoral"))
                fillProperties(true, "Prop_WaterAreaLittoral", pms)
            //18 Prop_ReservoirHydroLevel
            if (pms.containsKey("ReservoirHydroLevel"))
                fillProperties(true, "Prop_ReservoirHydroLevel", pms)*/

        } else {
            checkForExistReservoir(params)
            own = pms.getLong("obj")
            par.put("id", own)
            eu.updateEntity(par)
            //
            pms.put("own", own)
            pms.put("isFirst", 1)

            //todo isUniq
            //1 Prop_Region
            if (params.containsKey("idRegion"))
                updateProperties("Prop_Region", pms)
            //2 Prop_Branch
            if (params.containsKey("idBranch"))
                updateProperties("Prop_Branch", pms)


            //1_1 Prop_District
            if (params.containsKey("idDistrict"))
                updateProperties("Prop_District", pms)
            else if (params.containsKey("District"))
                fillProperties(true, "Prop_District", pms)

            //3 Prop_ReservoirType
            if (pms.containsKey("idReservoirType"))
                updateProperties("Prop_ReservoirType", pms)
            else if (pms.containsKey("pvReservoirType"))
                fillProperties(true, "Prop_ReservoirType", pms)

            //4 Prop_ReservoirStatus
            if (pms.containsKey("idReservoirStatus"))
                updateProperties("Prop_ReservoirStatus", pms)
            else if (pms.containsKey("pvReservoirStatus"))
                fillProperties(true, "Prop_ReservoirStatus", pms)

            //4.1 Prop_FishFarmingType
            if (pms.containsKey("idFishFarmingType"))
                updateProperties("Prop_FishFarmingType", pms)
            else if (pms.containsKey("pvFishFarmingType"))
                fillProperties(true, "Prop_FishFarmingType", pms)


//********************************************************************************************
/*
            //7 Prop_WaterArea
            if (pms.containsKey("idWaterArea")) {
                if (pms.getBoolean("newlife")) {
                    pms.put("isFirst", 2)
                    if (pms.getDouble("WaterArea") != pms.getDouble("oldWaterArea"))
                        fillProperties(true, "Prop_WaterArea", pms)
                } else {
                    if (pms.getDouble("WaterArea") == 0) {
                        deleteData(pms.getLong("idWaterArea"))
                    } else if (pms.getDouble("WaterArea") != pms.getDouble("oldWaterArea"))
                        updateProperties("Prop_WaterArea", pms)
                }
            } else if (pms.containsKey("WaterArea")) {
                pms.put("isFirst", 1)
                fillProperties(true, "Prop_WaterArea", pms)
            }


            //16 Prop_WaterAreaFishing
            if (pms.containsKey("idWaterAreaFishing")) {
                if (pms.getBoolean("newlife")) {
                    pms.put("isFirst", 2)
                    if (pms.getDouble("WaterAreaFishing") != pms.getDouble("oldWaterAreaFishing"))
                        fillProperties(true, "Prop_WaterAreaFishing", pms)
                } else {
                    if (pms.getDouble("WaterAreaFishing") == 0) {
                        deleteData(pms.getLong("idWaterAreaFishing"))
                    } else if (pms.getDouble("WaterAreaFishing") != pms.getDouble("oldWaterAreaFishing"))
                        updateProperties("Prop_WaterAreaFishing", pms)
                }
            } else if (pms.containsKey("WaterAreaFishing")) {
                pms.put("isFirst", 1)
                fillProperties(true, "Prop_WaterAreaFishing", pms)
            }

            //17 Prop_WaterAreaLittoral
            if (pms.containsKey("idWaterAreaLittoral")) {
                if (pms.getBoolean("newlife")) {
                    pms.put("isFirst", 2)
                    if (pms.getDouble("WaterAreaLittoral") != pms.getDouble("oldWaterAreaLittoral"))
                        fillProperties(true, "Prop_WaterAreaLittoral", pms)
                } else {
                    if (pms.getDouble("WaterAreaLittoral") == 0) {
                        deleteData(pms.getLong("idWaterAreaLittoral"))
                    } else if (pms.getDouble("WaterAreaLittoral") != pms.getDouble("oldWaterAreaLittoral"))
                        updateProperties("Prop_WaterAreaLittoral", pms)
                }
            } else if (pms.containsKey("WaterAreaLittoral")) {
                pms.put("isFirst", 1)
                fillProperties(true, "Prop_WaterAreaLittoral", pms)
            }

            //18 Prop_ReservoirHydroLevel
            if (pms.containsKey("idReservoirHydroLevel")) {
                if (pms.getBoolean("newlife")) {
                    pms.put("isFirst", 2)
                    if (pms.getDouble("ReservoirHydroLevel") != pms.getDouble("oldReservoirHydroLevel"))
                        fillProperties(true, "Prop_ReservoirHydroLevel", pms)
                } else {
                    if (pms.getDouble("ReservoirHydroLevel") == 0) {
                        deleteData(pms.getLong("idReservoirHydroLevel"))
                    } else if (pms.getDouble("ReservoirHydroLevel") != pms.getDouble("oldReservoirHydroLevel"))
                        updateProperties("Prop_ReservoirHydroLevel", pms)
                }
            } else if (pms.containsKey("ReservoirHydroLevel")) {
                pms.put("isFirst", 1)
                fillProperties(true, "Prop_ReservoirHydroLevel", pms)
            }
*/

        }
        return loadReservors([codTyp: "", isRec: true, idObj: own,
                              dte   : pms.getString("dte"), periodType: pms.getString("periodType")] as Map<String, Object>)
    }

    void validatePropsMeter(long own, long periodType, String dte, String codProp) {
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", codProp, "")
        if (map.isEmpty())
            throw new XError("NotFoundCod@" + codProp)

        UtPeriod utPeriod = new UtPeriod()
        XDate d1 = utPeriod.calcDbeg(UtCnv.toDate(dte), periodType, 0)
        XDate d2 = utPeriod.calcDend(UtCnv.toDate(dte), periodType, 0)


        Store st = mdb.loadQuery("""
            select v.id
            from DataProp d, DataPropVal v
            where d.id=v.dataprop and d.objorrelobj=${own} and d.periodtype=${periodType} 
            and v.dbeg='${d1}' and v.dend='${d2}' and d.prop=${map.get(codProp)}
        """)
        if (st.size() > 0) {
            throw new XError(codProp+": на указанную дату существует значение")
        }
    }

    @DaoMethod
    Store saveReservoirPropertiesMeter(Map<String, Object> params) {
        VariantMap pms = new VariantMap(params)
        pms.put("own", UtCnv.toLong(params.get("obj")))

        if (pms.getString("mode")=="ins") {
            //Prop_WaterArea
            if (pms.containsKey("WaterArea")) {
                validatePropsMeter(pms.getLong("own"), pms.getLong("periodType"),
                        pms.getString("dte"), "Prop_WaterArea")
                fillProperties(true, "Prop_WaterArea", pms)
            }
            //Prop_WaterAreaFishing
            if (pms.containsKey("WaterAreaFishing")) {
                validatePropsMeter(pms.getLong("own"), pms.getLong("periodType"),
                        pms.getString("dte"), "Prop_WaterAreaFishing")
                fillProperties(true, "Prop_WaterAreaFishing", pms)
            }
            //Prop_WaterAreaLittoral
            if (pms.containsKey("WaterAreaLittoral")) {
                validatePropsMeter(pms.getLong("own"), pms.getLong("periodType"),
                        pms.getString("dte"), "Prop_WaterAreaLittoral")
                fillProperties(true, "Prop_WaterAreaLittoral", pms)
            }
            //Prop_ReservoirHydroLevel
            if (pms.containsKey("ReservoirHydroLevel")) {
                validatePropsMeter(pms.getLong("own"), pms.getLong("periodType"),
                        pms.getString("dte"), "Prop_ReservoirHydroLevel")
                fillProperties(true, "Prop_ReservoirHydroLevel", pms)
            }
        } else {
            //Prop_WaterArea
            if (pms.getLong("idWaterArea")>0) {
                updateProperties("Prop_WaterArea", pms)
            } else {
                validatePropsMeter(pms.getLong("own"), pms.getLong("periodType"),
                        pms.getString("dte"), "Prop_WaterArea")
                fillProperties(true, "Prop_WaterArea", pms)
            }
            //Prop_WaterAreaFishing
            if (pms.getLong("idWaterAreaFishing")>0) {
                updateProperties("Prop_WaterAreaFishing", pms)
            } else {
                validatePropsMeter(pms.getLong("own"), pms.getLong("periodType"),
                        pms.getString("dte"), "Prop_WaterAreaFishing")
                fillProperties(true, "Prop_WaterAreaFishing", pms)
            }

            //Prop_WaterAreaLittoral
            if (pms.getLong("idWaterAreaLittoral")>0) {
                updateProperties("Prop_WaterAreaLittoral", pms)
            } else {
                validatePropsMeter(pms.getLong("own"), pms.getLong("periodType"),
                        pms.getString("dte"), "Prop_WaterAreaLittoral")
                fillProperties(true, "Prop_WaterAreaLittoral", pms)
            }

            //Prop_ReservoirHydroLevel
            if (pms.getLong("idReservoirHydroLevel")>0) {
                updateProperties("Prop_ReservoirHydroLevel", pms)
            } else {
                validatePropsMeter(pms.getLong("own"), pms.getLong("periodType"),
                        pms.getString("dte"), "Prop_ReservoirHydroLevel")
                fillProperties(true, "Prop_ReservoirHydroLevel", pms)
            }

        }
        //
        return loadReservors([codTyp: "", isRec: true, idObj: pms.getLong("obj"),
                              dte   : pms.getString("dte"), periodType: pms.getString("periodType")] as Map<String, Object>)
    }


    private void deleteData(long id) {
        mdb.execQuery("""
            delete from DataPropVal where id=${id};
            delete from DataProp where id in (
                select id from DataProp
                except
                select dataProp as id from DataPropVal
            )
        """)
    }

    @DaoMethod
    Store saveTypesFishProperties(Map<String, Object> params) {
        VariantMap pms = new VariantMap(params)
        long own
        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        Map<String, Object> par = new HashMap<>(pms)
        par.put("fullName", pms.get("name"))
        if (pms.getString("mode").equalsIgnoreCase("ins")) {
            own = eu.insertEntity(par)
            par.put("own", own)
            // Prop_FishFamily
            if (pms.containsKey("fvFishFamily"))
                fillProperties(true, "Prop_FishFamily", par)
        } else {
            own = pms.getLong("obj")
            par.put("id", own)
            eu.updateEntity(par)
            //
            par.put("own", own)
            //1 Prop_FishFamily
            if (pms.containsKey("idFishFamily")) {
                //old propval
                Store stOld = mdb.loadQuery("""
                    select v.propval, d.objorrelobj as obj 
                    from datapropval v
                    left join dataprop d on d.id=v.dataprop and d.isobj=1
                    where v.id=${pms.getLong("idFishFamily")}
                """)
                //
                updateProperties("Prop_FishFamily", par)
                //
                if (stOld.get(0).getLong("propval") != pms.getLong("pvFishFamily")) {
                    Store stNew = mdb.loadQuery("""
                        select v.id, v.propval 
                        from datapropval v
                            left join dataprop d on d.id=v.dataprop and d.isobj=1
                        where d.objorrelobj in (
                        select o.id from Obj o, ObjVer v where o.id=v.ownerver and v.lastver=1 and v.objparent=${stOld.get(0).getLong("obj")}
                        )
                    """)
                    for (StoreRecord r in stNew) {
                        mdb.execQuery("""
                            update DataPropVal set propval=${pms.getLong("pvFishFamily")} where id=${r.getLong("id")}
                        """)
                    }

                }
            } else {
                fillProperties(true, "Prop_FishFamily", par)
            }
        }
        return loadFishFamily([codCls: "", isRec: true, idObj: own] as Map<String, Object>)
    }

    @DaoMethod
    Store saveSamplingStation(Map<String, Object> params) {
        VariantMap pms = new VariantMap(params)
        long own
        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        Map<String, Object> par = new HashMap<>(pms)
        par.put("fullName", pms.get("name"))
        if (pms.getString("mode").equalsIgnoreCase("ins")) {
            Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", "Cls_StationSampling", "")
            if (map.isEmpty()) throw new XError("NotFoundCod@Cls_StationSampling")
            par.put("cls", map.get("Cls_StationSampling"))
            own = eu.insertEntity(par)
            pms.put("own", own)
            // Prop_ReservoirShore
            fillProperties(true, "Prop_ReservoirShore", pms)
        } else {
            own = pms.getLong("obj")
            par.put("id", own)
            eu.updateEntity(par)
            //
            pms.put("own", own)
            //1 Prop_ReservoirShore
            if (pms.containsKey("idReservoirShore"))
                updateProperties("Prop_ReservoirShore", pms)
        }
        return loadSamplingStations([codCls: "", isRec: true, idObj: own] as Map<String, Object>)
    }

    @DaoMethod
    Store saveFishingArea(Map<String, Object> params) {
        VariantMap pms = new VariantMap(params)
        long own
        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        Map<String, Object> par = new HashMap<>(pms)
        par.put("fullName", pms.get("name"))
        if (pms.getString("mode").equalsIgnoreCase("ins")) {
            Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", "Cls_FishingArea", "")
            if (map.isEmpty()) throw new XError("NotFoundCod@Cls_FishingArea")
            par.put("cls", map.get("Cls_FishingArea"))
            own = eu.insertEntity(par)
            pms.put("own", own)
            // Prop_ReservoirShore
            fillProperties(true, "Prop_ReservoirShore", pms)
        } else {
            own = pms.getLong("obj")
            par.put("id", own)
            eu.updateEntity(par)
            //
            pms.put("own", own)
            //1 Prop_ReservoirShore
            if (pms.containsKey("idReservoirShore"))
                updateProperties("Prop_ReservoirShore", pms)
        }
        return loadFishingArea([codCls: "", isRec: true, idObj: own] as Map<String, Object>)
    }

    private long getIdDataProp(Store stProp, boolean isObj, long own, long prop, long periodType) {
        StoreRecord recDP = mdb.createStoreRecord("DataProp")
        recDP.set("isObj", isObj)
        recDP.set("objOrRelObj", own)
        recDP.set("prop", prop)
        if (stProp.get(0).getLong("statusFactor") > 0) {
            long fv = apiMeta().get(ApiMeta).getDefaultStatus(prop)
            recDP.set("status", fv)
        }
        if (stProp.get(0).getLong("providerTyp") > 0) {
            //todo
            // provider
            //
        }
        if (stProp.get(0).getBoolean("dependPeriod")) {
            recDP.set("periodType", periodType)
        }
        long idDP = mdb.insertRec("DataProp", recDP, true)
        return idDP
    }

    void fillProperties(boolean isObj, String cod, Map<String, Object> params) {
        long own = UtCnv.toLong(params.get("own"))
        String keyValue = cod.split("_")[1]
        long objRef = UtCnv.toLong(params.get("obj" + keyValue))
        long propVal = UtCnv.toLong(params.get("pv" + keyValue))

        Store stProp = apiMeta().get(ApiMeta).getPropInfo(cod)
        //
        long prop = stProp.get(0).getLong("id")
        long propType = stProp.get(0).getLong("propType")
        long attribValType = stProp.get(0).getLong("attribValType")
        boolean dependPeriod = stProp.get(0).getInt("dependPeriod") == 1
        long periodType = 0
        if (dependPeriod) {
            periodType = UtCnv.toLong(params.get("periodType"))
        }
        Integer digit = null
        double koef = stProp.get(0).getDouble("koef")
        if (koef == 0) koef = 1
        if (!stProp.get(0).isValueNull("digit"))
            digit = stProp.get(0).getInt("digit")

        long idDP = 0
        String dend = "3333-12-31"
        if (periodType > 0) {
            idDP = UtCnv.toLong(params.get("did" + keyValue))
            if (idDP == 0) {
                idDP = getIdDataProp(stProp, isObj, own, prop, periodType)
            }
        } else {
            if (params.keySet().contains("isFirst")) {
                if (UtCnv.toInt(params.get("isFirst")) == 1) {
                        Store st = mdb.loadQuery("""
                        select v.id, d.id as did, v.dbeg, v.dend 
                        from DataProp d, DataPropVal v
                        where d.id=v.dataprop and d.isobj=1 and d.prop=${prop} and d.objorrelobj=${own}
                        order by dbeg desc, dend
                    """)
                    if (st.size() > 0) {
                        String dte = UtCnv.toString(params.get("dte"))
                        for (StoreRecord r in st) {
                            if (XDate.create(dte).toJavaLocalDate().isBefore(r.getDate("dbeg").toJavaLocalDate())) {
                                dend = r.getDate("dbeg").toJavaLocalDate().minusDays(1)
                                idDP = r.getLong("did")
                            }
                        }
                    } else {
                        idDP = getIdDataProp(stProp, isObj, own, prop, periodType)
                    }
                } else {
                    idDP = UtCnv.toLong(params.get("did" + keyValue))
                }
            } else {
                idDP = getIdDataProp(stProp, isObj, own, prop, periodType)
            }
        }

        //
        StoreRecord recDPV = mdb.createStoreRecord("DataPropVal")
        recDPV.set("dataProp", idDP)
        // For Attrib
        if ([FD_AttribValType_consts.str].contains(attribValType)) {
            if (cod.equalsIgnoreCase("Prop_SampleNumber") ||
                    cod.equalsIgnoreCase("Prop_ResearchNumber")) {
                if (params.get(keyValue) != null) {
                    recDPV.set("strVal", UtCnv.toString(params.get(keyValue)))
                }
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }
        if ([FD_AttribValType_consts.dt].contains(attribValType)) {
            if (cod.equalsIgnoreCase("Prop_RegDocumentsDateApproval") ||
                    cod.equalsIgnoreCase("Prop_RegDocumentsLifeDoc") ||
                    cod.equalsIgnoreCase("Prop_SampleDate") ||
                    cod.equalsIgnoreCase("Prop_ResearchDate")) {
                if (params.get(keyValue) != null) {
                    recDPV.set("dateTimeVal", UtCnv.toString(params.get(keyValue)))
                }
            } else
                throw new XError("for dev: [${cod}] отсутствует в реализации")
        }
        if ([FD_AttribValType_consts.multistr].contains(attribValType)) {
            if (cod.equalsIgnoreCase("Prop_Description")) {
                if (params.get(keyValue) != null) {
                    recDPV.set("multiStrVal", UtCnv.toString(params.get(keyValue)))
                }
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }
        // For Typ
        if ([FD_PropType_consts.typ].contains(propType)) {
            if (cod.equalsIgnoreCase("Prop_Region") ||
                    cod.equalsIgnoreCase("Prop_Branch") ||
                    cod.equalsIgnoreCase("Prop_District") ||
                    cod.equalsIgnoreCase("Prop_ReservoirShore") ||
                    cod.equalsIgnoreCase("Prop_SampleExecutor") ||
                    cod.equalsIgnoreCase("Prop_StationSampling") ||
                    cod.equalsIgnoreCase("Prop_LinkToSample") ||
                    cod.equalsIgnoreCase("Prop_ResearchExecutor") ||
                    cod.equalsIgnoreCase("Prop_FishTyp") ||
                    cod.equalsIgnoreCase("Prop_FishGear") ||
                    cod.equalsIgnoreCase("Prop_FishZone")) {
                if (objRef > 0) {
                    recDPV.set("propVal", propVal)
                    recDPV.set("obj", objRef)
                }
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }
        // For FV
        if ([FD_PropType_consts.factor].contains(propType)) {
            if (cod.equalsIgnoreCase("Prop_ReservoirType") ||
                    cod.equalsIgnoreCase("Prop_ReservoirStatus") ||
                    cod.equalsIgnoreCase("Prop_FishFamily") ||
                    cod.equalsIgnoreCase("Prop_FishFarmingType") ||
                    cod.equalsIgnoreCase("Prop_FishGender")) {
                if (propVal > 0) {
                    recDPV.set("propVal", propVal)
                }
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }
        // For Meter
        if ([FD_PropType_consts.meter, FD_PropType_consts.rate].contains(propType)) {
            if (cod.equalsIgnoreCase("Prop_WaterArea") ||
                    cod.equalsIgnoreCase("Prop_WorkDuration") ||
                    cod.equalsIgnoreCase("Prop_FishAge") ||
                    cod.equalsIgnoreCase("Prop_FishWeight") ||
                    cod.equalsIgnoreCase("Prop_FishLength") ||
                    cod.equalsIgnoreCase("Prop_Ph") ||
                    cod.equalsIgnoreCase("Prop_DissGasesCO2") ||
                    cod.equalsIgnoreCase("Prop_DissGasesO2") ||
                    cod.equalsIgnoreCase("Prop_DissGasesCO2Percent") ||
                    cod.equalsIgnoreCase("Prop_BiogenicCompNH4") ||
                    cod.equalsIgnoreCase("Prop_BiogenicCompNO2") ||
                    cod.equalsIgnoreCase("Prop_BiogenicCompNO3") ||
                    cod.equalsIgnoreCase("Prop_BiogenicCompPO4") ||
                    cod.equalsIgnoreCase("Prop_OrganicMatter") ||
                    cod.equalsIgnoreCase("Prop_Mineralization") ||
                    cod.equalsIgnoreCase("Prop_WaterAreaFishing") ||
                    cod.equalsIgnoreCase("Prop_WaterAreaLittoral") ||
                    cod.equalsIgnoreCase("Prop_ReservoirHydroLevel") ||
                    cod.equalsIgnoreCase("Prop_AreaOfTon")) {
                if (params.get(keyValue) != null) {
                    double v = UtCnv.toDouble(params.get(keyValue))
                    v = v / koef
                    if (digit) v = v.round(digit)
                    recDPV.set("numberVal", v)
                }
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }

        //
        if (dependPeriod) {
            if (!params.containsKey("dte"))
                params.put("dte", XDateTime.create(new Date()).toString(XDateTimeFormatter.ISO_DATE))
            UtPeriod utPeriod = new UtPeriod()
            XDate d1 = utPeriod.calcDbeg(UtCnv.toDate(params.get("dte")), periodType, 0)
            XDate d2 = utPeriod.calcDend(UtCnv.toDate(params.get("dte")), periodType, 0)
            recDPV.set("dbeg", d1.toString(XDateTimeFormatter.ISO_DATE))
            recDPV.set("dend", d2.toString(XDateTimeFormatter.ISO_DATE))
        } else {
            if (params.keySet().contains("isFirst")) {
                if (UtCnv.toInt(params.get("isFirst")) == 1) {
                    recDPV.set("dbeg", UtCnv.toString(params.get("dte")))
                    recDPV.set("dend", dend)
                } else {
                    updateDbegDend(UtCnv.toInt(isObj), prop, cod, params, recDPV)
                }
            } else {
                recDPV.set("dbeg", params.get("dbeg"))
                recDPV.set("dend", params.get("dend"))
            }
        }

        long au = getUser()
        recDPV.set("authUser", au)
        recDPV.set("inputType", FD_InputType_consts.app)
        long idDPV = mdb.getNextId("DataPropVal")
        recDPV.set("id", idDPV)
        recDPV.set("ord", idDPV)
        recDPV.set("timeStamp", XDateTime.create(new Date()).toString(XDateTimeFormatter.ISO_DATE_TIME))
        mdb.insertRec("DataPropVal", recDPV, false)
    }

    private void updateDbegDend(int isObj, long prop, String codProp, Map<String, Object> params, StoreRecord rec) {
        String cod = codProp.split("_")[1]
        long own = UtCnv.toLong(params.get("own"))
        Store st = mdb.loadQuery("""
            select v.id, v.dbeg, v.dend 
            from DataProp d, DataPropVal v
            where d.id=v.dataprop and d.isobj=${isObj} and d.prop=${prop} and d.objorrelobj=${own}
            order by dbeg desc, dend
        """)
        String dte = UtCnv.toString(params.get("dte"))

        if (st.size() > 0) {
            for (StoreRecord rr in st) {
                String dbeg = rr.getString("dbeg")
                String dend = rr.getString("dend")
                String dendNew = rr.getString("dend")
                long idv = rr.getLong("id")
                if (XDate.create(dte).toJavaLocalDate().plusDays(1).isAfter(XDate.create(dbeg).toJavaLocalDate()) &&
                        XDate.create(dte).toJavaLocalDate().minusDays(1).isBefore(XDate.create(dend).toJavaLocalDate())) {
                    if (dbeg == dte) {
                        throw new XError("ExistsValueOnDate@" + cod)
                    }
                    String dendOLd = XDate.create(dte).toJavaLocalDate().minusDays(1)
                    mdb.execQuery("""
                        update DataPropVal set dend='${dendOLd}' where id=${idv}
                    """)
                    rec.set("dbeg", dte)
                    rec.set("dend", dendNew)
                    break
                }
            }
        } else {
            rec.set("dbeg", dte)
        }
    }

    void updateProperties(String cod, Map<String, Object> params) {
        VariantMap mapProp = new VariantMap(params)
        String keyValue = cod.split("_")[1]
        long idVal = mapProp.getLong("id" + keyValue)
        long objRef = mapProp.getLong("obj" + keyValue)
        long propVal = mapProp.getLong("pv" + keyValue)
        Store stProp = apiMeta().get(ApiMeta).getPropInfo(cod)
        //
        long propType = stProp.get(0).getLong("propType")
        long attribValType = stProp.get(0).getLong("attribValType")
        Integer digit = null
        double koef = stProp.get(0).getDouble("koef")
        if (koef == 0) koef = 1
        if (!stProp.get(0).isValueNull("digit"))
            digit = stProp.get(0).getInt("digit")

        String sql = ""
        def tmst = XDateTime.create(new Date()).toString(XDateTimeFormatter.ISO_DATE_TIME)
        // For Attrib
        if ([FD_AttribValType_consts.str].contains(attribValType)) {
            if (cod.equalsIgnoreCase("Prop_SampleNumber") ||
                    cod.equalsIgnoreCase("Prop_ResearchNumber")) {
                def strValue = mapProp.getString(keyValue)
                if (!mapProp.keySet().contains(keyValue) || strValue.trim() == "") {
                    sql = """
                        delete from DataPropVal where id=${idVal};
                        delete from DataProp where id in (
                            select id from DataProp
                            except
                            select dataProp as id from DataPropVal
                        );
                    """
                } else {
                    sql = "update DataPropval set strVal='${strValue}', timeStamp='${tmst}' where id=${idVal}"
                }
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }
        if ([FD_AttribValType_consts.dt].contains(attribValType)) {
            if (cod.equalsIgnoreCase("Prop_RegDocumentsDateApproval") ||
                    cod.equalsIgnoreCase("Prop_SampleDate") ||
                    cod.equalsIgnoreCase("Prop_ResearchDate")) {
                def dtValue = mapProp.getString(keyValue)
                if (!mapProp.keySet().contains(keyValue) || dtValue.trim() == "") {
                    sql = """
                        delete from DataPropVal where id=${idVal};
                        delete from DataProp where id in (
                            select id from DataProp
                            except
                            select dataProp as id from DataPropVal
                        );
                    """
                } else {
                    sql = "update DataPropval set dateTimeVal='${dtValue}', timeStamp='${tmst}' where id=${idVal}"
                }
            } else
                throw new XError("for dev: [${cod}] отсутствует в реализации")
        }
        if ([FD_AttribValType_consts.multistr].contains(attribValType)) {
            if (cod.equalsIgnoreCase("Prop_Description")) {
                def str = mapProp.getString(keyValue)
                if (!mapProp.keySet().contains(keyValue) || str.trim() == "") {
                    sql = """
                        delete from DataPropVal where id=${idVal};
                        delete from DataProp where id in (
                            select id from DataProp
                            except
                            select dataProp as id from DataPropVal
                        );
                    """
                } else {
                    sql = "update DataPropval set multiStrVal='${str}', timeStamp='${tmst}' where id=${idVal}"
                }
            } else
                throw new XError("for dev: [${cod}] отсутствует в реализации")
        }
        // For Typ
        if ([FD_PropType_consts.typ].contains(propType)) {
            if (cod.equalsIgnoreCase("Prop_Region") ||
                    cod.equalsIgnoreCase("Prop_Branch") ||
                    cod.equalsIgnoreCase("Prop_District") ||
                    cod.equalsIgnoreCase("Prop_ReservoirShore") ||
                    cod.equalsIgnoreCase("Prop_SampleExecutor") ||
                    cod.equalsIgnoreCase("Prop_LinkToSample") ||
                    cod.equalsIgnoreCase("Prop_ResearchExecutor") ||
                    cod.equalsIgnoreCase("Prop_FishTyp") ||
                    cod.equalsIgnoreCase("Prop_FishGear") ||
                    cod.equalsIgnoreCase("Prop_FishZone")) {
                if (objRef > 0)
                    sql = "update DataPropval set propVal=${propVal}, obj=${objRef}, timeStamp='${tmst}' where id=${idVal}"
                else {
                    sql = """
                        delete from DataPropVal where id=${idVal};
                        delete from DataProp where id in (
                            select id from DataProp
                            except
                            select dataProp as id from DataPropVal
                        );
                    """
                }
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }
        // For FV
        if ([FD_PropType_consts.factor].contains(propType)) {
            if (cod.equalsIgnoreCase("Prop_ReservoirType") ||
                    cod.equalsIgnoreCase("Prop_ReservoirStatus") ||
                    cod.equalsIgnoreCase("Prop_FishFamily") ||
                    cod.equalsIgnoreCase("Prop_FishFarmingType") ||
                    cod.equalsIgnoreCase("Prop_FishGender")) {
                if (propVal > 0)
                    sql = "update DataPropval set propVal=${propVal}, timeStamp='${tmst}' where id=${idVal}"
                else {
                    sql = """
                        delete from DataPropVal where id=${idVal};
                        delete from DataProp where id in (
                            select id from DataProp
                            except
                            select dataProp as id from DataPropVal
                        );
                    """
                }
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }
        // For Meter
        if ([FD_PropType_consts.meter, FD_PropType_consts.rate].contains(propType)) {
            if (cod.equalsIgnoreCase("Prop_WaterArea") ||
                    cod.equalsIgnoreCase("Prop_WorkDuration") ||
                    cod.equalsIgnoreCase("Prop_FishAge") ||
                    cod.equalsIgnoreCase("Prop_FishWeight") ||
                    cod.equalsIgnoreCase("Prop_FishLength") ||
                    cod.equalsIgnoreCase("Prop_Ph") ||
                    cod.equalsIgnoreCase("Prop_DissGasesCO2") ||
                    cod.equalsIgnoreCase("Prop_DissGasesO2") ||
                    cod.equalsIgnoreCase("Prop_DissGasesCO2Percent") ||
                    cod.equalsIgnoreCase("Prop_BiogenicCompNH4") ||
                    cod.equalsIgnoreCase("Prop_BiogenicCompNO2") ||
                    cod.equalsIgnoreCase("Prop_BiogenicCompNO3") ||
                    cod.equalsIgnoreCase("Prop_BiogenicCompPO4") ||
                    cod.equalsIgnoreCase("Prop_OrganicMatter") ||
                    cod.equalsIgnoreCase("Prop_Mineralization") ||
                    cod.equalsIgnoreCase("Prop_WaterAreaFishing") ||
                    cod.equalsIgnoreCase("Prop_WaterAreaLittoral") ||
                    cod.equalsIgnoreCase("Prop_ReservoirHydroLevel") ||
                    cod.equalsIgnoreCase("Prop_AreaOfTon")) {
                if (mapProp.keySet().contains(keyValue) && mapProp[keyValue] != "") {
                    def v = mapProp.getDouble(keyValue)
                    v = v / koef
                    if (digit) v = v.round(digit)
                    sql = "update DataPropval set numberVal=${v}, timeStamp='${tmst}' where id=${idVal}"
                } else {
                    sql = """
                        delete from DataPropVal where id=${idVal};
                        delete from DataProp where id in (
                            select id from DataProp
                            except
                            select dataProp as id from DataPropVal
                        );
                    """
                }
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }
        mdb.execQueryNative(sql)

    }

    private void execSql(String sql, String model) {
        if (model.equalsIgnoreCase("userdata"))
            apiUserData().get(ApiUserData).execSql(sql)
        else if (model.equalsIgnoreCase("nsidata"))
            apiNSIData().get(ApiNSIData).execSql(sql)
        else
            throw new XError("Unknown model [${model}]")
    }

    @DaoMethod
    String getPathFile(long id) {

        DbFileStorageService dfsrv = apiMeta().get(ApiMeta).getDbFileStorageService()
        dfsrv.setModelName(UtCnv.toString("nsidata"))
        DbFileStorageItem dfsi = dfsrv.getFile(id)

        String pdf_dir = getApp().getAppdir() + File.separator + "frontend" + File.separator + "pdf"
        //String pdf_dir = getApp().getAppdir() + File.separator + "pdf"
        File fle = dfsi.getFile()

        File file = new File(UtFile.join(pdf_dir, dfsi.originalFilename))
        if (UtFile.exists(file))
            file.delete()

        try (InputStream ins = new FileInputStream(fle)) {
            Files.copy(ins, Paths.get(pdf_dir, dfsi.originalFilename))
        } catch (Exception e) {
            e.printStackTrace()
        }
        String pathFile = '/pdf/' + dfsi.originalFilename

        return pathFile

    }

    @DaoMethod
    long loadClsId(String codCls) {
        return apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", codCls, "").get(codCls)
    }

    @DaoMethod
    Store loadReservoir(long branch) {
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "Prop_Branch", "")
        if (map.isEmpty())
            throw new XError("NotFoundCod@Prop_Branch")
        map.put("branch", branch)
        Store st = mdb.loadQuery("""
            select d.objorrelobj as id, o.cls, ov.name, 0 as pv
            from DataProp d
                left join DataPropVal v on d.id=v.dataprop
                left join Obj o on o.id=d.objorrelobj
                left join ObjVer ov on o.id=ov.ownerVer and ov.lastVer=1
            where d.isObj=1 and d.prop=:Prop_Branch and v.obj=:branch
            order by ov.name
        """, map)

        Store stPV = loadSqlMeta("""
            select pv.id, pv.cls 
            from PropVal pv, Prop p 
            where pv.prop=p.id and p.cod like 'Prop_ReservoirShore'
        """, "")
        StoreIndex indPV = stPV.getIndex("cls")
        for (StoreRecord r in st) {
            StoreRecord rec = indPV.get(r.getLong("cls"))
            if (rec != null)
                r.set("pv", rec.getLong("id"))
        }
        return st
    }

    @DaoMethod
    Store savePiscesReservoir(Map<String, Object> params) {
        VariantMap form = new VariantMap(params)
        //
        Store stTmp = mdb.loadQuery("""
            select v.name 
            from RelObjMember r 
                inner join RelObjMember r2 on r.relobj=r2.relobj and r2.cls=${form.getLong("cls2")} and r2.obj=${form.getLong("typeoffish")}
                inner join relobjver v on r.relobj=v.ownerver and v.lastver=1
            where r.cls=${form.getLong("cls1")} and r.obj=${form.getLong("reservoir")}
        """)
        if (stTmp.size() > 0) {
            throw new XError("exists@${stTmp.get(0).getString("name")}")
        }
        //
        long relCls = apiMetaFish().get(ApiMetaFish).idRelCls(form.getLong("cls1"), form.getLong("cls2"))
        Map<String, Object> pms = new HashMap<>()
        pms.put("relCls", relCls)
        String n1 = mdb.loadQuery(
                "select name from ObjVer where ownerVer=:o and lastVer=1", [o: form.getLong("reservoir")])
                .get(0).getString("name")
        String n2 = mdb.loadQuery(
                "select name from ObjVer where ownerVer=:o and lastVer=1", [o: form.getLong("typeoffish")])
                .get(0).getString("name")
        pms.put("name", n1 + " => " + n2)
        pms.put("fullName", n1 + " => " + n2)
        EntityMdbUtils eu = new EntityMdbUtils(mdb, "RelObj")
        long relobj = eu.insertEntity(pms)
        //
        Store stRCM = apiMetaFish().get(ApiMetaFish).loadRelClsMember(relCls)

        StoreRecord recROM = mdb.createStoreRecord("RelObjMember")
        recROM.set("relObj", relobj)
        recROM.set("relClsMember", stRCM.get(0).getLong("id"))
        recROM.set("cls", form.getLong("cls1"))
        recROM.set("obj", form.getLong("reservoir"))
        mdb.insertRec("RelObjMember", recROM, true)
        //
        recROM = mdb.createStoreRecord("RelObjMember")
        recROM.set("relObj", relobj)
        recROM.set("relClsMember", stRCM.get(1).getLong("id"))
        recROM.set("cls", form.getLong("cls2"))
        recROM.set("obj", form.getLong("typeoffish"))
        mdb.insertRec("RelObjMember", recROM, true)

        Store st = mdb.loadQuery("""
            select o.id as relobj, o.relCls, m1.obj as reservoir, m1.cls as cls1, m2.obj as typeOfFish, 
                m2.cls as cls2, null::bigint as branch
            from RelObj o
                left join RelObjVer v on o.id=v.ownerVer and v.lastVer=1
                left join RelObjMember m1 on m1.relobj=o.id and m1.cls=:cls1
                left join RelObjMember m2 on m2.relobj=o.id and m2.cls=:cls2
            where o.id=:relobj    
        """, [relobj: relobj, cls1: form.getLong("cls1"), cls2: form.getLong("cls2")])

        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "Prop_Branch", "")
        if (map.isEmpty())
            throw new XError("NotFoundCod@Prop_Branch")
        long reservoir = st.get(0).getLong("reservoir")
        Store stBranch = mdb.loadQuery("""
            select v.obj
            from DataProp d
                left join DataPropVal v on d.id=v.dataprop
            where d.isObj=1 and d.prop=${map.get("Prop_Branch")} and d.objorrelobj=${reservoir}
        """)

        for (StoreRecord r in st) {
            r.set("branch", stBranch.get(0).getLong("obj"))
        }
        //
        return st
    }

    @DaoMethod
    Store loadPiscesReservoir(Map<String, Object> params) {
        String codRelTyp = UtCnv.toString(params.get("codRelTyp"))
        Set<Object> idsRelCls = apiMeta().get(ApiMeta).setIdsOfRelCls(codRelTyp)
        String whe = "(0" + idsRelCls.join(",") + ")"
        Store st = mdb.loadQuery("""
            select o.id as relobj, o.relcls, null::bigint as reservoir, null::bigint as cls1, 
                null::bigint as typeOfFish, null::bigint as cls2, null::bigint as branch
            from RelObj o
            where o.relcls in ${whe}
        """)

        Store stROM = mdb.loadQuery("""
            select relobj, 
                STRING_AGG (cast(cls as varchar(1000)), ',' order by id) as clslist, 
                STRING_AGG (cast(obj as varchar(1000)), ',' order by id) as objlist 
            from relobjmember
            where relobj in (select id from RelObj where relcls in ${whe}) 
            group by relobj
        """)
        StoreIndex indROM = stROM.getIndex("relobj")
        //
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "Prop_Branch", "")
        if (map.isEmpty())
            throw new XError("NotFoundCod@Prop_Branch")
        Store stBranch = mdb.loadQuery("""
            select v.obj, d.objorrelobj as reservoir
            from DataProp d
                left join DataPropVal v on d.id=v.dataprop
            where d.isObj=1 and d.prop=${map.get("Prop_Branch")}
        """)
        StoreIndex indBranch = stBranch.getIndex("reservoir")

        for (StoreRecord r in st) {
            StoreRecord rec = indROM.get(r.getLong("relobj"))
            if (rec != null) {
                def clss = rec.getString("clslist").split(",")
                def objs = rec.getString("objlist").split(",")
                long cls1 = UtCnv.toLong(clss[0])
                long cls2 = UtCnv.toLong(clss[1])
                long obj1 = UtCnv.toLong(objs[0])
                long obj2 = UtCnv.toLong(objs[1])

                r.set("reservoir", obj1)
                r.set("cls1", cls1)
                r.set("typeOfFish", obj2)
                r.set("cls2", cls2)
                StoreRecord rec1 = indBranch.get(obj1)
                if (rec1 != null)
                    r.set("branch", rec1.getLong("obj"))
            }

        }
        return st
    }

    @DaoMethod
    Store loadCls(String codTyp) {
        Store st = apiMeta().get(ApiMeta).loadCls(codTyp)
        if (st.size() == 0)
            throw new XError("NotFoundCod@${codTyp}")
        return st
    }

    @DaoMethod
    Store loadExecutor(String codTyp, String codProp) {
        Set<Object> idsCls = apiMeta().get(ApiMeta).setIdsOfCls(codTyp)
        Store st = loadSql("""
            select o.id as value, o.cls, v.fullName as label, 0 as pv
            from Obj o, ObjVer v
            where o.id=v.ownerVer and v.lastVer=1 and o.cls in (0${idsCls.join(",")})
        """, "", "userdata")

        Store stPV = loadSqlMeta("""
            select pv.id, pv.cls from PropVal pv, Prop p 
            where pv.prop=p.id and p.cod like '${codProp}'
        """, "")
        StoreIndex indPV = stPV.getIndex("cls")
        for (StoreRecord r in st) {
            StoreRecord rec = indPV.get(r.getLong("cls"))
            if (rec != null)
                r.set("pv", rec.getLong("id"))
        }

        return st
    }

    @DaoMethod
    Store loadAreaSampling(long reservoir) {
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "Prop_ReservoirShore", "")
        Map<String, Long> map1 = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", "Cls_FishingArea", "")
        map.putAll(map1)
        //Prop_ReservoirShore
        map.put("obj", reservoir)
        Store st = mdb.loadQuery("""
            select d1.objorrelobj as id, o.cls, ov.name, 0 as pv
            from DataProp d1
                left join DataPropVal v1 on d1.id=v1.dataprop
                left join ObjVer ov on d1.objorrelobj=ov.ownerVer and ov.lastVer=1
                inner join Obj o on o.id=d1.objorrelobj and o.cls=:Cls_FishingArea
            where d1.prop=:Prop_ReservoirShore and v1.obj=:obj and d1.isObj=1
        """, map)

        Store stPV = loadSqlMeta("""
            select pv.id, pv.cls 
            from PropVal pv, Prop p 
            where pv.prop=p.id and p.cod like 'Prop_FishZone'
        """, "")
        StoreIndex indPV = stPV.getIndex("cls")
        for (StoreRecord r in st) {
            StoreRecord rec = indPV.get(r.getLong("cls"))
            if (rec != null)
                r.set("pv", rec.getLong("id"))
        }
        return st
    }

    String getNameExecutor(String ids) {
        Store st = loadSql("""
            select v.fullName
            from Obj o, ObjVer v where o.id=v.ownerVer and v.lastVer=1
                and o.id in (0${ids})
        """, "", "userdata")
        return st.getUniqueValues("fullName").join(", ")
    }

    @DaoMethod
    Map<String, Object> loadSampling(long cls, boolean isRec, long obj, String gridMode) {
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "", "Prop_%")
        String whe = "o.id=${obj}"
        if (!isRec) {
            whe = "o.cls=${cls}"
        }
        Store st = mdb.createStore("Obj.Sampling")
        String propParent = "Prop_FishRatio"
        String typeField = "long"
        if (gridMode == "massa") {
            propParent = "Prop_FishRatioWeight"
            typeField = "double"
        }
        Store stPropFishRatio = loadSqlMeta("""
            select id, name
            from Prop
            where parent=${map.get(propParent)}
            order by name
        """, "")

        Set<Object> idsPropFishRatio = stPropFishRatio.getUniqueValues("id")
        List<Map<String, String>> cols = new ArrayList<>()
        for (StoreRecord r in stPropFishRatio) {
            st.addField("p_" + r.getLong("id"), typeField)
            st.addField("id_p_" + r.getLong("id"), "long")
            cols.add(Map.of("name", "p_" + r.getLong("id"), "label", r.getString("name"), "field", "p_" + r.getLong("id"),
                    "align", "center", "classes", "bg-blue-grey-1", "headerStyle", "font-size: 1.2em; width: 5%"))
        }
        st.addField("r_" + map.get(propParent), typeField)
        //st.addField("id_p_" + map.get(propParent), "long")
        cols.add(Map.of("name", "r_" + map.get(propParent), "label", "Итого", "field", "r_" + map.get(propParent),
                "align", "center", "classes", "bg-blue-grey-1", "headerStyle", "font-size: 1.2em; width: 8%"))

        mdb.loadQuery(st, """
            with usr as (
                select  o.id as obj, o.cls,
                STRING_AGG (cast(v4.obj as varchar(8000)), ',') as objsSampleExecutor
                from Obj o
                    inner join DataProp d4 on d4.objorrelobj=o.id and d4.prop=:Prop_SampleExecutor   --1052
                    left join DataPropVal v4 on d4.id=v4.dataprop
                where ${whe}
                group by o.id, o.cls
            )
            select o.id as obj, o.cls, o.cmt,
                d5.periodType, v5.dbeg, v5.dend, '' as periodName,                    
                v3.id as idAreaOfTon, v3.numberVal as AreaOfTon,
                usr.objsSampleExecutor,
                v5.id as idFishZone, v5.propVal as pvFishZone, v5.obj as objFishZone,
                ov5.name as nameFishZone, null as nameSampleExecutor,
                v51.id as idReservoirShore, v51.propVal as pvReservoirShore, v51.obj as objReservoirShore,
                v6.id as idBranch, v6.propVal as pvBranch, v6.obj as objBranch, ov51.name as nameReservoir,
                v7.id as idFishGear, v7.propVal as pvFishGear, v7.obj as objFishGear
            from Obj o 
                left join DataProp d3 on d3.objorrelobj=o.id and d3.periodType is not null and d3.prop=:Prop_AreaOfTon --1195
                left join DataPropVal v3 on d3.id=v3.dataprop
                inner join DataProp d5 on d5.objorrelobj=o.id and d5.periodType is not null and d5.prop=:Prop_FishZone  --1196
                left join DataPropVal v5 on d5.id=v5.dataprop
                Left join ObjVer ov5 on ov5.ownerVer=v5.obj and ov5.lastVer=1
                left join DataProp d51 on d51.objorrelobj=v5.obj and d51.prop=:Prop_ReservoirShore  --1049
                left join DataPropVal v51 on d51.id=v51.dataprop
                left join ObjVer ov51 on ov51.ownerVer=v51.obj and ov51.lastVer=1
                left join DataProp d6 on d6.objorrelobj=v51.obj and d6.prop=:Prop_Branch    --1013
                left join DataPropVal v6 on d6.id=v6.dataprop
                left join DataProp d7 on d7.objorrelobj=o.id and d7.prop=:Prop_FishGear --1064
                left join DataPropVal v7 on d7.id=v7.dataprop
                left join usr on o.id=usr.obj
            where ${whe}
            order by v5.dbeg, v5.dend desc 
        """, map)
        //Datas
        Store dataSt = mdb.loadQuery("""
            select v.id as idVal, v.numberval, 
                case when d.prop=${map.get(propParent)} then 'r_' || d.prop || '_' || d.objorrelobj || '_' || v.dbeg || '_' || v.dend
                    else 'p_' || d.prop || '_' || d.objorrelobj || '_' || v.dbeg || '_' || v.dend end as key
            from DataProp d, DataPropVal v
            where d.id=v.dataprop and d.isobj=1 and (d.prop in (0${idsPropFishRatio.join(",")}) or d.prop=${map.get(propParent)})
                and d.periodtype is not null
        """)
        StoreIndex indDataSt = dataSt.getIndex("key")
        //
        //mdb.outTable(st)
        //
        PeriodGenerator pg = new PeriodGenerator()
        for (StoreRecord r in st) {
            String nm = pg.getPeriodName(r.getDate("dbeg"), r.getDate("dend"),
                    r.getLong("periodType"), FD_PeriodNameTml_consts.arabicFull)
            r.set("periodName", nm)
            //String nmE = r.getString("objsSampleExecutor").isEmpty() ? 0 : r.getString("objsSampleExecutor")
            String nameExecutor = getNameExecutor(r.getString("objsSampleExecutor"))
            r.set("nameSampleExecutor", nameExecutor)

            for (StoreField fld in r.getFields()) {
                if (fld.name.startsWith("p_")) {
                    String key = fld.name + "_" + r.getLong("obj") + "_" + r.getString("dbeg") + "_" + r.getString("dend")
                    StoreRecord rec = indDataSt.get(key)
                    if (rec != null) {
                        r.set("id_" + fld.name, rec.getLong("idVal"))
                        r.set(fld.name, rec.getDouble("numberVal"))
                    }
                }
                if (fld.name.startsWith("r_")) {
                    String key = fld.name + "_" + r.getLong("obj") + "_" + r.getString("dbeg") + "_" + r.getString("dend")
                    StoreRecord rec = indDataSt.get(key)
                    if (rec != null) {
                        r.set(fld.name, rec.getDouble("numberVal"))
                    }
                }
            }
        }

        //mdb.outTable(st)

        //
        Map<String, Object> rez = new HashMap<>()
        rez.put("cols", cols)
        rez.put("rows", st)
        return rez
    }

    private void validateRec(VariantMap pms) {
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "", "Prop_%")
        String whe = ""
        UtPeriod utPeriod = new UtPeriod()
        String d1 = utPeriod.calcDbeg(XDate.create(pms.getString("dte")), pms.getLong("periodType"), 0)
        String d2 = utPeriod.calcDend(XDate.create(pms.getString("dte")), pms.getLong("periodType"), 0)
        if (pms.getString("mode").equalsIgnoreCase("upd"))
            whe = " and o.id<>${pms.getLong("obj")}"
        String sql = """
            select o.id as obj, o.cls
            from Obj o 
                inner join DataProp d5 on d5.objorrelobj=o.id and d5.periodType is not null and d5.prop=:Prop_FishZone  --1196
                left join DataPropVal v5 on d5.id=v5.dataprop
                Left join ObjVer ov5 on ov5.ownerVer=v5.obj and ov5.lastVer=1
                left join DataProp d51 on d51.objorrelobj=v5.obj and d51.prop=:Prop_ReservoirShore  --1049
                left join DataPropVal v51 on d51.id=v51.dataprop
                left join ObjVer ov51 on ov51.ownerVer=v51.obj and ov51.lastVer=1
                left join DataProp d6 on d6.objorrelobj=v51.obj and d6.prop=:Prop_Branch    --1013
                left join DataPropVal v6 on d6.id=v6.dataprop
                left join DataProp d7 on d7.objorrelobj=o.id and d7.prop=:Prop_FishGear     --1064
                left join DataPropVal v7 on d7.id=v7.dataprop
            where o.cls=${pms.getLong("cls")} 
                and v7.obj=${pms.getLong("objFishGear")} and v51.obj=${pms.getLong("objReservoirShore")} 
                and v5.obj=${pms.getLong("objFishZone")} and d5.periodType=${pms.getLong("periodType")} 
                and v5.dbeg='${d1}' and v5.dend='${d2}' ${whe}
        """

        Store st = mdb.loadQuery(sql, map)
        if (st.size() > 0)
            throw new XError("Забор пробы с такими атрибутами уже существует")
    }

    @DaoMethod
    Map<String, Object> saveSamplingProperties(Map<String, Object> params) {
        VariantMap pms = new VariantMap(params)
        //
        //validateRec(pms)
        //
        long own
        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        Map<String, Object> par = new HashMap<>(pms)
        String nm = pms.getString("nameFishZone") + " от " + pms.getString("dte")
        par.put("name", nm)
        par.put("fullName", nm)
        if (pms.getString("mode").equalsIgnoreCase("ins")) {
            own = eu.insertEntity(par)
            pms.put("own", own)
            //1 Prop_FishZone
            fillProperties(true, "Prop_FishZone", pms)

            //2 Prop_FishGear
            fillProperties(true, "Prop_FishGear", pms)

            //3 PropAreaOfTon
            if (pms.containsKey("AreaOfTon"))
                fillProperties(true, "Prop_AreaOfTon", pms)
        } else {
            own = pms.getLong("obj")
            par.put("id", own)
            eu.updateEntity(par)
            //
            pms.put("own", own)

            //1 Prop_FishZone
            updateProperties("Prop_FishZone", pms)

            //2 Prop_FishGear
            updateProperties("Prop_FishGear", pms)

            //3 PropAreaOfTon
            if (params.containsKey("idAreaOfTon"))
                updateProperties("Prop_AreaOfTon", pms)
            else {
                if (params.containsKey("AreaOfTon"))
                    fillProperties(true, "Prop_AreaOfTon", pms)
            }
        }
        //
        //4 Prop_SampleExecutor
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "Prop_SampleExecutor", "")
        if (map.isEmpty())
            throw new XError("NotFoundCod@Prop_SampleExecutor")
        Map<String, Long> map1 = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", "Cls_Users", "")
        if (map.isEmpty())
            throw new XError("NotFoundCod@Cls_Users")
        map.putAll(map1)
        map.put("own", own)
        //Old Values
        Store stOld = mdb.loadQuery("""
            select v1.obj, v1.id as idVal
            from DataProp d1
                left join DataPropVal v1 on d1.id=v1.dataprop
            where d1.isObj=1 and d1.prop=:Prop_SampleExecutor and d1.objorrelobj=:own --1052
        """, map)
        Set<Object> oldIds = stOld.getUniqueValues("obj")
        StoreIndex indStOld = stOld.getIndex("obj")

        Set<Long> newIds = new HashSet<>()
        for (Object ob in pms.get("objsSampleExecutor")) {
            long it = UtCnv.toLong(ob)
            newIds.add(it)
        }

        for (Object ob in pms.get("objsSampleExecutor")) {
            long it = UtCnv.toLong(ob)
            //Ins
            if (!oldIds.contains(it)) {
                pms.put("objSampleExecutor", it)

                Store stTmp = loadSqlMeta("""
                        select id from PropVal
                        where cls=${map.get("Cls_Users")} and prop=${map.get("Prop_SampleExecutor")}      --1016    1059                    
                    """, "")
                if (stTmp.size() == 0)
                    throw new XError("Not fount propVal for ClsUsers")
                pms.put("pvSampleExecutor", stTmp.get(0).getLong("id"))
                fillProperties(true, "Prop_SampleExecutor", pms)
            }
            //Del
            for (Object ob1 in oldIds) {
                long it1 = UtCnv.toLong(ob1)
                if (!newIds.contains(it1)) {
                    StoreRecord rec = indStOld.get(it1)
                    if (rec != null) {
                        long idVal = rec.getLong("idVal")
                        mdb.execQuery("""
                            delete from DataPropVal where id=${idVal};
                            delete from DataProp where id in (
                                select id from DataProp
                                except
                                select dataprop as id from DataPropVal
                            );   
                        """)
                    }
                }
            }
        }
        //
        return loadSampling(pms.getLong("cls"), true, own, pms.getString("gridMode"))
    }

    @DaoMethod
    Map<String, Object> measureInfo() {
        return apiMeta().get(ApiMeta).measureInfo()
    }

    @DaoMethod
    Store loadSampleNumber(String codCls) {
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "Prop_SampleNumber", "")
        if (map.isEmpty())
            throw new XError("NotFoundCod@Prop_SampleNumber")
        Map<String, Long> map1 = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", codCls, "")
        if (map1.isEmpty())
            throw new XError("NotFoundCod@" + codCls)

        map.putAll(map1)

        Store st = mdb.loadQuery("""
            select o.id, o.cls, v1.strval as name, 0 as pv
            from Obj o
                left join DataProp d1 on d1.isobj=1 and d1.objorrelobj=o.id and d1.prop=:Prop_SampleNumber
                inner join DataPropVal v1 on d1.id=v1.dataprop 
            where o.cls=:${codCls}
        """, map)

        Store stPV = loadSqlMeta("""
            select pv.id, pv.cls 
            from PropVal pv, Prop p 
            where pv.prop=p.id and p.cod like 'Prop_LinkToSample'
        """, "")
        StoreIndex indPV = stPV.getIndex("cls")
        for (StoreRecord r in st) {
            StoreRecord rec = indPV.get(r.getLong("cls"))
            if (rec != null)
                r.set("pv", rec.getLong("id"))
        }

        return st
    }

    @DaoMethod
    Store loadResultSampling(long cls, boolean isRec, long obj) {
        Map<String, Long> map = apiMeta().get(ApiMeta)
                .getIdFromCodOfEntity("Prop", "", "Prop_%")
        String whe = "o.id=${obj}"
        if (!isRec) {
            whe = "o.cls=${cls} and v.objParent is null"
        }
        Store st = mdb.createStore("Obj.ResultSampling")

        mdb.loadQuery(st, """
            select o.id as obj, o.cls,
                v11.id as idLinkToSample, v11.strVal as SampleNumber, v1.propVal as pvLinkToSample, v1.obj as objLinkToSample,
                v2.id as idResearchNumber, v2.strVal as ResearchNumber,
                v3.id as idResearchDate, v3.dateTimeVal as ResearchDate,
                v4.id as idResearchExecutor, v4.propVal as pvResearchExecutor, v4.obj as objResearchExecutor                
            from Obj o
                inner join ObjVer v on o.id=v.ownerVer and v.lastVer=1 
                inner join DataProp d1 on d1.objorrelobj=o.id and d1.prop=:Prop_LinkToSample     --1056
                left join DataPropVal v1 on d1.id=v1.dataprop
                left join DataProp d11 on d11.objorrelobj=v1.obj and d11.prop=:Prop_SampleNumber    --1050
                left join DataPropVal v11 on d11.id=v11.dataprop
                left join DataProp d2 on d2.objorrelobj=o.id and d2.prop=:Prop_ResearchNumber       --1057
                left join DataPropVal v2 on d2.id=v2.dataprop
                left join DataProp d3 on d3.objorrelobj=o.id and d3.prop=:Prop_ResearchDate     --1058
                left join DataPropVal v3 on d3.id=v3.dataprop
                left join DataProp d4 on d4.objorrelobj=o.id and d4.prop=:Prop_ResearchExecutor     --1059
                left join DataPropVal v4 on d4.id=v4.dataprop
            where  ${whe}
            order by v3.dateTimeVal desc
        """, map)
        return st
    }

    @DaoMethod
    Store saveResultSamplingProperties(Map<String, Object> params) {
        VariantMap pms = new VariantMap(params)
        //
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "Prop_LinkToSample", "")
        map.put("objLinkToSample", pms.getLong("objLinkToSample"))
        if (map.isEmpty())
            throw new XError("NotFoundCod@Prop_LinkToSample")
        String whe = ""
        if (pms.getString("mode").equalsIgnoreCase("upd"))
            whe = "and d.objorrelobj<>${pms.getLong("obj")}"
        Store stNot = mdb.loadQuery("""
            select d.objorrelobj, v.obj
            from DataProp d
                left join DataPropVal v on d.id=v.dataprop 
            where d.prop=:Prop_LinkToSample and v.obj=:objLinkToSample ${whe}
        """, map)
        if (stNot.size() > 0)
            throw new XError("SampleNumberExists")
        //
        map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "Prop_ResearchNumber", "")
        if (map.isEmpty())
            throw new XError("NotFoundCod@Prop_ResearchNumber")
        Store st = mdb.loadQuery("""
            select d.objorrelobj as obj, v.id
            from DataProp d, DataPropVal v
            where d.id=v.dataprop and d.prop=${map.get("Prop_ResearchNumber")} and d.isobj=1 
                and lower(trim(strVal)) like lower(trim('${pms.getString("ResearchNumber")}'))
                ${whe}
        """)
        if (st.size() > 0) {
            throw new XError("ResearchNumberExists")
        }

        long own
        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        Map<String, Object> par = new HashMap<>(pms)
        String nm = pms.getString("ResearchNumber") + " от " + pms.getString("ResearchDate")
        par.put("name", nm)
        par.put("fullName", nm)
        if (pms.getString("mode").equalsIgnoreCase("ins")) {
            own = eu.insertEntity(par)
            pms.put("own", own)
            //1 Prop_LinkToSample
            fillProperties(true, "Prop_LinkToSample", pms)

            //2 Prop_ResearchNumber
            fillProperties(true, "Prop_ResearchNumber", pms)

            //3 Prop_ResearchDate
            fillProperties(true, "Prop_ResearchDate", pms)

            //4 Prop_ResearchExecutor
            fillProperties(true, "Prop_ResearchExecutor", pms)
        } else {
            own = pms.getLong("obj")
            par.put("id", own)
            eu.updateEntity(par)
            //
            pms.put("own", own)
            //1 Prop_LinkToSample
            if (params.containsKey("idLinkToSample"))
                updateProperties("Prop_LinkToSample", pms)

            //2 Prop_ResearchNumber
            if (params.containsKey("idResearchNumber"))
                updateProperties("Prop_ResearchNumber", pms)

            //3 Prop_ResearchDate
            if (params.containsKey("idResearchDate"))
                updateProperties("Prop_ResearchDate", pms)

            //4 Prop_ResearchExecutor
            if (params.containsKey("idResearchExecutor"))
                updateProperties("Prop_ResearchExecutor", pms)

        }
        return loadResultSampling(pms.getLong("cls"), true, own)
    }

    @DaoMethod
    Store loadResultSamplingChild(long parent, boolean isRec, long obj) {
        Map<String, Long> map = apiMeta().get(ApiMeta)
                .getIdFromCodOfEntity("Prop", "", "Prop_%")
        String whe = "o.id=${obj}"
        if (!isRec) {
            whe = "v.objParent=${parent}"
        }
        Store st = mdb.createStore("Obj.ResultSamplingChild")

        mdb.loadQuery(st, """
            select o.id as obj, o.cls, v.objParent as parent,
                v1.id as idFishTyp, v1.propVal as pvFishTyp, v1.obj as objFishTyp,
                v2.id as idFishAge, v2.numberVal as FishAge,
                v3.id as idFishWeight, v3.numberVal as FishWeight,
                v4.id as idFishLength, v4.numberVal as FishLength,
                v5.id as idFishGender, v5.propVal as pvFishGender, null as fvFishGender
            from Obj o
                left join ObjVer v on o.id=v.ownerVer and v.lastVer=1 
                left join DataProp d1 on d1.objorrelobj=o.id and d1.prop=:Prop_FishTyp
                left join DataPropVal v1 on d1.id=v1.dataprop
                left join DataProp d2 on d2.objorrelobj=o.id and d2.prop=:Prop_FishAge
                left join DataPropVal v2 on d2.id=v2.dataprop
                left join DataProp d3 on d3.objorrelobj=o.id and d3.prop=:Prop_FishWeight
                left join DataPropVal v3 on d3.id=v3.dataprop
                left join DataProp d4 on d4.objorrelobj=o.id and d4.prop=:Prop_FishLength
                left join DataPropVal v4 on d4.id=v4.dataprop
                left join DataProp d5 on d5.objorrelobj=o.id and d5.prop=:Prop_FishGender     --1065
                left join DataPropVal v5 on d5.id=v5.dataprop
            where  ${whe}
        """, map)

        Map<Long, Long> mapPV = apiMeta().get(ApiMeta).mapEntityIdFromPV("factorVal", true)

        for (StoreRecord record in st) {
            record.set("fvFishGender", mapPV.get(record.getLong("pvFishGender")))
        }

        return st
    }

    @DaoMethod
    Store saveResultSamplingPropertiesChild(Map<String, Object> params) {
        VariantMap pms = new VariantMap(params)

        long own
        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
        Map<String, Object> par = new HashMap<>(pms)

        String nm = mdb.loadQuery("""
            select v.name from Obj o, ObjVer v where o.id=v.ownerVer and v.lastVer=1 and o.id=:id
        """, [id: pms.getLong("objFishTyp")]).get(0).getString("name")
        nm = nm + " child"
        par.put("name", nm)
        par.put("fullName", nm)
        if (pms.getString("mode").equalsIgnoreCase("ins")) {
            own = eu.insertEntity(par)
            pms.put("own", own)
            //1 Prop_FishTyp
            fillProperties(true, "Prop_FishTyp", pms)

            //2 Prop_FishAge
            fillProperties(true, "Prop_FishAge", pms)

            //3 Prop_FishWeight
            fillProperties(true, "Prop_FishWeight", pms)

            //4 Prop_FishLength
            fillProperties(true, "Prop_FishLength", pms)

            //5 Prop_FishGender
            fillProperties(true, "Prop_FishGender", pms)
        } else {
            own = pms.getLong("obj")
            par.put("id", own)
            eu.updateEntity(par)
            //
            pms.put("own", own)
            //1 Prop_LinkToSample
            if (params.containsKey("idFishTyp"))
                updateProperties("Prop_FishTyp", pms)

            //2 Prop_ResearchNumber
            if (params.containsKey("idFishAge"))
                updateProperties("Prop_FishAge", pms)

            //3 Prop_ResearchDate
            if (params.containsKey("idFishWeight"))
                updateProperties("Prop_FishWeight", pms)

            //4 Prop_ResearchExecutor
            if (params.containsKey("idFishLength"))
                updateProperties("Prop_FishLength", pms)

            //5 Prop_FishGender
            if (params.containsKey("idFishGender"))
                updateProperties("Prop_FishGender", pms)
            else
                fillProperties(true, "Prop_FishGender", pms)
        }
        return loadResultSamplingChild(0, true, own)
    }

    @DaoMethod
    Store loadResultHydrochemistry(long parent, boolean isRec, long obj) {
        Map<String, Long> map = apiMeta().get(ApiMeta)
                .getIdFromCodOfEntity("Prop", "", "Prop_%")
        String whe = "o.id=${obj}"
        if (!isRec) {
            whe = "v.objParent=${parent}"

            Store stTmp = mdb.loadQuery("""
                select o.id from Obj o,ObjVer v where o.id=v.ownerver and v.lastVer=1
                    and v.objParent=${parent}  
            """)
            if (stTmp.size() == 0) {
                stTmp = mdb.loadQuery("""
                    select o.id, o.cls, v.name from Obj o,ObjVer v where o.id=v.ownerver and v.lastVer=1
                        and o.id=${parent}
                """)
                EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")
                Map<String, Object> par = new HashMap<>()
                String nm = stTmp.get(0).getString("name")
                nm = nm + " child"
                par.put("name", nm)
                par.put("fullName", nm)
                par.put("cls", stTmp.get(0).getLong("cls"))
                par.put("parent", parent)
                eu.insertEntity(par)
            }

        }
        Store st = mdb.createStore("Obj.ResultHydrochemistry")

        mdb.loadQuery(st, """
            select o.id as obj, o.cls, v.objParent as parent,
                v1.id as idPh, v1.numberVal as Ph,
                v2.id as idDissGasesCO2, v2.numberVal as DissGasesCO2,
                v3.id as idDissGasesO2, v3.numberVal as DissGasesO2,
                v4.id as idDissGasesCO2Percent, v4.numberVal as DissGasesCO2Percent,
                v5.id as idBiogenicCompNH4, v5.numberVal as BiogenicCompNH4,                
                v6.id as idBiogenicCompNO2, v6.numberVal as BiogenicCompNO2,
                v7.id as idBiogenicCompNO3, v7.numberVal as BiogenicCompNO3,
                v8.id as idBiogenicCompPO4, v8.numberVal as BiogenicCompPO4,
                v9.id as idOrganicMatter, v9.numberVal as OrganicMatter,
                v10.id as idMineralization, v10.numberVal as Mineralization                
            from Obj o
                left join ObjVer v on o.id=v.ownerVer and v.lastVer=1 
                left join DataProp d1 on d1.objorrelobj=o.id and d1.prop=:Prop_Ph   --1066
                left join DataPropVal v1 on d1.id=v1.dataprop
                left join DataProp d2 on d2.objorrelobj=o.id and d2.prop=:Prop_DissGasesCO2 --1067
                left join DataPropVal v2 on d2.id=v2.dataprop
                left join DataProp d3 on d3.objorrelobj=o.id and d3.prop=:Prop_DissGasesO2  --1068
                left join DataPropVal v3 on d3.id=v3.dataprop
                left join DataProp d4 on d4.objorrelobj=o.id and d4.prop=:Prop_DissGasesCO2Percent  --1069
                left join DataPropVal v4 on d4.id=v4.dataprop
                left join DataProp d5 on d5.objorrelobj=o.id and d5.prop=:Prop_BiogenicCompNH4  --1070
                left join DataPropVal v5 on d5.id=v5.dataprop                
                left join DataProp d6 on d6.objorrelobj=o.id and d6.prop=:Prop_BiogenicCompNO2  --1071
                left join DataPropVal v6 on d6.id=v6.dataprop
                left join DataProp d7 on d7.objorrelobj=o.id and d7.prop=:Prop_BiogenicCompNO3  --1072
                left join DataPropVal v7 on d7.id=v7.dataprop
                left join DataProp d8 on d8.objorrelobj=o.id and d8.prop=:Prop_BiogenicCompPO4  --1073
                left join DataPropVal v8 on d8.id=v8.dataprop
                left join DataProp d9 on d9.objorrelobj=o.id and d9.prop=:Prop_OrganicMatter    --1074
                left join DataPropVal v9 on d9.id=v9.dataprop
                left join DataProp d10 on d10.objorrelobj=o.id and d10.prop=:Prop_Mineralization    --1075
                left join DataPropVal v10 on d10.id=v10.dataprop
            where  ${whe}
        """, map)
        return st
    }

    @DaoMethod
    Store saveResultHydrochemistryChild(Map<String, Object> params) {
        VariantMap pms = new VariantMap(params)
        pms.put("own", pms.getLong("obj"))

        //1 Prop_Ph
        if (params.containsKey("idPh"))
            updateProperties("Prop_Ph", pms)
        else
            fillProperties(true, "Prop_Ph", pms)

        //2 Prop_DissGasesCO2
        if (params.containsKey("idDissGasesCO2"))
            updateProperties("Prop_DissGasesCO2", pms)
        else
            fillProperties(true, "Prop_DissGasesCO2", pms)

        //3 Prop_DissGasesO2
        if (params.containsKey("idDissGasesO2"))
            updateProperties("Prop_DissGasesO2", pms)
        else
            fillProperties(true, "Prop_DissGasesO2", pms)

        //4 Prop_DissGasesCO2Percent
        if (params.containsKey("idDissGasesCO2Percent"))
            updateProperties("Prop_DissGasesCO2Percent", pms)
        else
            fillProperties(true, "Prop_DissGasesCO2Percent", pms)

        //5 Prop_BiogenicCompNH4
        if (params.containsKey("idBiogenicCompNH4"))
            updateProperties("Prop_BiogenicCompNH4", pms)
        else
            fillProperties(true, "Prop_BiogenicCompNH4", pms)

        //6 Prop_BiogenicCompNO2
        if (params.containsKey("idBiogenicCompNO2"))
            updateProperties("Prop_BiogenicCompNO2", pms)
        else
            fillProperties(true, "Prop_BiogenicCompNO2", pms)

        //7 Prop_BiogenicCompNO3
        if (params.containsKey("idBiogenicCompNO3"))
            updateProperties("Prop_BiogenicCompNO3", pms)
        else
            fillProperties(true, "Prop_BiogenicCompNO3", pms)

        //8 Prop_BiogenicCompPO4
        if (params.containsKey("idBiogenicCompPO4"))
            updateProperties("Prop_BiogenicCompPO4", pms)
        else
            fillProperties(true, "Prop_BiogenicCompPO4", pms)

        //9 Prop_OrganicMatter
        if (params.containsKey("idOrganicMatter"))
            updateProperties("Prop_OrganicMatter", pms)
        else
            fillProperties(true, "Prop_OrganicMatter", pms)

        //10 Mineralization
        if (params.containsKey("idMineralization"))
            updateProperties("Prop_Mineralization", pms)
        else
            fillProperties(true, "Prop_Mineralization", pms)


        return loadResultHydrochemistry(0, true, pms.getLong("obj"))
    }

    @DaoMethod
    Store loadReservorLife(Map<String, Object> params) {
        String codProp = UtCnv.toString(params.get("codProp"))
        Map<String, Long> map = apiMeta().get(ApiMeta)
                .getIdFromCodOfEntity("Prop", codProp, "")
        map.put("obj", UtCnv.toLong(params.get("obj")))
        Store st = mdb.loadQuery("""
            select v.id, v.numberval as val, d.periodtype, '' as name, v.dbeg, v.dend 
            from dataprop d, DataPropVal v 
            where d.id=v.dataprop and d.objorrelobj=:obj and d.prop=:${codProp}
            order by dbeg desc, dend
        """, map)
        PeriodGenerator pg = new PeriodGenerator()
        UtPeriod up = new UtPeriod()
        for (StoreRecord r : st) {
            XDate d1 = up.calcDbeg(XDate.create(r.getString("dbeg")), r.getLong("periodType"), 0)
            XDate d2 = up.calcDend(XDate.create(r.getString("dbeg")), r.getLong("periodType"), 0)
            String nm = pg.getPeriodName(d1, d2, r.getLong("periodType"), FD_PeriodNameTml_consts.arabicFull)
            r.set("name", nm)
        }
        return st
    }

    @DaoMethod
    String codCls(long cls) {
        return loadSqlMeta("""
            select cod from Cls where id=${cls}
        """, "").get(0).getString("cod")
    }

    @DaoMethod
    Store loadPeriodType() {
        return loadSqlMeta("""
            select id, text
            from FD_PeriodType
            where vis=1
        """, "")
    }

    @DaoMethod
    Map<String, Object> loadFishSex(Map<String, Object> params) {
        IVariantMap map = new VariantMap(params)
        Store stRO = mdb.loadQuery("""
            select r1.relobj
            from relobjmember r1
                inner join relobjmember r2 on r1.relobj=r2.relobj and r2.obj=:typeoffish
            where r1.obj=:reservoir
        """, map)
        if (stRO.size() == 0)
            throw new XError("noResults")
        long relobj = stRO.get(0).getLong("relobj")

        List<Map<String, String>> cols = new ArrayList<>()
        cols.add(Map.of("name", "name", "label", "Пол", "field", "name",
                "align", "left", "classes", "bg-blue-grey-1", "headerStyle", "font-size: 1.2em", "style", "width: 10%"))

        Map<String, Long> mapPropFishCount = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "Prop_FishCount", "")
        if (mapPropFishCount.isEmpty())
            throw new XError("NotFoundCod@Prop_FishCount")
        Store st1 = loadSqlMeta("""
            select cod, id as prop, name, ${relobj} as relobj 
            from prop
            where parent=${mapPropFishCount.get("Prop_FishCount")}
            union all
            select cod, id as prop, 'Итого' as name, ${relobj} as relobj 
            from prop
            where id=${mapPropFishCount.get("Prop_FishCount")}
        """, "Analis.fish.sex")

        Set<Object> idsPropSexCount = st1.getUniqueValues("prop")
        Map<String, Long> mapPropFishSexRatios = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "Prop_FishSexRatios", "")
        if (mapPropFishSexRatios.isEmpty())
            throw new XError("NotFoundCod@Prop_FishSexRatios")

        Store st2 = loadSqlMeta("""
            select id as prop, name 
            from prop
            where parent=${mapPropFishSexRatios.get("Prop_FishSexRatios")}
        """, "Analis.fish.sex")
        Set<Object> idsPropSexPercent = st2.getUniqueValues("prop")
        //
        PeriodGenerator pg = new PeriodGenerator()
        UtPeriod up = new UtPeriod()
        def wh = 90 / map.getInt("count")
        for (int i = 0; i < map.getInt("count"); i++) {
            XDate d1 = up.calcDbeg(XDate.create(map.getString("dte")), map.getInt("pt"), i)
            XDate d2 = up.calcDend(XDate.create(map.getString("dte")), map.getInt("pt"), i)
            String nm = pg.getPeriodName(d1, d2, map.getInt("pt"), FD_PeriodNameTml_consts.arabicFull)

            st1.addField("p_${d1.toString(XDateTimeFormatter.ISO_DATE)}_${d2.toString(XDateTimeFormatter.ISO_DATE)}", "long")
            st1.addField("id_p_${d1.toString(XDateTimeFormatter.ISO_DATE)}_${d2.toString(XDateTimeFormatter.ISO_DATE)}", "long")
            cols.add(Map.of("name", "p_" + d1.toString(XDateTimeFormatter.ISO_DATE) + "_" + d2.toString(XDateTimeFormatter.ISO_DATE), "label", nm,
                    "field", "p_" + d1.toString(XDateTimeFormatter.ISO_DATE) + "_" + d2.toString(XDateTimeFormatter.ISO_DATE), "align", "center", "classes", "bg-blue-grey-1",
                    "headerStyle", "font-size: 1.2em", "style", "width: " + wh + "%"))
            //
            st2.addField("p_${d1.toString(XDateTimeFormatter.ISO_DATE)}_${d2.toString(XDateTimeFormatter.ISO_DATE)}", "double")

        }
        //
        String d1 = up.calcDbeg(XDate.create(map.getString("dte")), map.getInt("pt"), 0)
                .toString(XDateTimeFormatter.ISO_DATE)
        String d2 = up.calcDend(XDate.create(map.getString("dte")), map.getInt("pt"), map.getInt("count") - 1)
                .toString(XDateTimeFormatter.ISO_DATE)

        Store dataSt = mdb.loadQuery("""
            select v.id as idVal, v.numberval, 'p_' || v.dbeg || '_' || v.dend || '_' || d.prop as key
            from DataProp d, DataPropVal v
            where d.id=v.dataprop and d.isobj=0 and d.objorrelobj=${relobj} and d.prop in (${idsPropSexCount.join(",")})
                and d.periodtype=${map.getInt("pt")} and v.dbeg >= '${d1}' and v.dend <= '${d2}'
        """)
        StoreIndex indDataSt = dataSt.getIndex("key")

        for (StoreRecord r in st1) {
            for (StoreField fld in r.getFields()) {
                if (fld.name.startsWith("p_")) {
                    String key = fld.name + "_" + r.getLong("prop")
                    StoreRecord rec = indDataSt.get(key)
                    if (rec != null) {
                        r.set("id_" + fld.name, rec.getLong("idVal"))
                        r.set(fld.name, rec.getLong("numberVal"))
                    }
                }
            }
        }
        //Percent
        Store dataSt2 = mdb.loadQuery("""
            select v.numberval, 'p_' || v.dbeg || '_' || v.dend || '_' || d.prop as key
            from DataProp d, DataPropVal v
            where d.id=v.dataprop and d.isobj=0 and d.objorrelobj=${relobj} and d.prop in (${idsPropSexPercent.join(",")})
                and d.periodtype=${map.getInt("pt")} and v.dbeg >= '${d1}' and v.dend <= '${d2}'
        """)
        StoreIndex indDataSt2 = dataSt2.getIndex("key")
        for (StoreRecord r in st2) {
            for (StoreField fld in r.getFields()) {
                if (fld.name.startsWith("p_")) {
                    String key = fld.name + "_" + r.getLong("prop")
                    StoreRecord rec = indDataSt2.get(key)
                    if (rec != null) {
                        r.set(fld.name, rec.getDouble("numberVal"))
                    }
                }
            }
        }
        //
        Map<String, Object> rez = new HashMap<>()
        rez.put("cols", cols)
        rez.put("rows3", st1)
        rez.put("rows4", st2)
        return rez
    }

    @DaoMethod
    Map<String, Object> loadAnalisys(Map<String, Object> params) {
        IVariantMap map = new VariantMap(params)

        List<Map<String, String>> cols = new ArrayList<>();
        cols.add(Map.of("name", "namePeriod", "label", "Период", "field", "namePeriod",
                "align", "left", "classes", "bg-blue-grey-1", "headerStyle", "font-size: 1.2em", "style", "width: 10%"))


        Store stRO = mdb.loadQuery("""
            select r.relobj, v2.name 
            from RelObjMember r 
                inner join RelObjMember r2 on r.relobj=r2.relobj and r2.obj in (
                        select o.id from Obj o, ObjVer v where o.id=v.ownerver and v.lastver=1 and v.objparent=:typeoffish
                    )
                inner join objver v2 on r2.obj=v2.ownerver and v2.lastver=1                    
                inner join relobjver v on r.relobj=v.ownerver and v.lastver=1
            where r.obj=:reservoir
        """, map)
        if (stRO.size() == 0)
            throw new XError("noResults")

        Set<Object> idsRO = stRO.getUniqueValues("relobj")

        Store st1 = mdb.createStore("Analis.input")
        Store st2 = mdb.createStore("Analis.input")
        def wh = 90 / stRO.size()
        for (StoreRecord r in stRO) {
            st1.addField("ro_${r.getLong("relobj")}", "long")
            st1.addField("id_ro_${r.getLong("relobj")}", "long")
            st2.addField("ro_${r.getLong("relobj")}", "double")
            st2.addField("id_ro_${r.getLong("relobj")}", "long")
            //
            cols.add(Map.of("name", "ro_" + r.getLong("relobj"), "label", r.getString("name"),
                    "field", "ro_" + r.getLong("relobj"), "align", "center", "classes", "bg-blue-grey-1",
                    "headerStyle", "font-size: 1.2em", "style", "width: " + wh + "%"))
        }
        Map<String, Long> mapProp = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "", "Prop_Fish%")
        //Prop_FishCount    Prop_FishAverageWeight
        PeriodGenerator pg = new PeriodGenerator()
        UtPeriod up = new UtPeriod()
        for (int i = 0; i < map.getInt("count"); i++) {
            XDate d1 = up.calcDbeg(XDate.create(map.getString("dte")), map.getInt("pt"), i)
            XDate d2 = up.calcDend(XDate.create(map.getString("dte")), map.getInt("pt"), i)
            String nm = pg.getPeriodName(d1, d2, map.getInt("pt"), FD_PeriodNameTml_consts.arabicFull)
            StoreRecord rec1 = mdb.createStoreRecord("Analis.input")
            rec1.set("namePeriod", nm)
            rec1.set("dbeg", d1.toString(XDateTimeFormatter.ISO_DATE))
            rec1.set("dend", d2.toString(XDateTimeFormatter.ISO_DATE))
            rec1.set("prop", mapProp.get("Prop_FishCount"))
            st1.add(rec1)
            StoreRecord rec2 = mdb.createStoreRecord("Analis.input")
            rec2.set("namePeriod", nm)
            rec2.set("dbeg", d1.toString(XDateTimeFormatter.ISO_DATE))
            rec2.set("dend", d2.toString(XDateTimeFormatter.ISO_DATE))
            rec2.set("prop", mapProp.get("Prop_FishAverageWeight"))
            st2.add(rec2)
        }
        //
        Store dataSt = mdb.loadQuery("""
            select v.id as idVal, v.numberval, 'ro_' || d.objorrelobj || '_' || d.prop || '_' || v.dbeg || '_' || v.dend as key
            from DataProp d, DataPropVal v
            where d.id=v.dataprop and d.prop in (0${mapProp.get("Prop_FishCount")},${mapProp.get("Prop_FishAverageWeight")}) 
                and d.periodtype=${map.getLong("pt")}
                and d.isobj=0 and d.objorrelobj in (0${idsRO.join(",")})
        """)
        StoreIndex dataStInd = dataSt.getIndex("key")
        for (StoreRecord r in st1) {
            for (StoreField fld in r.getFields()) {
                if (fld.name.startsWith("ro_")) {
                    String key = fld.name + "_" + r.getLong("prop") + "_" + r.getString("dbeg") + "_" + r.getString("dend")
                    StoreRecord rec = dataStInd.get(key)
                    if (rec != null) {
                        r.set("id_" + fld.name, rec.getLong("idVal"))
                        r.set(fld.name, rec.getLong("numberVal"))
                    }
                }
            }
        }
        for (StoreRecord r in st2) {
            for (StoreField fld in r.getFields()) {
                if (fld.name.startsWith("ro_")) {
                    String key = fld.name + "_" + r.getLong("prop") + "_" + r.getString("dbeg") + "_" + r.getString("dend")
                    StoreRecord rec = dataStInd.get(key)
                    if (rec != null) {
                        r.set("id_" + fld.name, rec.getLong("idVal"))
                        r.set(fld.name, rec.getDouble("numberVal"))
                    }
                }
            }
        }

        //
        Map<String, Object> rez = new HashMap<>()
        rez.put("cols", cols)
        rez.put("rows1", st1)
        rez.put("rows2", st2)
        return rez
    }

    @DaoMethod
    double saveForAnalise(Map<String, Object> params) {
        long prop = UtCnv.toLong(params["prop"])
        String codProp = params["codProp"]
        if (prop == 0) {
            Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", codProp, "")
            if (map.isEmpty())
                throw new XError("NotFoundCod@" + codProp)
            prop = map.get(codProp)
        }
        double val = codProp == "Prop_FishCount" ? UtCnv.toLong(params["val"]) : UtCnv.toDouble(params["val"]).round(3)
        params.put("prop", prop)
        params.put("val", val)
        params.put("inputType", FD_InputType_consts.app)
        params.put("authUser", getUser())
        if (params["mode"] == "ins") {
            apiMonitoringData().get(ApiMonitoringData).insertData(params)
        } else {
            mdb.execQuery("""
                update DataPropVal set numberVal=:v, timestamp=:t where id=:id
            """, Map.of("v", val, "t", XDateTime.now(), "id", UtCnv.toLong(params["idVal"])))
        }
        return val
    }

    @DaoMethod
    void saveFishSex(Map<String, Object> params) {
        params.put("inputType", FD_InputType_consts.app)
        double val = UtCnv.toLong(params["val"])
        params.put("val", val)
        params.put("authUser", getUser())
        if (params["mode"] == "ins") {
            apiMonitoringData().get(ApiMonitoringData).insertData(params)
        } else {
            mdb.execQuery("""
                update DataPropVal set numberVal=:v, timestamp=:t where id=:id
            """, Map.of("v", val, "t", XDateTime.now(), "id", UtCnv.toLong(params["idVal"])))
        }
        // Prop_FishCount of FishSex
        //
        saveCalcedProps(params)
    }

    private void saveCalcedProps(Map<String, Object> params) {

        Map<String, Long> mapPropFishCount = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "Prop_FishCount", "")
        if (mapPropFishCount.isEmpty())
            throw new XError("NotFoundCod@Prop_FishCount")
        params.put("prop", mapPropFishCount.get("Prop_FishCount"))
        Store stPropFishCount = loadSqlMeta("""
            select id 
            from prop
            where parent=${mapPropFishCount.get("Prop_FishCount")}
        """, "")
        Set<Object> idsPropSexCount = stPropFishCount.getUniqueValues("id")
        Store valSt = mdb.loadQuery("""
            select sum(v.numberval) as sum
            from DataProp d, DataPropVal v
            where d.id=v.dataprop and d.isobj=0 and d.objorrelobj=:objOrRelObj and d.prop in (0${idsPropSexCount.join(",")})
                and d.periodtype=:periodType and v.dbeg=:dbeg and v.dend=:dend
        """, params)
        int sumVal = 0
        if (valSt.size() > 0)
            sumVal = valSt.get(0).getInt("sum")
        params.put("val", sumVal)
        Store stTmp = mdb.loadQuery("""
                select v.id from DataProp d, DataPropVal v
                where d.id=v.dataProp and d.isObj=0 and d.prop=:prop and d.objOrRelObj=:objOrRelObj
                    and d.periodType=:periodType and v.dbeg=:dbeg and v.dend=:dend
            """, params)
        if (stTmp.size() == 0) {
            apiMonitoringData().get(ApiMonitoringData).insertData(params)
        } else {
            long idVal = stTmp.get(0).getLong("id")
            mdb.execQuery("""
                    update DataPropVal set numberVal=:v, timestamp=:t where id=:id
                """, Map.of("v", sumVal, "t", XDateTime.now(), "id", idVal))
        }
        // [Prop_FishCountFemale, Prop_FishCountMale, Prop_FishCountJuvenile] of FishSex
        Store st3 = mdb.loadQuery("""
            select d.prop, '' as cod, v.numberVal
            from DataProp d, DataPropVal v
            where d.id=v.dataprop and d.isobj=0 
                and d.objorrelobj=:objOrRelObj and d.prop in (0${idsPropSexCount.join(",")})
                and d.periodtype=:periodType and v.dbeg=:dbeg and v.dend=:dend
        """, params)
        for (StoreRecord r in st3) {
            Store s = loadSqlMeta("""
                select cod from Prop where id=${r.getLong("prop")}
            """, "")
            r.set("cod", s.get(0).getString("cod"))
        }
        //Delete all percents
        stTmp = loadSqlMeta("""
            select id from Prop
            where parent in (select id from Prop where cod='Prop_FishSexRatios')
        """, "")
        Set<Object> idsPropsForDel = stTmp.getUniqueValues("id")
        mdb.execQuery("""
            with dv as (
                select v.id
                from DataProp d, DataPropVal v
                where d.id=v.dataprop and d.isobj=0 
                    and d.objorrelobj=:objOrRelObj and d.prop in (0${idsPropsForDel.join(",")})
                    and d.periodtype=:periodType and v.dbeg=:dbeg and v.dend=:dend
            )
            delete from DataPropVal where id in (select id from dv);
            delete from DataProp where id in (
                select id from DataProp
                except
                select dataprop as id from DataPropVal
            );
        """, params)
        //
        Map<String, String> mapCnv = Map.of("Prop_FishCountFemale", "Prop_FishSexRatiosFemale",
                "Prop_FishCountMale", "Prop_FishSexRatiosMale",
                "Prop_FishCountJuvenile", "Prop_FishSexRatiosJuvenile")
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "", "Prop_FishSex%")
        sumVal = sumVal > 0 ? sumVal : 1
        for (StoreRecord r in st3) {
            if (r.getLong("numberVal") > 0) {
                double v = r.getDouble("numberVal") / sumVal * 100
                params.put("prop", map.get(mapCnv.get(r.getString("cod"))))
                params.put("val", v)
                saveFishSexPercent(params)
            }
        }
    }

    private void saveFishSexPercent(Map<String, Object> params) {
        Store stTmp = mdb.loadQuery("""
                select v.id from DataProp d, DataPropVal v
                where d.id=v.dataProp and d.isObj=0 and d.prop=:prop and d.objOrRelObj=:objOrRelObj
                    and d.periodType=:periodType and v.dbeg=:dbeg and v.dend=:dend
            """, params)
        if (stTmp.size() == 0) {
            params.put("val", UtCnv.toDouble(params.get("val")).round(2))
            apiMonitoringData().get(ApiMonitoringData).insertData(params)
        } else {
            long idVal = stTmp.get(0).getLong("id")
            double val = UtCnv.toDouble(params.get("val")).round(2)
            mdb.execQuery("""
                    update DataPropVal set numberVal=:v, timestamp=:t where id=:id
                """, Map.of("v", val, "t", XDateTime.now(), "id", idVal))
        }
    }

    @DaoMethod
    void deleteAnalise(long id) {
        mdb.execQuery("""
            delete from DataPropVal where id=${id};
            delete from DataProp where id in (
                select id from dataprop
                except
                select dataprop as id from DataPropVal            
            );
        """)
    }

    @DaoMethod
    void deleteFishSex(long id, Map<String, Object> params) {
        try {
            params.put("authUser", getUser())
            params.put("inputType", FD_InputType_consts.app)

            mdb.execQuery("""
            delete from DataPropVal where id=${id};
            delete from DataProp where id in (
                select id from dataprop
                except
                select dataprop as id from DataPropVal            
            );
        """)
        } finally {
            saveCalcedProps(params)
        }
    }

    private void saveCalcedProp_FishRatio(Map<String, Object> params) {
        IVariantMap pms = new VariantMap(params)
        String codProp = pms.getString("codProp")
        Store stPropFishCount = loadSqlMeta("""
            select id 
            from prop
            where parent=${pms.getLong("prop")}
        """, "")
        Set<Object> idsPropFishRatio = stPropFishCount.getUniqueValues("id")
        Store stSum = mdb.loadQuery("""
            select sum(v.numberval) as sum
            from DataProp d, DataPropVal v
            where d.id=v.dataprop and d.isobj=1 and d.prop in (0${idsPropFishRatio.join(",")})
                and d.objorrelobj=:objOrRelObj and d.periodtype=:periodType and v.dbeg=:dbeg and v.dend=:dend
        """, pms)
        double val = 0
        if (stSum.size() > 0)
            val = codProp == "Prop_FishRatio" ? stSum.get(0).getLong("sum") : stSum.get(0).getDouble("sum").round(3)

        pms.put("inputType", FD_InputType_consts.app)
        pms.put("authUser", getUser())
        pms.put("val", val)
        pms.put("prop", pms.getLong("prop"))

        Store stParent = mdb.loadQuery("""
            select v.id
            from DataProp d, DataPropVal v
            where d.id=v.dataprop and d.isobj=1 and d.prop=${pms.getLong("prop")}
                and d.objorrelobj=:objOrRelObj and d.periodtype=:periodType and v.dbeg=:dbeg and v.dend=:dend
        """, pms)

        if (val == 0 && params.containsKey("isDel")) {
            mdb.execQuery("""
                delete from DataPropVal where id=${stParent.get(0).getLong("id")};
                delete from DataProp where id in (
                    select id from dataprop
                    except
                    select dataprop as id from DataPropVal            
                );
            """)
        } else {
            if (stParent.size() == 0) {
                apiMonitoringData().get(ApiMonitoringData).insertData(pms)
            } else {
                mdb.execQuery("""
                    update DataPropVal set numberVal=:v, timestamp=:t where id=:id
                """, Map.of("v", val, "t", XDateTime.now(), "id", stParent.get(0).getLong("id")))
            }
        }
    }

    @DaoMethod
    void saveSamplingTab1(Map<String, Object> params) {
        long prop = UtCnv.toLong(params["prop"])
        String codProp = params["codProp"]

        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", codProp, "")
        if (map.isEmpty())
            throw new XError("NotFoundCod@" + codProp)

        double val = codProp == "Prop_FishRatio" ? UtCnv.toLong(params["val"]) : UtCnv.toDouble(params["val"]).round(3)
        params.put("val", val)
        params.put("prop", prop)
        params.put("inputType", FD_InputType_consts.app)
        params.put("authUser", getUser())
        try {
            if (params["mode"] == "ins") {
                apiMonitoringData().get(ApiMonitoringData).insertData(params)
            } else {
                mdb.execQuery("""
                    update DataPropVal set numberVal=:v, timestamp=:t where id=:id
                """, Map.of("v", val, "t", XDateTime.now(), "id", UtCnv.toLong(params["idVal"])))
            }
        } finally {
            params.put("prop", map.get(codProp))
            saveCalcedProp_FishRatio(params)
        }

    }

    @DaoMethod
    void deleteSamplingTab1(long id, Map<String, Object> params) {
        try {
            params.put("authUser", getUser())
            params.put("inputType", FD_InputType_consts.app)

            mdb.execQuery("""
                delete from DataPropVal where id=${id};
                delete from DataProp where id in (
                    select id from dataprop
                    except
                    select dataprop as id from DataPropVal            
                );
            """)
        } finally {
            String codProp = params["codProp"]
            Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", codProp, "")
            if (map.isEmpty())
                throw new XError("NotFoundCod@" + codProp)
            params.put("prop", map.get(codProp))
            params.put("isDel", 1)
            saveCalcedProp_FishRatio(params)
        }
    }

    //-------------------------

    private long insertRecToTable(String tableName, Map<String, Object> params, String model, String metamodel, boolean generateId) {
        if (metamodel == "fish") {
            if (model.equalsIgnoreCase("userdata"))
                return apiUserData().get(ApiUserData).insertRecToTable(tableName, params, generateId)
            else if (model.equalsIgnoreCase("nsidata"))
                return apiNSIData().get(ApiNSIData).insertRecToTable(tableName, params, generateId)
            else
                throw new XError("Unknown model [${model}]")
        }
        throw new XError("Unknown id metamodel")
    }

    private Store loadSql(String sql, String domain, String model) {
        if (model.equalsIgnoreCase("userdata"))
            return apiUserData().get(ApiUserData).loadSql(sql, domain)
        else if (model.equalsIgnoreCase("nsidata"))
            return apiNSIData().get(ApiNSIData).loadSql(sql, domain)
        else
            throw new XError("Unknown model [${model}]")
    }

    private Store loadSqlMeta(String sql, String domain) {
        return apiMeta().get(ApiMeta).loadSql(sql, domain)
    }

    @DaoMethod
    Map<String, Object> getCurUserInfo() {
        AuthService authSvc = mdb.getApp().bean(AuthService.class)
        AuthUser au = authSvc.getCurrentUser()
        if (au == null) {
            throw new XError("NotLogined")
        }
        return au.getAttrs()
    }

    private long getUser() throws Exception {
        AuthService authSvc = mdb.getApp().bean(AuthService.class)
        long au = authSvc.getCurrentUser().getAttrs().getLong("id")
        if (au == 0)
            throw new XError("notLogined")
        return au
    }


}
