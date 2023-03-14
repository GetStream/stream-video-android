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
import io.getstream.video.android.core.viewmodel.CallViewModel
import io.getstream.video.android.xml.widget.participant.PictureInPictureView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

/**
 * Binds [PictureInPictureView] with [CallViewModel], updating the view's state based on data provided by the ViewModel,
 * and propagating view events to the ViewModel as needed.
 *
 * This function sets listeners on the view and ViewModel. Call this method
 * before setting any additional listeners on these objects yourself.
 *
 * @param viewModel [CallViewModel] for observing data and running actions.
 * @param lifecycleOwner The lifecycle owner, root component containing [PictureInPictureView]. Usually an Activity or
 * Fragment.
 */
public fun PictureInPictureView.bindView(
    viewModel: CallViewModel,
    lifecycleOwner: LifecycleOwner,
) {
    startJob(lifecycleOwner) {
        viewModel.callState.filterNotNull().collectLatest { call ->
            setRendererInitializer { videoRenderer, trackId, trackType, onRender ->
                call.initRenderer(videoRenderer, trackId, trackType, onRender)
            }
        }
    }

    startJob(lifecycleOwner) {
        viewModel.screenSharingSessions.map { it.firstOrNull() }.collectLatest {
            if (it != null) {
                setScreenShareView { it.bindView(viewModel, lifecycleOwner) }
            } else {
                setCallParticipantView { it.bindPictureInPictureView(viewModel, lifecycleOwner) }
            }
        }
    }
}
