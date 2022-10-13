package io.getstream.video.android.app.lifecycle

import android.app.Activity
import android.os.Bundle

internal class StreamActivityLifecycleCallbacks(
    private val onActivityCreated: (Activity) -> Unit = {},
    private val onActivityStarted: (Activity) -> Unit = {},
    private val onLastActivityStopped: (Activity) -> Unit = {},
) : ActivityLifecycleCallbacks() {
    override fun onActivityCreated(activity: Activity, bunlde: Bundle?) {
        super.onActivityCreated(activity, bunlde)
        onActivityCreated.invoke(activity)
    }

    override fun onActivityStarted(activity: Activity) {
        super.onActivityStarted(activity)
        onActivityStarted.invoke(activity)
    }

    override fun onLastActivityStopped(activity: Activity) {
        super.onLastActivityStopped(activity)
        onLastActivityStopped.invoke(activity)
    }
}