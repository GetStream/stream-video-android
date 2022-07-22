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

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.getstream.video.android.app.FakeTokenProvider
import io.getstream.video.android.app.VideoApp
import io.getstream.video.android.app.model.LoginCredentials
import io.getstream.video.android.app.ui.main.MainActivity
import stream.video.User
import java.util.*

class LoginActivity : AppCompatActivity() {

    private val callIdState = mutableStateOf("testroom")
    private val loginItemsState = mutableStateOf(getUsers())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (credentials in loginItems) {
                        LoginItem(loginCredentials = credentials)
                    }
                }

                val inputState by remember { callIdState }

                OutlinedTextField(
                    value = inputState,
                    onValueChange = { input ->
                        callIdState.value = input
                    },
                    label = {
                        Text(text = "Enter the call ID")
                    }
                )

                val isDataValid =
                    loginItemsState.value.any { it.isSelected } && callIdState.value.isNotBlank()

                Button(
                    enabled = isDataValid,
                    onClick = { logIn() },
                    content = { Text(text = "Join Call") }
                )
            }
        }
    }

    private fun logIn() {
        val selectedUser = loginItemsState.value.firstOrNull { it.isSelected } ?: return
        val callId = callIdState.value

        VideoApp.initializeClient(
            "fake-api-key",
            tokenProvider = FakeTokenProvider(
                selectedUser.token
            ),
            user = User(
                id = selectedUser.name.lowercase(Locale.getDefault())
            )
        )
        startActivity(MainActivity.getIntent(this, callId))
        finish()
    }

    @Composable
    private fun LoginItem(loginCredentials: LoginCredentials) {
        val buttonColor = if (!loginCredentials.isSelected) {
            MaterialTheme.colors.primary
        } else {
            MaterialTheme.colors.secondary
        }

        val colors = ButtonDefaults.textButtonColors(
            backgroundColor = buttonColor,
            contentColor = Color.White
        )

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            colors = colors,
            content = {
                Text(text = loginCredentials.name.replace("_", " "))
            },
            onClick = {
                val updated = loginItemsState.value.map {
                    it.copy(isSelected = it.token == loginCredentials.token)
                }

                loginItemsState.value = updated
            }
        )
    }

    private fun getUsers(): List<LoginCredentials> {
        return listOf(
            LoginCredentials(
                name = "Filip",
                token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiZmlsaXAifQ.qjL9ajhHQgtrlKDvINmN7Kmf8uuZv52d4j8elnqN2iM"
            ),
            LoginCredentials(
                name = "Mile_Kitic",
                token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoibWlsZV9raXRpYyJ9.BW7ckfQexMNS_aag9b7iNEfihffR2q8UxBRGtx3NL-8"
            ),
            LoginCredentials(
                name = "Toma_Zdravkovic",
                token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoidG9tYSJ9.0ZhjX_HCFE6cQ55znQeNi6DLXJ1g_mgww8idyQIeWMA"
            )
        )
    }
}
