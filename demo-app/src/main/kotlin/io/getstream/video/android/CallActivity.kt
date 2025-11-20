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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.android.video.generated.models.RingCallRequest
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.models.Filters
import io.getstream.chat.android.models.querysort.QuerySortByField
import io.getstream.result.onSuccessSuspend
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.ComposeStreamCallActivity
import io.getstream.video.android.compose.ui.StreamCallActivityComposeDelegate
import io.getstream.video.android.compose.ui.components.avatar.UserAvatar
import io.getstream.video.android.compose.ui.components.call.activecall.AudioOnlyCallContent
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.datastore.delegate.StreamUserDataStore
import io.getstream.video.android.ui.call.CallScreen
import io.getstream.video.android.ui.common.StreamActivityUiDelegate
import io.getstream.video.android.ui.common.StreamCallActivity
import io.getstream.video.android.ui.common.StreamCallActivityConfiguration
import io.getstream.video.android.ui.common.util.StreamCallActivityDelicateApi
import io.getstream.video.android.util.FullScreenCircleProgressBar
import io.getstream.video.android.util.StreamVideoInitHelper
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
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
            RingingRootContent(call) {
                val micEnabled by call.microphone.isEnabled.collectAsStateWithLifecycle()
                AudioOnlyCallContent(
                    call = call,
                    isMicrophoneEnabled = micEnabled,
                    onCallAction = { onCallAction(call, it) },
                    onBackPressed = { onBackPressed(call) },
                )
            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RingingRootContent(call: Call, bodyContent: @Composable () -> Unit) {
    Box {
        bodyContent()
        var showBottomPopUp by rememberSaveable { mutableStateOf(false) }

        Icon(
            imageVector = Icons.Default.People,
            tint = VideoTheme.colors.iconDefault,
            contentDescription = null,
            modifier = Modifier
                .padding(16.dp)
                .background(
                    color = VideoTheme.colors.baseSenary, // choose your color
                    shape = CircleShape,
                )
                .padding(12.dp)
                .align(Alignment.TopEnd)
                .clickable {
                    showBottomPopUp = !showBottomPopUp
                },
        )

        if (showBottomPopUp) {
            ModalBottomSheet(onDismissRequest = {
                showBottomPopUp = !showBottomPopUp
            }, containerColor = Color.Black) {
                MemberListRowContent(call)
            }
        }
    }
}

@Composable
private fun MemberListRowContent(call: Call) {
    val members by call.state.members.collectAsStateWithLifecycle()
    val participants by call.state.participants.collectAsStateWithLifecycle()
    val localCallingList = remember { mutableListOf<PeopleUiState>() }

    val peopleNotInCallList = arrayListOf<PeopleUiState>()
    peopleNotInCallList.addAll(
        members.filter { it.user.id !== StreamVideo.instance().userId }.map {
            PeopleUiState(
                it.user.userNameOrId,
                PeopleUiCallState.NOT_IN_CALL,
                false,
                false,
                it.user.image ?: "",
                it.user.id,
            )
        },
    )

    val peopleInCallList = arrayListOf<PeopleUiState>()
    participants.forEach { participant ->
        val personInCall = peopleNotInCallList.filter { it.userId == participant.userId.value }
        if (personInCall.isNotEmpty()) {
            peopleNotInCallList.remove(personInCall.first())
            peopleInCallList.add(
                personInCall.first().copy(peopleUiCallState = PeopleUiCallState.IN_CALL),
            )
        }
    }
    val peopleList = remember { mutableStateListOf<PeopleUiState>() }
    peopleList.addAll(peopleNotInCallList)
    peopleList.addAll(peopleInCallList)
    peopleList.removeIf { it.userId == StreamVideo.instance().userId }

    localCallingList.forEach { localCalling ->
        peopleList.forEachIndexed { index, people ->
            if (localCalling.userId == people.userId) {
                peopleList[index] = people.copy(peopleUiCallState = PeopleUiCallState.CALLING)
            }
        }
    }

    LazyColumn {
        items(count = peopleList.size, key = { index -> peopleList[index].userId }) { index ->
            val people = peopleList[index]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VideoTheme.dimens.spacingM),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val userName = people.name
                    val userImage = people.image

                    UserAvatar(
                        modifier = Modifier
                            .size(VideoTheme.dimens.genericXxl)
                            .testTag("Stream_ParticipantsListUserAvatar"),
                        userImage = userImage,
                        userName = userName,
                        isShowingOnlineIndicator = false,
                    )
                    Spacer(modifier = Modifier.size(VideoTheme.dimens.spacingM))
                    Text(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .testTag("Stream_ParticipantsListUserName"),
                        text = userName,
                        style = VideoTheme.typography.bodyM,
                        color = VideoTheme.colors.basePrimary,
                        fontSize = 16.sp,
                        maxLines = 1,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    when (people.peopleUiCallState) {
                        PeopleUiCallState.NOT_IN_CALL -> {
                            val scope = rememberCoroutineScope()
                            Icon(
                                modifier = Modifier.clickable {
                                    scope.launch {
                                        call.ring(
                                            RingCallRequest(
                                                call.isVideoEnabled(),
                                                listOf(people.userId),
                                            ),
                                        )
                                        localCallingList.add(people)
                                    }
                                },
                                tint = VideoTheme.colors.basePrimary,
                                imageVector = Icons.Default.Call,
                                contentDescription = null,
                            )
                        }
                        PeopleUiCallState.CALLING -> {
                            Icon(
                                modifier = Modifier,
                                tint = VideoTheme.colors.basePrimary,
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = null,
                            )
                        }
                        PeopleUiCallState.IN_CALL -> {}
                    }
                }
            }
            Spacer(modifier = Modifier.size(VideoTheme.dimens.spacingM))
        }
    }
}

internal data class PeopleUiState(
    val name: String,
    val peopleUiCallState: PeopleUiCallState,
    val videoEnabled: Boolean,
    val audioEnabled: Boolean,
    val image: String,
    val userId: String,
)

internal enum class PeopleUiCallState {
    NOT_IN_CALL,
    IN_CALL,
    CALLING,
}
