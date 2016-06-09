package com.acmetensortoys.watchviz;

import android.graphics.Canvas;

public abstract class RenderCB {
    abstract public void render(Canvas c, float[] a);
    public void onClick() { }
}