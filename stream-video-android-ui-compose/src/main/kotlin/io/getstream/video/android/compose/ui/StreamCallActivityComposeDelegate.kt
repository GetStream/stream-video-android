package io.getstream.android.sample.audiocall.sample.compose

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.log.taggedLogger
import io.getstream.video.android.compose.permission.LaunchPermissionRequest
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.base.StreamDialogPositiveNegative
import io.getstream.video.android.compose.ui.components.base.styling.ButtonStyles
import io.getstream.video.android.compose.ui.components.base.styling.StreamDialogStyles
import io.getstream.video.android.compose.ui.components.base.styling.StyleSize
import io.getstream.video.android.compose.ui.components.call.activecall.CallContent
import io.getstream.video.android.compose.ui.components.call.ringing.RingingCallContent
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.call.state.DeclineCall
import io.getstream.video.android.core.call.state.LeaveCall
import io.getstream.video.android.ui.common.StreamActivityUiDelegate
import io.getstream.video.android.ui.common.StreamCallActivity

/**
 * A default implementation of the compose delegate for the call activity.
 * Can be extended.
 * Provides functions with the context of the activity.
 */
public open class StreamCallActivityComposeDelegate : StreamActivityUiDelegate<StreamCallActivity> {

    public companion object {
        private val logger by taggedLogger("StreamCallActivityUiDelegate")
    }

    /**
     * Create the delegate.
     *
     * @param activity the activity
     * @param call the call
     */
    override fun setContent(activity: StreamCallActivity, call: Call) {
        logger.d { "[onCreate(activity, call)] invoked from compose delegate." }
        activity.setContent {
            logger.d { "[setContent] with RootContent" }
            activity.RootContent(call = call)
        }
    }

    /**
     * Root content of the screen.
     *
     * @param call the call object.
     */
    @Composable
    public open fun StreamCallActivity.RootContent(call: Call) {
        VideoTheme {
            LaunchPermissionRequest(listOf(Manifest.permission.RECORD_AUDIO)) {
                AllPermissionsGranted {
                    // All permissions granted
                    val connection by call.state.connection.collectAsStateWithLifecycle()
                    LaunchedEffect(key1 = connection) {
                        if (connection == RealtimeConnection.Disconnected) {
                            logger.w { "Call disconnected." }
                            finish()
                        } else if (connection is RealtimeConnection.Failed) {
                            // Safely cast, no need to crash if the error message is missing
                            val conn = connection as? RealtimeConnection.Failed
                            logger.e(Exception("${conn?.error}")) { "Call connection failed." }
                            finish()
                        }
                    }

                    RingingCallContent(
                        isVideoType = isVideoCall(call),
                        call = call,
                        modifier = Modifier.background(color = VideoTheme.colors.baseSheetPrimary),
                        onBackPressed = {
                            onBackPressed(call)
                        },
                        onAcceptedContent = {
                            if (isVideoCall(call)) {
                                DefaultCallContent(call = call)
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
                    )
                }

                SomeGranted { granted, notGranted, showRationale ->
                    // Some of the permissions were granted, you can check which ones.
                    if (showRationale) {
                        NoPermissions(granted, notGranted, true)
                    } else {
                        logger.w { "No permission, closing activity without rationale! [notGranted: [$notGranted]" }
                        finish()
                    }
                }
                NoneGranted {
                    // None of the permissions were granted.
                    if (it) {
                        NoPermissions(showRationale = true)
                    } else {
                        logger.w { "No permission, closing activity without rationale!" }
                        finish()
                    }
                }
            }
        }
    }

    /**
     * Content when the call is not answered.
     *
     * @param call the call.
     */
    @Composable
    public open fun StreamCallActivity.NoAnswerContent(call: Call) {
        onCallAction(call, LeaveCall)
    }

    /**
     * Content when the call is rejected.
     *
     * @param call the call.
     */
    @Composable
    public open fun StreamCallActivity.RejectedContent(call: Call) {
        onCallAction(call, DeclineCall)
    }

    /**
     * Content for audio calls.
     * Call type must be "audio_call"
     *
     * @param call the call.
     */
    @Composable
    public open fun StreamCallActivity.AudioCallContent(call: Call) {
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
            durationPlaceholder = duration ?: "Calling...",
        )
    }

    /**
     * Content for all other calls.
     *
     * @param call the call.
     */
    @Composable
    public open fun StreamCallActivity.DefaultCallContent(call: Call) {
        CallContent(call = call, onCallAction = {
            onCallAction(call, it)
        }, onBackPressed = {
            onBackPressed(call)
        })
    }

    /**
     * Content when permissions are missing.
     */
    @Composable
    public open fun StreamCallActivity.NoPermissions(
        granted: List<String> = emptyList(),
        notGranted: List<String> = emptyList(),
        showRationale: Boolean
    ) {
        StreamDialogPositiveNegative(
            content = {
                Text(
                    text = "Some permissions are required",
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
                    text = "The app needs access to your microphone.",
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
                "Settings",
                ButtonStyles.secondaryButtonStyle(StyleSize.S),
            ) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            },
            negativeButton = Triple(
                "Not now",
                ButtonStyles.tertiaryButtonStyle(StyleSize.S),
            ) {
                finish()
            },
        )
    }
}