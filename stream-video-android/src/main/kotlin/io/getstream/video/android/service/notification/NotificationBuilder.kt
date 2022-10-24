package io.getstream.video.android.service.notification

import android.app.Notification
import io.getstream.video.android.model.state.StreamCallState

/**
 * Handler responsible for showing and dismissing notification.
 */
public interface NotificationBuilder {

    /**
     * Shows a notification for the given [state].
     */
    public fun build(state: StreamCallState): Notification

}