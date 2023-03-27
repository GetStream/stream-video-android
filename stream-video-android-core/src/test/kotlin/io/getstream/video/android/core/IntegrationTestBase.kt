/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.core.model.User
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.rules.TestWatcher
import org.junit.runner.Description


class DispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        DispatcherProvider.set(testDispatcher, testDispatcher)
    }

    override fun finished(description: Description) {
        DispatcherProvider.reset()
    }
}

public class IntegrationTestHelper() {
    val users = mutableMapOf<String, User>()
    val tokens = mutableMapOf<String, String>()

    val client: StreamVideo
    val builder: StreamVideoBuilder2
    val context: Context

    init {
        // TODO: generate token from build vars
        val token =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoidGhpZXJyeSJ9._4aZL6BR0VGKfZsKYdscsBm8yKVgG-2LatYeHRJUq0g"

        val thierry = User(
            id = "thierry", role = "admin", name = "Thierry", imageUrl = "hello",
            teams = emptyList(), extraData = mapOf()
        )
        users["thierry"] = thierry
        tokens["thierry"] = token
        builder = StreamVideoBuilder2(
            context = ApplicationProvider.getApplicationContext(),
            apiKey = "hd8szvscpxvd",
            geo = GEO.GlobalEdgeNetwork,
            thierry,
            token,
            loggingLevel = LoggingLevel.BODY
        )
        client = builder.build()
        context = ApplicationProvider.getApplicationContext()
    }
}

open class IntegrationTestBase() {
    @get:Rule
    val dispatcherRule = DispatcherRule()


    val testData = IntegrationTestHelper()
    val client = testData.client
    val context = testData.context
    val apiKey = "hd8szvscpxvd"

}
