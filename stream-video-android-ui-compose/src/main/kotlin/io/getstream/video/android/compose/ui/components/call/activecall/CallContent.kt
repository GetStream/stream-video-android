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

package io.getstream.video.android.compose.ui.components.call.activecall

import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.content.res.Configuration.UI_MODE_TYPE_CAR
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.android.video.generated.models.CallModerationBlurEvent
import io.getstream.android.video.generated.models.CallModerationWarningEvent
import io.getstream.log.StreamLog
import io.getstream.video.android.compose.lifecycle.MediaPiPLifecycle
import io.getstream.video.android.compose.permission.VideoPermissionsState
import io.getstream.video.android.compose.permission.rememberCallPermissionsState
import io.getstream.video.android.compose.pip.enterPictureInPicture
import io.getstream.video.android.compose.pip.rememberIsInPipMode
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.CallAppBar
import io.getstream.video.android.compose.ui.components.call.activecall.internal.DefaultPermissionHandler
import io.getstream.video.android.compose.ui.components.call.controls.ControlActions
import io.getstream.video.android.compose.ui.components.call.controls.actions.DefaultOnCallActionHandler
import io.getstream.video.android.compose.ui.components.call.diagnostics.CallDiagnosticsContent
import io.getstream.video.android.compose.ui.components.call.moderation.DefaultModerationWarningUiContainer
import io.getstream.video.android.compose.ui.components.call.moderation.ModerationWarningAnimationConfig
import io.getstream.video.android.compose.ui.components.call.renderer.LayoutType
import io.getstream.video.android.compose.ui.components.call.renderer.ParticipantVideo
import io.getstream.video.android.compose.ui.components.call.renderer.ParticipantsLayout
import io.getstream.video.android.compose.ui.components.call.renderer.RegularVideoRendererStyle
import io.getstream.video.android.compose.ui.components.call.renderer.VideoRendererStyle
import io.getstream.video.android.compose.ui.components.call.renderer.internal.LocalVideoContentSize
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfig
import io.getstream.video.android.core.pip.PictureInPictureConfiguration
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall
import kotlinx.coroutines.delay

/**
 * Represents the UI in an Active call that shows participants and their video, as well as some
 * extra UI features to control the call settings, browse participants and more.
 *
 * @param call The call includes states and will be rendered with participants.
 * @param modifier Modifier for styling.
 * @param layout the type of layout that the call content will display [LayoutType]
 * @param onBackPressed Handler when the user taps on the back button.
 * @param permissions Android permissions that should be required to render a video call properly.
 * @param onCallAction Handler when the user triggers a Call Control Action.
 * @param appBarContent Content is shown that calls information or additional actions.
 * @param style Defined properties for styling a single video call track.
 * @param videoRenderer A single video renderer renders each individual participant.
 * @param floatingVideoRenderer A floating video renderer renders an individual participant.
 * @param videoContent Content is shown that renders all participants' videos.
 * @param videoOverlayContent Content is shown that will be drawn over the [videoContent], excludes [controlsContent].
 * @param controlsContent Content is shown that allows users to trigger different actions to control a joined call.
 * @param pictureInPictureConfiguration User can provide Picture-In-Picture configuration.
 * @param pictureInPictureContent Content shown when the user enters Picture in Picture mode, if it's been enabled in the app.
 * @param closedCaptionUi You can pass your composable lambda here to render Closed Captions
 * @param videoModerationBlurUi You can pass your composable lambda here to render your own UI on top of Blurry video
 * @param videoModerationWarningUi You can pass your composable lambda here to render your own video moderation warning UI
 */
