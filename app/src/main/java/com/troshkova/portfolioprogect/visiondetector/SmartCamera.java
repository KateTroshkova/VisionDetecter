package com.troshkova.portfolioprogect.visiondetector;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.WindowManager;

import com.troshkova.portfolioprogect.visiondetector.exception.Exception;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

//TODO: поворот вручную
//TODO: пересчет реже
//камера, обрабатывающая изображение. находит глаза и вычисляет направление взгляда
//родительский класс из open_cv CameraBridgeViewBase изменен!
public class SmartCamera extends JavaCameraView implements CameraBridgeViewBase.CvCameraViewListener{

    //классификатор, распознающий лицо и глаза
    private FaceClassifier classifier;
    //черно-белая матрица
    //матрица такого типа необходима для поиска лиц и части преобразований матриц
    private Mat grayMat;
    //активность, на которую прикрепляется камера
    //нужна для получения информации о наличии и типе камеры на устройстве
    //обычного контекста не достаточно
    private Activity parent;
    //интерфейс, который должна расширить активность, чтобы обрабатывать возникающие в процессе работы исключения
    private OnCameraExceptionListener listener;
    //интерфейс, через который активность получает точку, на которую смотрит пользователь
    private OnDetectSightListener sightListener;
    //переменная-флаг
    //если true будет происходить построение дополнительных линий
    private boolean debug=true;
    //переменная-флаг
    //если true при вычислении взгляда будет отдан приоритет правому глазу
    //иначе-левому
    private boolean rightPriority=true;
    //обасть, на которую смотрит пользователь, с учетом погрешности
    //за область берется максимум из областей глаз
    //так как все возможные ошибки  в нахождении контрольных точек и линий
    //связаны с найденной областью
    private Rect error=new Rect();


    private int rotate=0;
    private boolean mirror=false;

    public interface OnCameraExceptionListener{
        void onCameraExceptionListener(Exception exception);
    }

    public interface OnDetectSightListener{
        void onDetectSight(Point sight, Rect region);
    }

    //инициализация связанных с родительской активностью полей
    //проверка наличия камер
    //загрузка/подготовка open_cv
    public SmartCamera(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.parent=(Activity)getContext();
        this.listener=(OnCameraExceptionListener)getContext();
        this.sightListener=(OnDetectSightListener)getContext();

        if (hasCamera()) {
            if (hasFrontCamera()) {
                applyScreenSettings();
                setCvCameraViewListener(this);
                if (libReady()) {
                    classifier=new FaceClassifier(context, listener);
                    enableFpsMeter();
                    enableView();
                } else {
                    listener.onCameraExceptionListener(Exception.EXCEPTION_LIB_NOT_READY);
                }
            }
            else{
                listener.onCameraExceptionListener(Exception.EXCEPTION_NO_FRONT_CAMERA);
            }
        }
        else{
            listener.onCameraExceptionListener(Exception.EXCEPTION_NO_CAMERA);
        }

        SharedPreferences preferences=getContext().getSharedPreferences("preference_name", getContext().MODE_PRIVATE);
        mirror=preferences.getBoolean("mirror", false);
        rotate=preferences.getInt("rotation", 0);
    }

    //внешний интерфейс
    //нужно ли отрисовывать
    public void setRendering(boolean debug){
        this.debug=debug;
    }

    //внешний интерфейс
    //устраивает ли приоритет на правй глаз
    public void defaultPriority(boolean priority){
        rightPriority=priority;
    }

    public void addRotation(){
        rotate++;
        SharedPreferences preferences=getContext().getSharedPreferences("preference_name", getContext().MODE_PRIVATE);
        SharedPreferences.Editor editor=preferences.edit();
        editor.putInt("rotation", rotate%4);
        editor.apply();
    }

    public void mirror(){
        mirror=!mirror;
        SharedPreferences preferences=getContext().getSharedPreferences("preference_name", getContext().MODE_PRIVATE);
        SharedPreferences.Editor editor=preferences.edit();
        editor.putBoolean("mirror", mirror);
        editor.apply();
    }

    //уничтожение камеры
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        disableView();
        SharedPreferences preferences=getContext().getSharedPreferences("preference_name", getContext().MODE_PRIVATE);
        SharedPreferences.Editor editor=preferences.edit();
        editor.putBoolean("mirror", mirror);
        editor.putInt("rotation", rotate%4);
        editor.apply();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {}

