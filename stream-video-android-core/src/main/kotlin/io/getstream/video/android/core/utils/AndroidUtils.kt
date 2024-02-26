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

package io.getstream.video.android.core.utils

import android.app.Activity
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow

@JvmSynthetic
internal suspend fun Context.registerReceiverAsFlow(vararg actions: String): Flow<Intent> {
    return callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySendBlocking(intent)
            }
        }
        registerReceiver(
            receiver,
            IntentFilter().apply {
                actions.forEach {
                    addAction(it)
                }
            },
        )

        awaitClose {
            unregisterReceiver(receiver)
        }
    }.buffer(capacity = Channel.UNLIMITED)
}

internal val Context.notificationManager: NotificationManager
    @JvmSynthetic get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

internal val Context.vibrator: Vibrator
    @JvmSynthetic get() = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

internal val Context.vibratorManager: VibratorManager
    @RequiresApi(Build.VERSION_CODES.S)
    @JvmSynthetic
    get() = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager

public fun Activity.shouldShowRequestPermissionsRationale(permissions: Array<out String>): Boolean {
    return permissions.map {
        ActivityCompat.shouldShowRequestPermissionRationale(this, it)
    }.reduce { acc, value -> acc && value }
}

/**
 * Combine multiple comparators.
 *
 * @param comparators a list of comparators.
 */
internal fun <T> combineComparators(vararg comparators: Comparator<T>): Comparator<T> {
    return comparators.reduceRight { comparator, combined ->
        comparator.thenComparing(combined)
    }
}
