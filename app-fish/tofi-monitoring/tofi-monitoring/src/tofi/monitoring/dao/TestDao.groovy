package tofi.monitoring.dao

import jandcode.commons.UtCnv
import jandcode.commons.error.XError
import jandcode.core.dao.DaoMethod
import jandcode.core.dbm.domain.Domain
import jandcode.core.dbm.mdb.BaseMdbUtils
import jandcode.core.store.Store
import jandcode.core.store.StoreIndex
import jandcode.core.store.StoreRecord
import tofi.api.dta.ApiNSIData
import tofi.api.dta.model.utils.EntityMdbUtils
import tofi.api.mdl.ApiMeta
import tofi.apinator.ApinatorApi
import tofi.apinator.ApinatorService
import tofi.monitoring.dao.utils.XLSXReader_withoutDescription

class TestDao extends BaseMdbUtils {

    ApinatorApi apiMeta() {
        return getApp().bean(ApinatorService.class).getApi("meta")
    }

    ApinatorApi apiNSIData() {
        return getApp().bean(ApinatorService.class).getApi("nsidata")
    }

    protected static boolean isInteger(String s) {
        if (s == null || s.isEmpty()) return false
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false
            }
        }
        return true
    }

    @DaoMethod
    Store loadLog() {
        return mdb.loadQuery("""
            select * from log
        """)
    }

    @DaoMethod
    void fillReservoir(File file, boolean fill) {

        Store st = mdb.createStore()
        Domain d = mdb.createDomain(st)
        XLSXReader_withoutDescription reader = new XLSXReader_withoutDescription(mdb, file, d, st)


        //int countErr=0

        StoreIndex indexRegionBranch

        //свойства типа атрибут
        def props_atrib = [
                "Prop_Location": 1031L,
        ]

        //свойства типа измеритель
        def props_meter = [
                "Prop_WaterArea": 1004L
        ]

        //свойства типа объект
        def props_obj = [
                "Prop_Region"  : 1000L,
                "Prop_District": 1044L,
                "Prop_Branch"  : 1013L,
        ]

        //свойства типа fv
        def props_fv = [
                "Prop_ReservoirType"  : 1002L,
                "Prop_FishFarmingType": 1045L,
        ]

        DataDao dao = new DataDao(mdb)

        //*******************************************************
        // Анализ свойств
        //******************************************************
        def eachProps = { def m ->

            /* Obj */
            for (def k : props_obj.keySet()) {
                if (!m.get(k)) continue
                Map<String, Object> pms = new HashMap<>()
                pms.put("own", m.get("own"))
                pms.put(k, props_obj.get(k))
                pms.put(k.split("_")[1], m.get(k))
                pms.put("obj" + k.split("_")[1], m.get(k))
                StoreRecord rec = indexRegionBranch.get(UtCnv.toLong(m.get(k)))
                if (rec != null) {
                    pms.put("pv" + k.split("_")[1], rec.getLong("pv"))
                    dao.fillProperties(true, k, pms)
                }
            }

            /* Attrib */
            for (def k : props_atrib.keySet()) {
                if (!m.get(k)) continue
                Map<String, Object> pms = new HashMap<>()
                pms.put("own", m.get("own"))
                pms.put(k, props_atrib.get(k))
                pms.put(k.split("_")[1], m.get(k))
                if (UtCnv.toString(m.get(k)).trim() != "") {
                   dao.fillProperties(true, k, pms)
                }
            }

            /* Meter */
            for (def k : props_meter.keySet()) {
                if (!m.get(k)) continue
                Map<String, Object> pms = new HashMap<>()
                pms.put("own", m.get("own"))
                pms.put(k, props_meter.get(k))
                pms.put(k.split("_")[1], m.get(k))
                if (UtCnv.toDouble(m.get(k)) != 0) {
                   dao.fillProperties(true, k, pms)
                }
            }

            /* FV */
            for (def k : props_fv.keySet()) {
                if (!m.get(k)) continue
                Map<String, Object> pms = new HashMap<>()
                pms.put("own", m.get("own"))
                pms.put(k, props_fv.get(k))
                pms.put("pv" + k.split("_")[1], m.get(k))
                if (UtCnv.toLong(m.get(k)) > 0) {
                   dao.fillProperties(true, k, pms)
                }
            }

        }


        //*******************************************************
        // Функция обработки строк файла
        //******************************************************
        def eachLine = { def m ->
            long idObj = 0L
            def cls = UtCnv.toLong(m.get("cls"))
            def name = UtCnv.toString(m.get("name")).trim()

            if (cls > 0 && name) {

                try {
                    StoreRecord r = mdb.createStoreRecord("Obj.full")
                    r.set("cls", cls)
                    r.set("name", name)
                    r.set("fullName", name)
                    r.set("accessLevel", 1L)
                    EntityMdbUtils ent = new EntityMdbUtils(mdb, "Obj")
                    idObj = ent.insertEntity(r.getValues())
                } catch (Exception e) {
                    println("Ошибка при создании Obj (cls, name) = ${cls}, ${name}")
                    e.printStackTrace()
                }

                if (idObj > 0)
                    eachProps(m)
            }
        }


        //*******************************************************
        // Функция обработки строк файла
        //******************************************************

        Set<Long> idsCls = new HashSet<>()
        Set<Long> idsRegionBranch = new HashSet<>()
        def eachLineCalc = { Map m ->
            idsCls.add(UtCnv.toLong(m.get("cls")))
            idsRegionBranch.add(UtCnv.toLong(m.get("Prop_Region"))) //*
            idsRegionBranch.add(UtCnv.toLong(m.get("Prop_Branch"))) //*
            idsRegionBranch.add(UtCnv.toLong(m.get("Prop_District")))
        }
        //
        Set<String> reqFields = new HashSet<>()
        Set<String> emptyFields = new HashSet<>()
        Set<String> setDubleObj = new HashSet<>()
        int count = 0
        def eachLineTest = { Map m ->
            count++
            if (count == 1) {
                if (!m.keySet().contains("cls")) reqFields.add("cls")
                if (!m.keySet().contains("name")) reqFields.add("name")
                if (!m.keySet().contains("Prop_Region")) reqFields.add("Prop_Region")
                if (!m.keySet().contains("Prop_Branch")) reqFields.add("Prop_Branch")
            }

            if (!isInteger(UtCnv.toString(m.get("cls"))))
                emptyFields.add("cls")
            if (!m.get("name"))
                emptyFields.add("name")
            if (!isInteger(UtCnv.toString(m.get("Prop_Region"))))
                emptyFields.add("Prop_Region")
            if (!isInteger(UtCnv.toString(m.get("Prop_Branch"))))
                emptyFields.add("Prop_Branch")
            //
            def cls = UtCnv.toLong(m.get("cls"))
            def name = UtCnv.toString(m.get("name")).trim()
            def region = UtCnv.toLong(m.get("Prop_Region"))
            def branch = UtCnv.toLong(m.get("Prop_Branch"))
            def nm = name.toLowerCase()
            Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "", "Prop_%")
            Store stTest = mdb.loadQuery("""
                select o.id as obj
                from Obj o
                    inner join ObjVer v on o.id=v.ownerVer and v.lastVer=1 and trim(lower(v.name))='${nm}'
                    inner join DataProp d1 on d1.isobj=1 and d1.objorrelobj=o.id and d1.prop=:Prop_Region
                    inner join DataPropVal v1 on d1.id=v1.dataprop and v1.obj=${region}
                    inner join DataProp d2 on d2.isobj=1 and d2.objorrelobj=o.id and d2.prop=:Prop_Branch
                    inner join DataPropVal v2 on d2.id=v2.dataprop and v2.obj=${branch}
                where o.cls=${cls}
            """, map)
            if (stTest.size() > 0)
                setDubleObj.add("${name}, ${region}, ${branch}")
        }


        //*******************************************************
        // Основное тело алгоритма
        //*******************************************************

        if (fill) {
            reader.eachRow(eachLineCalc)
            //
            Store stReg = apiNSIData().get(ApiNSIData).loadSql("""
                select id, cls, 0 as pv from Obj where id in (${idsRegionBranch.join(",")})
            """, "")
            Map<Long, Long> mapPV = apiMeta().get(ApiMeta).mapEntityIdFromPV("Cls", false)
            for (StoreRecord r in stReg) {
                if (mapPV.get(r.getLong("cls")) > 0)
                    r.set("pv", mapPV.get(r.getLong("cls")))
                else
                    throw new XError("Не указан возможное значение класса [" + r.getString("cls") + "]")
            }
            indexRegionBranch = stReg.getIndex("id")
            //
            reader.eachRow(eachLine)
        } else {
            mdb.execQuery("""
                CREATE TABLE IF NOT EXISTS log (
                    id int8 NOT NULL,
                    msg varchar(800) NULL,
                    cnt int8 NULL,
                    err int2 NULL,
                    CONSTRAINT pk_log PRIMARY KEY (id)
                );                
                ALTER TABLE log OWNER TO pg;
                GRANT ALL ON TABLE log TO pg; 
                INSERT INTO log (id, msg, cnt, err)
                    SELECT 1 AS id, '' AS msg, 0 as cnt, 0 as err FROM log
                    WHERE NOT EXISTS ( SELECT id FROM log WHERE id = 1 );
                update log set err=0, msg='', cnt=0 where id=1;
            """)
            //
            reader.eachRow(eachLineTest)
            //
            if (reqFields.isEmpty() && emptyFields.isEmpty() && setDubleObj.isEmpty()) {
                mdb.execQuery("""
                    update log set err=0, msg='', cnt=${count} where id=1
                """)
            } else {
                if (!reqFields.isEmpty()) {
                    String msg = "Наименования обязательных полей отсутствует: [${reqFields.join(', ')}]"
                    mdb.execQuery("""
                        update log set err=1, msg='${msg}' where id=1
                    """)
                } else if (!emptyFields.isEmpty()) {
                    String msg = "Некоторые значения обязательных полей отсутствует: [${emptyFields.join(', ')}]"
                    mdb.execQuery("""
                        update log set err=1, msg='${msg}' where id=1
                    """)
                } else if (!setDubleObj.isEmpty()) {
                    String msg = "Повтор [" + setDubleObj.join("; ") + "]"
                    mdb.execQuery("""
                        update log set err=1, msg='${msg}' where id=1
                    """)
                }
            }
        }
    }


}


