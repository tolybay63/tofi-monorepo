package fish.monitoring.dao

import jandcode.commons.UtCnv
import jandcode.commons.datetime.XDate
import jandcode.commons.datetime.XDateTime
import jandcode.commons.datetime.XDateTimeFormatter
import jandcode.commons.error.XError
import jandcode.core.dao.DaoMethod
import jandcode.core.dbm.domain.Domain
import jandcode.core.dbm.mdb.BaseMdbUtils
import jandcode.core.store.Store
import jandcode.core.store.StoreIndex
import jandcode.core.store.StoreRecord
import tofi.api.dta.ApiNSIData
import tofi.api.dta.model.utils.EntityMdbUtils
import tofi.api.dta.model.utils.UtPeriod
import tofi.api.mdl.ApiMeta
import tofi.api.mdl.model.consts.FD_AttribValType_consts
import tofi.api.mdl.model.consts.FD_InputType_consts
import tofi.api.mdl.model.consts.FD_PropType_consts
import tofi.apinator.ApinatorApi
import tofi.apinator.ApinatorService
import fish.monitoring.dao.utils.XLSXReader_withoutDescription

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

    @DaoMethod
    void fillReservoirMeter(File file, boolean fill) {
        Store st = mdb.createStore()
        Domain d = mdb.createDomain(st)
        XLSXReader_withoutDescription reader = new XLSXReader_withoutDescription(mdb, file, d, st)


        int count = 0
        boolean errHeader = false
        def eachLineTest = { Map m ->
            count++
            if (count == 1) {
                if (!(m.keySet().containsAll(["owner", "isObj", "prop", "periodType", "dbeg", "dend", "value"]) && ["owner", "isObj", "prop", "status", "provider", "periodType", "dbeg", "dend", "value"].containsAll(m.keySet())))
                    errHeader = true
            }
        }

        count = 0
        int countErr = 0
        boolean errFill = false
        boolean errLine = false
        Set<String> textErr = new HashSet<>()
        def eachLine = { Map m ->
            count++
            errLine = false
            long owner = UtCnv.toLong(m.get("owner"))
            int isObj = UtCnv.toInt(m.get("isObj"))
            String codProp = UtCnv.toString(m.get("prop"))
            long status = UtCnv.toLong(m.get("status"))
            long provider = UtCnv.toLong(m.get("provider"))
            int periodType = UtCnv.toInt(m.get("periodType"))
            String dbeg = UtCnv.toString(m.get("dbeg"))
            String dend = UtCnv.toString(m.get("dend"))
            String value = UtCnv.toString(m.get("value"))
            if (owner == 0) {
                errLine = true
                textErr.add("Строка ${count + 1}: owner=${owner}")
            }
            if (isObj < 0 || isObj > 1) {
                errLine = true
                textErr.add("Строка ${count + 1}: isObj=${isObj}")
            }
            if (codProp.isEmpty()) {
                errLine = true
                textErr.add("Строка ${count + 1}: prop=BLANK")
            }
            if (![11, 21, 31, 41, 51, 61, 71].contains(periodType)) {
                errLine = true
                textErr.add("Строка ${count + 1}: periodType=${periodType}")
            }
            if (XDate.create(dbeg).toJavaLocalDate().isAfter(XDate.create(dend).toJavaLocalDate())) {
                errLine = true
                textErr.add("Строка ${count + 1}: dbeg=${dbeg} dend=${dend}")
            }
            if (UtCnv.toString(m.get("value")).isEmpty()) {
                errLine = true
                textErr.add("Строка ${count + 1}: value=${value}")
            }

            if (!errLine) { //fill
                try {
                    fillPropertiesMeter(owner, isObj, codProp, status, provider, periodType, dbeg, dend, value)
                } catch (Exception e) {
                    errLine = true
                    textErr.add("Строка ${count + 1}: prop=${codProp}")
                    errFill = true
                    countErr++
                }
            } else {
                errFill = true
                countErr++
            }
        }


        //*******************************************************
        // Основное тело алгоритма
        //*******************************************************

        if (fill) {
            mdb.execQuery("""
                update log set err=0, msg='' where id=1
            """)
            //
            reader.eachRow(eachLine)
            //
            Set<String> set = new HashSet<>()
            String cnt = "${count - countErr}/${count}"
            set.add(cnt)

            if (errFill) {
                set.add(textErr.join(";"))
            }
            String msg = set.join("@")
            mdb.execQuery("""
                update log set err=1, msg='${msg}' where id=1
            """)
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
            if (!errHeader) {
                mdb.execQuery("""
                    update log set err=0, msg='', cnt=${count} where id=1
                """)
            } else {
                String msg = "Заголовок файла не соответствует шаблону"
                mdb.execQuery("""
                        update log set err=1, msg='${msg}', cnt=${count} where id=1
                    """)
            }
        }
    }

    void fillPropertiesMeter(long own, int isObj, String codProp, def status, def provider,
                             long periodType, String dbeg, String dend, String value) {
        //
        Store stProp = apiMeta().get(ApiMeta).getPropInfo(codProp)
        if (stProp.size() == 0)
            throw new XError("Не найден код пропа [{0}]", codProp)
        //
        def prop = stProp.get(0).getLong("id")
        long propType = stProp.get(0).getLong("propType")
        Integer digit = null
        double koef = stProp.get(0).getDouble("koef")
        if (koef == 0) koef = 1
        if (!stProp.get(0).isValueNull("digit"))
            digit = stProp.get(0).getInt("digit")
        long idDP = 0
        //
        String whe = ""
        if (status > 0)
            whe = "and d.status=${status} "
        else
            whe = "and d.status is null "

        if (provider > 0)
            whe = whe + "and d.provider=${provider}"
        else
            whe = "and d.provider is null"
        Store st = mdb.loadQuery("""
            select d.id from DataProp d, DataPropVal v
            where d.id=v.dataProp and d.isObj=${isObj} and d.objorrelobj=${own} and d.prop=${prop} and d.periodType=${periodType}
                ${whe}
        """)

        if (st.size()>0)
            idDP = st.get(0).getLong("id")
        else {
            StoreRecord recDP = mdb.createStoreRecord("DataProp")
            recDP.set("isObj", isObj)
            recDP.set("objOrRelObj", own)
            recDP.set("prop", prop)
            if (status > 0)
                recDP.set("status", status)
            if (provider > 0)
                recDP.set("provider", provider)
            recDP.set("periodType", periodType)
            idDP = mdb.insertRec("DataProp", recDP, true)
        }
        //
        StoreRecord recDPV = mdb.createStoreRecord("DataPropVal")
        recDPV.set("dataProp", idDP)
        // For Meter
        if (codProp.equalsIgnoreCase("Prop_MaximumAllowableCatch") ||
                codProp.equalsIgnoreCase("Prop_FishNumber") ||
                codProp.equalsIgnoreCase("Prop_FishIchthyomassa") ||
                codProp.equalsIgnoreCase("Prop_FishIndustrialStock") ||
                codProp.equalsIgnoreCase("Prop_FishWithdrawalRate") ||
                codProp.equalsIgnoreCase("Prop_FishBiologicalCapacity") ||
                codProp.equalsIgnoreCase("Prop_FishMinimumSusPopulation") ||
                codProp.equalsIgnoreCase("Prop_FishAbsolutNumber") ||
                codProp.equalsIgnoreCase("Prop_FishCriticalValueBiomass") ||
                codProp.equalsIgnoreCase("Prop_FishNumberComStocksYear") ||
                codProp.equalsIgnoreCase("Prop_FishComStockBiomassYear") ||
                codProp.equalsIgnoreCase("Prop_ReservoirHydroLevel") ||
                codProp.equalsIgnoreCase("Prop_WaterArea") ||
                codProp.equalsIgnoreCase("Prop_FishAverageWeight") ||
                codProp.equalsIgnoreCase("Prop_FishCount") ||
                codProp.equalsIgnoreCase("Prop_FishCountFemale") ||
                codProp.equalsIgnoreCase("Prop_FishCountMale") ||
                codProp.equalsIgnoreCase("Prop_FishCountJuvenile")) {
            if (!value.isEmpty()) {
                double v = UtCnv.toDouble(value)
                v = v / koef
                if (digit) v = v.round(digit)
                recDPV.set("numberVal", v)
            }
        } else {
            throw new XError("for dev: [${codProp}] отсутствует в реализации")
        }
        recDPV.set("dbeg", dbeg)
        recDPV.set("dend", dend)
        recDPV.set("authUser", 1)
        recDPV.set("inputType", FD_InputType_consts.app)
        long idDPV = mdb.getNextId("DataPropVal")
        recDPV.set("id", idDPV)
        recDPV.set("ord", idDPV)
        recDPV.set("timeStamp", XDateTime.create(new Date()).toString(XDateTimeFormatter.ISO_DATE_TIME))
        mdb.insertRec("DataPropVal", recDPV, false)
    }
}


