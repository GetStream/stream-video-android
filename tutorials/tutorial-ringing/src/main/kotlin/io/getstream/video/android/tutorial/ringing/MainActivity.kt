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

package io.getstream.video.android.tutorial.ringing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ListItem
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.ComposeStreamCallActivity
import io.getstream.video.android.compose.ui.components.avatar.UserAvatar
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.notifications.NotificationHandler
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.ui.common.StreamCallActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * This is the video call sample project follows the official ringing flow tutorial:
 *
 * https://getstream.io/video/sdk/android/tutorial/ringing-flow/
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val resultLauncher = defaultPermissionLauncher {
            startOutgoingCallActivity(RingingApp.callee.id)
        }

        setContent {
            VideoTheme {
                var selectedUser by remember { mutableStateOf<TutorialUser?>(null) }
                when (selectedUser) {
                    null -> LoginScreen(onUserSelected = { user ->
                        RingingApp.login(applicationContext, user)
                        selectedUser = user
                    })

                    else -> HomeScreen(onLogoutClick = {
                        RingingApp.logout()
                        selectedUser = null
                    }, onDialClick = {
                        if (isAudioPermissionGranted() && isCameraPermissionGranted()) {
                            startOutgoingCallActivity(RingingApp.callee.id)
                        } else {
                            resultLauncher.requestDefaultPermissions()
                        }
                    })
                }
            }
        }

        observeIncomingCall()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeIncomingCall() {
        lifecycleScope.launch {
            StreamVideo.instanceState.flatMapLatest { instance ->
                instance?.state?.ringingCall ?: flowOf(null)
            }.collectLatest { call ->
                if (call != null) {
                    lifecycleScope.launch {
                        // Monitor the ringingState on a non-null call
                        call.state.ringingState.collectLatest {
                            if (it is RingingState.Incoming) {
                                startIncomingCallActivity(call)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startIncomingCallActivity(call: Call) {
        val intent = StreamCallActivity.callIntent(
            context = this,
            cid = StreamCallId.fromCallCid(call.cid),
            members = emptyList(),
            leaveWhenLastInCall = true,
            action = NotificationHandler.ACTION_INCOMING_CALL,
            clazz = ComposeStreamCallActivity::class.java,
        )
        startActivity(intent)
    }

    private fun startOutgoingCallActivity(vararg members: String) {
        val intent = StreamCallActivity.callIntent(
            context = this,
            cid = StreamCallId(type = "default", id = UUID.randomUUID().toString()),
            members = members.toList(),
            leaveWhenLastInCall = true,
            action = NotificationHandler.ACTION_OUTGOING_CALL,
            clazz = ComposeStreamCallActivity::class.java,
        )
        startActivity(intent)
    }
}

@Composable
fun HomeScreen(onLogoutClick: () -> Unit, onDialClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = VideoTheme.colors.baseTertiary),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(
                modifier = Modifier.size(48.dp),
                userImage = RingingApp.caller.image,
                userName = RingingApp.caller.name,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = RingingApp.caller.name,
                style = VideoTheme.typography.titleS,
                color = VideoTheme.colors.basePrimary,
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = onLogoutClick,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.ExitToApp,
                    tint = VideoTheme.colors.iconDefault,
                    contentDescription = null
                )
            }

        }

        Box(modifier = Modifier.fillMaxSize()) {
            Button(onClick = onDialClick, modifier = Modifier.align(Alignment.Center)) {
                Text(text = "Dial ${RingingApp.callee.name}")
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun LoginScreen(onUserSelected: (TutorialUser) -> Unit) {
    // step1 - select a user.
    Surface(modifier = Modifier.fillMaxSize(), color = VideoTheme.colors.baseTertiary) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = "Select a User",
                style = VideoTheme.typography.titleM,
                color = VideoTheme.colors.basePrimary,
                modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp),
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start,
            ) {
                itemsIndexed(TutorialUser.builtIn) { idx, user ->
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUserSelected(user) }
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                        icon = {
                            UserAvatar(
                                modifier = Modifier.size(48.dp),
                                userImage = user.image,
                                userName = user.name,
                            )
                        },
                        text = {
                            Text(
                                text = user.name,
                                style = VideoTheme.typography.titleS,
                            )
                        },
                    )
                    if (idx < TutorialUser.builtIn.lastIndex) {
                        Divider(color = VideoTheme.colors.baseQuinary, thickness = 1.dp)
                    }
                }
            }
        }
    }
}