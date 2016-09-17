package com.acmetensortoys.watchviz.vizlib;

import android.content.SharedPreferences;
import android.graphics.Canvas;

public abstract class Rendering {
    public Rendering(SharedPreferences lsp, SharedPreferences gsp) {}
    abstract public void render(Canvas c, float[] audio, float[] fft);
    public void onClick() { }
}