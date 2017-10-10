package com.troshkova.portfolioprogect.visiondetector;

import android.app.Activity;
import android.content.Context;
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

import java.util.ArrayList;

//TODO стабильная область глаз
public class SmartCamera extends JavaCameraView implements CameraBridgeViewBase.CvCameraViewListener{

    //классификатор, распознающий лицо и глаза
    private FaceClassifier faceClassifier;
    //черно-белая матрица
    //матрица такого типа необходима для поиска лиц и части преобразований матриц
    private Mat grayScaleImage;
    //размер пространства, которое предположительно занимает лицо на экране(в пикселях)
    //будет использован для поиска лица
    private int faceSize;
    //активность, на которую прикрепляется камера
    //нужна для получения информации о наличии и типе камеры на устройстве
    private Activity parent;
    //интерфейс, который должна расширить активность, чтобы обрабатывать возникающие в процессе работы исключения
    private OnCameraExceptionListener listener;

    private int rotate=0;
    private boolean mirror=false;

    public void addRotation(){
        rotate++;
    }

    public void changeMirror(){
        mirror=!mirror;
    }

    public interface OnCameraExceptionListener{
        void onCameraExceptionListener(Exception exception);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        disableView();
    }

    public SmartCamera(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.parent=(Activity)getContext();
        this.listener=(OnCameraExceptionListener)getContext();
        if (hasCamera()) {
            if (hasFrontCamera()) {
                applyScreenSettings();
                setCvCameraViewListener(this);
                if (libReady()) {
                    faceClassifier=new FaceClassifier(context, listener);
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
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        grayScaleImage = new Mat(height, width, CvType.CV_8UC4);
        faceSize = (int) (height * 0.2);
    }

    @Override
    public void onCameraViewStopped() {
        grayScaleImage.release();
    }

    //TODO:fix hand rotation
    @Override
    public Mat onCameraFrame(Mat inputFrame) {
        fixOrientation(inputFrame);
        switch(rotate){
            case 1:{
                rotateP90(inputFrame);
                break;
            }
            case 2:{
                rotate180(inputFrame);
                break;
            }
            case 3:{
                rotateM90(inputFrame);
            }
            default:{
                rotate=0;
            }
        }
        if (mirror){
            mirror(inputFrame);
        }
        Imgproc.cvtColor(inputFrame, grayScaleImage, Imgproc.COLOR_RGBA2GRAY);
        Rect[] faces=null;
        if (faceClassifier.isExist()) {
            faces=faceClassifier.getFaces(grayScaleImage, faceSize);
        }
        if (faces!=null) {
            if (faces.length < 1) {
                listener.onCameraExceptionListener(Exception.EXCEPTION_NO_FACE);
            }
            if (faces.length > 1) {
                listener.onCameraExceptionListener(Exception.EXCEPTION_MANY_FACES);
            }
            for (Rect face : faces) {
                drawFace(inputFrame, face);
            }
        }
        return inputFrame;
    }

    //отражение по горизонтали
    private void mirror(Mat mat){
        Core.flip(mat, mat, 0);
        Core.rotate(mat, mat, Core.ROTATE_180);
    }

    //поворот на +90 градусов
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

    private void fixOrientation(Mat mat){
        mirror(mat);
        WindowManager windowManager= (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        int rotation = windowManager.getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_0: {
                rotateP90(mat);
                break;
            }
            case Surface.ROTATION_90:
                break;
            case Surface.ROTATION_180: {
                rotateM90(mat);
                break;
            }
            case Surface.ROTATION_270:
                rotate180(mat);
                break;
        }
    }

    private void drawFace(Mat inputFrame, Rect face){
        Rect right = new Rect(face.x + face.width / 16, (int) (face.y + (face.height / 4.5)),
                (face.width - 2 * face.width / 16) / 2, (int) (face.height / 3.0));
        Rect left = new Rect(face.x + face.width / 16 + (face.width - 2 * face.width / 16) / 2,
                (int) (face.y + (face.height / 4.5)), (face.width - 2 * face.width / 16) / 2, (int) (face.height / 3.0));
        Line rightLine = drawTemplate(inputFrame, right);
        Line leftLine = drawTemplate(inputFrame, left);
        if (rightLine!=null && leftLine!=null) {
            Point bigPoint = rightLine.intersect(leftLine);
            Imgproc.circle(inputFrame, bigPoint, 10, new Scalar(255, 255, 255, 255));
        }
    }

    private Line drawTemplate(Mat inputMat, Rect area) {
        Point template = new Point();
        Rect[] eyesArray = faceClassifier.getEyes(grayScaleImage, area);
        Line result=null;
        for (Rect eye : eyesArray) {
            eye.x = area.x + eye.x;
            eye.y = area.y + eye.y;
            Rect eyeRectangle = new Rect((int) eye.tl().x, (int) (eye.tl().y + eye.height * 0.4), eye.width, (int) (eye.height * 0.6));

            double x = eye.tl().x + (eye.width) / 2;
            double y = (eye.tl().y + eye.height * 0.4) + (eye.height * 0.6) / 2;

            Core.MinMaxLocResult mmG = Core.minMaxLoc(grayScaleImage.submat(eyeRectangle));

            template.x = mmG.minLoc.x + eyeRectangle.x;
            template.y = mmG.minLoc.y + eyeRectangle.y;

            double slope = (double) (y - template.y) / (x - template.x);

            Point p = new Point(0, 0);
            Point q = new Point(inputMat.cols(), inputMat.rows());

            p.y = -(template.x - p.x) * slope + template.y;
            q.y = -(x - q.x) * slope + y;

            Imgproc.line(inputMat, p, q, new Scalar(0, 0, 255, 255), 1, 8, 0);
            result=new Line(p, q);
        }
        return result;
    }

    private boolean hasCamera(){
        return Camera.getNumberOfCameras() > 0;
    }

    private boolean hasFrontCamera(){
        return parent.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
    }

    private void applyScreenSettings(){
        parent.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private boolean libReady(){
        return OpenCVLoader.initDebug();
    }
}
