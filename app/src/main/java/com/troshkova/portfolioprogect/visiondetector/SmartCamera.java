package com.troshkova.portfolioprogect.visiondetector;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.support.annotation.IntDef;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.lang.annotation.Retention;
import java.util.ArrayList;

import static android.view.Surface.ROTATION_0;
import static android.view.Surface.ROTATION_180;
import static android.view.Surface.ROTATION_270;
import static android.view.Surface.ROTATION_90;
import static com.troshkova.portfolioprogect.visiondetector.SmartCamera.Exception.EXCEPTION_LIB_NOT_READY;
import static com.troshkova.portfolioprogect.visiondetector.SmartCamera.Exception.EXCEPTION_LITTLE_LIGHT;
import static com.troshkova.portfolioprogect.visiondetector.SmartCamera.Exception.EXCEPTION_MANY_EYES;
import static com.troshkova.portfolioprogect.visiondetector.SmartCamera.Exception.EXCEPTION_MANY_FACES;
import static com.troshkova.portfolioprogect.visiondetector.SmartCamera.Exception.EXCEPTION_NO_CAMERA;
import static com.troshkova.portfolioprogect.visiondetector.SmartCamera.Exception.EXCEPTION_NO_EYE;
import static com.troshkova.portfolioprogect.visiondetector.SmartCamera.Exception.EXCEPTION_NO_FACE;
import static com.troshkova.portfolioprogect.visiondetector.SmartCamera.Exception.EXCEPTION_NO_FRONT_CAMERA;
import static com.troshkova.portfolioprogect.visiondetector.SmartCamera.Exception.EXCEPTION_STRANGE_ORIENTATION;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Камера, обрабатывающая изображение. находит глаза и вычисляет направление взгляда
 * родительский класс из open_cv CameraBridgeViewBase изменен!
 */
public class SmartCamera extends JavaCameraView implements CameraBridgeViewBase.CvCameraViewListener {

    //классификатор, распознающий лицо и глаза
    private FaceClassifier mClassifier;
    //черно-белая матрица
    //матрица такого типа необходима для поиска лиц и части преобразований матриц
    private Mat mGrayMat;
    //интерфейс, который должна расширить активность, чтобы обрабатывать возникающие в процессе работы исключения
    private OnCameraExceptionListener mExceptionListener = null;
    //интерфейс, через который активность получает точку, на которую смотрит пользователь
    private OnEyeDirectionListener mSightListener = null;
    //листы для устранения "скачков" в распознавании
    private ArrayList<Point> mOptions;
    private ArrayList<Point> mCenters;
    //если true будет происходить построение дополнительных линий
    private boolean debug = true;
    //если true будет проверять освещенность комнаты
    private boolean light=false;
    private boolean mIsStarting = false;
    //изображение, которое будет возвращено при уничтожении камеры
    private Mat mCurrentMat;

    //количество дополнительных поворотов(пользовательские настройки)
    @Rotation
    private int mRotate = ROTATION_0;
    //дополнительное отражение(пользовательские настройки)
    private boolean mMirror = false;

    public interface OnCameraExceptionListener {
        void onCameraExceptionListener(@Exception int exception);
    }

    public interface OnEyeDirectionListener {
        void onDetectSight(Point sight, Rect region);
    }

