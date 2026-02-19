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

package io.getstream.video.android.xml.widget.calldetails

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.annotation.Px
import io.getstream.video.android.ui.common.util.getFloatResource
import io.getstream.video.android.xml.R
import io.getstream.video.android.xml.font.TextStyle
import io.getstream.video.android.xml.utils.extensions.dpToPx
import io.getstream.video.android.xml.utils.extensions.getColorCompat
import io.getstream.video.android.xml.utils.extensions.getDimension
import io.getstream.video.android.xml.utils.extensions.use
import io.getstream.video.android.xml.widget.transformer.TransformStyle
import io.getstream.video.android.ui.common.R as RCommon

/**
 * Style for [CallDetailsView].
 * Use this class together with [TransformStyle.callDetailsStyleTransformer] to change [CallDetailsView] styles
 * programmatically.
 *
 * @param callAvatarSize The size of avatars for group calls.
 * @param singleAvatarSize The size of avatar for direct call.
 * @param avatarSpacing The spacing between avatars when in group call.
 * @param participantsTextStyle Text style of the participants info text for a group call.
 * @param singleParticipantTextStyle Text style of participants info for a direct call.
 * @param callStateTextStyle Text style for the call state.
 * @param callStateTextAlpha Alpha value for the call state.
 */
public data class CallDetailsStyle(
    @Px public val callAvatarSize: Int,
    @Px public val singleAvatarSize: Int,
    @Px public val avatarSpacing: Int,
    public val participantsTextStyle: TextStyle,
    public val singleParticipantTextStyle: TextStyle,
    public val callStateTextStyle: TextStyle,
    public val callStateTextAlpha: Float,
) {

    internal companion object {
        private val DEFAULT_AVATAR_SPACING = 20.dpToPx()

        private val DIRECT_CALL_INFO_TEXT_SIZE = RCommon.dimen.stream_video_directCallUserNameTextSize
        private val GROUP_CALL_INFO_TEXT_SIZE = RCommon.dimen.stream_video_groupCallUserNameTextSize

        private val DEFAULT_TEXT_COLOR = RCommon.color.stream_video_text_high_emphasis

        operator fun invoke(context: Context, attrs: AttributeSet?): CallDetailsStyle {
            context.obtainStyledAttributes(
                attrs,
                R.styleable.CallDetailsView,
                R.attr.streamVideoCallDetailsViewStyle,
                R.style.StreamVideo_CallDetails,
            ).use {
                val callAvatarSize = it.getDimensionPixelSize(
                    R.styleable.CallDetailsView_streamVideoCallDetailsAvatarSize,
                    context.getDimension(RCommon.dimen.stream_video_callAvatarSize),
                )

                val singleAvatarSize = it.getDimensionPixelSize(
                    R.styleable.CallDetailsView_streamVideoCallDetailsDirectCallAvatarSize,
                    context.getDimension(RCommon.dimen.stream_video_singleAvatarSize),
                )

                val avatarSpacing = it.getDimensionPixelSize(
                    R.styleable.CallDetailsView_streamVideoCallDetailsAvatarSpacing,
                    DEFAULT_AVATAR_SPACING,
                )

                val participantsTextStyle = TextStyle.Builder(it)
                    .size(
                        R.styleable.CallDetailsView_streamVideoCallDetailsParticipantsInfoTextSize,
                        context.getDimension(GROUP_CALL_INFO_TEXT_SIZE),
                    )
                    .color(
                        R.styleable.CallDetailsView_streamVideoCallDetailsParticipantsInfoTextColor,
                        context.getColorCompat(DEFAULT_TEXT_COLOR),
                    )
                    .font(
                        R.styleable.CallDetailsView_streamVideoCallDetailsParticipantsInfoFontAsset,
                        R.styleable.CallDetailsView_streamVideoCallDetailsParticipantsInfoFont,
                    )
                    .style(
                        R.styleable.CallDetailsView_streamVideoCallDetailsParticipantsInfoTextStyle,
                        Typeface.NORMAL,
                    )
                    .build()

                val singleParticipantTextStyle = TextStyle.Builder(it)
                    .size(
                        R.styleable.CallDetailsView_streamVideoCallDetailsDirectCallParticipantInfoTextSize,
                        context.getDimension(DIRECT_CALL_INFO_TEXT_SIZE),
                    )
                    .color(
                        R.styleable.CallDetailsView_streamVideoCallDetailsParticipantsInfoTextColor,
                        context.getColorCompat(DEFAULT_TEXT_COLOR),
                    )
                    .font(
                        R.styleable.CallDetailsView_streamVideoCallDetailsParticipantsInfoFontAsset,
                        R.styleable.CallDetailsView_streamVideoCallDetailsParticipantsInfoFont,
                    )
                    .style(
                        R.styleable.CallDetailsView_streamVideoCallDetailsParticipantsInfoTextStyle,
                        Typeface.NORMAL,
                    )
                    .build()

                val callStateTextStyle = TextStyle.Builder(it)
                    .size(
                        R.styleable.CallDetailsView_streamVideoCallDetailsCallStateTextSize,
                        context.getDimension(GROUP_CALL_INFO_TEXT_SIZE),
                    )
                    .color(
                        R.styleable.CallDetailsView_streamVideoCallDetailsCallStateTextColor,
                        context.getColorCompat(DEFAULT_TEXT_COLOR),
                    )
                    .font(
                        R.styleable.CallDetailsView_streamVideoCallDetailsCallStateFontAsset,
                        R.styleable.CallDetailsView_streamVideoCallDetailsCallStateFont,
                    )
                    .style(
                        R.styleable.CallDetailsView_streamVideoCallDetailsCallStateTextStyle,
                        Typeface.BOLD,
                    )
                    .build()

                val callStateTextAlpha = it.getFloat(
                    R.styleable.CallDetailsView_streamVideoCallDetailsStateTextAlpha,
                    context.getFloatResource(
                        io.getstream.video.android.ui.common.R.dimen.stream_video_onCallStatusTextAlpha,
                    ),
                )

                return CallDetailsStyle(
                    callAvatarSize = callAvatarSize,
                    singleAvatarSize = singleAvatarSize,
                    avatarSpacing = avatarSpacing,
                    participantsTextStyle = participantsTextStyle,
                    singleParticipantTextStyle = singleParticipantTextStyle,
                    callStateTextStyle = callStateTextStyle,
                    callStateTextAlpha = callStateTextAlpha,
                ).let(TransformStyle.callDetailsStyleTransformer::transform)
            }
        }
    }
}
