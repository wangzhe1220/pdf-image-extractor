package model;

import com.alibaba.fastjson.annotation.JSONField;

import java.awt.image.RenderedImage;

/**
 * PDF model.Image Model
 * @Author: wangzhe
 * @Date: 2021/7/23 15:20
 */
public class Image {

    private static final long serialVersionUID = 1L;

    /**
     * X coordinate of the upper left corner of the picture
     */
    private Float coordinateX;

    /**
     * Y coordinate of the upper left corner of the picture
     */
    private Float coordinateY;

    /**
     * image width(pixel)
     */
    private Float width;

    /**
     * image height(pixel)
     */
    private Float height;

    /**
     * image path
     */
    private String imagePath;

    /**
     * image page num
     */
    private Integer pageNum;

    /**
     * image seq num
     */
    private Integer seqno;

    /**
     * image name
     */
    private String name;

    /**
     * picture binary object
     */
    @JSONField(serialize = false)
    private RenderedImage sourceImage;

    public Float getCoordinateX() {
        return coordinateX;
    }

    public void setCoordinateX(Float coordinateX) {
        this.coordinateX = coordinateX;
    }

    public Float getCoordinateY() {
        return coordinateY;
    }

    public void setCoordinateY(Float coordinateY) {
        this.coordinateY = coordinateY;
    }

    public Float getWidth() {
        return width;
    }

    public void setWidth(Float width) {
        this.width = width;
    }

    public Float getHeight() {
        return height;
    }

    public void setHeight(Float height) {
        this.height = height;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getSeqno() {
        return seqno;
    }

    public void setSeqno(Integer seqno) {
        this.seqno = seqno;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RenderedImage getSourceImage() {
        return sourceImage;
    }

    public void setSourceImage(RenderedImage sourceImage) {
        this.sourceImage = sourceImage;
    }

}
