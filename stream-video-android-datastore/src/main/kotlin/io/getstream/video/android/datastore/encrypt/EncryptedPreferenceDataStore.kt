/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.datastore.encrypt

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.google.crypto.tink.Aead
import io.getstream.video.android.datastore.encrypt.PreferenceDataStoreHack.fileExtension
import io.getstream.video.android.datastore.encrypt.PreferenceDataStoreHack.serializer
import io.getstream.video.android.datastore.encrypt.PreferenceDataStoreHack.wrap
import io.getstream.video.android.datastore.serializer.encrypted
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

@Suppress("unused")
internal fun PreferenceDataStoreFactory.createEncrypted(
    aead: Aead,
    corruptionHandler: ReplaceFileCorruptionHandler<Preferences>? = null,
    migrations: List<DataMigration<Preferences>> = listOf(),
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    produceFile: () -> File,
): DataStore<Preferences> {
    val delegate = DataStoreFactory.create(
        serializer = serializer.encrypted(aead),
        corruptionHandler = corruptionHandler,
        migrations = migrations,
        scope = scope,
    ) {
        val file = produceFile()
        check(file.extension == fileExtension) {
            "File extension for file: $file does not match required extension for" +
                " Preferences file: $fileExtension"
        }
        file
    }

    return wrap(delegate)
}
