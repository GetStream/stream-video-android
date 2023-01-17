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

package io.getstream.video.android.ui.xml.widget.participant

import android.content.Context
import android.util.AttributeSet
import androidx.cardview.widget.CardView
import io.getstream.video.android.model.CallParticipantState
import io.getstream.video.android.ui.xml.databinding.ViewFloatingParticipantBinding
import io.getstream.video.android.ui.xml.utils.extensions.createStreamThemeWrapper
import io.getstream.video.android.ui.xml.utils.extensions.streamThemeInflater

public class FloatingParticipantView : CardView {

    private val binding = ViewFloatingParticipantBinding.inflate(streamThemeInflater, this)

    public constructor(context: Context) : this(context, null)
    public constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    public constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context.createStreamThemeWrapper(),
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        isClickable = true
        isFocusable = true
    }

    public fun setRendererInitializer(rendererInitializer: RendererInitializer) {
        binding.localParticipant.setRendererInitializer(rendererInitializer)
    }

    public fun setParticipant(participant: CallParticipantState) {
        binding.localParticipant.setParticipant(participant)
    }
}
