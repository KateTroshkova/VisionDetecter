package ru.eyetracker;

import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class Face {

    private Rect region;
    private Rect rightEyeRegion;
    private Rect leftEyeRegion;

    public Face(Rect region){
        this.region=region;
        rightEyeRegion = new Rect(region.x + region.width / 16, (int) (region.y + (region.height / 4.5)),
                (region.width - 2 * region.width / 16) / 2, (int) (region.height / 3.0));
        leftEyeRegion = new Rect(region.x + region.width / 16 + (region.width - 2 * region.width / 16) / 2,
                (int) (region.y + (region.height / 4.5)), (region.width - 2 * region.width / 16) / 2, (int) (region.height / 3.0));
    }

    public Rect getRegion(){
        return region;
    }

    public void setRegion(Rect region){
        this.region=region;
    }

    public Rect getRightEyeRegion(){
        return rightEyeRegion;
    }

    public void setRightEyeRegion(Rect rightEyeRegion){
        this.rightEyeRegion=rightEyeRegion;
    }

    public Rect getLeftEyeRegion(){
        return leftEyeRegion;
    }

    public void setLeftEyeRegion(Rect leftEyeRegion){
        this.leftEyeRegion=leftEyeRegion;
    }

    public void draw(Mat mat){
        Imgproc.rectangle(mat, region.tl(), region.br(), new Scalar(0, 255, 255, 255));
        Imgproc.rectangle(mat, region.tl(), region.br(), new Scalar(0, 255, 128, 255));
        Imgproc.rectangle(mat, region.tl(), region.br(), new Scalar(0, 255, 128, 255));
    }
}
