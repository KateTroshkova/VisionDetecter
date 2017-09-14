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
import org.opencv.core.CvType;
import org.opencv.core.Mat;
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

    }

    @Override
    public Mat onCameraFrame(Mat inputFrame) {
        Imgproc.cvtColor(inputFrame, grayScaleImage, Imgproc.COLOR_RGBA2RGB);
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
                Imgproc.rectangle(inputFrame, face.tl(), face.br(), new Scalar(0.0, 255.0, 0.0, 255.0), 3);
            }
        }
        return inputFrame;
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
