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

import android.content.Context
import android.util.Log
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.datastore.delegate.StreamUserDataStore
import io.getstream.video.android.util.StreamVideoInitHelper
import kotlinx.coroutines.runBlocking

class SdkInitProvider {

    companion object {
        private val TAG = "SdkInitProvider"

        fun initSdk(ctx: Context, source: DemoAppSdkInitSource) {
            Log.d(TAG, "[initSdk], source: $source")
            if (StreamVideo.instanceOrNull() != null) {
                Log.d(TAG, "SDK is already installed, source: $source")
                return
            }
            StreamUserDataStore.install(ctx, isEncrypted = true)

            // Demo helper for initialising the Video and Chat SDK instances from one place.
            // For simpler code we "inject" the Context manually instead of using DI.
            StreamVideoInitHelper.init(ctx)

            runBlocking {
                StreamVideoInitHelper.loadSdk(
                    dataStore = StreamUserDataStore.instance(),
                    useRandomUserAsFallback = false,
                )
            }
        }
    }
}

sealed class DemoAppSdkInitSource(open val type: String) {
    public data object App : DemoAppSdkInitSource("app")
    public data object RegisterPushDevice : DemoAppSdkInitSource("RegisterPushDevice")
    public data object OnRemoteMessage : DemoAppSdkInitSource("OnRemoteMessage")
    public data class Custom(override val type: String) : DemoAppSdkInitSource(type)
}
