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

package io.getstream.video.android.xml.widget.call

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.Px
import io.getstream.video.android.xml.R
import io.getstream.video.android.xml.font.TextStyle
import io.getstream.video.android.xml.utils.extensions.getColorCompat
import io.getstream.video.android.xml.utils.extensions.getDimension
import io.getstream.video.android.xml.utils.extensions.getDrawableCompat
import io.getstream.video.android.xml.utils.extensions.getResourceId
import io.getstream.video.android.xml.utils.extensions.use
import io.getstream.video.android.xml.widget.control.CallControlsView
import io.getstream.video.android.xml.widget.participant.CallParticipantView
import io.getstream.video.android.xml.widget.participant.FloatingParticipantView
import io.getstream.video.android.xml.widget.participant.internal.CallParticipantsGridView
import io.getstream.video.android.xml.widget.participant.internal.CallParticipantsListView
import io.getstream.video.android.xml.widget.screenshare.ScreenShareView
import io.getstream.video.android.xml.widget.transformer.TransformStyle
import io.getstream.video.android.ui.common.R as RCommon

/**
 * Style for [CallView].
 * Use this class together with [TransformStyle.callViewStyleTransformer] to change [CallView]
 * styles programmatically.
 *
 * @param gridCallParticipantStyle The id of the custom style for [CallParticipantView] to be applied for each call
 * participant in the [CallParticipantsGridView].
 * @param listCallParticipantStyle The id of the custom style for [CallParticipantView] to be applied for each call
 * participant in the [CallParticipantsListView].
 * @param localParticipantHeight The height of the [FloatingParticipantView] used fot the local user.
 * @param localParticipantWidth The width of the [FloatingParticipantView] used fot the local user.
 * @param localParticipantPadding The padding between the [FloatingParticipantView] used fot the local user and the
 * borders of [CallView].
 * @param localParticipantRadius The corner radius of the [FloatingParticipantView] used fot the local user.
 * @param participantListHeight The height of the participants list when there is a screen share session active.
 * @param participantListPadding The padding applied to the participants list.
 * @param participantListItemMargin The margin between two adjacent [CallParticipantView]s inside
 * [CallParticipantsListView].
 * @param participantListItemWidth The width of a [CallParticipantView] inside [CallParticipantsListView].
 * @param screenShareMargin Size of the margin between [ScreenShareView] and [CallParticipantsListView].
 * @param presenterTextStyle Active screen share presenter text style.
 * @param presenterTextPadding Padding around the presenter text.
 * @param presenterTextMargin Margin between presenter text and screen share content.
 * @param preConnectionImage Placeholder image while the user is connecting to a call.
 * @param callControlsHeight The height of the [CallControlsView] in portrait mode.
 * @param callControlsWidthLandscape The width of the [CallControlsView] in landscape mode.
 * @param shouldShowGridUsersAsListLandscape True when we want [CallParticipantsGridView] to show the user in the list,
 * or false when we want them arranged in a grid. [true] by default.
 */
