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

package io.getstream.video.android.core.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidVersionCodesTest {

    @Test
    fun `SDK below Android 17 is not Android 17 or higher`() {
        assertFalse(isAndroid17OrHigher(BUILD_VERSION_CODES_CINNAMON_BUN - 1))
    }

    @Test
    fun `Android 17 SDK is Android 17 or higher`() {
        assertTrue(isAndroid17OrHigher(BUILD_VERSION_CODES_CINNAMON_BUN))
    }
}
