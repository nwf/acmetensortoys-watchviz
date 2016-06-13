package com.acmetensortoys.watchviz;

import android.graphics.Canvas;

public abstract class RenderCB {
    abstract public void render(Canvas c, float[] audio, float[] fft);
    public void onClick() { }
}