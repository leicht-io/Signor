package leicht.io.signor;

import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.navigation.NavigationBarView;
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

    private ExtendedFloatingActionButton playButton;
    private BottomNavigationView bottomNavigationView;

    private PhoneStateListener phoneStateListener;

    private AnimatedVectorDrawableCompat mPlayToPauseAnim;
    private AnimatedVectorDrawableCompat mPauseToPlay;

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

        bottomNavigationView = findViewById(R.id.bottom_navigation);

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
                bottomNavigationView.getChildAt(0).performClick();
                break;
            case Audio.SQUARE:
                bottomNavigationView.getChildAt(1).performClick();
                break;
            case Audio.SAWTOOTH:
                bottomNavigationView.getChildAt(2).performClick();
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
        bottomNavigationView.setOnItemSelectedListener((NavigationBarView.OnItemSelectedListener) item -> {
            switch (item.getItemId()) {
                case R.id.sine:
                    if (audio != null) {
                        audio.waveform = Audio.SINE;
                    }
                    break;
                case R.id.sawtooth:
                    if (audio != null) {
                        audio.waveform = Audio.SAWTOOTH;
                    }
                    break;
                case R.id.square:
                    if (audio != null) {
                        audio.waveform = Audio.SQUARE;
                    }
                    break;
            }

            return true;
        });

        playButton.setOnClickListener(view -> {
            if (audio != null) {
                audio.mute = !audio.mute;

                if (audio.mute) {
                    ((ExtendedFloatingActionButton) view).setText(R.string.start);
                    ((ExtendedFloatingActionButton) view).setIcon(mPauseToPlay);

                    mPauseToPlay.start();
                } else {
                    ((ExtendedFloatingActionButton) view).setText(R.string.stop);
                    ((ExtendedFloatingActionButton) view).setIcon(mPlayToPauseAnim);

                    mPlayToPauseAnim.start();
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
        mPlayToPauseAnim = AnimatedVectorDrawableCompat.create(this, R.drawable.play_to_pause);
        mPauseToPlay = AnimatedVectorDrawableCompat.create(this, R.drawable.pause_to_play);

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
