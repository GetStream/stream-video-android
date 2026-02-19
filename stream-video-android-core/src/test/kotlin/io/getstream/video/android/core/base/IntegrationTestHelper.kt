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

package io.getstream.video.android.core.base

import android.content.Context
import android.media.AudioAttributes
import androidx.test.core.app.ApplicationProvider
import io.getstream.video.android.model.User

public class IntegrationTestHelper {

    val users = mutableMapOf<String, User>()
    val tokens = mutableMapOf<String, String>()
    val context: Context

    val expiredToken =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiNW9STEJjMXQiLCJpc3MiOiJodHRwczovL3Byb250by5nZXRzdHJlYW0uaW8iLCJzdWIiOiJ1c2VyLzVvUkxCYzF0IiwiaWF0IjoxNzExNTIzNzkxLCJleHAiOjE3MTIxMjg1OTZ9.VzKQ1aMsL5LnJb6xHdV3stBfqTo-GU57FXLmrF4xpkI"

    val fakeSDP = """
        v=0
        o=Node 1 1 IN IP4 172.30.8.37
        s=Stream1
        t=0 0
        m=audio 5004 RTP/AVP 96
        c=IN IP4 239.30.22.1
        a=rtpmap:96 L24/48000/2
        a=recvonly
        a=ptime:1
        a=ts-refclk:ptp=IEEE1588-2008:01-23-45-67-89-AB-CD-EF:127
        a=mediaclk:direct=0
        a=sync-time:0
    """.trimIndent()

    val callConfigLegacyRunService = false
    val callConfigLegacyAudioUsage = AudioAttributes.USAGE_MEDIA
    val callConfigRegistryRunService = true
    val callConfigRegistryAudioUsage = AudioAttributes.USAGE_VOICE_COMMUNICATION

    init {
        // TODO: generate token from build vars
        val token =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoidGhpZXJyeSJ9._4aZL6BR0VGKfZsKYdscsBm8yKVgG-2LatYeHRJUq0g"

        val thierry = User(
            id = "thierry",
            role = "admin",
            name = "Thierry",
            image = "hello",
            teams = emptyList(),
            custom = mapOf(),
        )
        users["thierry"] = thierry
        users["tommaso"] = User(
            id = "tommaso", role = "admin", name = "Tommaso", image = "hello",
            teams = emptyList(), custom = mapOf(),
        )
        users["jaewoong"] = User(
            id = "jaewoong", role = "admin", name = "Jaewoong", image = "hello",
            teams = emptyList(), custom = mapOf(),
        )
        tokens["thierry"] = token
        context = ApplicationProvider.getApplicationContext()
    }
}
