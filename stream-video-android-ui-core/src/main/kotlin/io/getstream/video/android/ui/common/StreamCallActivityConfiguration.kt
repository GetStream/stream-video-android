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

package io.getstream.video.android.ui.common

import android.os.Bundle

internal object StreamCallActivityConfigStrings {
    const val EXTRA_STREAM_CONFIG = "stream-activity-config"
    const val EXTRA_CLOSE_ON_ERROR = "close-on-error"
    const val EXTRA_CLOSE_ON_ENDED = "close-on-ended"
    const val EXTRA_CAN_SKIP_RATIONALE = "skip-rationale-allowed"
    const val EXTRA_CUSTOM = "custom-fields"
}

/**
 * A configuration that controls various behaviors of the [StreamCallActivity].
 */
public data class StreamCallActivityConfiguration(
    /** When there has been a technical error, close the screen. */
    val closeScreenOnError: Boolean = true,
    /** When the call has ended for any reason, close the screen */
    val closeScreenOnCallEnded: Boolean = true,
    /** When set to false, the activity will simply ignore the `showRationale` from the system and show the rationale screen anyway. */
    val canSkiPermissionRationale: Boolean = true,
    /**
     * Custom configuration extension for any extending classes.
     * Can be used same as normal extras.
     */
    val custom: Bundle? = null,
) {
}

/**
 * Extract a [StreamCallActivityConfigStrings] from bundle.
 */
public fun Bundle.extractStreamActivityConfig(): StreamCallActivityConfiguration {
    val closeScreenOnError =
        getBoolean(StreamCallActivityConfigStrings.EXTRA_CLOSE_ON_ERROR, true)
    val closeScreenOnCallEnded =
        getBoolean(StreamCallActivityConfigStrings.EXTRA_CLOSE_ON_ENDED, true)
    val canSkipPermissionRationale =
        getBoolean(StreamCallActivityConfigStrings.EXTRA_CAN_SKIP_RATIONALE, true)
    val custom = getBundle(StreamCallActivityConfigStrings.EXTRA_CUSTOM)
    return StreamCallActivityConfiguration(
        closeScreenOnError = closeScreenOnError,
        closeScreenOnCallEnded = closeScreenOnCallEnded,
        canSkiPermissionRationale = canSkipPermissionRationale,
        custom = custom,
    )
}

/**
 * Add a [StreamCallActivityConfiguration] into a bundle.
 */
public fun StreamCallActivityConfiguration.toBundle(): Bundle {
    val bundle = Bundle()
    bundle.putBoolean(StreamCallActivityConfigStrings.EXTRA_CLOSE_ON_ERROR, closeScreenOnError)
    bundle.putBoolean(StreamCallActivityConfigStrings.EXTRA_CLOSE_ON_ENDED, closeScreenOnCallEnded)
    bundle.putBoolean(
        StreamCallActivityConfigStrings.EXTRA_CAN_SKIP_RATIONALE,
        canSkiPermissionRationale
    )
    bundle.putBundle(StreamCallActivityConfigStrings.EXTRA_CUSTOM, custom)
    return bundle
}
