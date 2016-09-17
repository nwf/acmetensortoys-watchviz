package com.acmetensortoys.watchviz.vizlib;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import org.jtransforms.fft.FloatFFT_1D;

import java.util.HashSet;
import java.util.Set;

public class AudioProvider {
    private static final int AUDIO_SAMPLE_HZ = 11025;
    private static final int AUDIO_RECORDER_BUFFER_SIZE = 2048;
    private static final int AUDIO_SAMPLES = 512;

    // AudioReceiver callbacks run every (AUDIO_SAMPLES / AUDIO_SAMPLE_HZ) seconds.

    private Set<AudioReceiver> audioReceivers;
    private Thread audioSourceThread;

    public void add(AudioReceiver ar) {
        synchronized(this) {
            boolean empty = audioReceivers.isEmpty();
            boolean did = audioReceivers.add(ar);

            if (empty) {
                Log.d("AudioProvider", "Starting audio source thread...");
                this.start();
            } else if (did) {
                Log.d("AudioProvider", "Added receiver; now=" + audioReceivers.size());
            }
        }
    }
    public void remove(AudioReceiver ar) {
        synchronized(this) {
            boolean did = audioReceivers.remove(ar);
            if(!did) {
                // Nothing to do!
                return;
            }
            if(audioReceivers.isEmpty()) {
                Log.d("AudioProvider", "Stopping audio source thread...");
                this.stop();
            } else {
                Log.d("AudioProvider", "Removed receiver; left=" + audioReceivers.size());
            }
        }
    }
    public AudioProvider() {
        audioReceivers = new HashSet<AudioReceiver>();
    }

    private void start() {
        final AudioRecord ar = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                AUDIO_SAMPLE_HZ, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT,
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

                    // Snapshot the current callbacks and iterate that.  This prevents
                    // concurrent mutation exceptions, at the cost of being kind of terrible.
                    Set<AudioReceiver> ars;
                    synchronized(this) {
                        ars = new HashSet<>(audioReceivers);
                    }
                    for (AudioReceiver ar : ars) {
                        ar.onAudio(samples, fft);
                    }
                }
                ar.stop();
                ar.release();
            }
        };
        audioSourceThread.start();
    }
    private void stop() {
        synchronized(this) {
            if (audioSourceThread != null) {
                audioSourceThread.interrupt();

                try {
                    audioSourceThread.join();
                } catch (InterruptedException ie) {
                    Log.w("AudioProvider", "join interrupted");
                } finally {
                    audioSourceThread = null;
                }
            }
        }
    }
}