@Composable
public fun CallContent(
    call: Call,
    modifier: Modifier = Modifier,
    layout: LayoutType = LayoutType.DYNAMIC,
    permissions: VideoPermissionsState = rememberCallPermissionsState(call = call),
    onBackPressed: () -> Unit = {},
    onCallAction: (CallAction) -> Unit = { DefaultOnCallActionHandler.onCallAction(call, it) },
    appBarContent: @Composable (call: Call) -> Unit = {
        CallAppBar(
            call = call,
            onBackPressed = onBackPressed,
            onCallAction = onCallAction,
        )
    },
    style: VideoRendererStyle = RegularVideoRendererStyle(),
    videoRenderer: @Composable (
        modifier: Modifier,
        call: Call,
        participant: ParticipantState,
        style: VideoRendererStyle,
    ) -> Unit = { videoModifier, videoCall, videoParticipant, videoStyle ->
        ParticipantVideo(
            modifier = videoModifier,
            call = videoCall,
            participant = videoParticipant,
            style = videoStyle,
        )
    },
    floatingVideoRenderer: @Composable (BoxScope.(call: Call, IntSize) -> Unit)? = null,
    videoContent: @Composable RowScope.(call: Call) -> Unit = {
        ParticipantsLayout(
            layoutType = layout,
            call = call,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(bottom = VideoTheme.dimens.spacingXXs),
            style = style,
            videoRenderer = videoRenderer,
            floatingVideoRenderer = floatingVideoRenderer,
        )
    },
    videoOverlayContent: @Composable (call: Call) -> Unit = {},
    controlsContent: @Composable (call: Call) -> Unit = {
        ControlActions(
            modifier = Modifier.wrapContentWidth(),
            call = call,
            onCallAction = onCallAction,
        )
    },
    pictureInPictureConfiguration: PictureInPictureConfiguration =
        PictureInPictureConfiguration(true),
    pictureInPictureContent: @Composable (Call) -> Unit = { DefaultPictureInPictureContent(it) },
    enableDiagnostics: Boolean = false,
    closedCaptionUi: @Composable (Call) -> Unit = {},
    videoModerationBlurUi: @Composable (Call) -> Unit = {},
    videoModerationWarningUi: @Composable (Call, String?) -> Unit = { _, message ->
        val callServiceConfig = StreamVideo.instanceOrNull()?.state?.callConfigRegistry?.get(call.type) ?: CallServiceConfig()
        val displayTime = callServiceConfig.moderationConfig.moderationWarningConfig.displayTime
        DefaultModerationWarningUiContainer(
            call,
            message,
            moderationWarningAnimationConfig = ModerationWarningAnimationConfig(displayTime),
        )
    },
) {
    val context = LocalContext.current
    val orientation = LocalConfiguration.current.orientation
    val isInPictureInPicture = rememberIsInPipMode()

    DefaultPermissionHandler(videoPermission = permissions)

    MediaPiPLifecycle(call = call, pictureInPictureConfiguration)

    BackHandler {
        if (pictureInPictureConfiguration.enable) {
            try {
                enterPictureInPicture(context = context, call = call, pictureInPictureConfiguration)
            } catch (e: Exception) {
                StreamLog.e(tag = "CallContent") { e.stackTraceToString() }
                call.leave()
            }
        } else {
            onBackPressed.invoke()
        }
    }

    if (isInPictureInPicture && pictureInPictureConfiguration.enable) {
        pictureInPictureContent(call)
    } else {
        Scaffold(
            backgroundColor = VideoTheme.colors.baseSheetPrimary,
            contentColor = VideoTheme.colors.baseSheetPrimary,
            topBar = {
                if (orientation == ORIENTATION_PORTRAIT) {
                    appBarContent.invoke(call)
                }
            },
            bottomBar = {
                if (orientation == ORIENTATION_PORTRAIT) {
                    controlsContent.invoke(call)
                }
            },
            content = {
                val paddings = PaddingValues(
                    top = (it.calculateTopPadding() - VideoTheme.dimens.spacingS)
                        .coerceAtLeast(0.dp),
                    start = it.calculateStartPadding(
                        layoutDirection = LocalLayoutDirection.current,
                    ),
                    end = it.calculateEndPadding(
                        layoutDirection = LocalLayoutDirection.current,
                    ),
                    bottom = (it.calculateBottomPadding() - VideoTheme.dimens.spacingS)
                        .coerceAtLeast(0.dp),
                )
                var showDiagnostics by remember { mutableStateOf(false) }
                Row(
                    modifier = modifier
                        .background(color = VideoTheme.colors.baseSheetPrimary)
                        .padding(paddings)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    showDiagnostics = true
                                },
                            )
                        },
                ) {
                    BoxWithConstraints {
                        val contentSize = IntSize(constraints.maxWidth, constraints.maxHeight)

                        CompositionLocalProvider(LocalVideoContentSize provides contentSize) {
                            videoContent.invoke(this@Row, call)
                            videoOverlayContent.invoke(call)
                        }
                    }
                }

                if (enableDiagnostics && showDiagnostics) {
                    CallDiagnosticsContent(call = call) {
                        showDiagnostics = false
                    }
                }
                closedCaptionUi(call)
                ModerationBlurUi(call, videoModerationBlurUi)
                ModerationWarningRootUi(call, videoModerationWarningUi)
            },
        )
    }
}

