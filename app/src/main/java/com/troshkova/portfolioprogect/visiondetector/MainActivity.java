package com.troshkova.portfolioprogect.visiondetector;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;

import org.opencv.core.Point;
import org.opencv.core.Rect;

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

    //отражение по горизонтали
    public void mirror(View view){
        camera.setMirror(((CheckBox) view).isChecked());
    }

    //поворот на -90 градусов
    public void rotateM90(View view){
        camera.setRotation(Rotation.ROTATION_270);
    }

    public void stop(View view){
        camera.stopCamera();
        //единственный способ ее остановить без ущерба для родительского класса-сделать невидимой
        camera.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onCameraExceptionListener(@SmartCamera.Exception int exception) {
        Log.e("LAG", exception+"");
    }
}