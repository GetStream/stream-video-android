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

import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.model.UserType
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ClientAndAuthTest : IntegrationTestBase() {
    /**
     * So what do we need to test on the client..
     *
     * StreamVideoConfig. I'm not sure any of this belongs here.
     * This should be on the call type....
     *
     * Things I didn't expect on the client
     * ** AndroidInputs (not sure what it does)
     * ** InputLauncher (again unsure)
     * ** PushDevice Generators, probably to support firebase alternatives, not sure
     *
     * Missing..
     * * Geofencing policy (also a good moment to show our edge network)
     * * Filters (video and audio, hooks so you can build your own filters)
     * * Guest and anon users
     * * Token expiration
     * * Connection timeout settings
     *
     * Client setup errors
     * ** Invalid API key
     *
     */
    @Test
    fun regularUser() = runTest {
        StreamVideoBuilder2(
            context = context,
            apiKey = apiKey,
            geo = GEO.GlobalEdgeNetwork,
            testData.users["thierry"]!!,
            testData.tokens["thierry"]!!,
        ).build()
    }

    @Test
    fun anonymousUser() = runTest {
        StreamVideoBuilder2(
            context = context,
            apiKey = apiKey,
            geo = GEO.GlobalEdgeNetwork,
            user = User(id="anon", type=UserType.Anonymous)
        ).build()
    }

    @Test
    fun guestUser() = runTest {
        StreamVideoBuilder2(
            context = context,
            apiKey = apiKey,
            geo = GEO.GlobalEdgeNetwork,
            user = User(id="guest", type=UserType.Guest)
        ).build()
    }

    @Test
    fun testInvalidAPIKey() = runTest {
        StreamVideoBuilder2(
            context = context,
            apiKey = "notvalid",
            geo = GEO.GlobalEdgeNetwork,
            testData.users["thierry"]!!,
            testData.tokens["thierry"]!!,
        ).build()
    }

    @Test
    fun testEmptyAPIKey() = runTest {
        StreamVideoBuilder2(
            context = context,
            apiKey = "",
            geo = GEO.GlobalEdgeNetwork,
            testData.users["thierry"]!!,
            testData.tokens["thierry"]!!,
        ).build()
    }

    @Test
    fun testInvalidToken() = runTest {
        StreamVideoBuilder2(
            context = context,
            apiKey = apiKey,
            geo = GEO.GlobalEdgeNetwork,
            testData.users["thierry"]!!,
            "invalidtoken",
        ).build()
    }
}
