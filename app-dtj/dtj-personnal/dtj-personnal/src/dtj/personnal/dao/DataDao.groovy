package dtj.personnal.dao

import groovy.transform.CompileStatic
import jandcode.commons.UtCnv
import jandcode.commons.datetime.XDate
import jandcode.commons.datetime.XDateTime
import jandcode.commons.datetime.XDateTimeFormatter
import jandcode.commons.error.XError
import jandcode.commons.variant.VariantMap
import jandcode.core.auth.AuthService
import jandcode.core.dao.DaoMethod
import jandcode.core.dbm.mdb.BaseMdbUtils
import jandcode.core.store.Store
import jandcode.core.store.StoreIndex
import jandcode.core.store.StoreRecord
import tofi.api.adm.ApiAdm
import tofi.api.dta.ApiNSIData
import tofi.api.dta.ApiObjectData
import tofi.api.dta.ApiOrgStructureData
import tofi.api.dta.ApiPersonnalData
import tofi.api.dta.ApiPlanData
import tofi.api.dta.ApiUserData
import tofi.api.dta.model.utils.EntityMdbUtils
import tofi.api.dta.model.utils.UtPeriod
import tofi.api.mdl.ApiMeta
import tofi.api.mdl.model.consts.FD_AttribValType_consts
import tofi.api.mdl.model.consts.FD_InputType_consts
import tofi.api.mdl.model.consts.FD_PeriodType_consts
import tofi.api.mdl.model.consts.FD_PropType_consts
import tofi.apinator.ApinatorApi
import tofi.apinator.ApinatorService


@CompileStatic
class DataDao extends BaseMdbUtils {

    ApinatorApi apiAdm() {
        return app.bean(ApinatorService).getApi("adm")
    }

    ApinatorApi apiMeta() {
        return app.bean(ApinatorService).getApi("meta")
    }

    ApinatorApi apiUserData() {
        return app.bean(ApinatorService).getApi("userdata")
    }

    ApinatorApi apiNSIData() {
        return app.bean(ApinatorService).getApi("nsidata")
    }

    ApinatorApi apiObjectData() {
        return app.bean(ApinatorService).getApi("objectdata")
    }

    ApinatorApi apiPlanData() {
        return app.bean(ApinatorService).getApi("plandata")
    }

    ApinatorApi apiPersonnalData() {
        return app.bean(ApinatorService).getApi("personnaldata")
    }

    ApinatorApi apiOrgStructureData() {
        return app.bean(ApinatorService).getApi("orgstructuredata")
    }

    /* =================================================================== */

