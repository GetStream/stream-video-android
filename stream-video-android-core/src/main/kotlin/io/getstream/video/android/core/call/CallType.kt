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

package io.getstream.video.android.core.call

sealed class CallType(val name: String) {
    object Livestream : CallType("livestream")
    object AudioCall : CallType("audio_call")
    object Default : CallType("default")
    object AnyMarker : CallType("ALL_CALL_TYPES")

    // Allows adding custom call types dynamically
    class CustomCallType(name: String) : CallType(name)

    override fun toString(): String {
        return name
    }

    companion object {
        fun fromName(name: String): CallType? {
            return listOf(Livestream, AudioCall, Default, AnyMarker).find { it.name == name }
        }
    }
}
