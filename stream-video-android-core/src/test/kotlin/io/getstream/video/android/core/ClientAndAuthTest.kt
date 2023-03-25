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

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ClientAndAuthTest : IntegrationTestBase() {
    /**
     * So what do we need to test on the client..
     *
     * Auth User types
     * - Normal user, guest user, not-authenticated
     *
     * Joining call auth
     * - Token auth
     *
     * Token refreshing
     * - Tokens can expire, so there needs to be a token refresh handler
     *
     * Client setup
     * ** Geofencing policy (also a good moment to show our edge network)
     *
     * StreamVideoConfig. I'm not sure any of this belongs here.
     * This should be on the call type....
     *
     * Things I didn't expect on the client
     * ** AndroidInputs (not sure what it does)
     * ** InputLauncher (again unsure)
     * ** PushDevice Generators..
     *
     * Missing..
     * * Filters
     *
     * Client setup errors
     * ** Invalid API key
     *
     */
    @Test
    fun clientBuilder() = runTest {
        val builder = StreamVideoBuilder(
            context = ApplicationProvider.getApplicationContext(),
            helper.users["thierry"]!!,
            apiKey = "hd8szvscpxvd",
        )
        val client = builder.build()
    }
}
