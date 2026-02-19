/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core

public interface StreamVideoConfig {

    public val dropTimeout: Long
    public val cancelOnTimeout: Boolean
    public val joinOnAcceptedByCallee: Boolean

    public val autoPublish: Boolean
    public val defaultAudioOn: Boolean
    public val defaultVideoOn: Boolean
    public val defaultSpeakerPhoneOn: Boolean
}

public object StreamVideoConfigDefault : StreamVideoConfig {
    override val dropTimeout: Long = 30_000L
    override val cancelOnTimeout: Boolean = true
    override val joinOnAcceptedByCallee: Boolean = true

    override val autoPublish: Boolean = true
    override val defaultAudioOn: Boolean = false
    override val defaultVideoOn: Boolean = true
    override val defaultSpeakerPhoneOn: Boolean = false
}
