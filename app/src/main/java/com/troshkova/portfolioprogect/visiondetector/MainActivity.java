package com.troshkova.portfolioprogect.visiondetector;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.troshkova.portfolioprogect.visiondetector.exception.Exception;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;

public class MainActivity extends AppCompatActivity implements SmartCamera.OnCameraExceptionListener, SmartCamera.OnDetectSightListener {

    private SmartCamera camera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        camera=(SmartCamera)findViewById(R.id.camera);
    }

    @Override
    public void onCameraExceptionListener(Exception exception) {
        Log.e("LOG", exception.toString());
    }

    @Override
    public void onDetectSight(Point sight, Rect region) {
        //экспериментально выяснила, что координаты на камере во весь экран без погрешностей.
        //учесть погрешность при изменении размера и положения камеры
    }

    //отражение по горизонтали
    public void mirror(View view){
        camera.mirror();
    }

    //поворот на -90 градусов
    public void rotateM90(View view){
        camera.addRotation();
    }
}