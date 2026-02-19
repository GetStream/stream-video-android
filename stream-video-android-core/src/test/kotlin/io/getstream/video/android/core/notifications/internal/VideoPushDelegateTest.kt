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

package io.getstream.video.android.core.notifications.internal

import io.getstream.video.android.core.notifications.internal.VideoPushDelegate.Companion.KEY_CALL_DISPLAY_NAME
import io.getstream.video.android.core.notifications.internal.VideoPushDelegate.Companion.KEY_CREATED_BY_DISPLAY_NAME
import org.junit.Test
import kotlin.test.assertEquals

class VideoPushDelegateTest {

    private val delegate = VideoPushDelegate()

    @Test
    fun `check getCallDisplayName() with empty payload`() {
        assertEquals(VideoPushDelegate.DEFAULT_CALL_TEXT, delegate.getCallDisplayName(emptyMap()))
    }

    @Test
    fun `check getCallDisplayName() with non-String value`() {
        assertEquals(
            VideoPushDelegate.DEFAULT_CALL_TEXT,
            delegate.getCallDisplayName(mapOf(KEY_CALL_DISPLAY_NAME to 1)),
        )
    }

    @Test
    fun `check getCallDisplayName() with String value`() {
        val createdBy = "createdBy"
        assertEquals(
            createdBy,
            delegate.getCallDisplayName(mapOf(KEY_CREATED_BY_DISPLAY_NAME to createdBy)),
        )
    }

    @Test
    fun `check getCallDisplayName() pick KEY_CALL_DISPLAY_NAME`() {
        val callDisplayName = "callDisplayName"
        val createdBy = "createdBy"
        assertEquals(
            callDisplayName,
            delegate.getCallDisplayName(
                mapOf(
                    KEY_CALL_DISPLAY_NAME to callDisplayName,
                    KEY_CREATED_BY_DISPLAY_NAME to createdBy,
                ),
            ),
        )
    }
}
