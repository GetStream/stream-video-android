/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

@file:OptIn(ExperimentalSerializationApi::class)

package io.getstream.video.android.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.format.DateTimeFormatter

@Serializable
public sealed class UserType {
    /** A user that's authenticated in your system */
    @Serializable
    public object Authenticated : UserType()

    /** A temporary guest user, that can have an image, name etc */
    @Serializable
    public object Guest : UserType()

    /** Not authentication, anonymous user. Commonly used for audio rooms and livestreams */
    @Serializable
    public object Anonymous : UserType()
}

@Serializer(forClass = OffsetDateTime::class)
public object OffsetDateTimeSerializer : KSerializer<OffsetDateTime> {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    override fun serialize(encoder: Encoder, value: OffsetDateTime) {
        encoder.encodeString(value.format(formatter))
    }

    override fun deserialize(decoder: Decoder): OffsetDateTime {
        return OffsetDateTime.parse(decoder.decodeString(), formatter)
    }
}

@Serializable
public data class User(
    /** ID is required, the rest is optional */
    // TODO: TBD, do we generate the ID client side or not... TBD
    val id: String,
    val role: String = "",
    val type: UserType = UserType.Authenticated,
    val name: String = "",
    val image: String = "",
    val teams: List<String> = emptyList(),
    val custom: Map<String, String> = emptyMap(),
    @Serializable(with = OffsetDateTimeSerializer::class)
    val createdAt: OffsetDateTime? = null,
    @Serializable(with = OffsetDateTimeSerializer::class)
    val updatedAt: OffsetDateTime? = null,
    @Serializable(with = OffsetDateTimeSerializer::class)
    val deletedAt: OffsetDateTime? = null,
) {
    public fun isValid(): Boolean {
        return id.isNotEmpty()
    }
}
