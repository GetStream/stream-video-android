/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.notifications.internal.telecom

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfig
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class TelecomPermissionsTest {

    private lateinit var context: Context
    private lateinit var packageManager: PackageManager
    private lateinit var telecomManager: TelecomManager
    private lateinit var callServiceConfig: CallServiceConfig
    private lateinit var telecomPermissions: TelecomPermissions

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockkStatic(ContextCompat::class)
//        mockkStatic(StreamVideo::class)
        mockkObject(StreamVideo)

        context = mockk(relaxed = true)
//        context = RuntimeEnvironment.getApplication()
//        callService = CallService()
//        testCallId = StreamCallId(type = "default", id = "test-call-123")

        packageManager = mockk(relaxed = true)
        telecomManager = mockk(relaxed = true)
        callServiceConfig = CallServiceConfig(enableTelecom = true)

        every { context.packageManager } returns packageManager
        every { context.getSystemService(Context.TELECOM_SERVICE) } returns telecomManager

        telecomPermissions = TelecomPermissions()
    }

    @After
    fun tearDown() {
        unmockkObject(StreamVideo)
        unmockkStatic(ContextCompat::class)
        unmockkStatic(StreamVideo::class)
        clearAllMocks()
    }

    @Test
    fun `supportsTelecom returns true when device has telephony and default dialer`() {
        every { packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) } returns true
        every { telecomManager.defaultDialerPackage } returns "com.android.dialer"

        val result = telecomPermissions.supportsTelecom(context)

        assertTrue(result)
    }

    @Test
    fun `supportsTelecom returns false when no telephony feature`() {
        every { packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) } returns false
        every { telecomManager.defaultDialerPackage } returns "com.android.dialer"

        val result = telecomPermissions.supportsTelecom(context)

        assertFalse(result)
    }

    @Test
    fun `supportsTelecom returns false when no default dialer`() {
        every { packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) } returns true
        every { telecomManager.defaultDialerPackage } returns null

        val result = telecomPermissions.supportsTelecom(context)

        assertFalse(result)
    }
//
//    // endregion
//
//    // region canUseTelecom()

    @Test
    fun `canUseTelecom returns true when all conditions pass`() {
        val client = mockk<StreamVideoClient> {
            every { telecomConfig } returns mockk()
        }
        every { StreamVideo.instanceOrNull() } returns client
        every { packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) } returns true
        every { telecomManager.defaultDialerPackage } returns "com.android.dialer"

        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.MANAGE_OWN_CALLS)
        } returns PackageManager.PERMISSION_GRANTED

        val result = telecomPermissions.canUseTelecom(callServiceConfig, context)

        assertTrue(result)
    }

    @Test
    fun `canUseTelecom returns false when telecom disabled in config`() {
        val result = telecomPermissions.canUseTelecom(
            callServiceConfig.copy(enableTelecom = false),
            context,
        )
        assertFalse(result)
    }

    @Test
    fun `canUseTelecom returns false when StreamVideo client not opted for telecom`() {
        every { StreamVideo.instanceOrNull() } returns null
        val result = telecomPermissions.canUseTelecom(callServiceConfig, context)
        assertFalse(result)
    }

    @Test
    fun `canUseTelecom returns false when missing permission`() {
        val client = mockk<StreamVideoClient> { every { telecomConfig } returns mockk() }
        every { StreamVideo.instanceOrNull() } returns client

        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.MANAGE_OWN_CALLS)
        } returns PackageManager.PERMISSION_DENIED

        every { packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) } returns true
        every { telecomManager.defaultDialerPackage } returns "com.android.dialer"

        val result = telecomPermissions.canUseTelecom(callServiceConfig, context)

        assertFalse(result)
    }

    @Test
    fun `canUseTelecom returns false when device doesn't support telecom`() {
        val client = mockk<StreamVideoClient> { every { telecomConfig } returns mockk() }
        every { StreamVideo.instanceOrNull() } returns client

        every { packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) } returns false
        every { telecomManager.defaultDialerPackage } returns "com.android.dialer"

        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.MANAGE_OWN_CALLS)
        } returns PackageManager.PERMISSION_GRANTED

        val result = telecomPermissions.canUseTelecom(callServiceConfig, context)

        assertFalse(result)
    }
}
