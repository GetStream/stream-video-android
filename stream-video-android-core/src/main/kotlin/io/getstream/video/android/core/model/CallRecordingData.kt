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
 * Represents a single call recording with its metadata.
 *
 * @param fileName The name of the file.
 * @param url The location of the recording file.
 * @param start The start time in epoch.
 * @param end The end time in epoch.
 */
@Stable
public data class CallRecordingData(
    val fileName: String,
    val url: String,
    val start: Long,
    val end: Long,
)
