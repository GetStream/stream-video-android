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

package io.getstream.video.android.compose.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SignalWifiBad
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.android.video.generated.models.OwnCapability
import io.getstream.log.taggedLogger
import io.getstream.video.android.compose.R
import io.getstream.video.android.compose.permission.LaunchPermissionRequest
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.base.StreamDialogPositiveNegative
import io.getstream.video.android.compose.ui.components.base.styling.ButtonStyles
import io.getstream.video.android.compose.ui.components.base.styling.StreamDialogStyles
import io.getstream.video.android.compose.ui.components.base.styling.StyleSize
import io.getstream.video.android.compose.ui.components.call.CallAppBar
import io.getstream.video.android.compose.ui.components.call.activecall.CallContent
import io.getstream.video.android.compose.ui.components.call.ringing.RingingCallContent
import io.getstream.video.android.compose.ui.components.livestream.LivestreamPlayer
import io.getstream.video.android.compose.ui.components.video.config.videoRenderConfig
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.MemberState
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.call.CallType
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.CancelCall
import io.getstream.video.android.core.call.state.CustomAction
import io.getstream.video.android.core.call.state.DeclineCall
import io.getstream.video.android.core.call.state.LeaveCall
import io.getstream.video.android.ui.common.StreamCallActivity
import io.getstream.video.android.ui.common.util.StreamCallActivityDelicateApi

/**
 * A default implementation of the compose delegate for the call activity.
 * Can be extended.
 * Provides functions with the context of the activity.
 */
// We suppress build fails since we do not have default parameters in our abstract @Composable
// Remove the @Suppress line once the listed issue is fixed.
// https://issuetracker.google.com/issues/322121224
@OptIn(StreamCallActivityDelicateApi::class)
@Suppress("ABSTRACT_COMPOSABLE_DEFAULT_PARAMETER_VALUE")
public open class StreamCallActivityComposeDelegate : StreamCallActivityComposeUi {

    public companion object {
        private val logger by taggedLogger("StreamCallActivityComposeDelegate")
    }

    /**
     * Shows a progressbar until everything is set.
     */
    @StreamCallActivityDelicateApi
    override fun loadingContent(activity: StreamCallActivity) {
        // Nothing to set, by default.
    }

    /**
     * Create the delegate.
     *
     * @param activity the activity
     * @param call the call
     */
    override fun setContent(activity: StreamCallActivity, call: Call) {
        logger.d { "[setContent(activity, call)] invoked from compose delegate." }
        activity.setContent {
            VideoTheme {
                Box(
                    modifier = Modifier
                        .background(VideoTheme.colors.baseSheetPrimary)
                        .systemBarsPadding(),
                ) {
                    logger.d { "[setContent] with RootContent" }
                    activity.RootContent(call = call)
                }
            }
        }
    }

