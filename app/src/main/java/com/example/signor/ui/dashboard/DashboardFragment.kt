package com.example.signor.ui.dashboard

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.signor.databinding.FragmentDashboardBinding
import com.github.nisrulz.zentone.ZenTone
import java.lang.Math.PI


class DashboardFragment : Fragment() {
    lateinit var Track: AudioTrack
    private var _binding: FragmentDashboardBinding? = null
    var isPlaying: Boolean = false
    val Fs: Int = 44100
    val buffLength: Int = AudioTrack.getMinBufferSize(Fs, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val playBtn: Button = binding.PlayBtn;
        playBtn.setOnClickListener {
            if (!isPlaying) {
                // Create a new thread to play the audio.

                // Performing intensive operations and computations on the main UI thread,
                // makes the app slow.

                // That is, it is a bad idea to do intensive computations on main UI thread,
                // so it is recommended to create a new thread to do computations in the background

                Thread {
                    val zenTone = ZenTone(channelMask = AudioFormat.CHANNEL_OUT_STEREO)
                    zenTone.play(frequency = 30f, volume = 2)

                    /* initTrack()
                    startPlaying()
                    playback() */

                }.start()
            }
        }


        val stopBtn: Button = binding.StopBtn;

        stopBtn.setOnClickListener {
            stopPlaying()
        }

        return root
    }
    private fun initTrack() {
        Track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(44100)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(Fs)
            .build()
    }

    private fun generateTone(freqHz: Double, durationMs: Int): AudioTrack? {
        val count = (44100.0 * 2.0 * (durationMs / 1000.0)).toInt() and 1.inv()
        val samples = ShortArray(count)
        var i = 0
        while (i < count) {
            val sample = (Math.sin(2 * PI * i / (44100.0 / freqHz)) * 0x7FFF).toInt().toShort()
            samples[i + 0] = sample
            samples[i + 1] = sample
            i += 2
        }
        val track = AudioTrack(
            AudioManager.STREAM_MUSIC, 44100,
            AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
            count * (java.lang.Short.SIZE / 8), AudioTrack.MODE_STATIC
        )
        track.write(samples, 0, count)
        return track
    }

    private fun playback() {
        // simple sine wave generator
        val frame_out: ShortArray = ShortArray(buffLength)
        val amplitude: Int = 32767
        val frequency: Int = 30
        val twopi: Double = PI * 2
        var phase: Double = 0.0

        while (isPlaying) {
            for (i in 0 until buffLength) {
                frame_out[i] = (amplitude * Math.sin(phase)).toInt().toShort()
                phase += twopi * frequency / Fs
                if (phase > twopi) {
                    phase -= twopi
                }
            }

            Track.write(frame_out, 0, buffLength)
        }
    }

    private fun startPlaying() {
        Track.play()
        isPlaying = true
    }

    private fun stopPlaying() {
        if (isPlaying) {
            isPlaying = false

            Track.stop()
            Track.release()
        }
    }

    override fun onDestroyView() {
        stopPlaying();

        super.onDestroyView()
        _binding = null
    }
}