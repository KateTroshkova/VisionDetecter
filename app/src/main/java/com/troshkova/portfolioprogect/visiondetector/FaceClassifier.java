package com.troshkova.portfolioprogect.visiondetector;

import android.content.Context;

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
    private SmartCamera.OnCameraExceptionListener listener;
    private OnClassifierPrepareListener prepareListener;

    private static String eyePath;
    private static String path;

    private CascadeClassifier mFaceClassifier;
    private CascadeClassifier mEyeClassifier;

    public FaceClassifier(Context context, SmartCamera.OnCameraExceptionListener listener, OnClassifierPrepareListener prepareListener){
        this.context=context;
        this.listener=listener;
        this.prepareListener=prepareListener;
    }

    //асинхронная перезапись ресурсов в память устройства
    public void prepare(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                String path=rewriteDataSource(R.raw.lbpcascade_frontalface, "cascade", "frontalface.xml");
                if (path!=null){
                    mFaceClassifier=new CascadeClassifier(path);
                }
                String eyePath=rewriteDataSource(R.raw.haarcascade_eye, "cascadeER", "eye.xml");
                if (eyePath!=null) {
                    mEyeClassifier=new CascadeClassifier(eyePath);
                }
                prepareListener.onClassifierPrepare();
            }
        }).start();
    }

    public interface OnClassifierPrepareListener{
        void onClassifierPrepare();
    }

    //распознавание лиц
    public synchronized Rect[] getFaces(Mat grayScaleImage, int faceSize){
        //CascadeClassifier faceClassifier = new CascadeClassifier(path);
        MatOfRect faces = new MatOfRect();
        mFaceClassifier.detectMultiScale(grayScaleImage, faces, 1.1, 2, 2, new Size(faceSize, faceSize), new Size());
        return faces.toArray();
    }

    //распознавание глаз
    public synchronized Rect[] getEyes(Mat grayScaleImage, Rect eyeArea){
       // CascadeClassifier eyeClassifier = new CascadeClassifier(eyePath);
        MatOfRect eyes=new MatOfRect();
        mEyeClassifier.detectMultiScale(grayScaleImage.submat(eyeArea), eyes, 1.15, 2, Objdetect.CASCADE_FIND_BIGGEST_OBJECT
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
            if (listener!=null) {
                listener.onCameraExceptionListener(SmartCamera.Exception.EXCEPTION_NO_FILE);
            }
        }
        catch(IOException e){
            if (listener!=null) {
                listener.onCameraExceptionListener(SmartCamera.Exception.EXCEPTION_IO);
            }
        }
        return null;
    }
}
