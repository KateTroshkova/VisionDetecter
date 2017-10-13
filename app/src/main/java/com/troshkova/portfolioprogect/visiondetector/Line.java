package com.troshkova.portfolioprogect.visiondetector;

import android.util.Log;

import org.opencv.core.Point;

//TODO: пересмотреть алгоритм на слишком далекое пересечение
//структура данных для хранения линий-направлений взгляда
//встроенных в open_cv нет
public class Line {

    //начаьная точка-центр глаза
    private Point from;
    //конечная точка-направление взгляда
    private Point to;
    //коэффициенты в уравнении прямой
    //для удобства успользуются те же обозначения, что и в аналитической геометрии
    private double a, b, c;

    public Line(Point from, Point to){
        this.from=from;
        this.to=to;
        convertPointsToCoefficients();
    }

    private double getA(){
        return a;
    }

    private double getB(){
        return b;
    }

    private double getC(){
        return c;
    }

    public Point getFrom(){
        return from;
    }

    public Point getTo(){
        return to;
    }

    //вычисляет точку пересечения линий
    //она же-точка, на которую смотрит пользоватеь
    //если линии параллельны, то искомая точка - среднее арифметическое между направлениями взглядов
    //если прямые не параллельны, то искомая точка - решения уравнений
    //a1x+b1y+c1=0
    //a2x+b2y+c2=0
    //если линии пересекаются вне экрана, то в зависимости от приоритета
    //выбирается либо правое, либо левое направление взгляда
    public Point intersect(Line line, Point tl, Point br, boolean rightPriority){
        Log.e("POINT_LINE", to.x+" "+to.y+" "+from.x+" "+from.y);
        Log.e("POINT_LINE", line.getTo().x+" "+line.getTo().y+" "+line.getFrom().x+" "+line.getFrom().y);
        if (parallel(line)){
            Log.e("POINT", (to.x+line.getTo().x)/2+" "+(to.y+line.getTo().y)/2);
            return new Point((to.x+line.getTo().x)/2, (to.y+line.getTo().y)/2);
        }
        Point result=new Point();
        result.x=-1*((c*line.getB()-line.getC()*b)/(a*line.getB()-line.getA()*b));
        result.y=-1*((a*line.getC()-line.getA()*c)/(a*line.getB()-line.getA()*b));
        if (result.x<tl.x || result.y<tl.y || result.x>br.x || result.y>br.y){
            if (rightPriority) {
                Log.e("POINT", "out");
                return to;
            }
            else{
                Log.e("POINT", "out");
                return line.getTo();
            }
        }
        Log.e("POINT", result.x+" "+result.y);
        return result;
    }

    //если прямые параллельны и коэффициенты пропорциональны, то линии совпадают
    private boolean equal(Line line){
        return parallel(line) &&
                (a*line.getC()-line.getA()*c)==0 &&
                (b*line.getC()-line.getB()*c==0);

    }

    //если определитель матрица коэффициентов равен нулю, то линии параллельны
    private boolean parallel(Line line){
        return (a*line.getB()-line.getA()*b)==0;
    }

    //коэффициенты выводятся из уравнения прямой по двум точкам
    //(x-x1)/(x2-x1)=(y-y1)/(y2-y1)
    private void convertPointsToCoefficients(){
        a=from.y-to.y;
        b=to.x-from.x;
        c=from.x*to.y-to.x*from.y;
    }
}
