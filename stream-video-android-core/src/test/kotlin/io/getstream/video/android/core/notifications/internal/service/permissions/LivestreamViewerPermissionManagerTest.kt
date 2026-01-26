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

package io.getstream.video.android.core.notifications.internal.service.permissions

import android.content.pm.ServiceInfo
import android.os.Build
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class LivestreamViewerPermissionManagerTest {

    private lateinit var manager: LivestreamViewerPermissionManager

    @Before
    fun setup() {
        manager = LivestreamViewerPermissionManager()
    }

    @Test
    fun `requiredForegroundTypes contains only media playback`() {
        assertEquals(
            setOf(ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK),
            manager.requiredForegroundTypes,
        )
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `androidQServiceType returns media playback on Q`() {
        val type = manager.androidQServiceType()

        assertEquals(
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            type,
        )
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `noPermissionServiceType returns media playback on Q`() {
        val type = manager.noPermissionServiceType()

        assertEquals(
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            type,
        )
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun `noPermissionServiceType returns media playback above Q`() {
        val type = manager.noPermissionServiceType()

        assertEquals(
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            type,
        )
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun `noPermissionServiceType returns phone call below Q`() {
        val type = manager.noPermissionServiceType()

        assertEquals(
            ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL,
            type,
        )
    }
}
