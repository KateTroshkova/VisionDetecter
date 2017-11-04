package com.troshkova.portfolioprogect.visiondetector;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import static com.troshkova.portfolioprogect.visiondetector.SmartCamera.Exception.EXCEPTION_MANY_FACES;
import static com.troshkova.portfolioprogect.visiondetector.SmartCamera.Exception.EXCEPTION_NO_FACE;

public class MainActivity extends AppCompatActivity implements SmartCamera.OnCameraExceptionListener, SmartCamera.OnEyeDirectionListener {

    private SmartCamera camera;

    Mat mat;
    Bitmap bitmap;
    ImageView image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        camera=(SmartCamera)findViewById(R.id.camera);
        camera.setDebugMode(true);
        camera.setOnCameraExceptionListener(this);
        camera.setOnDetectSightListener(this);
        camera.startCamera();
      /**  OpenCVLoader.initDebug();
        Rect face = getFace();
        if (face != null) {
            Rect rightRegion = new Rect(face.x + face.width / 16, (int) (face.y + (face.height / 4.5)), (face.width - 2 * face.width / 16) / 2, (int) (face.height / 3.0));
            Rect leftRegion = new Rect(face.x + face.width / 16 + (face.width - 2 * face.width / 16) / 2, (int) (face.y + (face.height / 4.5)), (face.width - 2 * face.width / 16) / 2, (int) (face.height / 3.0));
            Mat closed_mat=mat.submat(rightRegion);
            Mat strange_color_mat=new Mat();
            Imgproc.cvtColor(closed_mat, strange_color_mat, Imgproc.COLOR_RGB2YCrCb);
            Mat mask=closed_mat.clone();
            Imgproc.cvtColor(mask, mask, Imgproc.COLOR_RGB2GRAY);

            // Изъять из картинок указанные диапазоны значений пикселей
            // для каждого из цветовых каналов YCbCr.
            //cv::inRange(opened_mat, cv::Scalar(0,133,77), cv::Scalar(255,173,127), opened_mat);
            Core.inRange(strange_color_mat, new Scalar(0,133,77), new Scalar(255,173,127), strange_color_mat);
            Rect rect=new Rect(new Point(strange_color_mat.cols()/2-strange_color_mat.cols()/4, strange_color_mat.rows()/2-strange_color_mat.rows()/8),
                    new Point(strange_color_mat.cols()/2+strange_color_mat.cols()/4, strange_color_mat.rows()/2+strange_color_mat.rows()/8));
            Imgproc.rectangle(strange_color_mat, rect.tl(), rect.br(), new Scalar(0, 0, 0, 255));
            //Imgproc.rectangle(closed_mat, rightRegion.tl(), rightRegion.br(), new Scalar(255, 0, 0, 255));
            Mat eye_region=strange_color_mat.submat(rect);
            bitmap = Bitmap.createBitmap(strange_color_mat.cols(), strange_color_mat.rows(), Bitmap.Config.ARGB_8888);
            Log.e("LOG", "size "+strange_color_mat.cols()+" "+strange_color_mat.rows());
            int white=0;
            int black=0;
            for(int i=0; i<strange_color_mat.rows(); i++){
                for(int j=0; j<strange_color_mat.cols(); j++){
                    if (strange_color_mat.get(i, j)[0]==0){
                        black++;
                    }
                    else{
                        white++;
                    }
                }
            }
            if(black>=eye_region.cols()*eye_region.rows()*0.1){
                Log.e("LOG", "open");
            }
            Utils.matToBitmap(strange_color_mat, bitmap);
            image.setImageBitmap(bitmap);
        }*/
    }



    @Override
    public void onDetectSight(Point sight, Rect region) {

        //экспериментально выяснила, что координаты на камере во весь экран без погрешностей.
        //учесть погрешность при изменении размера и положения камеры
    }


    @Override
    public void onCameraExceptionListener(int exception) {

    }
}