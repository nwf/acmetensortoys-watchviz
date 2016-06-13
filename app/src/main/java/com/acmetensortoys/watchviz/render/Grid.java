package com.acmetensortoys.watchviz.render;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.acmetensortoys.watchviz.RenderCB;

public final class Grid extends RenderCB {
    private boolean doDebug;
    private final Paint p = new Paint();
    private final Paint dbp = new Paint();
    private float[] hsv = new float[]{0.0f, 1.0f, 1.0f};

    public Grid()
    {
        dbp.setColor(Color.WHITE);
    }

    @Override
    public void onClick() {
        doDebug = !doDebug;
    }

    @Override
    public void render(Canvas cv, float[] au, float[] fft) {
        int c = Color.HSVToColor(hsv);
        hsv[0] = hsv[0] >= 359 ? 0.0f : hsv[0] + 1.0f;
        p.setColor(c);

        final float scale = 32;

        int rxs = cv.getWidth() / 8;
        int rys = cv.getHeight() / 8;
        for (int rx = 0; rx < 8; rx++) {
            for (int ry = 0; ry < 8; ry++) {
                int ix = (rx * 8 + ry) * 4;
                float x = (Math.abs(fft[ix]) + Math.abs(fft[ix+2])) * scale;
                int b = x > 255 ? 255 : (int)x;
                p.setAlpha(b > 223 ? 255 : b + 32);
                cv.drawRect(rx * rxs, ry * rys, (rx + 1) * rxs - 1, (ry + 1) * rys - 1, p);
                if(doDebug) {
                    cv.drawText(Integer.toHexString(b), rx*rxs + rxs/2, ry*rys + rys/2, dbp);
                }
            }
        }
    }
};
