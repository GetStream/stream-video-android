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

package io.getstream.video.android.xml.widget.active

import android.content.Context
import android.util.AttributeSet
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.model.CallParticipantState
import io.getstream.video.android.ui.common.R
import io.getstream.video.android.xml.databinding.ViewActiveCallBinding
import io.getstream.video.android.xml.utils.extensions.getColorCompat
import io.getstream.video.android.xml.utils.extensions.streamThemeInflater
import io.getstream.video.android.xml.widget.callcontent.CallContent
import io.getstream.video.android.xml.widget.control.CallControlItem
import io.getstream.video.android.xml.widget.participant.RendererInitializer

/**
 * Represents the UI in an Active call that shows participants and their video, as well as some
 * extra UI features to control the call settings, browse participants and more.
 */
public class ActiveCallView : CallContent {

    private val binding by lazy { ViewActiveCallBinding.inflate(streamThemeInflater, this) }

    /**
     * Handler that notifies when a Call Action has been performed.
     */
    public var callActionListener: (CallAction) -> Unit = {}

    public constructor(context: Context) : this(context, null)
    public constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    public constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        setBackgroundColor(context.getColorCompat(R.color.stream_app_background))
        binding.controlsView.callControlItemClickListener = { callActionListener(it) }
    }

    /**
     * Sets the Call Controls with which the user can interact.
     *
     * @param items The CallActions wrapped in [CallControlItem]s which we wish to expose to the users.
     */
    public fun setControlItems(items: List<CallControlItem>) {
        binding.controlsView.setItems(items)
    }

    /**
     * Updates the state of Call Controls previously set using [setControlItems].
     *
     * @param items Call Controls whose state we wish to update.
     */
    public fun updateControlItems(items: List<CallControlItem>) {
        binding.controlsView.updateItems(items)
    }

    /**
     * Sets the [RendererInitializer] handler used to initialize the renderer for each users video and to notify when
     * the video has been rendered.
     *
     * @param rendererInitializer [RendererInitializer] handler used to initialize the renderer and notify when the
     * video has been rendered.
     */
    public fun setParticipantsRendererInitializer(rendererInitializer: RendererInitializer) {
        binding.participantsView.setRendererInitializer(rendererInitializer)
    }

    /**
     * Updates the participants that are inside the call.
     *
     * @param participants The list of participants that are inside the call.
     */
    public fun updateParticipants(participants: List<CallParticipantState>) {
        binding.participantsView.updateParticipants(participants)
    }

    /**
     * Updates the current primary speaker and shows a border around the primary speaker.
     *
     * @param participant The call participant marked as a primary speaker.
     */
    public fun updatePrimarySpeaker(participant: CallParticipantState?) {
        binding.participantsView.updatePrimarySpeaker(participant)
    }
}
