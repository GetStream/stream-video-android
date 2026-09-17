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

package io.getstream.video.android.core.call.components

import io.getstream.video.android.core.call.utils.PreJoinMicrophoneRecorder

/**
 * Creates the [PreJoinMicrophoneRecorder] a call meters the microphone with before it is joined.
 *
 * Injected into [CallMediaManager] so tests can drive the start / stop handover without opening a
 * real microphone.
 *
 * @param onSamples receives 16 bit PCM buffers read from the microphone.
 */
internal fun interface PreJoinMicrophoneRecorderFactory {
    fun create(onSamples: (ByteArray) -> Unit): PreJoinMicrophoneRecorder
}
