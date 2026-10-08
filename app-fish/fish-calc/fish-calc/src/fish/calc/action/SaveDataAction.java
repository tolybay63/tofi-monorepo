package fish.calc.action;

import fish.calc.dao.DataDao;
import jandcode.commons.UtCnv;
import jandcode.commons.UtJson;
import jandcode.commons.error.XError;
import jandcode.commons.variant.IVariantMap;
import jandcode.commons.variant.VariantMapNoCase;
import jandcode.core.dbm.ModelService;
import jandcode.core.dbm.mdb.Mdb;
import jandcode.core.web.action.BaseAction;

import java.io.File;
import java.util.Map;

public class SaveDataAction extends BaseAction {

    protected void onExec() throws Exception {
        //Извлекаем параметры метаданных
        IVariantMap params = getReq().getParams();

        String jsonStr = params.getString("params");
        Map innerMap = UtJson.getGson().fromJson(jsonStr, Map.class);
        String fnOrg = innerMap != null ? String.valueOf(innerMap.get("filename")) : "";


        javax.servlet.http.Part filePart = getReq().getPart("file");

        if (filePart == null) {
            throw new XError("File not found in request payload");
        }

        //Создаем временный файл, привязанный строго к этому потоку выполнения
        File fle = File.createTempFile("upload_", ".tmp");
        try (java.io.InputStream input = filePart.getInputStream()) {
            java.nio.file.Files.copy(input, fle.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        ModelService modelSvc = getApp().bean(ModelService.class);
        Mdb mdb = modelSvc.getModel().createMdb();
        DataDao dao = mdb.createDao(DataDao.class);

        dao.saveDataFromCalcFastApi(fle, fnOrg);

        getReq().render("filename: " + fnOrg);

    }

}
