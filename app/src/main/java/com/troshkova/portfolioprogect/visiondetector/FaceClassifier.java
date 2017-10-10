package com.troshkova.portfolioprogect.visiondetector;

import android.content.Context;

import com.troshkova.portfolioprogect.visiondetector.exception.Exception;

import org.opencv.core.Core;
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

public class FaceClassifier {

    private Context context;
    private SmartCamera.OnCameraExceptionListener listener;
    private CascadeClassifier classifier;
    private CascadeClassifier eyeClassifier;

    public FaceClassifier(Context context, SmartCamera.OnCameraExceptionListener listener){
        this.context=context;
        this.listener=listener;
        String path=rewriteDataSource();
        if (path!=null){
            classifier = new CascadeClassifier(path);
        }
        String eyePath=rewriteEyeDataSource();
        if (eyePath!=null) {
            eyeClassifier = new CascadeClassifier(eyePath);
        }
    }

    public boolean isExist(){
        return classifier!=null;
    }

    public Rect[] getFaces(Mat grayScaleImage, int faceSize){
        MatOfRect faces = new MatOfRect();
        classifier.detectMultiScale(grayScaleImage, faces, 1.1, 2, 2, new Size(faceSize, faceSize), new Size());
        return faces.toArray();
    }

    public Rect[] getEyes(Mat grayScaleImage, Rect eyeArea){
        MatOfRect eyes=new MatOfRect();
        eyeClassifier.detectMultiScale(grayScaleImage.submat(eyeArea), eyes, 1.15, 2, Objdetect.CASCADE_FIND_BIGGEST_OBJECT
                        | Objdetect.CASCADE_SCALE_IMAGE, new Size(30, 30), new Size());
        return eyes.toArray();
    }

    private String rewriteDataSource(){
        try {
            InputStream input = context.getResources().openRawResource(R.raw.lbpcascade_frontalface);
            File inputFile = context.getDir("cascade", Context.MODE_PRIVATE);
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
            listener.onCameraExceptionListener(Exception.EXCEPTION_NO_FILE);
        }
        catch(IOException e){
            listener.onCameraExceptionListener(Exception.EXCEPTION_IO);
        }
        return null;
    }

    private String rewriteEyeDataSource(){
        try {
            InputStream einput = context.getResources().openRawResource(R.raw.haarcascade_eye);
            File eyeDir = context.getDir("cascadeER", Context.MODE_PRIVATE);
            File eyeFile = new File(eyeDir, "haarcascade_eye.xml");
            FileOutputStream eoutput = new FileOutputStream(eyeFile);

            byte[] ebuffer = new byte[4096];
            int ebytesRead;
            while ((ebytesRead = einput.read(ebuffer)) != -1) {
                eoutput.write(ebuffer, 0, ebytesRead);
            }
            einput.close();
            eoutput.close();
            return eyeFile.getAbsolutePath();
        } catch(FileNotFoundException e){
            listener.onCameraExceptionListener(Exception.EXCEPTION_NO_FILE);
        } catch(IOException e){
            listener.onCameraExceptionListener(Exception.EXCEPTION_IO);
        }
        return null;
    }
}
