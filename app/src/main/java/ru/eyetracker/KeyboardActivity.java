package ru.eyetracker;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Locale;

public class KeyboardActivity extends AppCompatActivity {
    private EditText mEditText;
private TextToSpeechManager mTTSManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_keyboard);
        try {
            mTTSManager = new TextToSpeechManager(this, Locale.getDefault());
        } catch (TextToSpeechManager.TTSManagerException e) {
            Toast.makeText(this,"Ваш язык не поддерживается", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            e.printStackTrace();
        }
        mEditText = (EditText) findViewById(R.id.text_et);
    }

    public void play(View view) {
        mTTSManager.initQueue(mEditText.getText().toString());
    }

    public void onBack(View view) {
        mTTSManager.shutDown();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
