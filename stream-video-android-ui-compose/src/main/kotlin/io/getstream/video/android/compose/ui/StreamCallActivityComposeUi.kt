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

package io.getstream.video.android.compose.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.MemberState
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.ui.common.StreamActivityUiDelegate
import io.getstream.video.android.ui.common.StreamCallActivity
import io.getstream.video.android.ui.common.util.StreamCallActivityDelicateApi
import java.lang.Exception

public interface StreamCallActivityComposeUi : StreamActivityUiDelegate<StreamCallActivity> {

    /**
     * Root content of the UI. If you override this, the rest might not work as expected and need to
     * be called from this method.
     *
     * @param call the call.
     */
    @StreamCallActivityDelicateApi
    @Composable
    public fun StreamCallActivity.RootContent(call: Call)

    /**
     * Content shown when there is data to be loaded.
     *
     * @param call the call.
     */
    @Composable
    public fun StreamCallActivity.LoadingContent(call: Call)

    /**
     * Content for in-call audio-only calls.
     * Audio call is a call which returns `false` for [Call.hasCapability] when the argument is `SendVideo`
     *
     * @param call the call.
     */
    @Composable
    public fun StreamCallActivity.AudioCallContent(call: Call)

    /**
     * Content for in-call for every other call that has video capabilities.
     * Default call is a call which returns `true` for [Call.hasCapability] when the argument is `SendVideo`
     *
     * @param call the call.
     */
    @Composable
    public fun StreamCallActivity.VideoCallContent(call: Call)

    /**
     * Content for outgoing call.
     *
     * @param call the call.
     * @param modifier the UI modifier
     * @param isVideoType if the call is video or not
     * @param isShowingHeader if the header will be shown
     * @param headerContent the content of the header
     * @param detailsContent the details content (participant avatars etc..)
     */
    @Composable
    public fun StreamCallActivity.OutgoingCallContent(
        modifier: Modifier,
        call: Call,
        isVideoType: Boolean,
        isShowingHeader: Boolean,
        headerContent: (@Composable ColumnScope.() -> Unit)?,
        detailsContent: (
            @Composable ColumnScope.(
                participants: List<MemberState>,
                topPadding: Dp,
            ) -> Unit
        )?,
        controlsContent: (@Composable BoxScope.() -> Unit)?,
        onBackPressed: () -> Unit,
        onCallAction: (CallAction) -> Unit,
    )

    /**
     * Content for incoming call.
     *
     * @param call the call.
     * @param modifier the modifier
     * @param isVe
     */
    @Composable
    public fun StreamCallActivity.IncomingCallContent(
        modifier: Modifier,
        call: Call,
        isVideoType: Boolean,
        isShowingHeader: Boolean,
        headerContent: (@Composable ColumnScope.() -> Unit)?,
        detailsContent: (
            @Composable ColumnScope.(
                participants: List<MemberState>,
                topPadding: Dp,
            ) -> Unit
        )?,
        controlsContent: (@Composable BoxScope.() -> Unit)?,
        onBackPressed: () -> Unit,
        onCallAction: (CallAction) -> Unit,
    )

    /**
     * Content for when the call was not answered.
     *
     * @param call the call.
     */
    @Composable
    public fun StreamCallActivity.NoAnswerContent(call: Call)

    /**
     * Call when the call was rejected.
     *
     * @param call the call.
     */
    @Composable
    public fun StreamCallActivity.RejectedContent(call: Call)

    /**
     * Content when the call has failed for whatever reason.
     *
     * @param call the call
     */
    @Composable
    public fun StreamCallActivity.CallFailedContent(call: Call, exception: Exception)

    /**
     * Content when the call has failed for whatever reason.
     *
     * @param call the call
     */
    @Composable
    public fun StreamCallActivity.CallDisconnectedContent(call: Call)

    /**
     * Content shown when the required permissions are not granted and the call cannot happen.
     * Note: There are other places that permissions are required like in the service etc..
     * Best practice is to request these permissions a screen before starting the call.
     */
    @Composable
    public fun StreamCallActivity.PermissionsRationaleContent(
        call: Call,
        granted: List<String>,
        notGranted: List<String>,
    )
}
