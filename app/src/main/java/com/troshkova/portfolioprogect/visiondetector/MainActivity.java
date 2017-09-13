package com.troshkova.portfolioprogect.visiondetector;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener {

    private CameraBridgeViewBase camera;
    private CascadeClassifier classifier;
    private Mat grayScaleImage;
    private int faceSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        applyScreenSettings();
        camera = (JavaCameraView)findViewById(R.id.camera);
        camera.setCvCameraViewListener(this);
        if (libReady()){
            initClassifier();
        }
        else{
            Toast.makeText(this, getString(R.string.lib_exception), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void applyScreenSettings(){
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private boolean libReady(){
        return OpenCVLoader.initDebug();
    }

    private void initClassifier(){
        String path=rewriteDataSource();
        if (path!=null){
            classifier = new CascadeClassifier(path);
            camera.enableView();
        }
        else{
            finish();
        }
    }

    private String rewriteDataSource(){
        try {
            InputStream input = getResources().openRawResource(R.raw.lbpcascade_frontalface);
            //TODO:string names
            File inputFile = getDir("cascade", Context.MODE_PRIVATE);
            File outputFile = new File(inputFile, "lbpcascade_frontalface.xml");
            FileOutputStream output = new FileOutputStream(outputFile);
            byte[] buffer = new byte[4096];
            int bytesRead = 0;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
            input.close();
            output.close();
            return outputFile.getAbsolutePath();
        }
        catch(FileNotFoundException e){
            Toast.makeText(this, getString(R.string.file_not_found), Toast.LENGTH_SHORT).show();
        }
        catch(IOException e){
            Toast.makeText(this, getString(R.string.io_exception), Toast.LENGTH_SHORT).show();
        }
        return null;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        grayScaleImage = new Mat(height, width, CvType.CV_8UC4);
        //TODO:0.2
        faceSize = (int) (height * 0.2);
    }

    @Override
    public void onCameraViewStopped() {}

    @Override
    public Mat onCameraFrame(Mat inputFrame) {
        Imgproc.cvtColor(inputFrame, grayScaleImage, Imgproc.COLOR_RGBA2RGB);
        //TODO:params
        MatOfRect faces = new MatOfRect();
        if (classifier != null) {
            classifier.detectMultiScale(grayScaleImage, faces, 1.1, 2, 2, new Size(faceSize, faceSize), new Size());
        }
        Rect[] facesArray = faces.toArray();
        for (Rect face:facesArray)
            Imgproc.rectangle(inputFrame, face.tl(), face.br(), new Scalar(0.0, 255.0, 0.0, 255.0), 3);

        return inputFrame;
    }
}
