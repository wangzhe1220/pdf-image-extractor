import com.alibaba.fastjson.JSONArray;
import model.Image;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import pdfbox.PDFImageStreamEngine;
import pdfbox.PDFTextStripperByImageArea;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: wangzhe
 * @Date: 2021/7/23 16:40
 */
public class Extractor {

    public String extractImages(String pdfFilePath, String imageSavePath) {
        List<Image> imageResult = new ArrayList<>();
        File pdfFile = new File(pdfFilePath);
        try (PDDocument document = PDDocument.load(pdfFile)) {

            if (!imageSavePath.endsWith("/")) {
                imageSavePath = imageSavePath + "/";
            }
            mkdir(imageSavePath);


            PDFImageStreamEngine printer = new PDFImageStreamEngine();

            for (int i = 0; i < document.getNumberOfPages(); i++) {
                PDPage page = document.getPage(i);
                float pageHeight = page.getMediaBox().getHeight();
                float pageWidth = page.getMediaBox().getWidth();

                // get all image coordinates from current page
                printer.clearPageLocations();
                printer.processPage(page);
                List<Image> locations = printer.getPageLocations();
                List<RenderedImage> images = getImagesFromResources(page.getResources());
                // continue if there's no image in current page
                if (images.size() == 0) {
                    continue;
                }

                // judge whether image object coordinates equals image layout coordinates
                boolean rightLocations = true;
                if (locations.size() != images.size()) {
                    rightLocations = false;
                }

                for (int j = 0; j < images.size(); j++) {
                    RenderedImage renderedImage = images.get(j);
                    Image imageDto = new Image();
                    imageDto.setPageNum(i + 1);
                    imageDto.setSourceImage(renderedImage);
                    if (rightLocations) {
                        imageDto.setCoordinateX(locations.get(j).getCoordinateX());
                        imageDto.setCoordinateY(locations.get(j).getCoordinateY());
                        imageDto.setWidth(locations.get(j).getWidth());
                        imageDto.setHeight(locations.get(j).getHeight());

                        // find image descriptions
                        // construct text box UNDER image
                        // TODO: is there a better way to find image description?
                        float areaCoordinateX = 0f;
                        float areaCoordinateY = 0f;
                        float areaWidth = 0f;
                        float areaHeight = 0f;
                        float heightTolerance = 0f;

                        // To ensure that the text immediately below the picture can be intercepted, the Y coordinate is raised here
                        heightTolerance = pageHeight * 0.02f;
                        // Take the area about 8% of the page height as the height (about three lines)
                        areaHeight = pageHeight * 0.08f + heightTolerance;
                        // three ways to determine the picture width
                        // full page picture
                        if (imageDto.getWidth() > pageWidth * 0.5) {
                            areaWidth = pageWidth;
                            areaCoordinateX = 0f;
                            areaCoordinateY = imageDto.getCoordinateY() + imageDto.getHeight() - heightTolerance;
                        } else {
                            // half page picture
                            areaWidth = pageWidth * 0.5f;
                            areaCoordinateY = imageDto.getCoordinateY() + imageDto.getHeight() - heightTolerance;
                            // right side
                            if (imageDto.getCoordinateX() > pageWidth * 0.5) {
                                areaCoordinateX = pageWidth * 0.5f;
                            } else {
                                // left side
                                areaCoordinateX = 0f;
                            }
                        }
                        // draw a definite area
                        Rectangle rect = new Rectangle((int) areaCoordinateX, (int) areaCoordinateY, (int) areaWidth, (int) areaHeight);
                        // extract image description by font size
                        PDFTextStripperByImageArea stripper = new PDFTextStripperByImageArea();
                        stripper.setSortByPosition(true);
                        stripper.addRegion("area", rect);
                        stripper.extractRegions(page);
                        String imageText = stripper.getTextForRegion("area");
                        imageText = formatTitleByFont(imageText);
                        // extract image description by description meanings
                        PDFTextStripperByArea stripper2 = new PDFTextStripperByArea();
                        stripper2.setSortByPosition(true);
                        stripper2.addRegion("area", rect);
                        stripper2.extractRegions(page);
                        String imageText2 = stripper2.getTextForRegion("area");
                        imageText2 = formatTitleBySemantic(imageText2);
                        // i'll choose longer one
                        String resultText = imageText.trim().length() > imageText2.trim().length() ? imageText : imageText2;
                        imageDto.setName(resultText);
                    }
                    imageResult.add(imageDto);
                }

                // remove tiny images(total pixels less than 1000)
                imageResult.removeIf(image -> {
                    Float height = image.getHeight();
                    Float width = image.getWidth();
                    if (Objects.isNull(height) || Objects.isNull(width)) {
                        return true;
                    }
                    Float area = height * width;
                    if (area < 1000f) {
                        return true;
                    }
                    return false;
                });

                for (int k = 0; k < imageResult.size(); k++) {
                    Image dto = imageResult.get(k);
                    int seq = k + 1;
                    String imagePath = imageSavePath + seq + ".jpg";
                    File out = new File(imagePath);
                    dto.setImagePath(imagePath);
                    dto.setSeqno(seq);
                    RenderedImage image = dto.getSourceImage();

                    ImageIO.write(convertRenderedImage(image), "jpg", out);
                }


            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return JSONArray.toJSONString(imageResult);

    }

    private static void mkdir(String dir) {
        File folder = new File(dir);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        if (!folder.canWrite()) {
            folder.setWritable(true);
        }
    }

    private static List<RenderedImage> getImagesFromResources(PDResources resources) throws IOException {
        List<RenderedImage> images = new ArrayList<>();
        for (COSName xObjectName : resources.getXObjectNames()) {
            PDXObject xObject = resources.getXObject(xObjectName);
            // handle FormXObject
            if (xObject instanceof PDFormXObject) {
                images.addAll(getImagesFromResources(((PDFormXObject) xObject).getResources()));
                // handle ImageXObject
            } else if (xObject instanceof PDImageXObject) {
                images.add(((PDImageXObject) xObject).getImage());
            }
        }
        return images;
    }

    private String formatTitleByFont(String text) {
        String result = "";
        // delete separator and useless symbols
        text = text.replace(System.getProperty("line.separator"), "").trim();
        if (Objects.isNull(text) || text.length() == 0) {
            return "";
        }
        // sort and save by font size
        List<String> strs = new ArrayList<>();
        strs.addAll(Arrays.asList(text.split("★")));
        float minSize = 50f;
        for (String str : strs) {
            String fontsizeStr = "";
            try {
                fontsizeStr = str.substring(0, str.indexOf("]")).trim().replace("[", "").replace("]", "");
            } catch (IndexOutOfBoundsException e) {
                continue;
            }
            float fontsize = Float.valueOf(fontsizeStr);
            if (fontsize < minSize) {
                minSize = fontsize;
                result = str;
            }
        }
        try {
            result = result.substring(result.indexOf("]") + 1);
        } catch (IndexOutOfBoundsException e) {
            // do nothing
        }
        return result;
    }

    private String formatTitleBySemantic(String text) {
        String result = "";


        // get system line separator
        String lineSeparator = System.getProperty("line.separator");
        // full angle char to half angle char
        text = fullAngle2halfAngle(text);
        // delete space between two Chinese characters
        text = deleteBlankBetweenCHNChars(text);


        if (Objects.isNull(text) || text.length() == 0) {
            return "";
        }

        // convert to lowercase
        String lowercaseText = text.toLowerCase();

        // find image keyword
        int picIndex = lowercaseText.indexOf("图");
        int figIndex = lowercaseText.indexOf("fig");

        // Chinese text has higher priority
        if (picIndex > 0) {
            if (figIndex > 0) {
                // if Chinese and English coexist, split according the position of first Eng char
                result = text.substring(picIndex, figIndex);
            } else {
                // else use separator
                result = text.substring(picIndex, text.indexOf(lineSeparator, picIndex));
            }
        } else if (figIndex > 0) {
            // use separator if there's only English
            result = text.substring(figIndex, text.indexOf(lineSeparator, figIndex));
        }

        return result;

    }

    /**
     * full angle char to half angle char
     *
     * @param str
     * @return
     */
    private static String fullAngle2halfAngle(String str) {
        if (Objects.isNull(str) || str.length() == 0) {
            return "";
        }
        // full angle char to half angle char
        char c[] = str.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (c[i] == '\u3000') {
                c[i] = ' ';
            } else if (c[i] > '\uFF00' && c[i] < '\uFF5F') {
                c[i] = (char) (c[i] - 65248);
            }
        }
        return new String(c);
    }


    /**
     * Judge whether there is a space between two Chinese characters
     */
    private static Pattern delBlankBetweenCHNCharsPattern = Pattern.compile("(?<=[\\x{4e00}-\\x{9fa5}])\\s(?=[\\x{4e00}-\\x{9fa5}])");


    private static String deleteBlankBetweenCHNChars(String str) {
        if (Objects.isNull(str) || str.length() == 0) {
            return "";
        }
        Matcher m = delBlankBetweenCHNCharsPattern.matcher(str);
        return m.replaceAll("");
    }

    /**
     * RenderedImage -> BufferedImage
     * @param img
     * @return
     */
    private BufferedImage convertRenderedImage(RenderedImage img) {
        if (img instanceof BufferedImage) {
            BufferedImage newBufferedImage = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
            newBufferedImage.createGraphics().drawImage((BufferedImage) img, 0, 0, Color.WHITE, null);
            return newBufferedImage;
        }
        ColorModel cm = img.getColorModel();
        int width = img.getWidth();
        int height = img.getHeight();
        WritableRaster raster = cm.createCompatibleWritableRaster(width, height);
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        Hashtable properties = new Hashtable();
        String[] keys = img.getPropertyNames();
        if (keys != null) {
            for (int i = 0; i < keys.length; i++) {
                properties.put(keys[i], img.getProperty(keys[i]));
            }
        }
        BufferedImage result = new BufferedImage(cm, raster, isAlphaPremultiplied, properties);
        img.copyData(raster);
        BufferedImage newBufferedImage = new BufferedImage(result.getWidth(), result.getHeight(), BufferedImage.TYPE_INT_RGB);
        newBufferedImage.createGraphics().drawImage(result, 0, 0, Color.WHITE, null);
        return newBufferedImage;
    }
}
