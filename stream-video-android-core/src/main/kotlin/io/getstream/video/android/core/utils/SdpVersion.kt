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
 * This is a Kotlin port of the original (MIT licensed) located at
 * https://github.com/clux/sdp-transform. This port is from: https://github.com/ggarber/sdpparser
 */

data class SdpVersion(val value: Long)
data class SdpOrigin(
    val username: String,
    val sessionId: Long,
    val sessionVersion: Long,
    val netType: String,
    val ipVer: Long,
    val address: String,
)
data class SdpName(val value: String)
data class SdpDescription(val value: String)
data class SdpUri(val value: String)
data class SdpEmail(val value: String)
data class SdpPhone(val value: String)
data class SdpTimezones(val value: String)
data class SdpRepeats(val value: String)
data class SdpTiming(val start: Long, val stop: Long)
data class SdpConnection(val version: Long, val ip: String)
data class SdpBandwidth(val type: String, val limit: String)
data class SdpMline(val type: String, val port: Long, val protocol: String, val payloads: String)
data class SdpRtp(val payload: Long, val codec: String, val rate: Long?, val encoding: String?)
data class SdpFmtp(val payload: Long, val config: String)
data class SdpControl(val value: String)
data class SdpRtcp(val port: Long, val netType: String?, val ipVer: Long?, val address: String?)
data class SdpRtcpfbtrrint(val payload: String, val value: Long)
data class SdpRtcpfb(val payload: String, val type: String, val subtype: String?)
data class SdpExt(
    val value: Long,
    val direction: String?,
    val encryptUri: String?,
    val uri: String,
    val config: String?,
)
data class SdpExtmapallowmixed(val value: String)
data class SdpCrypto(
    val id: Long,
    val suite: String,
    val config: String,
    val sessionConfig: String?,
)
data class SdpSetup(val value: String)
data class SdpConnectiontype(val value: String)
data class SdpMid(val value: String)
data class SdpMsid(val value: String)
data class SdpPtime(val value: Long)
data class SdpMaxptime(val value: Long)
data class SdpDirection(val value: String)
data class SdpIcelite(val value: String)
data class SdpIceufrag(val value: String)
data class SdpIcepwd(val value: String)
data class SdpFingerprint(val type: String, val hash: String)
data class SdpCandidates(
    val foundation: String,
    val component: Long,
    val transport: String,
    val priority: Long,
    val ip: String,
    val port: Long,
    val type: String,
    val raddr: String?,
    val rport: Long?,
    val tcptype: String?,
    val generation: Long?,
    val networkId: Long?,
    val networkCost: Long?,
)
data class SdpEndofcandidates(val value: String)
data class SdpRemotecandidates(val value: String)
data class SdpIceoptions(val value: String)
data class SdpSsrcs(val id: String, val attribute: String?, val value: String?)
data class SdpSsrcgroups(val semantics: String, val ssrcs: String)
data class SdpMsidsemantic(val semantic: String, val token: String)
data class SdpGroups(val type: String, val mids: String)
data class SdpRtcpmux(val value: String)
data class SdpRtcprsize(val value: String)
data class SdpSctpmap(val sctpmapNumber: String)
data class SdpXgoogleflag(val value: String)
data class SdpRids(val id: String)
data class SdpImageattrs(val pt: String)
data class SdpSimulcast(val dir1: String)
data class SdpSimulcast03(val value: String)
data class SdpFramerate(val value: Long)
data class SdpSourcefilter(
    val filterMode: String,
    val netType: String,
    val addressTypes: String,
    val destAddress: String,
    val srcList: String,
)
data class SdpBundleonly(val value: String)
data class SdpLabel(val value: String)
data class SdpSctpport(val value: Long)
data class SdpMaxmessagesize(val value: Long)
data class SdpTsrefclocks(val clksrc: String)
data class SdpMediaclk(val id: String)
data class SdpKeywords(val value: String)
data class SdpContent(val value: String)
data class SdpBfcpfloorctrl(val value: String)
data class SdpBfcpconfid(val value: String)
data class SdpBfcpuserid(val value: String)
data class SdpBfcpfloorid(val id: String, val mStream: String)
data class SdpInvalid(val value: String)

fun toOptionalLong(str: String): Long? = if (str.isEmpty()) null else str.toLong()
fun toOptionalString(str: String): String? = str.ifEmpty { null }

operator fun MatchResult.Destructured.component11(): String = match.groupValues[11]
operator fun MatchResult.Destructured.component12(): String = match.groupValues[12]
operator fun MatchResult.Destructured.component13(): String = match.groupValues[13]

object SdpGrammar {

    private val VERSION = """(\d*)""".toRegex()
    fun tryParseVersion(string: String): SdpVersion? {
        val match = VERSION.matchEntire(string) ?: return null
        val (value) = match.destructured
        return SdpVersion(value.toLong())
    }
    fun writeVersion(item: SdpVersion): String {
        return "${item.value}"
    }

    private val ORIGIN = """(\S*) (\d*) (\d*) (\S*) IP(\d) (\S*)""".toRegex()
    fun tryParseOrigin(string: String): SdpOrigin? {
        val match = ORIGIN.matchEntire(string) ?: return null
        val (username, sessionId, sessionVersion, netType, ipVer, address) = match.destructured
        return SdpOrigin(
            username,
            sessionId.toLong(),
            sessionVersion.toLong(),
            netType,
            ipVer.toLong(),
            address,
        )
    }
    fun writeOrigin(item: SdpOrigin): String {
        return "${item.username} ${item.sessionId} ${item.sessionVersion} ${item.netType} IP${item.ipVer} ${item.address}"
    }

    private val NAME = """(.*)""".toRegex()
    fun tryParseName(string: String): SdpName? {
        val match = NAME.matchEntire(string) ?: return null
        val (value) = match.destructured
        return SdpName(value)
    }
    fun writeName(item: SdpName): String {
        return "${item.value}"
    }

    private val DESCRIPTION = """(.*)""".toRegex()
    fun tryParseDescription(string: String): SdpDescription? {
        val match = DESCRIPTION.matchEntire(string) ?: return null
        val (value) = match.destructured
        return SdpDescription(value)
    }
    fun writeDescription(item: SdpDescription): String {
        return "${item.value}"
    }

    private val URI = """(.*)""".toRegex()
    fun tryParseUri(string: String): SdpUri? {
        val match = URI.matchEntire(string) ?: return null
        val (value) = match.destructured
        return SdpUri(value)
    }
    fun writeUri(item: SdpUri): String {
        return "${item.value}"
    }

    private val EMAIL = """(.*)""".toRegex()
    fun tryParseEmail(string: String): SdpEmail? {
        val match = EMAIL.matchEntire(string) ?: return null
        val (value) = match.destructured
        return SdpEmail(value)
    }
    fun writeEmail(item: SdpEmail): String {
        return "${item.value}"
    }

    private val PHONE = """(.*)""".toRegex()
    fun tryParsePhone(string: String): SdpPhone? {
        val match = PHONE.matchEntire(string) ?: return null
        val (value) = match.destructured
        return SdpPhone(value)
    }
    fun writePhone(item: SdpPhone): String {
        return "${item.value}"
    }

    private val TIMEZONES = """(.*)""".toRegex()
    fun tryParseTimezones(string: String): SdpTimezones? {
        val match = TIMEZONES.matchEntire(string) ?: return null
        val (value) = match.destructured
        return SdpTimezones(value)
    }
    fun writeTimezones(item: SdpTimezones): String {
        return "${item.value}"
    }

    private val REPEATS = """(.*)""".toRegex()
    fun tryParseRepeats(string: String): SdpRepeats? {
        val match = REPEATS.matchEntire(string) ?: return null
        val (value) = match.destructured
        return SdpRepeats(value)
    }
    fun writeRepeats(item: SdpRepeats): String {
        return "${item.value}"
    }

    private val TIMING = """(\d*) (\d*)""".toRegex()
    fun tryParseTiming(string: String): SdpTiming? {
        val match = TIMING.matchEntire(string) ?: return null
        val (start, stop) = match.destructured
        return SdpTiming(start.toLong(), stop.toLong())
    }
    fun writeTiming(item: SdpTiming): String {
        return "${item.start} ${item.stop}"
    }

    private val CONNECTION = """IN IP(\d) (\S*)""".toRegex()
    fun tryParseConnection(string: String): SdpConnection? {
        val match = CONNECTION.matchEntire(string) ?: return null
        val (version, ip) = match.destructured
        return SdpConnection(version.toLong(), ip)
    }
    fun writeConnection(item: SdpConnection): String {
        return "IN IP${item.version} ${item.ip}"
    }

    private val BANDWIDTH = """(TIAS|AS|CT|RR|RS):(\d*)""".toRegex()
    fun tryParseBandwidth(string: String): SdpBandwidth? {
        val match = BANDWIDTH.matchEntire(string) ?: return null
        val (type, limit) = match.destructured
        return SdpBandwidth(type, limit)
    }
    fun writeBandwidth(item: SdpBandwidth): String {
        return "${item.type}:${item.limit}"
    }

    private val MLINE = """(\w*) (\d*) ([\w/]*)(?: (.*))?""".toRegex()
    fun tryParseMline(string: String): SdpMline? {
        val match = MLINE.matchEntire(string) ?: return null
        val (type, port, protocol, payloads) = match.destructured
        return SdpMline(type, port.toLong(), protocol, payloads)
    }
    fun writeMline(item: SdpMline): String {
        return "${item.type} ${item.port} ${item.protocol} ${item.payloads}"
    }

