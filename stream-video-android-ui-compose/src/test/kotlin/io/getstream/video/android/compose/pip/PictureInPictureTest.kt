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

package io.getstream.video.android.compose.pip

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.util.Rational
import androidx.core.content.ContextCompat
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.model.ScreenSharingSession
import io.getstream.video.android.core.pip.PictureInPictureConfiguration
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
public class PictureInPictureTest {

    private lateinit var context: Context
    private lateinit var call: Call
    private lateinit var pipConfig: PictureInPictureConfiguration

    @Before
    public fun setup() {
        context = mockk(relaxed = true)
        call = mockk(relaxed = true)
        pipConfig = PictureInPictureConfiguration(true)

        val packageManager = mockk<PackageManager>()
        every { context.packageManager } returns packageManager
        every { packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) } returns true

        val resources = mockk<android.content.res.Resources>()
        val configuration = Configuration()
        configuration.orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        every { resources.configuration } returns configuration
        every { context.resources } returns resources

        // Mock findActivity()
        val activity = mockk<Activity>(relaxed = true)
        mockkStatic("io.getstream.video.android.compose.pip.PictureInPictureKt")
        every { context.findActivity() } returns activity
    }

    @After
    public fun tearDown() {
        unmockkStatic(ContextCompat::class)
        clearAllMocks()
    }

    // enterPictureInPicture Test
    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    public fun `should enter pip mode with correct params`() {
        val screenSharing = mockk<ScreenSharingSession>(relaxed = true)
        every { call.state.screenSharingSession.value } returns screenSharing
        val activity = context.findActivity()!!

        enterPictureInPicture(context, call, pipConfig)

        verify(exactly = 1) { activity.enterPictureInPictureMode(any()) }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.N]) // below O
    public fun `should enter pip mode without params when below O`() {
        every {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        } returns true
        val activity = context.findActivity()!!

        enterPictureInPicture(context, call, pipConfig)

        verify(exactly = 1) { activity.enterPictureInPictureMode() }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.O])
    public fun `should not enter pip when feature not supported`() {
        every {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        } returns false
        enterPictureInPicture(context, call, pipConfig)
        val activity = context.findActivity()!!
        verify(exactly = 0) { activity.enterPictureInPictureMode(any()) }
    }

    // getAspect test

    @Test
    public fun `should return 9x16 when portrait and local or no screen share`() {
        val localParticipant = mockk<ParticipantState>()
        every { localParticipant.isLocal } returns true

        val screenSharing = mockk<ScreenSharingSession>()
        every { screenSharing.participant } returns localParticipant

        val aspect1 = getAspect(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, null)
        val aspect2 = getAspect(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, screenSharing)

        assertEquals(Rational(9, 16), aspect1)
        assertEquals(Rational(9, 16), aspect2)
    }

    @Test
    public fun `should return 16x9 when landscape`() {
        val aspect = getAspect(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, null)
        assertEquals(Rational(16, 9), aspect)
    }

    @Test
    public fun `should return 16x9 when remote participant sharing in portrait`() {
        val remoteParticipant = mockk<ParticipantState>()
        every { remoteParticipant.isLocal } returns false

        val screenSharing = mockk<ScreenSharingSession>()
        every { screenSharing.participant } returns remoteParticipant

        val aspect = getAspect(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, screenSharing)
        assertEquals(Rational(16, 9), aspect)
    }

    // getPictureInPictureParams Test

    @Test
    @Config(sdk = [Build.VERSION_CODES.S]) // Android 12
    public fun `should set aspect ratio and auto-enter when SDK S`() {
        val builder = getPictureInPictureParams(Rational(16, 9), pipConfig)
        val params = builder.build()

        assertNotNull(params)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    public fun `should set title and seamless resize for TIRAMISU`() {
        val builder = getPictureInPictureParams(Rational(9, 16), pipConfig)
        val params = builder.build()
        assertNotNull(params)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.O])
    public fun `should only set aspect ratio for Oreo`() {
        val builder = getPictureInPictureParams(Rational(9, 16), pipConfig)
        val params = builder.build()
        assertNotNull(params)
    }
}
