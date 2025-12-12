package fish.monitoring.action;

import jandcode.commons.UtFile;
import jandcode.core.web.UtWeb;
import jandcode.core.web.render.BaseRender;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class DownFileRender extends BaseRender {

    @Override
    protected void onRender(Object data) throws Exception {
        DownFile md = (DownFile) data;

        UtWeb.setHeaderDownload(getRequest(), md.getClientName(), md.getFile().length());

        try (InputStream stmSource = new FileInputStream(md.getFile())) {
            OutputStream stmDest = getRequest().getOutStream();
            UtFile.copyStream(stmSource, stmDest);
        }

    }

}