    private val RTP = """rtpmap:(\d*) ([\w\-.]*)(?:\s*/(\d*)(?:\s*/(\S*))?)?""".toRegex()
    fun tryParseRtp(string: String): SdpRtp? {
        val match = RTP.matchEntire(string) ?: return null
        val (payload, codec, rate, encoding) = match.destructured
        return SdpRtp(payload.toLong(), codec, toOptionalLong(rate), toOptionalString(encoding))
    }
    fun writeRtp(item: SdpRtp): String {
        return "rtpmap:${item.payload} ${item.codec}${if (item.rate != null) ("/" + item.rate) else ""}${if (item.encoding != null) ("/" + item.encoding) else ""}"
    }

    private val FMTP = """fmtp:(\d*) ([\S| ]*)""".toRegex()
    fun tryParseFmtp(string: String): SdpFmtp? {
        val match = FMTP.matchEntire(string) ?: return null
        val (payload, config) = match.destructured
        return SdpFmtp(payload.toLong(), config)
    }
    fun writeFmtp(item: SdpFmtp): String {
        return "fmtp:${item.payload} ${item.config}"
    }

    private val CONTROL = """control:(.*)""".toRegex()
    fun tryParseControl(string: String): SdpControl? {
        val match = CONTROL.matchEntire(string) ?: return null
        val (value) = match.destructured
        return SdpControl(value)
    }
    fun writeControl(item: SdpControl): String {
        return "control:${item.value}"
    }

    private val RTCP = """rtcp:(\d*)(?: (\S*) IP(\d) (\S*))?""".toRegex()
    fun tryParseRtcp(string: String): SdpRtcp? {
        val match = RTCP.matchEntire(string) ?: return null
        val (port, netType, ipVer, address) = match.destructured
        return SdpRtcp(
            port.toLong(),
            toOptionalString(netType),
            toOptionalLong(ipVer),
            toOptionalString(address),
        )
    }
    fun writeRtcp(item: SdpRtcp): String {
        return "rtcp:${item.port}${if (item.netType != null) (" " + item.netType) else ""}${if (item.ipVer != null) (" IP" + item.ipVer) else ""}${if (item.address != null) (" " + item.address) else ""}"
    }

    private val RTCPFBTRRINT = """rtcp-fb:(\*|\d*) trr-int (\d*)""".toRegex()
    fun tryParseRtcpfbtrrint(string: String): SdpRtcpfbtrrint? {
        val match = RTCPFBTRRINT.matchEntire(string) ?: return null
        val (payload, value) = match.destructured
        return SdpRtcpfbtrrint(payload, value.toLong())
    }
    fun writeRtcpfbtrrint(item: SdpRtcpfbtrrint): String {
        return "rtcp-fb:${item.payload} trr-int ${item.value}"
    }

    private val RTCPFB = """rtcp-fb:(\*|\d*) ([\w-_]*)(?: ([\w-_]*))?""".toRegex()
    fun tryParseRtcpfb(string: String): SdpRtcpfb? {
        val match = RTCPFB.matchEntire(string) ?: return null
        val (payload, type, subtype) = match.destructured
        return SdpRtcpfb(payload, type, toOptionalString(subtype))
    }
    fun writeRtcpfb(item: SdpRtcpfb): String {
        return "rtcp-fb:${item.payload} ${item.type}${if (item.subtype != null) (" " + item.subtype) else ""}"
    }

    private val EXT = """extmap:(\d+)(?:/(\w+))?(?: (urn:ietf:params:rtp-hdrext:encrypt))? (\S*)(?: (\S*))?""".toRegex()
    fun tryParseExt(string: String): SdpExt? {
        val match = EXT.matchEntire(string) ?: return null
        val (value, direction, encryptUri, uri, config) = match.destructured
        return SdpExt(
            value.toLong(),
            toOptionalString(direction),
            toOptionalString(encryptUri),
            uri,
            toOptionalString(config),
        )
    }
    fun writeExt(item: SdpExt): String {
        return "extmap:${item.value}${if (item.direction != null) ("/" + item.direction) else ""}${if (item.encryptUri != null) (" " + item.encryptUri) else ""} ${item.uri}${if (item.config != null) (" " + item.config) else ""}"
    }

    private val EXTMAPALLOWMIXED = """(extmap-allow-mixed)""".toRegex()
    fun tryParseExtmapallowmixed(string: String): SdpExtmapallowmixed? {
        val match = EXTMAPALLOWMIXED.matchEntire(string) ?: return null
        val (value) = match.destructured
        return SdpExtmapallowmixed(value)
    }
    fun writeExtmapallowmixed(item: SdpExtmapallowmixed): String {
        return "${item.value}"
    }

    private val CRYPTO = """crypto:(\d*) ([\w_]*) (\S*)(?: (\S*))?""".toRegex()
    fun tryParseCrypto(string: String): SdpCrypto? {
        val match = CRYPTO.matchEntire(string) ?: return null
        val (id, suite, config, sessionConfig) = match.destructured
        return SdpCrypto(id.toLong(), suite, config, toOptionalString(sessionConfig))
    }
    fun writeCrypto(item: SdpCrypto): String {
        return "crypto:${item.id} ${item.suite} ${item.config}${if (item.sessionConfig != null) (" " + item.sessionConfig) else ""}"
    }

    private val SETUP = """setup:(\w*)""".toRegex()
    fun tryParseSetup(string: String): SdpSetup? {
        val match = SETUP.matchEntire(string) ?: return null
        val (value) = match.destructured
        return SdpSetup(value)
    }
    fun writeSetup(item: SdpSetup): String {
        return "setup:${item.value}"
    }

    private val CONNECTIONTYPE = """connection:(new|existing)""".toRegex()
    fun tryParseConnectiontype(string: String): SdpConnectiontype? {
        val match = CONNECTIONTYPE.matchEntire(string) ?: return null
        val (value) = match.destructured
        return SdpConnectiontype(value)
    }
    fun writeConnectiontype(item: SdpConnectiontype): String {
        return "connection:${item.value}"
    }

    private val MID = """mid:([^\s]*)""".toRegex()
    fun tryParseMid(string: String): SdpMid? {
        val match = MID.matchEntire(string) ?: return null
        val (value) = match.destructured
        return SdpMid(value)
    }
    fun writeMid(item: SdpMid): String {
        return "mid:${item.value}"
    }

    private val MSID = """msid:(.*)""".toRegex()
    fun tryParseMsid(string: String): SdpMsid? {
        val match = MSID.matchEntire(string) ?: return null
        val (value) = match.destructured
        return SdpMsid(value)
    }
    fun writeMsid(item: SdpMsid): String {
        return "msid:${item.value}"
    }

    private val PTIME = """ptime:(\d*(?:\.\d*)*)""".toRegex()
    fun tryParsePtime(string: String): SdpPtime? {
        val match = PTIME.matchEntire(string) ?: return null
        val (value) = match.destructured
        return SdpPtime(value.toLong())
    }
    fun writePtime(item: SdpPtime): String {
        return "ptime:${item.value}"
    }

    private val MAXPTIME = """maxptime:(\d*(?:\.\d*)*)""".toRegex()
    fun tryParseMaxptime(string: String): SdpMaxptime? {
        val match = MAXPTIME.matchEntire(string) ?: return null
        val (value) = match.destructured
        return SdpMaxptime(value.toLong())
    }
    fun writeMaxptime(item: SdpMaxptime): String {
        return "maxptime:${item.value}"
    }

    private val DIRECTION = """(sendrecv|recvonly|sendonly|inactive)""".toRegex()
    fun tryParseDirection(string: String): SdpDirection? {
        val match = DIRECTION.matchEntire(string) ?: return null
        val (value) = match.destructured
        return SdpDirection(value)
    }
    fun writeDirection(item: SdpDirection): String {
        return "${item.value}"
    }

    private val ICELITE = """(ice-lite)""".toRegex()
    fun tryParseIcelite(string: String): SdpIcelite? {
        val match = ICELITE.matchEntire(string) ?: return null
        val (value) = match.destructured
        return SdpIcelite(value)
    }
    fun writeIcelite(item: SdpIcelite): String {
        return "${item.value}"
    }

    private val ICEUFRAG = """ice-ufrag:(\S*)""".toRegex()
    fun tryParseIceufrag(string: String): SdpIceufrag? {
        val match = ICEUFRAG.matchEntire(string) ?: return null
        val (value) = match.destructured
        return SdpIceufrag(value)
    }
    fun writeIceufrag(item: SdpIceufrag): String {
        return "ice-ufrag:${item.value}"
    }

    private val ICEPWD = """ice-pwd:(\S*)""".toRegex()
    fun tryParseIcepwd(string: String): SdpIcepwd? {
        val match = ICEPWD.matchEntire(string) ?: return null
        val (value) = match.destructured
        return SdpIcepwd(value)
    }
    fun writeIcepwd(item: SdpIcepwd): String {
        return "ice-pwd:${item.value}"
    }

    private val FINGERPRINT = """fingerprint:(\S*) (\S*)""".toRegex()
    fun tryParseFingerprint(string: String): SdpFingerprint? {
        val match = FINGERPRINT.matchEntire(string) ?: return null
        val (type, hash) = match.destructured
        return SdpFingerprint(type, hash)
    }
    fun writeFingerprint(item: SdpFingerprint): String {
        return "fingerprint:${item.type} ${item.hash}"
    }

