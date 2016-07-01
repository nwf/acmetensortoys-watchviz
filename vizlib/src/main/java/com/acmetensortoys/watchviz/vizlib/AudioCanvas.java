package com.acmetensortoys.watchviz.vizlib;

import android.graphics.Canvas;
import android.graphics.Color;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class AudioCanvas implements Renderer {
    private AudioProvider mAudioProvider;
    private SurfaceView mSurfaceView;
    private Rendering rendering;

    public AudioCanvas(SurfaceView surfaceView, AudioProvider ap) {
        this.mSurfaceView = surfaceView;
        this.mAudioProvider = ap;

        mSurfaceView.getHolder().addCallback(shc);
    }

    public void setRendering(Rendering r) {
        synchronized(this) { rendering = r; }
    }

    public void onClick() {
        final Rendering rcb;
        synchronized(this) { rcb = rendering; }
        rcb.onClick();
    }

    // The surface to which this callback is bound is created only after audio permissions
    // have been checked.  We therefore can simply start in the created callback.  Stopping
    // happens in onPause below, which fires before the surface is destroyed.
    private SurfaceHolder.Callback shc = new SurfaceHolder.Callback() {
        private AudioReceiver ar;

        @Override
        public void surfaceCreated(final SurfaceHolder h) {
            Log.d("shc", "Surface Created");
            Canvas c = h.lockCanvas();
            c.drawColor(Color.RED);
            h.unlockCanvasAndPost(c);

            synchronized(this) {
                if (ar != null) {
                    Log.d("shc", "Create with existing ar?");
                    mAudioProvider.remove(ar);
                }
                ar = new AudioReceiver() {
                    @Override
                    public void onAudio(float[] audio, float[] fft) {
                        final Rendering rcb;
                        synchronized (this) { rcb = rendering; }
                        Canvas cv = h.lockCanvas();
                        if (cv == null) {
                            // the surface must have been destroyed out from under us;
                            // just stop here, on the presumption that we will be
                            // removed from the provider by the hook below
                            return;
                        }
                        cv.drawColor(Color.BLACK);
                        rcb.render(cv, audio, fft);
                        h.unlockCanvasAndPost(cv);
                    }
                };
                mAudioProvider.add(ar);
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d("shc", "Surface Changed");
            ;
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d("shc", "Surface Destroyed");
            synchronized(this) {
                mAudioProvider.remove(ar);
                ar = null;
            }
        }
    };
}
