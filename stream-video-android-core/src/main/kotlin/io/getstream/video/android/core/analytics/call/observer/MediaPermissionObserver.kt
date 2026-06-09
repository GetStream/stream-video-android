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

package io.getstream.video.android.core.analytics.call.observer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import io.getstream.video.android.core.analytics.call.observer.model.JoinReason
import io.getstream.video.android.core.analytics.reporting.ClientEventReporter

internal class MediaPermissionObserver(
    val context: Context,
    val callId: String,
    val callType: String,
    val eventReporter: ClientEventReporter,
    val joinTelemetryRepository: JoinTelemetryRepository,
) {

    fun mediaPermissionStatus() {
        eventReporter.reportMediaPermissionStatus(
            callId,
            callType,
            joinTelemetryRepository.state.value.joinStageAttemptId ?: "unknown",
            joinTelemetryRepository.state.value.joinReason ?: JoinReason.Unknown,
            isCameraPermissionGranted(),
            isMicrophonePermissionGranted(),
        )
    }

    fun isMicrophonePermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
    }
}
