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

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.ui.components.avatar.UserAvatarBackground
import io.getstream.video.android.compose.ui.components.call.DefaultCallAppBarCenterContent
import io.getstream.video.android.compose.ui.components.call.DefaultCallAppBarLeadingContent
import io.getstream.video.android.compose.ui.components.call.activecall.DefaultPictureInPictureContent
import io.getstream.video.android.compose.ui.components.call.controls.actions.DefaultOnCallActionHandler
import io.getstream.video.android.compose.ui.components.call.controls.actions.LeaveCallAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.buildDefaultCallControlActions
import io.getstream.video.android.compose.ui.components.call.lobby.DefaultParticipantLabel
import io.getstream.video.android.compose.ui.components.call.lobby.OnDisabledContent
import io.getstream.video.android.compose.ui.components.call.lobby.OnRenderedContent
import io.getstream.video.android.compose.ui.components.call.lobby.buildDefaultLobbyControlActions
import io.getstream.video.android.compose.ui.components.call.moderation.DefaultModerationWarningUiContainer
import io.getstream.video.android.compose.ui.components.call.moderation.ModerationWarningAnimationConfig
import io.getstream.video.android.compose.ui.components.call.pinning.ParticipantActions
import io.getstream.video.android.compose.ui.components.call.renderer.DefaultReaction
import io.getstream.video.android.compose.ui.components.call.renderer.ParticipantLabel
import io.getstream.video.android.compose.ui.components.call.renderer.ParticipantsLayout
import io.getstream.video.android.compose.ui.components.call.ringing.incomingcall.IncomingCallControls
import io.getstream.video.android.compose.ui.components.call.ringing.incomingcall.IncomingCallDetails
import io.getstream.video.android.compose.ui.components.call.ringing.outgoingcall.OutgoingCallControls
import io.getstream.video.android.compose.ui.components.call.ringing.outgoingcall.OutgoingCallDetails
import io.getstream.video.android.compose.ui.components.indicator.NetworkQualityIndicator
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.call.state.LeaveCall
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfig
import io.getstream.video.android.ui.common.R

/**
 * Default implementation of [VideoComponentFactory]. A singleton, so providing it as a default
 * never invalidates the composition locals that carry the factory.
 */
internal object DefaultVideoComponentFactory : VideoComponentFactory

/**
 * A factory for creating the components used by the Video Compose SDK.
 *
 * Each method has a default implementation that renders the built-in component, so you only need
 * to override the components you want to customize. The factory is provided through
 * [VideoTheme], and the built-in components read it via [VideoTheme.componentFactory].
 *
 * ```
 * VideoTheme(
 *     componentFactory = object : VideoComponentFactory {
 *         @Composable
 *         override fun RowScope.CallAppBarTrailingContent(
 *             params: CallAppBarTrailingContentParams,
 *         ) {
 *             // Custom trailing content
 *         }
 *     },
 * ) {
 *     CallContent(call = call)
 * }
 * ```
 *
 * Use [CompoundComponentFactory] to layer overrides on top of the current factory for a subtree.
 */
@Suppress("TooManyFunctions", "LargeClass")
public interface VideoComponentFactory {

    /**
     * The app bar shown in calls. The default implementation renders
     * [io.getstream.video.android.compose.ui.components.call.CallAppBar].
     *
     * @param params Parameters for this component.
     */
    @Composable
    public fun CallAppBar(params: CallAppBarParams) {
        io.getstream.video.android.compose.ui.components.call.CallAppBar(
            call = params.call,
            modifier = params.modifier,
            onBackPressed = params.onBackPressed,
            onCallAction = params.onCallAction,
            title = params.title
                ?: stringResource(id = R.string.stream_video_default_app_bar_title),
        )
    }

    /**
     * The leading content of the call app bar. Usually the back button.
     *
     * @param params Parameters for this component.
     */
    @Composable
    public fun RowScope.CallAppBarLeadingContent(params: CallAppBarLeadingContentParams) {
        DefaultCallAppBarLeadingContent(onBackButtonClicked = params.onBackPressed)
    }

