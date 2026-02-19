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

package io.getstream.video.android.xml.binding

import androidx.lifecycle.LifecycleOwner
import io.getstream.video.android.xml.state.CallDeviceState
import io.getstream.video.android.xml.viewmodel.CallViewModel
import io.getstream.video.android.xml.widget.callcontainer.CallContainerView
import io.getstream.video.android.xml.widget.control.CallControlItem

/**
 * Binds [CallContainerView] with [CallViewModel], updating the view's state based on data provided by the ViewModel,
 * and propagating view events to the ViewModel as needed.
 *
 * This function sets listeners on the view and ViewModel. Call this method
 * before setting any additional listeners on these objects yourself.
 *
 * @param viewModel [CallViewModel] for observing data and running actions.
 * @param lifecycleOwner The lifecycle owner, root component containing [CallContainerView]. Usually an Activity or
 * Fragment.
 * @param fetchCallMediaState Handler used to fetch the new state of the call controls buttons when the media state
 * changes.
 * @param onCallAction Handler that listens to interactions with call media controls.
 * @param onBackPressed Handler that notifies when the back button has been pressed.
 * @param onIdle Handler that notifies when the call has entered the idle state, notifies about end of the call.
 */
public fun CallContainerView.bindView(
    viewModel: CallViewModel,
    lifecycleOwner: LifecycleOwner,
    fetchCallMediaState: (
        CallDeviceState,
        Boolean,
    ) -> List<CallControlItem> = { mediaState, isScreenSharingActive ->
        defaultControlList(mediaState, isScreenSharingActive)
    },
    // onCallAction: (CallAction) -> Unit = viewModel::onCallAction,
    onParticipantsPressed: () -> Unit = { },
    onBackPressed: () -> Unit = { },
    onIdle: () -> Unit = { },
) {
//    lifecycleOwner.lifecycleScope.launchWhenCreated {
//        viewModel.streamCallState.combine(viewModel.isInPictureInPicture) { state, isPictureInPicture ->
//            state to isPictureInPicture
//        }.collect { (state, isPictureInPicture) ->
//            when {
//                state is StreamCallState.Incoming && !state.acceptedByMe ->
//                    showIncomingScreen {
//                        when (it) {
//                            is IncomingCallView -> it.bindView(
//                                viewModel = viewModel,
//                                lifecycleOwner = lifecycleOwner,
//                                onCallAction = onCallAction
//                            )
//                            is CallAppBarView -> it.bindView(
//                                viewModel = viewModel,
//                                lifecycleOwner = lifecycleOwner,
//                                onBackPressed = onBackPressed,
//                                onParticipantsPressed = onParticipantsPressed
//                            )
//                        }
//                    }
//
//                state is StreamCallState.Outgoing && !state.acceptedByCallee ->
//                    showOutgoingScreen {
//                        when (it) {
//                            is OutgoingCallView -> it.bindView(
//                                viewModel = viewModel,
//                                lifecycleOwner = lifecycleOwner,
//                                onCallAction = onCallAction
//                            )
//                            is CallAppBarView -> it.bindView(
//                                viewModel = viewModel,
//                                lifecycleOwner = lifecycleOwner,
//                                onBackPressed = onBackPressed,
//                                onParticipantsPressed = onParticipantsPressed
//                            )
//                        }
//                    }
//
//                state is StreamCallState.Connected && isPictureInPicture ->
//                    showPipLayout { it.bindView(viewModel, lifecycleOwner) }
//
//                state is StreamCallState.Idle -> onIdle()
//
//                else -> showCallContent {
//                    when (it) {
//                        is CallView -> it.bindView(
//                            viewModel = viewModel,
//                            lifecycleOwner = lifecycleOwner,
//                            fetchCallMediaState = fetchCallMediaState,
//                            onCallAction = onCallAction
//                        )
//                        is CallAppBarView -> it.bindView(
//                            viewModel = viewModel,
//                            lifecycleOwner = lifecycleOwner,
//                            onBackPressed = onBackPressed,
//                            onParticipantsPressed = onParticipantsPressed
//                        )
//                    }
//                }
//            }
//        }
//    }
}
