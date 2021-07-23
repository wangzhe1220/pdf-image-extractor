package pdfbox;

import model.Image;
import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.DrawObject;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.contentstream.operator.state.Concatenate;
import org.apache.pdfbox.contentstream.operator.state.Restore;
import org.apache.pdfbox.contentstream.operator.state.Save;
import org.apache.pdfbox.contentstream.operator.state.SetGraphicsStateParameters;
import org.apache.pdfbox.contentstream.operator.state.SetMatrix;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: wangzhe
 * @Date: 2021/7/23 15:53
 */
public class PDFImageStreamEngine extends PDFStreamEngine {

    private List<Image> pageLocations = new ArrayList<>();

    public PDFImageStreamEngine() throws IOException {
        this.addOperator(new Concatenate());
        this.addOperator(new DrawObject());
        this.addOperator(new SetGraphicsStateParameters());
        this.addOperator(new Save());
        this.addOperator(new Restore());
        this.addOperator(new SetMatrix());
    }

    @Override
    protected void processOperator(Operator operator, List<COSBase> operands) throws IOException {
        String operation = operator.getName();
        if ("Do".equals(operation)) {
            COSName objectName = (COSName)operands.get(0);
            PDXObject xobject = this.getResources().getXObject(objectName);
            if (xobject instanceof PDImageXObject) {

                Matrix ctmNew = getGraphicsState().getCurrentTransformationMatrix();
                float imageXScale = ctmNew.getScalingFactorX();
                float imageYScale = ctmNew.getScalingFactorY();

                float yScaling = ctmNew.getScaleY();
                float angle = (float)Math.acos(ctmNew.getValue(0, 0)/ctmNew.getScaleX());
                if (ctmNew.getValue(0, 1) < 0 && ctmNew.getValue(1, 0) > 0)
                {
                    angle = (-1)*angle;
                }

                PDPage page = getCurrentPage();
                double pageHeight = page.getMediaBox().getHeight();
                ctmNew.setValue(2, 1, (float)(pageHeight - ctmNew.getTranslateY() - Math.cos(angle)*yScaling));
                ctmNew.setValue(2, 0, (float)(ctmNew.getTranslateX() - Math.sin(angle)*yScaling));

                ctmNew.setValue(0, 1, (-1)*ctmNew.getValue(0, 1));
                ctmNew.setValue(1, 0, (-1)*ctmNew.getValue(1, 0));

                Image location = new Image();
                location.setCoordinateX(ctmNew.getTranslateX());
                location.setCoordinateY(ctmNew.getTranslateY());
                location.setWidth(imageXScale);
                location.setHeight(imageYScale);
                pageLocations.add(location);

            } else if (xobject instanceof PDFormXObject) {
                PDFormXObject form = (PDFormXObject)xobject;
                this.showForm(form);
            }
        } else {
            super.processOperator(operator, operands);
        }
    }

    public List<Image> getPageLocations() {
        return pageLocations;
    }

    public void clearPageLocations() {
        pageLocations.clear();
    }
}
