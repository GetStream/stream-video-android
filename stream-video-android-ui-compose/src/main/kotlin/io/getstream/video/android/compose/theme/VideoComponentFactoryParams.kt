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

package io.getstream.video.android.compose.theme

import android.view.View
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import io.getstream.video.android.compose.ui.components.call.pinning.ParticipantAction
import io.getstream.video.android.compose.ui.components.call.renderer.LayoutType
import io.getstream.video.android.compose.ui.components.call.renderer.MirrorMode
import io.getstream.video.android.compose.ui.components.call.renderer.RegularVideoRendererStyle
import io.getstream.video.android.compose.ui.components.call.renderer.VideoRendererStyle
import io.getstream.video.android.compose.ui.components.video.VideoScalingType
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.MemberState
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.model.NetworkQuality
import io.getstream.video.android.core.model.ScreenSharingSession
import io.getstream.video.android.model.User

/**
 * Parameters for [VideoComponentFactory.CallAppBar].
 *
 * @param call The call that contains all the participants state and tracks.
 * @param modifier Modifier for styling.
 * @param onBackPressed Handler when the user taps on the back button.
 * @param onCallAction Handler when the user triggers a Call Control Action.
 * @param title The title shown in the app bar. When `null`, the default title is used.
 */
public data class CallAppBarParams(
    val call: Call,
    val modifier: Modifier = Modifier,
    val onBackPressed: () -> Unit = {},
    val onCallAction: (CallAction) -> Unit = {},
    val title: String? = null,
)

/**
 * Parameters for [VideoComponentFactory.CallAppBarLeadingContent].
 *
 * @param call The call that contains all the participants state and tracks.
 * @param onBackPressed Handler when the user taps on the back button.
 */
public data class CallAppBarLeadingContentParams(
    val call: Call,
    val onBackPressed: () -> Unit = {},
)

/**
 * Parameters for [VideoComponentFactory.CallAppBarCenterContent].
 *
 * @param call The call that contains all the participants state and tracks.
 * @param title The title shown in the app bar. When `null`, the default title is used.
 */
public data class CallAppBarCenterContentParams(
    val call: Call,
    val title: String? = null,
)

/**
 * Parameters for [VideoComponentFactory.CallAppBarTrailingContent].
 *
 * @param call The call that contains all the participants state and tracks.
 * @param onCallAction Handler when the user triggers a Call Control Action.
 */
public data class CallAppBarTrailingContentParams(
    val call: Call,
    val onCallAction: (CallAction) -> Unit = {},
)

/**
 * Parameters for [VideoComponentFactory.CallContentVideoContent].
 *
 * @param call The call that contains all the participants state and tracks.
 * @param layoutType The type of layout used to render the participants.
 * @param style Defined properties for styling a single video call track.
 * @param videoRenderer A single video renderer that renders each individual participant. Defaults
 * to [VideoComponentFactory.ParticipantVideo].
 * @param floatingVideoRenderer A floating video renderer that renders the local participant.
 * When `null`, the default floating video renderer is used.
 */
public data class CallContentVideoContentParams(
    val call: Call,
    val layoutType: LayoutType = LayoutType.DYNAMIC,
    val style: VideoRendererStyle = RegularVideoRendererStyle(),
    val videoRenderer: @Composable (
        modifier: Modifier,
        call: Call,
        participant: ParticipantState,
        style: VideoRendererStyle,
    ) -> Unit = { videoModifier, videoCall, videoParticipant, videoStyle ->
        VideoTheme.componentFactory.ParticipantVideo(
            params = ParticipantVideoParams(
                call = videoCall,
                participant = videoParticipant,
                modifier = videoModifier,
                style = videoStyle,
            ),
        )
    },
    val floatingVideoRenderer: (@Composable BoxScope.(call: Call, IntSize) -> Unit)? = null,
)

/**
 * Parameters for [VideoComponentFactory.CallContentVideoOverlayContent].
 *
 * @param call The call that contains all the participants state and tracks.
 */
public data class CallContentVideoOverlayContentParams(
    val call: Call,
)

/**
 * Parameters for [VideoComponentFactory.CallContentPictureInPictureContent].
 *
 * @param call The call that contains all the participants state and tracks.
 */
public data class CallContentPictureInPictureContentParams(
    val call: Call,
)

/**
 * Parameters for [VideoComponentFactory.CallContentClosedCaptions].
 *
 * @param call The call that contains all the participants state and tracks.
 */
public data class CallContentClosedCaptionsParams(
    val call: Call,
)

/**
 * Parameters for [VideoComponentFactory.CallContentVideoModerationBlur].
 *
 * @param call The call that contains all the participants state and tracks.
 */
public data class CallContentVideoModerationBlurParams(
    val call: Call,
)

