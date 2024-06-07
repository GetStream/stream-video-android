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

import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.ComposeStreamCallActivity
import io.getstream.video.android.compose.ui.StreamCallActivityComposeDelegate
import io.getstream.video.android.compose.ui.components.call.controls.actions.AcceptCallAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.GenericAction
import io.getstream.video.android.compose.ui.components.participants.ParticipantAvatars
import io.getstream.video.android.compose.ui.components.participants.internal.ParticipantInformation
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.MemberState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.LeaveCall
import io.getstream.video.android.core.model.CallStatus
import io.getstream.video.android.core.notifications.NotificationHandler
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.ui.common.StreamActivityUiDelegate
import io.getstream.video.android.ui.common.StreamCallActivity
import io.getstream.video.android.ui.common.StreamCallActivityConfiguration
import io.getstream.video.android.ui.common.util.StreamCallActivityDelicateApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import org.openapitools.client.models.CustomVideoEvent
import java.util.UUID

// Extends the ComposeStreamCallActivity class to provide a custom UI for the calling screen.
@Suppress("UNCHECKED_CAST")
class CustomCallActivity : ComposeStreamCallActivity() {

    // Internal delegate to customize the UI aspects of the call.
    private val _internalDelegate = CustomUiDelegate()

    // Getter for UI delegate, specifies the custom UI delegate for handling UI related functionality.
    override val uiDelegate: StreamActivityUiDelegate<StreamCallActivity>
        get() = _internalDelegate

    private lateinit var acceptPermissionHandler: ActivityResultLauncher<String>

