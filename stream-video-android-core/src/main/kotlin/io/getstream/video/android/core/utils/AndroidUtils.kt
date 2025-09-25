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

@file:OptIn(ExperimentalContracts::class)

package io.getstream.video.android.core.utils

import android.app.Activity
import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import io.getstream.log.StreamLog
import io.getstream.result.Error
import io.getstream.result.Result
import io.getstream.video.android.core.notifications.internal.service.CallService.Companion.TRIGGER_INCOMING_CALL
import io.getstream.video.android.core.notifications.internal.service.CallService.Companion.TRIGGER_ONGOING_CALL
import io.getstream.video.android.core.notifications.internal.service.CallService.Companion.TRIGGER_OUTGOING_CALL
import io.getstream.video.android.core.screenshare.StreamScreenShareService.Companion.TRIGGER_SHARE_SCREEN
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

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

/**
 * Safely call a suspending function and handle exceptions.
 *
 * @param block the suspending function to call.
 */
internal suspend fun safeSuspendingCall(block: suspend () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    try {
        block()
    } catch (e: Exception) {
        // Handle or log the exception here
        StreamLog.e("SafeSuspendingCall", e) { "Exception occurred: ${e.message}" }
    }
}

/**
 * Safely call a function and handle exceptions.
 *
 * @param block the function to call.
 */
internal inline fun safeCall(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    try {
        block()
    } catch (e: Exception) {
        // Handle or log the exception here
        StreamLog.e("SafeCall", e) { "Exception occurred: ${e.message}" }
    }
}

/**
 * Safely call a suspending function and handle exceptions.
 *
 * @param default the default value to return in case of an exception.
 * @param block the suspending function to call.
 */
internal suspend fun <T> safeSuspendingCallWithDefault(
    default: T,
    defaultProvider: ((exception: Throwable) -> T)? = null,
    block: suspend () -> T,
): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return try {
        block()
    } catch (e: Throwable) {
        // Handle or log the exception here
        StreamLog.e("SafeSuspendingCall", e) { "Exception occurred: ${e.message}" }
        safeCallWithDefault(default) { defaultProvider?.invoke(e) ?: default }
    }
}

/**
 * Safely call a function and handle exceptions.
 *
 * @param default the default value to return in case of an exception.
 * @param block the function to call.
 */
internal inline fun <T> safeCallWithDefault(default: T, block: () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return try {
        block()
    } catch (e: Exception) {
        // Handle or log the exception here
        StreamLog.e("SafeCall", e) { "Exception occurred: ${e.message}" }
        default
    }
}

/**
 * Start a foreground service with a service type to meet requirements introduced in Android 14.
 *
 * @param notificationId The notification ID
 * @param notification The notification to show
 * @param trigger The trigger that started the service: [TRIGGER_ONGOING_CALL], [TRIGGER_OUTGOING_CALL], [TRIGGER_INCOMING_CALL], [TRIGGER_SHARE_SCREEN]
 */
internal fun Service.startForegroundWithServiceType(
    notificationId: Int,
    notification: Notification,
    trigger: String,
    foregroundServiceType: Int = ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL,
) = safeCallWithResult {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        startForeground(notificationId, notification)
        Log.d(
            "AndroidUtils",
            "[startForegroundWithServiceType] 1 startForeground notificationId: $notificationId",
        )
    } else {
        val beforeOrAfterAndroid14Type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE // TODO RAHUL, THIS NEEDS TO BE REMOVED ELSE THE SERVICE WILL KILL ITSELF IN 10 MINS
        } else {
            foregroundServiceType
        }

        ServiceCompat.startForeground(
            this,
            notificationId,
            notification,
            when (trigger) {
                TRIGGER_ONGOING_CALL -> foregroundServiceType
                TRIGGER_OUTGOING_CALL, TRIGGER_INCOMING_CALL -> beforeOrAfterAndroid14Type
                TRIGGER_SHARE_SCREEN -> ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                else -> beforeOrAfterAndroid14Type
            },
        )
        Log.d(
            "AndroidUtils",
            "[startForegroundWithServiceType] 2 startForeground notificationId: $notificationId",
        )
    }
}

/**
 * Safely call a function and handle exceptions while returning a [Result].
 */
internal inline fun <T : Any> safeCallWithResult(block: () -> T): Result<T> {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return try {
        Result.Success(block())
    } catch (e: Exception) {
        // Handle or log the exception here
        StreamLog.e("SafeCall", e) { "Exception occurred: ${e.message}" }
        Result.Failure(Error.ThrowableError("Safe call failed with ${e.message}", e))
    }
}

/**
 * Safely call a function and handle exceptions while returning a [Result].
 */
internal suspend fun <T : Any> safeSuspendingCallWithResult(block: suspend () -> T): Result<T> {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return try {
        Result.Success(block())
    } catch (e: Exception) {
        // Handle or log the exception here
        StreamLog.e("SafeCall", e) { "Exception occurred: ${e.message}" }
        Result.Failure(Error.ThrowableError("Safe call failed with ${e.message}", e))
    }
}

internal fun isAppInForeground(): Boolean {
    return try {
        ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    } catch (e: IllegalStateException) {
        false // fallback if lifecycle isn't initialized yet
    }
}
