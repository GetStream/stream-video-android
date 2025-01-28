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

package io.getstream.video.android.core.telecom.bridge.telecomtostream

import android.telecom.DisconnectCause
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.telecom.TELECOM_LOG_TAG
import io.getstream.video.android.core.telecom.TelecomCall
import kotlin.getValue

internal fun defaultTelecomToStreamEventBridgeFactory(): TelecomToStreamEventBridgeFactory {
    return DefaultTelecomToStreamEventBridgeFactory()
}

internal class DefaultTelecomToStreamEventBridgeFactory : TelecomToStreamEventBridgeFactory {
    override fun create(telecomCall: TelecomCall): TelecomToStreamEventBridge {
        return DefaultTelecomToStreamEventBridge(telecomCall)
    }
}

internal class DefaultTelecomToStreamEventBridge(telecomCall: TelecomCall) : TelecomToStreamEventBridge {
    // TODO-Telecom: review SDK methods that are called here and take results into account
    private val logger by taggedLogger(TELECOM_LOG_TAG)
    private val streamCall = telecomCall.streamCall

    override suspend fun onAnswer(callType: Int) {
        logger.d { "[DefaultTelecomToStreamEventBridge#onAnswer]" }
        streamCall.accept()
        streamCall.join()
    }

    override suspend fun onDisconnect(cause: DisconnectCause) {
        logger.d { "[DefaultTelecomToStreamEventBridge#onDisconnect]" }
        streamCall.leave()
    }

    override suspend fun onSetActive() {
        logger.d { "[DefaultTelecomToStreamEventBridge#onSetActive]" }
        streamCall.join()
    }

    override suspend fun onSetInactive() {
        logger.d { "[DefaultTelecomToStreamEventBridge#onSetInactive]" }
        streamCall.leave()
    }
}
