package leicht.io.signor.utils

import leicht.io.signor.MainActivity
import kotlin.math.pow

fun calculateNewFrequency(value: Float, knobValue: Float): Double {
    var frequency = (10.0).pow(knobValue / 200.0) * 10.0
    val adjust =
        (value - (MainActivity.MAX_FINE / 2).toDouble()) / MainActivity.MAX_FINE.toDouble() / 50.0
    frequency += frequency * adjust
    return frequency
}