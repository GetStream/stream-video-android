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

package io.getstream.video.android.ui.common

import io.getstream.video.android.core.Call

public interface StreamActivityUiDelegate<T : StreamCallActivity> {

    /**
     * Called when the [setContent] cannot yet be invoked (missing [Call] for e.g.).
     * Used to show some UI in the activity if the call is not yet loaded.
     */
    public fun loadingContent(activity: T)

    /**
     * Set the content for the activity,
     *
     * @param activity the activity
     * @param call the call
     */
    public fun setContent(activity: T, call: Call)
}
