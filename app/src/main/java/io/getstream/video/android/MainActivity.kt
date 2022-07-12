/*
 * Copyright 2022 Stream.IO, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.getstream.video.android

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.getstream.video.android.databinding.ActivityMainBinding
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
    }

    private fun enrichUrl(url: String): String {
        if (url.startsWith("wss://")) return url

        return "wss://$url"
    }
}
