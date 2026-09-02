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

package io.getstream.video.android.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class StreamVideoBuilderConfigurationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `notification ringtone is enabled by default and can be disabled`() {
        val builder = StreamVideoBuilder(context, "api-key")

        assertTrue(builder.debugUseNotificationRingtoneForIncomingCalls)

        builder.debugUseNotificationRingtoneForIncomingCalls(false)

        assertFalse(builder.debugUseNotificationRingtoneForIncomingCalls)
    }

    @Test
    fun `Telecom first incoming calls can be enabled below Android 17`() {
        val builder = StreamVideoBuilder(context, "api-key")

        assertFalse(builder.debugUseTelecomFirstForIncomingCalls)

        builder.debugUseTelecomFirstForIncomingCalls(true)

        assertTrue(builder.debugUseTelecomFirstForIncomingCalls)
    }
}
