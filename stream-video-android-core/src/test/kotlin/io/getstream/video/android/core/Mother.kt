package io.getstream.video.android.core

import io.getstream.video.android.model.User
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.threeten.bp.Instant
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import java.io.File
import java.util.Date
import kotlin.random.Random

private val charPool: CharArray = (('a'..'z') + ('A'..'Z') + ('0'..'9')).toCharArray()

public fun positiveRandomInt(maxInt: Int = Int.MAX_VALUE - 1): Int =
    Random.nextInt(1, maxInt + 1)

public fun positiveRandomLong(maxLong: Long = Long.MAX_VALUE - 1): Long =
    Random.nextLong(1, maxLong + 1)

public fun randomInt(): Int = Random.nextInt()
public fun randomIntBetween(min: Int, max: Int): Int = Random.nextInt(min, max + 1)
public fun randomLong(): Long = Random.nextLong()
public fun randomLongBetween(
    min: Long,
    max: Long = Long.MAX_VALUE - 1,
): Long = Random.nextLong(min, max + 1)
public fun randomBoolean(): Boolean = Random.nextBoolean()
public fun randomString(size: Int = 20): String = buildString(capacity = size) {
    repeat(size) {
        append(charPool.random())
    }
}

public fun randomDateOrNull(): OffsetDateTime? = randomDate().takeIf { randomBoolean() }
public fun randomDate(): OffsetDateTime = OffsetDateTime.ofInstant(
    Instant.ofEpochMilli(positiveRandomLong()),
    ZoneId.systemDefault(),
)
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

/**
 * Provides a GET request with a configurable [url].
 */
fun randomGetRequest(
    url: String = "http://${randomString()}",
): Request =
    Request.Builder()
        .url(url)
        .build()

/**
 * Provides a POST request with a configurable [url] and [body].
 */
fun randomPostRequest(
    url: String = "http://${randomString()}",
    body: String = randomString(),
): Request =
    Request.Builder()
        .url(url)
        .post(body.toRequestBody())
        .build()