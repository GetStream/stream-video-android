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
import io.getstream.video.android.core.call.state.CallMediaState
import io.getstream.video.android.core.model.state.StreamCallState
import io.getstream.video.android.core.viewmodel.CallViewModel
import io.getstream.video.android.xml.widget.callcontainer.CallContainerView
import io.getstream.video.android.xml.widget.control.CallControlItem
import kotlinx.coroutines.flow.combine

public fun CallContainerView.bindView(
    viewModel: CallViewModel,
    lifecycleOwner: LifecycleOwner,
    updateCallMediaState: (CallMediaState, Boolean) -> List<CallControlItem> = { mediaState, isScreenSharingActive ->
        defaultControlList(mediaState, isScreenSharingActive)
    },
    onCallAction: (CallAction) -> Unit = { viewModel.onCallAction(it) },
    onParticipantsPressed: () -> Unit = { },
    onBackPressed: () -> Unit = { },
    onIdle: () -> Unit = { },
) {

    binding.callToolbar.bindView(
        viewModel = viewModel,
        lifecycleOwner = lifecycleOwner,
        onBackPressed = onBackPressed,
        onParticipantsPressed = onParticipantsPressed
    )

    lifecycleOwner.lifecycleScope.launchWhenCreated {
        viewModel.streamCallState.combine(viewModel.isInPictureInPicture) { state, isPictureInPicture ->
            state to isPictureInPicture
        }.collect { (state, isPictureInPicture) ->
            when {
                state is StreamCallState.Incoming && !state.acceptedByMe ->
                    showIncomingScreen { it.bindView(viewModel, lifecycleOwner) }

                state is StreamCallState.Outgoing && !state.acceptedByCallee ->
                    showOutgoingScreen { it.bindView(viewModel, lifecycleOwner) }

                state is StreamCallState.Connected && isPictureInPicture ->
                    showPipLayout { it.bindView(viewModel, lifecycleOwner) }

                state is StreamCallState.Idle -> onIdle()

                else -> showCallContentScreen {
                    it.bindView(
                        viewModel = viewModel,
                        lifecycleOwner = lifecycleOwner,
                        updateCallMediaState = updateCallMediaState,
                        onCallAction = onCallAction,
                    )
                }
            }
        }
    }
}
