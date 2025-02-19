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

package io.getstream.video.android.core.telecom

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallEndpointCompat
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.audio.StreamAudioDevice
import io.getstream.video.android.model.StreamCallId

internal enum class TelecomCallState {
    IDLE,
    INCOMING,
    OUTGOING,
    ONGOING,
}

enum class TelecomEvent {
    ANSWER,
    DISCONNECT,
    SET_ACTIVE,
    SET_INACTIVE,
}

internal fun Call.buildStreamCallId() = StreamCallId.fromCallCid(this.cid)

internal typealias StreamCall = Call

internal typealias DeviceListener = (available: List<StreamAudioDevice>, selected: StreamAudioDevice?) -> Unit

@RequiresApi(Build.VERSION_CODES.O)
internal fun CallEndpointCompat.toStreamAudioDevice(): StreamAudioDevice = when (this.type) {
    CallEndpointCompat.TYPE_BLUETOOTH -> StreamAudioDevice.BluetoothHeadset(telecomDevice = this)
    CallEndpointCompat.TYPE_EARPIECE -> StreamAudioDevice.Earpiece(telecomDevice = this)
    CallEndpointCompat.TYPE_SPEAKER -> StreamAudioDevice.Speakerphone(telecomDevice = this)
    CallEndpointCompat.TYPE_WIRED_HEADSET -> StreamAudioDevice.WiredHeadset(telecomDevice = this)
    else -> StreamAudioDevice.Earpiece()
}
