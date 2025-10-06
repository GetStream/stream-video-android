/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core

import com.google.common.truth.Truth.assertThat
import io.getstream.video.android.core.base.TestBase
import io.getstream.video.android.core.utils.MinimalSdpParser
import io.getstream.video.android.core.utils.mangleSdpUtil
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import io.getstream.webrtc.SessionDescription

@RunWith(RobolectricTestRunner::class)
class SdpTest : TestBase() {
    // see https://github.com/GetStream/stream-video-swift/blob/main/StreamVideoTests/Utils/StringExtensions_Tests.swift

    val first = """
        v=0
        o=- 4752151233443144455 2 IN IP4 127.0.0.1
        s=-
        t=0 0
        a=group:BUNDLE 0 1
        a=extmap-allow-mixed
        a=msid-semantic: WMS
        m=video 9 UDP/TLS/RTP/SAVPF 97 98 99 35 36 100 101 125 124 127 37 96
        c=IN IP4 0.0.0.0
        a=rtcp:9 IN IP4 0.0.0.0
        a=ice-ufrag:OSLx
        a=ice-pwd:FLyse6qHso1wtUy178VvrtV6
        a=ice-options:trickle renomination
        a=fingerprint:sha-256 2D:46:BC:2A:08:5C:FB:5E:7B:14:A1:7E:42:B8:E0:EB:0C:E0:60:C5:78:B7:84:88:F5:C2:8C:AE:B6:E1:E7:C5
        a=setup:actpass
        a=mid:0
        a=extmap:1 urn:ietf:params:rtp-hdrext:toffset
        a=extmap:2 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time
        a=extmap:3 urn:3gpp:video-orientation
        a=extmap:4 http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01
        a=extmap:5 http://www.webrtc.org/experiments/rtp-hdrext/playout-delay
        a=extmap:6 http://www.webrtc.org/experiments/rtp-hdrext/video-content-type
        a=extmap:7 http://www.webrtc.org/experiments/rtp-hdrext/video-timing
        a=extmap:8 http://www.webrtc.org/experiments/rtp-hdrext/color-space
        a=extmap:9 urn:ietf:params:rtp-hdrext:sdes:mid
        a=extmap:10 urn:ietf:params:rtp-hdrext:sdes:rtp-stream-id
        a=extmap:11 urn:ietf:params:rtp-hdrext:sdes:repaired-rtp-stream-id
        a=recvonly
        a=rtcp-mux
        a=rtcp-rsize
        a=rtpmap:96 VP8/90000
        a=rtcp-fb:96 goog-remb
        a=rtcp-fb:96 transport-cc
        a=rtcp-fb:96 ccm fir
        a=rtcp-fb:96 nack
        a=rtcp-fb:96 nack pli
        a=rtpmap:97 rtx/90000
        a=fmtp:97 apt=96
        a=rtpmap:98 VP9/90000
        a=rtcp-fb:98 goog-remb
        a=rtcp-fb:98 transport-cc
        a=rtcp-fb:98 ccm fir
        a=rtcp-fb:98 nack
        a=rtcp-fb:98 nack pli
        a=rtpmap:99 rtx/90000
        a=fmtp:99 apt=98
        a=rtpmap:35 AV1/90000
        a=rtcp-fb:35 goog-remb
        a=rtcp-fb:35 transport-cc
        a=rtcp-fb:35 ccm fir
        a=rtcp-fb:35 nack
        a=rtcp-fb:35 nack pli
        a=rtpmap:36 rtx/90000
        a=fmtp:36 apt=35
        a=rtpmap:100 H264/90000
        a=rtcp-fb:100 goog-remb
        a=rtcp-fb:100 transport-cc
        a=rtcp-fb:100 ccm fir
        a=rtcp-fb:100 nack
        a=rtcp-fb:100 nack pli
        a=fmtp:100 level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=42e01f
        a=rtpmap:101 rtx/90000
        a=fmtp:101 apt=100
        a=rtpmap:125 red/90000
        a=rtpmap:124 rtx/90000
        a=fmtp:124 apt=125
        a=rtpmap:127 ulpfec/90000
        a=rtpmap:37 flexfec-03/90000
        a=rtcp-fb:37 goog-remb
        a=rtcp-fb:37 transport-cc
        a=fmtp:37 repair-window=10000000
        m=audio 9 UDP/TLS/RTP/SAVPF 111 63 103 104 9 102 0 8 106 105 13 110 112 113 126
        c=IN IP4 0.0.0.0
        a=rtcp:9 IN IP4 0.0.0.0
        a=ice-ufrag:OSLx
        a=ice-pwd:FLyse6qHso1wtUy178VvrtV6
        a=ice-options:trickle renomination
        a=fingerprint:sha-256 2D:46:BC:2A:08:5C:FB:5E:7B:14:A1:7E:42:B8:E0:EB:0C:E0:60:C5:78:B7:84:88:F5:C2:8C:AE:B6:E1:E7:C5
        a=setup:actpass
        a=mid:1
        a=extmap:14 urn:ietf:params:rtp-hdrext:ssrc-audio-level
        a=extmap:2 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time
        a=extmap:4 http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01
        a=extmap:9 urn:ietf:params:rtp-hdrext:sdes:mid
        a=recvonly
        a=rtcp-mux
        a=rtpmap:111 opus/48000/2
        a=rtcp-fb:111 transport-cc
        a=fmtp:111 minptime=10;useinbandfec=1
        a=rtpmap:63 red/48000/2
        a=fmtp:63 111/111
        a=rtpmap:103 ISAC/16000
        a=rtpmap:104 ISAC/32000
        a=rtpmap:9 G722/8000
        a=rtpmap:102 ILBC/8000
        a=rtpmap:0 PCMU/8000
        a=rtpmap:8 PCMA/8000
        a=rtpmap:106 CN/32000
        a=rtpmap:105 CN/16000
        a=rtpmap:13 CN/8000
        a=rtpmap:110 telephone-event/48000
        a=rtpmap:112 telephone-event/32000
        a=rtpmap:113 telephone-event/16000
        a=rtpmap:126 telephone-event/8000
    """.trimIndent()

