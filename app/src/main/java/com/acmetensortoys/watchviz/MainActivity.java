/*
    AcmeTensorToys WatchViz: a sound visualization application for Android Wear
    Copyright (C) 2016 Nathaniel Wesley Filardo

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.acmetensortoys.watchviz;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Locale;
import java.util.Queue;

import org.jtransforms.fft.FloatFFT_1D;

public class MainActivity extends WearableActivity
{
    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);

    private BoxInsetLayout mOuterContainer;
    private LinearLayout mInnerContainer;
    private TextView mTextView, mClockView, mDebugView;

    private SurfaceView cyclersv;
    private Thread cycler;

    public abstract static class RenderCB {
        boolean doDebug = false;

        abstract public void render(Canvas c, float[] a);
        public void setDebug(boolean b) { doDebug = b; }
        public final void toggleDebug() { setDebug(!doDebug); }
    }
    private final RenderCB renderWholeScreenMax = new RenderCB() {
        private float[] hsv = new float[]{0.0f, 1.0f, 1.0f};

        public void setDebug(boolean d) {
            super.setDebug(d);
            if(!d) {
                mDebugView.post(new Runnable() {
                    public void run() {
                        mDebugView.setVisibility(View.GONE);
                    }
                } );
            } else {
                mDebugView.post(new Runnable() {
                    public void run() {
                        mDebugView.setVisibility(View.VISIBLE);
                    }
                } );
            }
        }

        public void render(Canvas cv, float[] samples) {
            float msamp = 0.0f;
            int mix = -1;
            for (int i = 0; i < samples.length; i += 2) {
                if (samples[i] > msamp) {
                    msamp = samples[i];
                    mix = i;
                }
            }
            if (doDebug) {
                final float fmsamp = msamp;
                final int fmix = mix;
                mDebugView.post(new Runnable() {
                    public void run() {
                        mDebugView.setText(String.format(Locale.US, "%1$8.1E @ %2$d", fmsamp, fmix));
                    }
                });
            }
            int canvalpha = msamp > 3 ? 255 : (int) (msamp * 64) + 63; // XXX wtf scale?

            int c = Color.HSVToColor(hsv);
            hsv[0] = hsv[0] >= 359 ? 0.0f : hsv[0] + 1.0f;

            cv.drawColor(c);
            cv.drawColor(Color.rgb(canvalpha, canvalpha, canvalpha), PorterDuff.Mode.MULTIPLY);
        }
    };
    private final RenderCB renderGrid = new RenderCB() {
        final Paint p = new Paint();
        final Paint dbp = new Paint();
        float[] hsv = new float[]{0.0f, 1.0f, 1.0f};

        // Ahahaha; anonymous classes of course have anonymous constructors.  How do you like that?
        {
            dbp.setColor(Color.WHITE);
        }

        public void render(Canvas cv, float[] samples) {
            int c = Color.HSVToColor(hsv);
            hsv[0] = hsv[0] >= 359 ? 0.0f : hsv[0] + 1.0f;
            p.setColor(c);

            int rxs = cv.getWidth() / 8;
            int rys = cv.getHeight() / 8;
            for (int rx = 0; rx < 8; rx++) {
                for (int ry = 0; ry < 8; ry++) {
                    float x = Math.abs(samples[((rx * 8 + ry) << 2) + 32]) * 64;
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

    private RenderCB cyclercb;
    private Queue<RenderCB> cyclercbq = new ArrayDeque<>();
    {
        cyclercbq.add(renderGrid);
        cyclercb = renderWholeScreenMax;
    }
    private void nextCyclerCB() {
        synchronized(this) {
            cyclercb.setDebug(false);
            cyclercbq.add(cyclercb);
            cyclercb = cyclercbq.remove();
        }
    }

    // The surface to which this callback is bound is created only after audio permissions
    // have been checked.  We therefore can simply start in the created callback.  Stopping
    // happens in onPause below, which fires before the surface is destroyed.
    private SurfaceHolder.Callback shc = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(final SurfaceHolder h) {
            Log.d("shc", "Surface Created");
            Canvas c = h.lockCanvas();
            c.drawColor(Color.RED);
            h.unlockCanvasAndPost(c);

            final AudioRecord ar = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    11025, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT, 2048);
            Log.d("shc", "Audio session ID:" + ar.getAudioSessionId());

            cycler = new Thread() {
                public void run() {
                    float[] samples = new float[512];
                    FloatFFT_1D fft = new FloatFFT_1D(samples.length);

                    ar.startRecording();
                    while (!Thread.interrupted()) {
                        final RenderCB rcb;
                        ar.read(samples, 0, samples.length, AudioRecord.READ_BLOCKING);
                        synchronized(this) {
                            rcb = cyclercb;
                        }
                        /*
                         * Debug: triangle wave
                         */
                        /*
                        for(int i = 0; i < samples.length; i++) {
                            samples[i] = (float)((i % 16) - 8) / 8;
                            if (i % 32 >= 16) { samples[i] *= -1; }
                        }
                        */
                        fft.realForward(samples);

                        Canvas cv = h.lockCanvas();
                        cv.drawColor(Color.BLACK);
                        rcb.render(cv,samples);
                        h.unlockCanvasAndPost(cv);

                        // try { Thread.sleep(100); } catch (InterruptedException e) { break; }
                    }
                    ar.stop();
                    ar.release();
                    Log.d("cycler", "exit");
                }
            };
            cycler.start();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d("shc", "Surface Changed");
            ;
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d("shc", "Surface Destroyed");
        }
    };

    private void createSurface() {
        Log.d("createSurface", "top");
        cyclersv = new SurfaceView(this);
        cyclersv.getHolder().addCallback(shc);
        cyclersv.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                final RenderCB rcb;
                synchronized(this) {
                    rcb = cyclercb;
                }
                rcb.toggleDebug();
            }
        });
        cyclersv.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        nextCyclerCB();
                        return true;
                    }
                });

        mInnerContainer.addView(cyclersv, -1,
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void removeSurface() {
        Log.d("removeSurface", "stopping cycler");
        if(cycler != null) {
            cycler.interrupt();
            try { cycler.join(); }
            catch (InterruptedException e) { Log.d("shc", "IE while join cycler"); }
        }
        Log.d("removeSurface", "removing view");
        mInnerContainer.removeView(cyclersv);
        cyclersv = null;
        Log.d("removeSurface", "done");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        mOuterContainer = (BoxInsetLayout) findViewById(R.id.top_container);
        mInnerContainer = (LinearLayout) findViewById(R.id.linear_container);
        mTextView = (TextView) findViewById(R.id.text);
        mDebugView = (TextView) findViewById(R.id.dbg);
        mClockView = (TextView) findViewById(R.id.clock);

        updateDisplay();
    }

    @Override
    public void onStart() {
        Log.d("onStart", "top");
        super.onStart();
    }

    @Override
    public void onResume() {
        Log.d("onResume", "top");
        super.onResume();

        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            createSurface();
        } else {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int rq, @NonNull String[] ps, @NonNull int[] rs) {
        if(rq == 1) {
            if (rs[0] == 1) {
                createSurface();
            } else {
                //TODO: Something else
                throw new RuntimeException("asdf");
            }
        }
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {
        final int black = getResources().getColor(android.R.color.black,null);
        mClockView.setText(AMBIENT_DATE_FORMAT.format(new Date()));

        if (isAmbient()) {
            final int white = getResources().getColor(android.R.color.white,null);
            mOuterContainer.setBackgroundColor(black);
            mTextView.setTextColor(white);
            mClockView.setTextColor(white);
        } else {
            mOuterContainer.setBackground(null);
            mTextView.setTextColor(black);
            mClockView.setTextColor(black);
        }
    }

    @Override
    public void onPause() {
        Log.d("onPause", "top");
        removeSurface();
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.d("onStop", "top");
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.d("onDestroy", "top");
        super.onDestroy();
    }
}
