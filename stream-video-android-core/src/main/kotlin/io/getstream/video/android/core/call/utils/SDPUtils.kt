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

package io.getstream.video.android.core.call.utils

import io.getstream.result.Error
import io.getstream.result.Result
import io.getstream.result.Result.Failure
import io.getstream.result.Result.Success
import io.getstream.webrtc.SdpObserver
import io.getstream.webrtc.SessionDescription
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@JvmSynthetic
internal suspend inline fun createValue(
    crossinline call: (SdpObserver) -> Unit,
): Result<SessionDescription> = suspendCoroutine {
    val observer = object : SdpObserver {

        /**
         * Handling of create values.
         */
        override fun onCreateSuccess(description: SessionDescription?) {
            if (description != null) {
                it.resume(Success(description))
            } else {
                Failure(Error.GenericError("SessionDescription is null!"))
            }
        }

        override fun onCreateFailure(message: String?) = it.resume(
            Failure(
                Error.GenericError(message ?: "Couldn't create a SDP message."),
            ),
        )

        /**
         * We ignore set results.
         */
        override fun onSetSuccess() = Unit
        override fun onSetFailure(p0: String?) = Unit
    }

    call(observer)
}

@JvmSynthetic
internal suspend inline fun setValue(
    crossinline call: (SdpObserver) -> Unit,
): Result<Unit> = suspendCoroutine {
    val observer = object : SdpObserver {
        /**
         * We ignore create results.
         */
        override fun onCreateFailure(p0: String?) = Unit
        override fun onCreateSuccess(p0: SessionDescription?) = Unit

        /**
         * Handling of set values.
         */
        override fun onSetSuccess() = it.resume(Success(Unit))
        override fun onSetFailure(message: String?) = it.resume(
            Failure(
                Error.GenericError(message ?: "Couldn't create a SDP message."),
            ),
        )
    }

    call(observer)
}
