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

@file:OptIn(ExperimentalMaterialApi::class)

package io.getstream.video.android.ui.call

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.media.projection.MediaProjectionManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Snackbar
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GroupRemove
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.SignalWifiBad
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.android.video.generated.models.OwnCapability
import io.getstream.android.video.generated.models.TranscriptionSettingsResponse
import io.getstream.chat.android.ui.common.state.messages.list.MessageItemState
import io.getstream.video.android.BuildConfig
import io.getstream.video.android.R
import io.getstream.video.android.compose.pip.rememberIsInPipMode
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.base.StreamBadgeBox
import io.getstream.video.android.compose.ui.components.base.StreamDialogPositiveNegative
import io.getstream.video.android.compose.ui.components.base.StreamIconToggleButton
import io.getstream.video.android.compose.ui.components.call.CallAppBar
import io.getstream.video.android.compose.ui.components.call.activecall.CallContent
import io.getstream.video.android.compose.ui.components.call.controls.actions.ChatDialogAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.DefaultOnCallActionHandler
import io.getstream.video.android.compose.ui.components.call.controls.actions.FlipCameraAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.GenericAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.LeaveCallAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ScreenShareToggleAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleCameraAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleMicrophoneAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleSettingsAction
import io.getstream.video.android.compose.ui.components.call.pinning.ParticipantAction
import io.getstream.video.android.compose.ui.components.call.pinning.ParticipantActions
import io.getstream.video.android.compose.ui.components.call.renderer.FloatingParticipantVideo
import io.getstream.video.android.compose.ui.components.call.renderer.LayoutType
import io.getstream.video.android.compose.ui.components.call.renderer.ParticipantVideo
import io.getstream.video.android.compose.ui.components.call.renderer.RegularVideoRendererStyle
import io.getstream.video.android.compose.ui.components.call.renderer.copy
import io.getstream.video.android.compose.ui.components.video.VideoScalingType
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.call.state.ChooseLayout
import io.getstream.video.android.core.model.PreferredVideoResolution
import io.getstream.video.android.core.pip.PictureInPictureConfiguration
import io.getstream.video.android.core.utils.isEnabled
import io.getstream.video.android.filters.video.BlurredBackgroundVideoFilter
import io.getstream.video.android.filters.video.VirtualBackgroundVideoFilter
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall
import io.getstream.video.android.tooling.extensions.toPx
import io.getstream.video.android.tooling.util.StreamBuildFlavorUtil
import io.getstream.video.android.ui.closedcaptions.ClosedCaptionUiState
import io.getstream.video.android.ui.closedcaptions.ClosedCaptionUiState.Available.toClosedCaptionUiState
import io.getstream.video.android.ui.closedcaptions.ClosedCaptionsContainer
import io.getstream.video.android.ui.closedcaptions.ClosedCaptionsDefaults
import io.getstream.video.android.ui.menu.SettingsMenu
import io.getstream.video.android.ui.menu.VideoFilter
import io.getstream.video.android.ui.menu.availableVideoFilters
import io.getstream.video.android.util.config.AppConfig
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
@Composable
fun CallScreen(
    call: Call,
    showDebugOptions: Boolean = false,
    pictureInPictureConfiguration: PictureInPictureConfiguration? = null,
    onCallDisconnected: () -> Unit = {},
    onUserLeaveCall: () -> Unit = {},
) {
    val context = LocalContext.current
    val isCameraEnabled by call.camera.isEnabled.collectAsStateWithLifecycle()
    val isMicrophoneEnabled by call.microphone.isEnabled.collectAsStateWithLifecycle()
    val speakingWhileMuted by call.state.speakingWhileMuted.collectAsStateWithLifecycle()
    var isShowingSettingMenu by remember { mutableStateOf(false) }
    var isShowingLayoutChooseMenu by remember { mutableStateOf(false) }
    var isShowingAvailableDeviceMenu by remember { mutableStateOf(false) }
    var selectedVideoFilter by remember { mutableIntStateOf(0) }
    var isShowingFeedbackDialog by remember { mutableStateOf(false) }
    var isShowingStats by remember { mutableStateOf(false) }
    var layout by remember { mutableStateOf(LayoutType.DYNAMIC) }
    var unreadCount by remember { mutableIntStateOf(0) }
    var showShareDialog by rememberSaveable { mutableStateOf(true) }
    var showParticipants by remember { mutableStateOf(false) }
    val chatState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true,
    )
    var showRecordingWarning by remember {
        mutableStateOf(false)
    }
    val orientation = LocalConfiguration.current.orientation
    var showEndRecordingDialog by remember { mutableStateOf(false) }
    var acceptedCallRecording by remember { mutableStateOf(false) }
    val isRecording by call.state.recording.collectAsStateWithLifecycle()
    val participantsSize by call.state.participants.collectAsStateWithLifecycle()
    val messages: MutableList<MessageItemState> = remember { mutableStateListOf() }
    var messagesVisibility by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val messageScope = rememberCoroutineScope()
    var showingLandscapeControls by remember { mutableStateOf(false) }
    var preferredScaleType by remember { mutableStateOf(VideoScalingType.SCALE_ASPECT_FILL) }
    var selectedIncomingVideoResolution by remember {
        mutableStateOf<PreferredVideoResolution?>(null)
    }
    var isIncomingVideoEnabled by remember { mutableStateOf(true) }

    val connection by call.state.connection.collectAsStateWithLifecycle()
    val me by call.state.me.collectAsStateWithLifecycle()
    var anyPausedVideos by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = call) {
        while (true) {
            delay(2000)
            call.state.participants
                .sample(2000L)
                .map { participants ->
                    // Compute only paused videos
                    participants.filter { it.videoPaused.value }
                }
                .distinctUntilChanged() // Only emit if the filtered list actually changes
                .collect { pausedVideos ->
                    anyPausedVideos = pausedVideos.isNotEmpty()
                }
        }
    }

    LaunchedEffect(key1 = connection) {
        if (connection == RealtimeConnection.Disconnected) {
            onCallDisconnected.invoke()
        } else if (connection is RealtimeConnection.Failed) {
            Toast.makeText(
                context,
                "Call connection failed (${(connection as RealtimeConnection.Failed).error}",
                Toast.LENGTH_LONG,
            ).show()
            onCallDisconnected.invoke()
        }
    }
    val paddings = if (orientation == Configuration.ORIENTATION_PORTRAIT) {
        PaddingValues(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 16.dp)
    } else {
        PaddingValues(0.dp)
    }

    /**
     * Logic to Closed Captions UI State and render UI accordingly
     */

    val ccMode by call.state.ccMode.collectAsStateWithLifecycle()
    val captioning by call.state.isCaptioning.collectAsStateWithLifecycle()

    var closedCaptionUiState: ClosedCaptionUiState by remember {
        mutableStateOf(ccMode.toClosedCaptionUiState())
    }

    val updateClosedCaptionUiState: (ClosedCaptionUiState) -> Unit = { newState ->
        closedCaptionUiState = newState
    }

    val onLocalClosedCaptionsClick: () -> Unit = {
        scope.launch {
            when (closedCaptionUiState) {
                is ClosedCaptionUiState.Running -> {
                    updateClosedCaptionUiState(ClosedCaptionUiState.Available)
                }

                is ClosedCaptionUiState.Available -> {
                    if (captioning) {
                        updateClosedCaptionUiState(ClosedCaptionUiState.Running)
                    } else {
                        call.startClosedCaptions()
                    }
                }

                else -> {
                    throw Exception(
                        "This state $closedCaptionUiState should not invoke any ui operation",
                    )
                }
            }
        }
    }

    /**
     * AUTO START/STOP TRANSCRIPTION LOGIC
     *
     * This code handles the automatic transcription logic, ensuring transcription starts or stops
     * based on the current settings and state. While it usually behaves as expected, consider the following scenario:
     *
     * - Transcription is set to "Auto-On" in the settings.
     * - The current transcription state (`isCurrentlyTranscribing`) is `false` because it was toggled by a participant.
     * - A new participant joins the call.
     *
     * In this scenario, the transcription will automatically start, overriding the previous `false` state.
     * This behavior is intentional for this demo-app ONLY and designed to prioritize the "Auto-On" setting over the current state.
     *
     * Please keep this behavior in mind, as it might appear unexpected at first glance.
     *
     * Note: Occasionally, when `call.startTranscription()` might throw a 400 error in the demo app when Transcription is set to "Auto-On", indicating that
     * transcription is already in progress. This is expected and can safely be ignored as it does not impact
     * the ongoing transcription functionality.
     */
    val isCurrentlyTranscribing by call.state.transcribing.collectAsStateWithLifecycle()

    val isScreenSharing by call.screenShare.isEnabled.collectAsStateWithLifecycle()

    val screenSharePermissionResult = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            if (it.resultCode == Activity.RESULT_OK && it.data != null) {
                call.startScreenSharing(it.data!!, includeAudio = true)
            }
        },
    )

    LaunchedEffect(Unit) {
        call.state.settings.map { it?.transcription }
            .collectLatest { transcription ->
                executeTranscriptionApis(call, isCurrentlyTranscribing, transcription)
            }
    }

    VideoTheme {
        ChatDialog(
            state = chatState,
            call = call,
            content = {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    CallContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddings)
                            .background(
                                color = VideoTheme.colors.baseSheetPrimary,
                            ),
                        call = call,
                        layout = layout,
                        pictureInPictureConfiguration = pictureInPictureConfiguration
                            ?: PictureInPictureConfiguration(true),
                        enableDiagnostics = BuildConfig.DEBUG || StreamBuildFlavorUtil.isDevelopment,
                        onCallAction = {
                            when (it) {
                                ChooseLayout -> isShowingLayoutChooseMenu = true
                                else -> DefaultOnCallActionHandler.onCallAction(
                                    call,
                                    it,
                                )
                            }
                        },
                        appBarContent = {
                            CallAppBar(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                call = call,
                                leadingContent = {
                                    val iconOnOff = ImageVector.vectorResource(
                                        R.drawable.ic_layout_grid,
                                    )
                                    Row {
                                        ToggleAction(
                                            modifier = Modifier.testTag("Stream_CallViewButton"),
                                            offStyle = VideoTheme.styles.buttonStyles.secondaryIconButtonStyle(),
                                            isActionActive = !isShowingLayoutChooseMenu,
                                            iconOnOff = Pair(iconOnOff, iconOnOff),
                                        ) {
                                            isShowingLayoutChooseMenu =
                                                !isShowingLayoutChooseMenu
                                        }

                                        Spacer(
                                            modifier = Modifier.size(
                                                VideoTheme.dimens.spacingM,
                                            ),
                                        )

                                        FlipCameraAction(
                                            modifier = Modifier.testTag(
                                                "Stream_FlipCameraIcon_${call.camera.direction.collectAsState().value}",
                                            ),
                                            onCallAction = { call.camera.flip() },
                                        )
                                    }
                                },
                                trailingContent = {
                                    LeaveCallAction(
                                        modifier = Modifier.testTag("Stream_HangUpButton"),
                                    ) {
                                        call.leave()
                                    }
                                },
                            )
                        },
                        onBackPressed = {
                            if (chatState.currentValue == ModalBottomSheetValue.Expanded) {
                                scope.launch { chatState.hide() }
                            } else {
                                onUserLeaveCall.invoke()
                            }
                        },
                        controlsContent = {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row {
                                    ToggleSettingsAction(
                                        modifier = Modifier
                                            .testTag(
                                                "Stream_CallSettingsToggle_Open_$isShowingSettingMenu",
                                            ),
                                        isShowingSettings = !isShowingSettingMenu,
                                        onCallAction = {
                                            isShowingSettingMenu = !isShowingSettingMenu
                                        },
                                    )
                                    Spacer(modifier = Modifier.size(VideoTheme.dimens.spacingM))
                                    if (isTablet()) {
                                        ScreenShareToggleAction(
                                            active = isScreenSharing,
                                            onCallAction = {
                                                if (!isScreenSharing) {
                                                    scope.launch {
                                                        val mediaProjectionManager =
                                                            context.getSystemService(
                                                                MediaProjectionManager::class.java,
                                                            )
                                                        screenSharePermissionResult.launch(
                                                            mediaProjectionManager.createScreenCaptureIntent(),
                                                        )
                                                    }
                                                } else {
                                                    call.stopScreenSharing()
                                                }
                                            },
                                        )
                                        Spacer(
                                            modifier = Modifier.size(
                                                VideoTheme.dimens.spacingM,
                                            ),
                                        )
                                    }
                                    ToggleCameraAction(
                                        modifier = Modifier
                                            .testTag(
                                                "Stream_CameraToggle_Enabled_$isCameraEnabled",
                                            ),
                                        isCameraEnabled = isCameraEnabled,
                                        onCallAction = { call.camera.setEnabled(it.isEnabled) },
                                    )
                                    Spacer(modifier = Modifier.size(VideoTheme.dimens.spacingM))
                                    ToggleMicrophoneAction(
                                        modifier = Modifier
                                            .testTag(
                                                "Stream_MicrophoneToggle_Enabled_$isMicrophoneEnabled",
                                            ),
                                        isMicrophoneEnabled = isMicrophoneEnabled,
                                        onCallAction = {
                                            call.microphone.setEnabled(
                                                it.isEnabled,
                                            )
                                        },
                                    )
                                    Spacer(modifier = Modifier.size(VideoTheme.dimens.spacingM))
                                }
                                Row {
                                    StreamBadgeBox(
                                        text = participantsSize.size.toString(),
                                    ) {
                                        GenericAction(icon = Icons.Default.People) {
                                            showParticipants = !showParticipants
                                        }
                                    }
                                    Spacer(modifier = Modifier.size(VideoTheme.dimens.spacingM))
                                    ChatDialogAction(
                                        modifier = Modifier.testTag("Stream_ChatButton"),
                                        messageCount = unreadCount,
                                        onCallAction = { scope.launch { chatState.show() } },
                                    )
                                }
                            }
                        },
                        videoRenderer = { modifier, call, participant, style ->
                            ParticipantVideo(
                                modifier = modifier.testTag("Stream_VideoView"),
                                call = call,
                                participant = participant,
                                style = style,
                                scalingType = preferredScaleType,
                                reactionContent = {
                                    CustomReactionContent(
                                        participant = participant,
                                        style = style.copy(
                                            reactionPosition = Alignment.TopCenter,
                                            reactionDuration = 5000,
                                        ),
                                    )
                                },
                                actionsContent = { actions, call, participant ->
                                    ParticipantActions(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(8.dp)
                                            .testTag("Stream_ParticipantActionsIcon"),
                                        actions = actions + listOf(
                                            ParticipantAction(
                                                icon = Icons.Filled.GroupRemove,
                                                label = "Kick",
                                                condition = { call, participantState ->
                                                    call.hasCapability(OwnCapability.KickUser)
                                                },
                                                action = { call, participantState ->
                                                    launch {
                                                        call.kickUser(
                                                            participantState.userId.value,
                                                            false,
                                                        )
                                                    }
                                                },
                                            ),
                                        ),
                                        call = call,
                                        participant = participant,
                                    )
                                },
                            )
                        },
                        floatingVideoRenderer = { _, _ ->
                            me?.let {
                                FloatingParticipantVideo(
                                    call = call,
                                    participant = it,
                                    parentBounds = IntSize(
                                        this@BoxWithConstraints.constraints.maxWidth,
                                        this@BoxWithConstraints.constraints.maxHeight,
                                    ),
                                    videoRenderer = { participant ->
                                        ParticipantVideo(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(VideoTheme.shapes.dialog)
                                                .testTag("Stream_FloatingVideoView"),
                                            call = call,
                                            participant = participant,
                                            reactionContent = {
                                                CustomReactionContent(
                                                    participant = participant,
                                                    style = RegularVideoRendererStyle().copy(
                                                        isShowingConnectionQualityIndicator = false,
                                                        reactionPosition = Alignment.TopCenter,
                                                        reactionDuration = 5000,
                                                    ),
                                                )
                                            },
                                        )
                                    },
                                )
                            }
                        },
                        videoOverlayContent = {
                            Crossfade(
                                modifier = Modifier.align(Alignment.BottomStart),
                                targetState = messagesVisibility,
                                label = "chat_overlay",
                            ) { visibility ->
                                if (visibility) {
                                    ChatOverly(messages = messages)
                                }
                            }
                        },
                        closedCaptionUi = { call ->
                            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                                ClosedCaptionsContainer(
                                    call,
                                    ClosedCaptionsDefaults.streamThemeConfig(),
                                    closedCaptionUiState,
                                )
                            } else {
                                ClosedCaptionsContainer(
                                    call,
                                    ClosedCaptionsDefaults.streamThemeConfig().copy(
                                        yOffset = (-100).dp,
                                    ),
                                    closedCaptionUiState,
                                )
                            }
                        },
                    )
                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        StreamIconToggleButton(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(VideoTheme.dimens.spacingM),
                            toggleState = rememberUpdatedState(
                                newValue = ToggleableState(
                                    showingLandscapeControls,
                                ),
                            ),
                            onIcon = Icons.Default.MoreVert,
                            onStyle = VideoTheme.styles.buttonStyles.secondaryIconButtonStyle(),
                            offStyle = VideoTheme.styles.buttonStyles.tertiaryIconButtonStyle(),
                        ) {
                            showingLandscapeControls = when (it) {
                                ToggleableState.On -> false
                                ToggleableState.Off -> true
                                ToggleableState.Indeterminate -> false
                            }
                        }
                    }
                }
            },
            updateUnreadCount = { unreadCount = it },
            onNewMessages = { updatedMessages ->
                messages.clear()
                messages += updatedMessages
                messageScope.launch {
                    messagesVisibility = true
                    delay(10000L)
                    messagesVisibility = false
                }
            },
            onDismissed = {
                if (chatState.currentValue == ModalBottomSheetValue.Expanded) {
                    scope.launch { chatState.hide() }
                } else {
                    onCallDisconnected.invoke()
                }
            },
        )

        val isPictureInPictureMode = rememberIsInPipMode()
        if (!isPictureInPictureMode) {
            if (showShareDialog &&
                participantsSize.size == 1 &&
                !chatState.isVisible
            ) {
                val clipboardManager = remember(context) {
                    context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                }
                val env = AppConfig.currentEnvironment.collectAsStateWithLifecycle()

                val configuration = LocalConfiguration.current
                val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

                val popupYOffset = if (isPortrait) {
                    -(VideoTheme.dimens.componentHeightL + VideoTheme.dimens.spacingS).toPx()
                        .toInt()
                } else {
                    0
                }

                Popup(
                    alignment = Alignment.BottomCenter,
                    offset = IntOffset(
                        0,
                        popupYOffset,
                    ),
                ) {
                    Box {
                        ShareCallWithOthers(
                            modifier = Modifier.fillMaxWidth(),
                            call = call,
                            clipboardManager = clipboardManager,
                            env = env,
                            context = context,
                        )

                        IconButton(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 10.dp, end = 10.dp)
                                .testTag("Stream_InviteCloseButton"),
                            onClick = { showShareDialog = false },
                        ) {
                            Icon(
                                tint = Color.White,
                                imageVector = Icons.Default.Close,
                                contentDescription = Icons.Default.Close.name,
                            )
                        }
                    }
                }
            }
        }

        if (speakingWhileMuted) {
            SpeakingWhileMuted()
        }

        if (anyPausedVideos) {
            BadNetworkLabel()
        }

        if (showingLandscapeControls && orientation == Configuration.ORIENTATION_LANDSCAPE) {
            LandscapeControls(call, onChat = {
                showingLandscapeControls = false
                scope.launch { chatState.show() }
            }, onSettings = {
                showingLandscapeControls = false
                isShowingSettingMenu = true
            }) {
                showingLandscapeControls = !showingLandscapeControls
            }
        }

        if (isShowingSettingMenu) {
            var isNoiseCancellationEnabled by remember {
                mutableStateOf(call.isAudioProcessingEnabled())
            }
            val settings by call.state.settings.collectAsStateWithLifecycle()
            val noiseCancellationFeatureEnabled =
                settings?.audio?.noiseCancellation?.isEnabled == true
            SettingsMenu(
                call = call,
                selectedVideoFilter = selectedVideoFilter,
                showDebugOptions = showDebugOptions,
                noiseCancellationFeatureEnabled = noiseCancellationFeatureEnabled,
                noiseCancellationEnabled = isNoiseCancellationEnabled,
                onDismissed = { isShowingSettingMenu = false },
                onSelectVideoFilter = { filterIndex ->
                    selectedVideoFilter = filterIndex

                    when (val filter = availableVideoFilters[filterIndex]) {
                        is VideoFilter.None -> {
                            call.videoFilter = null
                        }

                        is VideoFilter.BlurredBackground -> {
                            call.videoFilter = BlurredBackgroundVideoFilter()
                        }

                        is VideoFilter.VirtualBackground -> {
                            call.videoFilter =
                                VirtualBackgroundVideoFilter(context, filter.drawable)
                        }
                    }
                },
                onShowFeedback = {
                    isShowingSettingMenu = false
                    isShowingFeedbackDialog = true
                },
                onNoiseCancellation = {
                    isNoiseCancellationEnabled = call.toggleAudioProcessing()
                },
                selectedIncomingVideoResolution = selectedIncomingVideoResolution,
                onSelectIncomingVideoResolution = {
                    call.setIncomingVideoEnabled(true)
                    isIncomingVideoEnabled = true

                    call.setPreferredIncomingVideoResolution(it)
                    selectedIncomingVideoResolution = it

                    isShowingSettingMenu = false
                },
                isIncomingVideoEnabled = isIncomingVideoEnabled,
                onToggleIncomingVideoVisibility = {
                    call.setIncomingVideoEnabled(it)
                    isIncomingVideoEnabled = it

                    isShowingSettingMenu = false
                },
                onSelectScaleType = {
                    preferredScaleType = it
                    isShowingSettingMenu = false
                },
                onShowCallStats = {
                    isShowingStats = true
                    isShowingSettingMenu = false
                },
                closedCaptionUiState = closedCaptionUiState,
                onClosedCaptionsToggle = onLocalClosedCaptionsClick,
            )
        }

        if (isShowingFeedbackDialog) {
            FeedbackDialog(call = call) {
                isShowingFeedbackDialog = false
            }
        }

        if (isShowingLayoutChooseMenu) {
            LayoutChooser(
                onLayoutChoice = {
                    layout = it
                    isShowingLayoutChooseMenu = false
                },
                current = layout,
                onDismiss = { isShowingLayoutChooseMenu = false },
            )
        }

        if (isShowingStats) {
            CallStatsDialog(call) { isShowingStats = false }
        }

        if (showParticipants) {
            ParticipantsDialog(call) {
                showParticipants = !showParticipants
            }
        }

        if (isShowingAvailableDeviceMenu) {
            AvailableDeviceMenu(
                call = call,
                onDismissed = { isShowingAvailableDeviceMenu = false },
            )
        }

        // TODO: AAP, move recording and actions in separate composables.
        if (isRecording && !showRecordingWarning) {
            StreamDialogPositiveNegative(
                title = "This call is being recorded",
                contentText = "By staying in the call you’re consenting to being recorded.",
                positiveButton = Triple(
                    "Continue",
                    VideoTheme.styles.buttonStyles.secondaryButtonStyle(),
                ) {
                    showRecordingWarning = true
                    acceptedCallRecording = true
                },
                negativeButton = Triple(
                    "Leave",
                    VideoTheme.styles.buttonStyles.tertiaryButtonStyle(),
                ) {
                    showRecordingWarning = false
                    acceptedCallRecording = false
                    call.leave()
                },
            )
        }
        if (showEndRecordingDialog) {
            StreamDialogPositiveNegative(
                title = "End recording",
                contentText = "Are you sure you want to end the recording?",
                positiveButton = Triple(
                    "End",
                    VideoTheme.styles.buttonStyles.alertButtonStyle(),
                ) {
                    scope.launch {
                        call.stopRecording()
                    }
                    showEndRecordingDialog = false
                },
                negativeButton = Triple(
                    "Cancel",
                    VideoTheme.styles.buttonStyles.tertiaryButtonStyle(),
                ) {
                    showEndRecordingDialog = false
                },
            )
        }
    }
}

