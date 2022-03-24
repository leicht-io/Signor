package com.example.signor.ui.dashboard

import android.graphics.Color
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
import kotlin.math.sin

class DashboardFragment : Fragment() {
    private lateinit var audioTrack: AudioTrack

    private val sampleRate: Int = 44100
    private val bufferLength: Int = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    private var fragmentDashboardBinding: FragmentDashboardBinding? = null
    private var isPlaying: Boolean = false

    private val binding get() = fragmentDashboardBinding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentDashboardBinding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val playBtn: Button = binding.PlayBtn;

        binding.siriView.apply {
            updateSpeaking(false)
            updateViewColor(Color.BLACK)
            updateAmplitude(0.5f)
            updateSpeed(-0.1f)
        }

        playBtn.setOnClickListener {
            if (!isPlaying) {
                Thread {
                    initTrack()
                    startPlaying()
                    playback()

                }.start()

                binding.siriView.apply {
                    updateSpeaking(true)
                }
            } else {
                stopPlaying()

                binding.siriView.apply {
                    updateSpeaking(true)
                }
            }
        }

        return root
    }

    private fun initTrack() {
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(sampleRate)
            .build()
    }

    private fun playback() {
        val frameOut: ShortArray = ShortArray(bufferLength)
        val amplitude: Int = 32767 // Volume
        val frequency: Int = 30 // 30hz
        val twoPi: Double = PI * 2
        var phase: Double = 0.0

        while (isPlaying) {
            for (i in 0 until bufferLength) {
                frameOut[i] = (amplitude * sin(phase)).toInt().toShort()
                phase += twoPi * frequency / sampleRate
                if (phase > twoPi) {
                    phase -= twoPi
                }
            }

            audioTrack.write(frameOut, 0, bufferLength)
        }
    }

    private fun startPlaying() {
        audioTrack.play()
        isPlaying = true
    }

    private fun stopPlaying() {
        if (isPlaying) {
            isPlaying = false

            audioTrack.stop()
            audioTrack.release()
        }
    }

    override fun onDestroyView() {
        stopPlaying();

        super.onDestroyView()
        fragmentDashboardBinding = null
    }
}