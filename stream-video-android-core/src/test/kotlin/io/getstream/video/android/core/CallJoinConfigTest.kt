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

package io.getstream.video.android.core

import com.google.common.truth.Truth.assertThat
import io.getstream.android.video.generated.models.RejectCallResponse
import io.getstream.result.Error
import io.getstream.result.Result.Failure
import io.getstream.result.Result.Success
import io.getstream.video.android.core.base.IntegrationTestBase
import io.getstream.video.android.core.call.JoinCallConfig
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.errors.SfuJoinException
import io.getstream.video.android.core.model.RejectReason
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
internal class CallJoinConfigTest : IntegrationTestBase() {

    @Test
    fun `rejects ringing call when SFU join fails and auto reject is enabled`() = runTest {
        val call = spyk(client.call("default", randomUUID()), recordPrivateCalls = true)

        every { call.clientImpl.permissionCheck.checkAndroidPermissions(any(), any()) } returns true

        val joinFailure = Failure(
            Error.ThrowableError(
                message = "sfu join failed",
                cause = SfuJoinException("failed to connect", RuntimeException("boom")),
            ),
        )

        coEvery { call._join(any(), any(), any(), any()) } returns joinFailure
        coEvery { call.reject(RejectReason.Cancel) } returns Success(RejectCallResponse(duration = "0"))

        val result = call.join(
            ring = true,
            joinCallConfig = JoinCallConfig(
                rejectRingingCallOnFailure = true,
                maxRetries = 1,
            ),
        )

        assertThat(result.isFailure).isTrue()
        coVerify(exactly = 1) { call.isSFuJoinError(any()) }
        coVerify(exactly = 1) { call.reject(RejectReason.Cancel) }
        assertThat(call.state.connection.value).isInstanceOf(RealtimeConnection.Failed::class.java)
    }

    @Test
    fun `join surfaces reject error if it has to reject ringing call when sfu join fails`() = runTest {
        val call = spyk(client.call("default", randomUUID()), recordPrivateCalls = true)

        every { call.clientImpl.permissionCheck.checkAndroidPermissions(any(), any()) } returns true

        val joinFailure = Failure(
            Error.ThrowableError(
                message = "sfu join failed",
                cause = SfuJoinException("failed to connect", RuntimeException("boom")),
            ),
        )

        coEvery { call._join(any(), any(), any(), any()) } returns joinFailure
        val failure = Failure(Error.GenericError("reject failed"))
        coEvery { call.reject(RejectReason.Cancel) } returns failure

        val result = call.join(
            ring = true,
            joinCallConfig = JoinCallConfig(
                rejectRingingCallOnFailure = true,
                maxRetries = 1,
            ),
        )

        assertThat(result.isFailure).isTrue()
        coVerify(exactly = 1) { call.isSFuJoinError(any()) }
        coVerify(exactly = 1) { call.reject(RejectReason.Cancel) }
        assertThat(call.state.connection.value).isInstanceOf(RealtimeConnection.Failed::class.java)
        assertEquals(failure, result)
    }

    @Test
    fun `join retries until success respecting the configured max retries`() = runTest {
        val call = spyk(client.call("default", randomUUID()), recordPrivateCalls = true)

        every { call.clientImpl.permissionCheck.checkAndroidPermissions(any(), any()) } returns true

        val rtcSession = mockk<RtcSession>(relaxed = true)
        val transientFailure =
            Failure(Error.ThrowableError("Unable to resolve host", RuntimeException("boom")))
        coEvery { call._join(any(), any(), any(), any()) } returnsMany listOf(
            transientFailure,
            Success(rtcSession),
        )

        val result = call.join(joinCallConfig = JoinCallConfig(maxRetries = 2))

        println(result)
        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 2) { call._join(any(), any(), any(), any()) }
    }
}
