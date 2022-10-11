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

package io.getstream.video.android.webrtc.datachannel

import io.getstream.video.android.events.SfuDataEvent
import okio.ByteString
import org.webrtc.DataChannel
import stream.video.sfu.event.SfuEvent

public class StreamDataChannel(
    private val dataChannel: DataChannel,
    private val onMessage: (SfuDataEvent) -> Unit,
    private val onStateChange: (DataChannel.State) -> Unit
) : DataChannel.Observer {

    init {
        dataChannel.registerObserver(this)
    }

    override fun onBufferedAmountChange(p0: Long): Unit = Unit
    override fun onStateChange(): Unit = onStateChange(dataChannel.state())

    override fun onMessage(buffer: DataChannel.Buffer?) {
        val bytes = buffer?.data ?: return
        val byteArray = ByteArray(bytes.capacity())
        bytes.get(byteArray)

        try {
            this.onMessage(RTCEventMapper.mapEvent(SfuEvent.ADAPTER.decode(byteArray)))
        } catch (error: Throwable) {
            error.printStackTrace()
        }
    }

    public fun send(data: ByteString): Boolean {
        return dataChannel.send(
            DataChannel.Buffer(
                data.asByteBuffer(),
                true
            )
        )
    }
}
