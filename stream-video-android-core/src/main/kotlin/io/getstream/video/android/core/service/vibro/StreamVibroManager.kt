/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.service.vibro

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.RequiresApi
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.utils.vibrator
import io.getstream.video.android.core.utils.vibratorManager

public interface StreamVibroManager {

    public fun vibrateIncoming()
    public fun cancel()
}

public class StreamVibroManagerImpl(
    private val context: Context
) : StreamVibroManager {

    private val logger by taggedLogger("Call:VibroManager")

    private val incomingPattern = longArrayOf(0, 100, 200, 300, 400)

    public override fun vibrateIncoming(): Unit = with(context) {
        logger.d { "[vibrateIncoming] no args" }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            vibratorManager.defaultVibrator.vibrateCompat(incomingPattern, REPEAT_FROM_ZERO_INDEX)
        } else {
            vibrator.vibrateCompat(incomingPattern, REPEAT_FROM_ZERO_INDEX)
        }
    }

    public override fun cancel(): Unit = with(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            vibratorManager.defaultVibrator.cancel()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.cancel()
        } else {
            vibrator.cancel()
        }
    }

    private fun Vibrator.vibrateCompat(pattern: LongArray, repeat: Int) {
        if (!hasVibrator()) {
            logger.w { "[vibrateCompat] rejected (no vibrator)" }
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrate(createVibrationEffect(pattern, repeat))
        } else {
            vibrate(pattern, repeat)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createVibrationEffect(pattern: LongArray, repeat: Int): VibrationEffect {
        return VibrationEffect.createWaveform(pattern, repeat)
    }

    private companion object {
        private const val REPEAT_FROM_ZERO_INDEX = 0
    }
}
