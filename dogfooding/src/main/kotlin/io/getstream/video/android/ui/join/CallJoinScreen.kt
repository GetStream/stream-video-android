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

package io.getstream.video.android.ui.join

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import io.getstream.video.android.BuildConfig
import io.getstream.video.android.DeeplinkingActivity
import io.getstream.video.android.R
import io.getstream.video.android.analytics.FirebaseEvents
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.avatar.UserAvatar
import io.getstream.video.android.datastore.delegate.StreamUserDataStore
import io.getstream.video.android.mock.StreamMockUtils
import io.getstream.video.android.mock.mockUsers
import io.getstream.video.android.ui.theme.Colors
import io.getstream.video.android.ui.theme.StreamButton

@Composable
fun CallJoinScreen(
    callJoinViewModel: CallJoinViewModel = hiltViewModel(),
    navigateToCallLobby: (callId: String) -> Unit,
    navigateUpToLogin: (autoLogIn: Boolean) -> Unit,
    navigateToDirectCallJoin: () -> Unit,
) {
    val uiState by callJoinViewModel.uiState.collectAsState(CallJoinUiState.Nothing)
    val isLoggedOut by callJoinViewModel.isLoggedOut.collectAsState(initial = false)
    val qrCodeCallback = rememberQrCodeCallback()
    val context = LocalContext.current

    HandleCallJoinUiState(
        callJoinUiState = uiState,
        navigateToCallLobby = navigateToCallLobby,
        navigateUpToLogin = { navigateUpToLogin(true) },
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Colors.background),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CallJoinHeader(
            onAvatarLongClick = {
                callJoinViewModel.autoLogInAfterLogOut = false
                callJoinViewModel.logOut()
            },
            callJoinViewModel = callJoinViewModel,
            onDirectCallClick = navigateToDirectCallJoin,
            onSignOutClick = {
                callJoinViewModel.autoLogInAfterLogOut = false
                callJoinViewModel.logOut()
            },
        )

        CallJoinBody(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .widthIn(0.dp, 500.dp)
                .verticalScroll(rememberScrollState())
                .weight(1f),
            callJoinViewModel = callJoinViewModel,
            openCamera = {
                val options = GmsBarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_AZTEC).build()
                val scanner = GmsBarcodeScanning.getClient(context, options)
                scanner.startScan().addOnSuccessListener(qrCodeCallback)
            },
        )
    }

    LaunchedEffect(key1 = isLoggedOut) {
        if (isLoggedOut) {
            navigateUpToLogin.invoke(callJoinViewModel.autoLogInAfterLogOut)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CallJoinHeader(
    callJoinViewModel: CallJoinViewModel = hiltViewModel(),
    onAvatarLongClick: () -> Unit,
    onDirectCallClick: () -> Unit,
    onSignOutClick: () -> Unit,
) {
    val user by callJoinViewModel.user.collectAsState(initial = null)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        user?.let {
            Box(
                modifier = if (BuildConfig.FLAVOR == "production") {
                    Modifier.combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                        onLongClick = onAvatarLongClick,
                    )
                } else {
                    Modifier
                },
            ) {
                UserAvatar(
                    modifier = Modifier.size(24.dp),
                    userName = it.userNameOrId,
                    userImage = it.image,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
        }

        Text(
            modifier = Modifier.weight(1f),
            color = Color.White,
            text = user?.name?.ifBlank { user?.id }?.ifBlank { user!!.custom["email"] }.orEmpty(),
            maxLines = 1,
            fontSize = 16.sp,
        )

        if (user?.custom?.get("email")?.contains("getstreamio") == true) {
            TextButton(
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                content = { Text(text = stringResource(R.string.direct_call)) },
                onClick = { onDirectCallClick.invoke() },
            )
        }

        if (BuildConfig.FLAVOR == "dogfooding") {
            Spacer(modifier = Modifier.width(5.dp))

            StreamButton(
                modifier = Modifier.widthIn(125.dp),
                text = stringResource(id = R.string.sign_out),
                onClick = onSignOutClick,
            )
        }
    }
}

@Composable
private fun CallJoinBody(
    modifier: Modifier,
    openCamera: () -> Unit,
    callJoinViewModel: CallJoinViewModel = hiltViewModel(),
) {
    val user by if (LocalInspectionMode.current) {
        remember { mutableStateOf(mockUsers[0]) }
    } else {
        callJoinViewModel.user.collectAsState(initial = null)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Colors.background)
            .semantics { testTagsAsResourceId = true },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (user != null) {
            Image(
                modifier = Modifier.size(102.dp),
                painter = painterResource(id = R.drawable.ic_stream_video_meeting_logo),
                contentDescription = null,
            )
            Spacer(modifier = Modifier.height(25.dp))

            Text(
                modifier = Modifier.padding(horizontal = 30.dp),
                text = stringResource(id = R.string.app_name),
                color = Color.White,
                fontSize = 32.sp,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(id = R.string.join_description),
            color = Colors.description,
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            modifier = Modifier.widthIn(0.dp, 320.dp),
        )

        Spacer(modifier = Modifier.height(42.dp))

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 35.dp),
            text = stringResource(id = R.string.call_id_number),
            color = Color(0xFF979797),
            fontSize = 13.sp,
        )

        Spacer(modifier = Modifier.height(8.dp))

        var callId by remember {
            mutableStateOf(
                if (BuildConfig.FLAVOR == "dogfooding") {
                    "default:79cYh3J5JgGk"
                } else {
                    ""
                },
            )
        }
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
                        RoundedCornerShape(6.dp),
                    ),
                shape = RoundedCornerShape(6.dp),
                value = callId,
                singleLine = true,
                onValueChange = { callId = it },
                trailingIcon = {
                    IconButton(
                        onClick = openCamera,
                        modifier = Modifier.fillMaxHeight(),
                        content = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_scan_qr),
                                contentDescription = stringResource(
                                    id = R.string.join_call_by_qr_code,
                                ),
                                tint = Colors.description,
                                modifier = Modifier.size(36.dp),
                            )
                        },
                    )
                },
                colors = TextFieldDefaults.textFieldColors(
                    textColor = Color.White,
                    focusedLabelColor = VideoTheme.colors.primaryAccent,
                    unfocusedIndicatorColor = Colors.secondBackground,
                    focusedIndicatorColor = Colors.secondBackground,
                    backgroundColor = Colors.secondBackground,
                ),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Email,
                ),
                placeholder = {
                    Text(
                        stringResource(id = R.string.join_call_call_id_hint),
                        color = Color(0xFF5D6168),
                    )
                },
                keyboardActions = KeyboardActions(
                    onDone = {
                        callJoinViewModel.handleUiEvent(CallJoinEvent.JoinCall(callId = callId))
                    },
                ),
            )

            StreamButton(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .fillMaxHeight()
                    .testTag("join_call"),
                onClick = {
                    callJoinViewModel.handleUiEvent(CallJoinEvent.JoinCall(callId = callId))
                },
                text = stringResource(id = R.string.join_call),
            )
        }

        Spacer(modifier = Modifier.height(25.dp))

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 35.dp),
            text = stringResource(id = R.string.join_call_no_id_hint),
            color = Color(0xFF979797),
            fontSize = 13.sp,
        )

        Spacer(modifier = Modifier.height(8.dp))

        StreamButton(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 35.dp)
                .testTag("start_new_call"),
            text = stringResource(id = R.string.start_a_new_call),
            onClick = { callJoinViewModel.handleUiEvent(CallJoinEvent.JoinCall()) },
        )
    }
}

