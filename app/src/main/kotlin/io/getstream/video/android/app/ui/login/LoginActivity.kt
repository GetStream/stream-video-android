/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.app.ui.login

import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.getstream.video.android.app.FakeCredentialsProvider
import io.getstream.video.android.app.VideoApp
import io.getstream.video.android.app.ui.components.UserList
import io.getstream.video.android.app.ui.home.HomeActivity
import io.getstream.video.android.app.utils.getUsers
import io.getstream.video.android.notifications.CallNotificationReceiver
import io.getstream.video.android.notifications.CallNotificationReceiver.Companion.ACTION_CALL
import stream.video.User

class LoginActivity : AppCompatActivity() {

    private val loginItemsState = mutableStateOf(getUsers())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerReceiver(
            CallNotificationReceiver(),
            IntentFilter(ACTION_CALL)
        )

        setContent {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier.padding(4.dp),
                    text = "Select a user to log in!",
                    fontSize = 18.sp
                )

                val loginItems by remember { loginItemsState }

                UserList(
                    modifier = Modifier.fillMaxWidth(),
                    userItems = loginItems,
                    onClick = { credentials ->
                        val updated = loginItemsState.value.map {
                            it.copy(isSelected = it.token == credentials.token)
                        }

                        loginItemsState.value = updated
                    }
                )

                val isDataValid = loginItemsState.value.any { it.isSelected }

                Button(
                    enabled = isDataValid,
                    onClick = { logIn() },
                    content = { Text(text = "Log In") }
                )
            }
        }
    }

    private fun logIn() {
        val selectedUser = loginItemsState.value.firstOrNull { it.isSelected } ?: return

        VideoApp.initializeClient(
            credentialsProvider = FakeCredentialsProvider(
                token = selectedUser.token,
                apiKey = "key1"
            ),
            user = User(id = selectedUser.id, name = selectedUser.name, image_url = selectedUser.image)
        )
        startActivity(HomeActivity.getIntent(this))
        finish()
    }
}
