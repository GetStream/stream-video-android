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

package io.getstream.video.android.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.permission.PermissionManager

public class CallViewModelFactory(
    private val streamVideo: StreamVideo,
    private val call: Call,
    private val permissionManager: PermissionManager?
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CallViewModel(
            client = streamVideo,
            call = call,
            permissionManager = permissionManager,
        ) as T
    }
}
