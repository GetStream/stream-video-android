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

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.ParserException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy.LoadErrorInfo
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.net.SocketTimeoutException

@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
internal class HlsLoadErrorHandlingPolicyTest {

    private val policy = HlsLoadErrorHandlingPolicy()
    private val dataSpec = DataSpec(Uri.parse("https://example.com/live.m3u8"))

    @Test
    fun `loads are retried without limit`() {
        assertThat(policy.getMinimumLoadableRetryCount(C.DATA_TYPE_MANIFEST))
            .isEqualTo(Int.MAX_VALUE)
        assertThat(policy.getMinimumLoadableRetryCount(C.DATA_TYPE_MEDIA))
            .isEqualTo(Int.MAX_VALUE)
    }

    @Test
    fun `network errors are retried with a delay capped at five seconds`() {
        val error = SocketTimeoutException("offline")

        assertThat(policy.getRetryDelayMsFor(errorInfo(error, errorCount = 2))).isEqualTo(1_000)
        assertThat(policy.getRetryDelayMsFor(errorInfo(error, errorCount = 4))).isEqualTo(3_000)
        assertThat(policy.getRetryDelayMsFor(errorInfo(error, errorCount = 100))).isEqualTo(5_000)
    }

    @Test
    fun `server errors are retried`() {
        assertThat(policy.getRetryDelayMsFor(errorInfo(httpError(503), errorCount = 2)))
            .isEqualTo(1_000)
    }

    @Test
    fun `client errors fail at once`() {
        listOf(400, 403, 404, 410).forEach { code ->
            assertThat(policy.getRetryDelayMsFor(errorInfo(httpError(code))))
                .isEqualTo(C.TIME_UNSET)
        }
    }

    @Test
    fun `a wrapped client error fails at once`() {
        val error = IOException("wrapped", httpError(403))

        assertThat(policy.getRetryDelayMsFor(errorInfo(error))).isEqualTo(C.TIME_UNSET)
    }

    @Test
    fun `request timeouts and rate limits are retried`() {
        listOf(408, 429).forEach { code ->
            assertThat(policy.getRetryDelayMsFor(errorInfo(httpError(code), errorCount = 2)))
                .isEqualTo(1_000)
        }
    }

    @Test
    fun `malformed playlists fail at once`() {
        val error = ParserException.createForMalformedManifest("bad playlist", null)

        assertThat(policy.getRetryDelayMsFor(errorInfo(error))).isEqualTo(C.TIME_UNSET)
    }

    private fun httpError(code: Int) = HttpDataSource.InvalidResponseCodeException(
        code,
        /* responseMessage = */
        null,
        /* cause = */
        null,
        /* headerFields = */
        emptyMap(),
        dataSpec,
        /* responseBody = */
        ByteArray(0),
    )

    private fun errorInfo(error: IOException, errorCount: Int = 1) = LoadErrorInfo(
        LoadEventInfo(
            LoadEventInfo.getNewId(),
            dataSpec,
            /* elapsedRealtimeMs = */
            0L,
        ),
        MediaLoadData(C.DATA_TYPE_MANIFEST),
        error,
        errorCount,
    )
}