@Composable
private fun ModerationBlurUi(call: Call, moderationBlurUi: @Composable (Call) -> Unit) {
    val callServiceConfig = StreamVideo.instanceOrNull()?.state?.callConfigRegistry?.get(call.type) ?: CallServiceConfig()
    if (callServiceConfig.moderationConfig.videoModerationConfig.enable) {
        var blurEvent by remember { mutableStateOf<CallModerationBlurEvent?>(null) }
        LaunchedEffect(call) {
            call.events.collect { event ->
                when (event) {
                    is CallModerationBlurEvent -> {
                        blurEvent = event
                        call.state.moderationManager.enableVideoModeration()
                        delay(callServiceConfig.moderationConfig.videoModerationConfig.blurDuration)
                        blurEvent = null // auto-dismiss after config duration
                        call.state.moderationManager.disableVideoModeration()
                    }
                }
            }
        }
        blurEvent?.let {
            moderationBlurUi(call)
        }
    }
}

@Composable
private fun ModerationWarningRootUi(
    call: Call,
    moderationWarningUi: @Composable (Call, String?) -> Unit,
) {
    val callServiceConfig = StreamVideo.instanceOrNull()?.state?.callConfigRegistry?.get(call.type) ?: CallServiceConfig()
    if (callServiceConfig.moderationConfig.moderationWarningConfig.enable) {
        var warningEvent by remember { mutableStateOf<CallModerationWarningEvent?>(null) }
        LaunchedEffect(call) {
            call.events.collect { event ->
                when (event) {
                    is CallModerationWarningEvent -> {
                        warningEvent = event
                        delay(
                            callServiceConfig.moderationConfig.moderationWarningConfig.displayTime,
                        )
                        warningEvent = null // auto-dismiss after config duration
                    }
                }
            }
        }
        warningEvent?.let { event ->
            moderationWarningUi(call, event.message)
        }
    }
}

