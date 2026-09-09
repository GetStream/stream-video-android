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

/**
 * Name of the invite-link query parameter carrying the shared passphrase. Read when joining and
 * written when sharing, so it lives in one place — the two sides have to agree on it, and they
 * have to agree with the web demo.
 */
internal const val E2EE_KEY_QUERY_PARAM = "encryption_key"

/**
 * The passphrase of the call being joined, so the in-call share sheet can put it back on the
 * invite link. The passphrase travels in that link anyway, so this is not about keeping it secret.
 *
 * It holds one call at a time, and [of] answers only for the cid it was stored against. Applying
 * one call's passphrase to another is the thing to prevent: everyone who saw the first call's link
 * could then decrypt the second. Demo call IDs are short and get reused, so a per-cid cache would
 * happily serve a passphrase from an earlier session of the same ID — hence a single slot that
 * every join overwrites, rather than a map that accumulates.
 */
internal object DemoE2eeKeys {

    @Volatile
    private var current: Entry? = null

    private data class Entry(val cid: String, val passphrase: String)

    fun remember(cid: String, passphrase: String) {
        current = Entry(cid, passphrase)
    }

    /** The passphrase stored for [cid], or null when the stored one belongs to another call. */
    fun of(cid: String): String? = current?.takeIf { it.cid == cid }?.passphrase

    fun forget(cid: String) {
        if (current?.cid == cid) current = null
    }
}
