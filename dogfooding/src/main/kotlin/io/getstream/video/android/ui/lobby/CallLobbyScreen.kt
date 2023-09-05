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

@file:OptIn(ExperimentalComposeUiApi::class)

package io.getstream.video.android.ui.lobby

import android.content.Intent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import io.getstream.video.android.BuildConfig
import io.getstream.video.android.R
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.avatar.UserAvatar
import io.getstream.video.android.compose.ui.components.call.lobby.CallLobby
import io.getstream.video.android.core.call.state.ToggleCamera
import io.getstream.video.android.core.call.state.ToggleMicrophone
import io.getstream.video.android.datastore.delegate.StreamUserDataStore
import io.getstream.video.android.mock.StreamMockUtils
import io.getstream.video.android.ui.call.CallActivity
import io.getstream.video.android.ui.theme.Colors
import io.getstream.video.android.ui.theme.StreamButton
import kotlinx.coroutines.delay

@Composable
fun CallLobbyScreen(
    callLobbyViewModel: CallLobbyViewModel = hiltViewModel(),
    navigateUpToLogin: () -> Unit,
) {
    val isLoading by callLobbyViewModel.isLoading.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Colors.background)
                .testTag("call_lobby"),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CallLobbyHeader(
                navigateUpToLogin = navigateUpToLogin,
                callLobbyViewModel = callLobbyViewModel,
            )

            CallLobbyBody(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth()
                    .weight(1f),
                callLobbyViewModel = callLobbyViewModel,
            )
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = VideoTheme.colors.primaryAccent,
            )
        }
    }
}

@Composable
private fun CallLobbyHeader(
    callLobbyViewModel: CallLobbyViewModel = hiltViewModel(),
    navigateUpToLogin: () -> Unit,
) {
    val uiState by callLobbyViewModel.uiState.collectAsState(initial = CallLobbyUiState.Nothing)
    val isLoggedOut by callLobbyViewModel.isLoggedOut.collectAsState(initial = false)
    val user = callLobbyViewModel.user.collectAsState(initial = null)

    HandleCallLobbyUiState(
        callLobbyUiState = uiState,
        callLobbyViewModel = callLobbyViewModel,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val userValue = user.value
        if (userValue != null) {
            UserAvatar(
                modifier = Modifier.size(32.dp),
                user = userValue,
            )

            Spacer(modifier = Modifier.width(4.dp))
        }

        Text(
            modifier = Modifier.weight(1f),
            color = Color.White,
            text = userValue?.id.orEmpty(),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            fontSize = 16.sp,
        )

        if (BuildConfig.FLAVOR == "dogfooding") {
            Spacer(modifier = Modifier.width(4.dp))

            StreamButton(
                modifier = Modifier.width(125.dp),
                text = stringResource(id = R.string.sign_out),
                onClick = { callLobbyViewModel.signOut() },
            )
        }
    }

    LaunchedEffect(key1 = isLoggedOut) {
        if (isLoggedOut) {
            navigateUpToLogin.invoke()
        }
    }
}

@Composable
private fun CallLobbyBody(
    modifier: Modifier,
    callLobbyViewModel: CallLobbyViewModel = hiltViewModel(),
) {
    val call by remember { mutableStateOf(callLobbyViewModel.call) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Colors.background)
            .semantics { testTagsAsResourceId = true },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 30.dp),
            text = stringResource(id = R.string.stream_video),
            color = Color.White,
            fontSize = 26.sp,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            modifier = Modifier.padding(horizontal = 30.dp),
            text = stringResource(id = R.string.call_lobby_description),
            color = Colors.description,
            textAlign = TextAlign.Center,
            fontSize = 17.sp,
        )

        Spacer(modifier = Modifier.height(20.dp))

        val isCameraEnabled: Boolean by if (LocalInspectionMode.current) {
            remember { mutableStateOf(true) }
        } else {
            callLobbyViewModel.cameraEnabled.collectAsState(initial = false)
        }

        val isMicrophoneEnabled by if (LocalInspectionMode.current) {
            remember { mutableStateOf(true) }
        } else {
            callLobbyViewModel.microphoneEnabled.collectAsState(initial = false)
        }

        // turn on camera and microphone by default
        LaunchedEffect(key1 = Unit) {
            delay(300)
            if (BuildConfig.BENCHMARK.toBoolean()) {
                callLobbyViewModel.call.camera.disable()
                callLobbyViewModel.call.microphone.disable()
            }
        }

        CallLobby(
            call = call,
            modifier = Modifier.fillMaxWidth(),
            isCameraEnabled = isCameraEnabled,
            isMicrophoneEnabled = isMicrophoneEnabled,
            onCallAction = { action ->
                when (action) {
                    is ToggleCamera -> callLobbyViewModel.enableCamera(action.isEnabled)
                    is ToggleMicrophone -> callLobbyViewModel.enableMicrophone(action.isEnabled)
                    else -> Unit
                }
            },
        )

        LobbyDescription(callLobbyViewModel = callLobbyViewModel)
    }
}

@Composable
private fun LobbyDescription(
    callLobbyViewModel: CallLobbyViewModel,
) {
    val session by callLobbyViewModel.call.state.session.collectAsState()

    Column(
        modifier = Modifier
            .padding(horizontal = 35.dp)
            .background(
                color = VideoTheme.colors.callLobbyBackground,
                shape = RoundedCornerShape(16.dp),
            ),
    ) {
        Text(
            modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 12.dp, bottom = 8.dp),
            text = stringResource(
                id = R.string.join_call_description,
                session?.participants?.size ?: 0,
            ),
            color = Color.White,
        )

        StreamButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .testTag("start_call"),
            text = stringResource(id = R.string.join_call),
            onClick = {
                callLobbyViewModel.handleUiEvent(
                    CallLobbyEvent.JoinCall,
                )
            },
        )
    }
}

@Composable
private fun HandleCallLobbyUiState(
    callLobbyUiState: CallLobbyUiState,
    callLobbyViewModel: CallLobbyViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    LaunchedEffect(key1 = callLobbyUiState) {
        when (callLobbyUiState) {
            is CallLobbyUiState.JoinCompleted -> {
                val intent = CallActivity.createIntent(
                    context = context,
                    callId = callLobbyViewModel.callId,
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                context.startActivity(intent)
            }

            is CallLobbyUiState.JoinFailed -> {
                Toast.makeText(context, callLobbyUiState.reason, Toast.LENGTH_SHORT).show()
            }

            else -> Unit
        }
    }
}

@Preview
@Composable
private fun CallLobbyScreenPreview() {
    StreamMockUtils.initializeStreamVideo(LocalContext.current)
    StreamUserDataStore.install(LocalContext.current)
    VideoTheme {
        CallLobbyScreen(
            callLobbyViewModel = CallLobbyViewModel(
                savedStateHandle = SavedStateHandle(
                    mapOf("cid" to "default:123"),
                ),
                dataStore = StreamUserDataStore.instance(),
            ),
        ) {}
    }
}
