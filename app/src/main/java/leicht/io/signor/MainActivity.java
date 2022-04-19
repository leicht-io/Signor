package leicht.io.signor;

import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.google.android.material.slider.Slider;

import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {
    // TODO: Add to enum.
    private static final int MAX_LEVEL = 100;
    private static final int MAX_FINE = 1000;
    private static final String STATE = "state";
    private static final String KNOB = "knob";
    private static final String MUTE = "mute";
    private static final String WAVE = "wave";
    private static final String LEVEL = "level";
    private static final String FINE = "fine";

    private final DecimalFormat decimalFormat = new DecimalFormat("0.00");

    private Audio audio;
    private Knob knob;
    private TextView frequencyDisplay;
    private TextView volumeDisplay;

    private Slider volumeAdjust;
    private Slider fineAdjust;

    private ImageButton sineButton;
    private ImageButton sawtoothButton;
    private ImageButton squareButton;
    private ImageButton playButton;

    private PhoneStateListener phoneStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        frequencyDisplay = findViewById(R.id.frequencyDisplay);
        volumeDisplay = findViewById(R.id.volumeDisplay);

        knob = findViewById(R.id.knob);
        fineAdjust = findViewById(R.id.fineAdjust);
        volumeAdjust = findViewById(R.id.volumeAdjust);

        sineButton = findViewById(R.id.sine);
        sineButton.setSelected(true);

        sawtoothButton = findViewById(R.id.sawtooth);
        squareButton = findViewById(R.id.square);
        playButton = findViewById(R.id.play);

        initDefaultUi();
        setPhoneStateListener();
        setOnClickListeners();

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
        switch (waveform) {
            case Audio.SINE:
                sineButton.performClick();
                break;
            case Audio.SQUARE:
                squareButton.performClick();
                break;
            case Audio.SAWTOOTH:
                sawtoothButton.performClick();
                break;
        }

        boolean mute = bundle.getBoolean(MUTE, false);
        if (!mute) {
            playButton.performClick();
        }

        fineAdjust.setValue(bundle.getFloat(FINE, MAX_FINE / 2));
        volumeAdjust.setValue(bundle.getFloat(LEVEL, MAX_LEVEL / 10));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle bundle = new Bundle();

        bundle.putFloat(KNOB, knob.getValue());
        bundle.putInt(WAVE, audio.waveform);
        bundle.putBoolean(MUTE, audio.mute);
        bundle.putFloat(FINE, fineAdjust.getValue());
        bundle.putFloat(LEVEL, volumeAdjust.getValue());
        outState.putBundle(STATE, bundle);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            TelephonyManager manager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            manager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        } catch (Exception e) {
            // Do nothing
        }

        if (audio != null) {
            audio.stop();
        }
    }

    private void setOnClickListeners() {
        sineButton.setOnClickListener(view -> {
            if (audio != null) {
                audio.waveform = Audio.SINE;
                view.setSelected(true);

                squareButton.setSelected(false);
                sawtoothButton.setSelected(false);
            }
        });

        squareButton.setOnClickListener(view -> {
            if (audio != null) {
                audio.waveform = Audio.SQUARE;
                view.setSelected(true);

                sawtoothButton.setSelected(false);
                sineButton.setSelected(false);
            }
        });

        sawtoothButton.setOnClickListener(view -> {
            if (audio != null) {
                audio.waveform = Audio.SAWTOOTH;
                view.setSelected(true);

                squareButton.setSelected(false);
                sineButton.setSelected(false);
            }
        });

        playButton.setOnClickListener(view -> {
            if (audio != null) {
                audio.mute = !audio.mute;

                if (audio.mute) {
                    ((ImageButton) view).setImageResource(R.drawable.ic_action_play);
                } else {
                    ((ImageButton) view).setImageResource(R.drawable.ic_action_pause);
                }
            }
        });

        if (fineAdjust != null) {
            fineAdjust.addOnChangeListener((slider, value, fromUser) -> {
                double frequency = Math.pow(10.0, knob.getValue() / 200.0) * 10.0;
                double adjust = ((value - (double) (MAX_FINE / 2)) / (double) MAX_FINE) / 50.0;

                frequency += frequency * adjust;

                if (frequencyDisplay != null) {
                    frequencyDisplay.setText(String.format("%sHz", decimalFormat.format(frequency)));
                }

                if (audio != null) {
                    audio.frequency = frequency;
                }
            });

            fineAdjust.setValueTo(MAX_FINE);
            fineAdjust.setValue(MAX_FINE / 2);
        }

        if (volumeAdjust != null) {
            volumeAdjust.addOnChangeListener((slider, value, fromUser) -> {
                if (volumeDisplay != null) {
                    double level = Math.log10(value / (double) MAX_LEVEL) * 20.0;

                    if (level < -80.0)
                        level = -80.0;

                    volumeDisplay.setText(String.format("%sdB", decimalFormat.format(level)));
                }

                if (audio != null) {
                    audio.level = value / (double) MAX_LEVEL;
                }
            });

            volumeAdjust.setValueTo(MAX_LEVEL);
            volumeAdjust.setValue(MAX_LEVEL / 10);
        }
    }

    private void initDefaultUi() {
        audio = new Audio();
        audio.start();


        if (knob != null) {
            knob.setOnKnobChangeListener((knob, value) -> {
                double frequency = Math.pow(10.0, value / 200.0) * 10.0;
                double adjust = ((fineAdjust.getValue() - (double) (MAX_FINE / 2)) / (double) MAX_FINE) / 100.0;

                frequency += frequency * adjust;

                if (frequencyDisplay != null) {
                    frequencyDisplay.setText(String.format("%sHz", decimalFormat.format(frequency)));
                }

                if (audio != null) {
                    audio.frequency = frequency;
                }
            });

            knob.setValue(400);
        }
    }

    private void setPhoneStateListener() {
        // TODO: Change to TelephonyCallback instead. See: https://developer.android.com/reference/android/telephony/TelephonyCallback
        phoneStateListener = new PhoneStateListener() {
            public void onCallStateChanged(int state, String incomingNumber) {
                if (state != TelephonyManager.CALL_STATE_IDLE) {
                    if (!audio.mute) {
                        View view = findViewById(R.id.play);
                        view.performClick();
                    }
                }
            }

        };

        try {
            TelephonyManager manager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            manager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        } catch (Exception e) {
            // Do nothing
        }
    }
}
