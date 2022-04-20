package leicht.io.signor.utils

import leicht.io.signor.MainActivity
import kotlin.math.log10

fun calculateNewLevel(value: Float): Double {
    var level = log10(value / MainActivity.MAX_LEVEL.toDouble()) * 20.0
    if (level < -80.0) {
        level = -80.0
    }
    return level
}