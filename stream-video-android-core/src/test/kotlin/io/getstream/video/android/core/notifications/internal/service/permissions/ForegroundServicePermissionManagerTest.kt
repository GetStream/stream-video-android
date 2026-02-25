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

import android.Manifest
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import io.getstream.video.android.core.notifications.internal.service.CallService
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ForegroundServicePermissionManagerTest {

    private lateinit var context: Context
    private lateinit var manager: ForegroundServicePermissionManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        manager = ForegroundServicePermissionManager()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun `ongoing call with permissions returns combined service type`() {
        ShadowApplication.getInstance().grantPermissions(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
        )

        val type = manager.getServiceType(
            context,
            CallService.Companion.Trigger.OnGoingCall,
        )

        val expected =
            ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL or
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE

        assertEquals(expected, type)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun `ongoing call without permissions falls back to no-permission service type`() {
        // No permissions granted

        val type = manager.getServiceType(
            context,
            CallService.Companion.Trigger.OnGoingCall,
        )

        assertEquals(
            ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL,
            type,
        )
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `android Q uses phone call service type`() {
        val type = manager.allPermissionsServiceType()

        assertEquals(
            ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL,
            type,
        )
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun `pre Q returns zero service type`() {
        val type = manager.allPermissionsServiceType()
        assertEquals(0, type)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
    fun `no permission uses short service on android 14`() {
        val type = manager.noPermissionServiceType()

        assertEquals(
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE,
            type,
        )
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun `no permission uses phone call service below android 14`() {
        val type = manager.noPermissionServiceType()

        assertEquals(
            ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL,
            type,
        )
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun `hasAllPermissions returns true when all permissions granted`() {
        ShadowApplication.getInstance().grantPermissions(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
        )

        assertTrue(manager.hasAllPermissions(context))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun `hasAllPermissions returns false when permission missing`() {
        ShadowApplication.getInstance().grantPermissions(
            Manifest.permission.CAMERA,
        )

        assertFalse(manager.hasAllPermissions(context))
    }
}
