/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.xml.binding

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import io.getstream.video.android.core.viewmodel.CallViewModel
import io.getstream.video.android.xml.widget.appbar.CallAppBarView
import kotlinx.coroutines.flow.collectLatest

public fun CallAppBarView.bindView(
    viewModel: CallViewModel,
    lifecycleOwner: LifecycleOwner,
    onBackPressed: () -> Unit = { },
    onParticipantsPressed: () -> Unit = { }
) {
    this.onBackPressed = onBackPressed
    this.onParticipantsPressed = onParticipantsPressed

    lifecycleOwner.lifecycleScope.launchWhenResumed {
        viewModel.streamCallState.collectLatest {
            renderState(it)
        }
    }
}
