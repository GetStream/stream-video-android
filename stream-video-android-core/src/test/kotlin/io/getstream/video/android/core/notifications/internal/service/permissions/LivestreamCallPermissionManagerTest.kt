/*
 * Copyright (c) 2014-2025 Stream.io Inc. All rights reserved.
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
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals

class LivestreamCallPermissionManagerTest {

    private lateinit var manager: LivestreamCallPermissionManager

    @Before
    fun setup() {
        manager = LivestreamCallPermissionManager()
    }

    @Test
    fun `requiredForegroundTypes contains camera and microphone only`() {
        val types = manager.requiredForegroundTypes

        assertEquals(
            setOf(
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
            ),
            types,
        )
    }
}
