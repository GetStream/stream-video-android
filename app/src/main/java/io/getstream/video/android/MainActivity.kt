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
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.track.Track
import io.livekit.android.room.track.VideoTrack
import kotlinx.coroutines.launch
import stream.video.SelectEdgeServerRequest
import stream.video.SelectEdgeServerResponse

class MainActivity : AppCompatActivity() {

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
                    user_id = "filbabic"
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

        room.connect(
            url = url,
            token = token,
            options = ConnectOptions()
        )

        room.localParticipant.setCameraEnabled(true)
        room.localParticipant.setMicrophoneEnabled(true)

        lifecycleScope.launch {
            room.events.collect { event ->
                when (event) {
                    is RoomEvent.Reconnecting -> {}
                    is RoomEvent.Reconnected -> {}
                    is RoomEvent.Disconnected -> {}
                    is RoomEvent.ParticipantConnected -> {
                        Log.d("henloParticipant", "frens")
                    }
                    is RoomEvent.ParticipantDisconnected -> {}
                    is RoomEvent.ActiveSpeakersChanged -> {}
                    is RoomEvent.RoomMetadataChanged -> {}
                    is RoomEvent.ParticipantMetadataChanged -> {}
                    is RoomEvent.TrackMuted -> {}
                    is RoomEvent.TrackUnmuted -> {}
                    is RoomEvent.TrackPublished -> {
                        Log.d("henloPublished", "frens")
                        val track = event.publication.track
                        renderVideo(track)
                    }
                    is RoomEvent.TrackUnpublished -> {}
                    is RoomEvent.TrackSubscribed -> {
                        Log.d("henloSubscribed", "frens")
                        renderVideo(event.track)
                    }
                    is RoomEvent.TrackSubscriptionFailed -> {}
                    is RoomEvent.TrackUnsubscribed -> {}
                    is RoomEvent.TrackStreamStateChanged -> {}
                    is RoomEvent.TrackSubscriptionPermissionChanged -> {}
                    is RoomEvent.DataReceived -> {}
                    is RoomEvent.ConnectionQualityChanged -> {}
                    is RoomEvent.FailedToConnect -> {}
                    is RoomEvent.ParticipantPermissionsChanged -> {
                    }
                }
            }
        }
    }

    private fun renderVideo(track: Track?) {
        val video = track as? VideoTrack
        if (video != null) {
            video.addRenderer(binding.rendererView)
        }
    }

    private fun enrichUrl(url: String): String {
        if (url.startsWith("wss://")) return url

        return "wss://$url"
    }
}