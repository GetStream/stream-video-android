package io.getstream.video.android.dogfooding

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.avatar.Avatar
import io.getstream.video.android.logging.LoggingLevel
import io.getstream.video.android.model.User
import io.getstream.video.android.utils.onError
import io.getstream.video.android.utils.onSuccess
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authenticateUser()
        setContent {
            VideoTheme {
                HomeScreen()
            }
        }
    }

    private val callIdState: MutableState<String> = mutableStateOf("call123")

    private val loadingState: MutableState<Boolean> = mutableStateOf(false)

    private fun authenticateUser() {
        val token = "hardcoded-token"
        val user = User(
            "hardcoded-id",
            "admin",
            "Tutorial Droid",
            token,
            "hardcoded image",
            emptyList(),
            emptyMap()
        )

        videoApp.initializeStreamCalls(
            AuthCredentialsProvider(
                apiKey = "hardcoded-api-key",
                userToken = token,
                user = user
            ),
            loggingLevel = LoggingLevel.BODY
        )
    }

    @Composable
    private fun HomeScreen() {
        Column(modifier = Modifier.fillMaxSize()) {
            UserDetails()

            JoinCallContent()

            Spacer(modifier = Modifier.height(16.dp))

            val isLoading by loadingState

            if (isLoading) {
                Spacer(modifier = Modifier.height(16.dp))

                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }

    @Composable
    fun ColumnScope.JoinCallContent() {
        CallIdInput()

        val isDataValid = callIdState.value.isNotBlank()

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .align(Alignment.CenterHorizontally),
            enabled = isDataValid, onClick = {
                joinCall(callId = callIdState.value)
            }
        ) {
            Text(text = "Join call")
        }
    }

    private fun joinCall(callId: String) {
        lifecycleScope.launch {
            loadingState.value = true
            val result = videoApp.streamCalls.joinCall(
                "default", id = callId
            )
            loadingState.value = false
            result.onSuccess {
                /*startActivity(
                    CallActivity.getIntent(
                        this@MainActivity,
                        CallInput(
                            callCid = it.call.cid,
                            callType = it.call.type,
                            callId = it.call.id,
                            callUrl = it.callUrl,
                            userToken = it.userToken,
                            iceServers = it.iceServers
                        )
                    )
                )*/
            }
            result.onError {
                Toast.makeText(this@MainActivity, it.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Composable
    fun CallIdInput() {
        val inputState by remember { callIdState }

        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            value = inputState,
            onValueChange = { input ->
                callIdState.value = input
            },
            label = {
                Text(text = "Enter the call ID")
            }
        )
    }

    @Composable
    private fun UserDetails() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            UserIcon()

            val user = videoApp.streamCalls.getUser()

            Text(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .align(Alignment.CenterVertically),
                text = user.name,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }

    @Composable
    fun UserIcon() {
        val user = videoApp.credentialsProvider.getUserCredentials()
        Avatar(
            modifier = Modifier
                .size(40.dp)
                .padding(top = 8.dp, start = 8.dp)
                .clip(CircleShape),
            imageUrl = user.imageUrl.orEmpty(),
            initials = user.name,
        )
    }
}