    @Override
    public void onCameraViewStopped() {}

    //обновление экрана
    //исправляется ориентация
    //по параметрам входного изображения пересоздается черно-белая матрица
    //ищется точка, на которую смотрит пользователь
    //inputFrame(передается по ссылке) - матрица с прорисованными(если нужно) линиями
    //возвращается для отрисовки на экране
    @Override
    public Mat onCameraFrame(Mat inputFrame) {
        fixOrientation(inputFrame);
        grayMat = new Mat(inputFrame.rows(), inputFrame.cols(), CvType.CV_8UC4);
        Imgproc.cvtColor(inputFrame, grayMat, Imgproc.COLOR_RGBA2GRAY);
        userAttention(inputFrame);
        grayMat.release();
        return inputFrame;
    }

    //отражение по горизонтали
    private void mirror(Mat mat){
        Core.flip(mat, mat, 0);
        Core.rotate(mat, mat, Core.ROTATE_180);
    }

    //поворот на +90 градусов
    //изменение высоты и ширины родительского bitmap
    //необходимо, так как размер меняется
    //и размер изображения в вертикальном и горизонтальном состоянии не равны
    private void rotateP90(Mat mat){
        Core.transpose(mat, mat);
        setWidth(mat.cols());
        setHeight(mat.rows());
    }

    //поворот на -90 градусов
    private void rotateM90(Mat mat){
        rotateP90(mat);
        Core.flip(mat, mat, 0);
    }

    //поворот на 180 градусов
    private void rotate180(Mat mat){
        Core.rotate(mat, mat, Core.ROTATE_180);
    }

    //исправляет ориентацию для большинства устройств
    //выяснено экспериментально
    private void fixOrientation(Mat mat){
        mirror(mat);
        WindowManager windowManager= (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        int rotation = windowManager.getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_0: {
                rotateP90(mat);
                mirror(mat);
                break;
            }
            case Surface.ROTATION_90: {
                break;
            }
            case Surface.ROTATION_180: {
                rotateM90(mat);
                break;
            }
            case Surface.ROTATION_270: {
                rotate180(mat);
                break;
            }
            default:{
                listener.onCameraExceptionListener(Exception.EXCEPTION_STRANGE_ORIENTATION);
            }
        }
        for(int i=0; i<rotate; i++){
            rotateM90(mat);
        }
        if (mirror){
            mirror(mat);
        }
    }

    //на основании вспомогательных линий
    //ищет точку, на которую смотрит пользователь
    //если хотя бы один глаз распознан, координаты точки будут отданы активности
    //если распознаны оба глаза, то искомая точка ищется как пересечение двух линий
    //если есть только один глаз, то искомая точка-пересечение направления взгляда и края изображения
    //отрисовку нецелесообразно выносить в отдельный метод
    private void userAttention(Mat inputFrame){
        Rect face=getFace();
        if (face!=null) {
            Rect rightRegion = new Rect(face.x + face.width / 16, (int) (face.y + (face.height / 4.5)), (face.width - 2 * face.width / 16) / 2, (int) (face.height / 3.0));
            Rect leftRegion = new Rect(face.x + face.width / 16 + (face.width - 2 * face.width / 16) / 2, (int) (face.y + (face.height / 4.5)), (face.width - 2 * face.width / 16) / 2, (int) (face.height / 3.0));
            Line rightLine = getSightDirection(inputFrame, rightRegion);
            Line leftLine = getSightDirection(inputFrame, leftRegion);

            double coordinateX0=inputFrame.cols()/2;
            double coordinateY0=inputFrame.rows()/2;

            Point attention=new Point();
            if (rightLine!=null || leftLine!=null) {
                if (rightLine != null && leftLine != null) {
                    //обязательно right. ...
                    attention = rightLine.intersect(leftLine, new Point(0, 0), new Point(inputFrame.cols(), inputFrame.rows()), rightPriority);
                } else {
                    if (rightLine != null) {
                        attention = rightLine.getTo();
                    }
                    if (leftLine != null) {
                        attention = leftLine.getTo();
                    }
                }
                error=new Rect((int)(attention.x-error.width/2), (int)(attention.y-error.height/2), error.width/2, error.height/2);
                sightListener.onDetectSight(attention, error);
            }
            if (debug){
                Imgproc.rectangle(inputFrame, face.tl(), face.br(), new Scalar(0, 255, 255, 255));
                Imgproc.rectangle(inputFrame, rightRegion.tl(), rightRegion.br(), new Scalar(0, 255, 128, 255));
                Imgproc.rectangle(inputFrame, leftRegion.tl(), leftRegion.br(), new Scalar(0, 255, 128, 255));
                Imgproc.circle(inputFrame, attention, 3, new Scalar(255, 255, 255, 255), 3);
                Imgproc.rectangle(inputFrame, error.tl(), error.br(), new Scalar(255, 255, 255, 128));

                Imgproc.line(inputFrame, new Point(coordinateX0, 0), new Point(coordinateX0, inputFrame.cols()), new Scalar(0, 0, 255, 255));
                Imgproc.line(inputFrame, new Point(0, coordinateY0), new Point(inputFrame.rows(), coordinateY0), new Scalar(0, 0, 255, 255));
            }
        }
    }

