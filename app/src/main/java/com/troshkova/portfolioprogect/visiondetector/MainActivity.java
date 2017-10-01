package com.troshkova.portfolioprogect.visiondetector;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.troshkova.portfolioprogect.visiondetector.exception.Exception;

public class MainActivity extends AppCompatActivity implements SmartCamera.OnCameraExceptionListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public void onCameraExceptionListener(Exception exception) {
        Log.e("LOG", exception.toString());
    }
}