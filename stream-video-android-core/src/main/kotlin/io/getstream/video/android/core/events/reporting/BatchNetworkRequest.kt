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

package io.getstream.video.android.core.events.reporting

import android.content.Context
import io.getstream.android.video.generated.apis.ProductvideoApi
import io.getstream.android.video.generated.infrastructure.Serializer
import io.getstream.android.video.generated.models.ClientEvent
import io.getstream.android.video.generated.models.ReportClientEventRequest
import io.getstream.log.taggedLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

internal class BatchNetworkRequest(
    private val context: Context,
    private val file: File,
    private val api: ProductvideoApi,
    private val scope: CoroutineScope,
) {

    private val logger by taggedLogger("BatchNetworkRequest")
    private val adapter = Serializer.moshi.adapter(ClientEvent::class.java)

    fun write(clientEvent: ClientEvent) {
        file.appendText(adapter.toJson(clientEvent) + "\n")
    }

    fun readAll(size: Int): List<ClientEvent> {
        if (!file.exists()) return emptyList()
        return file.readLines()
            .take(size)
            .mapNotNull { runCatching { adapter.fromJson(it) }.getOrNull() }
    }

    fun sendBatchNetworkRequest() {
        scope.launch {
            // TODO: wrap with StreamRetryPolicy when retries are added
            var clientEventsSize = 0
            runCatching {
                val clientEvents = readAll(Integer.MAX_VALUE)
                clientEventsSize = clientEvents.size
                api.reportClientCallEvent(ReportClientEventRequest(clientEvents))
            }.onFailure { e ->
                logger.w { "[sendEvent] Failed to send client event: ${e.message}" }
            }.onSuccess {
                removeLinesFromFile(clientEventsSize)
            }
        }
    }

    fun removeLinesFromFile(linesToRemove: Int) {
        if (!file.exists() || linesToRemove <= 0) return
        val remaining = file.readLines().drop(linesToRemove)
        file.writeText(
            if (remaining.isEmpty()) {
                ""
            } else {
                remaining.joinToString("\n", postfix = "\n")
            },
        )
    }
}
