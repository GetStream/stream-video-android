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
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Rational
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.model.ScreenSharingSession
import io.getstream.video.android.core.pip.PictureInPictureConfiguration
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
public class PictureInPictureTest {

    private lateinit var activity: Activity
    private lateinit var call: Call
    private lateinit var pipConfig: PictureInPictureConfiguration

    @Before
    public fun setup() {
        activity = Robolectric.buildActivity(Activity::class.java).create().get()
        call = mockk(relaxed = true)
        pipConfig = PictureInPictureConfiguration(true)

        shadowOf(activity.packageManager)
            .setSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE, true)
    }

    @After
    public fun tearDown() {
        clearAllMocks()
    }

    // enterPictureInPicture Test
    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    public fun `should enter pip mode with correct params`() {
        val screenSharing = mockk<ScreenSharingSession>(relaxed = true)
        every { call.state.screenSharingSession.value } returns screenSharing

        enterPictureInPicture(activity, call, pipConfig)

        assertTrue(activity.isInPictureInPictureMode)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.N]) // below O
    public fun `should enter pip mode without params when below O`() {
        enterPictureInPicture(activity, call, pipConfig)

        assertTrue(activity.isInPictureInPictureMode)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.O])
    public fun `should not enter pip when feature not supported`() {
        shadowOf(activity.packageManager)
            .setSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE, false)

        enterPictureInPicture(activity, call, pipConfig)

        assertFalse(activity.isInPictureInPictureMode)
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

        // The PictureInPictureParams getters are only public from API 33,
        // so the params values can only be asserted in the TIRAMISU tests.
        assertNotNull(params)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    public fun `should set title and seamless resize for TIRAMISU`() {
        val builder = getPictureInPictureParams(Rational(9, 16), pipConfig)
        val params = builder.build()

        assertEquals(Rational(9, 16), params.aspectRatio)
        assertEquals("Video Player", params.title.toString())
        assertTrue(params.isSeamlessResizeEnabled)
        assertTrue(params.isAutoEnterEnabled)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    public fun `should disable auto-enter when configuration disables it for TIRAMISU`() {
        val config = PictureInPictureConfiguration(enable = true, autoEnterEnabled = false)

        val params = getPictureInPictureParams(Rational(9, 16), config).build()

        assertFalse(params.isAutoEnterEnabled)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.O])
    public fun `should only set aspect ratio for Oreo`() {
        val builder = getPictureInPictureParams(Rational(9, 16), pipConfig)
        val params = builder.build()
        assertNotNull(params)
    }
}
