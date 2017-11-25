package ru.eyetracker;

import android.content.Context;
import android.os.Build;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

public class TextToSpeechManager {

    private TextToSpeech mSpeech = null;

    public TextToSpeechManager(Context context, final Locale language) throws TTSManagerException {
        final String[] error = {null};
        mSpeech = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = mSpeech.setLanguage(language);

                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        error[0] = "This Language is not supported";
                    }
                } else error[0] = "Initialization Failed!";
            }
        });
        if (error[0] != null) throw new TTSManagerException(error[0]);
    }

    public void addQueue(String text) {
        speak(text, TextToSpeech.QUEUE_ADD);
    }

    public void initQueue(String text) {
        speak(text, TextToSpeech.QUEUE_FLUSH);
    }

    public void shutDown() {
        mSpeech.shutdown();
    }

    public void setLanguage(Locale language) throws TTSManagerException {
        int result = mSpeech.setLanguage(language);
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            throw new TTSManagerException("This Language is not supported");
        }
    }

    private void speak(String text, int queueMode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mSpeech.speak(text, queueMode, null, null);
        } else mSpeech.speak(text, queueMode, null);
    }

    class TTSManagerException extends Exception {
        public TTSManagerException(String message) {
            super(message);
        }
    }

}