    @StreamCallActivityDelicateApi
    @Composable
    override fun StreamCallActivity.RootContent(call: Call) {
        if (call.type == CallType.Livestream.name) {
            LaunchPermissionRequest(getRequiredPermissions(call)) {
                AllPermissionsGranted {
                    Box {
                        LivestreamPlayer(
                            call = call,
                            videoRendererConfig =
                            videoRenderConfig {
                                this.fallbackContent = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                color = VideoTheme.colors.baseSheetPrimary,
                                            ),
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .align(Alignment.Center),
                                            color = VideoTheme.colors.basePrimary,
                                        )
                                    }
                                }
                                this.badNetworkContent = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize(),
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    color = VideoTheme.colors.baseSheetPrimary,
                                                )
                                                .align(Alignment.Center),
                                            horizontalAlignment = CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                        ) {
                                            Icon(
                                                tint = VideoTheme.colors.basePrimary,
                                                imageVector = Icons.Default.SignalWifiBad,
                                                contentDescription = null,
                                            )
                                            Text(
                                                color = VideoTheme.colors.basePrimary,
                                                modifier = Modifier.padding(top = 16.dp),
                                                text = getString(io.getstream.video.android.ui.common.R.string.stream_video_call_bad_network_single_video),
                                            )
                                        }
                                    }
                                }
                            },
                        )
                        CallAppBar(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(end = 16.dp, top = 16.dp),
                            call = call,
                            centerContent = { },
                            onCallAction = {
                                call.leave()
                                safeFinish()
                            },
                        )
                    }
                }
            }
        } else {
            var callAction: CallAction by remember {
                mutableStateOf(CustomAction(tag = "initial"))
            }

            when (callAction) {
                is LeaveCall, is DeclineCall, is CancelCall -> {
                    CallDisconnectedContent(call)
                }

                else -> {
                    LaunchPermissionRequest(getRequiredPermissions(call)) {
                        AllPermissionsGranted {
                            // All permissions granted
                            RingingCallContent(
                                isVideoType = isVideoCall(call),
                                call = call,
                                modifier = Modifier.background(
                                    color = VideoTheme.colors.baseSheetPrimary,
                                ),
                                onBackPressed = {
                                    onBackPressed(call)
                                },
                                onOutgoingContent = {
                                        modifier: Modifier,
                                        call: Call,
                                        isVideoType: Boolean,
                                        isShowingHeader: Boolean,
                                        headerContent: @Composable (ColumnScope.() -> Unit)?,
                                        detailsContent: @Composable (
                                            ColumnScope.(
                                                participants: List<MemberState>,
                                                topPadding: Dp,
                                            ) -> Unit
                                        )?,
                                        controlsContent: @Composable (BoxScope.() -> Unit)?,
                                        onBackPressed: () -> Unit,
                                        onCallAction: (CallAction) -> Unit,
                                    ->
                                    OutgoingCallContent(
                                        call = call,
                                        isVideoType = isVideoType,
                                        modifier = modifier,
                                        isShowingHeader = isShowingHeader,
                                        headerContent = headerContent,
                                        detailsContent = detailsContent,
                                        controlsContent = controlsContent,
                                        onBackPressed = onBackPressed,
                                        onCallAction = onCallAction,
                                    )
                                },
                                onIncomingContent = {
                                        modifier: Modifier,
                                        call: Call,
                                        isVideoType: Boolean, isShowingHeader: Boolean,
                                        headerContent: @Composable (ColumnScope.() -> Unit)?,
                                        detailsContent: @Composable (
                                            ColumnScope.(
                                                participants: List<MemberState>,
                                                topPadding: Dp,
                                            ) -> Unit
                                        )?,
                                        controlsContent: @Composable (BoxScope.() -> Unit)?,
                                        onBackPressed: () -> Unit,
                                        onCallAction: (CallAction) -> Unit,
                                    ->
                                    IncomingCallContent(
                                        call = call,
                                        isVideoType = isVideoType,
                                        modifier = modifier,
                                        isShowingHeader = isShowingHeader,
                                        headerContent = headerContent,
                                        detailsContent = detailsContent,
                                        controlsContent = controlsContent,
                                        onBackPressed = onBackPressed,
                                        onCallAction = onCallAction,
                                    )
                                },
                                onAcceptedContent = {
                                    ConnectionAvailable(call = call) { theCall ->
                                        if (isVideoCall(theCall)) {
                                            VideoCallContent(call = theCall)
                                        } else {
                                            AudioCallContent(call = theCall)
                                        }
                                    }
                                },
                                onNoAnswerContent = {
                                    NoAnswerContent(call)
                                },
                                onRejectedContent = {
                                    RejectedContent(call)
                                },
                                onCallAction = {
                                    onCallAction(call, it)
                                    callAction = it
                                },
                                onIdle = {
                                    LoadingContent(call)
                                },
                            )
                        }

                        SomeGranted { granted, notGranted, showRationale ->
                            InternalPermissionContent(showRationale, call, granted, notGranted)
                        }

                        NoneGranted {
                            InternalPermissionContent(it, call, emptyList(), emptyList())
                        }
                    }
                }
            }

            /**
             * Call can be rejected by [RejectCallBroadcastReceiver] so the activity
             * needs to observe [Call.state.rejectedBy]
             */
            val rejectedBy by call.state.rejectedBy.collectAsStateWithLifecycle()
            LaunchedEffect(rejectedBy) {
                val currentUserId = StreamVideo.instanceOrNull()?.userId
                if (rejectedBy.contains(currentUserId)) {
                    // check if there is no ongoing call then safely finish it else do nothing
                    val noActiveCall = (StreamVideo.instanceOrNull()?.state?.activeCall?.value == null)
                    if (noActiveCall) {
                        onEnded(call)
                        val configuration = configurationMap[call.id]
                        if (configuration?.closeScreenOnCallEnded == true) {
                            safeFinish()
                        }
                        else {
                            logger.d { "Don't close activity as some other call is active" }
                        }
                    }
                }
            }
        }
    }

    private fun getRequiredPermissions(call: Call): List<String> {
        return mutableListOf<String>().apply {
            if (call.state.ownCapabilities.value.contains(OwnCapability.SendAudio)) {
                add(Manifest.permission.RECORD_AUDIO)
            }
            if (call.state.ownCapabilities.value.contains(OwnCapability.SendVideo)) {
                add(Manifest.permission.CAMERA)
            }
        }
    }

    @Composable
    private fun StreamCallActivity.InternalPermissionContent(
        showRationale: Boolean,
        call: Call,
        granted: List<String>,
        notGranted: List<String>,
    ) {
        if (!showRationale && configuration.canSkipPermissionRationale) {
            logger.w { "Permissions were not granted, but rationale is required to be skipped." }
            safeFinish()
        } else {
            PermissionsRationaleContent(call, granted, notGranted)
        }
    }

    @Composable
    override fun StreamCallActivity.LoadingContent(call: Call) {
        // No loading screen by default...
    }

    @Composable
    private fun StreamCallActivity.ConnectionAvailable(
        call: Call,
        content: @Composable (call: Call) -> Unit,
    ) {
        val connection by call.state.connection.collectAsStateWithLifecycle()
        when (connection) {
            RealtimeConnection.Disconnected -> {
                if (!configuration.closeScreenOnCallEnded) {
                    CallDisconnectedContent(call)
                } else {
                    // This is just for safety, will be called from other place as well.
                    safeFinish()
                }
            }

            is RealtimeConnection.Failed -> {
                if (!configuration.closeScreenOnError) {
                    val err = Exception("${(connection as? RealtimeConnection.Failed)?.error}")
                    CallFailedContent(call, err)
                } else {
                    // This is just for safety, will be called from other place as well.
                    safeFinish()
                }
            }

            else -> {
                content.invoke(call)
            }
        }
    }

    @Composable
    override fun StreamCallActivity.AudioCallContent(call: Call) {
        val micEnabled by call.microphone.isEnabled.collectAsStateWithLifecycle()

        io.getstream.video.android.compose.ui.components.call.activecall.AudioCallContent(
            call = call,
            isMicrophoneEnabled = micEnabled,
            onCallAction = { onCallAction(call, it) },
            onBackPressed = { onBackPressed(call) },
        )
    }

    @Composable
    override fun StreamCallActivity.VideoCallContent(call: Call) {
        CallContent(call = call, onCallAction = {
            onCallAction(call, it)
        }, onBackPressed = {
            onBackPressed(call)
        })
    }

    @Composable
    override fun StreamCallActivity.OutgoingCallContent(
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
    ) {
        io.getstream.video.android.compose.ui.components.call.ringing.outgoingcall.OutgoingCallContent(
            call = call,
            isVideoType = isVideoType,
            modifier = modifier,
            isShowingHeader = isShowingHeader,
            headerContent = headerContent,
            detailsContent = detailsContent,
            controlsContent = controlsContent,
            onBackPressed = onBackPressed,
            onCallAction = onCallAction,
        )
    }

    @Composable
    override fun StreamCallActivity.IncomingCallContent(
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
    ) {
        io.getstream.video.android.compose.ui.components.call.ringing.incomingcall.IncomingCallContent(
            call = call,
            isVideoType = isVideoType,
            modifier = modifier,
            isShowingHeader = isShowingHeader,
            headerContent = headerContent,
            detailsContent = detailsContent,
            controlsContent = controlsContent,
            onBackPressed = onBackPressed,
            onCallAction = onCallAction,
        )
    }

    @Composable
    override fun StreamCallActivity.NoAnswerContent(call: Call) {
        // There is not default UI for no-answer content.
        CallDisconnectedContent(call = call)
    }

    @Composable
    override fun StreamCallActivity.RejectedContent(call: Call) {
        // There is not default UI for rejected content.
        CallDisconnectedContent(call = call)
    }

    @Composable
    override fun StreamCallActivity.CallFailedContent(call: Call, exception: java.lang.Exception) {
        // By default we finish the activity regardless of config.
        // There is not default UI for call failed content.
        safeFinish()
    }

    @Composable
    override fun StreamCallActivity.CallDisconnectedContent(call: Call) {
        // By default we finish the activity regardless of config.
        // There is not default UI for call ended content.
        safeFinish()
    }

    @Composable
    override fun StreamCallActivity.PermissionsRationaleContent(
        call: Call,
        granted: List<String>,
        notGranted: List<String>,
    ) {
        // Show default dialog to go to settings.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VideoTheme.colors.baseSheetSecondary),
        ) {
            // Proceed as normal
            StreamDialogPositiveNegative(
                content = {
                    Text(
                        text = stringResource(
                            id = R.string.stream_default_call_ui_permissions_rationale_title,
                        ),
                        style = TextStyle(
                            fontSize = 24.sp,
                            lineHeight = 28.sp,
                            fontWeight = FontWeight(500),
                            color = VideoTheme.colors.basePrimary,
                            textAlign = TextAlign.Center,
                        ),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = stringResource(
                            id = R.string.stream_default_call_ui_permission_rationale,
                        ),
                        style = TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 18.5.sp,
                            fontWeight = FontWeight(400),
                            color = VideoTheme.colors.baseSecondary,
                            textAlign = TextAlign.Center,
                        ),
                    )
                },
                style = StreamDialogStyles.defaultDialogStyle(),
                positiveButton = Triple(
                    stringResource(id = R.string.stream_default_call_ui_settings_button),
                    ButtonStyles.secondaryButtonStyle(StyleSize.S),
                ) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                    startActivity(intent)
                },
                negativeButton = Triple(
                    stringResource(id = R.string.stream_default_call_ui_not_now_button),
                    ButtonStyles.tertiaryButtonStyle(StyleSize.S),
                ) {
                    // No permissions, leave the call
                    onCallAction(call, LeaveCall)
                },
            )
        }
    }
}
