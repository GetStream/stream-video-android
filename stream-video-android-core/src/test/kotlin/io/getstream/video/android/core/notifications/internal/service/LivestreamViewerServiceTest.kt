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
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.content.ContextCompat
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class LivestreamViewerServiceTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        context = RuntimeEnvironment.getApplication()
        mockkStatic(ContextCompat::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(ContextCompat::class)
        clearAllMocks()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `LivestreamViewerService has correct service type for Android Q`() {
        val service = LivestreamViewerService()
        service.attachContext(context)

        assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK, service.serviceType)
    }

    @Test
    fun `LivestreamViewerService has correct service type for Pre Android Q`() {
        val service = LivestreamViewerService()
        service.attachContext(context)

        assertEquals(0, service.serviceType)
    }
}
