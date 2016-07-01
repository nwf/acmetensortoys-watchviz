package com.acmetensortoys.watchviz.vizlib;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import org.jtransforms.fft.FloatFFT_1D;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class AudioProvider {
    private static final int AUDIO_RECORDER_BUFFER_SIZE = 2048;
    private static final int AUDIO_SAMPLES = 512;

    private Set<AudioReceiver> audioReceivers;
    private Thread audioSourceThread;

    public void add(AudioReceiver ar) {
        synchronized(this) {
            boolean empty = audioReceivers.isEmpty();
            audioReceivers.add(ar);

            if (empty) {
                this.start();
            }
        }
    }

    public void remove(AudioReceiver ar) {
        synchronized(this) {
            audioReceivers.remove(ar);
            if(audioReceivers.isEmpty()) {
                this.stop();
            }
        }
    }

    public AudioProvider() {
        audioReceivers = Collections.synchronizedSet(new HashSet<AudioReceiver>());
    }

    private void start() {
        final AudioRecord ar = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                11025, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT,
                AUDIO_RECORDER_BUFFER_SIZE);
        Log.d("shc", "Audio session ID:" + ar.getAudioSessionId());

        audioSourceThread = new Thread() {
            public void run() {
                // Raw audio samples
                float[] samples = new float[AUDIO_SAMPLES];

                // FFT data and engine
                float[] fft = new float[AUDIO_SAMPLES];
                FloatFFT_1D fftc = new FloatFFT_1D(AUDIO_SAMPLES);

                ar.startRecording();
                while (!Thread.interrupted()) {
                    final Rendering rcb;
                    ar.read(samples, 0, samples.length, AudioRecord.READ_BLOCKING);
                    System.arraycopy(samples, 0, fft, 0, AUDIO_SAMPLES);
                    fftc.realForward(fft);

                    for(AudioReceiver ar : audioReceivers) {
                        ar.onAudio(samples, fft);
                    }
                }
            }
        };
    }

    private void stop() {
        synchronized(this) {
            audioSourceThread.interrupt();

            try { audioSourceThread.join(); }
            catch (InterruptedException ie) { ; }
            finally { audioSourceThread = null; }
        }
    }
}
