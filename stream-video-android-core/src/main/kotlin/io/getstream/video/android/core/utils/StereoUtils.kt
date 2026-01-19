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

/**
 * Enable stereo for the audio track in the answer SDP if the offer SDP contains stereo=1 for opus.
 * In case the parsing of SDP fails the answer SDP is returned as is.
 *
 * @param offerSdp The offer SDP.
 * @param answerSdp The answer SDP.
 * @return The modified answer SDP.
 */
internal fun enableStereo(
    offerSdp: String,
    answerSdp: String,
): String = safeCallWithDefault(answerSdp) {
    val offeredStereoMids = mutableSetOf<String>()
    val offerLines = offerSdp.lines()

    var currentMid: String? = null
    var currentPayload: String? = null
    var inAudio = false

    // Step 1: Collect mids from the offer that support stereo
    for (line in offerLines) {
        when {
            line.startsWith("m=audio") -> {
                inAudio = true
                currentMid = null
                currentPayload = null
            }
            line.startsWith("m=") -> {
                inAudio = false
                currentMid = null
                currentPayload = null
            }
            inAudio && line.startsWith("a=mid:") -> {
                currentMid = line.removePrefix("a=mid:")
            }
            inAudio && line.startsWith("a=rtpmap:") && "opus" in line -> {
                currentPayload = line.substringAfter(":").substringBefore(" ")
            }
            inAudio && currentPayload != null &&
                line.startsWith("a=fmtp:$currentPayload") && "stereo=1" in line -> {
                currentMid?.let { offeredStereoMids.add(it) }
            }
        }
    }

    if (offeredStereoMids.isEmpty()) return answerSdp

    val result = buildString {
        val answerLines = answerSdp.lines()
        var currentMid: String? = null
        var currentPayload: String? = null
        var inAudio = false

        for (line in answerLines) {
            when {
                line.startsWith("m=audio") -> {
                    inAudio = true
                    currentMid = null
                    currentPayload = null
                    appendLine(line)
                }
                line.startsWith("m=") -> {
                    inAudio = false
                    currentMid = null
                    currentPayload = null
                    appendLine(line)
                }
                inAudio && line.startsWith("a=mid:") -> {
                    currentMid = line.removePrefix("a=mid:")
                    appendLine(line)
                }
                inAudio && line.startsWith("a=rtpmap:") && "opus" in line -> {
                    currentPayload = line.substringAfter(":").substringBefore(" ")
                    appendLine(line)
                }
                inAudio && currentMid in offeredStereoMids &&
                    currentPayload != null && line.startsWith("a=fmtp:$currentPayload") -> {
                    if ("stereo=1" !in line) {
                        appendLine("$line;stereo=1")
                    } else {
                        appendLine(line)
                    }
                }
                else -> appendLine(line)
            }
        }
    }

    return result.trimEnd().plus("\n")
}
