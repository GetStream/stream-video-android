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

package io.getstream.video.android.core.base

import io.getstream.log.KotlinStreamLogger
import io.getstream.log.Priority
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * A logger that prints to stdout
 */
internal class StreamTestLogger : KotlinStreamLogger() {
    public override val now: () -> LocalDateTime = {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    }

    override fun log(priority: Priority, tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) {
        }
    }

    override fun install(minPriority: Priority, maxTagLength: Int) {}
}
