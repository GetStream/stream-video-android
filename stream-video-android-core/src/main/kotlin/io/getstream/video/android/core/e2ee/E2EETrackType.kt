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

package io.getstream.video.android.core.e2ee

import stream.video.sfu.models.TrackType

/**
 * The kind of track being encrypted or decrypted, passed to [E2EEManager] so implementations can
 * key or tag frames per media type.
 *
 * This is a public mirror of the SFU's internal track type rather than a re-export of it, so that
 * custom [E2EEManager] implementations do not have to depend on generated protobuf types.
 */
public enum class E2EETrackType {
    AUDIO,
    VIDEO,
    SCREEN_SHARE,
    SCREEN_SHARE_AUDIO,
}

/**
 * Maps an SFU track type onto its public counterpart. Returns `null` for unspecified or unknown
 * types, which [E2EEManager] implementations receive as "no hint available".
 */
internal fun TrackType.toE2EETrackType(): E2EETrackType? = when (this) {
    TrackType.TRACK_TYPE_AUDIO -> E2EETrackType.AUDIO
    TrackType.TRACK_TYPE_VIDEO -> E2EETrackType.VIDEO
    TrackType.TRACK_TYPE_SCREEN_SHARE -> E2EETrackType.SCREEN_SHARE
    TrackType.TRACK_TYPE_SCREEN_SHARE_AUDIO -> E2EETrackType.SCREEN_SHARE_AUDIO
    TrackType.TRACK_TYPE_UNSPECIFIED -> null
}
