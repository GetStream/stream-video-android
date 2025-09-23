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

package io.getstream.video.android.core.call

import io.getstream.video.android.core.Call

/**
 * Configuration knobs for the [Call.join] flow.
 *
 * @property rejectRingingCallOnFailure Remove the call from the local ringing list if the join attempt fails.
 * @property maxRetries Number of retry attempts for transient join errors before surfacing failure.
 */
data class JoinCallConfig(
    val rejectRingingCallOnFailure: Boolean = false,
    val maxRetries: Int = 3,
)