    val second = """
        v=0
        o=- 8341293427282787427 2 IN IP4 127.0.0.1
        s=-
        t=0 0
        a=group:BUNDLE 0
        a=extmap-allow-mixed
        a=msid-semantic: WMS 2bad0712-c1b8-423c-9f5d-6479ed74b58b
        m=audio 9 UDP/TLS/RTP/SAVPF 111 63 103 104 9 102 0 8 106 105 13 110 112 113 126
        c=IN IP4 0.0.0.0
        a=rtcp:9 IN IP4 0.0.0.0
        a=ice-ufrag:Vocj
        a=ice-pwd:zZEvVEk1C+gBvRVhLhrpjrh0
        a=ice-options:trickle renomination
        a=fingerprint:sha-256 48:DB:98:69:27:A5:AC:0C:7B:20:C5:5B:07:36:7E:6A:8C:96:0F:79:A3:A8:20:62:07:7E:4B:C6:AE:68:D0:51
        a=setup:actpass
        a=mid:0
        a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level
        a=extmap:2 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time
        a=extmap:3 http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01
        a=extmap:4 urn:ietf:params:rtp-hdrext:sdes:mid
        a=sendonly
        a=msid:2bad0712-c1b8-423c-9f5d-6479ed74b58b audioTrack
        a=rtcp-mux
        a=rtpmap:111 opus/48000/2
        a=rtcp-fb:111 transport-cc
        a=fmtp:111 minptime=10;useinbandfec=1
        a=rtpmap:63 red/48000/2
        a=fmtp:63 111/111
        a=rtpmap:103 ISAC/16000
        a=rtpmap:104 ISAC/32000
        a=rtpmap:9 G722/8000
        a=rtpmap:102 ILBC/8000
        a=rtpmap:0 PCMU/8000
        a=rtpmap:8 PCMA/8000
        a=rtpmap:106 CN/32000
        a=rtpmap:105 CN/16000
        a=rtpmap:13 CN/8000
        a=rtpmap:110 telephone-event/48000
        a=rtpmap:112 telephone-event/32000
        a=rtpmap:113 telephone-event/16000
        a=rtpmap:126 telephone-event/8000
        a=ssrc:721677896 cname:xNido+CxbbwlEwGG
        a=ssrc:721677896 msid:2bad0712-c1b8-423c-9f5d-6479ed74b58b audioTrack
    """.trimIndent()

    /**
     * For DTX
     *             updatedSdp = initialOffer.sdp.replacingOccurrences(
     of: "useinbandfec=1",
     with: "useinbandfec=1;usedtx=1"
     )
     */

    val sdp1 = SessionDescription(SessionDescription.Type.OFFER, first)
    val sdp2 = SessionDescription(SessionDescription.Type.OFFER, second)

    @Test
    fun `test parser`() = runTest {
        // enabling dtx is easy
        val result = MinimalSdpParser(sdp1.description)
    }

    @Test
    fun `enable dtx and opus red`() = runTest {
        // enabling dtx is easy
        val new = mangleSdpUtil(sdp1, true, true)
        assertThat(new.description).contains("useinbandfec=1;usedtx=1")
        println(new.description)
    }
}
