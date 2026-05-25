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

package io.getstream.video.android.core.faultinjector

import io.getstream.video.android.core.internal.InternalStreamVideoApi

@InternalStreamVideoApi
public enum class FaultKey {

    // REST
    FAIL_JOIN_CALL,
    FAIL_CREATE_CALL,

    // WebSocket
    FAIL_WS_CONNECT,
    FAIL_WS_SEND,

    // RTC
    FAIL_PEER_CONNECTION_CREATE,
    FAIL_SET_REMOTE_DESCRIPTION,

    // MediaCodec
    FAIL_VIDEO_ENCODER_INIT,
}
