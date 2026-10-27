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

package io.getstream.video.android.util

import android.util.Log
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.util.config.AppConfig

/**
 * Retains the first [Call] used for each CID when the corresponding debug option is enabled.
 *
 * This provider deliberately lives outside `CallActivity` so the retained instances survive
 * activity recreation. It clears them when the option is disabled or the [StreamVideo] client
 * changes, preventing Calls owned by an old client from being returned.
 */
internal class RetainedCallProvider private constructor() {

    internal companion object {
        private const val TAG = "ReusableCallDemo"

        val instance = RetainedCallProvider()
    }

    private val lock = Any()
    private val retainedCalls = mutableMapOf<String, Call>()
    private var retainedCallsClient: StreamVideo? = null

    /**
     * Returns `true` when this provider handled the request. A `false` result tells the caller to
     * use its normal Call provider because the debug option is disabled.
     */
    fun provide(
        callId: StreamCallId,
        onSuccess: ((Call) -> Unit)?,
        onError: ((Exception) -> Unit)?,
    ): Boolean {
        if (!AppConfig.reuseCallAfterLeave.value) {
            clear()
            return false
        }

        val streamVideo = StreamVideo.instanceOrNull()
        if (streamVideo == null) {
            onError?.invoke(
                IllegalStateException(
                    "StreamVideoBuilder.build() must be called before obtaining StreamVideo instance.",
                ),
            )
            return true
        }

        val call = getOrCreate(streamVideo, callId)
        onSuccess?.invoke(call)
        return true
    }

    /**
     * Returns the retained Call when the debug option is enabled, or delegates to the SDK's normal
     * get-or-create behaviour when it is disabled.
     *
     * Join and lobby screens must use this entry point as well as CallActivity. Otherwise they can
     * populate the SDK cache with a second Call after the retained instance has left but before the
     * activity asks for it.
     */
    fun getOrCreate(streamVideo: StreamVideo, callId: StreamCallId): Call {
        if (!AppConfig.reuseCallAfterLeave.value) {
            clear()
            return streamVideo.call(callId.type, callId.id)
        }

        return synchronized(lock) {
            if (retainedCallsClient !== streamVideo) {
                retainedCalls.clear()
                retainedCallsClient = streamVideo
            }
            retainedCalls.getOrPut(callId.cid) {
                streamVideo.call(callId.type, callId.id)
            }.also { call ->
                Log.i(
                    TAG,
                    "Using retained Call for ${call.cid}: identity=${System.identityHashCode(
                        call,
                    )}",
                )
            }
        }
    }

    private fun clear() {
        synchronized(lock) {
            retainedCalls.clear()
            retainedCallsClient = null
        }
    }
}
