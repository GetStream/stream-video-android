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

package io.getstream.video.android.core.notifications.internal.service.telecom

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.telecom.CallEndpoint
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.annotation.RequiresApi
import io.getstream.video.android.core.R
import io.getstream.video.android.core.audio.StreamAudioDevice

fun isTelecomIntegrationAvailable(context: Context): Boolean {
    try {
        context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager ?: return false
        return true
    } catch (e: Error) {
        return false
    }
}

fun getMyPhoneAccountHandle(context: Context): PhoneAccountHandle {
    val componentName = ComponentName(
        context,
        TelecomCallService::class.java,
    )
    val accountId = context.packageName
    return PhoneAccountHandle(componentName, accountId)
}

fun promptEnablePhoneAccount(context: Context) {
    try {
        val intent = Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS)
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback to generic Settings if Telecom settings unavailable
        context.startActivity(Intent(Settings.ACTION_SETTINGS))
    }
}

fun registerMyPhoneAccount(context: Context) {
    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
    val phoneAccountHandle = getMyPhoneAccountHandle(context)

    // Build the PhoneAccount
    var capabilities = PhoneAccount.CAPABILITY_CALL_PROVIDER or PhoneAccount.CAPABILITY_VIDEO_CALLING

    if (Build.VERSION.SDK_INT > 26) {
        capabilities = PhoneAccount.CAPABILITY_SELF_MANAGED
    }

    val phoneAccount = PhoneAccount.builder(
        phoneAccountHandle,
        "Stream calls", // Visible to the user in the system settings
    )
        // Capabilities define what kind of calls this account supports
        .setCapabilities(capabilities)
        // Optionally set an icon (for display in system dialer)
        .setIcon(Icon.createWithResource(context, R.drawable.stream_video_ic_call))
        .setShortDescription("My VoIP calls")
        .build()

    telecomManager.registerPhoneAccount(phoneAccount)
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
fun CallEndpoint.toStreamAudioDevice(): StreamAudioDevice = when (this.endpointType) {
    CallEndpoint.TYPE_BLUETOOTH -> StreamAudioDevice.BluetoothHeadset(telecomDevice = this)
    CallEndpoint.TYPE_EARPIECE -> StreamAudioDevice.Earpiece(telecomDevice = this)
    CallEndpoint.TYPE_SPEAKER -> StreamAudioDevice.Speakerphone(telecomDevice = this)
    CallEndpoint.TYPE_WIRED_HEADSET -> StreamAudioDevice.WiredHeadset(telecomDevice = this)
    else -> StreamAudioDevice.Earpiece()
}
