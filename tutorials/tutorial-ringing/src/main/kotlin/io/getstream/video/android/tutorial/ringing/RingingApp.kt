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

package io.getstream.video.android.tutorial.ringing

import android.app.Application
import android.content.Context
import com.google.firebase.FirebaseApp
import io.getstream.android.push.firebase.FirebasePushDeviceGenerator
import io.getstream.android.samples.ringingcall.FirebasePushService
import io.getstream.log.Priority
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.GEO
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.core.logging.HttpLoggingLevel
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.core.notifications.NotificationConfig
import io.getstream.video.android.core.permission.android.StreamPermissionCheck

class RingingApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize firebase first.
        // Ensure that you have the correct service account credentials updated in the Stream Dashboard.
        FirebaseApp.initializeApp(this)

        // You do not have to use runBlocking { } to initialize the StreamVideo instance. Its only for the purpose of loading the mock user.
    }

    companion object {

        val caller get() = TutorialUser.builtIn.first { it.id == StreamVideo.instance().user.id }

        val callees get() = TutorialUser.builtIn.filter { it.id != StreamVideo.instance().user.id }

        fun login(context: Context, user: TutorialUser) {
            if (!StreamVideo.isInstalled) {
                // step2 - initialize StreamVideo. For a production app we recommend adding
                // the client to your Application class or di module.
                StreamVideoBuilder(
                    context = context.applicationContext,
//                    apiKey = "mmhfdzb5evj2",
//                    apiKey = "par8f5s3gn2j",
                    apiKey = "hd8szvscpxvd",
                    geo = GEO.GlobalEdgeNetwork,
                    user = user.delegate,
                    token = user.token,
                    loggingLevel = LoggingLevel(Priority.VERBOSE, HttpLoggingLevel.BODY),
                    notificationConfig = NotificationConfig(
                        // Make the notification low prio if the app is in foreground, so its not visible as a popup, since we want to handle
                        // the incoming call in full screen when app is running.
                        hideRingingNotificationInForeground = true,
                        // Make sure that the provider name is equal to the "Name" of the configuration in Stream Dashboard.
                        pushDeviceGenerators = listOf(FirebasePushDeviceGenerator(providerName = FirebasePushService.PROVIDER_NAME)),
                    ),
                ).build()
            }
        }

        fun logout() {
            if (StreamVideo.isInstalled) {
                StreamVideo.instance().logOut()
            }
            StreamVideo.removeClient()
        }
    }
}
