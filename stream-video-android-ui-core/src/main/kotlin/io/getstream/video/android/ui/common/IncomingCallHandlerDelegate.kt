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

import android.content.Context
import android.content.Intent
import io.getstream.video.android.core.Call

/**
 * Interface to allow customization of call handling behavior
 */
public interface IncomingCallHandlerDelegate {
    /**
     * Called when a new call comes in while there's an ongoing call
     * @param activeCall The current ongoing call
     * @param intent New [Intent] which has new call information
     * @return true to accept the new call, false to ignore it
     */
    public fun shouldAcceptNewCall(activeCall: Call, intent: Intent): Boolean

    /**
     * Called when accepting a new call
     * @param intent The [Intent] for the call
     */
    public fun onAcceptCall(context: Context, intent: Intent)

    /**
     * Called when ignoring a call (same call or based on delegate decision)
     * @param intent New [Intent] which has new call information
     * @param reason The reason for ignoring [IgnoreReason.SameCall], [IgnoreReason.DelegateDeclined],
     * [IgnoreReason.Custom]
     */
    public fun onIgnoreCall(intent: Intent, reason: IgnoreReason)
}

/**
 * Use [IgnoreReason] when we want to ignore the incoming call while we are
 * already on a active call
 */
public sealed class IgnoreReason {
    public data object SameCall : IgnoreReason()
    public data object DelegateDeclined : IgnoreReason()
    public data class Custom(val message: String) : IgnoreReason()
}
