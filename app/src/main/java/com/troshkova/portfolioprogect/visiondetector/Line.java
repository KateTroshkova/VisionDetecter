package com.troshkova.portfolioprogect.visiondetector;

import org.opencv.core.Point;

public class Line {

    Point from;
    Point to;
    double a, b, c;

    public Line(Point from, Point to){
        this.from=from;
        this.to=to;
        convertPointsToCoefficients();
    }

    public double getA(){
        return a;
    }

    public double getB(){
        return b;
    }

    public double getC(){
        return c;
    }

    public Point intersect(Line line){
        //TODO:проверить сучай параллельности и совпадения
        Point result=new Point();
        result.x=-1*((c*line.getB()-line.getC()*b)/(a*line.getB()-line.getA()*b));
        result.y=-1*((a*line.getC()-line.getA()*c)/(a*line.getB()-line.getA()*b));
        return result;
    }

    private void convertPointsToCoefficients(){
        a=from.y-to.y;
        b=to.x-from.x;
        c=from.x*to.y-to.x*from.y;
    }
}
