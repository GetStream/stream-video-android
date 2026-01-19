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

package io.getstream.video.android.core.model

import androidx.compose.runtime.Stable
import io.getstream.video.android.model.User

/**
 * Represents the information about a successfully sent reaction.
 *
 * @param type The type of reaction.
 * @param user The User who sent the reaction.
 * @param emoji Code of the emoji, if it exists.
 * @param custom Custom extra data to enrich the reaction.
 */
@Stable
public data class ReactionData(
    public val type: String,
    public val user: User,
    public val emoji: String?,
    public val custom: Map<String, Any?>?,
)
