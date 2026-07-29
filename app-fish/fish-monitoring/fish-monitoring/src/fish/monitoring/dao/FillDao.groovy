package fish.monitoring.dao

import fish.monitoring.dao.utils.XLSXReader_withoutDescription
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
import tofi.api.dta.ApiMonitoringData
import tofi.api.dta.ApiPersonnelData
import tofi.api.dta.model.utils.UtPeriod
import tofi.api.mdl.ApiMeta
import tofi.api.mdl.model.consts.FD_InputType_consts
import tofi.api.mdl.model.consts.FD_PeriodType_consts
import tofi.apinator.ApinatorApi
import tofi.apinator.ApinatorService

import java.time.LocalDate
import java.time.format.DateTimeFormatter


class FillDao extends BaseMdbUtils {

    ApinatorApi apiMeta() { return getApp().bean(ApinatorService.class).getApi("meta") }

    ApinatorApi apiPersonnelData() { return getApp().bean(ApinatorService.class).getApi("personneldata") }

    ApinatorApi apiMonitoringData() { return getApp().bean(ApinatorService.class).getApi("monitoringdata") }

    private static boolean isInteger(String s) {
        if (s == null || s.isEmpty()) return false
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false
            }
        }
        return true
    }

    private void saveMeter(long own, boolean isObj, long prop, double val, long periodType, String dte) {
        Store stProp = apiMeta().get(ApiMeta).loadSql("""
                select p.id, p.cod, p.proptype, a.attribvaltype, p.isuniq, p.isdependvalueonperiod as dependperiod,
                    p.statusfactor, p.providertyp, m.kfrombase as koef, p.digit  
                from Prop p
                    left join Attrib a on a.id=p.attrib
                    left join Measure m on m.id=p.measure
                where p.id=${prop}
        """, "")
        //
        Integer digit = null
        double koef = stProp.get(0).getDouble("koef")
        if (koef == 0) koef = 1
        if (!stProp.get(0).isValueNull("digit"))
            digit = stProp.get(0).getInt("digit")

        long idDP
        StoreRecord recDP = mdb.createStoreRecord("DataProp")
        String whe = isObj ? "and isObj=1 " : "and isObj=0 "
        if (stProp.get(0).getLong("statusFactor") > 0) {
            long fv = apiMeta().get(ApiMeta).getDefaultStatus(prop)
            whe += "and status = ${fv} "
        } else {
            whe += "and status is null "
        }
        //todo if (stProp.get(0).getLong("providerTyp") > 0)
        whe += "and provider is null "
        if (stProp.get(0).getBoolean("dependPeriod")) {
            whe += "and periodType=${FD_PeriodType_consts.month} "
        } else {
            whe += "and periodType is null "
        }
        Store stDP = mdb.loadQuery("""
            select * from DataProp
            where objOrRelObj=${own} and prop=${prop} ${whe}
        """)
        if (stDP.size() > 0) {
            idDP = stDP.get(0).getLong("id")
            recDP.setValues(stDP.get(0))
        } else {
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
            if (stProp.get(0).getBoolean("dependperiod")) {
                recDP.set("periodType", periodType)
                //recDP.set("periodType", FD_PeriodType_consts.year)
            }
            idDP = mdb.insertRec("DataProp", recDP, true)
        }
        StoreRecord recDPV = mdb.createStoreRecord("DataPropVal")
        recDPV.set("dataProp", idDP)
        double v = val / koef
        if (digit) v = v.round(digit)
        recDPV.set("numberVal", v)
        if (recDP.getLong("periodType") > 0) {
            if (dte.isEmpty())
                dte = XDateTime.create(new Date()).toString(XDateTimeFormatter.ISO_DATE)
            UtPeriod utPeriod = new UtPeriod()
            XDate d1 = utPeriod.calcDbeg(UtCnv.toDate(dte), recDP.getLong("periodType"), 0)
            XDate d2 = utPeriod.calcDend(UtCnv.toDate(dte), recDP.getLong("periodType"), 0)
            recDPV.set("dbeg", d1.toString(XDateTimeFormatter.ISO_DATE))
            recDPV.set("dend", d2.toString(XDateTimeFormatter.ISO_DATE))
        } else {
            recDPV.set("dbeg", "1800-01-01")
            recDPV.set("dend", "3333-12-31")
        }

        recDPV.set("authUser", 1L)
        recDPV.set("inputType", FD_InputType_consts.app)
        long idDPV = mdb.getNextId("DataPropVal")
        recDPV.set("id", idDPV)
        recDPV.set("ord", idDPV)
        recDPV.set("timeStamp", XDateTime.create(new Date()).toString(XDateTimeFormatter.ISO_DATE_TIME))
        mdb.insertRec("DataPropVal", recDPV, false)

    }

    def props_atrib = [
            "Prop_StartDate": 1044L,
    ]

    def props_meter = [
            "Prop_AreaOfTon": 1023L, "Prop_1057": 1057L, "Prop_1097": 1097L,
            "Prop_1137"     : 1137L, "Prop_1177": 1177L, "Prop_1257": 1257L,
            "Prop_1217"     : 1217L, "Prop_1297": 1297L, "Prop_1337": 1337L,
            "Prop_1377"     : 1377L, "Prop_1417": 1417L, "Prop_1457": 1457L, "Prop_1497": 1497L,
            "Prop_1537"     : 1537L, "Prop_1577": 1577L, "Prop_1617": 1617L,
            "Prop_1657"     : 1657L, "Prop_1697": 1697L, "Prop_1737": 1737L
    ]

    def props_obj = [
            "Prop_FishLocation"    : 1045,
            "Prop_FishGear"        : 1046,
            "Prop_FishManager"     : 1047L,
            "Prop_FishParticipants": 1048L
    ]

    @DaoMethod
    Store loadLog() {
        return mdb.loadQuery("""
            select * from log
        """)
    }

    @DaoMethod
    void fillFishing(File file, boolean fill, int num) {
        if (num == 1)
            fillFishing_1(file, fill)
        else if (num == 2)
            fillFishing_2(file, fill)
    }

    @DaoMethod
    void fillFishing_1(File file, boolean fill) {
        Store st = mdb.createStore()
        Domain d = mdb.createDomain(st)
        XLSXReader_withoutDescription reader = new XLSXReader_withoutDescription(mdb, file, d, st)
        List<String> fields = reader.getFields()
        boolean errTest = false


        StoreIndex indexLocationAndGear, indexManagerAndParticipants
        //*******************************************************
        // Анализ свойств
        //******************************************************
        def eachProps = { def own, m ->

            /* Obj */
            for (def k : props_obj.keySet()) {
                if (!m.get(k)) continue
                Map<String, Object> pms = new HashMap<>()
                pms.put("own", own)
                pms.put(k, props_obj.get(k))
                pms.put("obj" + k.split("_")[1], m.get(k))
                if (k == "Prop_FishLocation" || k == "Prop_FishGear") {
                    StoreRecord rec = indexLocationAndGear.get(UtCnv.toLong(m.get(k)))
                    if (rec != null) {
                        pms.put("pv" + k.split("_")[1], rec.getLong("pv"))
                        DataDao dao = mdb.createDao(DataDao.class)
                        dao.fillProperties(true, k, pms)
                    }
                }
                if (k == "Prop_FishManager") {
                    StoreRecord rec = indexManagerAndParticipants.get(UtCnv.toLong(m.get(k)))
                    if (rec != null) {
                        pms.put("pv" + k.split("_")[1], rec.getLong("pv"))
                        DataDao dao = mdb.createDao(DataDao.class)
                        dao.fillProperties(true, k, pms)
                    }
                }
                if (k == "Prop_FishParticipants") {
                    for (def it in m.get("Prop_FishParticipants").toString().split(";")) {
                        StoreRecord rec = indexManagerAndParticipants.get(UtCnv.toLong(it))
                        if (rec != null) {
                            pms.put("pv" + k.split("_")[1], rec.getLong("pv"))
                            DataDao dao = mdb.createDao(DataDao.class)
                            dao.fillProperties(true, k, pms)
                        }
                    }
                }

            }

            /* Attrib */
            for (def k : props_atrib.keySet()) {
                if (!m.get(k)) continue
                Map<String, Object> pms = new HashMap<>()
                pms.put("own", own)
                pms.put(k, props_atrib.get(k))
                pms.put(k.split("_")[1], m.get(k))
                if (UtCnv.toString(m.get(k)).trim() != "") {
                    DataDao dao = mdb.createDao(DataDao.class)
                    dao.fillProperties(true, k, pms)
                }
            }

            /* Meter */
            for (def k : props_meter.keySet()) {
                if (!m.get(k)) continue
                long prop = UtCnv.toLong(props_meter.get(k))
                double val = UtCnv.toDouble(m.get(k))
                //
                saveMeter(UtCnv.toLong(own), true, prop, val, 0, "")
            }

        }

        //*******************************************************
        // Функция обработки строк файла
        //******************************************************
        def eachLine = { def m ->
            long idObj = 0L
            def cls = UtCnv.toLong(m.get("cls"))
            def objLocation = UtCnv.toLong(m.get("Prop_FishLocation"))
            def StartDate = UtCnv.toString(m.get("Prop_StartDate")).trim()
            Store stTmp = apiMeta().get(ApiMeta).loadSql("""
                select name from ClsVer where ownerVer=${cls} and lastVer=1
            """, "")
            String nm = stTmp.get(0).getString("name")
            stTmp = mdb.loadQuery("""
                select name from ObjVer where ownerVer=${objLocation} and lastVer=1
            """)
            nm = nm + "_" + StartDate + "_" + stTmp.get(0).getString("name")
            //

            if (cls > 0 && !nm.isEmpty()) {

                try {
                    Map<String, Object> params = new HashMap<>()
                    params.put("name", nm)
                    params.put("fullName", nm)
                    params.put("cls", cls)
                    params.put("isObj", 1)
                    params.put("tableName", "Obj")
                    params.put("mode", "ins")
                    idObj = apiMonitoringData().get(ApiMonitoringData).createOwner(params)
                } catch (Exception e) {
                    println("Ошибка при создании Obj (cls, name) = ${cls}, ${nm}")
                    e.printStackTrace()
                }
                if (idObj > 0)
                    eachProps(idObj, m)
            }
        }

        //*******************************************************
        // Функция обработки строк файла
        //******************************************************

        Set<Long> idsFishLocation = new HashSet<>()
        Set<Long> idsFishGear = new HashSet<>()
        Set<Long> idsFishManager = new HashSet<>()
        Set<Long> idsFishParticipants = new HashSet<>()

        def eachLineCalc = { Map m ->
            idsFishLocation.add(UtCnv.toLong(m.get("Prop_FishLocation")))
            idsFishGear.add(UtCnv.toLong(m.get("Prop_FishGear")))
            idsFishManager.add(UtCnv.toLong(m.get("Prop_FishManager")))
            for (def it in m.get("Prop_FishParticipants").toString().split(";")) {
                idsFishParticipants.add(UtCnv.toLong(it))
            }
        }
        //
        Set<String> reqFields = new HashSet<>()
        Set<String> emptyFields = new HashSet<>()
        def count = 0
        def countVal = 0
        if (!fields.contains("cls")) reqFields.add("cls")
        if (!fields.contains("Prop_StartDate")) reqFields.add("Prop_StartDate")
        if (!fields.contains("Prop_AreaOfTon")) reqFields.add("Prop_AreaOfTon")
        if (!fields.contains("Prop_FishLocation")) reqFields.add("Prop_FishLocation")
        if (!fields.contains("Prop_FishGear")) reqFields.add("Prop_FishGear")
        if (!fields.contains("Prop_FishManager")) reqFields.add("Prop_FishManager")
        if (!fields.contains("Prop_FishParticipants")) reqFields.add("Prop_FishParticipants")

        if (fields.size() != 25) {
            errTest = true
        }
        if (!fields.containsAll(["cls", "Prop_StartDate", "Prop_FishLocation", "Prop_FishGear", "Prop_FishManager",
                                 "Prop_FishParticipants", "Prop_AreaOfTon", "Prop_1057", "Prop_1097", "Prop_1137", "Prop_1177", "Prop_1257",
                                 "Prop_1217", "Prop_1297", "Prop_1337", "Prop_1377", "Prop_1417", "Prop_1457", "Prop_1497", "Prop_1537",
                                 "Prop_1577", "Prop_1617", "Prop_1657", "Prop_1697", "Prop_1737"])) {
            errTest = true
        }


        def eachLineTest = { Map m ->
            count++
            if (!m.get("Prop_StartDate"))
                emptyFields.add("Prop_StartDate: Строка-${count + 1}")
            if (!m.get("Prop_AreaOfTon"))
                emptyFields.add("Prop_AreaOfTon: Строка-${count + 1}")
            if (!m.get("Prop_FishParticipants"))
                emptyFields.add("Prop_FishParticipants: Строка-${count + 1}")
            if (!isInteger(UtCnv.toString(m.get("cls"))))
                emptyFields.add("cls: Строка-${count + 1}")
            if (!isInteger(UtCnv.toString(m.get("Prop_FishLocation"))))
                emptyFields.add("Prop_FishLocation: Строка-${count + 1}")
            if (!isInteger(UtCnv.toString(m.get("Prop_FishGear"))))
                emptyFields.add("Prop_FishGear: Строка-${count + 1}")
            if (!isInteger(UtCnv.toString(m.get("Prop_FishManager"))))
                emptyFields.add("Prop_FishManager: Строка-${count + 1}")

            countVal += m.size()
        }

        //*******************************************************
        // Основное тело алгоритма
        //*******************************************************

        if (fill) {
            reader.eachRow(eachLineCalc)
            //
            idsFishLocation.addAll(idsFishGear)
            Store stReg = mdb.loadQuery("""
                select id, cls, 0 as pv from Obj where id in (${idsFishLocation.join(",")})
            """, "")
            Store stPV = apiMeta().get(ApiMeta).loadSql("""
                select id, cls from PropVal where cls is not null
            """, "")
            StoreIndex indPV = stPV.getIndex("cls")

            for (StoreRecord r in stReg) {
                StoreRecord rec = indPV.get(r.getLong("cls"))
                if (rec != null)
                    r.set("pv", rec.getLong("id"))
                else {
                    String msg = "Не указан возможное значение класса [" + r.getString("cls") + "]"
                    mdb.execQuery("""
                        update log set err=1, msg='${msg}', count=${count}, countval=${countVal} where id=1
                    """)
                    throw new XError(msg)
                }
            }
            indexLocationAndGear = stReg.getIndex("id")
            //
            idsFishManager.addAll(idsFishParticipants)
            stReg = apiPersonnelData().get(ApiPersonnelData).loadSql("""
                select id, cls, 0 as pv from Obj where id in (${idsFishManager.join(",")})
            """, "")

            for (StoreRecord r in stReg) {
                StoreRecord rec = indPV.get(r.getLong("cls"))
                if (rec != null)
                    r.set("pv", rec.getLong("id"))
                else {
                    String msg = "Не указан возможное значение класса [" + r.getString("cls") + "]"
                    mdb.execQuery("""
                        update log set err=1, msg='${msg}', count=${count}, countval=${countVal} where id=1
                    """)
                    throw new XError(msg)
                }
            }
            indexManagerAndParticipants = stReg.getIndex("id")
            //
            reader.eachRow(eachLine)
        } else {
            try {
                mdb.execQueryNative("""
                    CREATE TABLE IF NOT EXISTS log (
                        id int8 NOT NULL,
                        msg varchar(800) NULL,
                        count int8 NULL,
                        countval int8 NULL,
                        err int2 NULL,
                        CONSTRAINT pk_log PRIMARY KEY (id)
                    );
                    ALTER TABLE log OWNER TO pg;
                    GRANT ALL ON TABLE log TO pg;
                """)
                Store stLog = mdb.loadQuery("select * from log")
                if (stLog.size() == 0) {
                    mdb.execQueryNative("""
                        INSERT INTO log (id, msg, count, countval, err) VALUES (1, '', 0, 0, 0);
                    """)
                } else {
                    mdb.execQueryNative("""
                        UPDATE log SET msg='', count=0, countval=0, err=0 WHERE id=1;
                    """)
                }
            } catch (Exception e) {
                e.printStackTrace()
            }
            //
            reader.eachRow(eachLineTest)
            countVal = countVal - count
            //
            String msg
            def err = 0
            if (!reqFields.isEmpty()) {
                err = 1
                msg = "Наименования полей отсутствуют: [${reqFields.join('; ')}]"
            } else if (!emptyFields.isEmpty()) {
                err = 1
                msg = "Некоторые значения обязательных полей отсутствуют: [${emptyFields.join('; ')}]"
            } else if (errTest) {
                err = 1
                msg = "Формат шаблона не верный"
            } else {
                err = 0
                msg = ""
            }
            mdb.execQuery("""
                update log set err='${err}', msg='${msg}', count=${count}, countval=${countVal} where id=1
            """)
        }
    }

    static boolean isValidDate(String dateStr) {
        try {
            LocalDate.parse(dateStr, DateTimeFormatter.ofPattern('yyyy-MM-dd'))
            return true
        } catch (Exception e) {
            return false
        }
    }

    @DaoMethod
    void fillFishing_2(File file, boolean fill) {
        Store st = mdb.createStore()
        Domain d = mdb.createDomain(st)
        XLSXReader_withoutDescription reader = new XLSXReader_withoutDescription(mdb, file, d, st)
        List<String> fields = reader.getFields()
        boolean errTest = false
        def count = 0
        def countVal = 0
        //

        def eachLine = { Map m ->
            def owner = UtCnv.toLong(m.get("owner"))
            def isObj = UtCnv.toInt(m.get("isObj")) == 1
            def prop = UtCnv.toLong(m.get("prop"))
            def periodType = UtCnv.toLong(m.get("periodType"))
            def dte = UtCnv.toString(m.get("dte"))
            def val = UtCnv.toDouble(m.get("value"))
            saveMeter(owner, isObj, prop, val, periodType, dte)
        }


        Set<Long> idsProp = new HashSet<>()

        def eachLineCalc = { Map m ->
            idsProp.add(UtCnv.toLong(m.get("prop")))
        }
        //
        Set<String> reqFields = new HashSet<>()
        Set<String> emptyFields = new HashSet<>()

        if (!fields.contains("owner")) reqFields.add("owner")
        if (!fields.contains("isObj")) reqFields.add("isObj")
        if (!fields.contains("prop")) reqFields.add("prop")
        if (!fields.contains("periodType")) reqFields.add("periodType")
        if (!fields.contains("dte")) reqFields.add("dte")
        if (!fields.contains("value")) reqFields.add("value")

        if (fields.size() != 6) {
            errTest = true
        }
        if (!fields.containsAll(["owner", "isObj", "prop", "periodType", "dte","value"])) {
            errTest = true
        }


        def eachLineTest = { Map m ->
            count++
            if (!isInteger(UtCnv.toString(m.get("owner"))))
                emptyFields.add("owner: Строка-${count + 1}")
            if (!isInteger(UtCnv.toString(m.get("isObj"))))
                emptyFields.add("isObj: Строка-${count + 1}")
            if (!isInteger(UtCnv.toString(m.get("prop"))))
                emptyFields.add("prop: Строка-${count + 1}")
            if (!isInteger(UtCnv.toString(m.get("periodType"))))
                emptyFields.add("periodType: Строка-${count + 1}")
            else if (UtCnv.toLong(m.get("periodType")) > 0) {
                if (!isValidDate(UtCnv.toString(m.get("dte"))))
                    emptyFields.add("dte: Строка-${count + 1}")

            }
            if (!m.get("value"))
                emptyFields.add("value: Строка-${count + 1}")

            countVal = count
        }



        //*******************************************************
        // Основное тело алгоритма
        //*******************************************************

        if (fill) {
            reader.eachRow(eachLineCalc)
            //
            reader.eachRow(eachLine)
        } else {
            try {
                mdb.execQueryNative("""
                    CREATE TABLE IF NOT EXISTS log (
                        id int8 NOT NULL,
                        msg varchar(800) NULL,
                        count int8 NULL,
                        countval int8 NULL,
                        err int2 NULL,
                        CONSTRAINT pk_log PRIMARY KEY (id)
                    );
                    ALTER TABLE log OWNER TO pg;
                    GRANT ALL ON TABLE log TO pg;
                """)
                Store stLog = mdb.loadQuery("select * from log")
                if (stLog.size() == 0) {
                    mdb.execQueryNative("""
                        INSERT INTO log (id, msg, count, countval, err) VALUES (1, '', 0, 0, 0);
                    """)
                } else {
                    mdb.execQueryNative("""
                        UPDATE log SET msg='', count=0, countval=0, err=0 WHERE id=1;
                    """)
                }
            } catch (Exception e) {
                e.printStackTrace()
            }
            //
            reader.eachRow(eachLineTest)
            //
            String msg
            def err = 0
            if (!reqFields.isEmpty()) {
                err = 1
                msg = "Наименования полей отсутствуют: [${reqFields.join('; ')}]"
            } else if (!emptyFields.isEmpty()) {
                err = 1
                msg = "Некоторые значения обязательных полей отсутствуют: [${emptyFields.join('; ')}]"
            } else if (errTest) {
                err = 1
                msg = "Формат шаблона не верный"
            } else {
                err = 0
                msg = ""
            }
            mdb.execQuery("""
                update log set err='${err}', msg='${msg}', count=${count}, countval=${countVal} where id=1
            """)
        }

    }

}
