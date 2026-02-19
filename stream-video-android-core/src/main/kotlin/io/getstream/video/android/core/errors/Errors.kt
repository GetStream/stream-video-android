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

package io.getstream.video.android.core.errors

/**
 * Triggered if for whatever reason we fail to access the camera
 * There are dozens of underlying reasons for why the camera can fail
 * We catch them and wrap them in this exception
 */
public class CameraException(
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * Triggered if for whatever reason we fail to access the microphone
 */
public class MicrophoneException(
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * Wraps any exception that occurs in the RTC layer
 * With a recommendation if we should retry the current SFU, stop
 * or switch to a different one
 */
public class RtcException(
    message: String? = null,
    cause: Throwable? = null,
    retryCurrentSfu: Boolean = false,
    switchSfu: Boolean = false,
    error: stream.video.sfu.models.Error? = null,
) : Exception(message, cause)
