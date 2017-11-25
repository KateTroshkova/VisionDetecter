package ru.eyetracker;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.troshkova.portfolioprogect.visiondetector.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    public void showKeyboard(View view) {
        startActivity(new Intent(this,KeyboardActivity.class));
    }

    public void showInfo(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Об авторе")
                .setMessage("Продукт разработан Дмитрием Марковым")
                .setCancelable(false)
                .setNegativeButton("Понятно",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void showEyeRemote(View view) {
        startActivity(new Intent(this,EyeActivity.class));
    }
}