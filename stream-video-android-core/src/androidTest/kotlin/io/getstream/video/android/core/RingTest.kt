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

package io.getstream.video.android.core

import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import io.getstream.log.taggedLogger
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/**
 * call.ring()
 * call.notify()
 * call.accept()
 * call.reject()
 * render 2 styles of push notifications
 * customize them
 *
 * see https://www.notion.so/stream-wiki/Push-Notifications-Call-Flow-a623dd508757404fa35ac4355a0bc814
 *
 */
class RingTest : IntegrationTestBase(connectCoordinatorWS = false) {

    private val logger by taggedLogger("Test:AndroidDeviceTest")

    @Test
    fun ring() = runTest(timeout = 10.seconds) {
        val call = client.call("default")
        val response = call.create(listOf("thierry", "tommaso"))
        assertSuccess(response)

        // ring a call
        val result = call.ring()
        assertSuccess(result)

        // verify event is received and so is the push
        // val event = waitForNextEvent<CallRingEvent>()

        // TODO: push
    }

    @Test
    fun notify2() = runTest {
        // ring a call
        val result = call.notify()
        assertSuccess(result)

        // verify event is received and so is the push
        // val event = waitForNextEvent<CallRingEvent>()

        // TODO: push
    }

    @Test
    fun accept() = runTest {
        val call = client.call("default")
        val response = call.create(listOf("thierry", "tommaso"), ring = true)
        assertSuccess(response)
        val acceptResponse = call.accept()
    }

    @Test
    fun reject() = runTest {
        val call = client.call("default")
        val response = call.create(listOf("thierry", "tommaso"), ring = true)
        assertSuccess(response)
        val rejectResponse = call.reject()
    }

    @Test
    fun basicPush() = runTest {
        val notificationManager = NotificationManagerCompat.from(context)
        val INCOMING_CALL_CHANNEL_ID = "incoming_call"
// Creating an object with channel data

        val channel = NotificationChannelCompat.Builder(
            INCOMING_CALL_CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_HIGH,
        ).setName("incoming call").setDescription("Incoming audio and video call alerts").build()

        notificationManager.createNotificationChannel(channel)

        /*

        val notificationBuilder = NotificationCompat.Builder(
            context,
            INCOMING_CALL_CHANNEL_ID

        )
            .setSmallIcon(R.drawable.icon)
            .setContentTitle("incoming call")
            .setContentText("James")


        .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.logo))
        .setOngoing(true)



        val notification = notificationBuilder.build()

        val INCOMING_CALL_NOTIFICATION_ID = 1 // call to int
        notificationManager.notify(INCOMING_CALL_NOTIFICATION_ID, notification)


        const val ACTION_ACCEPT_CALL = 101



// We create a normal intent, just like when we start a new Activity



        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            action = ACTION_ACCEPT_CALL
        }



        val acceptCallIntent = PendingIntent.getActivity(applicationContext, REQUEST_CODE_ACCEPT_CALL, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val action = NotificationCompat.Action.Builder(
            IconCompat.createWithResource(applicationContext, R.drawable.icon_accept_call),
            getString(R.string.accept_call),
            // The action itself, PendingIntent
            acceptCallIntent

        ).build()


    //notificationManager.cancel(INCOMING_CALL_NOTIFICATION_ID) */
    }
}
