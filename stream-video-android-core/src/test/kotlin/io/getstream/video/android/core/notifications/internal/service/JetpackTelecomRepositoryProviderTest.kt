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

package io.getstream.video.android.core.notifications.internal.service

import android.content.Context
import android.os.Build
import androidx.core.telecom.CallsManager
import androidx.test.core.app.ApplicationProvider
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.model.StreamCallId
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class JetpackTelecomRepositoryProviderTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        mockkConstructor(CallsManager::class)
        mockkObject(StreamVideo)

        every { StreamVideo.instance() } returns mockk<StreamVideoClient>(relaxed = true)
        every { anyConstructed<CallsManager>().registerAppWithTelecom(any()) } just runs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `registers app with video calling and call streaming capabilities`() {
        JetpackTelecomRepositoryProvider(context).get(StreamCallId("default", "call-id"))

        verify(exactly = 1) {
            anyConstructed<CallsManager>().registerAppWithTelecom(
                CallsManager.CAPABILITY_SUPPORTS_CALL_STREAMING or
                    CallsManager.CAPABILITY_SUPPORTS_VIDEO_CALLING,
            )
        }
    }
}
