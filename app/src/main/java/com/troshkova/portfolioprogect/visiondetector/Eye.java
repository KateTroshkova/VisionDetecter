package com.troshkova.portfolioprogect.visiondetector;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class Eye {

    private Point pupil;
    private Point center;
    private Rect region;

    public Eye(Point pupil, Point center, Rect region){
        this.pupil=pupil;
        this.center=center;
        this.region=region;
    }

    public Eye(Mat grayMat, Rect region){
        this.region=region;
        calculateCenter();
        calculatePupil(grayMat);
    }

    public Point getPupil(){
        return pupil;
    }

    public Point getCenter(){
        return center;
    }

    public Rect getRegion(){
        return region;
    }

    public void setPupil(Point pupil){
        this.pupil=pupil;
    }

    public void setCenter(Point center){
        this.center=center;
    }

    public void setRegion(Rect region){
        this.region=region;
    }

    private void calculateCenter(){
        center=new Point(region.tl().x + region.width / 2, region.tl().y + region.height / 2);
    }

    private void calculatePupil(Mat grayMat){
        Core.MinMaxLocResult darkestPoint = Core.minMaxLoc(grayMat.submat(region));
        pupil = new Point(darkestPoint.minLoc.x + region.x, darkestPoint.minLoc.y + region.y);
    }

    public void draw(Mat mat, Scalar regionScalar, Scalar centerScalar, Scalar pupilScalar){
        if (region!=null) {
            Imgproc.rectangle(mat, region.tl(), region.br(), regionScalar);
        }
        if (center!=null) {
            Imgproc.circle(mat, center, 2, centerScalar, 2);
        }
        if (pupil!=null) {
            Imgproc.circle(mat, pupil, 2, pupilScalar, 2);
        }
    }
}
