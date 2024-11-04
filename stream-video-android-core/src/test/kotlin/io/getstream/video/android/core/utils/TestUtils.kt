/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
 *
 * Licensed under the Stream License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/GetStream/stream-chat-android/blob/main/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.getstream.video.android.core.utils

import io.getstream.video.android.model.Device
import io.getstream.video.android.model.User
import org.openapitools.client.models.ChannelMute
import org.threeten.bp.Instant
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import java.io.File
import java.util.Calendar
import java.util.Date
import java.util.UUID
import kotlin.random.Random

private val charPool: CharArray = (('a'..'z') + ('A'..'Z') + ('0'..'9')).toCharArray()

public fun positiveRandomInt(maxInt: Int = Int.MAX_VALUE - 1): Int =
    Random.nextInt(1, maxInt + 1)

public fun positiveRandomLong(maxLong: Long = Long.MAX_VALUE - 1): Long =
    Random.nextLong(1, maxLong + 1)

public fun randomInt(): Int = Random.nextInt()
public fun randomIntBetween(min: Int, max: Int): Int = Random.nextInt(min, max + 1)
public fun randomLong(): Long = Random.nextLong()
public fun randomLongBetween(min: Long, max: Long = Long.MAX_VALUE - 1): Long = Random.nextLong(min, max + 1)
public fun randomBoolean(): Boolean = Random.nextBoolean()
public fun randomString(size: Int = 20): String = buildString(capacity = size) {
    repeat(size) {
        append(charPool.random())
    }
}

public fun randomDateOrNull(): OffsetDateTime? = randomDate().takeIf { randomBoolean() }
public fun randomDate(): OffsetDateTime = OffsetDateTime.ofInstant(Instant.ofEpochMilli(positiveRandomLong()), ZoneId.systemDefault())
public fun randomDateBefore(date: Long): Date = Date(date - positiveRandomInt())
public fun randomDateAfter(date: Long): Date = Date(randomLongBetween(date))
public fun randomCID(): String = "${randomString()}:${randomString()}"
public fun randomFile(extension: String = randomString(3)): File {
    return File("${randomString()}.$extension")
}

public fun randomUser(
    id: String = randomString(),
    name: String = randomString(),
    image: String = randomString(),
    role: String = randomString(),
    createdAt: OffsetDateTime? = randomDateOrNull(),
    updatedAt: OffsetDateTime? = randomDateOrNull(),
    teams: List<String> = listOf(),
): User = User(
    id = id,
    role = role,
    name = name,
    image = image,
    createdAt = createdAt,
    updatedAt = updatedAt,
    teams = teams,
)