    private int quarter(Mat inputMat, Point center, Point pupil){
        double coordinateX0=inputMat.cols()/2;
        double coordinateY0=inputMat.rows()/2;
        if (pupil.x>center.x && pupil.y<center.y){
            Imgproc.rectangle(inputMat, new Point(coordinateX0, 0), new Point(inputMat.cols(), coordinateY0), new Scalar(0, 0, 255, 255));
            return 1;
        }
        if (pupil.x<center.x && pupil.y<center.y){
            Imgproc.rectangle(inputMat, new Point(0, 0), new Point(coordinateX0, coordinateY0), new Scalar(0, 0, 255, 255));
            return 2;
        }
        if (pupil.x<center.x && pupil.y>center.y){
            Imgproc.rectangle(inputMat, new Point(0, coordinateY0), new Point(coordinateX0, inputMat.rows()), new Scalar(0, 0, 255, 255));
            return 3;
        }
        if (pupil.x>center.x && pupil.y>center.y){
            Imgproc.rectangle(inputMat, new Point(coordinateX0, coordinateY0), new Point(inputMat.cols(), inputMat.rows()), new Scalar(0, 0, 255, 255));
            return 4;
        }
        return -1;
    }

    //находит все распознанные на изображении лица
    //возможны случаи, когда лиц не будет вообще или их будет больше одного
    //причины-более одного человека перед камерой, отсутствие людей перед камерой,
    //слишком близкое или слишком далекое расположение лица(оптимально-20%)
    //освещение, поворот
    private Rect getFace(){
        Rect[] faces=classifier.getFaces(grayMat, (int) (grayMat.rows()*0.2));
        if (faces.length==1) {
            return faces[0];
        }
        else {
            if (faces.length<1){
                listener.onCameraExceptionListener(Exception.EXCEPTION_NO_FACE);
            }
            if (faces.length>1){
                listener.onCameraExceptionListener(Exception.EXCEPTION_MANY_FACES);
            }
        }
        return null;
    }

    //возвращает линию, на которой находится точка, на которую смотрит пользователь
    //находит область глаза, ее центр и зрачок
    //подсчитывается погрешность
    //центр области и зрачок используются для построение линии
    //возможно null исключение, если глаз не будет найден
    //из-за большого количества локальных переменных
    //нецелесообразно выносить рисование в отдельный метод
    private Line getSightDirection(Mat mat, Rect eyeRegion){
        try {
            Rect eye = getEyeRegion(eyeRegion);
            if (error.area()<eye.area()){
                error = eye;
            }
            Point center = getCentralPoint(eye);
            Point pupil = getPupil(eye);

            int quarter=quarter(mat, center, pupil);

            Line sightDirection = twoPointLine(mat, center, pupil, quarter);
            if (debug && eye!=null) {
                Imgproc.rectangle(mat, eye.tl(), eye.br(), new Scalar(0, 255, 0, 255));
                Imgproc.circle(mat, center, 2, new Scalar(255, 0, 0, 255), 2);
                Imgproc.circle(mat, pupil, 2, new Scalar(255, 255, 255, 255), 2);
                Imgproc.line(mat, sightDirection.getFrom(), sightDirection.getTo(), new Scalar(128, 128, 128, 255));
            }
            return sightDirection;
        }
        catch(NullPointerException e){
            e.printStackTrace();
        }
        return null;
    }

