package ru.eyetracker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

public class EyeActivity extends AppCompatActivity implements SmartCamera.OnCameraExceptionListener, SmartCamera.OnEyeDirectionListener {

    private SmartCamera mCamera;

    private ImageView mUp;
    private ImageView mDown;
    private ImageView mLeft;
    private ImageView mRight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eye);
        mCamera = (SmartCamera) findViewById(R.id.camera);
        mDown = (ImageView) findViewById(R.id.image_down);
        mUp = (ImageView) findViewById(R.id.image_up);
        mLeft = (ImageView) findViewById(R.id.image_left);
        mRight = (ImageView) findViewById(R.id.image_right);
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mCamera = (SmartCamera) findViewById(R.id.camera);
            mCamera.setDebugMode(true);
            mCamera.setOnCameraExceptionListener(this);
            mCamera.setOnDetectSightListener(this);
            mCamera.startCamera();
        } else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCamera != null) {
            mCamera.pause();
        }
    }

    @Override
    public void onDetectSight(Direction direction) {
    }

    @Override
    public void onCameraExceptionListener(int exception) {

    }

    public void onBack(View view) {
    }

    private int getWidthScreen() {
        return getResources().getDisplayMetrics().widthPixels;
    }

    private int getHeightScreen() {
        return getResources().getDisplayMetrics().heightPixels;
    }

    //TODO: timer for taking pictures
    public void click(View view){
        mCamera.takePicture();
    }
}