    @DaoMethod
    Store loadPersonnal(long id) {
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", "Cls_Personnel", "")
        if (map.isEmpty())
            throw new XError("Not found [Cls_Personnel]")

        String whe = "o.id=${id}"
        if (id == 0)
            whe = "o.cls=(${map.get("Cls_Personnel")})"

        map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Prop", "", "Prop_%")
        Store st = mdb.createStore("Obj.Personnal")
        mdb.loadQuery(st, """
            select o.id, o.cls, v.name, v.fullName,
                v1.id as idTabNumber, v1.strVal as TabNumber,
                v2.id as idUserSecondName, v2.strVal as UserSecondName,
                v4.id as idUserFirstName, v4.strVal as UserFirstName,
                v5.id as idUserMiddleName, v5.strVal as UserMiddleName,
                v6.id as idUserEmail, v6.strVal as UserEmail,
                v7.id as idUserPhone, v7.strVal as UserPhone,
                v8.id as idUserDateBirth, v8.dateTimeVal as UserDateBirth,
                v9.id as idDateEmployment, v9.dateTimeVal as DateEmployment,
                v10.id as idDateDateDismissal, v10.dateTimeVal as DateDismissal,
                v11.id as idCreatedAt, v11.dateTimeVal as CreatedAt,
                v12.id as idUpdatedAt, v12.dateTimeVal as UpdatedAt,
                v13.id as idUserSex, v13.propVal as pvUserSex, null as fvUserSex,
                v14.id as idPosition, v14.propVal as pvPosition, null as fvPosition,
                v15.id as idLocation, v15.obj as objLocation, v15.propVal as pvLocation, ov15.name as nameLocation
            from Obj o 
                left join ObjVer v on o.id=v.ownerver and v.lastver=1
                left join DataProp d1 on d1.objorrelobj=o.id and d1.prop=:Prop_TabNumber
                left join DataPropVal v1 on d1.id=v1.dataprop
                left join DataProp d2 on d2.objorrelobj=o.id and d2.prop=:Prop_UserSecondName
                left join DataPropVal v2 on d2.id=v2.dataprop
                left join DataProp d4 on d4.objorrelobj=o.id and d4.prop=:Prop_UserFirstName
                left join DataPropVal v4 on d4.id=v4.dataprop
                left join DataProp d5 on d5.objorrelobj=o.id and d5.prop=:Prop_UserMiddleName
                left join DataPropVal v5 on d5.id=v5.dataprop
                left join DataProp d6 on d6.objorrelobj=o.id and d6.prop=:Prop_UserEmail
                left join DataPropVal v6 on d6.id=v6.dataprop
                left join DataProp d7 on d7.objorrelobj=o.id and d7.prop=:Prop_UserPhone
                left join DataPropVal v7 on d7.id=v7.dataprop
                left join DataProp d8 on d8.objorrelobj=o.id and d8.prop=:Prop_UserDateBirth
                left join DataPropVal v8 on d8.id=v8.dataprop
                left join DataProp d9 on d9.objorrelobj=o.id and d9.prop=:Prop_DateEmployment
                left join DataPropVal v9 on d9.id=v9.dataprop
                left join DataProp d10 on d10.objorrelobj=o.id and d10.prop=:Prop_DateDismissal
                left join DataPropVal v10 on d10.id=v10.dataprop
                left join DataProp d11 on d11.objorrelobj=o.id and d11.prop=:Prop_CreatedAt
                left join DataPropVal v11 on d11.id=v11.dataprop
                left join DataProp d12 on d12.objorrelobj=o.id and d12.prop=:Prop_UpdatedAt
                left join DataPropVal v12 on d12.id=v12.dataprop
                left join DataProp d13 on d13.objorrelobj=o.id and d13.prop=:Prop_UserSex
                left join DataPropVal v13 on d13.id=v13.dataprop
                left join DataProp d14 on d14.objorrelobj=o.id and d14.prop=:Prop_Position
                left join DataPropVal v14 on d14.id=v14.dataprop     
                left join DataProp d15 on d15.objorrelobj=o.id and d15.prop=:Prop_Location
                left join DataPropVal v15 on d15.id=v15.dataprop
                left join ObjVer ov15 on v15.obj=ov15.ownerVer and ov15.lastVer=1
            where ${whe}
        """, map)

        Map<Long, Long> mapPV = apiMeta().get(ApiMeta).mapEntityIdFromPV("factorVal", true)

        for (StoreRecord record in st) {
            record.set("fvUserSex", mapPV.get(record.getLong("pvUserSex")))
            record.set("fvPosition", mapPV.get(record.getLong("pvPosition")))
        }
        //
        return st
    }

    private static void validatePersonal(String mode, Map<String, Object> params) {
        if (mode=="ins") {
            if (!params.containsKey("login"))
                throw new XError("login not specified")
            if (!params.containsKey("passwd"))
                throw new XError("passwd not specified")
        }
        if (!params.containsKey("TabNumber"))
            throw new XError("TabNumber not specified")
        if (!params.containsKey("UserSecondName"))
            throw new XError("UserSecondName not specified")
        if (!params.containsKey("UserFirstName"))
            throw new XError("UserFirstName not specified")
        if (!params.containsKey("UserDateBirth"))
            throw new XError("UserDateBirth not specified")
        if (!params.containsKey("fvUserSex"))
            throw new XError("UserSex not specified")
        if (!params.containsKey("fvPosition"))
            throw new XError("Position not specified")
        if (!params.containsKey("objLocation"))
            throw new XError("Location not specified")
    }

    private long regUser(Map<String, Object> params) {
        Map<String, Object> rec = new HashMap<>()
        rec.put("login", params.get("login"))
        rec.put("passwd", params.get("passwd"))
        rec.put("accessLevel", 1)
        rec.put("email", params.get("UserEmail"))
        if (params.containsKey("UserPhone"))
            rec.put("phone", params.get("UserPhone"))
        rec.put("name", params.get("UserFirstName"))
        String fn = params.get("UserSecondName").toString() + " " + params.get("UserFirstName").toString()
        rec.put("fullName", fn)
        return apiAdm().get(ApiAdm).regUser(rec)
    }