@Composable
private fun HandleCallJoinUiState(
    callJoinUiState: CallJoinUiState,
    navigateToCallLobby: (callId: String) -> Unit,
    navigateUpToLogin: () -> Unit,
) {
    LaunchedEffect(key1 = callJoinUiState) {
        when (callJoinUiState) {
            is CallJoinUiState.JoinCompleted ->
                navigateToCallLobby.invoke(callJoinUiState.callId)

            is CallJoinUiState.GoBackToLogin ->
                navigateUpToLogin.invoke()

            else -> Unit
        }
    }
}

@Composable
private fun rememberQrCodeCallback(): OnSuccessListener<Barcode> {
    val context = LocalContext.current
    val firebaseAnalytics by lazy { FirebaseAnalytics.getInstance(context) }

    return remember {
        OnSuccessListener<Barcode> {
            val url = it.url?.url
            val callId = if (url != null) {
                val id = Uri.parse(url).getQueryParameter("id")
                if (!id.isNullOrEmpty()) {
                    id
                } else {
                    null
                }
            } else {
                null
            }

            if (!callId.isNullOrEmpty()) {
                firebaseAnalytics.logEvent(FirebaseEvents.SCAN_QR_CODE, null)
                context.startActivity(DeeplinkingActivity.createIntent(context, callId))
            } else {
                Toast.makeText(
                    context,
                    "Unrecognised meeting QR code format",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }
}

@Preview
@Composable
private fun CallJoinScreenPreview() {
    StreamMockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        StreamUserDataStore.install(LocalContext.current)
        CallJoinScreen(
            callJoinViewModel = CallJoinViewModel(StreamUserDataStore.instance()),
            navigateToCallLobby = {},
            navigateUpToLogin = {},
            navigateToDirectCallJoin = {},
        )
    }
}
