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

package io.getstream.video.android.ui.common

import io.getstream.video.android.core.Call
import io.getstream.video.android.core.model.RejectReason
import io.getstream.video.android.ui.common.util.StreamCallActivityDelicateApi

public interface ActivityCallOperations {

    @StreamCallActivityDelicateApi
    public fun createCall(
        call: Call,
        ring: Boolean,
        members: List<String>,
        onSuccess: (suspend (Call) -> Unit)? = null,
        onError: (suspend (StreamCallActivityError) -> Unit)? = null,
    )

    @StreamCallActivityDelicateApi
    public fun leaveCall(
        call: Call,
        onSuccess: (suspend (Call) -> Unit)? = null,
        onError: (suspend (StreamCallActivityError) -> Unit)? = null,
    )

    @StreamCallActivityDelicateApi
    public fun endCall(
        call: Call,
        onSuccess: (suspend (Call) -> Unit)? = null,
        onError: (suspend (StreamCallActivityError) -> Unit)? = null,
    )

    @StreamCallActivityDelicateApi
    public fun cancelCall(
        call: Call,
        onSuccess: (suspend (Call) -> Unit)? = null,
        onError: (suspend (StreamCallActivityError) -> Unit)? = null,
    )

    public fun rejectCall(
        call: Call,
        reason: RejectReason? = null,
        onSuccess: (suspend (Call) -> Unit)? = null,
        onError: (suspend (StreamCallActivityError) -> Unit)? = null,
    )

    @StreamCallActivityDelicateApi
    public fun acceptCall(
        call: Call,
        onSuccess: (suspend (Call) -> Unit)? = null,
        onError: (suspend (StreamCallActivityError) -> Unit)? = null,
    )

    @StreamCallActivityDelicateApi
    public fun joinCall(
        call: Call,
        onSuccess: (suspend (Call) -> Unit)? = null,
        onError: (suspend (StreamCallActivityError) -> Unit)? = null,
    )

    @StreamCallActivityDelicateApi
    public fun getCall(
        call: Call,
        onSuccess: (suspend (Call) -> Unit)? = null,
        onError: (suspend (StreamCallActivityError) -> Unit)? = null,
    )
}