    /**
     * The center content of the call app bar. Usually the call duration or title, with the
     * recording and reconnecting states.
     *
     * @param params Parameters for this component.
     */
    @Composable
    public fun RowScope.CallAppBarCenterContent(params: CallAppBarCenterContentParams) {
        DefaultCallAppBarCenterContent(
            call = params.call,
            title = params.title
                ?: stringResource(id = R.string.stream_video_default_app_bar_title),
        )
    }

    /**
     * The trailing content of the call app bar. Usually the leave call button.
     *
     * @param params Parameters for this component.
     */
    @Composable
    public fun RowScope.CallAppBarTrailingContent(params: CallAppBarTrailingContentParams) {
        LeaveCallAction {
            params.onCallAction(LeaveCall)
        }
    }

    /**
     * The video content of the active call. The default implementation renders
     * [ParticipantsLayout] with all the participants' videos.
     *
     * @param params Parameters for this component.
     */
    @Composable
    public fun RowScope.CallContentVideoContent(params: CallContentVideoContentParams) {
        ParticipantsLayout(
            layoutType = params.layoutType,
            call = params.call,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(bottom = VideoTheme.dimens.spacingXXs),
            style = params.style,
            videoRenderer = params.videoRenderer,
            floatingVideoRenderer = params.floatingVideoRenderer,
        )
    }

    /**
     * Content drawn over the video content of the active call, excluding the controls.
     * Empty by default.
     *
     * @param params Parameters for this component.
     */
    @Composable
    public fun CallContentVideoOverlayContent(params: CallContentVideoOverlayContentParams) {
    }

    /**
     * Content shown when the user enters Picture in Picture mode.
     *
     * @param params Parameters for this component.
     */
    @Composable
    public fun CallContentPictureInPictureContent(
        params: CallContentPictureInPictureContentParams,
    ) {
        DefaultPictureInPictureContent(call = params.call)
    }

    /**
     * Content that renders closed captions in the active call. Empty by default.
     *
     * @param params Parameters for this component.
     */
    @Composable
    public fun CallContentClosedCaptions(params: CallContentClosedCaptionsParams) {
    }

    /**
     * Content rendered on top of the video when it is blurred by moderation. Empty by default.
     *
     * @param params Parameters for this component.
     */
    @Composable
    public fun CallContentVideoModerationBlur(params: CallContentVideoModerationBlurParams) {
    }

    /**
     * Content that renders the video moderation warning.
     *
     * @param params Parameters for this component.
     */
    @Composable
    public fun CallContentVideoModerationWarning(
        params: CallContentVideoModerationWarningParams,
    ) {
        val callServiceConfig = StreamVideo.instanceOrNull()
            ?.state
            ?.callConfigRegistry
            ?.get(params.call.type)
            ?: CallServiceConfig()
        val displayTime = callServiceConfig.moderationConfig.moderationWarningConfig.displayTime
        DefaultModerationWarningUiContainer(
            call = params.call,
            message = params.message,
            moderationWarningAnimationConfig = ModerationWarningAnimationConfig(displayTime),
        )
    }

    /**
     * The set of controls the user can use to change their audio and video device state, browse
     * other types of settings, or leave the call. The default implementation renders
     * [io.getstream.video.android.compose.ui.components.call.controls.ControlActions].
     *
     * @param params Parameters for this component.
     */
    @Composable
    public fun ControlActions(params: ControlActionsParams) {
        val onCallAction = params.onCallAction
            ?: { DefaultOnCallActionHandler.onCallAction(params.call, it) }
        io.getstream.video.android.compose.ui.components.call.controls.ControlActions(
            call = params.call,
            modifier = params.modifier,
            onCallAction = onCallAction,
            actions = params.actions ?: buildDefaultCallControlActions(
                call = params.call,
                onCallAction = onCallAction,
            ),
        )
    }

    /**
     * A single participant video with a label, connection indicator, reactions and actions.
     * The default implementation renders
     * [io.getstream.video.android.compose.ui.components.call.renderer.ParticipantVideo].
     *
     * @param params Parameters for this component.
     */
    @Composable
    public fun ParticipantVideo(params: ParticipantVideoParams) {
        io.getstream.video.android.compose.ui.components.call.renderer.ParticipantVideo(
            call = params.call,
            participant = params.participant,
            modifier = params.modifier,
            style = params.style,
            scalingType = params.scalingType,
            mirrorMode = params.mirrorMode,
        )
    }

