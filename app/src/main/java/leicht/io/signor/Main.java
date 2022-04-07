package leicht.io.signor;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.PowerManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import java.text.DecimalFormat;

public class Main extends Activity implements Knob.OnKnobChangeListener, SeekBar.OnSeekBarChangeListener,
        View.OnClickListener, ValueAnimator.AnimatorUpdateListener {

    private static final int DELAY = 250;
    private static final int MAX_LEVEL = 100;
    private static final int MAX_FINE = 1000;

    private static final String LOCK = "Signor:lock";

    private static final String STATE = "state";

    private static final String KNOB = "knob";
    private static final String MUTE = "mute";
    private static final String WAVE = "wave";
    private static final String LEVEL = "level";
    private static final String SLEEP = "sleep";
    private static final String FINE = "fine";

    private Audio audio;
    private final DecimalFormat decimalFormat = new DecimalFormat("0.00");
    private Knob knob;
    private TextView frequencyDisplay;
    private TextView volumeDisplay;

    private SeekBar level;
    private SeekBar fine;

    private PowerManager.WakeLock wakeLock;
    private PhoneStateListener phoneListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        frequencyDisplay = findViewById(R.id.frequency);
        volumeDisplay = findViewById(R.id.volume);
        knob = findViewById(R.id.knob);
        fine = findViewById(R.id.fine);
        level = findViewById(R.id.level);

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOCK);

        audio = new Audio();
        audio.start();

        // Setup widgets
        setupWidgets();

        // Setup phone state listener
        setupPhoneStateListener();

        // Restore state
        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        }
    }

    // Restore state
    private void restoreState(Bundle savedInstanceState) {
        Bundle bundle = savedInstanceState.getBundle(STATE);

        if (knob != null) {
            knob.setValue(bundle.getFloat(KNOB, 400));
        }

        // Waveform
        int waveform = bundle.getInt(WAVE, Audio.SINE);

        // Waveform buttons
        View view = null;
        switch (waveform) {
            case Audio.SINE:
                view = findViewById(R.id.sine);
                break;

            case Audio.SQUARE:
                view = findViewById(R.id.square);
                break;

            case Audio.SAWTOOTH:
                view = findViewById(R.id.sawtooth);
                break;
        }

        onClick(view);

        boolean mute = bundle.getBoolean(MUTE, false);

        if (mute) {
            view = findViewById(R.id.mute);
            onClick(view);
        }

        fine.setProgress(bundle.getInt(FINE, MAX_FINE / 2));
        level.setProgress(bundle.getInt(LEVEL, MAX_LEVEL / 10));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle bundle = new Bundle();

        bundle.putFloat(KNOB, knob.getValue());
        bundle.putInt(WAVE, audio.waveform);
        bundle.putBoolean(MUTE, audio.mute);
        bundle.putInt(FINE, fine.getProgress());
        bundle.putInt(LEVEL, level.getProgress());
        outState.putBundle(STATE, bundle);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            TelephonyManager manager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            manager.listen(phoneListener, PhoneStateListener.LISTEN_NONE);
        } catch (Exception e) {
        }

        if (audio != null) {
            audio.stop();
        }
    }

    // On knob change
    @Override
    public void onKnobChange(Knob knob, float value) {
        // Frequency
        double frequency = Math.pow(10.0, value / 200.0) * 10.0;
        double adjust = ((fine.getProgress() - MAX_FINE / 2) / (double) MAX_FINE) / 100.0;

        frequency += frequency * adjust;

        // Display
        if (frequencyDisplay != null) {
            frequencyDisplay.setText(decimalFormat.format(frequency) + "Hz");
        }

        if (audio != null) {
            audio.frequency = frequency;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int id = seekBar.getId();

        if (audio == null) {
            return;
        }

        // Check id
        switch (id) {
            // Fine
            case R.id.fine: {
                double frequency = Math.pow(10.0, knob.getValue() / 200.0) * 10.0;
                double adjust = ((progress - MAX_FINE / 2) / (double) MAX_FINE) / 50.0;

                frequency += frequency * adjust;

                if (frequencyDisplay != null) {
                    frequencyDisplay.setText(decimalFormat.format(frequency) + "Hz");
                }

                if (audio != null) {
                    audio.frequency = frequency;
                }
            }
            break;

            // Level
            case R.id.level:
                if (volumeDisplay != null) {
                    double level = Math.log10(progress / (double) MAX_LEVEL) * 20.0;

                    if (level < -80.0)
                        level = -80.0;

                    volumeDisplay.setText(decimalFormat.format(level) + "dB");
                }

                if (audio != null)
                    audio.level = progress / (double) MAX_LEVEL;
                break;
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        switch (id) {
            case R.id.sine:
                if (audio != null)
                    audio.waveform = Audio.SINE;
                ((Button) v).setCompoundDrawablesWithIntrinsicBounds(
                        android.R.drawable.radiobutton_on_background, 0, 0, 0);

                v = findViewById(R.id.square);
                ((Button) v).setCompoundDrawablesWithIntrinsicBounds(
                        android.R.drawable.radiobutton_off_background, 0, 0, 0);
                v = findViewById(R.id.sawtooth);
                ((Button) v).setCompoundDrawablesWithIntrinsicBounds(
                        android.R.drawable.radiobutton_off_background, 0, 0, 0);
                break;
            case R.id.square:
                if (audio != null)
                    audio.waveform = Audio.SQUARE;
                ((Button) v).setCompoundDrawablesWithIntrinsicBounds(
                        android.R.drawable.radiobutton_on_background, 0, 0, 0);

                v = findViewById(R.id.sine);
                ((Button) v).setCompoundDrawablesWithIntrinsicBounds(
                        android.R.drawable.radiobutton_off_background, 0, 0, 0);
                v = findViewById(R.id.sawtooth);
                ((Button) v).setCompoundDrawablesWithIntrinsicBounds(
                        android.R.drawable.radiobutton_off_background, 0, 0, 0);
                break;
            case R.id.sawtooth:
                if (audio != null)
                    audio.waveform = Audio.SAWTOOTH;
                ((Button) v).setCompoundDrawablesWithIntrinsicBounds(
                        android.R.drawable.radiobutton_on_background, 0, 0, 0);

                v = findViewById(R.id.sine);
                ((Button) v).setCompoundDrawablesWithIntrinsicBounds(
                        android.R.drawable.radiobutton_off_background, 0, 0, 0);
                v = findViewById(R.id.square);
                ((Button) v).setCompoundDrawablesWithIntrinsicBounds(
                        android.R.drawable.radiobutton_off_background, 0, 0, 0);
                break;

            case R.id.mute:
                if (audio != null)
                    audio.mute = !audio.mute;

                /* if (audio.mute)
                    ((Button) v).setCompoundDrawablesWithIntrinsicBounds(
                            android.R.drawable.checkbox_on_background, 0, 0, 0);
                else
                    ((Button) v).setCompoundDrawablesWithIntrinsicBounds(
                            android.R.drawable.checkbox_off_background, 0, 0, 0); */
                break;
            case R.id.lower:
                if (fine != null) {
                    int progress = fine.getProgress();
                    fine.setProgress(--progress);
                }
                break;
            case R.id.higher: {
                int progress = fine.getProgress();
                fine.setProgress(++progress);
            }
            break;
        }
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        float value = (Float) animation.getAnimatedValue();

        if (knob != null) {
            knob.setValue(value);
        }
    }

    private void setupWidgets() {
        View view;

        if (knob != null) {
            knob.setOnKnobChangeListener(this);
            knob.setValue(400);

            view = findViewById(R.id.previous);
            if (view != null)
                view.setOnClickListener(knob);

            view = findViewById(R.id.next);
            if (view != null)
                view.setOnClickListener(knob);
        }

        view = findViewById(R.id.back);
        if (view != null) {
            view.setOnClickListener(this);
        }

        view = findViewById(R.id.forward);
        if (view != null) {
            view.setOnClickListener(this);
        }

        view = findViewById(R.id.lower);
        if (view != null) {
            view.setOnClickListener(this);
        }

        view = findViewById(R.id.higher);
        if (view != null) {
            view.setOnClickListener(this);
        }

        if (fine != null) {
            fine.setOnSeekBarChangeListener(this);

            fine.setMax(MAX_FINE);
            fine.setProgress(MAX_FINE / 2);
        }

        if (level != null) {
            level.setOnSeekBarChangeListener(this);

            level.setMax(MAX_LEVEL);
            level.setProgress(MAX_LEVEL / 10);
        }

        view = findViewById(R.id.sine);
        if (view != null) {
            view.setOnClickListener(this);
        }

        view = findViewById(R.id.square);
        if (view != null) {
            view.setOnClickListener(this);
        }

        view = findViewById(R.id.sawtooth);
        if (view != null) {
            view.setOnClickListener(this);
        }

        view = findViewById(R.id.mute);
        if (view != null) {
            view.setOnClickListener(this);
        }
    }

    private void setupPhoneStateListener() {
        phoneListener = new PhoneStateListener() {
            public void onCallStateChanged(int state, String incomingNumber) {
                if (state != TelephonyManager.CALL_STATE_IDLE) {
                    if (!audio.mute) {
                        View v = findViewById(R.id.mute);
                        if (v != null) {
                            onClick(v);
                        }
                    }
                }
            }

        };

        try {
            TelephonyManager manager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            manager.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE);
        } catch (Exception e) {
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    // Audio
    protected class Audio implements Runnable {
        protected static final int SINE = 0;
        protected static final int SQUARE = 1;
        protected static final int SAWTOOTH = 2;

        protected int waveform;
        protected boolean mute = true;

        protected double frequency;
        protected double level;

        protected float duty;

        protected Thread thread;

        private AudioTrack audioTrack;

        protected Audio() {
            frequency = 440.0;
            level = 16384.0;
        }

        // Start
        protected void start() {
            thread = new Thread(this, "Audio");
            thread.start();
        }

        // Stop
        protected void stop() {
            Thread t = thread;
            thread = null;

            try {
                if (t != null && t.isAlive()) {
                    t.join(); // Wait for the thread to exit
                }
            } catch (Exception e) {
            }
        }

        public void run() {
            processAudio();
        }

        // Process audio
        @SuppressWarnings("deprecation")
        protected void processAudio() {
            short buffer[];

            int rate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC);
            int minSize = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

            // Find a suitable buffer size
            int sizes[] = {1024, 2048, 4096, 8192, 16384, 32768};
            int size = 0;

            for (int s : sizes) {
                if (s > minSize) {
                    size = s;
                    break;
                }
            }

            final double K = 2.0 * Math.PI / rate;

            // Create the audio track
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, size, AudioTrack.MODE_STREAM);

            // Check state
            int state = audioTrack.getState();

            if (state != AudioTrack.STATE_INITIALIZED) {
                audioTrack.release();
                return;
            }

            audioTrack.play();

            // Create the buffer
            buffer = new short[size];

            // Initialise the generator variables
            double f = frequency;
            double l = 0.0;
            double q = 0.0;

            while (thread != null) {
                double t = (duty * 2.0 * Math.PI) - Math.PI;

                // Fill the current buffer
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
}
