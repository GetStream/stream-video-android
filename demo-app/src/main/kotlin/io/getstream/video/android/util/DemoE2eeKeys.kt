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

package io.getstream.video.android.util

import java.util.concurrent.ConcurrentHashMap

/**
 * Name of the invite-link query parameter carrying the shared passphrase. Read when joining and
 * written when sharing, so it lives in one place — the two sides have to agree on it, and they
 * have to agree with the web demo.
 */
internal const val E2EE_KEY_QUERY_PARAM = "encryption_key"

/**
 * Passphrases of the calls this process joined encrypted, so the in-call share sheet can put one
 * back on the invite link.
 *
 * Memory only, and deliberately: the passphrase is the key. Persisting it would leave key material
 * on disk, and putting it anywhere the coordinator stores — custom call data, a member field —
 * would hand the server the key and make the encryption pointless.
 */
internal object DemoE2eeKeys {

    private val passphrases = ConcurrentHashMap<String, String>()

    fun remember(cid: String, passphrase: String) {
        passphrases[cid] = passphrase
    }

    /** The passphrase for [cid], or null when this process did not join that call encrypted. */
    fun of(cid: String): String? = passphrases[cid]

    fun forget(cid: String) {
        passphrases.remove(cid)
    }
}
