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

package io.getstream.video.android.ui.xml.widget.active

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import io.getstream.video.android.model.CallParticipantState
import io.getstream.video.android.ui.xml.databinding.ViewActiveCallBinding
import io.getstream.video.android.ui.xml.utils.extensions.createStreamThemeWrapper
import io.getstream.video.android.ui.xml.utils.extensions.initToolbar
import io.getstream.video.android.ui.xml.utils.extensions.streamThemeInflater
import io.getstream.video.android.ui.xml.widget.control.CallControlItem
import io.getstream.video.android.ui.xml.widget.control.CallControlsView
import io.getstream.video.android.ui.xml.widget.participant.RendererInitializer

public class ActiveCallView : ConstraintLayout {

    public constructor(context: Context) : this(context, null)
    public constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    public constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0)
    public constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(
        context.createStreamThemeWrapper(),
        attrs,
        defStyleAttr,
        defStyleRes
    ) {
        init(context, attrs)
    }

    private val binding by lazy { ViewActiveCallBinding.inflate(streamThemeInflater, this) }

    private fun init(context: Context, attrs: AttributeSet?) {
        initToolbar(binding.toolbar)
    }

    public fun setOnControlItemClickListener(listener: CallControlsView.OnControlItemClickListener) {
        binding.controlsView.setOnControlItemClickListener(listener)
    }

    public fun setControlItems(items: List<CallControlItem>) {
        binding.controlsView.setItems(items)
    }

    public fun setToolbarTitle(title: String) {
        binding.toolbar.title = title
    }

    public fun setParticipantsRendererInitializer(rendererInitializer: RendererInitializer) {
        binding.participantsView.setRendererInitializer(rendererInitializer)
    }

    public fun setParticipants(participants: List<CallParticipantState>) {
        binding.participantsView.setParticipants(participants)
    }
}