    private void deleteAuthUser(long id) {
        apiAdm().get(ApiAdm).deleteAuthUser(id)
    }

    @DaoMethod
    Store savePersonnal(String mode, Map<String, Object> params) {
        Map<String, Long> map = apiMeta().get(ApiMeta).getIdFromCodOfEntity("Cls", "Cls_Personnel", "")
        if (map.isEmpty())
            throw new XError("Not found [Cls_Personnel]")

        validatePersonal(mode, params)

        long own = 0
        EntityMdbUtils eu = new EntityMdbUtils(mdb, "Obj")

        if (mode.equalsIgnoreCase("ins")) {
            //
            long userId = regUser(params)
            //
            try {
                Map<String, Object> par = new HashMap<>(params)
                par.put("cls", map.get("Cls_Personnel"))
                par.put("name", par.get("UserFirstName"))
                String fn = par.get("UserSecondName").toString() + " " + par.get("UserFirstName").toString()
                par.put("fullName", fn)
                own = eu.insertEntity(par)
                params.put("own", own)
                //1 Prop_TabNumber
                if (params.containsKey("TabNumber"))
                    fillProperties(true, "Prop_TabNumber", params)
                //2 Prop_UserSecondName
                if (params.containsKey("UserSecondName"))
                    fillProperties(true, "Prop_UserSecondName", params)
                //3 Prop_UserFirstName
                if (params.containsKey("UserFirstName"))
                    fillProperties(true, "Prop_UserFirstName", params)
                //4 Prop_UserMiddleName
                if (params.containsKey("UserMiddleName"))
                    fillProperties(true, "Prop_UserMiddleName", params)
                //5 Prop_UserDateBirth
                if (params.containsKey("UserDateBirth"))
                    fillProperties(true, "Prop_UserDateBirth", params)
                //6 Prop_UserEmail
                if (params.containsKey("UserEmail"))
                    fillProperties(true, "Prop_UserEmail", params)
                //7 Prop_UserPhone
                if (params.containsKey("UserPhone"))
                    fillProperties(true, "Prop_UserPhone", params)
                //8 Prop_DateEmployment
                if (params.containsKey("DateEmployment"))
                    fillProperties(true, "Prop_DateEmployment", params)
                //9 Prop_DateDismissal
                if (params.containsKey("DateDismissal"))
                    fillProperties(true, "Prop_DateDismissal", params)
                //10 Prop_CreatedAt
                if (params.containsKey("CreatedAt"))
                    fillProperties(true, "Prop_CreatedAt", params)
                //11 Prop_UpdatedAt
                if (params.containsKey("UpdatedAt"))
                    fillProperties(true, "Prop_UpdatedAt", params)

                //12 Prop_UserId
                params.put("UserId", userId)
                fillProperties(true, "Prop_UserId", params)

                //13 Prop_UserSex
                if (params.containsKey("fvUserSex"))
                    fillProperties(true, "Prop_UserSex", params)
                //14 Prop_Position
                if (params.containsKey("fvPosition"))
                    fillProperties(true, "Prop_Position", params)

                //15 Prop_Location
                if (params.containsKey("objLocation"))
                    fillProperties(true, "Prop_Location", params)
            } catch (Exception e) {
                e.printStackTrace()
                if (userId > 0)
                    deleteAuthUser(userId)
            }

        } else if (mode.equalsIgnoreCase("upd")) {
            own = UtCnv.toLong(params.get("id"))
            eu.updateEntity(params)
            //
            params.put("own", own)
            //1 Prop_TabNumber
            if (params.containsKey("idTabNumber"))
                updateProperties("Prop_TabNumber", params)
            else {
                if (!params.get("TabNumber").toString().isEmpty())
                    fillProperties(true, "Prop_TabNumber", params)
            }

            //2 Prop_UserSecondName
            if (params.containsKey("idUserSecondName"))
                updateProperties("Prop_UserSecondName", params)
            else {
                if (!params.get("UserSecondName").toString().isEmpty())
                    fillProperties(true, "Prop_UserSecondName", params)
            }
            //3 Prop_UserFirstName
            if (params.containsKey("idUserFirstName"))
                updateProperties("Prop_UserFirstName", params)
            else {
                if (!params.get("UserFirstName").toString().isEmpty())
                    fillProperties(true, "Prop_UserFirstName", params)
            }
            //4 Prop_UserMiddleName
            if (params.containsKey("idUserMiddleName"))
                updateProperties("Prop_UserMiddleName", params)
            else {
                if (!params.get("UserMiddleName").toString().isEmpty())
                    fillProperties(true, "Prop_UserMiddleName", params)
            }
            //5 Prop_UserDateBirth
            if (params.containsKey("idUserDateBirth"))
                updateProperties("Prop_UserDateBirth", params)
            else {
                if (!params.get("UserDateBirth").toString().isEmpty())
                    fillProperties(true, "Prop_UserDateBirth", params)
            }
            //6 Prop_UserEmail
            if (params.containsKey("idUserEmail"))
                updateProperties("Prop_UserEmail", params)
            else {
                if (!params.get("UserEmail").toString().isEmpty())
                    fillProperties(true, "Prop_UserEmail", params)
            }
            //7 Prop_UserPhone
            if (params.containsKey("idUserPhone"))
                updateProperties("Prop_UserPhone", params)
            else {
                if (!params.get("UserPhone").toString().isEmpty())
                    fillProperties(true, "Prop_UserPhone", params)
            }
            //8 Prop_DateEmployment
            if (params.containsKey("idDateEmployment"))
                updateProperties("Prop_DateEmployment", params)
            else {
                if (!params.get("DateEmployment").toString().isEmpty())
                    fillProperties(true, "Prop_DateEmployment", params)
            }
            //9 Prop_DateDismissal
            if (params.containsKey("idDateDismissal"))
                updateProperties("Prop_DateDismissal", params)
            else {
                if (!params.get("DateDismissal").toString().isEmpty())
                    fillProperties(true, "Prop_DateDismissal", params)
            }
            //10 Prop_CreatedAt
            if (params.containsKey("idCreatedAt"))
                updateProperties("Prop_CreatedAt", params)
            else {
                if (!params.get("CreatedAt").toString().isEmpty())
                    fillProperties(true, "Prop_CreatedAt", params)
            }
            //11 Prop_UpdatedAt
            if (params.containsKey("idUpdatedAt"))
                updateProperties("Prop_UpdatedAt", params)
            else {
                if (!params.get("UpdatedAt").toString().isEmpty())
                    fillProperties(true, "Prop_UpdatedAt", params)
            }

            //12 Prop_UserId

            //13 Prop_UserSex
            if (params.containsKey("idUserSex"))
                updateProperties("Prop_UserSex", params)
            else {
                if (params.containsKey("fvUserSex"))
                    fillProperties(true, "Prop_UserSex", params)
            }
            //14 Prop_Position
            if (params.containsKey("idPosition"))
                updateProperties( "Prop_Position", params)
            else {
                if (params.containsKey("fvPosition"))
                    fillProperties(true, "Prop_Position", params)
            }
            //15 Prop_Location
            if (params.containsKey("idLocation"))
                updateProperties("Prop_Location", params)
            else {
                if (params.containsKey("objLocation"))
                    fillProperties(true, "Prop_Location", params)
            }
        } else {
            throw new XError("Unknown mode")
        }
        loadPersonnal(own)
    }

