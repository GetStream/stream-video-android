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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.getstream.video.android.app.FakeCredentialsProvider
import io.getstream.video.android.app.VideoApp
import io.getstream.video.android.app.ui.home.HomeActivity
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.model.UserCredentials
import io.getstream.video.android.utils.generateSFUToken
import stream.video.sfu.User

class CustomLoginActivity : AppCompatActivity() {

    private val nameInputState: MutableState<String> = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VideoTheme {
                CustomLoginContent()
            }
        }
    }

    @Composable
    private fun CustomLoginContent() {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Insert your name", style = VideoTheme.typography.title3)

            var inputState by remember { nameInputState }

            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                value = inputState,
                onValueChange = { inputState = it },
                label = { Text(text = "e.g. Alice") }
            )

            Button(
                onClick = { logIn(generateUser(inputState)) },
                content = { Text(text = "Log In") }
            )
        }
    }

    private fun logIn(selectedUser: UserCredentials) {
        VideoApp.userPreferences.storeUserCredentials(selectedUser)

        VideoApp.initializeClient(
            credentialsProvider = FakeCredentialsProvider(
                userCredentials = selectedUser,
                apiKey = "key1"
            ),
            user = User(
                id = selectedUser.id,
                name = selectedUser.name,
                image_url = selectedUser.image
            )
        )
        startActivity(HomeActivity.getIntent(this))
    }

    private fun generateUser(
        userName: String
    ): UserCredentials {
        val userId = userName.replace(" ", "_").lowercase()

        val token = generateSFUToken(userId, "call:123")

        return UserCredentials(
            id = userId,
            name = userName,
            token = token
        )
    }

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, CustomLoginActivity::class.java)
        }
    }
}
