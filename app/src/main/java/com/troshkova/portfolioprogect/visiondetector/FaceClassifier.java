package com.troshkova.portfolioprogect.visiondetector;

import android.content.Context;

import com.troshkova.portfolioprogect.visiondetector.exception.Exception;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

//класс для распознавания лица и глаз
//используется встроенный в open_cv классификатор
public class FaceClassifier {

    private Context context;
    //для выброса исключений
    private SmartCamera.OnCameraExceptionListener listener;
    private CascadeClassifier faceClassifier;
    private CascadeClassifier eyeClassifier;

    public FaceClassifier(Context context, SmartCamera.OnCameraExceptionListener listener){
        this.context=context;
        this.listener=listener;
        String path=rewriteDataSource(R.raw.lbpcascade_frontalface, "cascade", "frontalface.xml");
        if (path!=null){
            faceClassifier = new CascadeClassifier(path);
        }
        String eyePath=rewriteDataSource(R.raw.haarcascade_eye, "cascadeER", "eye.xml");
        if (eyePath!=null) {
            eyeClassifier = new CascadeClassifier(eyePath);
        }
    }

    //распознавание лиц
    public Rect[] getFaces(Mat grayScaleImage, int faceSize){
        MatOfRect faces = new MatOfRect();
        faceClassifier.detectMultiScale(grayScaleImage, faces, 1.1, 2, 2, new Size(faceSize, faceSize), new Size());
        return faces.toArray();
    }

    //распознавание глаз
    public Rect[] getEyes(Mat grayScaleImage, Rect eyeArea){
        MatOfRect eyes=new MatOfRect();
        eyeClassifier.detectMultiScale(grayScaleImage.submat(eyeArea), eyes, 1.15, 2, Objdetect.CASCADE_FIND_BIGGEST_OBJECT
                        | Objdetect.CASCADE_SCALE_IMAGE, new Size(30, 30), new Size());
        return eyes.toArray();
    }

    //перезапись файла из ресурсов в отдельный файл, откуда классификатор может считать данные
    private String rewriteDataSource(int id, String direction, String name){
        try {
            InputStream input = context.getResources().openRawResource(id);
            File inputFile = context.getDir(direction, Context.MODE_PRIVATE);
            File outputFile = new File(inputFile, name);
            FileOutputStream output = new FileOutputStream(outputFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
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
