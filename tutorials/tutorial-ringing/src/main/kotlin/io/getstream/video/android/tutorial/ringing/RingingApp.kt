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
import io.getstream.log.Priority
import io.getstream.video.android.core.GEO
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.core.logging.HttpLoggingLevel
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.core.notifications.NotificationConfig
import io.getstream.video.android.model.User

class RingingApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize firebase first.
        // Ensure that you have the correct service account credentials updated in the Stream Dashboard.
        FirebaseApp.initializeApp(this)
    }

    companion object {

        var currentUser: TutorialUser? = null

        val caller get() = currentUser ?: error("#caller; user not logged in")

        val callees get() = currentUser?.let { loggedInUser ->
            TutorialUser.builtIn.filter { it.id != loggedInUser.id }
        } ?: error("#callees; user not logged in")

        fun login(context: Context, user: TutorialUser) {
            if (!StreamVideo.isInstalled) {
                currentUser = user
                // step2 - initialize StreamVideo. For a production app we recommend adding
                // the client to your Application class or di module.
                StreamVideoBuilder(
                    context = context.applicationContext,
                    apiKey = "mmhfdzb5evj2",
                    geo = GEO.GlobalEdgeNetwork,
                    user = user.delegate,
                    token = user.token,
                    loggingLevel = LoggingLevel(Priority.VERBOSE, HttpLoggingLevel.BODY),
                    notificationConfig = NotificationConfig(
                        // Make the notification low priority if the app is in foreground, so its not visible as a popup, since we want to handle
                        // the incoming call in full screen when app is running.
                        hideRingingNotificationInForeground = true,
                        // Make sure that the provider name is equal to the "Name" of the configuration in Stream Dashboard.
                        pushDeviceGenerators = listOf(
                            FirebasePushDeviceGenerator(
                                context = context,
                                providerName = "firebase",
                            ),
                        ),

                        notificationHandler = CustomNotificationHandler(
                            context.applicationContext as Application,
                        ),
                    ),
                ).build()
            }
        }

        fun logout() {
            if (StreamVideo.isInstalled) {
                StreamVideo.instance().logOut()
                StreamVideo.removeClient()
                currentUser = null
            }
        }
    }
}

data class TutorialUser(
    val delegate: User,
    val token: String,
    val checked: Boolean = false,
) {
    val id: String get() = delegate.id
    val name: String? get() = delegate.name
    val image: String? get() = delegate.image

    companion object {
        val builtIn = listOf(
            TutorialUser(
                User(
                    id = "android-tutorial-1",
                    name = "User 1",
                    image = "https://getstream.io/chat/docs/sdk/avatars/jpg/Willard%20Hessel.jpg",
                ),
                token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiYW5kcm9pZC10dXRvcmlhbC0xIn0.3qB-FI6OAqf5ZEETtgs0XhaMmiaRF2jDJOqCVRsqqbc",
            ),
            TutorialUser(
                User(
                    id = "android-tutorial-2",
                    name = "User 2",
                    image = "https://getstream.io/chat/docs/sdk/avatars/jpg/Claudia%20Bradtke.jpg",
                ),
                token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiYW5kcm9pZC10dXRvcmlhbC0yIn0.ksOq5ahC0745oZdPDKr2hyLp0j9exfwLE-AQITc9ZSc",
            ),
            TutorialUser(
                User(
                    id = "android-tutorial-3",
                    name = "User 3",
                    image = "https://getstream.io/chat/docs/sdk/avatars/jpg/Bernard%20Windler.jpg",
                ),
                token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiYW5kcm9pZC10dXRvcmlhbC0zIn0.g5h8coX8J1XUNHagPFoGBI0D7bN6P0w2Sd2rui89puE",
            ),
        )
    }
}
