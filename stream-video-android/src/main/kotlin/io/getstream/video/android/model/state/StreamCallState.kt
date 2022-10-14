/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.model.state

import io.getstream.video.android.errors.VideoError
import io.getstream.video.android.model.CallDetails
import io.getstream.video.android.model.CallInfo
import io.getstream.video.android.model.CallUser
import io.getstream.video.android.model.JoinedCall

public sealed interface StreamCallState : java.io.Serializable {

    public object Idle : StreamCallState

    public interface Active : StreamCallState

    public data class Starting(
        val users: Map<String, CallUser>,
    ) : Active

    public data class Outgoing(
        val callCid: String,
        val users: Map<String, CallUser>,
        val info: CallInfo,
        val details: CallDetails
    ) : Active

    public data class Incoming(
        val callCid: String,
        val users: Map<String, CallUser>,
        val info: CallInfo,
        val details: CallDetails
    ) : Active

    public data class InCall(
        val joinedCall: JoinedCall
    ) : Active

    public data class Drop(
        val reason: DropReason
    ) : StreamCallState
}

public sealed class DropReason {
    public data class Timeout(val waitMillis: Long) : DropReason()
    public data class Failure(val error: VideoError) : DropReason()
    public object Rejected : DropReason()
    public object Cancelled : DropReason()
}
