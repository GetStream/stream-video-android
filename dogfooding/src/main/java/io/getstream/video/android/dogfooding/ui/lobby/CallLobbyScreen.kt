/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

@file:OptIn(ExperimentalPermissionsApi::class, ExperimentalPermissionsApi::class)

package io.getstream.video.android.dogfooding.ui.lobby

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.avatar.UserAvatar
import io.getstream.video.android.compose.ui.components.call.lobby.CallLobby
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.call.state.ToggleCamera
import io.getstream.video.android.core.call.state.ToggleMicrophone
import io.getstream.video.android.core.call.state.ToggleSpeakerphone
import io.getstream.video.android.dogfooding.R
import io.getstream.video.android.dogfooding.ui.call.CallActivity
import io.getstream.video.android.dogfooding.ui.theme.Colors
import io.getstream.video.android.dogfooding.ui.theme.StreamButton

@Composable
fun CallLobbyScreen(
    callLobbyViewModel: CallLobbyViewModel = hiltViewModel(),
    navigateUpToLogin: () -> Unit
) {
    val isLoading by callLobbyViewModel.isLoading.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Colors.background)
                .testTag("call_lobby"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CallJoinHeader(navigateUpToLogin = navigateUpToLogin)

            CallLobbyBody(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth()
                    .weight(1f),
            )
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = VideoTheme.colors.primaryAccent
            )
        }
    }
}

@Composable
private fun CallJoinHeader(
    callLobbyViewModel: CallLobbyViewModel = hiltViewModel(),
    navigateUpToLogin: () -> Unit
) {
    val uiState by callLobbyViewModel.uiState.collectAsState()

    HandleCallLobbyUiState(callLobbyUiState = uiState)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val user = callLobbyViewModel.user
        if (user != null) {
            UserAvatar(
                modifier = Modifier.size(32.dp),
                user = user,
            )

            Spacer(modifier = Modifier.width(4.dp))
        }

        Text(
            modifier = Modifier.weight(1f),
            color = Color.White,
            text = callLobbyViewModel.user?.id.orEmpty(),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.width(4.dp))

        StreamButton(
            modifier = Modifier.width(125.dp),
            text = stringResource(id = R.string.sign_out),
            onClick = {
                callLobbyViewModel.signOut()
                navigateUpToLogin.invoke()
            }
        )
    }
}

@Composable
private fun CallLobbyBody(
    modifier: Modifier,
    callLobbyViewModel: CallLobbyViewModel = hiltViewModel()
) {
    val call by remember { mutableStateOf(callLobbyViewModel.call) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Colors.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 30.dp),
            text = stringResource(id = R.string.stream_video),
            color = Color.White,
            fontSize = 26.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(id = R.string.call_lobby_description),
            color = Colors.description,
            textAlign = TextAlign.Center,
            fontSize = 17.sp,
        )

        Spacer(modifier = Modifier.height(20.dp))

        val callDeviceState by callLobbyViewModel.deviceState.collectAsState()
        val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
        val micPermissionState =
            rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)
        val deviceState by callLobbyViewModel.deviceState.collectAsState()
        val user = StreamVideo.instance().user

        CallLobby(
            call = call,
            user = user,
            modifier = Modifier.fillMaxWidth(),
            callDeviceState = deviceState,
            onCallAction = { action ->
                when (action) {
                    is ToggleMicrophone -> {
                        micPermissionState.launchPermissionRequest()
                        callLobbyViewModel.enableMicrophone(!callDeviceState.isMicrophoneEnabled)
                    }

                    is ToggleCamera -> {
                        cameraPermissionState.launchPermissionRequest()
                        callLobbyViewModel.enableCamera(!callDeviceState.isCameraEnabled)
                    }

                    is ToggleSpeakerphone -> {
                        callLobbyViewModel.enableSpeakerphone(!callDeviceState.isSpeakerphoneEnabled)
                    }

                    else -> Unit
                }
            }
        )

        Spacer(modifier = Modifier.height(50.dp))

        StreamButton(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 35.dp)
                .testTag("start_call"),
            text = stringResource(id = R.string.start_call),
            onClick = { callLobbyViewModel.handleUiEvent(CallLobbyEvent.JoinCall) }
        )
    }
}

@Composable
private fun HandleCallLobbyUiState(
    callLobbyUiState: CallLobbyUiState,
    callLobbyViewModel: CallLobbyViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    LaunchedEffect(key1 = callLobbyUiState) {
        when (callLobbyUiState) {
            is CallLobbyUiState.JoinCompleted -> {
                val intent = CallActivity.getIntent(context, callId = callLobbyViewModel.callId)
                context.startActivity(intent)
            }

            is CallLobbyUiState.JoinFailed -> {
                Toast.makeText(context, callLobbyUiState.reason, Toast.LENGTH_SHORT).show()
            }

            else -> Unit
        }
    }
}