    private val CANDIDATES = """candidate:(\S*) (\d*) (\S*) (\d*) (\S*) (\d*) typ (\S*)(?: raddr (\S*) rport (\d*))?(?: tcptype (\S*))?(?: generation (\d*))?(?: network-id (\d*))?(?: network-cost (\d*))?""".toRegex()
    fun tryParseCandidates(string: String): SdpCandidates? {
        val match = CANDIDATES.matchEntire(string) ?: return null
        val (foundation, component, transport, priority, ip, port, type, raddr, rport, tcptype, generation, networkId, networkCost) = match.destructured
        return SdpCandidates(
            foundation, component.toLong(), transport, priority.toLong(), ip, port.toLong(), type,
            toOptionalString(
                raddr,
            ),
            toOptionalLong(
                rport,
            ),
            toOptionalString(
                tcptype,
            ),
            toOptionalLong(generation), toOptionalLong(networkId), toOptionalLong(networkCost),
        )
    }
    fun writeCandidates(item: SdpCandidates): String {
        return "candidate:${item.foundation} ${item.component} ${item.transport} ${item.priority} ${item.ip} ${item.port} typ ${item.type}${if (item.raddr != null && item.rport != null) (" raddr " + item.raddr + " rport " + item.rport) else "" }${if (item.tcptype != null) (" tcptype " + item.tcptype) else "" }${if (item.generation != null) (" generation " + item.generation) else "" }${if (item.networkId != null) (" network-id " + item.networkId) else "" }${if (item.networkCost != null) (" network-cost " + item.networkCost) else "" }"
    }

    private val ENDOFCANDIDATES = """(end-of-candidates)""".toRegex()
    fun tryParseEndofcandidates(string: String): SdpEndofcandidates? {
        val match = ENDOFCANDIDATES.matchEntire(string) ?: return null
        val (value) = match.destructured
        return SdpEndofcandidates(value)
    }
    fun writeEndofcandidates(item: SdpEndofcandidates): String {
        return "${item.value}"
    }

    private val REMOTECANDIDATES = """remote-candidates:(.*)""".toRegex()
    fun tryParseRemotecandidates(string: String): SdpRemotecandidates? {
        val match = REMOTECANDIDATES.matchEntire(string) ?: return null
        val (value) = match.destructured
        return SdpRemotecandidates(value)
    }
    fun writeRemotecandidates(item: SdpRemotecandidates): String {
        return "remote-candidates:${item.value}"
    }

    private val ICEOPTIONS = """ice-options:(\S*)""".toRegex()
    fun tryParseIceoptions(string: String): SdpIceoptions? {
        val match = ICEOPTIONS.matchEntire(string) ?: return null
        val (value) = match.destructured
        return SdpIceoptions(value)
    }
    fun writeIceoptions(item: SdpIceoptions): String {
        return "ice-options:${item.value}"
    }

    private val SSRCS = """ssrc:(\d*) ([\w_-]*)(?::(.*))?""".toRegex()
    fun tryParseSsrcs(string: String): SdpSsrcs? {
        val match = SSRCS.matchEntire(string) ?: return null
        val (id, attribute, value) = match.destructured
        return SdpSsrcs(id, toOptionalString(attribute), toOptionalString(value))
    }
    fun writeSsrcs(item: SdpSsrcs): String {
        return "ssrc:${item.id}${if (item.attribute != null) (" " + item.attribute + (if (item.value != null) ":" + item.value else "")) else ""}"
    }

    private val SSRCGROUPS = """ssrc-group:([\x21\x23\x24\x25\x26\x27\x2A\x2B\x2D\x2E\w]*) (.*)""".toRegex()
    fun tryParseSsrcgroups(string: String): SdpSsrcgroups? {
        val match = SSRCGROUPS.matchEntire(string) ?: return null
        val (semantics, ssrcs) = match.destructured
        return SdpSsrcgroups(semantics, ssrcs)
    }
    fun writeSsrcgroups(item: SdpSsrcgroups): String {
        return "ssrc-group:${item.semantics} ${item.ssrcs}"
    }

    private val MSIDSEMANTIC = """msid-semantic:\s?(\w*) (\S*)""".toRegex()
    fun tryParseMsidsemantic(string: String): SdpMsidsemantic? {
        val match = MSIDSEMANTIC.matchEntire(string) ?: return null
        val (semantic, token) = match.destructured
        return SdpMsidsemantic(semantic, token)
    }
    fun writeMsidsemantic(item: SdpMsidsemantic): String {
        return "msid-semantic: ${item.semantic} ${item.token}"
    }

    private val GROUPS = """group:(\w*) (.*)""".toRegex()
    fun tryParseGroups(string: String): SdpGroups? {
        val match = GROUPS.matchEntire(string) ?: return null
        val (type, mids) = match.destructured
        return SdpGroups(type, mids)
    }
    fun writeGroups(item: SdpGroups): String {
        return "group:${item.type} ${item.mids}"
    }

    private val RTCPMUX = """(rtcp-mux)""".toRegex()
    fun tryParseRtcpmux(string: String): SdpRtcpmux? {
        val match = RTCPMUX.matchEntire(string) ?: return null
        val (value) = match.destructured
        return SdpRtcpmux(value)
    }
    fun writeRtcpmux(item: SdpRtcpmux): String {
        return "${item.value}"
    }

    private val RTCPRSIZE = """(rtcp-rsize)""".toRegex()
    fun tryParseRtcprsize(string: String): SdpRtcprsize? {
        val match = RTCPRSIZE.matchEntire(string) ?: return null
        val (value) = match.destructured
        return SdpRtcprsize(value)
    }
    fun writeRtcprsize(item: SdpRtcprsize): String {
        return "${item.value}"
    }

    private val SCTPMAP = """sctpmap:([\w_/]*) (\S*)(?: (\S*))?""".toRegex()
    fun tryParseSctpmap(string: String): SdpSctpmap? {
        val match = SCTPMAP.matchEntire(string) ?: return null
        val (sctpmapNumber) = match.destructured
        return SdpSctpmap(sctpmapNumber)
    }
    fun writeSctpmap(item: SdpSctpmap): String {
        return "sctpmap:${item.sctpmapNumber}"
    }

    private val XGOOGLEFLAG = """x-google-flag:([^\s]*)""".toRegex()
    fun tryParseXgoogleflag(string: String): SdpXgoogleflag? {
        val match = XGOOGLEFLAG.matchEntire(string) ?: return null
        val (value) = match.destructured
        return SdpXgoogleflag(value)
    }
    fun writeXgoogleflag(item: SdpXgoogleflag): String {
        return "x-google-flag:${item.value}"
    }

    private val RIDS = """rid:([\d\w]+) (\w+)(?: ([\S| ]*))?""".toRegex()
    fun tryParseRids(string: String): SdpRids? {
        val match = RIDS.matchEntire(string) ?: return null
        val (id) = match.destructured
        return SdpRids(id)
    }
    fun writeRids(item: SdpRids): String {
        return "rid:${item.id}"
    }

    private val IMAGEATTRS = """imageattr:(\d+|\*)[\s\t]+(send|recv)[\s\t]+(\*|[\S+](?:[\s\t]+[\S+])*)(?:[\s\t]+(recv|send)[\s\t]+(\*|[\S+](?:[\s\t]+[\S+])*))?""".toRegex()
    fun tryParseImageattrs(string: String): SdpImageattrs? {
        val match = IMAGEATTRS.matchEntire(string) ?: return null
        val (pt) = match.destructured
        return SdpImageattrs(pt)
    }
    fun writeImageattrs(item: SdpImageattrs): String {
        return "imageattr:${item.pt}"
    }

    private val SIMULCAST = """simulcast:(send|recv) ([a-zA-Z0-9\-_~;,]+)(?:\s?(send|recv) ([a-zA-Z0-9\-_~;,]+))?$""".toRegex()
    fun tryParseSimulcast(string: String): SdpSimulcast? {
        val match = SIMULCAST.matchEntire(string) ?: return null
        val (dir1) = match.destructured
        return SdpSimulcast(dir1)
    }
    fun writeSimulcast(item: SdpSimulcast): String {
        return "simulcast:${item.dir1}"
    }

    private val SIMULCAST03 = """simulcast:[\s\t]+([\S+\s\t]+)$""".toRegex()
    fun tryParseSimulcast03(string: String): SdpSimulcast03? {
        val match = SIMULCAST03.matchEntire(string) ?: return null
        val (value) = match.destructured
        return SdpSimulcast03(value)
    }
    fun writeSimulcast03(item: SdpSimulcast03): String {
        return "simulcast:${item.value}"
    }

    private val FRAMERATE = """framerate:(\d+(?:$|\.\d+))""".toRegex()
    fun tryParseFramerate(string: String): SdpFramerate? {
        val match = FRAMERATE.matchEntire(string) ?: return null
        val (value) = match.destructured
        return SdpFramerate(value.toLong())
    }
    fun writeFramerate(item: SdpFramerate): String {
        return "framerate:${item.value}"
    }

    private val SOURCEFILTER = """source-filter: *(excl|incl) (\S*) (IP4|IP6|\*) (\S*) (.*)""".toRegex()
    fun tryParseSourcefilter(string: String): SdpSourcefilter? {
        val match = SOURCEFILTER.matchEntire(string) ?: return null
        val (filterMode, netType, addressTypes, destAddress, srcList) = match.destructured
        return SdpSourcefilter(filterMode, netType, addressTypes, destAddress, srcList)
    }
    fun writeSourcefilter(item: SdpSourcefilter): String {
        return "source-filter: ${item.filterMode} ${item.netType} ${item.addressTypes} ${item.destAddress} ${item.srcList}"
    }

    private val BUNDLEONLY = """(bundle-only)""".toRegex()
    fun tryParseBundleonly(string: String): SdpBundleonly? {
        val match = BUNDLEONLY.matchEntire(string) ?: return null
        val (value) = match.destructured
        return SdpBundleonly(value)
    }
    fun writeBundleonly(item: SdpBundleonly): String {
        return "${item.value}"
    }

