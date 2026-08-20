package fish.monitoring.action;

import fish.monitoring.dao.FillDao;
import jandcode.commons.error.XError;
import jandcode.commons.variant.IVariantMap;
import jandcode.core.dbm.ModelService;
import jandcode.core.dbm.mdb.Mdb;
import jandcode.core.web.action.BaseAction;

import java.io.File;

public class FillDataAction extends BaseAction {

    protected void onExec() throws Exception {
        //Извлекаем параметры метаданных
        IVariantMap params = getReq().getParams();
                //IVariantMap params = UtJson.fromJson(getReq().getParams().getString("params"), VariantMap.class);
        String fnOrg = params.getString("filename");
        boolean fill = params.getBoolean("fill");
        int num = params.getInt("num");

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
        FillDao dao = mdb.createDao(FillDao.class);
        dao.fillFishing(fle, fill, num);

        getReq().render("filename: " + fnOrg);

    }

}
