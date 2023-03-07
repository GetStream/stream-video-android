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
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.model.state.StreamCallState
import io.getstream.video.android.core.viewmodel.CallViewModel
import io.getstream.video.android.xml.widget.callcontainer.CallContainerView
import kotlinx.coroutines.flow.combine

public fun CallContainerView.bindView(
    viewModel: CallViewModel,
    lifecycleOwner: LifecycleOwner,
    handleCallAction: (CallAction) -> Unit = { viewModel.onCallAction(it) },
    handleBackPressed: () -> Unit = { },
    onIdle: () -> Unit = { },
) {
    this.handleBackPressed = { handleBackPressed() }

    lifecycleOwner.lifecycleScope.launchWhenCreated {
        viewModel.streamCallState.combine(viewModel.isInPictureInPicture) { state, isPictureInPicture ->
            state to isPictureInPicture
        }.collect { (state, isPictureInPicture) ->
            updateToolbar(state, isPictureInPicture)
            when {
                state is StreamCallState.Incoming && !state.acceptedByMe ->
                    showIncomingScreen()?.apply { bindView(viewModel, lifecycleOwner) }

                state is StreamCallState.Outgoing && !state.acceptedByCallee ->
                    showOutgoingScreen()?.apply { bindView(viewModel, lifecycleOwner) }

                state is StreamCallState.Connected && isPictureInPicture ->
                    showPipLayout()?.apply { bindView(viewModel, lifecycleOwner) }

                state is StreamCallState.Idle -> onIdle()

                else -> showCallContentScreen()?.apply {
                    bindView(
                        viewModel = viewModel,
                        lifecycleOwner = lifecycleOwner,
                        onCallAction = handleCallAction
                    )
                }
            }
        }
    }
}
