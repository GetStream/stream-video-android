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

package io.getstream.video.android.core.call.audio

import java.nio.ByteBuffer

@Deprecated(
    message = "Use InputAudioFilter instead",
    replaceWith = ReplaceWith(
        expression = "InputAudioFilter",
        imports = ["io.getstream.video.android.core.call.audio.InputAudioFilter"]
    )
)
public fun interface AudioFilter : InputAudioFilter

/**
 * Manipulates the audio data before it's fed into WebRTC.
 */
public fun interface InputAudioFilter {
    /**
     * Invoked after an audio sample is recorded. Can be used to manipulate
     * the ByteBuffer before it's fed into WebRTC. Currently the audio in the
     * ByteBuffer is always PCM 16bit and the buffer sample size is ~10ms.
     *
     * @param audioFormat format in android.media.AudioFormat
     */
    public fun applyFilter(
        audioFormat: Int,
        channelCount: Int,
        sampleRate: Int,
        sampleData: ByteBuffer,
    )
}
