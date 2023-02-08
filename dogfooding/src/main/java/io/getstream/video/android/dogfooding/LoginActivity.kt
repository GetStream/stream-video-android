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

package io.getstream.video.android.dogfooding

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.lifecycleScope
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.google.firebase.auth.FirebaseAuth
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.token.AuthCredentialsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL

class LoginActivity : ComponentActivity() {

    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            onSignInFailed()
            return@registerForActivityResult
        }

        val email = result?.idpResponse?.email

        if (email != null) {
            onSignInSuccess(email)
        } else {
            onSignInFailed()
        }
    }

    private val isShowingGuestLogin = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = dogfoodingApp.userPreferences.getCachedCredentials()

        if (user != null && user.isValid()) {
            startHome(
                user
            )
            finish()
        }

        setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 8.dp),
                        content = {
                            Text(text = "Authenticate")
                        },
                        onClick = ::authenticate
                    )

                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 8.dp),
                        content = {
                            Text(text = "Login as Guest")
                        },
                        onClick = {
                            isShowingGuestLogin.value = true
                        }
                    )
                }

                val isShowingGuestLogin by isShowingGuestLogin

                if (isShowingGuestLogin) {
                    GuestLoginOverlay()
                }
            }
        }
    }

    @Composable
    private fun GuestLoginOverlay() {
        var guestLoginEmailState by remember { mutableStateOf("") }

        Dialog(
            onDismissRequest = {
                isShowingGuestLogin.value = false
            },
            content = {
                Surface(modifier = Modifier.width(300.dp)) {
                    Column {
                        TextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            value = guestLoginEmailState,
                            onValueChange = { guestLoginEmailState = it },
                            label = { Text(text = "Enter your e-mail address") },
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Email
                            )
                        )

                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            onClick = {
                                onSignInSuccess(guestLoginEmailState)
                                isShowingGuestLogin.value = false
                            },
                            content = { Text(text = "Log in") }
                        )
                    }
                }
            }
        )
    }

    private fun authenticate() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        // Create and launch sign-in intent
        val signInIntent =
            AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(providers)
                .build()
        signInLauncher.launch(signInIntent)
    }

    private fun onSignInSuccess(email: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val request =
                URL("https://stream-calls-dogfood.vercel.app/api/auth/create-token?user_id=$email")
            val connection = request.openConnection()

            connection.connect()

            // Read and print the input
            val inputStream = BufferedReader(InputStreamReader(connection.getInputStream()))
            val response = inputStream.readLines().toString()
            println(response)
            inputStream.close()

            logIn(response)
        }
    }

    private fun logIn(response: String) {
        val jsonArray = JSONArray(response)
        val userJSON = jsonArray.get(0) as? JSONObject
        val authUser = FirebaseAuth.getInstance().currentUser

        if (userJSON != null) {
            val token = userJSON.getString("token")
            val user = User(
                authUser?.email ?: userJSON.getString("userId"),
                "admin",
                authUser?.displayName ?: "",
                token,
                authUser?.photoUrl?.toString() ?: "",
                emptyList(),
                emptyMap()
            )

            startHome(user)
        }
    }

    private fun startHome(user: User) {
        dogfoodingApp.initializeStreamVideo(
            AuthCredentialsProvider(
                apiKey = API_KEY,
                user = user
            ),
            loggingLevel = LoggingLevel.BODY
        )
        startActivity(HomeActivity.getIntent(this))
    }

    private fun onSignInFailed() {
        Toast.makeText(this, "Verification failed!", Toast.LENGTH_SHORT).show()
    }
}
