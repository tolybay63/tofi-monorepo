package tofi.nsi.pdf;

import com.documents4j.api.DocumentType;
import com.documents4j.api.IConverter;
import com.documents4j.job.LocalConverter;
import jandcode.core.apx.test.Apx_Test;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.concurrent.TimeUnit;

public class CnvToPdfTest extends Apx_Test {

    @Test
    void testWord2Prf() throws Exception {
        String docxFileName = "D:\\cnv\\word.docx";
        String pdfFileName = "D:\\cnv\\word.pdf";



        try (InputStream docxInputStream = new FileInputStream(docxFileName);
             OutputStream pdfOutputStream = new FileOutputStream(pdfFileName)) {
            IConverter converter = LocalConverter.builder()
                    .workerPool(20, 25, 2, TimeUnit.SECONDS)
                    .processTimeout(5, TimeUnit.SECONDS)
                    .build();

            converter.convert(docxInputStream).as(DocumentType.MS_WORD)
                    .to(pdfOutputStream).as(DocumentType.PDF)
                    .execute();

            converter.shutDown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Test
    void testExcel2Pdf() throws Exception {
        String docxFileName = "D:\\cnv\\excel.xlsx";
        String pdfFileName = "D:\\cnv\\excel.pdf";

        try (InputStream docxInputStream = new FileInputStream(docxFileName);
             OutputStream pdfOutputStream = new FileOutputStream(pdfFileName)) {
            IConverter converter = LocalConverter.builder()
                    .workerPool(20, 25, 2, TimeUnit.SECONDS)
                    .processTimeout(5, TimeUnit.SECONDS)
                    .build();

            converter.convert(docxInputStream).as(DocumentType.MS_EXCEL)
                    .to(pdfOutputStream).as(DocumentType.PDF)
                    .execute();

            converter.shutDown();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Test
    void testImage2Pdf() throws Exception {
        String imagePath="d:\\cnv\\bmp.bmp";
        String outputFile="d:\\cnv\\bmp.pdf";

        PDDocument document = new PDDocument();
        InputStream in = new FileInputStream(imagePath);
        BufferedImage bimg = ImageIO.read(in);
        float width = bimg.getWidth();
        float height = bimg.getHeight();
        PDPage page = new PDPage(new PDRectangle(width, height));
        document.addPage(page);
        PDImageXObject img = LosslessFactory.createFromImage(document, bimg);    //new PDImage(document, new FileInputStream(imagePath));
        PDPageContentStream contentStream = new PDPageContentStream(document, page);
        contentStream.drawImage(img, 0, 0);
        contentStream.close();
        in.close();
        document.save(outputFile);
        document.close();


    }


}


