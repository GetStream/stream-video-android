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

package io.getstream.video.android.xml.widget.participant

import android.content.Context
import android.util.AttributeSet
import androidx.cardview.widget.CardView
import androidx.core.view.children
import io.getstream.video.android.model.CallParticipantState
import io.getstream.video.android.xml.R
import io.getstream.video.android.xml.utils.extensions.createStreamThemeWrapper

/**
 * View to ve shown when inside a call and the app enter picture in picture mode. Will show the primary speaker.
 */
public class PictureInPictureView : CardView {

    private lateinit var style: PictureInPictureStyle

    /**
     * Handler to initialise the renderer. If the participant view was created before the renderer has been initialised
     * will take care of init.
     */
    public var rendererInitializer: RendererInitializer? = null
        set(value) {
            field = value
            field?.let { getCallParticipantView()?.setRendererInitializer(it) }
        }

    /**
     * The participant we wish to show. If the view was created before the participant has been set will take care of
     * setting the correct participant.
     */
    public var participant: CallParticipantState? = null
        set(value) {
            field = value
            field?.let { getCallParticipantView()?.setParticipant(it) }
        }

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
        style = PictureInPictureStyle(context, attrs)

        showCallParticipantView()
    }

    /**
     * Populates the PiP ui with the [CallParticipantView] to preview the primary speaker.
     */
    private fun showCallParticipantView() {
        val callParticipantView = CallParticipantView(
            context = context,
            attrs = null,
            defStyleAttr = R.attr.streamPictureInPictureCallParticipantViewStyle,
            defStyleRes = style.callParticipantStyle
        )
        callParticipantView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        rendererInitializer?.let { callParticipantView.setRendererInitializer(it) }
        participant?.let { callParticipantView.setParticipant(it) }
        addView(callParticipantView)
    }

    /**
     * Retrieves the [CallParticipantView] from the content holder if it is currently being used.
     *
     * @return The [CallParticipantView] if one is inflated in the layout.
     */
    private fun getCallParticipantView(): CallParticipantView? {
        return children.firstOrNull { it is CallParticipantView } as? CallParticipantView
    }
}
