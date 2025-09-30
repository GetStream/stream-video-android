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

package io.getstream.video.android

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.SpeakerPhone
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.models.Filters
import io.getstream.chat.android.models.querysort.QuerySortByField
import io.getstream.result.onSuccessSuspend
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.ComposeStreamCallActivity
import io.getstream.video.android.compose.ui.StreamCallActivityComposeDelegate
import io.getstream.video.android.compose.ui.components.call.activecall.AudioOnlyCallContent
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.MemberState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.audio.StreamAudioDevice
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.datastore.delegate.StreamUserDataStore
import io.getstream.video.android.ui.call.CallScreen
import io.getstream.video.android.ui.common.StreamActivityUiDelegate
import io.getstream.video.android.ui.common.StreamCallActivity
import io.getstream.video.android.ui.common.StreamCallActivityConfiguration
import io.getstream.video.android.ui.common.util.StreamCallActivityDelicateApi
import io.getstream.video.android.util.FullScreenCircleProgressBar
import io.getstream.video.android.util.StreamVideoInitHelper
import kotlinx.coroutines.runBlocking

@OptIn(StreamCallActivityDelicateApi::class)
class CallActivity : ComposeStreamCallActivity() {

    override val uiDelegate: StreamActivityUiDelegate<StreamCallActivity> = StreamDemoUiDelegate()

    /**
     * This code is required to pass the UI-tests (as it hardcodes the configuration)
     * Later, improve the UI-tests
     */
    override fun loadConfigFromIntent(intent: Intent?): StreamCallActivityConfiguration {
        return super.loadConfigFromIntent(intent)
            .copy(closeScreenOnCallEnded = false, canSkipPermissionRationale = false)
    }

    @StreamCallActivityDelicateApi
    override fun onPreCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        runBlocking {
            if (!StreamVideo.isInstalled) {
                runBlocking { StreamVideoInitHelper.reloadSdk(StreamUserDataStore.instance()) }
            }
        }
        super.onPreCreate(savedInstanceState, persistentState)
    }

    private class StreamDemoUiDelegate : StreamCallActivityComposeDelegate() {

        @Composable
        override fun StreamCallActivity.OutgoingCallContent(
            modifier: Modifier,
            call: Call,
            isVideoType: Boolean,
            isShowingHeader: Boolean,
            headerContent:
            @Composable()
            (ColumnScope.() -> Unit)?,
            detailsContent:
            @Composable()
            (
                ColumnScope.(participants: List<MemberState>, topPadding: Dp) -> Unit
            )?,
            controlsContent:
            @Composable()
            (BoxScope.() -> Unit)?,
            onBackPressed: () -> Unit,
            onCallAction: (CallAction) -> Unit,
        ) {
            val isCameraEnabled by if (LocalInspectionMode.current) {
                remember { mutableStateOf(true) }
            } else {
                call.camera.isEnabled.collectAsStateWithLifecycle()
            }
            val isMicrophoneEnabled by if (LocalInspectionMode.current) {
                remember { mutableStateOf(true) }
            } else {
                call.microphone.isEnabled.collectAsStateWithLifecycle()
            }

            val selectedMicroPhoneDevice by call.microphone.selectedDevice.collectAsStateWithLifecycle()
            val availableDevices by call.microphone.devices.collectAsStateWithLifecycle()
            val audioDeviceUiStateList: List<AudioDeviceUiState> = availableDevices.map {
                val icon = when (it) {
                    is StreamAudioDevice.BluetoothHeadset -> Icons.Default.BluetoothAudio
                    is StreamAudioDevice.Earpiece -> Icons.Default.Headphones
                    is StreamAudioDevice.Speakerphone -> Icons.Default.SpeakerPhone
                    is StreamAudioDevice.WiredHeadset -> Icons.Default.HeadsetMic
                }
                AudioDeviceUiState(
                    it,
                    it.name,
                    icon,
                    it.name == selectedMicroPhoneDevice?.name,
                )
            }

            OutgoingCallContent(
                call = call,
                isVideoType = isVideoType,
                modifier = modifier,
                isShowingHeader = isShowingHeader,
                headerContent = headerContent,
                detailsContent = detailsContent,
                onBackPressed = onBackPressed,
                onCallAction = onCallAction,
                controlsContent = controlsContent ?: {
                    OutgoingCallControlsV2(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = VideoTheme.dimens.genericXxl),
                        isVideoCall = isVideoType,
                        isCameraEnabled = isCameraEnabled,
                        isMicrophoneEnabled = isMicrophoneEnabled,
                        audioDeviceUiStateList = audioDeviceUiStateList,
                        call = call,
                        onCallAction = onCallAction,
                    )
                },
            )
        }

        @Composable
        override fun StreamCallActivity.LoadingContent(call: Call) {
            // Use as loading screen.. so the layout is shown.
            FullScreenCircleProgressBar(text = "Connecting...")
        }

        @Composable
        override fun StreamCallActivity.CallDisconnectedContent(call: Call) {
            goBackToMainScreen()
        }

        @Composable
        override fun StreamCallActivity.VideoCallContent(call: Call) {
            CallScreen(
                call = call,
                showDebugOptions = BuildConfig.DEBUG,
                onCallDisconnected = {
                    leave(call)
                    goBackToMainScreen()
                },
                onUserLeaveCall = {
                    leave(call)
                    goBackToMainScreen()
                },
            )

            // step 4 (optional) - chat integration
            val user by ChatClient.instance().clientState.user.collectAsState(initial = null)
            LaunchedEffect(key1 = user) {
                if (user != null) {
                    val channel = ChatClient.instance().channel("videocall", call.id)
                    channel.queryMembers(
                        offset = 0,
                        limit = 10,
                        filter = Filters.neutral(),
                        sort = QuerySortByField(),
                    ).await().onSuccessSuspend { members ->
                        if (members.isNotEmpty()) {
                            channel.addMembers(listOf(user!!.id)).await()
                        } else {
                            channel.create(listOf(user!!.id), emptyMap()).await()
                        }
                    }
                }
            }
        }

        @Composable
        override fun StreamCallActivity.AudioCallContent(call: Call) {
            val micEnabled by call.microphone.isEnabled.collectAsStateWithLifecycle()

            AudioOnlyCallContent(
                call = call,
                isMicrophoneEnabled = micEnabled,
                onCallAction = { onCallAction(call, it) },
                onBackPressed = { onBackPressed(call) },
                controlsContent = {
                    OutgoingCallControlsRoot(
                        call,
                        micEnabled,
                    ) { onCallAction(call, it) }
                },
            )
        }

        private fun StreamCallActivity.goBackToMainScreen() {
            if (!isFinishing) {
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                safeFinish()
            }
        }
    }
}
