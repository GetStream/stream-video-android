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

package io.getstream.video.android.dogfooding.ui.join

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.avatar.UserAvatar
import io.getstream.video.android.datastore.delegate.StreamUserDataStore
import io.getstream.video.android.dogfooding.R
import io.getstream.video.android.dogfooding.ui.theme.Colors
import io.getstream.video.android.dogfooding.ui.theme.StreamButton
import io.getstream.video.android.mock.StreamMockUtils
import io.getstream.video.android.mock.mockUsers

@Composable
fun CallJoinScreen(
    callJoinViewModel: CallJoinViewModel = hiltViewModel(),
    navigateToCallLobby: (callId: String) -> Unit,
    navigateUpToLogin: () -> Unit
) {
    val uiState by callJoinViewModel.uiState.collectAsState()

    HandleCallJoinUiState(
        callJoinUiState = uiState,
        navigateToCallLobby = navigateToCallLobby,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Colors.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CallJoinHeader(
            navigateUpToLogin = navigateUpToLogin,
            callJoinViewModel = callJoinViewModel
        )

        CallJoinBody(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth()
                .weight(1f),
            callJoinViewModel = callJoinViewModel
        )
    }
}

@Composable
private fun CallJoinHeader(
    callJoinViewModel: CallJoinViewModel = hiltViewModel(),
    navigateUpToLogin: () -> Unit
) {
    val user by callJoinViewModel.user.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            color = Color.White,
            text = user?.name?.ifBlank { user?.id }?.ifBlank { user!!.custom["email"] }.orEmpty(),
            maxLines = 1,
            fontSize = 16.sp
        )

        StreamButton(
            modifier = Modifier.width(125.dp),
            text = stringResource(id = R.string.sign_out),
            onClick = {
                callJoinViewModel.signOut()
                navigateUpToLogin.invoke()
            }
        )
    }
}

@Composable
private fun CallJoinBody(
    modifier: Modifier,
    callJoinViewModel: CallJoinViewModel = hiltViewModel(),
) {
    val user by if (LocalInspectionMode.current) {
        remember { mutableStateOf(mockUsers[0]) }
    } else {
        callJoinViewModel.user.collectAsState()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Colors.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (user != null) {
            val name =
                user?.name?.ifBlank { user?.id }?.ifBlank { user!!.custom["email"] }.orEmpty()
            UserAvatar(
                modifier = Modifier.size(100.dp),
                user = user!!,
            )

            Spacer(modifier = Modifier.height(25.dp))

            Text(
                modifier = Modifier.padding(horizontal = 30.dp),
                text = "Welcome, $name",
                color = Color.White,
                fontSize = 32.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(id = R.string.join_description),
            color = Colors.description,
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
        )

        Spacer(modifier = Modifier.height(42.dp))

        StreamButton(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 35.dp)
                .testTag("start_new_call"),
            text = stringResource(id = R.string.start_a_new_call),
            onClick = { callJoinViewModel.handleUiEvent(CallJoinEvent.JoinCall()) }
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 35.dp),
            text = stringResource(id = R.string.call_id_number),
            color = Color(0xFF979797),
            fontSize = 13.sp,
        )

        Spacer(modifier = Modifier.height(4.dp))

        var callId by remember { mutableStateOf("default:79cYh3J5JgGk") }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(horizontal = 35.dp),
        ) {
            TextField(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .border(
                        BorderStroke(1.dp, Color(0xFF4C525C)),
                        RoundedCornerShape(6.dp)
                    ),
                shape = RoundedCornerShape(6.dp),
                value = callId,
                onValueChange = { callId = it },
                colors = TextFieldDefaults.textFieldColors(
                    textColor = Color.White,
                    focusedLabelColor = VideoTheme.colors.primaryAccent,
                    unfocusedIndicatorColor = Colors.secondBackground,
                    focusedIndicatorColor = Colors.secondBackground,
                    backgroundColor = Colors.secondBackground
                ),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Email
                )
            )

            StreamButton(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .fillMaxHeight()
                    .testTag("join_call"),
                onClick = {
                    callJoinViewModel.handleUiEvent(CallJoinEvent.JoinCall(callId = callId))
                },
                text = stringResource(id = R.string.join_call)
            )
        }
    }
}

@Composable
private fun HandleCallJoinUiState(
    callJoinUiState: CallJoinUiState,
    navigateToCallLobby: (callId: String) -> Unit,
) {
    LaunchedEffect(key1 = callJoinUiState) {
        when (callJoinUiState) {
            is CallJoinUiState.JoinCompleted ->
                navigateToCallLobby.invoke(callJoinUiState.callId)

            else -> Unit
        }
    }
}

@Preview
@Composable
private fun CallJoinScreenPreview() {
    StreamMockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        CallJoinScreen(
            callJoinViewModel = CallJoinViewModel(StreamUserDataStore.instance()),
            navigateToCallLobby = {},
            navigateUpToLogin = {}
        )
    }
}
