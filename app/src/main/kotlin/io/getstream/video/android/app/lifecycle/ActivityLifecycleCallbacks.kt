package io.getstream.video.android.app.lifecycle

import android.app.Activity
import android.app.Application
import android.os.Bundle

internal abstract class ActivityLifecycleCallbacks : Application.ActivityLifecycleCallbacks {

    private var activityCount: Int = 0

    override fun onActivityCreated(activity: Activity, bunlde: Bundle?) { /* no-op */ }

    override fun onActivityStarted(activity: Activity) {
        if (activityCount++ == 0) {
            onFirstActivityStarted(activity)
        }
    }

    public open fun onFirstActivityStarted(activity: Activity) { /* no-op */ }

    override fun onActivityResumed(activity: Activity) { /* no-op */ }

    override fun onActivityPaused(activity: Activity) { /* no-op */ }

    override fun onActivityStopped(activity: Activity) {
        if (--activityCount == 0) {
            onLastActivityStopped(activity)
        }
    }

    public open fun onLastActivityStopped(activity: Activity) { /* no-op */ }

    override fun onActivitySaveInstanceState(activity: Activity, bunlde: Bundle) { /* no-op */ }

    override fun onActivityDestroyed(activity: Activity) { /* no-op */ }
}
