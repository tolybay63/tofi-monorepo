package fish.calc.action;

import jandcode.commons.UtJson;
import jandcode.commons.error.XError;
import jandcode.commons.variant.IVariantMap;
import jandcode.commons.variant.VariantMap;
import jandcode.core.std.DataDirService;
import jandcode.core.web.action.BaseAction;
import tofi.api.mdl.ApiMeta;
import tofi.api.mdl.utils.dbfilestorage.DbFileStorageItem;
import tofi.api.mdl.utils.dbfilestorage.DbFileStorageService;
import tofi.apinator.ApinatorApi;
import tofi.apinator.ApinatorService;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class UploadCalcAction extends BaseAction {

    ApinatorApi apiMeta() {
        return getApp().bean(ApinatorService.class).getApi("meta");
    }

    protected void onExec() throws Exception {
        // 1. Извлекаем параметры метаданных
        String paramsStr = getReq().getParams().getString("params");
        if (paramsStr.isEmpty()) {
            paramsStr = "{}"; // Защита от пустых параметров
        }
        IVariantMap params = UtJson.fromJson(paramsStr, VariantMap.class);

        // filename теперь берем из оригинального имени загружаемого файла, если его нет в params
        javax.servlet.http.Part filePart = getReq().getPart("file");
        if (filePart == null) {
            throw new XError("File not found in request payload");
        }

        String fnOrg = params.getString("filename", filePart.getSubmittedFileName());

        // 2. Создаем временный файл
        File fle = File.createTempFile("upload_calc_", ".tmp");
        try (java.io.InputStream input = filePart.getInputStream()) {
            java.nio.file.Files.copy(input, fle.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        String path = "";
        try {
            path = getApp().bean(DataDirService.class).getPath("dbfilestorage");
        } catch (Exception e) {
            path = "";
        }

        // 3. Форсируем работу через FileSystem
        if (!path.isEmpty()) {
            long fileId = uploadFS(fle, fnOrg);

            // Чистим временный файл
            if (fle.exists()) {
                fle.delete();
            }

            // 4. ВОЗВРАЩАЕМ JSON С ID ФАЙЛА ДЛЯ calc-fastapi
            Map<String, Object> result = new HashMap<>();
            result.put("id", fileId);
            result.put("filename", fnOrg);
            getReq().render(UtJson.toJson(result));

        } else {
            if (fle.exists()) {
                fle.delete();
            }
            throw new XError("Критическая ошибка: Локальное FileStorage не настроено в конфигурации системы!");
        }
    }

    private long uploadFS(File fle, String fnOrg) throws Exception {
        try {
            DbFileStorageService fsService = apiMeta().get(ApiMeta.class).getDbFileStorageService();
            // Можно создать отдельную модель для calc или использовать общую
            fsService.setModelName("calcdata");
            DbFileStorageItem dfi = fsService.addFile(fle, fnOrg);
            return dfi.getId();
        } catch (Exception e) {
            throw new XError("Ошибка при сохранении файла в dbfilestorage: " + e.getMessage());
        }
    }
}