    private val LABEL = """label:(.+)""".toRegex()
    fun tryParseLabel(string: String): SdpLabel? {
        val match = LABEL.matchEntire(string) ?: return null
        val (value) = match.destructured
        return SdpLabel(value)
    }
    fun writeLabel(item: SdpLabel): String {
        return "label:${item.value}"
    }

    private val SCTPPORT = """sctp-port:(\d+)$""".toRegex()
    fun tryParseSctpport(string: String): SdpSctpport? {
        val match = SCTPPORT.matchEntire(string) ?: return null
        val (value) = match.destructured
        return SdpSctpport(value.toLong())
    }
    fun writeSctpport(item: SdpSctpport): String {
        return "sctp-port:${item.value}"
    }

    private val MAXMESSAGESIZE = """max-message-size:(\d+)$""".toRegex()
    fun tryParseMaxmessagesize(string: String): SdpMaxmessagesize? {
        val match = MAXMESSAGESIZE.matchEntire(string) ?: return null
        val (value) = match.destructured
        return SdpMaxmessagesize(value.toLong())
    }
    fun writeMaxmessagesize(item: SdpMaxmessagesize): String {
        return "max-message-size:${item.value}"
    }

    private val TSREFCLOCKS = """ts-refclk:([^\s=]*)(?:=(\S*))?""".toRegex()
    fun tryParseTsrefclocks(string: String): SdpTsrefclocks? {
        val match = TSREFCLOCKS.matchEntire(string) ?: return null
        val (clksrc) = match.destructured
        return SdpTsrefclocks(clksrc)
    }
    fun writeTsrefclocks(item: SdpTsrefclocks): String {
        return "ts-refclk:${item.clksrc}"
    }

    private val MEDIACLK = """mediaclk:(?:id=(\S*))? *([^\s=]*)(?:=(\S*))?(?: *rate=(\d+)/(\d+))?""".toRegex()
    fun tryParseMediaclk(string: String): SdpMediaclk? {
        val match = MEDIACLK.matchEntire(string) ?: return null
        val (id) = match.destructured
        return SdpMediaclk(id)
    }
    fun writeMediaclk(item: SdpMediaclk): String {
        return "mediaclk:${item.id}"
    }

    private val KEYWORDS = """keywds:(.+)$""".toRegex()
    fun tryParseKeywords(string: String): SdpKeywords? {
        val match = KEYWORDS.matchEntire(string) ?: return null
        val (value) = match.destructured
        return SdpKeywords(value)
    }
    fun writeKeywords(item: SdpKeywords): String {
        return "keywds:${item.value}"
    }

    private val CONTENT = """content:(.+)""".toRegex()
    fun tryParseContent(string: String): SdpContent? {
        val match = CONTENT.matchEntire(string) ?: return null
        val (value) = match.destructured
        return SdpContent(value)
    }
    fun writeContent(item: SdpContent): String {
        return "content:${item.value}"
    }

    private val BFCPFLOORCTRL = """floorctrl:(c-only|s-only|c-s)""".toRegex()
    fun tryParseBfcpfloorctrl(string: String): SdpBfcpfloorctrl? {
        val match = BFCPFLOORCTRL.matchEntire(string) ?: return null
        val (value) = match.destructured
        return SdpBfcpfloorctrl(value)
    }
    fun writeBfcpfloorctrl(item: SdpBfcpfloorctrl): String {
        return "${item.value}"
    }

    private val BFCPCONFID = """confid:(\d+)""".toRegex()
    fun tryParseBfcpconfid(string: String): SdpBfcpconfid? {
        val match = BFCPCONFID.matchEntire(string) ?: return null
        val (value) = match.destructured
        return SdpBfcpconfid(value)
    }
    fun writeBfcpconfid(item: SdpBfcpconfid): String {
        return "${item.value}"
    }

    private val BFCPUSERID = """userid:(\d+)""".toRegex()
    fun tryParseBfcpuserid(string: String): SdpBfcpuserid? {
        val match = BFCPUSERID.matchEntire(string) ?: return null
        val (value) = match.destructured
        return SdpBfcpuserid(value)
    }
    fun writeBfcpuserid(item: SdpBfcpuserid): String {
        return "${item.value}"
    }

    private val BFCPFLOORID = """floorid:(.+) (?:m-stream|mstrm):(.+)""".toRegex()
    fun tryParseBfcpfloorid(string: String): SdpBfcpfloorid? {
        val match = BFCPFLOORID.matchEntire(string) ?: return null
        val (id, mStream) = match.destructured
        return SdpBfcpfloorid(id, mStream)
    }
    fun writeBfcpfloorid(item: SdpBfcpfloorid): String {
        return "floorid:${item.id} mstrm:${item.mStream}"
    }

    private val INVALID = """(.*)""".toRegex()
    fun tryParseInvalid(string: String): SdpInvalid? {
        val match = INVALID.matchEntire(string) ?: return null
        val (value) = match.destructured
        return SdpInvalid(value)
    }
    fun writeInvalid(item: SdpInvalid): String {
        return "${item.value}"
    }
}

class SdpMedia {
    var version: SdpVersion? = null
    var origin: SdpOrigin? = null
    var name: SdpName? = null
    var description: SdpDescription? = null
    var uri: SdpUri? = null
    var email: SdpEmail? = null
    var phone: SdpPhone? = null
    var timezones: SdpTimezones? = null
    var repeats: SdpRepeats? = null
    var timing: SdpTiming? = null
    var connection: SdpConnection? = null
    var bandwidth = mutableListOf<SdpBandwidth>()
    var mline: SdpMline? = null
    var rtp = mutableListOf<SdpRtp>()
    var fmtp = mutableListOf<SdpFmtp>()
    var control: SdpControl? = null
    var rtcp: SdpRtcp? = null
    var rtcpFbTrrInt = mutableListOf<SdpRtcpfbtrrint>()
    var rtcpFb = mutableListOf<SdpRtcpfb>()
    var ext = mutableListOf<SdpExt>()
    var extmapAllowMixed: SdpExtmapallowmixed? = null
    var crypto = mutableListOf<SdpCrypto>()
    var setup: SdpSetup? = null
    var connectionType: SdpConnectiontype? = null
    var mid: SdpMid? = null
    var msid: SdpMsid? = null
    var ptime: SdpPtime? = null
    var maxptime: SdpMaxptime? = null
    var direction: SdpDirection? = null
    var icelite: SdpIcelite? = null
    var iceUfrag: SdpIceufrag? = null
    var icePwd: SdpIcepwd? = null
    var fingerprint: SdpFingerprint? = null
    var candidates = mutableListOf<SdpCandidates>()
    var endOfCandidates: SdpEndofcandidates? = null
    var remoteCandidates: SdpRemotecandidates? = null
    var iceOptions: SdpIceoptions? = null
    var ssrcs = mutableListOf<SdpSsrcs>()
    var ssrcGroups = mutableListOf<SdpSsrcgroups>()
    var msidSemantic: SdpMsidsemantic? = null
    var groups = mutableListOf<SdpGroups>()
    var rtcpMux: SdpRtcpmux? = null
    var rtcpRsize: SdpRtcprsize? = null
    var sctpmap: SdpSctpmap? = null
    var xGoogleFlag: SdpXgoogleflag? = null
    var rids = mutableListOf<SdpRids>()
    var imageattrs = mutableListOf<SdpImageattrs>()
    var simulcast: SdpSimulcast? = null
    var simulcast03: SdpSimulcast03? = null
    var framerate: SdpFramerate? = null
    var sourceFilter: SdpSourcefilter? = null
    var bundleOnly: SdpBundleonly? = null
    var label: SdpLabel? = null
    var sctpPort: SdpSctpport? = null
    var maxMessageSize: SdpMaxmessagesize? = null
    var tsRefClocks = mutableListOf<SdpTsrefclocks>()
    var mediaClk: SdpMediaclk? = null
    var keywords: SdpKeywords? = null
    var content: SdpContent? = null
    var bfcpFloorCtrl: SdpBfcpfloorctrl? = null
    var bfcpConfId: SdpBfcpconfid? = null
    var bfcpUserId: SdpBfcpuserid? = null
    var bfcpFloorId: SdpBfcpfloorid? = null
    var invalid = mutableListOf<SdpInvalid>()

    override fun equals(other: Any?) = (other is SdpMedia) &&
        version == other.version &&
        origin == other.origin &&
        name == other.name &&
        description == other.description &&
        uri == other.uri &&
        email == other.email &&
        phone == other.phone &&
        timezones == other.timezones &&
        repeats == other.repeats &&
        timing == other.timing &&
        connection == other.connection &&
        bandwidth == other.bandwidth &&
        mline == other.mline &&
        rtp == other.rtp &&
        fmtp == other.fmtp &&
        control == other.control &&
        rtcp == other.rtcp &&
        rtcpFbTrrInt == other.rtcpFbTrrInt &&
        rtcpFb == other.rtcpFb &&
        ext == other.ext &&
        extmapAllowMixed == other.extmapAllowMixed &&
        crypto == other.crypto &&
        setup == other.setup &&
        connectionType == other.connectionType &&
        mid == other.mid &&
        msid == other.msid &&
        ptime == other.ptime &&
        maxptime == other.maxptime &&
        direction == other.direction &&
        icelite == other.icelite &&
        iceUfrag == other.iceUfrag &&
        icePwd == other.icePwd &&
        fingerprint == other.fingerprint &&
        candidates == other.candidates &&
        endOfCandidates == other.endOfCandidates &&
        remoteCandidates == other.remoteCandidates &&
        iceOptions == other.iceOptions &&
        ssrcs == other.ssrcs &&
        ssrcGroups == other.ssrcGroups &&
        msidSemantic == other.msidSemantic &&
        groups == other.groups &&
        rtcpMux == other.rtcpMux &&
        rtcpRsize == other.rtcpRsize &&
        sctpmap == other.sctpmap &&
        xGoogleFlag == other.xGoogleFlag &&
        rids == other.rids &&
        imageattrs == other.imageattrs &&
        simulcast == other.simulcast &&
        simulcast03 == other.simulcast03 &&
        framerate == other.framerate &&
        sourceFilter == other.sourceFilter &&
        bundleOnly == other.bundleOnly &&
        label == other.label &&
        sctpPort == other.sctpPort &&
        maxMessageSize == other.maxMessageSize &&
        tsRefClocks == other.tsRefClocks &&
        mediaClk == other.mediaClk &&
        keywords == other.keywords &&
        content == other.content &&
        bfcpFloorCtrl == other.bfcpFloorCtrl &&
        bfcpConfId == other.bfcpConfId &&
        bfcpUserId == other.bfcpUserId &&
        bfcpFloorId == other.bfcpFloorId &&
        invalid == other.invalid
}

