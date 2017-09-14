package com.troshkova.portfolioprogect.visiondetector;

import android.content.Context;

import com.troshkova.portfolioprogect.visiondetector.exception.Exception;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FaceClassifier {

    private Context context;
    private SmartCamera.OnCameraExceptionListener listener;
    private CascadeClassifier classifier;

    public FaceClassifier(Context context, SmartCamera.OnCameraExceptionListener listener){
        this.context=context;
        this.listener=listener;
        String path=rewriteDataSource();
        if (path!=null){
            classifier = new CascadeClassifier(path);
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
}
