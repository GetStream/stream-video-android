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

import androidx.annotation.RawRes
import io.getstream.video.android.core.R

/**
 * Contains all the sounds that the SDK uses.
 *
 * @param incomingCallSound Resource used as a ringtone for incoming calls.
 * @param outgoingCallSound Resource used as a ringing tone for outgoing calls.
 */
data class Sounds(
    @RawRes val incomingCallSound: Int? = R.raw.call_incoming_sound,
    @RawRes val outgoingCallSound: Int? = R.raw.call_outgoing_sound,
)
