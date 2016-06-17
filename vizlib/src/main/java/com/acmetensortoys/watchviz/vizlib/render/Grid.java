package com.acmetensortoys.watchviz.vizlib.render;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.acmetensortoys.watchviz.vizlib.RenderCB;
import com.acmetensortoys.watchviz.vizlib.meta.Avg;

public final class Grid extends RenderCB {
    private boolean doDebug;
    private final Paint p = new Paint();
    private final Paint dbp = new Paint();
    private float[] hsv = new float[]{0.0f, 1.0f, 1.0f};

    // 2^7 == 128 frames, at 512 samples per frame and 11025 KHz, this works out
    // to six seconds, which seems fine.
    private Avg meta = new Avg(7);

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

        float thisFrameSum = 0f;
        final float winAvg = meta.get();

        int rxs = cv.getWidth() / 8;
        int rys = cv.getHeight() / 8;
        for (int rx = 0; rx < 8; rx++) {
            for (int ry = 0; ry < 8; ry++) {
                int ix = (rx * 8 + ry) * 4 + 2;
                float x = (Math.abs(fft[ix]) + Math.abs(fft[ix+2]));

                thisFrameSum += x;

                // Two points define a line: (0 -> 0x20), (meta -> 0x60), cap at 0xFF.
                float sx = (0x40 / winAvg) * x + 0x20;
                int b = sx > 255 ? 255 : (int)sx;

                p.setAlpha(b);
                cv.drawRect(rx * rxs, ry * rys, (rx + 1) * rxs - 1, (ry + 1) * rys - 1, p);
                if(doDebug) {
                    cv.drawText(Integer.toHexString(b), rx*rxs + rxs/2, ry*rys + rys/2, dbp);
                }
            }
        }

        meta.update(thisFrameSum/64);
    }
};