    /**
     * The label of a participant video, showing the participant's name and device states.
     *
     * @param params Parameters for this component.
     */
    @Composable
    public fun BoxScope.ParticipantVideoLabelContent(
        params: ParticipantVideoLabelContentParams,
    ) {
        ParticipantLabel(
            call = params.call,
            participant = params.participant,
            labelPosition = params.labelPosition,
        )
    }

    /**
     * The indicator that shows the connection quality of a participant.
     *
     * @param params Parameters for this component.
     */
    @Composable
    public fun BoxScope.ParticipantVideoConnectionIndicatorContent(
        params: ParticipantVideoConnectionIndicatorContentParams,
    ) {
        NetworkQualityIndicator(
            networkQuality = params.networkQuality,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .height(VideoTheme.dimens.componentHeightM)
                .testTag("Stream_ParticipantNetworkQualityIndicator"),
        )
    }

    /**
     * Content shown when the participant video track fails to load or is not available.
     * The default implementation renders the participant's avatar.
     *
     * @param params Parameters for this component.
     */
    @Composable
    public fun ParticipantVideoFallbackContent(params: ParticipantVideoFallbackContentParams) {
        val userName by params.participant.userNameOrId.collectAsStateWithLifecycle()
        val userImage by params.participant.image.collectAsStateWithLifecycle()
        UserAvatarBackground(userImage = userImage, userName = userName)
    }

    /**
     * Content shown for a participant's reaction.
     *
     * @param params Parameters for this component.
     */
    @Composable
    public fun BoxScope.ParticipantVideoReactionContent(
        params: ParticipantVideoReactionContentParams,
    ) {
        DefaultReaction(
            participant = params.participant,
            style = params.style,
        )
    }

    /**
     * The action picker with call actions related to the selected participant.
     *
     * @param params Parameters for this component.
     */
    @Composable
    public fun BoxScope.ParticipantVideoActionsContent(
        params: ParticipantVideoActionsContentParams,
    ) {
        ParticipantActions(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .testTag("Stream_ParticipantActionsIcon"),
            actions = params.actions,
            call = params.call,
            participant = params.participant,
        )
    }

    /**
     * The video preview of the call lobby, rendering the local video track before joining a call.
     *
     * @param params Parameters for this component.
     */
    @Composable
    public fun CallLobbyOnRenderedContent(params: CallLobbyOnRenderedContentParams) {
        OnRenderedContent(
            call = params.call,
            video = params.video,
            onRendered = params.onRendered,
        )
    }

    /**
     * Content shown in the call lobby when the local camera is disabled. It displays the user
     * avatar by default.
     *
     * @param params Parameters for this component.
     */
    @Composable
    public fun CallLobbyOnDisabledContent(params: CallLobbyOnDisabledContentParams) {
        OnDisabledContent(user = params.user)
    }

    /**
     * The participant label overlaid on the call lobby preview, showing the user's name and
     * microphone state.
     *
     * @param params Parameters for this component.
     */
    @Composable
    public fun BoxScope.CallLobbyParticipantLabelContent(
        params: CallLobbyParticipantLabelContentParams,
    ) {
        DefaultParticipantLabel(
            user = params.user,
            isMicrophoneEnabled = params.isMicrophoneEnabled,
            labelPosition = params.labelPosition,
        )
    }

    /**
     * The set of controls the user can use in the call lobby to change their audio and video
     * device state before joining a call. Intentionally independent from [ControlActions], so
     * overriding the in-call controls does not affect the lobby.
     *
     * @param params Parameters for this component.
     */
    @Composable
    public fun CallLobbyControlsContent(params: CallLobbyControlsContentParams) {
        val onCallAction = params.onCallAction
            ?: { DefaultOnCallActionHandler.onCallAction(params.call, it) }
        io.getstream.video.android.compose.ui.components.call.controls.ControlActions(
            call = params.call,
            modifier = params.modifier,
            onCallAction = onCallAction,
            actions = buildDefaultLobbyControlActions(
                call = params.call,
                onCallAction = onCallAction,
                isCameraEnabled = params.isCameraEnabled,
                isMicrophoneEnabled = params.isMicrophoneEnabled,
            ),
        )
    }

