/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
 *
 * Licensed under the Stream License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/GetStream/stream-video-android/blob/main/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.getstream.video.android.core.sounds

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
fun enableRingingCallVibrationConfig() = RingingCallVibrationConfig(enabled = true)

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
