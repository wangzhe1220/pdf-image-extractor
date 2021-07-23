package pdfbox;

import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.util.List;

public class PDFTextImageTitleStripper extends PDFTextStripper {


    public PDFTextImageTitleStripper() throws Exception {
    }

    String prevBaseFont = "";
    float prevFontSizePt = 1.0F;
    int tolerance = 1;
    float nextFontSizePt = 1.0F;
    float sizeAfterNextFont = 1.0F;

    @Override
    protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
        StringBuilder builder = new StringBuilder();

        boolean end = false;
        boolean isNumEnd = false;

        for (int i = 0; i < textPositions.size(); i++) {
            TextPosition position = textPositions.get(i);
            Float baseFont = position.getFontSizeInPt();

            try {
                nextFontSizePt = textPositions.get(i + 1).getFontSizeInPt();
            } catch (IndexOutOfBoundsException e) {
                nextFontSizePt = baseFont;
                end = true;
            }
            if (prevFontSizePt == nextFontSizePt) {
                baseFont = prevFontSizePt;
            } else {
                // preview next two chars
                try {
                    sizeAfterNextFont = textPositions.get(i + 2).getFontSizeInPt();
                } catch (IndexOutOfBoundsException e) {
                    sizeAfterNextFont = baseFont;
                    end = true;
                }
                if (prevFontSizePt == sizeAfterNextFont) {
                    baseFont = prevFontSizePt;
                }
            }
            if (end) {
                try {
                    int num = Integer.parseInt(position.getUnicode());
                    if (num >= 1 && num <= 9) {
                        isNumEnd = true;
                    }
                } catch (Exception e) {

                    // do noting
                }
            }
            if (!baseFont.equals(prevFontSizePt) && !isNumEnd) {
                builder.append("★[").append(baseFont).append(']');
            }
            if (!isNumEnd) {
                prevFontSizePt = baseFont;
            }
            builder.append(position.getUnicode());
        }

        writeString(builder.toString());
    }
}
