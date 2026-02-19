/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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
        val resultLauncher = defaultPermissionLauncher()

        setContent {
            VideoTheme {
                var selectedUser by remember { mutableStateOf(RingingApp.currentUser) }
                when (selectedUser) {
                    null -> LoginScreen(onUserSelected = { user ->
                        RingingApp.login(applicationContext, user)
                        // For the simplicity of the tutorial, we are requesting the required permissions here.
                        // In a real app, you should request the permissions when needed.
                        resultLauncher.requestDefaultPermissions()
                        selectedUser = user
                    })

                    else -> HomeScreen(
                        onLogoutClick = {
                            lifecycleScope.launch {
                                RingingApp.logout()
                                selectedUser = null
                            }
                        },
                        onDialClick = { callees ->
                            startOutgoingCallActivity(callees)
                        },
                    )
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
            leaveWhenLastInCall = true,
            action = NotificationHandler.ACTION_INCOMING_CALL,
            clazz = ComposeStreamCallActivity::class.java,
        )
        startActivity(intent)
    }

    private fun startOutgoingCallActivity(callees: List<String>) {
        val intent = StreamCallActivity.callIntent(
            context = this,
            cid = StreamCallId(type = "default", id = UUID.randomUUID().toString()),
            members = callees.toList(),
            leaveWhenLastInCall = true,
            action = NotificationHandler.ACTION_OUTGOING_CALL,
            clazz = ComposeStreamCallActivity::class.java,
        )
        startActivity(intent)
    }
}

@Composable
fun LoginScreen(onUserSelected: (TutorialUser) -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = VideoTheme.colors.baseTertiary) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(
                text = "Select a User",
                style = VideoTheme.typography.titleM,
                color = VideoTheme.colors.basePrimary,
                modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.weight(1f))
            val users = TutorialUser.builtIn
            UserList(users, onUserClick = { userId ->
                users.find { it.id == userId }?.let { onUserSelected(it) }
            })
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun HomeScreen(onLogoutClick: () -> Unit, onDialClick: (callees: List<String>) -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = VideoTheme.colors.baseTertiary) {
        Column(modifier = Modifier.fillMaxSize()) {
            HomeHeader(onLogoutClick)
            Spacer(modifier = Modifier.weight(1f))
            var callees by remember { mutableStateOf(RingingApp.callees) }
            UserList(callees, checkboxVisible = true) { calleeId ->
                callees = callees.map {
                    when (it.id == calleeId) {
                        true -> it.copy(checked = !it.checked)
                        else -> it
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onDialClick(callees.filter { it.checked }.map { it.id }) },
                modifier = Modifier.align(Alignment.CenterHorizontally),
                enabled = callees.any { it.checked },
            ) {
                Text(text = "Dial Selected Users")
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
@OptIn(ExperimentalMaterialApi::class)
private fun UserList(
    users: List<TutorialUser>,
    modifier: Modifier = Modifier,
    checkboxVisible: Boolean = false,
    onUserClick: (userId: String) -> Unit,
) {
    LazyColumn(modifier = modifier) {
        itemsIndexed(users) { idx, user ->
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onUserClick(user.id) }
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
                        text = user.name.orEmpty(),
                        style = VideoTheme.typography.titleS,
                    )
                },
                trailing = {
                    if (checkboxVisible && user.checked) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = VideoTheme.colors.iconDefault,
                        )
                    }
                },
            )
            if (idx < users.lastIndex) {
                Divider(color = VideoTheme.colors.baseQuinary, thickness = 1.dp)
            }
        }
    }
}

@Composable
private fun HomeHeader(onLogoutClick: () -> Unit) {
    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        UserAvatar(
            modifier = Modifier.size(48.dp),
            userImage = RingingApp.caller.image,
            userName = RingingApp.caller.name,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = RingingApp.caller.name.orEmpty(),
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
                contentDescription = null,
            )
        }
    }
}

private fun ComponentActivity.defaultPermissionLauncher(
    allGranted: () -> Unit = {
    },
) = registerForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions(),
) { granted ->
    // Handle the permissions result here
    granted.entries.forEach { (permission, granted) ->
        if (!granted) {
            Toast.makeText(this, "$permission permission is required.", Toast.LENGTH_LONG).show()
        }
    }
    if (granted.entries.all { it.value }) {
        allGranted()
    }
}

private fun ActivityResultLauncher<Array<String>>.requestDefaultPermissions() {
    var permissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions += Manifest.permission.POST_NOTIFICATIONS
    }
    launch(permissions)
}

private fun Context.isAudioPermissionGranted() = ContextCompat.checkSelfPermission(
    this, Manifest.permission.RECORD_AUDIO,
) == PackageManager.PERMISSION_GRANTED

private fun Context.isCameraPermissionGranted() = ContextCompat.checkSelfPermission(
    this, Manifest.permission.CAMERA,
) == PackageManager.PERMISSION_GRANTED
