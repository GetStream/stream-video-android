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

import kotlin.jvm.Throws

/**
 * Controls when a ringing call transitions to [RingingState.Active].
 *
 * Implement this to insert custom logic (e.g. waiting for user confirmation) between
 * the publisher peer connection becoming ready and the call going active.
 * Has no effect on non-ringing joins (livestream, direct join).
 */
public interface CallJoinInterceptor {

    /**
     * Called when the SDK is ready to transition to [RingingState.Active].
     * Suspend here to delay the transition; return to allow it to proceed.
     *
     * Throw [CallJoinInterceptionException] to abort the join — the SDK will leave
     * the call cleanly
     *
     * The SDK enforces a 5-second maximum — the transition proceeds automatically on timeout.
     */
    @Throws(CallJoinInterceptionException::class)
    public suspend fun callReadyToJoin(call: Call)
}

/**
 * Thrown by a [CallJoinInterceptor] to abort the join.
 *
 * When raised inside [CallJoinInterceptor.callReadyToJoin], the SDK leaves the call
 * cleanly using [reason] for tracing.
 */
public class CallJoinInterceptionException(
    public val reason: String,
    cause: Throwable? = null,
) : RuntimeException(reason, cause)
