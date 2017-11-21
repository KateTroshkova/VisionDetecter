package com.troshkova.portfolioprogect.visiondetector;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import static com.troshkova.portfolioprogect.visiondetector.SmartCamera.Exception.EXCEPTION_MANY_EYES;
import static com.troshkova.portfolioprogect.visiondetector.SmartCamera.Exception.EXCEPTION_MANY_FACES;
import static com.troshkova.portfolioprogect.visiondetector.SmartCamera.Exception.EXCEPTION_NO_EYE;
import static com.troshkova.portfolioprogect.visiondetector.SmartCamera.Exception.EXCEPTION_NO_FACE;

public class MainActivity extends AppCompatActivity implements SmartCamera.OnCameraExceptionListener, SmartCamera.OnEyeDirectionListener {

    private SmartCamera camera;
    //private FaceClassifier mClassifier;
    String path, eyePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA},
                1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    camera=(SmartCamera)findViewById(R.id.camera);
                    camera.setDebugMode(true);
                    camera.setOnCameraExceptionListener(this);
                    camera.setOnDetectSightListener(this);
                    camera.startCamera();
                } else {
                }
                return;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
       // if (camera!=null) {
       //     camera.pause();
       // }
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