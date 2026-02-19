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

package io.getstream.video.android.core.socket.internal

internal enum class EventType(val type: String) {
    HEALTH_CHECK("health.check"),
    CONNECTION_OK("connection.ok"),
    CALL_CREATED("call.created"),
    CALL_ACCEPTED("call.accepted"),
    CALL_REJECTED("call.rejected"),
    CALL_CANCELLED("call.cancelled"),
    CALL_UPDATED("call.updated"),
    CALL_ENDED("call.ended"),
    PERMISSION_REQUEST("call.permission_request"),
    UPDATED_CALL_PERMISSIONS("call.permissions_updated"),
    RECORDING_STARTED("call.recording_started"),
    RECORDING_STOPPED("call.recording_stopped"),
    BLOCKED_USER("call.blocked_user"),
    UNBLOCKED_USER("call.unblocked_user"),
    CUSTOM("custom"),
    UNKNOWN("unknown"),
    ;

    companion object {
        fun from(value: String): EventType {
            return when (value) {
                HEALTH_CHECK.type -> HEALTH_CHECK
                CALL_CREATED.type -> CALL_CREATED
                CALL_ACCEPTED.type -> CALL_ACCEPTED
                CALL_REJECTED.type -> CALL_REJECTED
                CALL_CANCELLED.type -> CALL_CANCELLED
                CALL_UPDATED.type -> CALL_UPDATED
                CALL_ENDED.type -> CALL_ENDED
                CONNECTION_OK.type -> CONNECTION_OK
                PERMISSION_REQUEST.type -> PERMISSION_REQUEST
                UPDATED_CALL_PERMISSIONS.type -> UPDATED_CALL_PERMISSIONS
                CUSTOM.type -> CUSTOM

                else -> UNKNOWN
            }
        }
    }
}
