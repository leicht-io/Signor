package leicht.io.signor;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class Audio implements Runnable {
    // TODO: Add to enum
    protected static final int SINE = 0;
    protected static final int SQUARE = 1;
    protected static final int SAWTOOTH = 2;
    protected int waveform;

    protected boolean mute = true;

    protected double frequency;
    protected double level;

    protected float duty;

    protected Thread thread;

    protected Audio() {
        frequency = 440.0;
        level = 16384.0;
    }

    protected void start() {
        thread = new Thread(this, "Audio");
        thread.start();
    }

    protected void stop() {
        Thread t = thread;
        thread = null;

        try {
            if (t != null && t.isAlive()) {
                t.join();
            }
        } catch (Exception e) {
            // Do nothing
        }
    }

    public void run() {
        processAudio();
    }

    protected void processAudio() {
        short[] buffer;

        int rate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC);
        int minSize = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

        int[] sizes = {1024, 2048, 4096, 8192, 16384, 32768};
        int size = 0;

        for (int s : sizes) {
            if (s > minSize) {
                size = s;
                break;
            }
        }

        final double K = 2.0 * Math.PI / rate;

        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, size, AudioTrack.MODE_STREAM);

        int state = audioTrack.getState();

        if (state != AudioTrack.STATE_INITIALIZED) {
            audioTrack.release();
            return;
        }

        audioTrack.play();

        buffer = new short[size];

        double f = frequency;
        double l = 0.0;
        double q = 0.0;

        while (thread != null) {
            double t = (duty * 2.0 * Math.PI) - Math.PI;

            for (int i = 0; i < buffer.length; i++) {
                f += (frequency - f) / 4096.0;
                l += ((mute ? 0.0 : level) * 16384.0 - l) / 4096.0;
                q += ((q + (f * K)) < Math.PI) ? f * K :
                        (f * K) - (2.0 * Math.PI);

                switch (waveform) {
                    case SINE:
                        buffer[i] = (short) Math.round(Math.sin(q) * l);
                        break;
                    case SQUARE:
                        buffer[i] = (short) ((q > t) ? l : -l);
                        break;
                    case SAWTOOTH:
                        buffer[i] = (short) Math.round((q / Math.PI) * l);
                        break;
                }
            }

            audioTrack.write(buffer, 0, buffer.length);
        }

        audioTrack.stop();
        audioTrack.release();
    }
}