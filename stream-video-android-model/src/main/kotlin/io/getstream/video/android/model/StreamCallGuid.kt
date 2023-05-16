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

package io.getstream.video.android.model

import android.content.Intent
import android.os.Build
import android.os.Parcelable
import io.getstream.video.android.model.mapper.toTypeAndId
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Represents call cid.
 */
public typealias StreamCallCid = String

/**
 * Represents call type.
 */
public typealias StreamCallType = String

/**
 * Represents call id.
 */
public typealias StreamCallId = String


@Parcelize
@Serializable
public data class StreamCallGuid constructor(
    public val type: StreamCallType,
    public val id: StreamCallId,
    public val cid: StreamCallCid = when {
        type.isNotEmpty() && id.isNotEmpty() -> "$type:$id"
        id.isNotEmpty() -> id
        else -> error("[StreamCallCid] invalid arguments; type=$type, id=$id")
    }
) : Parcelable {

    public companion object {

        /**
         * Parses [StreamCallGuid] of call to callType and channelId.
         *
         * @return Pair<StreamCallType, StreamCallId> Pair with callType and channelId.
         * @throws IllegalStateException Throws an exception if format of cid is incorrect.
         */
        @Throws(IllegalStateException::class)
        public fun StreamCallGuid.toTypeAndId(): Pair<String, String> {
            return cid.toTypeAndId()
        }

        public fun fromCallCid(cid: String): StreamCallGuid {
            val (type, id) = cid.toTypeAndId()
            return StreamCallGuid(type, id)
        }
    }
}

public fun Intent.streamCallGuid(key: String): StreamCallGuid? = when {
    Build.VERSION.SDK_INT >= 33 -> getParcelableExtra(key, StreamCallGuid::class.java)
    else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? StreamCallGuid
}