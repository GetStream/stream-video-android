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
import io.getstream.android.push.PushDevice
import io.getstream.video.android.contentprovider.BaseContentProvider
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.notifications.interceptor.StreamVideoPushInterceptor
import io.getstream.video.android.core.notifications.interceptor.VideoPushEventInterceptor
import io.getstream.video.android.datastore.delegate.StreamUserDataStore
import io.getstream.video.android.util.StreamVideoInitHelper
import kotlinx.coroutines.runBlocking

class SdkInitProvider : BaseContentProvider() {

    override fun onCreate(): Boolean {
        Log.d(TAG, "onCreate")
        val ctx = context

        StreamVideoPushInterceptor.interceptor = object : VideoPushEventInterceptor {
            override fun registerPushDeviceHook(pushDevice: PushDevice): Boolean {
                if (ctx != null) {
                    initSdk(ctx, "registerPushDeviceHook")
                    return true
                } else {
                    return false
                }
            }

            override fun onRemoteMessageHook(
                metadata: Map<String, Any?>,
                payload: Map<String, Any?>,
            ): Boolean {
                if (ctx != null) {
                    initSdk(ctx, "onRemoteMessageHook")
                    return true
                } else {
                    return false
                }
            }
        }
        return true
    }

    companion object {
        private val TAG = "SdkInitProvider"

        fun initSdk(ctx: Context, source: String) {
            Log.d(TAG, "[initSdk], source: $source")
//            FirebaseApp.initializeApp(ctx)
            if (StreamVideo.instanceOrNull() != null) {
                Log.d(TAG, "SDK is already installed, source: $source")
                return
            }
            StreamUserDataStore.install(ctx, isEncrypted = true)

            // Demo helper for initialising the Video and Chat SDK instances from one place.
            // For simpler code we "inject" the Context manually instead of using DI.
            StreamVideoInitHelper.init(ctx)

            // Prepare the Video SDK if we already have a user logged in the demo app.
            // If you need to receive push messages (incoming call) then the SDK must be initialised
            // in Application.onCreate. Otherwise it doesn't know how to init itself when push arrives
            // and will ignore the push messages.
            // If push messages are not used then you don't need to init here - you can init
            // on-demand (initialising here is usually less error-prone).
            runBlocking {
                if (source == "App") {
                    StreamVideoInitHelper.loadSdk(
                        dataStore = StreamUserDataStore.instance(),
                        useRandomUserAsFallback = false,
                    )
                } else {
                    StreamVideoInitHelper.loadSdkFromPNFlow(
                        dataStore = StreamUserDataStore.instance(),
                    )
                }
            }
            Log.d(TAG, "[initSdk] Finish, source: $source")
        }
    }
}
