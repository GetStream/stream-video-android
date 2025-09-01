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
import io.getstream.video.android.tooling.util.StreamBuildFlavorUtil
import io.getstream.video.android.util.StreamVideoInitHelper
import kotlinx.coroutines.runBlocking

@HiltAndroidApp
class App : Application() {

    override fun onCreate() {
        super.onCreate()
    }
}

val Context.app get() = applicationContext as App

val defaultCallId get() = if (StreamBuildFlavorUtil.isDevelopment) {
    "default:79cYh3J5JgGk"
} else {
    ""
}