    private void fillProperties(boolean isObj, String cod, Map<String, Object> params) {
        long own = UtCnv.toLong(params.get("own"))
        String keyValue = cod.split("_")[1]
        def objRef = UtCnv.toLong(params.get("obj"+keyValue))
        def propVal = UtCnv.toLong(params.get("pv"+keyValue))

        Store stProp = apiMeta().get(ApiMeta).getPropInfo(cod)
        //
        long prop = stProp.get(0).getLong("id")
        long propType = stProp.get(0).getLong("propType")
        long attribValType = stProp.get(0).getLong("attribValType")
        Integer digit = null
        double koef = stProp.get(0).getDouble("koef")
        if (koef == 0) koef = 1
        if (!stProp.get(0).isValueNull("digit"))
            digit = stProp.get(0).getInt("digit")

        //
        long idDP
        StoreRecord recDP = mdb.createStoreRecord("DataProp")
        String whe = isObj ? "and isObj=1 " : "and isObj=0 "
        if (stProp.get(0).getLong("statusFactor") > 0) {
            long fv = apiMeta().get(ApiMeta).getDefaultStatus(prop)
            whe += "and status = ${fv} "
        } else {
            whe += "and status is null "
        }
        whe += "and provider is null "
        //todo if (stProp.get(0).getLong("providerTyp") > 0)

        if (stProp.get(0).getLong("providerTyp") > 0) {
            whe += "and periodType is not null "
        } else {
            whe += "and periodType is null"
        }
        Store stDP = mdb.loadQuery("""
            select * from DataProp
            where objOrRelObj=${own} and prop=${prop} ${whe}
        """)
        if (stDP.size() > 0) {
            idDP = stDP.get(0).getLong("id")
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
                recDP.set("periodType", FD_PeriodType_consts.year)
            }
            idDP = mdb.insertRec("DataProp", recDP, true)
        }
        //
        StoreRecord recDPV = mdb.createStoreRecord("DataPropVal")
        recDPV.set("dataProp", idDP)
        // Attrib
        if ([FD_AttribValType_consts.str].contains(attribValType)) {
            if ( cod.equalsIgnoreCase("Prop_TabNumber") ||
                    cod.equalsIgnoreCase("Prop_UserSecondName") ||
                    cod.equalsIgnoreCase("Prop_UserFirstName") ||
                    cod.equalsIgnoreCase("Prop_UserMiddleName") ||
                    cod.equalsIgnoreCase("Prop_UserEmail") ||
                    cod.equalsIgnoreCase("Prop_UserPhone") ||
                    cod.equalsIgnoreCase("Prop_UserId")) {
                if (params.get(keyValue) != null) {
                    recDPV.set("strVal", UtCnv.toString(params.get(keyValue)))
                }
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }
        //
        if ([FD_AttribValType_consts.multistr].contains(attribValType)) {
            if ( cod.equalsIgnoreCase("Prop_Description")) {
                if (params.get(keyValue) != null) {
                    recDPV.set("multiStrVal", UtCnv.toString(params.get(keyValue)))
                }
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }

        if ([FD_AttribValType_consts.dt].contains(attribValType)) {
            if (cod.equalsIgnoreCase("Prop_CreatedAt") ||
                    cod.equalsIgnoreCase("Prop_UpdatedAt") ||
                        cod.equalsIgnoreCase("Prop_DateEmployment") ||
                            cod.equalsIgnoreCase("Prop_DateDismissal") ||
                    cod.equalsIgnoreCase("Prop_UserDateBirth")) {
                if (params.get(keyValue) != null) {
                    recDPV.set("dateTimeVal", UtCnv.toString(params.get(keyValue)))
                }
            } else
                throw new XError("for dev: [${cod}] отсутствует в реализации")
        }

        // For FV
        if ([FD_PropType_consts.factor].contains(propType)) {
            if ( cod.equalsIgnoreCase("Prop_UserSex") ||
                    cod.equalsIgnoreCase("Prop_Position")) {
                if (propVal > 0) {
                    recDPV.set("propVal", propVal)
                }
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }

        // For Measure
        if ([FD_PropType_consts.measure].contains(propType)) {
            if ( cod.equalsIgnoreCase("Prop_ParamsMeasure")) {
                if (propVal > 0) {
                    recDPV.set("propVal", propVal)
                }
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }

        // For Meter
        if ([FD_PropType_consts.meter, FD_PropType_consts.rate].contains(propType)) {
            if (cod.equalsIgnoreCase("Prop_StartKm")) { // template
                if (params.get(keyValue) != null) {
                    double v = UtCnv.toDouble(params.get(keyValue))
                    v = v / koef
                    if (digit) v = v.round(digit)
                    recDPV.set("numberval", v)
                }
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }
        // For Typ
        if ([FD_PropType_consts.typ].contains(propType)) {
            if (cod.equalsIgnoreCase("Prop_Location")) {
                if (objRef > 0) {
                    recDPV.set("propVal", propVal)
                    recDPV.set("obj", objRef)
                }
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }
        //
        if (recDP.getLong("periodType") > 0) {
            if (!params.containsKey("dte"))
                params.put("dte", XDateTime.create(new Date()).toString(XDateTimeFormatter.ISO_DATE))
            UtPeriod utPeriod = new UtPeriod()
            XDate d1 = utPeriod.calcDbeg(UtCnv.toDate(params.get("dte")), recDP.getLong("periodType"), 0)
            XDate d2 = utPeriod.calcDend(UtCnv.toDate(params.get("dte")), recDP.getLong("periodType"), 0)
            recDPV.set("dbeg", d1.toString(XDateTimeFormatter.ISO_DATE))
            recDPV.set("dend", d2.toString(XDateTimeFormatter.ISO_DATE))
        } else {
            recDPV.set("dbeg", "1800-01-01")
            recDPV.set("dend", "3333-12-31")
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

    private void updateProperties(String cod, Map<String, Object> params) {
        VariantMap mapProp = new VariantMap(params)
        String keyValue = cod.split("_")[1]
        long idVal = mapProp.getLong("id" + keyValue)
        long propVal = mapProp.getLong("pv" + keyValue)
        long objRef = mapProp.getLong("obj" + keyValue)
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
        def strValue = mapProp.getString(keyValue)
        // For Attrib
        if ([FD_AttribValType_consts.str].contains(attribValType)) {
            if (cod.equalsIgnoreCase("Prop_TabNumber") ||
                    cod.equalsIgnoreCase("Prop_UserSecondName") ||
                    cod.equalsIgnoreCase("Prop_UserFirstName") ||
                    cod.equalsIgnoreCase("Prop_UserMiddleName") ||
                    cod.equalsIgnoreCase("Prop_UserEmail") ||
                    cod.equalsIgnoreCase("Prop_UserPhone") ||
                    cod.equalsIgnoreCase("Prop_UserId")) {
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

        if ([FD_AttribValType_consts.multistr].contains(attribValType)) {
            if ( cod.equalsIgnoreCase("Prop_Description")) {
                if (params.get(keyValue) != null) {
                    sql = "update DataPropval set multiStrVal='${strValue}', timeStamp='${tmst}' where id=${idVal}"
                }
            } else {
                throw new XError("for dev: [${cod}] отсутствует в реализации")
            }
        }

        if ([FD_AttribValType_consts.dt].contains(attribValType)) {
            if (cod.equalsIgnoreCase("Prop_CreatedAt") ||
                    cod.equalsIgnoreCase("Prop_UpdatedAt") ||
                    cod.equalsIgnoreCase("Prop_DateEmployment") ||
                    cod.equalsIgnoreCase("Prop_DateDismissal") ||
                    cod.equalsIgnoreCase("Prop_UserDateBirth")) {
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
                    sql = "update DataPropval set dateTimeVal='${strValue}', timeStamp='${tmst}' where id=${idVal}"
                }
            } else
                throw new XError("for dev: [${cod}] отсутствует в реализации")
        }

        // For FV
        if ([FD_PropType_consts.factor].contains(propType)) {
            if ( cod.equalsIgnoreCase("Prop_UserSex") ||
                    cod.equalsIgnoreCase("Prop_Position")) {
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

        // For Measure
        if ([FD_PropType_consts.measure].contains(propType)) {
            if ( cod.equalsIgnoreCase("Prop_ParamsMeasure") ) {
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
            if (cod.equalsIgnoreCase("Prop_StartKm")) {
                if (mapProp.keySet().contains(keyValue) && mapProp[keyValue] != 0) {
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
        // For Typ
        if ([FD_PropType_consts.typ].contains(propType)) {
            if (cod.equalsIgnoreCase("Prop_Location")) {
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

        mdb.execQueryNative(sql)
    }

    //-------------------------


    private Store loadSqlMeta(String sql, String domain) {
        return apiMeta().get(ApiMeta).loadSql(sql, domain)
    }

    private Store loadSqlService(String sql, String domain, String model) {
        if (model.equalsIgnoreCase("userdata"))
            return apiUserData().get(ApiUserData).loadSql(sql, domain)
        else if (model.equalsIgnoreCase("nsidata"))
            return apiNSIData().get(ApiNSIData).loadSql(sql, domain)
        else if (model.equalsIgnoreCase("objectdata"))
            return apiObjectData().get(ApiObjectData).loadSql(sql, domain)
        else if (model.equalsIgnoreCase("orgstructuredata"))
            return apiOrgStructureData().get(ApiOrgStructureData).loadSql(sql, domain)
        else if (model.equalsIgnoreCase("plandata"))
            return apiPlanData().get(ApiPlanData).loadSql(sql, domain)
        else if (model.equalsIgnoreCase("personnaldata"))
            return apiPersonnalData().get(ApiPersonnalData).loadSql(sql, domain)
        else
            throw new XError("Unknown model [${model}]")
    }

    private long getUser() throws Exception {
        AuthService authSvc = mdb.getApp().bean(AuthService.class)
        long au = authSvc.getCurrentUser().getAttrs().getLong("id")
        if (au == 0)
            au = 1//throw new XError("notLogined")
        return au
    }

}
