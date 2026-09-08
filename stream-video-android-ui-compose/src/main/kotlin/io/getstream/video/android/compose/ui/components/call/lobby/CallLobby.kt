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

package io.getstream.video.android.compose.ui.components.call.lobby

import android.content.res.Configuration
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.lifecycle.MediaPiPLifecycle
import io.getstream.video.android.compose.permission.VideoPermissionsState
import io.getstream.video.android.compose.permission.rememberCallPermissionsState
import io.getstream.video.android.compose.theme.CallLobbyControlsContentParams
import io.getstream.video.android.compose.theme.CallLobbyJoinContentParams
import io.getstream.video.android.compose.theme.CallLobbyOnDisabledContentParams
import io.getstream.video.android.compose.theme.CallLobbyOnRenderedContentParams
import io.getstream.video.android.compose.theme.CallLobbyParticipantLabelContentParams
import io.getstream.video.android.compose.theme.ParticipantLabelSoundIndicatorContentParams
import io.getstream.video.android.compose.theme.UserAvatarParams
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.theme.design.StreamTokens
import io.getstream.video.android.compose.ui.components.call.controls.actions.DefaultOnCallActionHandler
import io.getstream.video.android.compose.ui.components.call.renderer.ParticipantLabel
import io.getstream.video.android.compose.ui.components.video.DefaultMediaTrackFallbackContent
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.compose.ui.components.video.config.videoRenderConfig
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.ToggleCamera
import io.getstream.video.android.core.call.state.ToggleMicrophone
import io.getstream.video.android.core.model.VideoTrack
import io.getstream.video.android.core.pip.PictureInPictureConfiguration
import io.getstream.video.android.model.User
import io.getstream.video.android.ui.common.R

/** The aspect ratio of the lobby preview tile, taken from the design system tile. */
private const val PREVIEW_ASPECT_RATIO = 370f / 264f

/**
 * Represents the UI in a preview call that renders a local video track to pre-display a video
 * before joining a call. It stacks the video preview, the device controls and, when [onJoinCall] is
 * set, the join action.
 *
 * @param modifier Modifier for styling.
 * @param call The call includes states and will be rendered with participants.
 * @param user A user to display their name and avatar image on the preview.
 * @param isCameraEnabled Whether the camera is enabled. Selects the video or the avatar preview.
 * @param isMicrophoneEnabled Whether the microphone is enabled. Reflected on the label and the controls.
 * @param video A participant video to render on the preview renderer.
 * @param permissions Android permissions that should be required to render a video call properly. A
 * denied camera or microphone permission marks the matching control as unavailable, and tapping that
 * control requests the permission again.
 * @param onRendered An interface that will be invoked when the video is rendered.
 * @param onRenderedContent A video renderer, which renders a local video track before joining a call.
 * @param onDisabledContent Content is shown that a local camera is disabled. It displays user avatar by default.
 * @param videoPreviewModifier Modifier applied to the [Box] that wraps the local video preview. Defaults
 * to the full width with the design system tile aspect ratio in portrait, a fixed height in landscape,
 * and a 20dp rounded corner clip. The preview border (accent while the camera is on, subtle while it is
 * off) is drawn by the lobby with the same shape. Override to provide custom size, shape, padding, or
 * background, useful when the preview needs to match a host layout instead of the SDK's default sizing.
 * @param participantLabelContent Slot for the participant label overlaid on the preview. Defaults to a
 * label showing the user's name and media state at [Alignment.BottomStart]. Pass `{}` to hide the
 * label entirely, or override to provide custom positioning and content (use [BoxScope.align] inside).
 * @param onCallAction Handler when the user triggers a Call Control Action.
 * @param lobbyControlsContent Content is shown that allows users to trigger different actions to control a preview call.
 * @param onJoinCall Handler when the user confirms joining the call. When null, the join action is not
 * rendered and the host provides its own.
 * @param joinCallContent The join action, rendered below the controls only when [onJoinCall] is set.
 * Defaults to the full width "Join Call" button of the component factory.
 */
