package ru.eyetracker;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.SOURCE;

@Retention(SOURCE)
@IntDef({
        Rotation.ROTATION_0,
        Rotation.ROTATION_90,
        Rotation.ROTATION_180,
        Rotation.ROTATION_270
})
public @interface Rotation {
    int ROTATION_0 =0;
    int ROTATION_90 =90;
    int ROTATION_180 =180;
    int ROTATION_270 =270;
}
