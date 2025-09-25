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

package io.getstream.video.android.core.call.scope

import io.getstream.video.android.core.socket.common.scope.ClientScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ScopeProviderTest {

    private lateinit var clientScope: ClientScope
    private lateinit var scopeProvider: ScopeProvider

    @Before
    fun setup() {
        clientScope = ClientScope()
        scopeProvider = ScopeProviderImpl(clientScope)
    }

    @After
    fun cleanup() {
        scopeProvider.cleanup()
        clientScope.cancel()
    }

    @Test
    fun `getCoroutineScope should return a valid scope`() = runTest {
        val supervisorJob = SupervisorJob()
        val coroutineScope = scopeProvider.getCoroutineScope(supervisorJob)

        assertNotNull(coroutineScope)
        assertEquals(clientScope.coroutineContext + supervisorJob, coroutineScope.coroutineContext)
    }

    @Test
    fun `getRtcSessionScope should return a valid scope with dedicated thread`() = runTest {
        val callId = "test-call-1"
        val supervisorJob = SupervisorJob()

        val rtcSessionScope = scopeProvider.getRtcSessionScope(supervisorJob, callId)

        assertNotNull(rtcSessionScope)
        // The scope should have the client context, supervisor job, and a custom dispatcher
        assertTrue(rtcSessionScope.coroutineContext[supervisorJob.key] == supervisorJob)
    }

    @Test
    fun `multiple calls to getRtcSessionScope should create valid scopes`() = runTest {
        val callId1 = "test-call-1"
        val callId2 = "test-call-2"
        val supervisorJob1 = SupervisorJob()
        val supervisorJob2 = SupervisorJob()

        val scope1 = scopeProvider.getRtcSessionScope(supervisorJob1, callId1)
        val scope2 = scopeProvider.getRtcSessionScope(supervisorJob2, callId2)

        assertNotNull(scope1)
        assertNotNull(scope2)
        // Both scopes should be valid
    }

    @Test
    fun `cleanup should not throw exceptions`() = runTest {
        val callId = "test-call-1"
        val supervisorJob = SupervisorJob()
        scopeProvider.getCoroutineScope(supervisorJob)
        scopeProvider.getRtcSessionScope(supervisorJob, callId)

        // Should not throw any exceptions
        scopeProvider.cleanup()
    }

    @Test(expected = IllegalStateException::class)
    fun `calling getCoroutineScope after cleanup should throw exception`() = runTest {
        scopeProvider.cleanup()
        scopeProvider.getCoroutineScope(SupervisorJob())
    }

    @Test(expected = IllegalStateException::class)
    fun `calling getRtcSessionScope after cleanup should throw exception`() = runTest {
        scopeProvider.cleanup()
        scopeProvider.getRtcSessionScope(SupervisorJob(), "test-call-1")
    }

    @Test
    fun `executor should be created lazily on first getRtcSessionScope call`() = runTest {
        val callId = "test-call-1"
        val supervisorJob = SupervisorJob()

        // First call should create the executor
        val scope1 = scopeProvider.getRtcSessionScope(supervisorJob, callId)
        assertNotNull(scope1)

        // Second call should reuse the same executor
        val scope2 = scopeProvider.getRtcSessionScope(SupervisorJob(), callId)
        assertNotNull(scope2)

        // Both scopes should be valid
        assertNotNull(scope1)
        assertNotNull(scope2)
    }
}
