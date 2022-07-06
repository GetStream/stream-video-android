package io.getstream.video.android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.squareup.wire.GrpcClient
import okhttp3.OkHttpClient
import okhttp3.Protocol
import stream.video.GrpcCallCoordinatorServiceClient

class MainActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    val grpcClient = GrpcClient.Builder()
      .client(OkHttpClient.Builder().protocols(listOf(Protocol.H2_PRIOR_KNOWLEDGE)).build())
      .baseUrl("serverUrl")
      .build()

    // TODO - access the API from here
    val client = grpcClient.create(GrpcCallCoordinatorServiceClient::class)
  }
}