class SdpSession {
    var version: SdpVersion? = null
    var origin: SdpOrigin? = null
    var name: SdpName? = null
    var description: SdpDescription? = null
    var uri: SdpUri? = null
    var email: SdpEmail? = null
    var phone: SdpPhone? = null
    var timezones: SdpTimezones? = null
    var repeats: SdpRepeats? = null
    var timing: SdpTiming? = null
    var connection: SdpConnection? = null
    var bandwidth = mutableListOf<SdpBandwidth>()
    var mline: SdpMline? = null
    var rtp = mutableListOf<SdpRtp>()
    var fmtp = mutableListOf<SdpFmtp>()
    var control: SdpControl? = null
    var rtcp: SdpRtcp? = null
    var rtcpFbTrrInt = mutableListOf<SdpRtcpfbtrrint>()
    var rtcpFb = mutableListOf<SdpRtcpfb>()
    var ext = mutableListOf<SdpExt>()
    var extmapAllowMixed: SdpExtmapallowmixed? = null
    var crypto = mutableListOf<SdpCrypto>()
    var setup: SdpSetup? = null
    var connectionType: SdpConnectiontype? = null
    var mid: SdpMid? = null
    var msid: SdpMsid? = null
    var ptime: SdpPtime? = null
    var maxptime: SdpMaxptime? = null
    var direction: SdpDirection? = null
    var icelite: SdpIcelite? = null
    var iceUfrag: SdpIceufrag? = null
    var icePwd: SdpIcepwd? = null
    var fingerprint: SdpFingerprint? = null
    var candidates = mutableListOf<SdpCandidates>()
    var endOfCandidates: SdpEndofcandidates? = null
    var remoteCandidates: SdpRemotecandidates? = null
    var iceOptions: SdpIceoptions? = null
    var ssrcs = mutableListOf<SdpSsrcs>()
    var ssrcGroups = mutableListOf<SdpSsrcgroups>()
    var msidSemantic: SdpMsidsemantic? = null
    var groups = mutableListOf<SdpGroups>()
    var rtcpMux: SdpRtcpmux? = null
    var rtcpRsize: SdpRtcprsize? = null
    var sctpmap: SdpSctpmap? = null
    var xGoogleFlag: SdpXgoogleflag? = null
    var rids = mutableListOf<SdpRids>()
    var imageattrs = mutableListOf<SdpImageattrs>()
    var simulcast: SdpSimulcast? = null
    var simulcast03: SdpSimulcast03? = null
    var framerate: SdpFramerate? = null
    var sourceFilter: SdpSourcefilter? = null
    var bundleOnly: SdpBundleonly? = null
    var label: SdpLabel? = null
    var sctpPort: SdpSctpport? = null
    var maxMessageSize: SdpMaxmessagesize? = null
    var tsRefClocks = mutableListOf<SdpTsrefclocks>()
    var mediaClk: SdpMediaclk? = null
    var keywords: SdpKeywords? = null
    var content: SdpContent? = null
    var bfcpFloorCtrl: SdpBfcpfloorctrl? = null
    var bfcpConfId: SdpBfcpconfid? = null
    var bfcpUserId: SdpBfcpuserid? = null
    var bfcpFloorId: SdpBfcpfloorid? = null
    var invalid = mutableListOf<SdpInvalid>()
    val media = mutableListOf<SdpMedia>()

    init {
        version = SdpVersion(0)
    }

    fun parse(sdp: String) {
        val lines = sdp.split("\r\n").filter { line -> line.isNotEmpty() }
        if (lines.isEmpty()) {
            throw IllegalArgumentException("Invalid empty SDP")
        }
        lines.forEach { line -> parseLine(line) }
    }

