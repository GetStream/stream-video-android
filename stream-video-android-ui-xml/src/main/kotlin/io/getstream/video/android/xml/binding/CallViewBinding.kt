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
import io.getstream.video.android.xml.utils.extensions.getFirstViewInstance
import io.getstream.video.android.xml.viewmodel.CallViewModel
import io.getstream.video.android.xml.widget.call.CallView
import io.getstream.video.android.xml.widget.control.CallControlItem
import io.getstream.video.android.xml.widget.control.CallControlsView

/**
 * Binds [CallView] with [CallViewModel], updating the view's state based on data provided by the ViewModel,
 * and propagating view events to the ViewModel as needed.
 *
 * This function sets listeners on the view and ViewModel. Call this method
 * before setting any additional listeners on these objects yourself.
 *
 * @param viewModel [CallViewModel] for observing data and running actions.
 * @param lifecycleOwner The lifecycle owner, root component containing [CallView]. Usually an Activity or
 * Fragment.
 * @param fetchCallMediaState Handler used to fetch the new state of the call controls buttons when the media state
 * changes.
 * @param onCallAction Handler that listens to interactions with call media controls.
 */
public fun CallView.bindView(
    viewModel: CallViewModel,
    lifecycleOwner: LifecycleOwner,
    fetchCallMediaState: (
        CallDeviceState,
        Boolean,
    ) -> List<CallControlItem> = { mediaState, isScreenSharingActive ->
        defaultControlList(mediaState, isScreenSharingActive)
    },
    // onCallAction: (CallAction) -> Unit = viewModel::onCallAction,
) {
    getFirstViewInstance<CallControlsView>()?.bindView(
        viewModel = viewModel,
        lifecycleOwner = lifecycleOwner,
        fetchCallMediaState = fetchCallMediaState,
        // onCallAction = onCallAction,
    )

    startJob(lifecycleOwner) {
//        viewModel.participantList.combine(viewModel.screenSharingSessions) { participants, screenSharingSessions ->
//            participants to screenSharingSessions.firstOrNull()
//        }.collect { (participants, screenSharingSession) ->
//            if (screenSharingSession != null) {
//                setScreenSharingContent { view ->
//                    when (view) {
//                        is ScreenShareView -> view.bindView(viewModel, lifecycleOwner)
//                        is CallParticipantsListView -> view.bindView(viewModel, lifecycleOwner)
//                    }
//                }
//                updatePresenterText(screenSharingSession.participant.name.ifEmpty { screenSharingSession.participant.id })
//                setFloatingParticipant(null)
//            } else {
//                setRegularContent { it.bindView(viewModel, lifecycleOwner) }
//                val localParticipant = if (participants.size == 1 || participants.size == 4) {
//                    null
//                } else {
//                    participants.firstOrNull { it.isLocal }
//                }
//                setFloatingParticipant(localParticipant) { it.bindView(viewModel, lifecycleOwner) }
//            }
//        }
    }
}
