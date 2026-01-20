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

package io.getstream.video.android.datastore.serializer

import androidx.datastore.core.Serializer
import com.google.crypto.tink.Aead
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

internal class EncryptedSerializer<T>(
    private val aead: Aead,
    private val delegate: Serializer<T>,
) : Serializer<T> by delegate {

    override suspend fun readFrom(input: InputStream): T {
        val encryptedBytes = input.readBytes()
        val bytes =
            if (encryptedBytes.isEmpty()) encryptedBytes else aead.decrypt(encryptedBytes, null)

        return delegate.readFrom(bytes.inputStream())
    }

    override suspend fun writeTo(t: T, output: OutputStream) {
        val bytes = ByteArrayOutputStream().use { stream ->
            delegate.writeTo(t, stream)
            stream.toByteArray()
        }
        val encryptedBytes = aead.encrypt(bytes, null)

        @Suppress("BlockingMethodInNonBlockingContext")
        output.write(encryptedBytes)
    }
}

internal fun <T> Serializer<T>.encrypted(aead: Aead): Serializer<T> =
    EncryptedSerializer(aead, this)