/**
 * Parameters for [VideoComponentFactory.CallContentVideoModerationWarning].
 *
 * @param call The call that contains all the participants state and tracks.
 * @param message The moderation warning message to display.
 */
public data class CallContentVideoModerationWarningParams(
    val call: Call,
    val message: String? = null,
)

/**
 * Parameters for [VideoComponentFactory.ControlActions].
 *
 * @param call The call that contains all the participants state and tracks.
 * @param modifier Modifier for styling.
 * @param onCallAction Handler when the user triggers a Call Control Action. When `null`, the
 * default call action handler is used.
 * @param actions A list of composable call actions arranged in the layout. When `null`, the
 * default call control actions are used.
 */
public data class ControlActionsParams(
    val call: Call,
    val modifier: Modifier = Modifier,
    val onCallAction: ((CallAction) -> Unit)? = null,
    val actions: List<(@Composable () -> Unit)>? = null,
)

/**
 * Parameters for [VideoComponentFactory.ParticipantVideo].
 *
 * @param call The call that contains all the participants state and tracks.
 * @param participant Participant to render.
 * @param modifier Modifier for styling.
 * @param style Defined properties for styling a single video call track.
 * @param scalingType The scaling type for the video renderer.
 * @param mirrorMode Controls horizontal mirroring of the video stream.
 */
public data class ParticipantVideoParams(
    val call: Call,
    val participant: ParticipantState,
    val modifier: Modifier = Modifier,
    val style: VideoRendererStyle = RegularVideoRendererStyle(),
    val scalingType: VideoScalingType = VideoScalingType.SCALE_ASPECT_FILL,
    val mirrorMode: MirrorMode = MirrorMode.AUTO,
)

/**
 * Parameters for [VideoComponentFactory.ParticipantVideoLabelContent].
 *
 * @param call The call that contains all the participants state and tracks.
 * @param participant Participant to render the label for.
 * @param labelPosition The position of the label inside the participant video container.
 */
public data class ParticipantVideoLabelContentParams(
    val call: Call,
    val participant: ParticipantState,
    val labelPosition: Alignment = Alignment.BottomStart,
)

/**
 * Parameters for [VideoComponentFactory.ParticipantLabelSoundIndicatorContent].
 *
 * @param isSpeaking Whether the sound indicator animates as speaking.
 * @param isAudioEnabled Whether the participant's microphone is enabled.
 * @param audioLevel The audio level of the participant, between 0 and 1.
 * @param modifier Modifier for styling. It carries the position and padding of the indicator
 * inside the label, which differ between the label variants.
 */
public data class ParticipantLabelSoundIndicatorContentParams(
    val isSpeaking: Boolean,
    val isAudioEnabled: Boolean,
    val audioLevel: Float = 0f,
    val modifier: Modifier = Modifier,
)

/**
 * Parameters for [VideoComponentFactory.ParticipantVideoConnectionIndicatorContent].
 *
 * @param networkQuality The network quality of the participant connection.
 */
public data class ParticipantVideoConnectionIndicatorContentParams(
    val networkQuality: NetworkQuality,
)

/**
 * Parameters for [VideoComponentFactory.UserAvatar].
 *
 * @param userImage The URL of the user image, or null to show the initials of [userName].
 * @param userName The name whose initials are the fallback, or null for no fallback.
 * @param modifier The modifier applied to the avatar. The avatar fills the size the modifier gives it.
 * @param isShowingOnlineIndicator Whether the online indicator is drawn over the avatar.
 */
public data class UserAvatarParams(
    val userImage: String?,
    val userName: String?,
    val modifier: Modifier = Modifier,
    val isShowingOnlineIndicator: Boolean = false,
)

/**
 * Parameters for [VideoComponentFactory.ParticipantVideoFallbackContent].
 *
 * @param call The call that contains all the participants state and tracks.
 * @param participant Participant whose fallback is rendered.
 */
public data class ParticipantVideoFallbackContentParams(
    val call: Call,
    val participant: ParticipantState,
)

/**
 * Parameters for [VideoComponentFactory.ParticipantVideoReactionContent].
 *
 * @param participant Participant whose reaction is rendered.
 * @param style Defined properties for styling a single video call track.
 */
public data class ParticipantVideoReactionContentParams(
    val participant: ParticipantState,
    val style: VideoRendererStyle = RegularVideoRendererStyle(),
)

/**
 * Parameters for [VideoComponentFactory.ParticipantVideoActionsContent].
 *
 * @param actions The list of actions that can be applied to the participant.
 * @param call The call that contains all the participants state and tracks.
 * @param participant Participant the actions apply to.
 */
public data class ParticipantVideoActionsContentParams(
    val actions: List<ParticipantAction>,
    val call: Call,
    val participant: ParticipantState,
)

