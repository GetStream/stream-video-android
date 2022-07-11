package io.getstream.video.android

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import stream.video.SelectEdgeServerRequest
import utils.onError
import utils.onSuccess

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val client = VideoApp.videoClient

        lifecycleScope.launchWhenCreated {
            val result = client.selectEdgeServer(
                SelectEdgeServerRequest(
                    call_id = "testroom",
                    user_id = "filbabic"
                )
            )

            result.onSuccess { response ->
                val server = response.edge_server

                Log.d("selectResponse", server?.url ?: "")
            }

            result.onError {
                Log.d("selectResponse", it.message ?: "")
            }
        }
    }
}