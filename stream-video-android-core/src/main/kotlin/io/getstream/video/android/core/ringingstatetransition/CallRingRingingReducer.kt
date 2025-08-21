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

package io.getstream.video.android.core.ringingstatetransition

import io.getstream.android.video.generated.models.CallRingEvent
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient

/**
 * Changing to RingingState is done on the Service class because
 * For incoming call - we have 2 sources
 * - PN - we update the state in [io.getstream.video.android.core.notifications.internal.service.CallService.updateRingingCall]
 * - WS - we update the state in [io.getstream.video.android.core.ClientState.handleEvent]
 * For outgoing Call - we have only 1 source - StreamCallActivity //TODO Rahul
 */
internal class CallRingRingingReducer(val call: Call) :
    RingingStateReducer1<CallRingEvent, RingingState.Incoming> {

    override fun reduce(
        originalState: RingingState,
        event: CallRingEvent,
    ): ReducerOutput1<RingingState, CallRingEvent, RingingState.Incoming> {
        return ReducerOutput1.NoChange(originalState)
    }
}
