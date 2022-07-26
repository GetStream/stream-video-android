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

package io.getstream.video.android.app.ui.call

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES.M
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import io.getstream.video.android.app.VideoApp
import io.getstream.video.android.ui.components.CallDetails
import io.getstream.video.android.ui.components.MainStage
import io.getstream.video.android.utils.onError
import io.getstream.video.android.utils.onSuccessSuspend
import io.getstream.video.android.viewmodel.CallViewModel
import io.livekit.android.LiveKit
import io.livekit.android.RoomOptions
import kotlinx.coroutines.launch
import stream.video.SelectEdgeServerResponse

class CallActivity : AppCompatActivity() {

    private val callViewModel by viewModels<CallViewModel>()

    @RequiresApi(M)
    private val permissionsContract = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val missing = getMissingPermissions()
        val deniedCamera = !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
        val deniedMicrophone =
            !shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)

        when {
            missing.isNotEmpty() && !deniedCamera && !deniedMicrophone -> requestPermissions(missing)
            isGranted -> startVideoFlow()
            deniedCamera || deniedMicrophone -> showPermissionsDialog()
            else -> {
                checkPermissions()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VideoCallContent()
        }
    }

    @Composable
    private fun VideoCallContent() {
        val room by callViewModel.roomState.collectAsState(initial = null)
        val participants by callViewModel.participantList.collectAsState(initial = emptyList())
        val speaker by callViewModel.primarySpeaker.collectAsState(initial = null)
        val isCameraEnabled by callViewModel.isCameraEnabled.collectAsState(initial = false)
        val isMicrophoneEnabled by callViewModel.isMicrophoneEnabled.collectAsState(initial = false)

        Column(modifier = Modifier.fillMaxSize()) {
            val currentRoom = room
            val currentSpeaker = speaker

            if (currentRoom == null || currentSpeaker == null) {
                Box(
                    modifier = Modifier
                        .height(250.dp)
                        .fillMaxWidth()
                ) {
                    Image(
                        modifier = Modifier.align(Alignment.Center),
                        imageVector = Icons.Default.Call,
                        contentDescription = null
                    )
                }
            } else {
                MainStage(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxWidth(),
                    room = currentRoom,
                    speaker = currentSpeaker
                )

                CallDetails(
                    modifier = Modifier.weight(0.5f),
                    room = currentRoom,
                    isCameraEnabled = isCameraEnabled,
                    isMicrophoneEnabled = isMicrophoneEnabled,
                    participants = participants,
                    primarySpeaker = currentSpeaker,
                    onEndCall = {
                        currentRoom.disconnect()
                        finish()
                    },
                    onCameraToggled = { isEnabled -> callViewModel.toggleCamera(isEnabled) },
                    onMicrophoneToggled = { isEnabled ->
                        callViewModel.toggleMicrophone(
                            isEnabled
                        )
                    },
                    onCameraFlipped = callViewModel::flipCamera
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= M) {
            checkPermissions()
        } else {
            startVideoFlow()
        }
    }

    private fun startSettings() {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                val uri = Uri.fromParts("package", packageName, null)
                data = uri
            }
        )
    }

    private fun startVideoFlow() {
        val isInitialized = callViewModel.isVideoInitialized.value
        if (isInitialized) return

        val client = VideoApp.videoClient
        val callId = intent.getStringExtra(KEY_CALL_ID) ?: return
        val participants = intent.getStringArrayExtra(KEY_PARTICIPANTS) ?: emptyArray()

        lifecycleScope.launch {
            val result = client.joinCall(
                "video",
                id = callId,
                participantIds = participants.toList()
            )

            result.onSuccessSuspend { response -> connectToRoom(response) }

            result.onError {
                Log.d("Couldn't select server", it.message ?: "")
            }
        }
    }

    private fun connectToRoom(response: SelectEdgeServerResponse) {
        val server = response.edge_server ?: return
        val token = response.token

        val url = enrichUrl(server.url)

        val room = LiveKit.create(
            applicationContext,
            RoomOptions()
        )

        callViewModel.init(room, url, token)
    }

    private fun enrichUrl(url: String): String {
        if (url.startsWith("wss://")) return url

        return "wss://$url"
    }

    @RequiresApi(M)
    private fun checkPermissions() {
        val missing = getMissingPermissions()

        if (missing.isNotEmpty()) {
            requestPermissions(missing)
        } else {
            startVideoFlow()
        }
    }

    @RequiresApi(M)
    private fun requestPermissions(permissions: Array<out String>) {
        val deniedCamera = !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
        val deniedMicrophone =
            !shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)

        if (!deniedCamera && !deniedMicrophone) {
            permissionsContract.launch(permissions.first())
        } else {
            showPermissionsDialog()
        }
    }

    @RequiresApi(M)
    private fun getMissingPermissions(): Array<out String> {
        val permissionsToCheck = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        val missing = permissionsToCheck
            .map { it to (checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED) }
            .filter { (_, isGranted) -> !isGranted }
            .map { it.first }

        return missing.toTypedArray()
    }

    private fun showPermissionsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permissions required to launch the app")
            .setMessage("Open settings to allow camera and microphone permissions.")
            .setPositiveButton("Launch settings") { dialog, _ ->
                startSettings()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                finish()
                dialog.dismiss()
            }
            .create()
            .show()
    }

    companion object {
        private const val KEY_CALL_ID = "call_id"
        private const val KEY_PARTICIPANTS = "participants"

        internal fun getIntent(
            context: Context,
            callId: String,
            participants: List<String>
        ): Intent {
            return Intent(context, CallActivity::class.java).apply {
                putExtra(KEY_CALL_ID, callId)
                putExtra(KEY_PARTICIPANTS, participants.toTypedArray())
            }
        }
    }
}
