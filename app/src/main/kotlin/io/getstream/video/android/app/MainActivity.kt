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

package io.getstream.video.android.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES.M
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.getstream.video.android.app.databinding.ActivityMainBinding
import io.getstream.video.android.utils.onError
import io.getstream.video.android.utils.onSuccessSuspend
import io.livekit.android.ConnectOptions
import io.livekit.android.LiveKit
import io.livekit.android.RoomOptions
import io.livekit.android.room.RoomListener
import io.livekit.android.room.track.VideoTrack
import kotlinx.coroutines.launch
import stream.video.SelectEdgeServerRequest
import stream.video.SelectEdgeServerResponse


class MainActivity : AppCompatActivity(), RoomListener {

    private lateinit var binding: ActivityMainBinding
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


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
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            val uri = Uri.fromParts("package", packageName, null)
            data = uri
        })
    }

    private fun startVideoFlow() {
        if (hasInitializedVideo) return

        val client = VideoApp.videoClient

        lifecycleScope.launch {
            val result = client.selectEdgeServer(
                SelectEdgeServerRequest(
                    call_id = "testroom",
                )
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
        val participant = room.localParticipant

        participant.setCameraEnabled(true)
        participant.setMicrophoneEnabled(true)

        val videoTrack = participant.videoTracks.firstOrNull()?.second as? VideoTrack ?: return

        // we need to connect both the room to the renderer and the track, to show something
        room.initVideoRenderer(binding.rendererView)
        videoTrack.addRenderer(binding.rendererView)
        hasInitializedVideo = true
    }

    private fun enrichUrl(url: String): String {
        if (url.startsWith("wss://")) return url

        return "wss://$url"
    }

    companion object {
        private const val CODE_PERMISSIONS = 101
    }
}
