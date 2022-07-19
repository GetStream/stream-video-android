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

package io.getstream.video.android.app.ui.main

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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import io.getstream.video.android.app.VideoApp
import io.getstream.video.android.ui.components.MainStage
import io.getstream.video.android.ui.components.ParticipantsList
import io.getstream.video.android.utils.onError
import io.getstream.video.android.utils.onSuccessSuspend
import io.livekit.android.ConnectOptions
import io.livekit.android.LiveKit
import io.livekit.android.RoomOptions
import io.livekit.android.room.Room
import io.livekit.android.room.RoomListener
import io.livekit.android.room.participant.Participant
import io.livekit.android.room.participant.RemoteParticipant
import io.livekit.android.room.track.Track
import io.livekit.android.room.track.TrackPublication
import io.livekit.android.room.track.VideoTrack
import kotlinx.coroutines.launch
import stream.video.SelectEdgeServerResponse

class MainActivity : AppCompatActivity(), RoomListener {

    private var hasInitializedVideo: Boolean = false

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

    /**
     * State.
     */
    // TODO - Expose through a ViewModel at some point
    private var room: MutableState<Room?> = mutableStateOf(null)
    private var videoTrack: MutableState<VideoTrack?> = mutableStateOf(null)
    private var participants: MutableState<List<Participant>> = mutableStateOf(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val track by videoTrack
            val room by room

            Column(modifier = Modifier.fillMaxSize()) {

                val currentTrack = track
                val currentRoom = room

                if (currentTrack != null && currentRoom != null) {
                    MainStage(
                        room = currentRoom,
                        track = currentTrack
                    )
                } else {
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
                }

                if (currentRoom != null) {
                    ParticipantsList(
                        room = currentRoom,
                        participants = participants.value
                    )
                }
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

    private fun startSettings() {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                val uri = Uri.fromParts("package", packageName, null)
                data = uri
            }
        )
    }

    private fun startVideoFlow() {
        if (hasInitializedVideo) return

        val client = VideoApp.videoClient
        val callId = intent.getStringExtra(KEY_CALL_ID) ?: return

        lifecycleScope.launch {
            val result = client.joinCall(
                "video",
                id = callId
            )

            result.onSuccessSuspend { response ->
                val server = response.edge_server
                connectToRoom(response)

                Log.d("selectResponse", server?.url ?: "")
            }

            result.onError {
                Log.d("selectResponse", it.message ?: "")
            }
        }
    }

    private suspend fun connectToRoom(response: SelectEdgeServerResponse) {
        val server = response.edge_server ?: return
        val token = response.token

        val url = enrichUrl(server.url)

        val room = LiveKit.create(
            applicationContext,
            RoomOptions()
        )
        room.listener = this@MainActivity

        room.connect(
            url = url,
            token = token,
            options = ConnectOptions(autoSubscribe = true)
        )
        this.room.value = room
        val participant = room.localParticipant

        participant.setCameraEnabled(true)
        participant.setMicrophoneEnabled(true)

        val videoTrack = participant.videoTracks.firstOrNull()?.second as? VideoTrack ?: return

        this.videoTrack.value = videoTrack
        hasInitializedVideo = true
    }

    private fun enrichUrl(url: String): String {
        if (url.startsWith("wss://")) return url

        return "wss://$url"
    }

    // TODO - implement better event handling inside a VM
    override fun onTrackSubscribed(
        track: Track,
        publication: TrackPublication,
        participant: RemoteParticipant,
        room: Room
    ) {
        super.onTrackSubscribed(track, publication, participant, room)
        val current = participants.value

        participants.value = (current + participant).distinctBy { it.sid }
    }

    override fun onTrackUnsubscribed(
        track: Track,
        publications: TrackPublication,
        participant: RemoteParticipant,
        room: Room
    ) {
        super.onTrackUnsubscribed(track, publications, participant, room)
        val current = participants.value

        participants.value = (current - participant).distinctBy { it.sid }
    }

    override fun onParticipantConnected(room: Room, participant: RemoteParticipant) {
        super.onParticipantConnected(room, participant)
        val current = participants.value

        participants.value = (current + participant).distinctBy { it.sid }
    }

    override fun onParticipantDisconnected(room: Room, participant: RemoteParticipant) {
        super.onParticipantDisconnected(room, participant)
        val current = participants.value

        participants.value = (current - participant).distinctBy { it.sid }
    }

    companion object {
        private const val KEY_CALL_ID = "call_id"

        public fun getIntent(context: Context, callId: String): Intent {
            return Intent(context, MainActivity::class.java).apply {
                putExtra(KEY_CALL_ID, callId)
            }
        }
    }
}
