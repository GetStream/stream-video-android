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

package io.getstream.video.android.core.analytics.reporting.dispatcher

import io.getstream.android.video.generated.apis.ProductvideoApi
import io.getstream.android.video.generated.models.ClientEvent
import io.getstream.android.video.generated.models.ReportClientEventResponse
import io.getstream.video.android.core.analytics.reporting.datasource.PendingEventDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class ImmediateEventDispatcherTest {

    private val api = mockk<ProductvideoApi>()
    private val dataSource = mockk<PendingEventDataSource>(relaxed = true)
    private val response = mockk<ReportClientEventResponse>()

    private fun TestScope.dispatcher() = ImmediateEventDispatcher(api, backgroundScope, dataSource)

    private fun event(id: String) = ClientEvent(id = id)

    private fun httpException(code: Int) = HttpException(
        Response.error<Any>(code, "{}".toResponseBody("application/json".toMediaType())),
    )

    @Test
    fun `send delivers the event to the api without persisting anything`() = runTest {
        coEvery { api.reportClientCallEvent(any()) } returns response
        val pendingEvent = event("e1")

        dispatcher().send(pendingEvent)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            api.reportClientCallEvent(match { it.events == listOf(pendingEvent) })
        }
        verify(exactly = 0) { dataSource.save(any()) }
    }

    @Test
    fun `sendAll with an empty list never touches the api`() = runTest {
        dispatcher().sendAll(emptyList())
        advanceUntilIdle()

        coVerify(exactly = 0) { api.reportClientCallEvent(any()) }
    }

    @Test
    fun `a transient failure is retried five times and then persisted`() = runTest {
        coEvery { api.reportClientCallEvent(any()) } throws IOException("network down")
        val pendingEvent = event("e1")

        dispatcher().send(pendingEvent)
        advanceUntilIdle()

        coVerify(exactly = 5) { api.reportClientCallEvent(any()) }
        verify(exactly = 1) { dataSource.save(listOf(pendingEvent)) }
    }

    @Test
    fun `a transient failure that recovers mid-retry is not persisted`() = runTest {
        coEvery {
            api.reportClientCallEvent(any())
        } throws IOException("flaky") andThenThrows IOException("flaky again") andThen response

        dispatcher().send(event("e1"))
        advanceUntilIdle()

        coVerify(exactly = 3) { api.reportClientCallEvent(any()) }
        verify(exactly = 0) { dataSource.save(any()) }
    }

    @Test
    fun `server errors are retried and persisted on exhaustion`() = runTest {
        coEvery { api.reportClientCallEvent(any()) } throws httpException(503)

        dispatcher().send(event("e1"))
        advanceUntilIdle()

        coVerify(exactly = 5) { api.reportClientCallEvent(any()) }
        verify(exactly = 1) { dataSource.save(any()) }
    }

    @Test
    fun `client errors fail fast without retry or persistence`() = runTest {
        coEvery { api.reportClientCallEvent(any()) } throws httpException(400)

        dispatcher().send(event("e1"))
        advanceUntilIdle()

        coVerify(exactly = 1) { api.reportClientCallEvent(any()) }
        verify(exactly = 0) { dataSource.save(any()) }
    }

    @Test
    fun `retryPending resends previously failed events as one batch`() = runTest {
        val pending = listOf(event("p1"), event("p2"))
        every { dataSource.loadAndClear() } returns pending
        coEvery { api.reportClientCallEvent(any()) } returns response

        dispatcher().retryPending()
        advanceUntilIdle()

        coVerify(exactly = 1) { api.reportClientCallEvent(match { it.events == pending }) }
    }

    @Test
    fun `retryPending with an empty store does nothing`() = runTest {
        every { dataSource.loadAndClear() } returns emptyList()

        dispatcher().retryPending()
        advanceUntilIdle()

        coVerify(exactly = 0) { api.reportClientCallEvent(any()) }
    }

    @Test
    fun `a replayed batch that fails again is re-persisted`() = runTest {
        val pending = listOf(event("p1"))
        every { dataSource.loadAndClear() } returns pending
        coEvery { api.reportClientCallEvent(any()) } throws IOException("still down")

        dispatcher().retryPending()
        advanceUntilIdle()

        verify(exactly = 1) { dataSource.save(pending) }
    }

    @Test
    fun `deleteAll cancels the scope and clears the store`() {
        val scope = CoroutineScope(Job())
        val dispatcher = ImmediateEventDispatcher(api, scope, dataSource)

        dispatcher.deleteAll()

        verify(exactly = 1) { dataSource.clear() }
        assertFalse(scope.isActive)
    }

    @Test
    fun `shouldRetry classifies transient and permanent failures`() {
        val dispatcher = ImmediateEventDispatcher(api, CoroutineScope(Job()), dataSource)

        assertTrue(dispatcher.shouldRetry(IOException()))
        assertTrue(dispatcher.shouldRetry(SocketTimeoutException()))
        assertTrue(dispatcher.shouldRetry(ConnectException()))
        assertTrue(dispatcher.shouldRetry(UnknownHostException()))
        assertTrue(dispatcher.shouldRetry(httpException(500)))
        assertTrue(dispatcher.shouldRetry(httpException(599)))

        assertFalse(dispatcher.shouldRetry(httpException(400)))
        assertFalse(dispatcher.shouldRetry(httpException(404)))
        assertFalse(dispatcher.shouldRetry(IllegalStateException()))
        assertFalse(dispatcher.shouldRetry(null))
    }
}
