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

import io.getstream.video.android.core.utils.isAndroid17OrHigher
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CallServiceConfigAndroidVersionTest {

    @Before
    fun setup() {
        mockkStatic("io.getstream.video.android.core.utils.AndroidVersionCodesKt")
    }

    @After
    fun tearDown() {
        unmockkStatic("io.getstream.video.android.core.utils.AndroidVersionCodesKt")
    }

    @Test
    fun `Telecom is enabled by default on Android 17`() {
        every { isAndroid17OrHigher() } returns true

        assertTrue(CallServiceConfig().enableTelecom)
    }

    @Test
    fun `Telecom remains disabled by default before Android 17`() {
        every { isAndroid17OrHigher() } returns false

        assertFalse(CallServiceConfig().enableTelecom)
    }

    @Test
    fun `explicit Telecom configuration overrides Android default`() {
        every { isAndroid17OrHigher() } returns true

        assertFalse(CallServiceConfig(enableTelecom = false).enableTelecom)
    }
}
