package io.getstream.video.android.ui.common

import io.getstream.video.android.core.Call

public interface StreamActivityUiDelegate<T: StreamCallActivity> {

    /**
     * Set the content for the activity,
     *
     * @param activity the activity
     * @param call the call
     */
    public fun setContent(activity: T, call: Call)
}