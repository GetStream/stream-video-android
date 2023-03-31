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

import com.google.common.truth.Truth.assertThat
import io.getstream.video.android.core.events.*
import io.getstream.video.android.core.model.*
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.openapitools.client.models.OwnCapability
import org.robolectric.RobolectricTestRunner
import stream.video.sfu.event.ConnectionQualityInfo
import stream.video.sfu.models.ConnectionQuality
import stream.video.sfu.models.Participant
import java.util.*

@RunWith(RobolectricTestRunner::class)
class JoinCallTest : IntegrationTestBase() {

    /**
     *
     */
    @Test
    fun `test joining a call`() = runTest {
        val call = client.call("default", randomUUID())

        //call.join()

    }

}
