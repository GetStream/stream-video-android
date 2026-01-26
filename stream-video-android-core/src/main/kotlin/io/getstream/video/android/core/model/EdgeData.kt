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

/**
 * Represents the information about an Edge center in our network.
 *
 * @param id The ID of the center.
 * @param latencyTestUrl URL of the result for latency measurements.
 * @param latitude The latitude of the server location.
 * @param longitude The longitude of the server location.
 */
@Stable
public data class EdgeData(
    val id: String,
    val latencyTestUrl: String,
    val latitude: Float,
    val longitude: Float,
    val green: Int,
    val yellow: Int,
    val red: Int,
)
