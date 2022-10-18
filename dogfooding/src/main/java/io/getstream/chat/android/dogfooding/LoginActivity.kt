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

package io.getstream.chat.android.dogfooding

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.google.firebase.auth.FirebaseAuth
import io.getstream.video.android.logging.LoggingLevel
import io.getstream.video.android.model.User
import io.getstream.video.android.model.toCredentials
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = dogfoodingApp.userPreferences.getCachedCredentials()

        if (user.id.isNotEmpty() && user.token.isNotEmpty()) {
            startHome(
                user.token,
                user.toUser()
            )
            finish()
        }

        setContent {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                content = {
                    Text(text = "Authenticate")
                },
                onClick = ::authenticate
            )
        }
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
                authUser?.email ?: "",
                "admin",
                authUser?.displayName ?: "",
                token,
                authUser?.photoUrl?.toString() ?: "",
                emptyList(),
                emptyMap()
            )

            startHome(token, user)
        }
    }

    private fun startHome(
        token: String,
        user: User
    ) {
        dogfoodingApp.initializeStreamCalls(
            AuthCredentialsProvider(
                "key10", token,
                user = user
            ),
            loggingLevel = LoggingLevel.BODY
        )
        dogfoodingApp.userPreferences.storeUserCredentials(user.toCredentials())
        startActivity(HomeActivity.getIntent(this))
    }

    private fun onSignInFailed() {
        Toast.makeText(this, "Verification failed!", Toast.LENGTH_SHORT).show()
    }
}
