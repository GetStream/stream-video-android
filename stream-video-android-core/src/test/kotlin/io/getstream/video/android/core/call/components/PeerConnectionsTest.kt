/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.call.components

import com.google.common.truth.Truth.assertThat
import io.getstream.video.android.core.call.connection.Publisher
import io.getstream.video.android.core.call.connection.Subscriber
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

internal class PeerConnectionsTest {

    private val peerConnections = PeerConnections()

    @Test
    fun `both connections start out empty`() {
        assertThat(peerConnections.publisher.value).isNull()
        assertThat(peerConnections.subscriber.value).isNull()
    }

    @Test
    fun `connections are readable once set`() {
        val publisher = mockk<Publisher>(relaxed = true)
        val subscriber = mockk<Subscriber>(relaxed = true)

        peerConnections.setPublisher(publisher)
        peerConnections.setSubscriber(subscriber)

        assertThat(peerConnections.publisher.value).isSameInstanceAs(publisher)
        assertThat(peerConnections.subscriber.value).isSameInstanceAs(subscriber)
    }

    @Test
    fun `closing clears the subscriber tracks, closes both connections and drops them`() {
        val publisher = mockk<Publisher>(relaxed = true)
        val subscriber = mockk<Subscriber>(relaxed = true)
        peerConnections.setPublisher(publisher)
        peerConnections.setSubscriber(subscriber)

        peerConnections.close()

        verify(exactly = 1) { subscriber.clear() }
        verify(exactly = 1) { subscriber.close() }
        verify(exactly = 1) { publisher.close(true) }
        assertThat(peerConnections.publisher.value).isNull()
        assertThat(peerConnections.subscriber.value).isNull()
    }

    @Test
    fun `closing twice is safe`() {
        peerConnections.setPublisher(mockk(relaxed = true))
        peerConnections.setSubscriber(mockk(relaxed = true))

        peerConnections.close()
        peerConnections.close()

        assertThat(peerConnections.publisher.value).isNull()
        assertThat(peerConnections.subscriber.value).isNull()
    }

    @Test
    fun `a failing close still drops both connections`() {
        val publisher = mockk<Publisher> {
            io.mockk.every { close(any()) } throws IllegalStateException("already disposed")
        }
        peerConnections.setPublisher(publisher)
        peerConnections.setSubscriber(mockk(relaxed = true))

        peerConnections.close()

        assertThat(peerConnections.publisher.value).isNull()
        assertThat(peerConnections.subscriber.value).isNull()
    }
}