    /**
     * The incoming call screen, shown when the user receives a call from other people.
     * The default implementation renders
     * [io.getstream.video.android.compose.ui.components.call.ringing.incomingcall.IncomingCallContent].
     *
     * @param params Parameters for this component.
     */
    @Composable
    public fun IncomingCallContent(params: IncomingCallContentParams) {
        io.getstream.video.android.compose.ui.components.call.ringing.incomingcall.IncomingCallContent(
            call = params.call,
            modifier = params.modifier,
            isVideoType = params.isVideoType,
            isShowingHeader = params.isShowingHeader,
            backgroundContent = params.backgroundContent,
            headerContent = params.headerContent,
            detailsContent = params.detailsContent,
            controlsContent = params.controlsContent,
            onBackPressed = params.onBackPressed,
            onCallAction = params.onCallAction,
        )
    }

    /**
     * The header of the incoming call screen. Empty by default.
     *
     * @param params Parameters for this component.
     */
    @Composable
    public fun ColumnScope.IncomingCallHeaderContent(params: IncomingCallHeaderContentParams) {
    }

    /**
     * The details of the incoming call screen, such as call participant information.
     *
     * @param params Parameters for this component.
     */
    @Composable
    public fun ColumnScope.IncomingCallDetailsContent(params: IncomingCallDetailsContentParams) {
        IncomingCallDetails(
            modifier = Modifier
                .align(Alignment.CenterHorizontally),
            isVideoType = params.isVideoType,
            participants = params.participants,
        )
    }

    /**
     * The controls of the incoming call screen, such as accepting or declining the call.
     *
     * @param params Parameters for this component.
     */
    @Composable
    public fun BoxScope.IncomingCallControlsContent(
        params: IncomingCallControlsContentParams,
    ) {
        IncomingCallControls(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = VideoTheme.dimens.genericXxl),
            isVideoCall = params.isVideoCall,
            isMicrophoneEnabled = params.isMicrophoneEnabled,
            isCameraEnabled = params.isCameraEnabled,
            onCallAction = params.onCallAction,
        )
    }

    /**
     * The outgoing call screen, shown when the user is calling other people.
     * The default implementation renders
     * [io.getstream.video.android.compose.ui.components.call.ringing.outgoingcall.OutgoingCallContent].
     *
     * @param params Parameters for this component.
     */
    @Composable
    public fun OutgoingCallContent(params: OutgoingCallContentParams) {
        io.getstream.video.android.compose.ui.components.call.ringing.outgoingcall.OutgoingCallContent(
            call = params.call,
            modifier = params.modifier,
            isVideoType = params.isVideoType,
            isShowingHeader = params.isShowingHeader,
            backgroundContent = params.backgroundContent,
            headerContent = params.headerContent,
            detailsContent = params.detailsContent,
            controlsContent = params.controlsContent,
            onBackPressed = params.onBackPressed,
            onCallAction = params.onCallAction,
        )
    }

    /**
     * The header of the outgoing call screen. Empty by default.
     *
     * @param params Parameters for this component.
     */
    @Composable
    public fun ColumnScope.OutgoingCallHeaderContent(params: OutgoingCallHeaderContentParams) {
    }

    /**
     * The details of the outgoing call screen, such as call participant information.
     *
     * @param params Parameters for this component.
     */
    @Composable
    public fun ColumnScope.OutgoingCallDetailsContent(params: OutgoingCallDetailsContentParams) {
        OutgoingCallDetails(
            modifier = Modifier
                .align(Alignment.CenterHorizontally),
            participants = params.participants,
            isVideoType = params.isVideoType,
        )
    }

    /**
     * The controls of the outgoing call screen, such as cancelling the call.
     *
     * @param params Parameters for this component.
     */
    @Composable
    public fun BoxScope.OutgoingCallControlsContent(
        params: OutgoingCallControlsContentParams,
    ) {
        OutgoingCallControls(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = VideoTheme.dimens.genericXxl),
            isVideoCall = params.isVideoCall,
            isCameraEnabled = params.isCameraEnabled,
            isMicrophoneEnabled = params.isMicrophoneEnabled,
            onCallAction = params.onCallAction,
        )
    }
}
