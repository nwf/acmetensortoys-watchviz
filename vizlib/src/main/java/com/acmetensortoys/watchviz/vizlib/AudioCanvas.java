package com.acmetensortoys.watchviz.vizlib;

import android.graphics.Canvas;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import com.acmetensortoys.watchviz.vizlib.render.Grid;
import com.acmetensortoys.watchviz.vizlib.render.WholeMax;

import org.jtransforms.fft.FloatFFT_1D;

import java.util.ArrayDeque;
import java.util.Deque;

public class AudioCanvas {
    private TextView mDebugView;
    private SurfaceView mSurfaceView;
    
    private RenderCB renderCB;
    private Deque<Class<? extends RenderCB>> cbq = new ArrayDeque<>();
    private Thread cycler;

    public AudioCanvas(TextView debugView, SurfaceView surfaceView) {
        this.mDebugView = debugView;
        this.mSurfaceView = surfaceView;

        mSurfaceView.getHolder().addCallback(shc);

        cbq.add(Grid.class);
        cbq.add(WholeMax.class);
        nextCyclerCB();
    }

    private void _setCyclerCB(Class<? extends RenderCB> next) {
        try {
            renderCB = next.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String name = renderCB.getClass().getSimpleName();
        mDebugView.setText(name.substring(0,Math.min(10,name.length())));
    }
    public void nextCyclerCB() {
        synchronized(this) {
            Class <? extends RenderCB> next = cbq.removeFirst();
            cbq.addLast(next);
            _setCyclerCB(next);
        }
    }
    public void prevCyclerCB() {
        synchronized(this) {
            Class <? extends RenderCB> next = cbq.removeLast();
            cbq.addFirst(next);
            _setCyclerCB(next);
        }
    }
    public void onClick() {
        final RenderCB rcb;
        synchronized(this) { rcb = renderCB; }
        rcb.onClick();
    }

    // The surface to which this callback is bound is created only after audio permissions
    // have been checked.  We therefore can simply start in the created callback.  Stopping
    // happens in onPause below, which fires before the surface is destroyed.
    private SurfaceHolder.Callback shc = new SurfaceHolder.Callback() {

        private static final int AUDIO_RECORDER_BUFFER_SIZE = 2048;
        private static final int AUDIO_SAMPLES = 512;

        @Override
        public void surfaceCreated(final SurfaceHolder h) {
            Log.d("shc", "Surface Created");
            Canvas c = h.lockCanvas();
            c.drawColor(Color.RED);
            h.unlockCanvasAndPost(c);

            final AudioRecord ar = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    11025, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT,
                    AUDIO_RECORDER_BUFFER_SIZE);
            Log.d("shc", "Audio session ID:" + ar.getAudioSessionId());

            cycler = new Thread() {
                public void run() {
                    // Raw audio samples
                    float[] samples = new float[AUDIO_SAMPLES];

                    // FFT data and engine
                    float[] fft = new float[AUDIO_SAMPLES];
                    FloatFFT_1D fftc = new FloatFFT_1D(AUDIO_SAMPLES);

                    ar.startRecording();
                    while (!Thread.interrupted()) {
                        final RenderCB rcb;
                        ar.read(samples, 0, samples.length, AudioRecord.READ_BLOCKING);
                        System.arraycopy(samples,0,fft,0,AUDIO_SAMPLES);
                        /*
                         * Debug: triangle wave
                         */
                        /*
                        for(int i = 0; i < samples.length; i++) {
                            samples[i] = (float)((i % 16) - 8) / 8;
                            if (i % 32 >= 16) { samples[i] *= -1; }
                        }
                        */

                        fftc.realForward(fft);

                        synchronized(this) {
                            rcb = renderCB;
                        }

                        Canvas cv = h.lockCanvas();
                        if (cv == null) {
                            // the surface must have been destroyed out from under us;
                            // just stop here.
                            break;
                        }
                        cv.drawColor(Color.BLACK);
                        rcb.render(cv,samples,fft);
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
            if(cycler != null) {
                cycler.interrupt();
                try { cycler.join(); }
                catch (InterruptedException e) { Log.d("shc", "IE while join cycler"); }
            }
        }
    };
}
