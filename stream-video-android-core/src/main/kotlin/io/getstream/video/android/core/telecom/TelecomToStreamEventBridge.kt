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

package io.getstream.video.android.core.telecom

import android.os.Build
import android.telecom.DisconnectCause
import androidx.annotation.RequiresApi
import io.getstream.log.taggedLogger
import kotlin.getValue

@RequiresApi(Build.VERSION_CODES.O)
internal class TelecomToStreamEventBridge(telecomCall: TelecomCall) {
    // TODO-Telecom: review SDK methods that are called here and take results into account
    private val logger by taggedLogger(TELECOM_LOG_TAG)
    private val streamCall = telecomCall.streamCall

    suspend fun onAnswer(callType: Int) {
        logger.d { "[TelecomToStreamEventBridge#onAnswer]" }
        streamCall.accept()
        streamCall.join()
    }

    suspend fun onDisconnect(cause: DisconnectCause) {
        logger.d { "[TelecomToStreamEventBridge#onDisconnect]" }
        streamCall.leave()
    }

    suspend fun onSetActive() {
        logger.d { "[TelecomToStreamEventBridge#onSetActive]" }
        streamCall.join()
    }

    suspend fun onSetInactive() {
        logger.d { "[TelecomToStreamEventBridge#onSetInactive]" }
        streamCall.leave()
    }
}
