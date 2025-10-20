package io.getstream.video.android.core.sounds

import io.getstream.video.android.core.internal.InternalStreamVideoApi

/**
 * Configuration for the vibration pattern and enabled state for ringing calls.
 *
 * @param vibratePattern The vibration pattern to use. Defaults to a pattern of 0, 1000, 500, 1000.
 * @param enabled Whether vibration should be enabled. Defaults to false.
 */
public data class RingingCallVibrationConfig(
    val vibratePattern: LongArray = longArrayOf(0, 1000, 500, 1000),
    val enabled: Boolean = false,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RingingCallVibrationConfig

        if (enabled != other.enabled) return false
        if (!vibratePattern.contentEquals(other.vibratePattern)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = enabled.hashCode()
        result = 31 * result + vibratePattern.contentHashCode()
        return result
    }
}

/**
 * Returns a default ringing call vibration config.
 */
fun enableRingingCallVibrationConfig() = RingingCallVibrationConfig()

/**
 * Returns a ringing call vibration config that mutes (disables) vibration.
 */
fun disableVibrationConfig() = RingingCallVibrationConfig(enabled = false)

/**
 * Returns a ringing call vibration config with a custom vibration pattern.
 *
 * @see android.os.Vibrator#vibrate(long[], int)
 */
fun customVibrationPatternConfig(vibratePattern: LongArray) =
    RingingCallVibrationConfig(vibratePattern = vibratePattern, enabled = true)
