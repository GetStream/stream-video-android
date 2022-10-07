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

import io.getstream.logging.StreamLog
import io.getstream.video.android.events.SfuDataEvent
import okio.ByteString
import org.webrtc.DataChannel
import stream.video.sfu.SfuEvent

public class StreamDataChannel(
    private val dataChannel: DataChannel,
    private val onMessage: (SfuDataEvent) -> Unit,
    private val onStateChange: (DataChannel.State) -> Unit
) : DataChannel.Observer {

    private val logger = StreamLog.getLogger("Call:SFU-Events")

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
            val rawEvent = SfuEvent.ADAPTER.decode(byteArray)
            logger.v { "[onMessage] rawEvent: $rawEvent" }
            val message = RTCEventMapper.mapEvent(rawEvent)
            this.onMessage(message)
        } catch (error: Throwable) {
            logger.e { "[onMessage] failed: $error" }
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