    fun write(): String {
        val lines = mutableListOf<String>()
        if (version != null) lines.add("v=" + SdpGrammar.writeVersion(version!!))
        if (origin != null) lines.add("o=" + SdpGrammar.writeOrigin(origin!!))
        if (name != null) lines.add("s=" + SdpGrammar.writeName(name!!))
        if (description != null) lines.add("i=" + SdpGrammar.writeDescription(description!!))
        if (uri != null) lines.add("u=" + SdpGrammar.writeUri(uri!!))
        if (email != null) lines.add("e=" + SdpGrammar.writeEmail(email!!))
        if (phone != null) lines.add("p=" + SdpGrammar.writePhone(phone!!))
        if (timezones != null) lines.add("z=" + SdpGrammar.writeTimezones(timezones!!))
        if (repeats != null) lines.add("r=" + SdpGrammar.writeRepeats(repeats!!))
        if (timing != null) lines.add("t=" + SdpGrammar.writeTiming(timing!!))
        if (connection != null) lines.add("c=" + SdpGrammar.writeConnection(connection!!))
        bandwidth.forEach { item -> lines.add("b=" + SdpGrammar.writeBandwidth(item)) }
        if (mline != null) lines.add("m=" + SdpGrammar.writeMline(mline!!))
        rtp.forEach { item -> lines.add("a=" + SdpGrammar.writeRtp(item)) }
        fmtp.forEach { item -> lines.add("a=" + SdpGrammar.writeFmtp(item)) }
        if (control != null) lines.add("a=" + SdpGrammar.writeControl(control!!))
        if (rtcp != null) lines.add("a=" + SdpGrammar.writeRtcp(rtcp!!))
        rtcpFbTrrInt.forEach { item -> lines.add("a=" + SdpGrammar.writeRtcpfbtrrint(item)) }
        rtcpFb.forEach { item -> lines.add("a=" + SdpGrammar.writeRtcpfb(item)) }
        ext.forEach { item -> lines.add("a=" + SdpGrammar.writeExt(item)) }
        if (extmapAllowMixed != null) {
            lines.add(
                "a=" + SdpGrammar.writeExtmapallowmixed(extmapAllowMixed!!),
            )
        }
        crypto.forEach { item -> lines.add("a=" + SdpGrammar.writeCrypto(item)) }
        if (setup != null) lines.add("a=" + SdpGrammar.writeSetup(setup!!))
        if (connectionType != null) {
            lines.add(
                "a=" + SdpGrammar.writeConnectiontype(connectionType!!),
            )
        }
        if (mid != null) lines.add("a=" + SdpGrammar.writeMid(mid!!))
        if (msid != null) lines.add("a=" + SdpGrammar.writeMsid(msid!!))
        if (ptime != null) lines.add("a=" + SdpGrammar.writePtime(ptime!!))
        if (maxptime != null) lines.add("a=" + SdpGrammar.writeMaxptime(maxptime!!))
        if (direction != null) lines.add("a=" + SdpGrammar.writeDirection(direction!!))
        if (icelite != null) lines.add("a=" + SdpGrammar.writeIcelite(icelite!!))
        if (iceUfrag != null) lines.add("a=" + SdpGrammar.writeIceufrag(iceUfrag!!))
        if (icePwd != null) lines.add("a=" + SdpGrammar.writeIcepwd(icePwd!!))
        if (fingerprint != null) lines.add("a=" + SdpGrammar.writeFingerprint(fingerprint!!))
        candidates.forEach { item -> lines.add("a=" + SdpGrammar.writeCandidates(item)) }
        if (endOfCandidates != null) {
            lines.add(
                "a=" + SdpGrammar.writeEndofcandidates(endOfCandidates!!),
            )
        }
        if (remoteCandidates != null) {
            lines.add(
                "a=" + SdpGrammar.writeRemotecandidates(remoteCandidates!!),
            )
        }
        if (iceOptions != null) lines.add("a=" + SdpGrammar.writeIceoptions(iceOptions!!))
        ssrcs.forEach { item -> lines.add("a=" + SdpGrammar.writeSsrcs(item)) }
        ssrcGroups.forEach { item -> lines.add("a=" + SdpGrammar.writeSsrcgroups(item)) }
        if (msidSemantic != null) lines.add("a=" + SdpGrammar.writeMsidsemantic(msidSemantic!!))
        groups.forEach { item -> lines.add("a=" + SdpGrammar.writeGroups(item)) }
        if (rtcpMux != null) lines.add("a=" + SdpGrammar.writeRtcpmux(rtcpMux!!))
        if (rtcpRsize != null) lines.add("a=" + SdpGrammar.writeRtcprsize(rtcpRsize!!))
        if (sctpmap != null) lines.add("a=" + SdpGrammar.writeSctpmap(sctpmap!!))
        if (xGoogleFlag != null) lines.add("a=" + SdpGrammar.writeXgoogleflag(xGoogleFlag!!))
        rids.forEach { item -> lines.add("a=" + SdpGrammar.writeRids(item)) }
        imageattrs.forEach { item -> lines.add("a=" + SdpGrammar.writeImageattrs(item)) }
        if (simulcast != null) lines.add("a=" + SdpGrammar.writeSimulcast(simulcast!!))
        if (simulcast03 != null) lines.add("a=" + SdpGrammar.writeSimulcast03(simulcast03!!))
        if (framerate != null) lines.add("a=" + SdpGrammar.writeFramerate(framerate!!))
        if (sourceFilter != null) lines.add("a=" + SdpGrammar.writeSourcefilter(sourceFilter!!))
        if (bundleOnly != null) lines.add("a=" + SdpGrammar.writeBundleonly(bundleOnly!!))
        if (label != null) lines.add("a=" + SdpGrammar.writeLabel(label!!))
        if (sctpPort != null) lines.add("a=" + SdpGrammar.writeSctpport(sctpPort!!))
        if (maxMessageSize != null) {
            lines.add(
                "a=" + SdpGrammar.writeMaxmessagesize(maxMessageSize!!),
            )
        }
        tsRefClocks.forEach { item -> lines.add("a=" + SdpGrammar.writeTsrefclocks(item)) }
        if (mediaClk != null) lines.add("a=" + SdpGrammar.writeMediaclk(mediaClk!!))
        if (keywords != null) lines.add("a=" + SdpGrammar.writeKeywords(keywords!!))
        if (content != null) lines.add("a=" + SdpGrammar.writeContent(content!!))
        if (bfcpFloorCtrl != null) lines.add("a=" + SdpGrammar.writeBfcpfloorctrl(bfcpFloorCtrl!!))
        if (bfcpConfId != null) lines.add("a=" + SdpGrammar.writeBfcpconfid(bfcpConfId!!))
        if (bfcpUserId != null) lines.add("a=" + SdpGrammar.writeBfcpuserid(bfcpUserId!!))
        if (bfcpFloorId != null) lines.add("a=" + SdpGrammar.writeBfcpfloorid(bfcpFloorId!!))
        invalid.forEach { item -> lines.add("a=" + SdpGrammar.writeInvalid(item)) }
        for (m in media) {
            lines.add("m=" + SdpGrammar.writeMline(m.mline!!))
            if (m.version != null) lines.add("v=" + SdpGrammar.writeVersion(m.version!!))
            if (m.origin != null) lines.add("o=" + SdpGrammar.writeOrigin(m.origin!!))
            if (m.name != null) lines.add("s=" + SdpGrammar.writeName(m.name!!))
            if (m.description != null) {
                lines.add(
                    "i=" + SdpGrammar.writeDescription(m.description!!),
                )
            }
            if (m.uri != null) lines.add("u=" + SdpGrammar.writeUri(m.uri!!))
            if (m.email != null) lines.add("e=" + SdpGrammar.writeEmail(m.email!!))
            if (m.phone != null) lines.add("p=" + SdpGrammar.writePhone(m.phone!!))
            if (m.timezones != null) lines.add("z=" + SdpGrammar.writeTimezones(m.timezones!!))
            if (m.repeats != null) lines.add("r=" + SdpGrammar.writeRepeats(m.repeats!!))
            if (m.timing != null) lines.add("t=" + SdpGrammar.writeTiming(m.timing!!))
            if (m.connection != null) lines.add("c=" + SdpGrammar.writeConnection(m.connection!!))
            m.bandwidth.forEach { item -> lines.add("b=" + SdpGrammar.writeBandwidth(item)) }
            m.rtp.forEach { item -> lines.add("a=" + SdpGrammar.writeRtp(item)) }
            m.fmtp.forEach { item -> lines.add("a=" + SdpGrammar.writeFmtp(item)) }
            if (m.control != null) lines.add("a=" + SdpGrammar.writeControl(m.control!!))
            if (m.rtcp != null) lines.add("a=" + SdpGrammar.writeRtcp(m.rtcp!!))
            m.rtcpFbTrrInt.forEach { item -> lines.add("a=" + SdpGrammar.writeRtcpfbtrrint(item)) }
            m.rtcpFb.forEach { item -> lines.add("a=" + SdpGrammar.writeRtcpfb(item)) }
            m.ext.forEach { item -> lines.add("a=" + SdpGrammar.writeExt(item)) }
            if (m.extmapAllowMixed != null) {
                lines.add(
                    "a=" + SdpGrammar.writeExtmapallowmixed(m.extmapAllowMixed!!),
                )
            }
            m.crypto.forEach { item -> lines.add("a=" + SdpGrammar.writeCrypto(item)) }
            if (m.setup != null) lines.add("a=" + SdpGrammar.writeSetup(m.setup!!))
            if (m.connectionType != null) {
                lines.add(
                    "a=" + SdpGrammar.writeConnectiontype(m.connectionType!!),
                )
            }
            if (m.mid != null) lines.add("a=" + SdpGrammar.writeMid(m.mid!!))
            if (m.msid != null) lines.add("a=" + SdpGrammar.writeMsid(m.msid!!))
            if (m.ptime != null) lines.add("a=" + SdpGrammar.writePtime(m.ptime!!))
            if (m.maxptime != null) lines.add("a=" + SdpGrammar.writeMaxptime(m.maxptime!!))
            if (m.direction != null) lines.add("a=" + SdpGrammar.writeDirection(m.direction!!))
            if (m.icelite != null) lines.add("a=" + SdpGrammar.writeIcelite(m.icelite!!))
            if (m.iceUfrag != null) lines.add("a=" + SdpGrammar.writeIceufrag(m.iceUfrag!!))
            if (m.icePwd != null) lines.add("a=" + SdpGrammar.writeIcepwd(m.icePwd!!))
            if (m.fingerprint != null) {
                lines.add(
                    "a=" + SdpGrammar.writeFingerprint(m.fingerprint!!),
                )
            }
            m.candidates.forEach { item -> lines.add("a=" + SdpGrammar.writeCandidates(item)) }
            if (m.endOfCandidates != null) {
                lines.add(
                    "a=" + SdpGrammar.writeEndofcandidates(m.endOfCandidates!!),
                )
            }
            if (m.remoteCandidates != null) {
                lines.add(
                    "a=" + SdpGrammar.writeRemotecandidates(m.remoteCandidates!!),
                )
            }
            if (m.iceOptions != null) lines.add("a=" + SdpGrammar.writeIceoptions(m.iceOptions!!))
            m.ssrcs.forEach { item -> lines.add("a=" + SdpGrammar.writeSsrcs(item)) }
            m.ssrcGroups.forEach { item -> lines.add("a=" + SdpGrammar.writeSsrcgroups(item)) }
            if (m.msidSemantic != null) {
                lines.add(
                    "a=" + SdpGrammar.writeMsidsemantic(m.msidSemantic!!),
                )
            }
            m.groups.forEach { item -> lines.add("a=" + SdpGrammar.writeGroups(item)) }
            if (m.rtcpMux != null) lines.add("a=" + SdpGrammar.writeRtcpmux(m.rtcpMux!!))
            if (m.rtcpRsize != null) lines.add("a=" + SdpGrammar.writeRtcprsize(m.rtcpRsize!!))
            if (m.sctpmap != null) lines.add("a=" + SdpGrammar.writeSctpmap(m.sctpmap!!))
            if (m.xGoogleFlag != null) {
                lines.add(
                    "a=" + SdpGrammar.writeXgoogleflag(m.xGoogleFlag!!),
                )
            }
            m.rids.forEach { item -> lines.add("a=" + SdpGrammar.writeRids(item)) }
            m.imageattrs.forEach { item -> lines.add("a=" + SdpGrammar.writeImageattrs(item)) }
            if (m.simulcast != null) lines.add("a=" + SdpGrammar.writeSimulcast(m.simulcast!!))
            if (m.simulcast03 != null) {
                lines.add(
                    "a=" + SdpGrammar.writeSimulcast03(m.simulcast03!!),
                )
            }
            if (m.framerate != null) lines.add("a=" + SdpGrammar.writeFramerate(m.framerate!!))
            if (m.sourceFilter != null) {
                lines.add(
                    "a=" + SdpGrammar.writeSourcefilter(m.sourceFilter!!),
                )
            }
            if (m.bundleOnly != null) lines.add("a=" + SdpGrammar.writeBundleonly(m.bundleOnly!!))
            if (m.label != null) lines.add("a=" + SdpGrammar.writeLabel(m.label!!))
            if (m.sctpPort != null) lines.add("a=" + SdpGrammar.writeSctpport(m.sctpPort!!))
            if (m.maxMessageSize != null) {
                lines.add(
                    "a=" + SdpGrammar.writeMaxmessagesize(m.maxMessageSize!!),
                )
            }
            m.tsRefClocks.forEach { item -> lines.add("a=" + SdpGrammar.writeTsrefclocks(item)) }
            if (m.mediaClk != null) lines.add("a=" + SdpGrammar.writeMediaclk(m.mediaClk!!))
            if (m.keywords != null) lines.add("a=" + SdpGrammar.writeKeywords(m.keywords!!))
            if (m.content != null) lines.add("a=" + SdpGrammar.writeContent(m.content!!))
            if (m.bfcpFloorCtrl != null) {
                lines.add(
                    "a=" + SdpGrammar.writeBfcpfloorctrl(m.bfcpFloorCtrl!!),
                )
            }
            if (m.bfcpConfId != null) lines.add("a=" + SdpGrammar.writeBfcpconfid(m.bfcpConfId!!))
            if (m.bfcpUserId != null) lines.add("a=" + SdpGrammar.writeBfcpuserid(m.bfcpUserId!!))
            if (m.bfcpFloorId != null) {
                lines.add(
                    "a=" + SdpGrammar.writeBfcpfloorid(m.bfcpFloorId!!),
                )
            }
            m.invalid.forEach { item -> lines.add("a=" + SdpGrammar.writeInvalid(item)) }
        }

        return lines.joinToString("\r\n", postfix = "\r\n")
    }

