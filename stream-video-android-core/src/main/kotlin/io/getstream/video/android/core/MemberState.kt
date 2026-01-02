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

import androidx.compose.runtime.Immutable
import io.getstream.video.android.model.User

@Immutable
public data class MemberState(
    val user: User,
    val custom: Map<String, Any?>,
    val role: String?,
    val createdAt: org.threeten.bp.OffsetDateTime,
    val updatedAt: org.threeten.bp.OffsetDateTime,
    val deletedAt: org.threeten.bp.OffsetDateTime? = null,
    val acceptedAt: org.threeten.bp.OffsetDateTime? = null,
    val rejectedAt: org.threeten.bp.OffsetDateTime? = null,

) {
    // Probably an easy way to get the participant would be nice
}
