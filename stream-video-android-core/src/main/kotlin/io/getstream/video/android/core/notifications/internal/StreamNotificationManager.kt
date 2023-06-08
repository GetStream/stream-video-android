package io.getstream.video.android.core.notifications.internal

import io.getstream.log.TaggedLogger
import io.getstream.log.taggedLogger

internal class StreamNotificationManager private constructor(){

    internal companion object {
        private val logger: TaggedLogger by taggedLogger("StreamVideo:Notifications")
        private lateinit var internalStreamNotificationManager: StreamNotificationManager
        internal fun install(): StreamNotificationManager {
            synchronized(this) {
                if (Companion::internalStreamNotificationManager.isInitialized) {
                    logger.e {
                        "The $internalStreamNotificationManager is already installed but you've " +
                                "tried to install a new one."
                    }
                } else {
                    internalStreamNotificationManager = StreamNotificationManager()
                }
                return internalStreamNotificationManager
            }
        }
    }
}