package com.acmetensortoys.watchviz.vizlib;

public interface AudioReceiver {
    void onAudio(float[] audio, float[] fft);
}