    //находит центр области глаза
    //c=(x+x1)/2; (y+y1)/2
    private Point getCentralPoint(Rect eye){
        double centerX = eye.tl().x + eye.width / 2;
        double centerY = eye.tl().y + eye.height / 2;
        return new Point(centerX, centerY);
    }

    //находит зрачок
    //зрачок-самая темная точка на изображении глаза
    //возвращает точку с координатами, пересчитанными в абсолютные
    private Point getPupil(Rect eye) {
        Core.MinMaxLocResult pupil = Core.minMaxLoc(grayMat.submat(eye));
        return new Point(pupil.minLoc.x+eye.x, pupil.minLoc.y+eye.y);
    }

    //создает линию по двум точкам
    //первая-центр области глаза
    //для второй
    //рассчитывает угол наклона: k=dy/dx
    //сравнивает положение зрачка и центра области глаза
    //в зависимости от этого берет либо минимальное, либо максимальное значение х-координаты
    //пересчитывает значение y: y=kx+b
    //возвращает линию, соединяющую эти две точки
    private Line twoPointLine(Mat mat, Point center, Point pupil, int quarter){
        double k = (center.y - pupil.y) / (center.x - pupil.x);
        Point to=new Point(center.x, mat.rows());
        switch(quarter){
            case 1:{
                to=min(new Point(mat.cols(), k*mat.cols()+pupil.y), new Point(-pupil.y/k, 0), center);
                break;
            }
            case 2:{
                to=min(new Point(mat.cols(), k*mat.cols()+pupil.y), new Point(mat.rows()-pupil.y/k, mat.rows()), center);
                break;
            }
            case 3:{
                to=min(new Point(0, pupil.y), new Point(mat.rows()-pupil.y/k, mat.rows()), center);
                break;
            }
            case 4:{
                to=min(new Point(0, pupil.y), new Point(-pupil.y/k, 0), center);
                break;
            }
        }
        //if (pupil.x<center.x){
        //    to=new Point(0, 0);
        //}
        //else{
        //    to=new Point(mat.cols(), mat.rows());
        //}
        //to.y=-k*(pupil.x-to.x)+pupil.y;
        return new Line(center, to);
    }

    private Point min(Point point1, Point point2, Point center){
        if (Math.sqrt((center.x-point1.x)*(center.x-point1.x)+(center.y-point1.y)*(center.y-point1.y))<
                Math.sqrt((center.x-point2.x)*(center.x-point2.x)+(center.y-point2.y)*(center.y-point2.y))){
            return point1;
        }
        else{
            return point2;
        }
    }

    //находит область глаза(область, в которой движется зрачок)
    //с помощью нейросетей получает массив найденных "глаз"
    //исходная область выделяется так, что больше одного настоящего глаза в ней быть не может
    //возможен случай, когда глаз не будет найден вообще
    //причины-освещение, угол поворота лица, ошибка нейросети
    //если глаз найден верно, пересчитывает координаты области с учетом исходной области(получает абсолютные координаты)
    //возвращает новый прямоугольник с абсолютными координатами и экспериментально подогнанным размером
    private Rect getEyeRegion(Rect eyeRegion){
        Rect[] eyes = classifier.getEyes(grayMat, eyeRegion);
        if (eyes.length==1){
            Rect eye = eyes[0];
            eye.x = eyeRegion.x + eye.x;
            eye.y = eyeRegion.y + eye.y;
            return new Rect((int) eye.tl().x, (int) (eye.tl().y + eye.height * 0.4), eye.width, (int) (eye.height * 0.6));
        }
        else{
            if (eyes.length<1){
                listener.onCameraExceptionListener(Exception.EXCEPTION_NO_EYE);
            }
            if (eyes.length>1){
                listener.onCameraExceptionListener(Exception.EXCEPTION_MANY_EYES);
            }
        }
        return null;
    }

    //проверка наличия камеры вообще
    private boolean hasCamera(){
        return Camera.getNumberOfCameras() > 0;
    }

    //проверка наличия фронтальной камеры
    private boolean hasFrontCamera(){
        return parent.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
    }

    //подготовка экрана к съемке
    private void applyScreenSettings(){
        parent.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    //загрузка библиотеки open_cv
    private boolean libReady(){
        return OpenCVLoader.initDebug();
    }
}
