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

package io.getstream.video.android.core.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EnableStereoTest {

    private val offerSdp = """o=- 5087842825318906515 1750254551 IN IP4 127.0.0.1
s=-
t=0 0
a=group:BUNDLE 0 1
a=extmap-allow-mixed
a=msid-semantic: WMS a1e5f21f716affb7:TRACK_TYPE_AUDIO:eO a1e5f21f716affb7:TRACK_TYPE_VIDEO:Nk
a=ice-lite
m=audio 51808 UDP/TLS/RTP/SAVPF 111 63
a=rtcp:51808 IN IP4 18.119.157.125
a=candidate:965407073 1 udp 2130706431 18.119.157.125 51808 typ host generation 0
a=candidate:965407073 2 udp 2130706431 18.119.157.125 51808 typ host generation 0
a=ice-ufrag:AFJnYPvMfEaZeHdt
a=ice-pwd:mbwUwrcoSApXwOyGrQOAsipWsfFGHcww
a=fingerprint:sha-256 13:79:52:41:12:BB:A7:5D:39:F0:9B:1A:95:58:94:D6:F9:D3:1E:00:A4:9D:CA:12:26:AE:7C:2A:E1:FC:42:F4
a=setup:actpass
a=mid:0
a=sendrecv
a=msid:a1e5f21f716affb7:TRACK_TYPE_AUDIO:eO 5040549b-8458-4646-892d-ad08f4475568
a=rtcp-mux
a=rtcp-rsize
a=rtpmap:111 opus/48000/2
a=fmtp:111 maxaveragebitrate=510000;minptime=10;sprop-stereo=1;stereo=1;useinbandfec=1
a=rtpmap:63 red/48000/2
a=fmtp:63 111/111
a=ssrc:1826085709 cname:a1e5f21f716affb7:TRACK_TYPE_AUDIO:eO
a=ssrc:1826085709 msid:a1e5f21f716affb7:TRACK_TYPE_AUDIO:eO 5040549b-8458-4646-892d-ad08f4475568
m=video 9 UDP/TLS/RTP/SAVPF 96 97
a=rtcp:9 IN IP4 0.0.0.0
a=ice-ufrag:AFJnYPvMfEaZeHdt
a=ice-pwd:mbwUwrcoSApXwOyGrQOAsipWsfFGHcww
a=fingerprint:sha-256 13:79:52:41:12:BB:A7:5D:39:F0:9B:1A:95:58:94:D6:F9:D3:1E:00:A4:9D:CA:12:26:AE:7C:2A:E1:FC:42:F4
a=setup:actpass
a=mid:1
a=extmap:3 http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01
a=extmap:1 https://aomediacodec.github.io/av1-rtp-spec/#dependency-descriptor-rtp-header-extension
a=extmap:2 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time
a=sendrecv
a=msid:a1e5f21f716affb7:TRACK_TYPE_VIDEO:Nk 40c4a11d-fa67-49b6-a4fa-1625b4bde4a5
a=rtcp-mux
a=rtcp-rsize
a=rtpmap:96 VP8/90000
a=rtcp-fb:96 ccm fir
a=rtcp-fb:96 nack
a=rtcp-fb:96 nack pli
a=rtcp-fb:96 goog-remb
a=rtcp-fb:96 transport-cc
a=rtpmap:97 rtx/90000
a=fmtp:97 apt=96
a=ssrc-group:FID 3718708632 2750164767
a=ssrc:3718708632 cname:a1e5f21f716affb7:TRACK_TYPE_VIDEO:Nk
a=ssrc:3718708632 msid:a1e5f21f716affb7:TRACK_TYPE_VIDEO:Nk 40c4a11d-fa67-49b6-a4fa-1625b4bde4a5
a=ssrc:2750164767 cname:a1e5f21f716affb7:TRACK_TYPE_VIDEO:Nk
a=ssrc:2750164767 msid:a1e5f21f716affb7:TRACK_TYPE_VIDEO:Nk 40c4a11d-fa67-49b6-a4fa-1625b4bde4a5"""

    private val answerSdp = """v=0
o=- 6793916097087762106 2 IN IP4 127.0.0.1
s=-
t=0 0
a=group:BUNDLE 0 1
a=extmap-allow-mixed
a=msid-semantic: WMS
m=audio 9 UDP/TLS/RTP/SAVPF 111 63
c=IN IP4 0.0.0.0
a=rtcp:9 IN IP4 0.0.0.0
a=ice-ufrag:zcgT
a=ice-pwd:v559SXDwx4y9yAv7oeCvjsDR
a=ice-options:trickle
a=fingerprint:sha-256 F7:C8:B3:87:4A:AD:5A:86:48:1B:51:04:BE:CE:3B:D6:D3:7C:25:63:3E:9C:2B:F6:5B:8C:65:1F:72:8A:11:61
a=setup:active
a=mid:0
a=recvonly
a=rtcp-mux
a=rtcp-rsize
a=rtpmap:111 opus/48000/2
a=fmtp:111 minptime=10;useinbandfec=1
a=rtpmap:63 red/48000/2
a=fmtp:63 111/111
m=video 9 UDP/TLS/RTP/SAVPF 96 97
c=IN IP4 0.0.0.0
a=rtcp:9 IN IP4 0.0.0.0
a=ice-ufrag:zcgT
a=ice-pwd:v559SXDwx4y9yAv7oeCvjsDR
a=ice-options:trickle
a=fingerprint:sha-256 F7:C8:B3:87:4A:AD:5A:86:48:1B:51:04:BE:CE:3B:D6:D3:7C:25:63:3E:9C:2B:F6:5B:8C:65:1F:72:8A:11:61
a=setup:active
a=mid:1
a=extmap:2 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time
a=extmap:3 http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01
a=extmap:1 https://aomediacodec.github.io/av1-rtp-spec/#dependency-descriptor-rtp-header-extension
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
a=fmtp:97 apt=96"""

    @Test
    fun `should enable stereo in answer SDP if offered in offer SDP`() {
        val result = enableStereo(offerSdp, answerSdp)
        assertTrue(result.contains("a=fmtp:111 minptime=10;useinbandfec=1;stereo=1"))
    }

    @Test
    fun `should not modify answer SDP if stereo was not offered`() {
        val modifiedOffer = offerSdp.replace("stereo=1", "stereo=0")
        val result = enableStereo(modifiedOffer, answerSdp)
        assertEquals(answerSdp, result)
    }
}
