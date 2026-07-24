package fish.monitoring.action;

import fish.monitoring.dao.FillDao;
import jandcode.commons.UtJson;
import jandcode.commons.error.XError;
import jandcode.commons.variant.IVariantMap;
import jandcode.commons.variant.VariantMap;
import jandcode.core.dbm.ModelService;
import jandcode.core.dbm.mdb.Mdb;
import jandcode.core.web.action.BaseAction;

import java.io.File;

public class FillDataAction extends BaseAction {

    protected void onExec() throws Exception {

/*        String tempDir = UtCnv.toString(getReq().getHttpServlet().getServletContext().getAttribute("javax.servlet.context.tempdir"));
        if (tempDir==null) {
            throw new HttpError(404);
        }*/

        //Извлекаем параметры метаданных
        IVariantMap params = getReq().getParams();
                //IVariantMap params = UtJson.fromJson(getReq().getParams().getString("params"), VariantMap.class);
        String fnOrg = params.getString("filename");
        boolean fill = params.getBoolean("fill");

        javax.servlet.http.Part filePart = getReq().getPart("file");

        if (filePart == null) {
            throw new XError("File not found in request payload");
        }

        //Создаем временный файл, привязанный строго к этому потоку выполнения
        File fle = File.createTempFile("upload_", ".tmp");
        try (java.io.InputStream input = filePart.getInputStream()) {
            java.nio.file.Files.copy(input, fle.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }


        //String fnOrg = getReq().getParams().getString("filename");
        //boolean fill = getReq().getParams().getBoolean("fill");

        //Сгенерированный файл
        //File fle = findFile(tempDir);


        ModelService modelSvc = getApp().bean(ModelService.class);
        Mdb mdb = modelSvc.getModel().createMdb();
        FillDao dao = mdb.createDao(FillDao.class);
        dao.fillFishing_1(fle, fill);

        getReq().render("filename: " + fnOrg);

    }

    /*private File findFile(String path) throws Exception {
        for(File item : Objects.requireNonNull(new File(path).listFiles())){
            if (!item.isDirectory()){
                if (item.getName().startsWith("undertow") &&
                        item.getName().endsWith("upload")) {
                    return item;
                }
            }
        }
        return null;
    }*/


}
