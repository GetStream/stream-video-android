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

package io.getstream.video.android.input.internal

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import io.getstream.log.StreamLog
import io.getstream.video.android.input.CallActivityInput
import io.getstream.video.android.input.CallAndroidInput
import io.getstream.video.android.input.CallAndroidInputLauncher
import io.getstream.video.android.input.CallServiceInput

internal object DefaultCallAndroidInputLauncher : CallAndroidInputLauncher {
    private const val TAG = "Call:Input-Launcher"

    override fun launch(context: Context, input: CallAndroidInput) {
        when (input) {
            is CallActivityInput -> context.start(input)
            is CallServiceInput -> context.start(input)
        }
    }

    fun Context.start(input: CallAndroidInput) {
        when (input) {
            is CallActivityInput -> start(input)
            is CallServiceInput -> start(input)
        }
    }

    fun Context.start(input: CallActivityInput) {
        StreamLog.d(TAG) { "/start/ activity: ${input.className}" }
        startActivity(
            newIntent(input.className).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        )
    }

    fun Context.start(input: CallServiceInput) {
        StreamLog.d(TAG) { "/start/ service: ${input.className}" }
        ContextCompat.startForegroundService(this, newIntent(input.className))
    }

    private fun Context.newIntent(className: String): Intent {
        StreamLog.v(TAG) { "/newIntent/ className: $className" }
        return Intent().apply {
            component = ComponentName(packageName, className)
        }
    }
}