public data class CallViewStyle(
    public val gridCallParticipantStyle: Int,
    public val listCallParticipantStyle: Int,
    @Px public val localParticipantHeight: Float,
    @Px public val localParticipantWidth: Float,
    @Px public val localParticipantPadding: Float,
    @Px public val localParticipantRadius: Float,
    @Px public val participantListHeight: Int,
    @Px public val participantListPadding: Int,
    @Px public val participantListItemMargin: Int,
    @Px public val participantListItemWidth: Int,
    @Px public val screenShareMargin: Int,
    public val presenterTextStyle: TextStyle,
    @Px public val presenterTextPadding: Int,
    @Px public val presenterTextMargin: Int,
    public val preConnectionImage: Drawable,
    @Px public val callControlsHeight: Int,
    @Px public val callControlsWidthLandscape: Int,
    public val shouldShowGridUsersAsListLandscape: Boolean,
) {

    internal companion object {
        operator fun invoke(context: Context, attrs: AttributeSet?): CallViewStyle {
            context.obtainStyledAttributes(
                attrs,
                R.styleable.CallView,
                R.attr.streamVideoCallViewStyle,
                R.style.StreamVideo_CallView,
            ).use {
                val gridCallParticipantStyle = it.getResourceId(
                    R.styleable.CallView_streamVideoCallViewGridParticipantStyle,
                    context.getResourceId(
                        R.style.StreamVideoTheme,
                        R.attr.streamVideoCallParticipantViewStyle,
                    ),
                )

                val listCallParticipantStyle = it.getResourceId(
                    R.styleable.CallView_streamVideoCallViewListParticipantStyle,
                    context.getResourceId(
                        R.style.StreamVideoTheme,
                        R.attr.streamVideoCallParticipantViewStyle,
                    ),
                )

                val localParticipantHeight = it.getDimension(
                    R.styleable.CallView_streamVideoCallViewLocalParticipantHeight,
                    context.getDimension(RCommon.dimen.stream_video_floatingVideoHeight).toFloat(),
                )

                val localParticipantWidth = it.getDimension(
                    R.styleable.CallView_streamVideoCallViewLocalParticipantWidth,
                    context.getDimension(RCommon.dimen.stream_video_floatingVideoWidth).toFloat(),
                )

                val localParticipantPadding = it.getDimension(
                    R.styleable.CallView_streamVideoCallViewLocalParticipantPadding,
                    context.getDimension(RCommon.dimen.stream_video_floatingVideoPadding).toFloat(),
                )

                val localParticipantRadius = it.getDimension(
                    R.styleable.CallView_streamVideoCallViewLocalParticipantRadius,
                    context.getDimension(RCommon.dimen.stream_video_floatingVideoRadius).toFloat(),
                )

                val participantListHeight = it.getDimension(
                    R.styleable.CallView_streamVideoCallViewListHeight,
                    context.getDimension(
                        RCommon.dimen.stream_video_screenShareParticipantsListHeight,
                    ).toFloat(),
                ).toInt()

                val participantListPadding = it.getDimension(
                    R.styleable.CallView_streamVideoCallViewListPadding,
                    context.getDimension(
                        RCommon.dimen.stream_video_screenShareParticipantsListPadding,
                    ).toFloat(),
                ).toInt()

                val participantListItemMargin = it.getDimension(
                    R.styleable.CallView_streamVideoCallViewListItemMargin,
                    context.getDimension(
                        RCommon.dimen.stream_video_screenShareParticipantsListItemMargin,
                    ).toFloat(),
                ).toInt()

                val participantListItemWidth = it.getDimension(
                    R.styleable.CallView_streamVideoCallViewListItemWidth,
                    context.getDimension(
                        RCommon.dimen.stream_video_screenShareParticipantItemSize,
                    ).toFloat(),
                ).toInt()

                val screenShareMargin = it.getDimension(
                    R.styleable.CallView_streamVideoCallViewScreenShareListMargin,
                    context.getDimension(
                        RCommon.dimen.stream_video_screenShareParticipantsRadius,
                    ).toFloat(),
                ).toInt()

                val presenterTextStyle = TextStyle.Builder(it)
                    .size(
                        R.styleable.CallView_streamVideoCallViewPresenterInfoTextSize,
                        context.getDimension(RCommon.dimen.stream_video_title3TextSize),
                    )
                    .color(
                        R.styleable.CallView_streamVideoCallViewPresenterInfoTextColor,
                        context.getColorCompat(RCommon.color.stream_video_text_high_emphasis),
                    )
                    .font(
                        R.styleable.CallView_streamVideoCallViewPresenterInfoFontAsset,
                        R.styleable.CallView_streamVideoCallViewPresenterInfoFont,
                    )
                    .style(
                        R.styleable.CallView_streamVideoCallViewPresenterInfoTextStyle,
                        Typeface.BOLD,
                    )
                    .build()

                val presenterTextMargin = it.getDimension(
                    R.styleable.CallView_streamVideoCallViewPresenterTextMargin,
                    context.getDimension(
                        RCommon.dimen.stream_video_screenSharePresenterTooltipMargin,
                    ).toFloat(),
                ).toInt()

                val presenterTextPadding = it.getDimension(
                    R.styleable.CallView_streamVideoCallViewPresenterTextPadding,
                    context.getDimension(
                        RCommon.dimen.stream_video_screenSharePresenterPadding,
                    ).toFloat(),
                ).toInt()

                val preConnectionImage = it.getDrawable(
                    R.styleable.CallView_streamVideoCallViewPreConnectionImage,
                ) ?: context.getDrawableCompat(RCommon.drawable.stream_video_ic_call)!!

                val callControlsHeight = it.getDimension(
                    R.styleable.CallView_streamVideoCallViewCallControlsHeight,
                    context.getDimension(RCommon.dimen.stream_video_controlActionsHeight).toFloat(),
                ).toInt()

                val callControlsWidthLandscape = it.getDimension(
                    R.styleable.CallView_streamVideoCallViewCallControlsWidthLandscape,
                    context.getDimension(
                        RCommon.dimen.stream_video_landscapeControlActionsWidth,
                    ).toFloat(),
                ).toInt()

                val shouldShowGridUsersAsListLandscape = it.getBoolean(
                    R.styleable.CallView_streamVideoCallViewShouldShowGridUsersAsListLandscape,
                    true,
                )

                return CallViewStyle(
                    gridCallParticipantStyle = gridCallParticipantStyle,
                    listCallParticipantStyle = listCallParticipantStyle,
                    localParticipantHeight = localParticipantHeight,
                    localParticipantWidth = localParticipantWidth,
                    localParticipantPadding = localParticipantPadding,
                    localParticipantRadius = localParticipantRadius,
                    participantListHeight = participantListHeight,
                    participantListPadding = participantListPadding,
                    participantListItemMargin = participantListItemMargin,
                    participantListItemWidth = participantListItemWidth,
                    screenShareMargin = screenShareMargin,
                    presenterTextStyle = presenterTextStyle,
                    presenterTextMargin = presenterTextMargin,
                    presenterTextPadding = presenterTextPadding,
                    preConnectionImage = preConnectionImage,
                    callControlsHeight = callControlsHeight,
                    callControlsWidthLandscape = callControlsWidthLandscape,
                    shouldShowGridUsersAsListLandscape = shouldShowGridUsersAsListLandscape,
                ).let(TransformStyle.callViewStyleTransformer::transform)
            }
        }
    }
}
