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
import java.util.Date;
import java.util.Locale;

import org.jtransforms.fft.FloatFFT_1D;

public class MainActivity extends WearableActivity
{
    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);

    private BoxInsetLayout mOuterContainer;
    private LinearLayout mInnerContainer;
    private TextView mTextView, mClockView, mDebugView;
    private boolean doDebug = false;

    private SurfaceView cyclersv;
    private Thread cycler;

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

            final TextView dbv = mDebugView;
            final AudioRecord ar = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    11025, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT, 2048);
            Log.d("shc", "Audio session ID:" + ar.getAudioSessionId());

            cycler = new Thread() {
                public void run() {
                    float[] hsv = new float[]{0.0f, 1.0f, 1.0f};
                    float[] samples = new float[512];
                    FloatFFT_1D fft = new FloatFFT_1D(samples.length);

                    /*
                     * Debug: triangle wave
                     *
                    for(int i = 0; i < samples.length; i++) {
                        samples[i] = (float)((i % 16) - 8) / 8;
                        if (i % 32 >= 16) { samples[i] *= -1; }
                    }
                    */
                    ar.startRecording();
                    while (!Thread.interrupted()) {
                        ar.read(samples, 0, samples.length, AudioRecord.READ_BLOCKING);
                        fft.realForward(samples);

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
                            dbv.post(new Runnable() {
                                public void run() {
                                    dbv.setText(String.format(Locale.US, "%1$8.1E @ %2$d", fmsamp, fmix));
                                }
                            });
                        }

                        int canvalpha = msamp > 3 ? 255 : (int) (msamp * 64) + 63; // XXX wtf scale?

                        int c = Color.HSVToColor(hsv);
                        hsv[0] = hsv[0] >= 359 ? 0.0f : hsv[0] + 1.0f;

                        Canvas cv = h.lockCanvas();
                        cv.drawColor(c);
                        cv.drawColor(Color.rgb(canvalpha, canvalpha, canvalpha), PorterDuff.Mode.MULTIPLY);
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
            mDebugView.setVisibility(View.GONE);
            doDebug = false;
        } else {
            mOuterContainer.setBackground(null);
            mTextView.setTextColor(black);
            mClockView.setTextColor(black);
            mDebugView.setVisibility(View.VISIBLE);
            doDebug = true;
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
