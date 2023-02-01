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

package io.getstream.video.android.tutorial_starter.ui.login

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.getstream.log.taggedLogger
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.model.User
import io.getstream.video.android.tutorial_starter.ui.components.UserList
import io.getstream.video.android.tutorial_starter.ui.home.HomeActivity
import io.getstream.video.android.tutorial_starter.user.AppUser
import io.getstream.video.android.tutorial_starter.utils.getUsers
import io.getstream.video.android.tutorial_starter.videoApp

class LoginActivity : AppCompatActivity() {

    private val logger by taggedLogger("Call:LoginView")

    private val loginItemsState = mutableStateOf(
        getUsers().map {
            AppUser(it, false)
        }
    )

    init {
        logger.i { "<init> this: $this" }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkIfUserLoggedIn()

        setContent {
            VideoTheme {
                LoginContent()
            }
        }
    }

    private fun checkIfUserLoggedIn() {
        val preferences = videoApp.userPreferences

        val user = preferences.getCachedCredentials()

        if (user != null && user.isValid()) {
            logIn(user)
        }
    }

    @Composable
    private fun LoginContent() {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier.padding(8.dp),
                text = "Select a user to log in!",
                fontSize = 18.sp
            )

            val loginItems by remember { loginItemsState }

            UserList(
                modifier = Modifier.fillMaxWidth(),
                userItems = loginItems,
                onClick = { credentials ->
                    val updated = loginItemsState.value.map {
                        it.copy(isSelected = it.user.token == credentials.user.token)
                    }

                    loginItemsState.value = updated
                }
            )

            val isDataValid = loginItemsState.value.any { it.isSelected }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                enabled = isDataValid,
                onClick = {
                    val wrapper = loginItemsState.value.firstOrNull { it.isSelected }

                    if (wrapper != null) {
                        logIn(wrapper.user)
                    }
                },
                content = { Text(text = "Log In") }
            )
        }
    }

    private fun logIn(selectedUser: User) {
        // TODO - log in the user
        startActivity(HomeActivity.getIntent(this))
        finish()
    }
}
