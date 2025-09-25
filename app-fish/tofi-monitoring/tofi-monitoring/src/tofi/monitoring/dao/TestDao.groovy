package tofi.monitoring.dao

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
                if ( !(m.keySet().containsAll(["owner","isObj","prop","periodType","dbeg","dend","value"]) && ["owner","isObj","prop","status","provider","periodType","dbeg","dend","value"].containsAll(m.keySet())) )
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
            long prop = UtCnv.toLong(m.get("prop"))
            long status = UtCnv.toLong(m.get("status"))
            long provider = UtCnv.toLong(m.get("provider"))
            int periodType = UtCnv.toInt(m.get("periodType"))
            String dbeg = UtCnv.toString(m.get("dbeg"))
            String dend = UtCnv.toString(m.get("dend"))
            double value = UtCnv.toDouble(m.get("value"))
            if (owner==0) {
                errLine = true
                textErr.add("Строка ${count+1}: owner=${owner}")
            }
            if (isObj < 0 || isObj > 1) {
                errLine = true
                textErr.add("Строка ${count+1}: isObj=${isObj}")
            }
            if (prop==0) {
                errLine = true
                textErr.add("Строка ${count+1}: prop=${prop}")
            }
            if (![11,21,31,41,51,61,71].contains(periodType)) {
                errLine = true
                textErr.add("Строка ${count+1}: periodType=${periodType}")
            }
            if (XDate.create(dbeg).toJavaLocalDate().isAfter(XDate.create(dend).toJavaLocalDate())) {
                errLine = true
                textErr.add("Строка ${count+1}: dbeg=${dbeg} dend=${dend}")
            }
            if (UtCnv.toString(m.get("value")).isEmpty()) {
                errLine = true
                textErr.add("Строка ${count+1}: value=${value}")
            }

            if (!errLine) { //fill
                System.out.println(count)
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
            if (errFill) {
                String cnt = "${count-countErr}/${count}"
                Set<String> set = new HashSet<>()
                set.add(cnt)
                set.add(textErr.join(";"))
                String msg = set.join("@")
                mdb.execQuery("""
                    update log set err=1, msg='${msg}' where id=1
                """)
            }


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
                        update log set err=1, msg='${msg}' where id=1
                    """)
            }
        }
    }

    void fillPropertiesMeter(long own, boolean isObj, String codProp, def status, def provider,
            long periodType, double value) {
        //
        Store stProp = apiMeta().get(ApiMeta).getPropInfo(codProp)
        if (stProp.size()==0)
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
        if (status)
        String sql = """
            select d.id from DataProp d, DataPropVal v
            where d.id=v.dataProp and d.isObj=${isObj} and d.objorrelobj=${own} and d.prop=${prop} and d.periodType=${periodType}
                v.db
        """

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
}


