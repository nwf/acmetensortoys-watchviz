package com.acmetensortoys.watchviz;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;

import java.util.Locale;

public final class RenderWholeScreenMax extends RenderCB {
    private boolean doDebug;
    private final Paint dbp = new Paint();
    private float[] hsv = new float[]{0.0f, 1.0f, 1.0f};

    public RenderWholeScreenMax()
    {
        dbp.setColor(Color.WHITE);
    }

    @Override
    public void onClick() {
        doDebug = !doDebug;
    }

    @Override
    public void render(Canvas cv, float[] samples) {
        float msamp = 0.0f;
        int mix = -1;
            /* Restrict search to lowest half in agreement with RenderGrid */
        for (int i = 0; i < samples.length/2; i += 2) {
            if (samples[i] > msamp) {
                msamp = samples[i];
                mix = i;
            }
        }
        int canvalpha = msamp > 3 ? 255 : (int) (msamp * 64) + 63; // XXX wtf scale?

        int c = Color.HSVToColor(hsv);
        hsv[0] = hsv[0] >= 359 ? 0.0f : hsv[0] + 1.0f;

        cv.drawColor(c);
        cv.drawColor(Color.rgb(canvalpha, canvalpha, canvalpha), PorterDuff.Mode.MULTIPLY);
        if (doDebug) {
            cv.drawText(String.format(Locale.US, "%1$8.1E @ %2$d", msamp, mix), 30, 30, dbp);
        }
    }
};