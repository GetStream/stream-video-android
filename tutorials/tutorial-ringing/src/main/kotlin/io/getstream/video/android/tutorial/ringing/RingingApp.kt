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
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import io.getstream.android.push.PushProvider
import io.getstream.android.push.firebase.FirebasePushDeviceGenerator
import io.getstream.video.android.core.GEO
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.core.notifications.NotificationConfig
import io.getstream.video.android.model.Device
import io.getstream.video.android.model.User
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await

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

        // Use a more robust approach in a production app
        val fcmToken: String?
            get() = runBlocking {
                try {
                    FirebaseMessaging.getInstance().token.await()
                } catch (e: Exception) {
                    Log.e("FCM Token", "Failed to retrieve FCM token", e)
                    null
                }
            }

        fun login(context: Context, user: TutorialUser) {
            if (!StreamVideo.isInstalled) {
                currentUser = user
                // Initialize StreamVideo. For a production app we recommend adding
                // the client to your Application class or di module.
                StreamVideoBuilder(
                    context = context.applicationContext,
                    apiKey = "mmhfdzb5evj2",
                    geo = GEO.GlobalEdgeNetwork,
                    user = user.delegate,
                    token = user.token,
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

        suspend fun logout() {
            if (StreamVideo.isInstalled) {
                with(StreamVideo.instance()) {
                    fcmToken?.let {
                        deleteDevice(
                            Device(
                                id = it,
                                pushProvider = PushProvider.FIREBASE.key,
                                pushProviderName = "firebase",
                            ),
                        )
                    }
                    logOut()
                }

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
                ),
                token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiYW5kcm9pZC10dXRvcmlhbC0xIn0.3qB-FI6OAqf5ZEETtgs0XhaMmiaRF2jDJOqCVRsqqbc",
            ),
            TutorialUser(
                User(
                    id = "android-tutorial-2",
                    name = "User 2",
                ),
                token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiYW5kcm9pZC10dXRvcmlhbC0yIn0.ksOq5ahC0745oZdPDKr2hyLp0j9exfwLE-AQITc9ZSc",
            ),
            TutorialUser(
                User(
                    id = "android-tutorial-3",
                    name = "User 3",
                ),
                token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiYW5kcm9pZC10dXRvcmlhbC0zIn0.g5h8coX8J1XUNHagPFoGBI0D7bN6P0w2Sd2rui89puE",
            ),
        )
    }
}
