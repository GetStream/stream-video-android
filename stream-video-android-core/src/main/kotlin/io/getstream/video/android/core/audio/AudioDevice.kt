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

package io.getstream.video.android.core.audio

public sealed class AudioDevice {

    /** The friendly name of the device.*/
    public abstract val name: String

    /** An [AudioDevice] representing a Bluetooth Headset.*/
    public data class BluetoothHeadset internal constructor(override val name: String = "Bluetooth") :
        AudioDevice()

    /** An [AudioDevice] representing a Wired Headset.*/
    public data class WiredHeadset internal constructor(override val name: String = "Wired Headset") :
        AudioDevice()

    /** An [AudioDevice] representing the Earpiece.*/
    public data class Earpiece internal constructor(override val name: String = "Earpiece") :
        AudioDevice()

    /** An [AudioDevice] representing the Speakerphone.*/
    public data class Speakerphone internal constructor(override val name: String = "Speakerphone") :
        AudioDevice()
}
