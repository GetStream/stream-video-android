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

package io.getstream.video.android.core.notifications

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.ACTION_INCOMING_CALL
import io.getstream.video.android.core.notifications.internal.DismissNotificationActivity
import io.getstream.video.android.model.StreamCallId
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class DefaultStreamIntentResolverTest {

    private lateinit var context: Context
    private lateinit var resolver: DefaultStreamIntentResolver

    private val callId = StreamCallId(type = "default", id = "test-call")

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        resolver = DefaultStreamIntentResolver(
            context = context,
            notificationIntentBundleResolver = DefaultNotificationIntentBundleResolver(),
        )

        val incomingCallIntent = Intent(ACTION_INCOMING_CALL)
        shadowOf(context.packageManager).setResolveInfosForIntent(
            incomingCallIntent,
            listOf(
                ResolveInfo().apply {
                    priority = 1
                    activityInfo = ActivityInfo().apply {
                        packageName = context.packageName
                        name = IncomingCallActivity::class.java.name
                        applicationInfo = ApplicationInfo().apply {
                            packageName = context.packageName
                        }
                    }
                },
            ),
        )
    }

    @Test
    fun `incoming call content intent includes dismiss notification activity`() {
        val pendingIntent = resolver.searchIncomingCallPendingIntent(
            callId = callId,
            payload = emptyMap(),
        )

        val savedIntents = shadowOf(assertNotNull(pendingIntent)).savedIntents

        assertEquals(2, savedIntents.size)
        assertEquals(
            ComponentName(context, IncomingCallActivity::class.java),
            savedIntents[0].component,
        )
        assertEquals(
            ComponentName(context, DismissNotificationActivity::class.java),
            savedIntents[1].component,
        )
    }

    @Test
    fun `incoming call full screen intent directly opens call activity`() {
        val pendingIntent = resolver.searchIncomingCallFullScreenPendingIntent(
            callId = callId,
            payload = emptyMap(),
        )

        val savedIntents = shadowOf(assertNotNull(pendingIntent)).savedIntents

        assertEquals(1, savedIntents.size)
        assertEquals(
            ComponentName(context, IncomingCallActivity::class.java),
            savedIntents.single().component,
        )
    }

    private class IncomingCallActivity : Activity()
}
