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

package io.getstream.video.android.module

import io.getstream.video.android.webrtc.signal.LocalSignalService
import io.getstream.video.android.webrtc.signal.RemoteSignalService
import io.getstream.video.android.webrtc.signal.SignalClient
import io.getstream.video.android.webrtc.signal.SignalClientImpl
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.wire.WireConverterFactory

internal class WebRTCModule(
    private val okHttpClient: OkHttpClient,
    private val signalUrl: String
) {

    private val signalRetrofitClient: Retrofit by lazy {
        Retrofit.Builder()
            .client(okHttpClient)
            .addConverterFactory(WireConverterFactory.create())
            .baseUrl(signalUrl)
            .build()
    }

    internal val signalClient: SignalClient by lazy {
        val targetService = if (REDIRECT_SIGNAL_URL != null) {
            LocalSignalService::class.java
        } else {
            RemoteSignalService::class.java
        }

        val service = signalRetrofitClient.create(targetService)

        SignalClientImpl(service)
    }

    companion object {
        @Suppress("RedundantNullableReturnType")
        val REDIRECT_SIGNAL_URL: String? = "https://2c42-93-140-102-188.eu.ngrok.io" // "https://6dd4-78-1-28-238.eu.ngrok.io"

        internal const val SIGNAL_HOST_BASE: String =
            "10.0.2.2:3031" // "sfu2.fra1.gtstrm.com"

        const val SIGNAL_BASE_URL = "http://$SIGNAL_HOST_BASE/"
    }
}
