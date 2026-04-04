package tofi.mdl.model.dao.flat.table;

import jandcode.commons.UtCnv;
import jandcode.core.dao.DaoMethod;
import jandcode.core.dbm.mdb.BaseMdbUtils;
import jandcode.core.store.Store;
import tofi.mdl.consts.FD_StorageType_consts;
import tofi.mdl.model.utils.EntityMdbUtils;

import java.util.Map;


public class FlatTableMdbUtils extends BaseMdbUtils {

    @DaoMethod
    public Store loadTables(Map<String, Object> params) throws Exception {
        Store st = getMdb().createStore("FlatTable");
        getMdb().loadQuery(st, "select * from FlatTable where 0=0");
        //getMdb().outTable(st);
        return st;
    }

    @DaoMethod
    public Store load() throws Exception {
        Store st = getMdb().createStore("FlatTable.full");
        getMdb().loadQuery(st, """
                    select f.*, case when f.cls is null then rv.name else cv.name end as nameCls,
                        case when f.cls is null then r.dataBase else c.dataBase end as db,
                        case when f.cls is null then dr.name else dc.name end as nameDb
                    from FlatTable f
                        left join Cls c on f.cls=c.id
                        left join ClsVer cv on c.id=cv.ownerVer and cv.lastVer=1
                        left join DataBase dc on dc.id=c.dataBase
                        left join RelCls r on f.relCls=r.id
                        left join RelClsVer rv on r.id=rv.ownerVer and rv.lastVer=1
                        left join DataBase dr on dr.id=r.dataBase
                    where 0=0
                """);
        return st;
    }

    @DaoMethod
    public Store loadRec(long id) throws Exception {
        Store st = getMdb().createStore("FlatTable.full");
        getMdb().loadQuery(st, """
                    select f.*, case when f.cls is null then rv.name else cv.name end as nameCls,
                        case when f.cls is null then r.dataBase else c.dataBase end as db,
                        case when f.cls is null then dr.name else dc.name end as nameDb
                    from FlatTable f
                        left join Cls c on f.cls=c.id
                        left join ClsVer cv on c.id=cv.ownerVer and cv.lastVer=1
                        left join DataBase dc on dc.id=c.dataBase
                        left join RelCls r on f.relCls=r.id
                        left join RelClsVer rv on r.id=rv.ownerVer and rv.lastVer=1
                        left join DataBase dr on dr.id=r.dataBase
                    where f.id=:id
                """, Map.of("id", id));
        return st;
    }

    @DaoMethod
    public Store insertFlatTable(Map<String, Object> rec) throws Exception {
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), "FlatTable");
        long id = eu.insertEntity(rec);
        //
        return loadRec(id);
    }

    @DaoMethod
    public Store updateFlatTable(Map<String, Object> rec) throws Exception {
        EntityMdbUtils eu = new EntityMdbUtils(getMdb(), "FlatTable");
        eu.updateEntity(rec);
        //getMdb().updateRec("FlatTable", rec);
        //
        Store st = getMdb().createStore("FlatTable");
        return loadRec(UtCnv.toLong(rec.get("id")));

    }

    @DaoMethod
    public void deleteFlatTable(Map<String, Object> rec) throws Exception {
        String tn = UtCnv.toString(rec.get("nameTable"));
        long id = UtCnv.toLong(rec.get("id"));

        try {
            getMdb().execQuery("""
                        update TypCharGrProp set storageType=:sd, flatTable=null where flatTable=:ft;
                        update RelTypCharGrProp set storageType=:sd, flatTable=null where flatTable=:ft;
                    """, Map.of("sd", FD_StorageType_consts.std, "ft", id));
            EntityMdbUtils eu = new EntityMdbUtils(getMdb(), "FlatTable");
            eu.deleteEntity(rec);
        } finally {
            //todo Можно убрать, они в другой базе
            getMdb().execQuery("drop table if exists " + tn + " cascade");
            getMdb().execQuery("drop table if exists " + tn + "_notuniqprop cascade");
        }
    }


}
