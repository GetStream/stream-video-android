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

package io.getstream.video.android.core.base

import io.getstream.log.Priority
import io.getstream.log.StreamLog
import io.getstream.log.streamLog
import io.getstream.result.Result
import io.getstream.video.android.core.base.auth.GetAuthDataResponse
import io.getstream.video.android.core.base.auth.StreamService
import io.getstream.video.android.core.call.connection.StreamPeerConnectionFactory
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.junit4.MockKRule
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.threeten.bp.Clock
import org.threeten.bp.OffsetDateTime
import java.util.UUID

public open class TestBase {
    @get:Rule
    val dispatcherRule = DispatcherRule()

    /** Convenient helper with test data */
    val testData = IntegrationTestHelper()

    /** Android context */
    val context = testData.context

    val nowUtc = OffsetDateTime.now(Clock.systemUTC())

    @get:Rule
    val mockkRule = MockKRule(this)

    @MockK(relaxUnitFun = true, relaxed = true)
    lateinit var mockedPCFactory: StreamPeerConnectionFactory

    var authData: GetAuthDataResponse? = null

    init {
        runBlocking {
            authData = StreamService.instance.getAuthData(
                environment = "pronto",
                userId = testData.users["thierry"]!!.id,
            )
        }
    }

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true) // turn relaxUnitFun on for all mocks

        if (!StreamLog.isInstalled) {
            StreamLog.setValidator { priority, _ -> priority > Priority.VERBOSE }
            StreamLog.install(logger = testLogger)
            testLogger.streamLog { "test logger installed" }
        }
    }

    private val testLogger = StreamTestLogger()

    fun setLogLevel(newPriority: Priority) {
        StreamLog.setValidator { priority, _ -> priority > newPriority }
    }

    fun randomUUID(): String {
        return UUID.randomUUID().toString()
    }

    /**
     * Verify the Result is a success and raise a nice error if it isn't
     */
    fun assertSuccess(result: Result<Any>) {
        assert(result.isSuccess) {
            result.onError {
                "result wasn't a success, got an error $it."
            }
        }
    }

    fun assertError(result: Result<Any>) {
        assert(result.isFailure) {
            result.onSuccess {
                "result was a success, expected a failure"
            }
        }
    }
}