@Composable
public fun CallLobby(
    modifier: Modifier = Modifier,
    call: Call,
    user: User = StreamVideo.instance().user,
    isCameraEnabled: Boolean = if (LocalInspectionMode.current) {
        true
    } else {
        call.camera.isEnabled.value
    },
    isMicrophoneEnabled: Boolean = if (LocalInspectionMode.current) {
        true
    } else {
        call.microphone.isEnabled.value
    },
    video: ParticipantState.Video = ParticipantState.Video(
        sessionId = call.sessionId,
        track = VideoTrack(
            streamId = call.sessionId,
            video = if (LocalInspectionMode.current) {
                io.getstream.webrtc.VideoTrack(1000L)
            } else {
                call.camera.mediaManager.videoTrack
            },
        ),
        enabled = isCameraEnabled,
        paused = false,
    ),
    permissions: VideoPermissionsState = rememberCallPermissionsState(call = call),
    onRendered: (View) -> Unit = {},
    onRenderedContent: @Composable (video: ParticipantState.Video) -> Unit = {
        DefaultOnRenderedSlot(call, it, onRendered)
    },
    onDisabledContent: @Composable () -> Unit = {
        DefaultOnDisabledSlot(user)
    },
    videoPreviewModifier: Modifier = defaultVideoPreviewModifier(),
    participantLabelContent: @Composable BoxScope.() -> Unit = {
        DefaultParticipantLabelSlot(
            call,
            user,
            isMicrophoneEnabled,
            isCameraEnabled,
            Alignment.BottomStart,
        )
    },
    onCallAction: (CallAction) -> Unit = {
        DefaultOnCallActionHandler.onCallAction(call, it)
    },
    lobbyControlsContent: @Composable (modifier: Modifier, call: Call) -> Unit = { modifier, call ->
        DefaultLobbyControlsSlot(
            modifier = modifier,
            call = call,
            isCameraEnabled = isCameraEnabled,
            isMicrophoneEnabled = isMicrophoneEnabled,
            permissions = permissions,
            onCallAction = onCallAction,
        )
    },
    onJoinCall: (() -> Unit)? = null,
    joinCallContent: @Composable (modifier: Modifier, call: Call) -> Unit = { modifier, call ->
        onJoinCall?.let { DefaultJoinCallSlot(modifier, call, it) }
    },
) {
    DefaultPermissionHandler(videoPermission = permissions)

    MediaPiPLifecycle(call = call, PictureInPictureConfiguration(false, false))

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(StreamTokens.spacing2xl),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(StreamTokens.spacingSm)) {
            Box(
                modifier = videoPreviewModifier
                    .align(Alignment.CenterHorizontally)
                    .previewBorder(isCameraEnabled),
            ) {
                if (isCameraEnabled) {
                    onRenderedContent.invoke(video)
                } else {
                    onDisabledContent.invoke()
                }

                participantLabelContent()
            }

            lobbyControlsContent.invoke(Modifier.align(Alignment.CenterHorizontally), call)
        }

        if (onJoinCall != null) {
            joinCallContent.invoke(Modifier.fillMaxWidth(), call)
        }
    }
}

