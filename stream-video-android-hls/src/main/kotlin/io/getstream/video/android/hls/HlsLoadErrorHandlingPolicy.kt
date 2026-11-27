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

package io.getstream.video.android.hls

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy.LoadErrorInfo

/**
 * Keeps a live stream alive through network loss. The default policy turns a load error into a
 * playback error after three retries, a few seconds offline, and the player then stays idle. This
 * policy retries playlist and segment loads until they succeed, so the player buffers while the
 * network is down and resumes when it returns.
 *
 * A 4xx response fails at once instead: an expired or revoked URL, or a stream that has ended, will
 * not recover by retrying. Every other error keeps the default delay, which grows by a second per
 * attempt up to five seconds, and the default immediate failure for errors such as malformed
 * playlists.
 */
@OptIn(UnstableApi::class)
internal class HlsLoadErrorHandlingPolicy : DefaultLoadErrorHandlingPolicy() {

    override fun getMinimumLoadableRetryCount(dataType: Int): Int = Int.MAX_VALUE

    override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorInfo): Long =
        if (isClientError(loadErrorInfo.exception)) {
            C.TIME_UNSET
        } else {
            super.getRetryDelayMsFor(loadErrorInfo)
        }

    private fun isClientError(exception: Throwable): Boolean =
        generateSequence(exception, Throwable::cause).any {
            it is HttpDataSource.InvalidResponseCodeException &&
                it.responseCode in 400..499 &&
                it.responseCode !in TRANSIENT_CLIENT_ERRORS
        }

    private companion object {
        /** Request Timeout and Too Many Requests: worth retrying. */
        val TRANSIENT_CLIENT_ERRORS = setOf(408, 429)
    }
}