/**
 * Parameters for [VideoComponentFactory.ScreenSharingFallbackContent].
 *
 * @param session The screen sharing session the fallback is rendered for.
 */
public data class ScreenSharingFallbackContentParams(
    val session: ScreenSharingSession,
)

/**
 * Parameters for [VideoComponentFactory.CallLobbyOnRenderedContent].
 *
 * @param call The call that contains all the participants state and tracks.
 * @param video The local participant video to render on the preview.
 * @param onRendered An interface that is invoked when the video is rendered.
 */
public data class CallLobbyOnRenderedContentParams(
    val call: Call,
    val video: ParticipantState.Video,
    val onRendered: (View) -> Unit = {},
)

/**
 * Parameters for [VideoComponentFactory.CallLobbyOnDisabledContent].
 *
 * @param user A user to display their name and avatar image on the preview.
 */
public data class CallLobbyOnDisabledContentParams(
    val user: User,
)

/**
 * Parameters for [VideoComponentFactory.CallLobbyParticipantLabelContent].
 *
 * @param user A user to display their name on the label.
 * @param isMicrophoneEnabled Whether the microphone is enabled or not.
 * @param labelPosition The position of the label inside the preview container.
 */
public data class CallLobbyParticipantLabelContentParams(
    val user: User,
    val isMicrophoneEnabled: Boolean,
    val labelPosition: Alignment = Alignment.BottomStart,
)

/**
 * Parameters for [VideoComponentFactory.CallLobbyControlsContent].
 *
 * @param call The call that contains all the participants state and tracks.
 * @param isCameraEnabled Whether the camera is enabled or not.
 * @param isMicrophoneEnabled Whether the microphone is enabled or not.
 * @param modifier Modifier for styling.
 * @param onCallAction Handler when the user triggers a Call Control Action. When `null`, the
 * default call action handler is used.
 */
public data class CallLobbyControlsContentParams(
    val call: Call,
    val isCameraEnabled: Boolean,
    val isMicrophoneEnabled: Boolean,
    val modifier: Modifier = Modifier,
    val onCallAction: ((CallAction) -> Unit)? = null,
)

/**
 * Parameters for [VideoComponentFactory.IncomingCallContent].
 *
 * @param call The call that contains all the participants state and tracks.
 * @param modifier Modifier for styling.
 * @param isVideoType Represents whether the call type is a video or an audio.
 * @param isShowingHeader Whether or not the app bar is shown.
 * @param backgroundContent Content shown for the call background. When `null`, the default
 * background is used.
 * @param headerContent Content shown for the call header. When `null`,
 * [VideoComponentFactory.IncomingCallHeaderContent] is used.
 * @param detailsContent Content shown for call details, such as call participant information.
 * When `null`, [VideoComponentFactory.IncomingCallDetailsContent] is used.
 * @param controlsContent Content shown for controlling the call, such as accepting or declining.
 * When `null`, [VideoComponentFactory.IncomingCallControlsContent] is used.
 * @param onBackPressed Handler when the user taps on the back button.
 * @param onCallAction Handler used when the user interacts with Call UI.
 */
public data class IncomingCallContentParams(
    val call: Call,
    val modifier: Modifier = Modifier,
    val isVideoType: Boolean = true,
    val isShowingHeader: Boolean = true,
    val backgroundContent: (@Composable BoxScope.() -> Unit)? = null,
    val headerContent: (@Composable ColumnScope.() -> Unit)? = null,
    val detailsContent: (
        @Composable ColumnScope.(
            participants: List<MemberState>,
            topPadding: Dp,
        ) -> Unit
    )? = null,
    val controlsContent: (@Composable BoxScope.() -> Unit)? = null,
    val onBackPressed: () -> Unit = {},
    val onCallAction: (CallAction) -> Unit = {},
)

/**
 * Parameters for [VideoComponentFactory.IncomingCallHeaderContent].
 *
 * @param call The call that contains all the participants state and tracks.
 */
public data class IncomingCallHeaderContentParams(
    val call: Call,
)

/**
 * Parameters for [VideoComponentFactory.IncomingCallDetailsContent].
 *
 * @param participants List of call members shown in the details.
 * @param topPadding Suggested padding above the details content, computed from the participant
 * count. The default implementation does not apply it; it is provided for custom implementations.
 * @param isVideoType Represents whether the call type is a video or an audio.
 */
public data class IncomingCallDetailsContentParams(
    val participants: List<MemberState>,
    val topPadding: Dp,
    val isVideoType: Boolean = true,
)

