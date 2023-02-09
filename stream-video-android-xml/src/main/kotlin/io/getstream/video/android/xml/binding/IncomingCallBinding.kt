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
import io.getstream.video.android.core.model.CallStatus
import io.getstream.video.android.core.viewmodel.CallViewModel
import io.getstream.video.android.xml.widget.incoming.IncomingCallView
import kotlinx.coroutines.flow.collectLatest

/**
 * Binds [IncomingCallView] with [CallViewModel], updating the view's state based on data provided by the ViewModel,
 * and propagating view events to the ViewModel as needed.
 *
 * This function sets listeners on the view and ViewModel. Call this method
 * before setting any additional listeners on these objects yourself.
 */
public fun IncomingCallView.bindView(
    viewModel: CallViewModel,
    lifecycleOwner: LifecycleOwner,
) {
    setCallStatus(CallStatus.Outgoing)

    callActionListener = viewModel::onCallAction

    startJob(lifecycleOwner) {
        viewModel.callMediaState.collectLatest {
            setCameraEnabled(it.isCameraEnabled)
        }
    }

    startJob(lifecycleOwner) {
        viewModel.participants.collectLatest {
            setParticipants(it)
        }
    }
}
