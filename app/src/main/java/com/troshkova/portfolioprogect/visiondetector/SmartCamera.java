package com.troshkova.portfolioprogect.visiondetector;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.util.AttributeSet;
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

public class SmartCamera extends JavaCameraView implements CameraBridgeViewBase.CvCameraViewListener{

    private FaceClassifier faceClassifier;
    private Mat grayScaleImage;
    private int faceSize;
    private Activity parent;
    private OnCameraExceptionListener listener;

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

    @Override
    public Mat onCameraFrame(Mat inputFrame) {
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

    private void drawFace(Mat inputFrame, Rect face){
        Imgproc.rectangle(inputFrame, face.tl(), face.br(), new Scalar(0.0, 255.0, 0.0, 255.0), 3);
        Rect right = new Rect(face.x + face.width / 16, (int) (face.y + (face.height / 4.5)),
                (face.width - 2 * face.width / 16) / 2, (int) (face.height / 3.0));
        Rect left = new Rect(face.x + face.width / 16 + (face.width - 2 * face.width / 16) / 2,
                (int) (face.y + (face.height / 4.5)), (face.width - 2 * face.width / 16) / 2, (int) (face.height / 3.0));
        Imgproc.rectangle(inputFrame, left.tl(), left.br(),
                new Scalar(0, 255, 0, 255), 2);
        Imgproc.rectangle(inputFrame, right.tl(), right.br(),
                new Scalar(0, 255, 0, 255), 2);
        drawTemplate(inputFrame, right);
        drawTemplate(inputFrame, left);
    }

    private void drawTemplate(Mat inputMat, Rect area) {
        Point template = new Point();
        Rect[] eyesArray = faceClassifier.getEyes(grayScaleImage, area);

        for (Rect eye:eyesArray) {
            eye.x = area.x + eye.x;
            eye.y = area.y + eye.y;
            Rect eyeRectangle = new Rect((int) eye.tl().x, (int) (eye.tl().y + eye.height * 0.4), (int) eye.width, (int) (eye.height * 0.6));

            Core.MinMaxLocResult mmG = Core.minMaxLoc(grayScaleImage.submat(eyeRectangle));

            Imgproc.circle(inputMat.submat(eyeRectangle), mmG.minLoc, 2, new Scalar(255, 0, 0, 255), 2);

            template.x = mmG.minLoc.x + eyeRectangle.x;
            template.y = mmG.minLoc.y + eyeRectangle.y;

            Rect eyeTemplate = new Rect((int) template.x - 24 / 2, (int) template.y - 24 / 2, 24, 24);
            Imgproc.rectangle(inputMat, eyeTemplate.tl(), eyeTemplate.br(), new Scalar(0, 255, 0, 255), 2);
        }
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
