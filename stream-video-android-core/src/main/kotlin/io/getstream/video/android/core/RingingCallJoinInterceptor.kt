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

/**
 * Controls when a ringing call transitions to [RingingState.Active].
 *
 * Implement this to insert custom logic (e.g. waiting for user confirmation) between
 * the publisher peer connection becoming ready and the call going active.
 * Has no effect on non-ringing joins (livestream, direct join).
 *
 * @see DefaultRingingCallJoinInterceptor
 */
public interface RingingCallJoinInterceptor {

    /**
     * Called when the SDK is ready to transition to [RingingState.Active].
     * Suspend here to delay the transition; return to allow it to proceed.
     *
     * The SDK enforces a 5-second maximum — the transition proceeds automatically on timeout.
     */
    public suspend fun callReadyToJoinWithTimeout(call: Call)
}

/**
 * Default implementation that proceeds to [RingingState.Active] immediately.
 */
public object DefaultRingingCallJoinInterceptor : RingingCallJoinInterceptor {
    override suspend fun callReadyToJoinWithTimeout(call: Call) {}
}
