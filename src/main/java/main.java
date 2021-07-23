

/**
 * @Author: wangzhe
 * @Date: 2021/7/23 15:07
 */
public class main {


    public static void main(String[] args) {
        String pdfFileName = "D:\\GIt\\pdf-image-extractor\\src\\main\\resources\\Brazing of stainless steels using Cu-Ag-Mn-Zn braze filler Studies on wettability,mechanical properties,and microstructural aspects.pdf";
        String imageSavePath = "D:\\pdfimage\\";
        Extractor extractor = new Extractor();
        System.out.println(extractor.extractImages(pdfFileName, imageSavePath));
    }



}
