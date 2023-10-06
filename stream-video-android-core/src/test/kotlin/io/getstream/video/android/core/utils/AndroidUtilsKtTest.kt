/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

import android.content.Context
import android.content.res.Resources
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals

class AndroidUtilsKtTest {

    @Test
    fun `For valid resId the value is returned from stringOrDefault`() {
        // Given
        val resId = 123
        val context = mockk<Context>(relaxed = true)
        val expected = "String from strings.xml"
        every { context.getString(resId) } returns expected

        // When
        val actual = stringOrDefault(context, resId, "some default")

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `For invalid resId the default value is returned from stringOrDefault`() {
        // Given
        val resId = 123
        val context = mockk<Context>(relaxed = true)
        val default = "some default"
        every { context.getString(resId) } throws Resources.NotFoundException()

        // When
        val actual = stringOrDefault(context, resId, default)

        // Then
        assertEquals(default, actual)
    }
}
