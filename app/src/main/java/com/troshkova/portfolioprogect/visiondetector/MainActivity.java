package com.troshkova.portfolioprogect.visiondetector;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import static com.troshkova.portfolioprogect.visiondetector.SmartCamera.Exception.EXCEPTION_MANY_FACES;
import static com.troshkova.portfolioprogect.visiondetector.SmartCamera.Exception.EXCEPTION_NO_FACE;

public class MainActivity extends AppCompatActivity implements SmartCamera.OnCameraExceptionListener, SmartCamera.OnEyeDirectionListener {

    private SmartCamera camera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        camera=(SmartCamera)findViewById(R.id.camera);
        camera.setDebugMode(true);
        camera.setOnCameraExceptionListener(this);
        camera.setOnDetectSightListener(this);
        camera.startCamera();
    }



    @Override
    public void onDetectSight(Point sight, Rect region) {
        //экспериментально выяснила, что координаты на камере во весь экран без погрешностей.
        //учесть погрешность при изменении размера и положения камеры
    }

    @Override
    public void onCameraExceptionListener(int exception) {

    }
}