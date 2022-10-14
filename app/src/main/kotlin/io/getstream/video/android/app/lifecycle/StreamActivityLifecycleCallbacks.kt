/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.app.lifecycle

import android.app.Activity
import android.os.Bundle

internal class StreamActivityLifecycleCallbacks(
    private inline val onActivityCreated: (Activity) -> Unit = {},
    private inline val onActivityStarted: (Activity) -> Unit = {},
    private inline val onLastActivityStopped: (Activity) -> Unit = {},
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
