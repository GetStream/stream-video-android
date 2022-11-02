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

package io.getstream.video.android.webrtc.utils

import io.getstream.video.android.errors.VideoError
import io.getstream.video.android.utils.Failure
import io.getstream.video.android.utils.Result
import io.getstream.video.android.utils.Success
import org.webrtc.AddIceObserver
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal suspend fun PeerConnection.addRtcIceCandidate(iceCandidate: IceCandidate): Result<Unit> {
    return suspendCoroutine { cont ->
        addIceCandidate(
            iceCandidate,
            object : AddIceObserver {
                override fun onAddSuccess() {
                    cont.resume(Success(Unit))
                }

                override fun onAddFailure(error: String?) {
                    cont.resume(Failure(VideoError(message = error)))
                }
            }
        )
    }
}