    // network requests and callback for connectivity observation
    private val networkRequest = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
        .build()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        // lost network connection
        override fun onLost(network: Network) {
            super.onLost(network)
            val activeCall = StreamVideo.instance().state.activeCall.value
            if (activeCall != null) {
                end(activeCall)
            }
            Toast.makeText(
                this@CustomCallActivity,
                "Check out your network status!",
                Toast.LENGTH_SHORT,
            ).show()
            finish()
        }
    }

    @OptIn(StreamCallActivityDelicateApi::class)
    override val configuration: StreamCallActivityConfiguration
        get() = StreamCallActivityConfiguration(
            closeScreenOnCallEnded = false,
            canSkiPermissionRationale = false,
        )

    override fun onCreate(
        savedInstanceState: Bundle?,
        persistentState: PersistableBundle?,
        call: Call,
    ) {
        super.onCreate(savedInstanceState, persistentState, call)
        acceptPermissionHandler = registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            if (intent.action == NotificationHandler.ACTION_ACCEPT_CALL && granted) {
                accept(call)
            }
        }

        // register the network connectivity observer
        val connectivityManager =
            getSystemService(ConnectivityManager::class.java) as ConnectivityManager
        connectivityManager.requestNetwork(networkRequest, networkCallback)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent?.action == NotificationHandler.ACTION_ACCEPT_CALL) {
            val activeCall = StreamVideo.instance().state.activeCall.value
            if (activeCall != null) {
                end(activeCall)
                finish()
                startActivity(intent)
            }
        }
    }

    @OptIn(StreamCallActivityDelicateApi::class)
    override fun accept(
        call: Call,
        onSuccess: (suspend (Call) -> Unit)?,
        onError: (suspend (Exception) -> Unit)?,
    ) {
        if (isAudioPermissionGranted()) {
            super.accept(call, onSuccess, onError)
        } else {
            acceptPermissionHandler.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    override fun onCallAction(call: Call, action: CallAction) {
        if (action is LeaveCall && !isAudioPermissionGranted()) {
            finish()
        } else {
            super.onCallAction(call, action)
        }
    }

    // Custom delegate class to define specific UI behaviors and layouts for call states.
    private class CustomUiDelegate : StreamCallActivityComposeDelegate() {

        // Defines the UI for when the call is in a loading state with a full-screen progress bar and text.
        @Composable
        fun FullScreenCircleProgressBar(text: String) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(VideoTheme.colors.baseSheetPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text(
                        text = text,
                        style = VideoTheme.typography.bodyL,
                    )
                }
            }
        }

        @Composable
        override fun StreamCallActivity.LoadingContent(call: Call) {
            FullScreenCircleProgressBar(
                "Connecting...",
            )
        }

        // Outgoing call content shows the calling or ringing status to the user.
        @Composable
        override fun StreamCallActivity.OutgoingCallContent(
            modifier: Modifier,
            call: Call,
            isVideoType: Boolean,
            isShowingHeader: Boolean,
            headerContent:
            @Composable()
            (ColumnScope.() -> Unit)?,
            detailsContent:
            @Composable()
            (
                ColumnScope.(participants: List<MemberState>, topPadding: Dp) -> Unit
            )?,
            controlsContent:
            @Composable()
            (BoxScope.() -> Unit)?,
            onBackPressed: () -> Unit,
            onCallAction: (CallAction) -> Unit,
        ) {
            val receiverAlive by call.receiverActive()
                .collectAsStateWithLifecycle(initialValue = false)
            val callingText = if (receiverAlive) {
                "Ringing..."
            } else {
                "Calling..."
            }

            io.getstream.video.android.compose.ui.components.call.ringing.outgoingcall.OutgoingCallContent(
                call = call,
                isVideoType = isVideoType,
                modifier = modifier,
                isShowingHeader = isShowingHeader,
                headerContent = headerContent,
                detailsContent = detailsContent ?: { members, topPadding ->
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = topPadding),
                    ) {
                        ParticipantInformation(
                            isVideoType = false,
                            callStatus = CallStatus.Calling(callingText),
                            participants = members,
                        )
                        Spacer(modifier = Modifier.size(16.dp))
                        ParticipantAvatars(participants = members)
                    }
                },
                controlsContent = controlsContent,
                onBackPressed = onBackPressed,
                onCallAction = onCallAction,
            )
        }

        // Defines the UI shown when the call is disconnected.
        @Composable
        override fun StreamCallActivity.CallDisconnectedContent(call: Call) {
            Log.d("LOGG", "Call disconnected!!")
            if (isCaller()) {
                // Display a UI allowing the user to close the call or redial.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(VideoTheme.colors.baseSheetPrimary),
                ) {
                    io.getstream.video.android.compose.ui.components.call.activecall.AudioCallContent(
                        call = call,
                        isMicrophoneEnabled = false,
                        detailsContent = { members, _ ->
                            ParticipantInformation(
                                isVideoType = false,
                                callStatus = CallStatus.Calling("Disconnected..."),
                                participants = members,
                            )
                            Spacer(modifier = Modifier.size(16.dp))
                            ParticipantAvatars(participants = members)
                        },
                        controlsContent = {
                            // Custom controls for redial and close actions.
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                // Close action column
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .weight(1f),
                                ) {
                                    GenericAction(
                                        enabled = true,
                                        onAction = {
                                            finish()
                                        },
                                        icon = Icons.Default.Close,
                                        color = VideoTheme.colors.baseSheetTertiary,
                                        iconTint = VideoTheme.colors.basePrimary,
                                        modifier = Modifier.size(56.dp),
                                    )
                                    Text(
                                        text = "Close",
                                        color = VideoTheme.colors.basePrimary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }

                                // Redial action column
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .weight(1f),
                                ) {
                                    AcceptCallAction(
                                        onCallAction = {
                                            val restartIntent = intent.apply {
                                                putExtra(
                                                    NotificationHandler.INTENT_EXTRA_CALL_CID,
                                                    StreamCallId(
                                                        "audio_call",
                                                        UUID.randomUUID().toString(),
                                                    ),
                                                )
                                            }
                                            finish()
                                            startActivity(restartIntent)
                                        },
                                        modifier = Modifier.size(56.dp),
                                    )
                                    Text(
                                        text = "Call again",
                                        color = VideoTheme.colors.basePrimary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        },
                    )
                }
            } else {
                finish()
            }
        }
    }
}

const val ALIVE_KEY = "v=fNFzfwLM72c"
const val USER_KEY = "user-key"

fun Call.receiverActive() = customEvents(ALIVE_KEY).map {
    val originatorUser = it.custom.getOrDefault(USER_KEY, null)
    originatorUser != state.me.value?.userId
}

fun Call.customEvents(withKey: String? = null): Flow<CustomVideoEvent> {
    val events = defaultBufferedFlow<CustomVideoEvent>()
    subscribeFor(CustomVideoEvent::class.java) {
        val custom = it as CustomVideoEvent
        if (withKey != null) {
            if (custom.custom.containsKey(withKey)) {
                events.tryEmit(custom)
            }
        } else {
            events.tryEmit(it)
        }
    }

    return events
}

fun <T> defaultBufferedFlow() = MutableSharedFlow<T>(
    replay = 0,
    extraBufferCapacity = 15, // 15 events to be buffered at most
    onBufferOverflow = BufferOverflow.DROP_OLDEST,
)
