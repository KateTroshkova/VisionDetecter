package com.troshkova.portfolioprogect.visiondetector;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.troshkova.portfolioprogect.visiondetector.exception.Exception;

import org.opencv.core.Point;
import org.opencv.core.Rect;

public class MainActivity extends AppCompatActivity implements SmartCamera.OnCameraExceptionListener, SmartCamera.OnDetectSightListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
}