    private fun parseLine(line: String) {
        if (line.length < 2 || line[1] != '=') {
            throw IllegalArgumentException("Invalid SDP line")
        }
        val type = line[0]
        val value = line.substring(2)
        var lastMedia = media.lastOrNull()
        when (type) {
            'v' -> {
                val tryVersion = SdpGrammar.tryParseVersion(value)
                if (tryVersion != null) {
                    if (lastMedia != null) lastMedia.version = tryVersion else version = tryVersion
                    return
                }
            }
            'o' -> {
                val tryOrigin = SdpGrammar.tryParseOrigin(value)
                if (tryOrigin != null) {
                    if (lastMedia != null) lastMedia.origin = tryOrigin else origin = tryOrigin
                    return
                }
            }
            's' -> {
                val tryName = SdpGrammar.tryParseName(value)
                if (tryName != null) {
                    if (lastMedia != null) lastMedia.name = tryName else name = tryName
                    return
                }
            }
            'i' -> {
                val tryDescription = SdpGrammar.tryParseDescription(value)
                if (tryDescription != null) {
                    if (lastMedia != null) lastMedia.description = tryDescription else description = tryDescription
                    return
                }
            }
            'u' -> {
                val tryUri = SdpGrammar.tryParseUri(value)
                if (tryUri != null) {
                    if (lastMedia != null) lastMedia.uri = tryUri else uri = tryUri
                    return
                }
            }
            'e' -> {
                val tryEmail = SdpGrammar.tryParseEmail(value)
                if (tryEmail != null) {
                    if (lastMedia != null) lastMedia.email = tryEmail else email = tryEmail
                    return
                }
            }
            'p' -> {
                val tryPhone = SdpGrammar.tryParsePhone(value)
                if (tryPhone != null) {
                    if (lastMedia != null) lastMedia.phone = tryPhone else phone = tryPhone
                    return
                }
            }
            'z' -> {
                val tryTimezones = SdpGrammar.tryParseTimezones(value)
                if (tryTimezones != null) {
                    if (lastMedia != null) lastMedia.timezones = tryTimezones else timezones = tryTimezones
                    return
                }
            }
            'r' -> {
                val tryRepeats = SdpGrammar.tryParseRepeats(value)
                if (tryRepeats != null) {
                    if (lastMedia != null) lastMedia.repeats = tryRepeats else repeats = tryRepeats
                    return
                }
            }
            't' -> {
                val tryTiming = SdpGrammar.tryParseTiming(value)
                if (tryTiming != null) {
                    if (lastMedia != null) lastMedia.timing = tryTiming else timing = tryTiming
                    return
                }
            }
            'c' -> {
                val tryConnection = SdpGrammar.tryParseConnection(value)
                if (tryConnection != null) {
                    if (lastMedia != null) lastMedia.connection = tryConnection else connection = tryConnection
                    return
                }
            }
            'b' -> {
                val tryBandwidth = SdpGrammar.tryParseBandwidth(value)
                if (tryBandwidth != null) {
                    if (lastMedia != null) {
                        lastMedia.bandwidth.add(
                            tryBandwidth,
                        )
                    } else {
                        bandwidth.add(tryBandwidth)
                    }
                    return
                }
            }
            'm' -> {
                val tryMline = SdpGrammar.tryParseMline(value)
                if (tryMline != null) {
                    lastMedia = SdpMedia()
                    lastMedia.mline = tryMline
                    media.add(lastMedia)
                    return
                }
            }
            'a' -> {
                val tryRtp = SdpGrammar.tryParseRtp(value)
                if (tryRtp != null) {
                    if (lastMedia != null) lastMedia.rtp.add(tryRtp) else rtp.add(tryRtp)
                    return
                }
                val tryFmtp = SdpGrammar.tryParseFmtp(value)
                if (tryFmtp != null) {
                    if (lastMedia != null) lastMedia.fmtp.add(tryFmtp) else fmtp.add(tryFmtp)
                    return
                }
                val tryControl = SdpGrammar.tryParseControl(value)
                if (tryControl != null) {
                    if (lastMedia != null) lastMedia.control = tryControl else control = tryControl
                    return
                }
                val tryRtcp = SdpGrammar.tryParseRtcp(value)
                if (tryRtcp != null) {
                    if (lastMedia != null) lastMedia.rtcp = tryRtcp else rtcp = tryRtcp
                    return
                }
                val tryRtcpfbtrrint = SdpGrammar.tryParseRtcpfbtrrint(value)
                if (tryRtcpfbtrrint != null) {
                    if (lastMedia != null) {
                        lastMedia.rtcpFbTrrInt.add(
                            tryRtcpfbtrrint,
                        )
                    } else {
                        rtcpFbTrrInt.add(tryRtcpfbtrrint)
                    }
                    return
                }
                val tryRtcpfb = SdpGrammar.tryParseRtcpfb(value)
                if (tryRtcpfb != null) {
                    if (lastMedia != null) {
                        lastMedia.rtcpFb.add(
                            tryRtcpfb,
                        )
                    } else {
                        rtcpFb.add(tryRtcpfb)
                    }
                    return
                }
                val tryExt = SdpGrammar.tryParseExt(value)
                if (tryExt != null) {
                    if (lastMedia != null) lastMedia.ext.add(tryExt) else ext.add(tryExt)
                    return
                }
                val tryExtmapallowmixed = SdpGrammar.tryParseExtmapallowmixed(value)
                if (tryExtmapallowmixed != null) {
                    if (lastMedia != null) lastMedia.extmapAllowMixed = tryExtmapallowmixed else extmapAllowMixed = tryExtmapallowmixed
                    return
                }
                val tryCrypto = SdpGrammar.tryParseCrypto(value)
                if (tryCrypto != null) {
                    if (lastMedia != null) {
                        lastMedia.crypto.add(
                            tryCrypto,
                        )
                    } else {
                        crypto.add(tryCrypto)
                    }
                    return
                }
                val trySetup = SdpGrammar.tryParseSetup(value)
                if (trySetup != null) {
                    if (lastMedia != null) lastMedia.setup = trySetup else setup = trySetup
                    return
                }
                val tryConnectiontype = SdpGrammar.tryParseConnectiontype(value)
                if (tryConnectiontype != null) {
                    if (lastMedia != null) lastMedia.connectionType = tryConnectiontype else connectionType = tryConnectiontype
                    return
                }
                val tryMid = SdpGrammar.tryParseMid(value)
                if (tryMid != null) {
                    if (lastMedia != null) lastMedia.mid = tryMid
                    return
                }
                val tryMsid = SdpGrammar.tryParseMsid(value)
                if (tryMsid != null) {
                    if (lastMedia != null) lastMedia.msid = tryMsid
                    return
                }
                val tryPtime = SdpGrammar.tryParsePtime(value)
                if (tryPtime != null) {
                    if (lastMedia != null) lastMedia.ptime = tryPtime else ptime = tryPtime
                    return
                }
                val tryMaxptime = SdpGrammar.tryParseMaxptime(value)
                if (tryMaxptime != null) {
                    if (lastMedia != null) lastMedia.maxptime = tryMaxptime else maxptime = tryMaxptime
                    return
                }
                val tryDirection = SdpGrammar.tryParseDirection(value)
                if (tryDirection != null) {
                    if (lastMedia != null) lastMedia.direction = tryDirection else direction = tryDirection
                    return
                }
                val tryIcelite = SdpGrammar.tryParseIcelite(value)
                if (tryIcelite != null) {
                    if (lastMedia != null) lastMedia.icelite = tryIcelite else icelite = tryIcelite
                    return
                }
                val tryIceufrag = SdpGrammar.tryParseIceufrag(value)
                if (tryIceufrag != null) {
                    if (lastMedia != null) lastMedia.iceUfrag = tryIceufrag else iceUfrag = tryIceufrag
                    return
                }
                val tryIcepwd = SdpGrammar.tryParseIcepwd(value)
                if (tryIcepwd != null) {
                    if (lastMedia != null) lastMedia.icePwd = tryIcepwd else icePwd = tryIcepwd
                    return
                }
                val tryFingerprint = SdpGrammar.tryParseFingerprint(value)
                if (tryFingerprint != null) {
                    if (lastMedia != null) lastMedia.fingerprint = tryFingerprint else fingerprint = tryFingerprint
                    return
                }
                val tryCandidates = SdpGrammar.tryParseCandidates(value)
                if (tryCandidates != null) {
                    if (lastMedia != null) {
                        lastMedia.candidates.add(
                            tryCandidates,
                        )
                    } else {
                        candidates.add(tryCandidates)
                    }
                    return
                }
                val tryEndofcandidates = SdpGrammar.tryParseEndofcandidates(value)
                if (tryEndofcandidates != null) {
                    if (lastMedia != null) lastMedia.endOfCandidates = tryEndofcandidates else endOfCandidates = tryEndofcandidates
                    return
                }
                val tryRemotecandidates = SdpGrammar.tryParseRemotecandidates(value)
                if (tryRemotecandidates != null) {
                    if (lastMedia != null) lastMedia.remoteCandidates = tryRemotecandidates else remoteCandidates = tryRemotecandidates
                    return
                }
                val tryIceoptions = SdpGrammar.tryParseIceoptions(value)
                if (tryIceoptions != null) {
                    if (lastMedia != null) lastMedia.iceOptions = tryIceoptions else iceOptions = tryIceoptions
                    return
                }
                val trySsrcs = SdpGrammar.tryParseSsrcs(value)
                if (trySsrcs != null) {
                    if (lastMedia != null) lastMedia.ssrcs.add(trySsrcs) else ssrcs.add(trySsrcs)
                    return
                }
                val trySsrcgroups = SdpGrammar.tryParseSsrcgroups(value)
                if (trySsrcgroups != null) {
                    if (lastMedia != null) {
                        lastMedia.ssrcGroups.add(
                            trySsrcgroups,
                        )
                    } else {
                        ssrcGroups.add(trySsrcgroups)
                    }
                    return
                }
                val tryMsidsemantic = SdpGrammar.tryParseMsidsemantic(value)
                if (tryMsidsemantic != null) {
                    if (lastMedia != null) lastMedia.msidSemantic = tryMsidsemantic else msidSemantic = tryMsidsemantic
                    return
                }
                val tryGroups = SdpGrammar.tryParseGroups(value)
                if (tryGroups != null) {
                    if (lastMedia != null) {
                        lastMedia.groups.add(
                            tryGroups,
                        )
                    } else {
                        groups.add(tryGroups)
                    }
                    return
                }
                val tryRtcpmux = SdpGrammar.tryParseRtcpmux(value)
                if (tryRtcpmux != null) {
                    if (lastMedia != null) lastMedia.rtcpMux = tryRtcpmux else rtcpMux = tryRtcpmux
                    return
                }
                val tryRtcprsize = SdpGrammar.tryParseRtcprsize(value)
                if (tryRtcprsize != null) {
                    if (lastMedia != null) lastMedia.rtcpRsize = tryRtcprsize else rtcpRsize = tryRtcprsize
                    return
                }
                val trySctpmap = SdpGrammar.tryParseSctpmap(value)
                if (trySctpmap != null) {
                    if (lastMedia != null) lastMedia.sctpmap = trySctpmap else sctpmap = trySctpmap
                    return
                }
                val tryXgoogleflag = SdpGrammar.tryParseXgoogleflag(value)
                if (tryXgoogleflag != null) {
                    if (lastMedia != null) lastMedia.xGoogleFlag = tryXgoogleflag else xGoogleFlag = tryXgoogleflag
                    return
                }
                val tryRids = SdpGrammar.tryParseRids(value)
                if (tryRids != null) {
                    if (lastMedia != null) lastMedia.rids.add(tryRids) else rids.add(tryRids)
                    return
                }
                val tryImageattrs = SdpGrammar.tryParseImageattrs(value)
                if (tryImageattrs != null) {
                    if (lastMedia != null) {
                        lastMedia.imageattrs.add(
                            tryImageattrs,
                        )
                    } else {
                        imageattrs.add(tryImageattrs)
                    }
                    return
                }
                val trySimulcast = SdpGrammar.tryParseSimulcast(value)
                if (trySimulcast != null) {
                    if (lastMedia != null) lastMedia.simulcast = trySimulcast else simulcast = trySimulcast
                    return
                }
                val trySimulcast03 = SdpGrammar.tryParseSimulcast03(value)
                if (trySimulcast03 != null) {
                    if (lastMedia != null) lastMedia.simulcast03 = trySimulcast03 else simulcast03 = trySimulcast03
                    return
                }
                val tryFramerate = SdpGrammar.tryParseFramerate(value)
                if (tryFramerate != null) {
                    if (lastMedia != null) lastMedia.framerate = tryFramerate else framerate = tryFramerate
                    return
                }
                val trySourcefilter = SdpGrammar.tryParseSourcefilter(value)
                if (trySourcefilter != null) {
                    if (lastMedia != null) lastMedia.sourceFilter = trySourcefilter else sourceFilter = trySourcefilter
                    return
                }
                val tryBundleonly = SdpGrammar.tryParseBundleonly(value)
                if (tryBundleonly != null) {
                    if (lastMedia != null) lastMedia.bundleOnly = tryBundleonly else bundleOnly = tryBundleonly
                    return
                }
                val tryLabel = SdpGrammar.tryParseLabel(value)
                if (tryLabel != null) {
                    if (lastMedia != null) lastMedia.label = tryLabel else label = tryLabel
                    return
                }
                val trySctpport = SdpGrammar.tryParseSctpport(value)
                if (trySctpport != null) {
                    if (lastMedia != null) lastMedia.sctpPort = trySctpport else sctpPort = trySctpport
                    return
                }
                val tryMaxmessagesize = SdpGrammar.tryParseMaxmessagesize(value)
                if (tryMaxmessagesize != null) {
                    if (lastMedia != null) lastMedia.maxMessageSize = tryMaxmessagesize else maxMessageSize = tryMaxmessagesize
                    return
                }
                val tryTsrefclocks = SdpGrammar.tryParseTsrefclocks(value)
                if (tryTsrefclocks != null) {
                    if (lastMedia != null) {
                        lastMedia.tsRefClocks.add(
                            tryTsrefclocks,
                        )
                    } else {
                        tsRefClocks.add(tryTsrefclocks)
                    }
                    return
                }
                val tryMediaclk = SdpGrammar.tryParseMediaclk(value)
                if (tryMediaclk != null) {
                    if (lastMedia != null) lastMedia.mediaClk = tryMediaclk else mediaClk = tryMediaclk
                    return
                }
                val tryKeywords = SdpGrammar.tryParseKeywords(value)
                if (tryKeywords != null) {
                    if (lastMedia != null) lastMedia.keywords = tryKeywords else keywords = tryKeywords
                    return
                }
                val tryContent = SdpGrammar.tryParseContent(value)
                if (tryContent != null) {
                    if (lastMedia != null) lastMedia.content = tryContent else content = tryContent
                    return
                }
                val tryBfcpfloorctrl = SdpGrammar.tryParseBfcpfloorctrl(value)
                if (tryBfcpfloorctrl != null) {
                    if (lastMedia != null) lastMedia.bfcpFloorCtrl = tryBfcpfloorctrl else bfcpFloorCtrl = tryBfcpfloorctrl
                    return
                }
                val tryBfcpconfid = SdpGrammar.tryParseBfcpconfid(value)
                if (tryBfcpconfid != null) {
                    if (lastMedia != null) lastMedia.bfcpConfId = tryBfcpconfid else bfcpConfId = tryBfcpconfid
                    return
                }
                val tryBfcpuserid = SdpGrammar.tryParseBfcpuserid(value)
                if (tryBfcpuserid != null) {
                    if (lastMedia != null) lastMedia.bfcpUserId = tryBfcpuserid else bfcpUserId = tryBfcpuserid
                    return
                }
                val tryBfcpfloorid = SdpGrammar.tryParseBfcpfloorid(value)
                if (tryBfcpfloorid != null) {
                    if (lastMedia != null) lastMedia.bfcpFloorId = tryBfcpfloorid else bfcpFloorId = tryBfcpfloorid
                    return
                }
                val tryInvalid = SdpGrammar.tryParseInvalid(value)
                if (tryInvalid != null) {
                    if (lastMedia != null) {
                        lastMedia.invalid.add(
                            tryInvalid,
                        )
                    } else {
                        invalid.add(tryInvalid)
                    }
                    return
                }
            }
        }
    }

