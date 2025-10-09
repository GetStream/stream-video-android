/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.model

import android.content.Intent
import android.os.Parcelable
import androidx.compose.runtime.Stable
import androidx.core.content.IntentCompat
import io.getstream.video.android.core.notifications.NotificationType
import io.getstream.video.android.model.mapper.toTypeAndId
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Represents the stream call details about [type], [id], and [cid], which is essential for joining a call.
 *
 * @param type A call type.
 * @param id A call id.
 * @param cid A cid, which consists of the [type] and [id]. For instance, `default:123`
 * @param isValid Represents is the [cid] is valid or not.
 */
@Stable
@Parcelize
@Serializable
public data class StreamCallId constructor(
    public val type: String,
    public val id: String,
    public val cid: String = when {
        type.isNotEmpty() && id.isNotEmpty() -> "$type:$id"
        id.isNotEmpty() -> id
        else -> error("[StreamCallId] invalid arguments; type=$type, id=$id")
    },
    public val isValid: Boolean = type.isNotEmpty() || id.isNotEmpty(),
) : Parcelable {

    public companion object {

        /**
         * Parses [StreamCallId] of call to callType and channelId.
         *
         * @return Pair<String, String> Pair with type and id.
         * @throws IllegalStateException Throws an exception if format of cid is incorrect.
         */
        @Throws(IllegalStateException::class)
        public fun StreamCallId.toTypeAndId(): Pair<String, String> {
            return cid.toTypeAndId()
        }

        /**
         * Create a new [StreamCallId] with a given [cid].
         *
         * @return A new [StreamCallId].
         */
        public fun fromCallCid(cid: String): StreamCallId {
            val (type, id) = cid.toTypeAndId()
            return StreamCallId(type = type.trim(), id = id.trim())
        }
    }

    fun getNotificationId(notificationType: NotificationType): Int {
        return (notificationType.type + cid).hashCode()
    }
}

/**
 * Gets a [StreamCallId], which is [Parcelable] object from an [Intent] with a given [key].
 *
 * @return A parceled [StreamCallId].
 */
public fun Intent.streamCallId(key: String): StreamCallId? =
    IntentCompat.getParcelableExtra(this, key, StreamCallId::class.java)

public fun Intent.streamCallDisplayName(key: String): String? = this.getStringExtra(key)
