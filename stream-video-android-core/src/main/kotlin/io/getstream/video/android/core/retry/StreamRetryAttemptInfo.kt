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

package io.getstream.video.android.core.retry

/**
 * Snapshot describing the state of the current retry loop iteration.
 * Copied from stream-core-android's `StreamRetryAttemptInfo`.
 *
 * @property attempt 1-based attempt index.
 * @property currentDelay Delay (ms) applied before this attempt.
 * @property previousAttemptError The error from the previous attempt, or null for the first.
 * @property policy The retry policy governing this run.
 */
internal data class StreamRetryAttemptInfo(
    val attempt: Int,
    val currentDelay: Long,
    val previousAttemptError: Throwable? = null,
    val policy: StreamRetryPolicy,
)
