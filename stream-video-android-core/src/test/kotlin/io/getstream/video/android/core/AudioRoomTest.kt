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

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import io.getstream.video.android.core.model.QueryCallsData
import io.getstream.video.android.core.utils.mapSuspend
import io.getstream.video.android.core.utils.onSuccess
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner


@RunWith(RobolectricTestRunner::class)
class AudioRoomTest: IntegrationTestBase() {
    /**
     * The Swift tutorial is good inspiration
     * https://github.com/GetStream/stream-video-swift/blob/main/docusaurus/docs/iOS/guides/quickstart/audio-room.md
     *
     * Create a call
     * Go live in that call
     *
     * Join a call as a listener
     * Request permission to speak
     *
     * Errors
     * - Trying to publish while not having permissions to do so should raise an error
     *
     */

    /**
     * Possible developer experience
     *
     * Expose a call object, with data, stateflows and methods for easier developer experience:
     *
     * call = client.joinCall("default", 123) //returns a call state object
     * call.custom (stateflow)
     * call.participants (stateflow)
     * call.requestPermissions()
     * call.goLive()
     * call.leave()
     *
     *
     *     viewModel: CallViewModel = viewModel(
            factory = CallViewModel.provideFactory(
            call,
            ...
            )
            )

     *
     * On the compose layer we currently hide too much. It would be better to show
     * some components so you know where to start
     *
     * <Call><ParticipantGrid><CallButtons>Button12,23</CallButtons></Call> etc
     *
     */

    @Test
    fun queryCalls() = runTest {
        /**
         * To test:
         * - Filter on custom fields
         * - Filter on about to start in X hours
         * - Filter on currently live
         */
        val filters = mutableMapOf("active" to true)
        val result = client.queryCalls(QueryCallsData(filters))
        assert(result.isSuccess)
    }

    @Test
    fun createACall() = runTest {
        val result = client.getOrCreateCall("default", "123")
        assert(result.isSuccess)
    }

    @Test
    fun goLive() = runTest {

        val result = client.goLive("default", "123")
        assert(result.isSuccess)

    }

    @Test
    fun requestPermissions() = runTest {
        val result = client.requestPermissions("default", "123", mutableListOf("hellworld"))
        assert(result.isSuccess)

    }

    @Test
    fun joinCall() = runTest {
        val result = client.getOrCreateCall("default", "123")
        assert(result.isSuccess)

        val result2 = result.mapSuspend { client.joinCall(it) }
        assert(result.isSuccess)

        result2.onSuccess {
            // joined call
            // state is in the viewmodel


        }



    }
}
