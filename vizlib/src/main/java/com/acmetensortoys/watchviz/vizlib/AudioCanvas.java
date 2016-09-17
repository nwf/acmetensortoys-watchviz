package com.acmetensortoys.watchviz.vizlib;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewTreeObserver;


public class AudioCanvas implements Renderer {
    private final AudioProvider mAp;
    private final SurfaceView mSv;
    private AudioReceiver mAr;
    private Rendering mRend;
    private Rect gvr = new Rect();

    public AudioCanvas(final String debug1, SurfaceView sv, AudioProvider ap) {
        this.mAp = ap;
        this.mSv = sv;
        sv.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(final SurfaceHolder h) {
                Log.d("shc:" + debug1, "Surface Created");
                Canvas c = h.lockCanvas();
                c.drawColor(Color.RED);
                h.unlockCanvasAndPost(c);

                synchronized(AudioCanvas.this) {
                    if (mAr != null) {
                        Log.d("shc:" + debug1, "Create with existing mAr?");
                        mAp.remove(mAr);
                    }
                    mAr = new AudioReceiver() {
                        @Override
                        public void onAudio(float[] audio, float[] fft) {
                            final Rendering rcb;
                            synchronized (AudioCanvas.this) {
                                rcb = mRend;
                                if (rcb == null) {
                                    mAp.remove(this);
                                }
                            }
                            final Canvas cv;
                            try {
                                cv = h.lockCanvas();
                            } catch (IllegalArgumentException iae) {
                                // Canvas must not exist and we have just somehow missed the
                                // destroy callback (this sometimes appears to happen when the
                                // application is in the midst of crashing).  Go ahead and
                                // unsubscribe us, so the messages at least stop happening.
                                mAp.remove(this);
                                return;
                            }
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
                }
            }
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.d("shc:"+debug1, "Surface Changed:"+format+":"+width+":"+height);
            }
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.d("shc:"+debug1, "Surface Destroyed");
                synchronized(AudioCanvas.this) {
                    mAp.remove(mAr);
                    mAr = null;
                }
            }
        });
        sv.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                manageSubscription();
                return true;
            }
        });
        sv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Rendering rcb;
                synchronized(AudioCanvas.this) { rcb = mRend; }
                if (rcb != null) {
                    rcb.onClick();
                }
            }
        });
    }

    public void setRendering(Rendering r) {
        synchronized(AudioCanvas.this) {
            mRend = r;
            manageSubscription();
        }
    }
    private void manageSubscription() {
        boolean gvrf = mSv.getGlobalVisibleRect(gvr);
        // Log.d("asv:"+debug1,"manageSubscription:gvrf="+gvrf+":gvr="+gvr.toString());
        synchronized(AudioCanvas.this) {
            if (mAr != null) {
                // Surface has been created
                if (gvrf && mRend != null) {
                    // Visible and have rendering
                    mAp.add(mAr);
                } else {
                    // Invisible or no rendering set
                    mAp.remove(mAr);
                }
            }
        }
    }
}