@Deprecated(
    message = "Use CallLobby with the participantLabelContent slot for full control over the lobby " +
        "label. Pass `{}` to hide it, or override to customize content and position.",
    replaceWith = ReplaceWith(
        "CallLobby(modifier = modifier, call = call, user = user, " +
            "isCameraEnabled = isCameraEnabled, isMicrophoneEnabled = isMicrophoneEnabled, " +
            "video = video, permissions = permissions, onRendered = onRendered, " +
            "onRenderedContent = onRenderedContent, onDisabledContent = onDisabledContent, " +
            "onCallAction = onCallAction, lobbyControlsContent = lobbyControlsContent)",
    ),
)
@Composable
public fun CallLobby(
    modifier: Modifier = Modifier,
    call: Call,
    user: User = StreamVideo.instance().user,
    labelPosition: Alignment,
    isCameraEnabled: Boolean = if (LocalInspectionMode.current) {
        true
    } else {
        call.camera.isEnabled.value
    },
    isMicrophoneEnabled: Boolean = if (LocalInspectionMode.current) {
        true
    } else {
        call.microphone.isEnabled.value
    },
    video: ParticipantState.Video = ParticipantState.Video(
        sessionId = call.sessionId,
        track = VideoTrack(
            streamId = call.sessionId,
            video = if (LocalInspectionMode.current) {
                io.getstream.webrtc.VideoTrack(1000L)
            } else {
                call.camera.mediaManager.videoTrack
            },
        ),
        enabled = isCameraEnabled,
        paused = false,
    ),
    permissions: VideoPermissionsState = rememberCallPermissionsState(call = call),
    onRendered: (View) -> Unit = {},
    onRenderedContent: @Composable (video: ParticipantState.Video) -> Unit = {
        DefaultOnRenderedSlot(call, it, onRendered)
    },
    onDisabledContent: @Composable () -> Unit = {
        DefaultOnDisabledSlot(user)
    },
    onCallAction: (CallAction) -> Unit = {
        DefaultOnCallActionHandler.onCallAction(call, it)
    },
    lobbyControlsContent: @Composable (modifier: Modifier, call: Call) -> Unit = { modifier, call ->
        DefaultLobbyControlsSlot(
            modifier = modifier,
            call = call,
            isCameraEnabled = isCameraEnabled,
            isMicrophoneEnabled = isMicrophoneEnabled,
            permissions = permissions,
            onCallAction = onCallAction,
        )
    },
) {
    CallLobby(
        modifier = modifier,
        call = call,
        user = user,
        isCameraEnabled = isCameraEnabled,
        isMicrophoneEnabled = isMicrophoneEnabled,
        video = video,
        permissions = permissions,
        onRendered = onRendered,
        onRenderedContent = onRenderedContent,
        onDisabledContent = onDisabledContent,
        participantLabelContent = {
            DefaultParticipantLabelSlot(
                call,
                user,
                isMicrophoneEnabled,
                isCameraEnabled,
                labelPosition,
            )
        },
        onCallAction = onCallAction,
        lobbyControlsContent = lobbyControlsContent,
    )
}

@Composable
private fun DefaultOnRenderedSlot(
    call: Call,
    video: ParticipantState.Video,
    onRendered: (View) -> Unit,
) {
    VideoTheme.componentFactory.CallLobbyOnRenderedContent(
        params = CallLobbyOnRenderedContentParams(
            call = call,
            video = video,
            onRendered = onRendered,
        ),
    )
}

@Composable
private fun DefaultOnDisabledSlot(user: User) {
    VideoTheme.componentFactory.CallLobbyOnDisabledContent(
        params = CallLobbyOnDisabledContentParams(user = user),
    )
}

@Composable
private fun BoxScope.DefaultParticipantLabelSlot(
    call: Call,
    user: User,
    isMicrophoneEnabled: Boolean,
    isCameraEnabled: Boolean,
    labelPosition: Alignment,
) {
    with(VideoTheme.componentFactory) {
        CallLobbyParticipantLabelContent(
            params = CallLobbyParticipantLabelContentParams(
                user = user,
                isMicrophoneEnabled = isMicrophoneEnabled,
                labelPosition = labelPosition,
                isCameraEnabled = isCameraEnabled,
                call = call,
            ),
        )
    }
}

@Composable
private fun DefaultLobbyControlsSlot(
    modifier: Modifier,
    call: Call,
    isCameraEnabled: Boolean,
    isMicrophoneEnabled: Boolean,
    permissions: VideoPermissionsState,
    onCallAction: (CallAction) -> Unit,
) {
    val isCameraUnavailable = permissions.isCameraPermissionDenied
    val isMicrophoneUnavailable = permissions.isMicrophonePermissionDenied
    VideoTheme.componentFactory.CallLobbyControlsContent(
        params = CallLobbyControlsContentParams(
            call = call,
            isCameraEnabled = isCameraEnabled,
            isMicrophoneEnabled = isMicrophoneEnabled,
            modifier = modifier,
            onCallAction = { action ->
                val needsPermission = (action is ToggleCamera && isCameraUnavailable) ||
                    (action is ToggleMicrophone && isMicrophoneUnavailable)
                if (needsPermission) {
                    permissions.launchPermissionRequest()
                } else {
                    onCallAction(action)
                }
            },
            isCameraUnavailable = isCameraUnavailable,
            isMicrophoneUnavailable = isMicrophoneUnavailable,
        ),
    )
}

@Composable
private fun DefaultJoinCallSlot(
    modifier: Modifier,
    call: Call,
    onJoinCall: () -> Unit,
) {
    VideoTheme.componentFactory.CallLobbyJoinContent(
        params = CallLobbyJoinContentParams(
            call = call,
            onJoinCall = onJoinCall,
            modifier = modifier,
        ),
    )
}

