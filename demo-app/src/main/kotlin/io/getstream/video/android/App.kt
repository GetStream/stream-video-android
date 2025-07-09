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

package io.getstream.video.android

import android.app.Application
import android.content.Context
import dagger.hilt.android.HiltAndroidApp
import io.getstream.video.android.datastore.delegate.StreamUserDataStore
import io.getstream.video.android.tooling.util.StreamFlavors
import io.getstream.video.android.util.StreamVideoInitHelper
import kotlinx.coroutines.runBlocking

@HiltAndroidApp
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // We use the provided StreamUserDataStore in the demo app for user data storage.
        // This is a convenience class provided for storage but the SDK itself is not aware of
        // this instance and doesn't use it. You can use it to store the logged in user and then
        // retrieve the information for SDK initialisation.
        StreamUserDataStore.install(this, isEncrypted = true)

        // Demo helper for initialising the Video and Chat SDK instances from one place.
        // For simpler code we "inject" the Context manually instead of using DI.
        StreamVideoInitHelper.init(this)
        // Prepare the Video SDK if we already have a user logged in the demo app.
        // If you need to receive push messages (incoming call) then the SDK must be initialised
        // in Application.onCreate. Otherwise it doesn't know how to init itself when push arrives
        // and will ignore the push messages.
        // If push messages are not used then you don't need to init here - you can init
        // on-demand (initialising here is usually less error-prone).
        runBlocking {
            StreamVideoInitHelper.loadSdk(
                dataStore = StreamUserDataStore.instance(),
                useRandomUserAsFallback = false,
            )
        }
    }
}

val Context.app get() = applicationContext as App

val defaultCallId get() = if (BuildConfig.FLAVOR == StreamFlavors.development) {
    "default:79cYh3J5JgGk"
} else {
    ""
}
