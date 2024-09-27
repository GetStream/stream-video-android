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

/**
 * Interface for audio processing.
 */
public interface AudioProcessor {

    /**
     * True if the audio processing is enabled, false otherwise.
     */
    public var isEnabled: Boolean

    /**
     * Creates the audio processor.
     *
     * @return The native pointer to the audio processor.
     */
    public fun createNative(): Long

    /**
     * Destroys the audio processor.
     */
    public fun destroyNative()
}

/**
 * Toggles the audio processing on or off.
 *
 * @return True if the audio processing is enabled, false otherwise.
 */
internal fun AudioProcessor.toggle(): Boolean {
    isEnabled = !isEnabled
    return isEnabled
}