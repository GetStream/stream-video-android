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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.log.taggedLogger
import io.getstream.video.android.compose.R
import io.getstream.video.android.compose.permission.LaunchPermissionRequest
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.base.StreamDialogPositiveNegative
import io.getstream.video.android.compose.ui.components.base.styling.ButtonStyles
import io.getstream.video.android.compose.ui.components.base.styling.StreamDialogStyles
import io.getstream.video.android.compose.ui.components.base.styling.StyleSize
import io.getstream.video.android.compose.ui.components.call.activecall.CallContent
import io.getstream.video.android.compose.ui.components.call.ringing.RingingCallContent
import io.getstream.video.android.compose.ui.components.call.ringing.incomingcall.IncomingCallContent
import io.getstream.video.android.compose.ui.components.call.ringing.outgoingcall.OutgoingCallContent
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.MemberState
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.DeclineCall
import io.getstream.video.android.core.call.state.LeaveCall
import io.getstream.video.android.ui.common.StreamActivityUiDelegate
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
        activity.setContent {
            VideoTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(VideoTheme.colors.baseSheetPrimary),
                ) {
                    IndeterminateProgressBar(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
            }
        }
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
            logger.d { "[setContent] with RootContent" }
            activity.RootContent(call = call)
        }
    }

    @StreamCallActivityDelicateApi
    @Composable
    override fun StreamCallActivity.RootContent(call: Call) {
        VideoTheme {
            LaunchPermissionRequest(listOf(Manifest.permission.RECORD_AUDIO)) {
                AllPermissionsGranted {
                    val configuration = configuration()
                    // All permissions granted
                    RingingCallContent(
                        isVideoType = isVideoCall(call),
                        call = call,
                        modifier = Modifier.background(color = VideoTheme.colors.baseSheetPrimary),
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
                            if (isVideoCall(call)) {
                                VideoCallContent(call = call)
                            } else {
                                AudioCallContent(call = call)
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
                        },
                        onIdle = {
                            LoadingContent(call)
                        },
                    )
                }

                SomeGranted { granted, notGranted, showRationale ->
                    PermissionsRationaleContent(call, granted, notGranted, showRationale)
                }

                NoneGranted {
                    PermissionsRationaleContent(call, emptyList(), emptyList(), it)
                }
            }
        }
    }

    @Composable
    override fun StreamCallActivity.LoadingContent(call: Call) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VideoTheme.colors.baseSheetPrimary)
        ) {
            IndeterminateProgressBar(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            )
        }
    }

    @Composable
    override fun StreamCallActivity.AudioCallContent(call: Call) {
        val micEnabled by call.microphone.isEnabled.collectAsStateWithLifecycle()
        val duration by call.state.durationInDateFormat.collectAsStateWithLifecycle()
        io.getstream.video.android.compose.ui.components.call.activecall.AudioCallContent(
            onBackPressed = {
                onBackPressed(call)
            },
            call = call,
            isMicrophoneEnabled = micEnabled,
            onCallAction = {
                onCallAction(call, it)
            },
            durationPlaceholder = duration
                ?: "...",
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
        // By default we finish the activity regardless of config.
        // There is not default UI for no-answer content.
        finish()
    }

    @Composable
    override fun StreamCallActivity.RejectedContent(call: Call) {
        // By default we finish the activity regardless of config.
        // There is not default UI for rejected content.
        finish()
    }

    @Composable
    override fun StreamCallActivity.PermissionsRationaleContent(
        call: Call,
        granted: List<String>,
        notGranted: List<String>,
        showRationale: Boolean
    ) {
        if (!showRationale) {
            logger.w { "Permissions were not granted, but rationale is required to be skipped." }
            finish()
            return
        }
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
                        id = R.string.stream_default_call_ui_microphone_rationale,
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
                finish()
            },
        )
    }

    // To not depend on the Compose progress bar due to this
    // https://issuetracker.google.com/issues/322214617
    @Composable
    private fun IndeterminateProgressBar(modifier: Modifier = Modifier) {
        val infiniteTransition = rememberInfiniteTransition(label = "")
        val translateAnim = infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ), label = ""
        )

        Box(
            modifier = modifier
                .height(4.dp)
                .fillMaxWidth()
                .background(
                    shape = CircleShape, color = VideoTheme.colors.baseSheetSecondary
                )
                .clip(CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(50.dp)
                    .background(VideoTheme.colors.brandPrimary)
                    .offset(x = (translateAnim.value * with(LocalDensity.current) { 300.dp.toPx() }).dp)
            )
        }
    }
}
