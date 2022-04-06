package com.example.signor;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.InputType;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class Main extends Activity
        implements Knob.OnKnobChangeListener, SeekBar.OnSeekBarChangeListener,
        View.OnClickListener, ValueAnimator.AnimatorUpdateListener {

    private static final int DELAY = 250;
    private static final int MAX_LEVEL = 100;
    private static final int MAX_FINE = 1000;

    private static final String LOCK = "SigGen:lock";

    private static final String STATE = "state";

    private static final String KNOB = "knob";
    private static final String WAVE = "wave";
    private static final String MUTE = "mute";
    private static final String FINE = "fine";
    private static final String LEVEL = "level";
    private static final String SLEEP = "sleep";

    public static final String PREF_BOOKMARKS = "pref_bookmarks";
    public static final String PREF_DARK_THEME = "pref_dark_theme";
    public static final String PREF_DUTY = "pref_duty";

    private Audio audio;

    private Knob knob;
    private Scale scale;
    private Display display;

    private SeekBar fine;
    private SeekBar level;

    private Toast toast;

    private PowerManager.WakeLock wakeLock;
    private PhoneStateListener phoneListener;
    private List<Double> bookmarks;

    private boolean sleep;
    private boolean darkTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getPreferences();

        if (!darkTheme)
            setTheme(R.style.AppTheme);

        setContentView(R.layout.main);

        // Get views
        display = findViewById(R.id.display);
        scale = findViewById(R.id.scale);
        knob = findViewById(R.id.knob);
        fine = findViewById(R.id.fine);
        level = findViewById(R.id.level);

        // Get wake lock
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

    // On Resume
    @Override
    protected void onResume() {
        super.onResume();

        boolean dark = darkTheme;

        // Get preferences
        getPreferences();

        if (dark != darkTheme && Build.VERSION.SDK_INT != Build.VERSION_CODES.M) {
            recreate();
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

        // Fine frequency and level
        fine.setProgress(bundle.getInt(FINE, MAX_FINE / 2));
        level.setProgress(bundle.getInt(LEVEL, MAX_LEVEL / 10));

        // Sleep
        sleep = bundle.getBoolean(SLEEP, false);

        if (sleep) {
            wakeLock.acquire(10 * 60 * 1000L); // 10 minutes
        }
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
        bundle.putBoolean(SLEEP, sleep);
        outState.putBundle(STATE, bundle);
    }

    // On pause
    @Override
    protected void onPause() {
        super.onPause();

        // Get preferences
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (bookmarks != null) {
            JSONArray json = new JSONArray(bookmarks);

            // Save preference
            SharedPreferences.Editor edit = preferences.edit();
            edit.putString(PREF_BOOKMARKS, json.toString());
            edit.apply();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            TelephonyManager manager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            manager.listen(phoneListener, PhoneStateListener.LISTEN_NONE);
        } catch (Exception e) {
        }

        if (sleep) {
            wakeLock.release();
        }

        if (audio != null) {
            audio.stop();
        }
    }

    // On knob change
    @Override
    public void onKnobChange(Knob knob, float value) {
        // Scale
        if (scale != null) {
            scale.setValue((int) (-value * 2.5));
        }

        // Frequency
        double frequency = Math.pow(10.0, value / 200.0) * 10.0;
        double adjust = ((fine.getProgress() - MAX_FINE / 2) / (double) MAX_FINE) / 100.0;

        frequency += frequency * adjust;

        // Display
        if (display != null) {
            display.setFrequency(frequency);
        }

        if (audio != null) {
            audio.frequency = frequency;
        }

        checkBookmarks();
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

                if (display != null) {
                    display.setFrequency(frequency);
                }

                if (audio != null) {
                    audio.frequency = frequency;
                }
            }
            break;

            // Level
            case R.id.level:
                if (display != null) {
                    double level = Math.log10(progress / (double) MAX_LEVEL) * 20.0;

                    if (level < -80.0)
                        level = -80.0;

                    display.setLevel(level);
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
            // Sine
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

            // Square
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

            // Sawtooth
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

            // Mute
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

            // Back
            case R.id.back:
                if (bookmarks != null) {
                    try {
                        Collections.reverse(bookmarks);
                        for (double bookmark : bookmarks) {
                            if (bookmark < audio.frequency) {
                                animateBookmark(audio.frequency, bookmark);
                                break;
                            }
                        }
                    } finally {
                        Collections.sort(bookmarks);
                    }
                }
                break;

            // Forward
            case R.id.forward:
                if (bookmarks != null) {
                    for (double bookmark : bookmarks) {
                        if (bookmark > audio.frequency) {
                            animateBookmark(audio.frequency, bookmark);
                            break;
                        }
                    }
                }
                break;

            // Lower
            case R.id.lower:
                if (fine != null) {
                    int progress = fine.getProgress();
                    fine.setProgress(--progress);
                }
                break;

            // Higher
            case R.id.higher: {
                int progress = fine.getProgress();
                fine.setProgress(++progress);
            }
            break;
        }
    }

    private void animateBookmark(double start, double finish) {
        float value = (float) Math.log10(start / 10.0) * 200;
        float target = (float) Math.log10(finish / 10.0) * 200;

        // Start the animation
        ValueAnimator animator = ValueAnimator.ofFloat(value, target);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(this);
        animator.start();

        if (fine != null) {
            fine.setProgress(MAX_FINE / 2);
        }
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        float value = (Float) animation.getAnimatedValue();

        if (knob != null) {
            knob.setValue(value);
        }
    }

    void showToast(String text) {
        if (toast != null) {
            toast.cancel();
        }

        // Make a new one
        toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    private void checkBookmarks() {
        knob.postDelayed(() ->        {
            View back = findViewById(R.id.back);
            View forward = findViewById(R.id.forward);

            back.setEnabled(false);
            forward.setEnabled(false);

            if (bookmarks != null) {
                for (double bookmark : bookmarks) {
                    if (bookmark < audio.frequency) {
                        back.setEnabled(true);
                    }

                    if (bookmark > audio.frequency) {
                        forward.setEnabled(true);
                    }
                }
            }
        }, DELAY);
    }

    private void getPreferences() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (audio != null) {
            audio.duty = Float.parseFloat(preferences.getString(PREF_DUTY, "0.5"));
        }

        darkTheme = preferences.getBoolean(PREF_DARK_THEME, false);

        String string = preferences.getString(PREF_BOOKMARKS, "");

        try {
            JSONArray json = new JSONArray(string);
            bookmarks = new ArrayList<>();
            for (int i = 0; i < json.length(); i++)
                bookmarks.add(json.getDouble(i));

            checkBookmarks();
        } catch (Exception e) {
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