@Composable
internal fun BoxScope.DefaultParticipantLabel(
    user: User,
    isMicrophoneEnabled: Boolean,
    labelPosition: Alignment,
    isCameraEnabled: Boolean = true,
    call: Call? = null,
) {
    val nameLabel = if (user.id == StreamVideo.instance().user.id) {
        stringResource(id = R.string.stream_video_myself)
    } else {
        user.userNameOrId
    }
    val audioLevel = call?.localMicrophoneAudioLevel?.collectAsStateWithLifecycle()?.value ?: 0f

    ParticipantLabel(
        nameLabel = nameLabel,
        labelPosition = labelPosition,
        hasAudio = isMicrophoneEnabled,
        hasVideo = isCameraEnabled,
        soundIndicatorContent = {
            with(VideoTheme.componentFactory) {
                ParticipantLabelSoundIndicatorContent(
                    params = ParticipantLabelSoundIndicatorContentParams(
                        isSpeaking = call != null,
                        isAudioEnabled = isMicrophoneEnabled,
                        audioLevel = audioLevel,
                        modifier = Modifier
                            .testTag("Stream_UserMicrophone_Enabled_$isMicrophoneEnabled"),
                    ),
                )
            }
        },
        isSpeaking = call != null,
    )
}

@Composable
private fun DefaultPermissionHandler(
    videoPermission: VideoPermissionsState,
) {
    LaunchedEffect(key1 = videoPermission) {
        videoPermission.launchPermissionRequest()
    }
}

@Composable
internal fun OnRenderedContent(
    call: Call,
    video: ParticipantState.Video,
    onRendered: (View) -> Unit = {},
) {
    var isVideoRendered by remember { mutableStateOf(false) }

    LaunchedEffect(video.track?.streamId) {
        isVideoRendered = false
    }

    val videoRendererConfig = remember {
        videoRenderConfig {
            // Loading UI is shown as an overlay until the first frame is rendered.
            fallbackContent = {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VideoTheme.colors.backgroundCoreSurfaceSubtle)
            .testTag("on_rendered_content"),
    ) {
        VideoRenderer(
            modifier = Modifier.fillMaxSize(),
            call = call,
            video = video,
            videoRendererConfig = videoRendererConfig,
            onRendered = {
                isVideoRendered = true
                onRendered(it)
            },
        )
        if (!isVideoRendered) {
            DefaultMediaTrackFallbackContent(
                modifier = Modifier.fillMaxSize(),
                call = call,
            )
        }
    }
}

@Composable
internal fun OnDisabledContent(user: User) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VideoTheme.colors.backgroundCoreSurfaceSubtle)
            .testTag("on_disabled_content"),
    ) {
        VideoTheme.componentFactory.UserAvatar(
            UserAvatarParams(
                userImage = user.image,
                userName = user.name.takeUnless { it.isNullOrBlank() } ?: user.id,
                modifier = Modifier
                    .size(StreamTokens.size80)
                    .align(Alignment.Center)
                    .border(
                        width = StreamTokens.strokeW200,
                        color = VideoTheme.colors.borderCoreOnInverse,
                        shape = CircleShape,
                    ),
            ),
        )
    }
}

/** The shape of the lobby preview tile, shared by the clip and the border. */
private fun previewShape() = RoundedCornerShape(StreamTokens.radius2xl)

/**
 * The accent outline of the live preview, or the subtle outline of the avatar fallback. Drawn inside
 * the clipped bounds so it follows the rounded corners.
 */
@Composable
private fun Modifier.previewBorder(isCameraEnabled: Boolean): Modifier = border(
    width = if (isCameraEnabled) StreamTokens.strokeW200 else StreamTokens.strokeW100,
    color = if (isCameraEnabled) {
        VideoTheme.colors.accentPrimary
    } else {
        VideoTheme.colors.borderCoreDefault
    },
    shape = previewShape(),
)

@Composable
private fun defaultVideoPreviewModifier(): Modifier {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val sizing = if (isPortrait) {
        Modifier
            .fillMaxWidth()
            .aspectRatio(PREVIEW_ASPECT_RATIO)
    } else {
        // No landscape frame in the design; keep the portrait aspect ratio at a fixed height.
        Modifier
            .height(200.dp)
            .aspectRatio(PREVIEW_ASPECT_RATIO, matchHeightConstraintsFirst = true)
    }
    return sizing.clip(previewShape())
}
