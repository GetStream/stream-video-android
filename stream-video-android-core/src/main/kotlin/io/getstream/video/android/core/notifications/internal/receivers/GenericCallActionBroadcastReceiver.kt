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

package io.getstream.video.android.core.notifications.internal.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.notifications.NotificationHandler
import io.getstream.video.android.model.streamCallId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal abstract class GenericCallActionBroadcastReceiver : BroadcastReceiver() {

    private val logger by taggedLogger("Call:ActionableReceiver")
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * The action that needs to be handled by the child receivers.
     */
    internal abstract val action: String

    override fun onReceive(context: Context?, intent: Intent?) {
        logger.v { "[onReceive] #ringing; context: $context, intent: $intent" }
        if (context != null && intent != null && intent.action != null) {
            val intentAction = intent.action!!
            if (action != intentAction) {
                // End the onReceive as declared action matches the intent action.
                return
            }
            // Extract call id
            val streamCallId = intent.streamCallId(NotificationHandler.INTENT_EXTRA_CALL_CID)
            // We want the broadcast to stay alive so we can finish the coroutine.
            val pendingResult = goAsync()

            if (streamCallId != null) {
                scope.launch {
                    try {
                        // Get stream video
                        val streamVideo: StreamVideo? = StreamVideo.instanceOrNull()
                        if (streamVideo == null) {
                            // Stream not initialized, action not handled
                            logger.e(
                                createMessage(
                                    intentAction,
                                    "StreamVideo is not initialised. To handle notifications to initialise StreamVideo in Application.onCreate().",
                                ),
                            )
                        } else {
                            val call = streamVideo.call(streamCallId.type, streamCallId.id)
                            onReceive(call, context, intent) // Invoke the actual action handler
                        }
                    } catch (e: Throwable) {
                        // Something happened
                        logger.e(
                            e,
                            createMessage(
                                intentAction,
                                "An error occured while invoking the action.",
                            ),
                        )
                    }
                    // Finish the broadcast regardless
                    pendingResult.finish()
                }
            } else {
                logger.w(createMessage(intentAction, "Stream call ID is not provided."))
            }
        } else {
            logger.w { "[onReceive] #ringing; Context or Intent or Action is null." }
        }
    }

    private fun createMessage(action: String, subMessage: String): () -> String = {
        "Received action $action, but: $subMessage"
    }

    /**
     * Called in the actual [onReceive], but with already extracted call object and intent.
     *
     * @param call the stream call.
     * @param intent the intent that came into the broadcast.
     * @param context the context
     */
    abstract suspend fun onReceive(call: Call, context: Context, intent: Intent)
}