@Deprecated("Use CallContent  with pictureInPictureConfiguration argument")
@Composable
public fun CallContent(
    call: Call,
    modifier: Modifier = Modifier,
    layout: LayoutType = LayoutType.DYNAMIC,
    permissions: VideoPermissionsState = rememberCallPermissionsState(call = call),
    onBackPressed: () -> Unit = {},
    onCallAction: (CallAction) -> Unit = { DefaultOnCallActionHandler.onCallAction(call, it) },
    appBarContent: @Composable (call: Call) -> Unit = {
        CallAppBar(
            call = call,
            onBackPressed = onBackPressed,
            onCallAction = onCallAction,
        )
    },
    style: VideoRendererStyle = RegularVideoRendererStyle(),
    videoRenderer: @Composable (
        modifier: Modifier,
        call: Call,
        participant: ParticipantState,
        style: VideoRendererStyle,
    ) -> Unit = { videoModifier, videoCall, videoParticipant, videoStyle ->
        ParticipantVideo(
            modifier = videoModifier,
            call = videoCall,
            participant = videoParticipant,
            style = videoStyle,
        )
    },
    floatingVideoRenderer: @Composable (BoxScope.(call: Call, IntSize) -> Unit)? = null,
    videoContent: @Composable RowScope.(call: Call) -> Unit = {
        ParticipantsLayout(
            layoutType = layout,
            call = call,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(bottom = VideoTheme.dimens.spacingXXs),
            style = style,
            videoRenderer = videoRenderer,
            floatingVideoRenderer = floatingVideoRenderer,
        )
    },
    videoOverlayContent: @Composable (call: Call) -> Unit = {},
    controlsContent: @Composable (call: Call) -> Unit = {
        ControlActions(
            modifier = Modifier.wrapContentWidth(),
            call = call,
            onCallAction = onCallAction,
        )
    },
    enableInPictureInPicture: Boolean,
    pictureInPictureContent: @Composable (Call) -> Unit = { DefaultPictureInPictureContent(it) },
    enableDiagnostics: Boolean = false,
    closedCaptionUi: @Composable (Call) -> Unit = {},
) {
    CallContent(
        call, modifier, layout, permissions, onBackPressed, onCallAction, appBarContent, style,
        videoRenderer, floatingVideoRenderer, videoContent, videoOverlayContent, controlsContent,
        PictureInPictureConfiguration(enableInPictureInPicture),
        pictureInPictureContent, enableDiagnostics, closedCaptionUi,
    )
}

/**
 * Renders the default PiP content, using the call state that's provided.
 *
 * @param call The state of the call, with its participants.
 */
@Composable
internal fun DefaultPictureInPictureContent(call: Call) {
    val screenSharingSession by call.state.screenSharingSession.collectAsStateWithLifecycle()
    val session = screenSharingSession
    val video = session?.participant?.video?.collectAsStateWithLifecycle()
    val pictureInPictureAspectRatio: Float = 16f / 9f

    if (session != null && !session.participant.isLocal) {
        VideoRenderer(
            modifier = Modifier.aspectRatio(pictureInPictureAspectRatio, false),
            call = call,
            video = video?.value,
        )
    } else {
        val me by call.state.me.collectAsStateWithLifecycle()
        val participants by call.state.participants.collectAsStateWithLifecycle()
        val notMeOfTwo by remember {
            // Special case where there are only two participants to take always the other participant,
            // regardless of video track.
            derivedStateOf {
                participants.takeIf {
                    it.size == 2
                }?.firstOrNull { it.sessionId != me?.sessionId }
            }
        }
        val activeSpeakers by call.state.activeSpeakers.collectAsStateWithLifecycle()
        val dominantSpeaker by call.state.dominantSpeaker.collectAsStateWithLifecycle()
        val notMeActiveOrDominant by remember {
            derivedStateOf {
                val activeNotMe = activeSpeakers.firstOrNull {
                    it.sessionId != me?.sessionId
                }
                val dominantNotMe = dominantSpeaker?.takeUnless {
                    it.sessionId == me?.sessionId
                }

                activeNotMe ?: dominantNotMe
            }
        }
        val participantToShow = notMeOfTwo ?: notMeActiveOrDominant ?: me
        if (participantToShow != null) {
            ParticipantVideo(
                call = call,
                participant = participantToShow,
                style = RegularVideoRendererStyle(labelPosition = Alignment.BottomStart),
            )
        }
    }
}

@Preview
@Composable
private fun CallContentPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        CallContent(call = previewCall)
    }
}

@Preview(
    widthDp = 640,
    heightDp = 360,
    uiMode = UI_MODE_TYPE_CAR,
)
@Composable
private fun CallContentPreviewLandscape() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        CallContent(call = previewCall)
    }
}