@Deprecated(
    "Use CallScreen with pictureInPictureConfiguration argument",
    ReplaceWith(
        "CallScreen(call, showDebugOptions, pictureInPictureConfiguration, onCallDisconnected, onUserLeaveCall)",
    ),
)
@OptIn(FlowPreview::class)
@Composable
fun CallScreen(
    call: Call,
    showDebugOptions: Boolean = false,
    onCallDisconnected: () -> Unit = {},
    onUserLeaveCall: () -> Unit = {},
) {
    CallScreen(
        call,
        showDebugOptions,
        PictureInPictureConfiguration(true),
        onCallDisconnected,
        onUserLeaveCall,
    )
}

/**
 * Executes the transcription APIs based on the current transcription state and settings.
 *
 * - Stops transcription if the mode is "Disabled" and transcription is currently active.
 * - Starts transcription if the mode is "Auto-On" and transcription is not currently active.
 * - Takes no action for other scenarios.
 */
private suspend fun executeTranscriptionApis(
    call: Call,
    transcribing: Boolean,
    transcriptionSettingsResponse:
    TranscriptionSettingsResponse?,
) {
    val mode = transcriptionSettingsResponse?.mode
    if (mode == TranscriptionSettingsResponse.Mode.Disabled && transcribing) {
        call.stopTranscription()
    } else if (mode == TranscriptionSettingsResponse.Mode.AutoOn && !transcribing) {
        call.startTranscription()
    } else {
    }
}

@Composable
private fun SpeakingWhileMuted() {
    Snackbar {
        Text(text = "You're talking while muting the microphone!")
    }
}

@Composable
private fun BadNetworkLabel(
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = modifier
                .align(Alignment.BottomCenter)
                .padding(vertical = 90.dp, horizontal = 16.dp)
                .background(
                    color = VideoTheme.colors.baseSheetQuarternary,
                    shape = VideoTheme.shapes.sheet,
                )
                .testTag("video_renderer_fallback_bad_network"),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier
                    .padding(12.dp)
                    .align(CenterVertically),
                imageVector = Icons.Default.SignalWifiBad,
                contentDescription = null,
                tint = VideoTheme.colors.basePrimary,
            )
            Text(
                modifier = Modifier.padding(12.dp),
                text = stringResource(
                    id = io.getstream.video.android.ui.common.R.string.stream_video_call_bad_network,
                ),
                color = VideoTheme.colors.basePrimary,
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
fun isTablet(): Boolean {
    return true
    val configuration = LocalConfiguration.current
    return if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        configuration.screenWidthDp > 840
    } else {
        configuration.screenWidthDp > 600
    }
}

@Preview
@Composable
private fun CallScreenPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        CallScreen(call = previewCall)
    }
}
