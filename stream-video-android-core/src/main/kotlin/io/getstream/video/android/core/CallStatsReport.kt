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

import io.getstream.video.android.core.call.stats.model.RtcStatsReport
import io.getstream.video.android.core.internal.InternalStreamVideoApi
import io.getstream.video.android.core.model.StreamPeerType
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.RTCStats

@InternalStreamVideoApi
data class CallStatsReport(
    val publisher: RtcStatsReport?,
    val subscriber: RtcStatsReport?,
    val local: LocalStats?,
    val stateStats: CallStats,
)

fun CallStatsReport.toJson(peerType: StreamPeerType): String {
    val statsKey: String
    val stats: Map<String, RTCStats>?

    if (peerType == StreamPeerType.PUBLISHER) {
        statsKey = "publisherStats"
        stats = publisher?.origin?.statsMap
    } else {
        statsKey = "subscriberStats"
        stats = subscriber?.origin?.statsMap
    }

    return JSONObject().apply {
        put(
            statsKey,
            JSONArray().also { array ->
                stats?.forEach { statsEntry ->
                    array.put(
                        JSONObject().apply {
                            put(
                                statsEntry.key,
                                JSONObject().apply {
                                    statsEntry.value.members.forEach { (key, value) ->
                                        put(
                                            key,
                                            try {
                                                JSONObject(value.toString())
                                            } catch (e: JSONException) {
                                                value
                                            },
                                        )
                                    }
                                },
                            )
                        },
                    )
                }
            },
        )
    }.toString(4)
}
