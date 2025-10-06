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

package io.getstream.video.android.core

import android.util.Log
import io.getstream.video.android.core.call.stats.model.RtcStatsReport
import io.getstream.video.android.core.internal.InternalStreamVideoApi
import io.getstream.video.android.core.model.StreamPeerType
import io.getstream.webrtc.RTCStats
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

@InternalStreamVideoApi
data class CallStatsReport(
    val publisher: RtcStatsReport?,
    val subscriber: RtcStatsReport?,
    val local: LocalStats?,
    val stateStats: CallStats,
)

fun CallStatsReport.toJson(peerType: StreamPeerType): String {
    val stats: Map<String, RTCStats>? = if (peerType == StreamPeerType.PUBLISHER) {
        publisher?.origin?.statsMap
    } else {
        subscriber?.origin?.statsMap
    }

    return JSONArray().also { array ->
        stats?.forEach { statsEntry ->
            array.put(
                JSONObject().apply {
                    // Cannot use statsEntry.value.toString() because it's not valid JSON sometimes
                    // So we add the properties one by one
                    put("timestamp", statsEntry.value.timestampUs)
                    put("id", statsEntry.value.id)
                    put("type", statsEntry.value.type)
                    statsEntry.value.members.forEach { (key, value) ->
                        put(
                            key,
                            try {
                                // Sometimes the member is a JSON string, so we try to parse it
                                when (value) {
                                    is Array<*> -> JSONArray(value)
                                    is Map<*, *> -> JSONObject(value.toString())
                                    else -> value // Or we just add the value
                                }
                            } catch (e: JSONException) {
                                Log.d(
                                    "CallStatsReport",
                                    "sendStats. Error for $key with $value: ${e.message}",
                                )
                                ""
                            },
                        )
                    }
                },
            )
        }
    }.toString(2)
}