/**
 * Parameters for [VideoComponentFactory.IncomingCallControlsContent].
 *
 * @param isCameraEnabled Whether the camera is enabled or not.
 * @param isMicrophoneEnabled Whether the microphone is enabled or not. When `null`, the
 * microphone toggle is not shown.
 * @param isVideoCall Represents whether the call is a video call or not.
 * @param onCallAction Handler used when the user interacts with Call UI.
 */
public data class IncomingCallControlsContentParams(
    val isCameraEnabled: Boolean,
    val isMicrophoneEnabled: Boolean? = null,
    val isVideoCall: Boolean = true,
    val onCallAction: (CallAction) -> Unit = {},
)

/**
 * Parameters for [VideoComponentFactory.OutgoingCallContent].
 *
 * @param call The call that contains all the participants state and tracks.
 * @param modifier Modifier for styling.
 * @param isVideoType Represents whether the call type is a video or an audio.
 * @param isShowingHeader Whether or not the app bar is shown.
 * @param backgroundContent Content shown for the call background. When `null`, the default
 * background is used.
 * @param headerContent Content shown for the call header. When `null`,
 * [VideoComponentFactory.OutgoingCallHeaderContent] is used.
 * @param detailsContent Content shown for call details, such as call participant information.
 * When `null`, [VideoComponentFactory.OutgoingCallDetailsContent] is used.
 * @param controlsContent Content shown for controlling the call, such as cancelling the call.
 * When `null`, [VideoComponentFactory.OutgoingCallControlsContent] is used.
 * @param onBackPressed Handler when the user taps on the back button.
 * @param onCallAction Handler used when the user interacts with Call UI.
 */
public data class OutgoingCallContentParams(
    val call: Call,
    val modifier: Modifier = Modifier,
    val isVideoType: Boolean = true,
    val isShowingHeader: Boolean = true,
    val backgroundContent: (@Composable BoxScope.() -> Unit)? = null,
    val headerContent: (@Composable ColumnScope.() -> Unit)? = null,
    val detailsContent: (
        @Composable ColumnScope.(
            participants: List<MemberState>,
            topPadding: Dp,
        ) -> Unit
    )? = null,
    val controlsContent: (@Composable BoxScope.() -> Unit)? = null,
    val onBackPressed: () -> Unit = {},
    val onCallAction: (CallAction) -> Unit = {},
)

/**
 * Parameters for [VideoComponentFactory.OutgoingCallHeaderContent].
 *
 * @param call The call that contains all the participants state and tracks.
 */
public data class OutgoingCallHeaderContentParams(
    val call: Call,
)

/**
 * Parameters for [VideoComponentFactory.OutgoingCallDetailsContent].
 *
 * @param participants List of call members shown in the details.
 * @param topPadding Suggested padding above the details content, computed from the participant
 * count and call type. The default implementation does not apply it; it is provided for custom
 * implementations.
 * @param isVideoType Represents whether the call type is a video or an audio.
 */
public data class OutgoingCallDetailsContentParams(
    val participants: List<MemberState>,
    val topPadding: Dp,
    val isVideoType: Boolean = true,
)

/**
 * Parameters for [VideoComponentFactory.OutgoingCallControlsContent].
 *
 * @param isCameraEnabled Whether the camera is enabled or not.
 * @param isMicrophoneEnabled Whether the microphone is enabled or not.
 * @param isVideoCall Represents whether the call is a video call or not.
 * @param onCallAction Handler used when the user interacts with Call UI.
 */
public data class OutgoingCallControlsContentParams(
    val isCameraEnabled: Boolean,
    val isMicrophoneEnabled: Boolean,
    val isVideoCall: Boolean = true,
    val onCallAction: (CallAction) -> Unit = {},
)

/**
 * Parameters for [VideoComponentFactory.AudioOnlyCallHeaderContent].
 *
 * @param call The call that contains all the participants state and tracks.
 */
public data class AudioOnlyCallHeaderContentParams(
    val call: Call,
)

/**
 * Parameters for [VideoComponentFactory.AudioOnlyCallDetailsContent].
 *
 * @param remoteParticipants List of the remote participants shown in the details.
 * @param topPadding Suggested padding above the details content. The default implementation does
 * not apply it; it is provided for custom implementations.
 * @param duration The current duration of the call, formatted as text.
 */
public data class AudioOnlyCallDetailsContentParams(
    val remoteParticipants: List<ParticipantState>,
    val topPadding: Dp,
    val duration: String = "",
)

/**
 * Parameters for [VideoComponentFactory.AudioOnlyCallControlsContent].
 *
 * @param isMicrophoneEnabled Whether the microphone is enabled or not.
 * @param onCallAction Handler used when the user interacts with Call UI.
 */
public data class AudioOnlyCallControlsContentParams(
    val isMicrophoneEnabled: Boolean,
    val onCallAction: (CallAction) -> Unit = {},
)
