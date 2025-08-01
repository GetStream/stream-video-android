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
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.ui.common.util.StreamCallActivityDelicateApi

public interface ActivityCallOperations {

    @StreamCallActivityDelicateApi
    public fun create(
        call: Call,
        ring: Boolean,
        members: List<String>,
        onSuccess: (suspend (Call) -> Unit)? = null,
        onError: (suspend (Exception) -> Unit)? = null,
    )

    @StreamCallActivityDelicateApi
    public fun call(
        cid: StreamCallId,
        onSuccess: ((Call) -> Unit)? = null,
        onError: ((Exception) -> Unit)? = null,
    )

    @StreamCallActivityDelicateApi
    public fun leave(
        call: Call,
        onSuccess: (suspend (Call) -> Unit)? = null,
        onError: (suspend (Exception) -> Unit)? = null,
    )

    @StreamCallActivityDelicateApi
    public fun end(
        call: Call,
        onSuccess: (suspend (Call) -> Unit)? = null,
        onError: (suspend (Exception) -> Unit)? = null,
    )

    @StreamCallActivityDelicateApi
    public fun cancel(
        call: Call,
        onSuccess: (suspend (Call) -> Unit)? = null,
        onError: (suspend (Exception) -> Unit)? = null,
    )

    public fun reject(
        call: Call,
        reason: RejectReason? = null,
        onSuccess: (suspend (Call) -> Unit)? = null,
        onError: (suspend (Exception) -> Unit)? = null,
    )

    @StreamCallActivityDelicateApi
    public fun accept(
        call: Call,
        onSuccess: (suspend (Call) -> Unit)? = null,
        onError: (suspend (Exception) -> Unit)? = null,
    )

    @StreamCallActivityDelicateApi
    public fun join(
        call: Call,
        onSuccess: (suspend (Call) -> Unit)? = null,
        onError: (suspend (Exception) -> Unit)? = null,
    )

    @StreamCallActivityDelicateApi
    public fun get(
        call: Call,
        onSuccess: (suspend (Call) -> Unit)? = null,
        onError: (suspend (Exception) -> Unit)? = null,
    )
}
