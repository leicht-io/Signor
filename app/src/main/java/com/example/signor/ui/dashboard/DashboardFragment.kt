package com.example.signor.ui.dashboard

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.signor.databinding.FragmentDashboardBinding
import java.lang.Math.PI

class DashboardFragment : Fragment() {
    lateinit var Track: AudioTrack
    private var _binding: FragmentDashboardBinding? = null
    var isPlaying: Boolean = false
    val Fs: Int = 44100
    val buffLength: Int = AudioTrack.getMinBufferSize(Fs, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)

    // This property is only valid between onCreateView and onDestroyView.
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
                Thread {
                    initTrack()
                    startPlaying()
                    playback()

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

    private fun playback() {
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