    override fun equals(other: Any?) = (other is SdpSession) &&
        media == other.media &&
        version == other.version &&
        origin == other.origin &&
        name == other.name &&
        description == other.description &&
        uri == other.uri &&
        email == other.email &&
        phone == other.phone &&
        timezones == other.timezones &&
        repeats == other.repeats &&
        timing == other.timing &&
        connection == other.connection &&
        bandwidth == other.bandwidth &&
        mline == other.mline &&
        rtp == other.rtp &&
        fmtp == other.fmtp &&
        control == other.control &&
        rtcp == other.rtcp &&
        rtcpFbTrrInt == other.rtcpFbTrrInt &&
        rtcpFb == other.rtcpFb &&
        ext == other.ext &&
        extmapAllowMixed == other.extmapAllowMixed &&
        crypto == other.crypto &&
        setup == other.setup &&
        connectionType == other.connectionType &&
        mid == other.mid &&
        msid == other.msid &&
        ptime == other.ptime &&
        maxptime == other.maxptime &&
        direction == other.direction &&
        icelite == other.icelite &&
        iceUfrag == other.iceUfrag &&
        icePwd == other.icePwd &&
        fingerprint == other.fingerprint &&
        candidates == other.candidates &&
        endOfCandidates == other.endOfCandidates &&
        remoteCandidates == other.remoteCandidates &&
        iceOptions == other.iceOptions &&
        ssrcs == other.ssrcs &&
        ssrcGroups == other.ssrcGroups &&
        msidSemantic == other.msidSemantic &&
        groups == other.groups &&
        rtcpMux == other.rtcpMux &&
        rtcpRsize == other.rtcpRsize &&
        sctpmap == other.sctpmap &&
        xGoogleFlag == other.xGoogleFlag &&
        rids == other.rids &&
        imageattrs == other.imageattrs &&
        simulcast == other.simulcast &&
        simulcast03 == other.simulcast03 &&
        framerate == other.framerate &&
        sourceFilter == other.sourceFilter &&
        bundleOnly == other.bundleOnly &&
        label == other.label &&
        sctpPort == other.sctpPort &&
        maxMessageSize == other.maxMessageSize &&
        tsRefClocks == other.tsRefClocks &&
        mediaClk == other.mediaClk &&
        keywords == other.keywords &&
        content == other.content &&
        bfcpFloorCtrl == other.bfcpFloorCtrl &&
        bfcpConfId == other.bfcpConfId &&
        bfcpUserId == other.bfcpUserId &&
        bfcpFloorId == other.bfcpFloorId &&
        invalid == other.invalid
}
