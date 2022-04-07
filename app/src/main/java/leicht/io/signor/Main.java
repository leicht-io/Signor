package leicht.io.signor;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import java.text.DecimalFormat;

public class Main extends Activity implements Knob.OnKnobChangeListener, SeekBar.OnSeekBarChangeListener,
        View.OnClickListener, ValueAnimator.AnimatorUpdateListener {

    private static final int MAX_LEVEL = 100;
    private static final int MAX_FINE = 1000;

    private static final String STATE = "state";

    private static final String KNOB = "knob";
    private static final String MUTE = "mute";
    private static final String WAVE = "wave";
    private static final String LEVEL = "level";
    private static final String FINE = "fine";

    private Audio audio;
    private final DecimalFormat decimalFormat = new DecimalFormat("0.00");
    private Knob knob;
    private TextView frequencyDisplay;
    private TextView volumeDisplay;

    private SeekBar level;
    private SeekBar fine;

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

        audio = new Audio();
        audio.start();

        setupWidgets();

        setupPhoneStateListener();

        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        }
    }

    private void restoreState(Bundle savedInstanceState) {
        Bundle bundle = savedInstanceState.getBundle(STATE);

        if (knob != null) {
            knob.setValue(bundle.getFloat(KNOB, 400));
        }

        int waveform = bundle.getInt(WAVE, Audio.SINE);

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

        if(view != null) {
            onClick(view);
        }

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
            // Do nothing
        }

        if (audio != null) {
            audio.stop();
        }
    }

    @Override
    public void onKnobChange(Knob knob, float value) {
        double frequency = Math.pow(10.0, value / 200.0) * 10.0;
        double adjust = ((fine.getProgress() - (double) (MAX_FINE / 2)) / (double) MAX_FINE) / 100.0;

        frequency += frequency * adjust;

        if (frequencyDisplay != null) {
            frequencyDisplay.setText(String.format("%sHz", decimalFormat.format(frequency)));
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

        switch (id) {
            case R.id.fine: {
                double frequency = Math.pow(10.0, knob.getValue() / 200.0) * 10.0;
                double adjust = ((progress - (double) (MAX_FINE / 2)) / (double) MAX_FINE) / 50.0;

                frequency += frequency * adjust;

                if (frequencyDisplay != null) {
                    frequencyDisplay.setText(String.format("%sHz", decimalFormat.format(frequency)));
                }

                if (audio != null) {
                    audio.frequency = frequency;
                }
            }
            break;
            case R.id.level:
                if (volumeDisplay != null) {
                    double level = Math.log10(progress / (double) MAX_LEVEL) * 20.0;

                    if (level < -80.0)
                        level = -80.0;

                    volumeDisplay.setText(String.format("%sdB", decimalFormat.format(level)));
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

                // TODO: Change icons on white round button
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
            if (view != null) {
                view.setOnClickListener(knob);
            }

            view = findViewById(R.id.next);
            if (view != null) {
                view.setOnClickListener(knob);
            }
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
        // TODO: Change to TelephonyCallback instead. See: https://developer.android.com/reference/android/telephony/TelephonyCallback
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
            // Do nothing
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }
}
