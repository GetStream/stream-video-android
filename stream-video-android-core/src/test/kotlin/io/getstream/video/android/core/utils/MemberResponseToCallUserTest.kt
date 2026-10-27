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

import com.google.common.truth.Truth.assertThat
import io.getstream.android.video.generated.models.MemberResponse
import io.getstream.android.video.generated.models.UserResponse
import org.junit.Test
import org.threeten.bp.OffsetDateTime

class MemberResponseToCallUserTest {

    @Test
    fun `toCallUser keeps the member dates`() {
        val createdAt = OffsetDateTime.parse("2026-10-22T08:15:30.123Z")
        val updatedAt = OffsetDateTime.parse("2026-10-22T09:00:00.456+02:00")
        val member = MemberResponse(
            createdAt = createdAt,
            updatedAt = updatedAt,
            userId = "user",
            user = UserResponse(
                createdAt = createdAt,
                id = "user",
                language = "en",
                role = "user",
                updatedAt = updatedAt,
            ),
        )

        val callUser = member.toCallUser()

        assertThat(callUser.createdAt!!.time).isEqualTo(createdAt.toInstant().toEpochMilli())
        assertThat(callUser.updatedAt!!.time).isEqualTo(updatedAt.toInstant().toEpochMilli())
    }
}
