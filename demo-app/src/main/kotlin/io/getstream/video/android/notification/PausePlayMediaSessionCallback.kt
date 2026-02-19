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

package io.getstream.video.android.notification

import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import io.getstream.video.android.core.StreamVideo

/**
 * Callback for media session.
 */
class PausePlayMediaSessionCallback : MediaSessionCompat.Callback() {

    override fun onPause() {
        Log.d("StreamVideoInitHelper", "Pause")
        StreamVideo.instanceOrNull()?.state?.activeCall?.value?.debug?.pause()
        super.onPause()
    }

    override fun onPlay() {
        Log.d("StreamVideoInitHelper", "Play")
        StreamVideo.instanceOrNull()?.state?.activeCall?.value?.debug?.resume()
        super.onPlay()
    }
}
