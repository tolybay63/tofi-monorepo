package fish.monitoring.action;

import jandcode.commons.UtJson;
import jandcode.commons.conf.Conf;
import jandcode.commons.error.XError;
import jandcode.commons.variant.IVariantMap;
import jandcode.commons.variant.VariantMap;
import jandcode.core.std.DataDirService;
import jandcode.core.web.action.BaseAction;
import tofi.api.dta.ApiMonitoringData;
import tofi.api.mdl.ApiMeta;
import tofi.api.mdl.utils.dbfilestorage.DbFileStorageItem;
import tofi.api.mdl.utils.dbfilestorage.DbFileStorageService;
import tofi.apinator.ApinatorApi;
import tofi.apinator.ApinatorService;

import java.io.File;

public class UploadAction extends BaseAction {

    ApinatorApi apiMeta() {
        return getApp().bean(ApinatorService.class).getApi("meta");
    }

    ApinatorApi apiMonitoringData() {
        return getApp().bean(ApinatorService.class).getApi("monitoringdata");
    }


    protected void onExec() throws Exception {
        // 1. Извлекаем параметры метаданных
        IVariantMap params = UtJson.fromJson(getReq().getParams().getString("params"), VariantMap.class);
        String fnOrg = params.getString("filename");

        javax.servlet.http.Part filePart = getReq().getPart("file");

        if (filePart == null) {
            throw new XError("File not found in request payload");
        }

        // 3. Создаем временный файл, привязанный строго к этому потоку выполнения
        File fle = File.createTempFile("upload_", ".tmp");
        try (java.io.InputStream input = filePart.getInputStream()) {
            java.nio.file.Files.copy(input, fle.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        // 4. Стандартная логика маршрутизации хранилища
        params.put("filePath", fle.getAbsolutePath());

        String path = "";
        try {
            path = getApp().bean(DataDirService.class).getPath("dbfilestorage");
        } catch (Exception e) {
            path = "";
        }

        String bucketName;
        try {
            Conf conf2 = getApp().getConf().getConf("datadir/minio");
            bucketName = conf2.getString("bucketName");
        } catch (Exception e) {
            bucketName = "";
        }

        // Временно форсируем работу ТОЛЬКО через FileSystem, пока MinIO на стадии разработки
        if (!path.isEmpty()) {
            uploadFS(fle, params);
            // Обязательно чистим временный файл после успешного импорта
            if (fle.exists()) {
                fle.delete();
            }
        } else {
            // Если путь к локальному хранилищу FS пуст — вот тогда жестко сигнализируем
            if (fle.exists()) {
                fle.delete();
            }
            throw new XError("Критическая ошибка: Локальное FileStorage не настроено в конфигурации системы!");
        }

        getReq().render("FileName: " + fnOrg);

    }

    private void uploadMinio(IVariantMap params) throws Exception {
        System.out.println(params);
        throw new XError("На стадии разработки");
    }

    private void uploadFS(File fle, IVariantMap params) throws Exception {
        String fnOrg = params.getString("filename");
        try {
            DbFileStorageService fsService = apiMeta().get(ApiMeta.class).getDbFileStorageService();
            fsService.setModelName("monitoringdata");
            DbFileStorageItem dfi = fsService.addFile(fle, fnOrg);
            long idFile = dfi.getId();
            //
            try {
                params.put("fileVal", idFile);
                apiMonitoringData().get(ApiMonitoringData.class).attachFile(params);
            } catch (Exception e) {
                fsService.removeFile(idFile);
                throw new XError(e.getMessage());
            }
        } catch (Exception e) {
            throw new XError(e.getMessage());
        }
    }

}