    public SmartCamera(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * проверка наличия камер
     * загрузка/подготовка open_cv
     */
    public void startCamera() {
        mOptions=new ArrayList<>();
        mCenters=new ArrayList<>();
        if (!hasCamera()) {
            callException(EXCEPTION_NO_CAMERA);
            return;
        } else if (!hasFrontCamera()) {
            callException(EXCEPTION_NO_FRONT_CAMERA);
            return;
        } else if (!libReady()) {
            callException(EXCEPTION_LIB_NOT_READY);
            return;
        } else if (mIsStarting) return;
        setCvCameraViewListener(this);
        mClassifier = new FaceClassifier(getContext(), mExceptionListener);
        enableFpsMeter();
        enableView();
    }

    public Bitmap stopCamera() {
        if (!mIsStarting) return null;
        mIsStarting = false;
        setCvCameraViewListener((CvCameraViewListener) null);
        mGrayMat.release();
        disableFpsMeter();
        disableView();
        disconnectCamera();
        releaseCamera();
        Bitmap lastFrame = Bitmap.createBitmap(mCurrentMat.cols(), mCurrentMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mCurrentMat, lastFrame);
        return lastFrame;
    }

    public void setOnDetectSightListener(OnEyeDirectionListener listener) {
        mSightListener = listener;
    }

    public void setOnCameraExceptionListener(OnCameraExceptionListener listener) {
        mExceptionListener = listener;
    }

    //внешний интерфейс
    //нужно ли отрисовывать
    public void setDebugMode(boolean isDebugEnable) {
        debug = isDebugEnable;
    }

    public void setRotation(@Rotation int rotate) {
        mRotate = rotate;
    }

    public void setMirror(boolean isMirror) {
        mMirror = isMirror;
    }

    public void checkLight(boolean light){
        this.light=light;
    }

    public boolean getDebugMode(){
        return debug;
    }
    //не getRotation, так как метод с таким названием уже есть в классе View
    public int getCurrentRotation(){
        return mRotate;
    }

    public boolean getCurrentMirror(){
        return mMirror;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
    }

    @Override
    public void onCameraViewStopped() {
    }

    /**
     * обновление экрана
     * исправляется ориентация
     * по параметрам входного изображения пересоздается черно-белая матрица
     * ищется точка, на которую смотрит пользователь
     * inputFrame(передается по ссылке) - матрица с прорисованными(если нужно) линиями
     * возвращается для отрисовки на экране
     */
    @Override
    public Mat onCameraFrame(Mat inputFrame) {
        fixOrientation(inputFrame);
        mGrayMat = new Mat(inputFrame.rows(), inputFrame.cols(), CvType.CV_8UC4);
        Imgproc.cvtColor(inputFrame, mGrayMat, Imgproc.COLOR_RGBA2GRAY);
        if (light){
            lightEnough();
        }
        userAttention(inputFrame);
        mGrayMat.release();
        mCurrentMat=inputFrame;
        return inputFrame;
    }

    //отражение по горизонтали
    private void setMirror(Mat mat) {
        Core.flip(mat, mat, 0);
        Core.rotate(mat, mat, Core.ROTATE_180);
    }

    //поворот на +90 градусов
    //изменение высоты и ширины родительского bitmap
    //необходимо, так как размер меняется
    //и размер изображения в вертикальном и горизонтальном состоянии не равны
    private void rotateP90(Mat mat) {
        Core.transpose(mat, mat);
        setWidth(mat.cols());
        setHeight(mat.rows());
    }

    //поворот на -90 градусов
    private void rotateM90(Mat mat) {
        rotateP90(mat);
        Core.flip(mat, mat, 0);
    }

    //поворот на 180 градусов
    private void rotate180(Mat mat) {
        Core.rotate(mat, mat, Core.ROTATE_180);
    }

    /**
     * исправляет ориентацию для большинства устройств
     * выяснено экспериментально
     * дополнительно применяет пользовательские изменения
     */
    private void fixOrientation(Mat mat) {
        setMirror(mat);
        WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        int rotation = windowManager.getDefaultDisplay().getRotation();
        switch (rotation) {
            case ROTATION_0:
                rotateP90(mat);
                setMirror(mat);
                break;
            case ROTATION_90:
                break;
            case ROTATION_180:
                rotateM90(mat);
                break;
            case ROTATION_270:
                rotate180(mat);
                break;
            default:
                callException(EXCEPTION_STRANGE_ORIENTATION);
                break;
        }
        additionalTurn(mat);
        if (mMirror) setMirror(mat);
    }

    private void additionalTurn(Mat mat){
        for(int i=0; i<mRotate/90; i++){
            rotateM90(mat);
        }
    }

    /**
     * на основании расположений зрачков относительно центра глаза
     * ищет точку, на которую смотрит пользователь
     * погрешность-четверть экрана
     * отрисовку нецелесообразно выносить в отдельный метод
     */
    private void userAttention(Mat inputFrame) {
        Face face=getFace();
        if (face != null) {
            Eye right = detectEye(inputFrame, face.getRightEyeRegion());
            Eye left = detectEye(inputFrame, face.getLeftEyeRegion());
            Point attention=averagePupil(inputFrame, right, left);
            Point zero=averageCenter(inputFrame, right, left);
            Rect error;
            error=quarter(inputFrame, zero, attention);
            Imgproc.rectangle(inputFrame, error.tl(), error.br(), new Scalar(0, 0, 255, 255), 10);
            Imgproc.circle(inputFrame, attention, 5, new Scalar(0, 0, 255, 255), 5);
            Imgproc.circle(inputFrame, zero, 3, new Scalar(128, 128, 255, 255), 3);
            if (mOptions.size()>=3){
                attention=average(mOptions);
                zero=average(mCenters);
                error=quarter(inputFrame, zero, attention);
                attention=new Point(error.tl().x+error.width/2, error.tl().y+error.height/2);
                Eye result=new Eye(attention, null, error);
                result.draw(inputFrame);
                onEyeEvent(attention, error);
                mOptions.clear();
                mCenters.clear();
            }
            else{
                mOptions.add(attention);
                mCenters.add(zero);
            }
            if (debug) {
                Eye intermediateValue=new Eye(attention, zero, null);
                intermediateValue.draw(inputFrame);
                face.draw(inputFrame);
            }
        }
    }

    //находит среднее значение между центрами правого и левого глаз
    private Point averageCenter(Mat mat, Eye right, Eye left){
        Point center=new Point(mat.cols()/2, mat.rows()/2);
        if (right!=null && left!=null) {
            ArrayList<Point> centers = new ArrayList<>();
            centers.add(right.getCenter());
            centers.add(left.getCenter());
            center = average(centers);
        }
        else{
            if (right!=null){
                center=right.getCenter();
            }
            else{
                if (left!=null){
                    center=left.getCenter();
                }
            }
        }
        return center;
    }

    //находит среднее значение между положением зрачков в правом и левом глазу
    private Point averagePupil(Mat mat, Eye right, Eye left){
        Point pupil=new Point(mat.cols()/2, mat.rows()/2);
        if (right!=null && left!=null) {
            ArrayList<Point> pupils = new ArrayList<>();
            pupils.add(right.getPupil());
            pupils.add(left.getPupil());
            pupil = average(pupils);
        }
        else{
            if (right!=null){
                pupil=right.getCenter();
            }
            else{
                if (left!=null){
                    pupil=left.getCenter();
                }
            }
        }
        return pupil;
    }

    //среднее значение по массиву точек
    private Point average(ArrayList<Point> points){
        float x=0;
        float y=0;
        for(Point point:points){
            x+=point.x;
            y+=point.y;
        }
        return new Point(x/points.size(), y/points.size());
    }

    /**
     * определяет область, на которую смотрит пользователь
     * сравнивается полоожение зрачка и центра глаза
     * центр глаза принимается за начало координат
     * если зрачок правее и выше-первая четверть
     * если зрачок левее и выше-вторая
     * если зрачок левее и ниже-третья
     * если зрачок правее и ниже-четвертая
     * при любой ошибке возвращается экран цеиком
     */
    private Rect quarter(Mat mat, Point center, Point pupil) {
        Rect result = new Rect(new Point(0, 0), new Point(mat.cols(), mat.rows()));
        if (pupil.x > center.x && pupil.y < center.y) {
            result = new Rect(new Point(mat.cols()/2, 0), new Point(mat.cols(), mat.rows()/2));
        }
        if (pupil.x < center.x && pupil.y < center.y) {
            result = new Rect(new Point(0, 0), new Point(mat.cols()/2, mat.rows()/2));
        }
        if (pupil.x < center.x && pupil.y > center.y) {
            result = new Rect(new Point(0, mat.rows()/2), new Point(mat.cols()/2, mat.rows()));
        }
        if (pupil.x > center.x && pupil.y > center.y) {
            result = new Rect(new Point(mat.cols()/2, mat.rows()/2), new Point(mat.cols(), mat.rows()));
        }
        return result;
    }

    /**
     * находит все распознанные на изображении лица
     * возможны случаи, когда лиц не будет вообще или их будет больше одного
     * причины-более одного человека перед камерой, отсутствие людей перед камерой,
     * слишком близкое или слишком далекое расположение лица(оптимально-20%)
     * освещение, поворот
     */
    private Face getFace() {
        Rect[] faces = mClassifier.getFaces(mGrayMat, (int) (mGrayMat.rows() * 0.2));
        if (faces.length == 1) {
            return new Face(faces[0]);
        } else {
            if (faces.length < 1) callException(EXCEPTION_NO_FACE);
            else if (faces.length > 1) callException(EXCEPTION_MANY_FACES);
        }
        return null;
    }

    /**
     * находит область глаза (область, где движется зрачок)
     * на основе этой области создается глаз с рассчитаными зрачком и центром глаза
     * слишком маленькие области отсеиваются - это заведомо ошибка распознавания
     */
    private Eye detectEye(Mat mat, Rect eyeRegion){
        try {
            Rect region=getEyeRegion(eyeRegion);
            if (tooSmall(region, eyeRegion)){
                return null;
            }
            Eye result=new Eye(mGrayMat, region);
            if (debug) {
                result.draw(mat);
            }
            return result;
        }
        catch(NullPointerException e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * находит область глаза(область, в которой движется зрачок)
     * с помощью нейросетей получает массив найденных "глаз"
     * исходная область выделяется так, что больше одного настоящего глаза в ней быть не может
     * возможен случай, когда глаз не будет найден вообще
     * причины-освещение, угол поворота лица, ошибка нейросети
     * если глаз найден верно, пересчитывает координаты области с учетом исходной области(получает абсолютные координаты)
     * возвращает новый прямоугольник с абсолютными координатами и экспериментально подогнанным размером
     */
    private Rect getEyeRegion(Rect eyeRegion) {
        Rect[] eyes = mClassifier.getEyes(mGrayMat, eyeRegion);
        if (eyes.length == 1) {
            Rect eye = eyes[0];
            eye.x = eyeRegion.x + eye.x;
            eye.y = eyeRegion.y + eye.y-eye.height/4;
            return new Rect((int) eye.tl().x, (int) (eye.tl().y + eye.height * 0.4), eye.width, (int) (eye.height * 0.6));
        } else {
            if (eyes.length < 1) callException(EXCEPTION_NO_EYE);
            else if (eyes.length > 1) callException(EXCEPTION_MANY_EYES);
        }
        return null;
    }

    //проверка наличия камеры вообще
    private boolean hasCamera() {
        return Camera.getNumberOfCameras() > 0;
    }

    //проверка наличия фронтальной камеры
    private boolean hasFrontCamera() {
        return getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
    }

    //загрузка библиотеки open_cv
    private boolean libReady() {
        return OpenCVLoader.initDebug();
    }

    //глаз должен занимать как минимум 1/16 от области, в которой его ищут
    private boolean tooSmall(Rect eye, Rect eyeRegion){
        return eye.area()<eyeRegion.area()/16;
    }

    /**
     * проверяет, достаточно ли светло для распознавания
     * требует ресурсов, поэтому на слабых устройствах лучше не проверять
     */
    private void lightEnough(){
        int color=0;
        for(int i=0; i<30; i++){
            for(int j=0; j<30; j++){
                color+=mGrayMat.get(i, j)[0];
            }
        }
        if(color/900>64){
            callException(EXCEPTION_LITTLE_LIGHT);
        }
    }

    private void callException(@Exception int exception) {
        if (mExceptionListener != null) mExceptionListener.onCameraExceptionListener(exception);
    }
    private void onEyeEvent(Point sight, Rect rect){
        if(mSightListener!=null) mSightListener.onDetectSight(sight,rect);
    }

    @Retention(SOURCE)
    @IntDef({
            EXCEPTION_NO_CAMERA,
            EXCEPTION_NO_FRONT_CAMERA,
            EXCEPTION_LIB_NOT_READY,
            Exception.EXCEPTION_NO_FILE,
            Exception.EXCEPTION_IO,
            EXCEPTION_NO_FACE,
            EXCEPTION_MANY_FACES,
            EXCEPTION_STRANGE_ORIENTATION,
            EXCEPTION_NO_EYE,
            EXCEPTION_MANY_EYES,
            EXCEPTION_LITTLE_LIGHT
    })
    public @interface Exception {
        int EXCEPTION_NO_CAMERA = 0;
        int EXCEPTION_NO_FRONT_CAMERA = 1;
        int EXCEPTION_LIB_NOT_READY = 2;
        int EXCEPTION_NO_FILE = 3;
        int EXCEPTION_IO = 4;
        int EXCEPTION_NO_FACE = 5;
        int EXCEPTION_MANY_FACES = 6;
        int EXCEPTION_STRANGE_ORIENTATION = 7;
        int EXCEPTION_NO_EYE = 8;
        int EXCEPTION_MANY_EYES = 9;
        int EXCEPTION_LITTLE_LIGHT=10;
    }
}
