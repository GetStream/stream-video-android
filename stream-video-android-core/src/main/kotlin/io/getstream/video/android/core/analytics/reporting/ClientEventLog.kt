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

package io.getstream.video.android.core.analytics.reporting

import io.getstream.android.video.api.generated.models.ClientEvent

internal fun ClientEvent.toLog(): String {
    return buildString {
        append("ClientEvent(")

        appendIfNotNull("type", type)
        appendIfNotNull("eventType", eventType)
        appendIfNotNull("stage", stage)
        appendIfNotNull("outcome", outcome)

        appendIfNotNull("callSessionId", callSessionId)
        appendIfNotNull("stageId", stageId)
        appendIfNotNull("userSessionId", userSessionId)
        appendIfNotNull("joinAttemptId", joinAttemptId)
        appendIfNotNull("joinReason", joinReason)
        appendIfNotNull("source", source)
        appendIfNotNull("coordinatorConnectId", coordinatorConnectId)

        appendIfNotNull("userId", userId)
        appendIfNotNull("sfuId", sfuId)

        appendIfNotNull("peerConnection", peerConnection)
        appendIfNotNull("iceState", iceState)

        appendIfNotNull("retryCountAttempt", retryCountAttempt)
        appendIfNotNull("retryFailureCode", retryFailureCode)
        appendIfNotNull("retryFailureReason", retryFailureReason)

        appendIfNotNull("elapsedTime", elapsedTime)

        appendIfNotNull(
            "wasPreviouslyConnected",
            wasPreviouslyConnected,
        )

        appendIfNotNull(
            "previouslyConnectedTimestamp",
            previouslyConnectedTimestamp,
        )

        appendIfNotNull("sdkVersion", sdkVersion)
        appendIfNotNull("timestamp", timestamp)
        appendIfNotNull("cameraPermissionStatus", cameraPermissionStatus)
        appendIfNotNull("microphonePermissionStatus", microphonePermissionStatus)
        appendIfNotNull("screenShareStatus", screenShareStatus)
        appendIfNotNull("trackId", trackId)

        append(")")
    }
}

private fun StringBuilder.appendIfNotNull(
    key: String,
    value: Any?,
) {
    if (value == null) return

    if (!endsWith("(")) {
        append(",\n")
    }

    append(key)
    append("=")
    append(value)
}
