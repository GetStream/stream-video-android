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

package io.getstream.video.android.compose.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import io.getstream.video.android.ui.common.R

/**
 * Contains all the colors in our palette. Each color is used for various things an can be changed to
 * customize the app design style.
 * @param textHighEmphasis Used for main text and active icon status.
 * @param textLowEmphasis Used for secondary text, default icon state, deleted messages text and datestamp background.
 * @param disabled Used for disabled icons and empty states.
 * @param borders Used for borders, the background of self messages, selected items, pressed state, button dividers.
 * @param inputBackground Used for the input background, deleted messages, section headings.
 * @param appBackground Used for the default app background and channel list item background.
 * @param barsBackground Used for button text, top and bottom bar background and other user messages.
 * @param linkBackground Used for the message link card background.
 * @param overlay Used for general overlays and background when opening modals.
 * @param overlayDark Used for the date separator background color.
 * @param primaryAccent Used for selected icon state, call to actions, white buttons text and links.
 * @param errorAccent Used for error text labels, notification badges and disruptive action text and icons.
 * @param infoAccent Used for the online status.
 * @param highlight Used for message highlights.
 */
@Immutable
public data class StreamColors(
    public val textHighEmphasis: Color,
    public val textLowEmphasis: Color,
    public val disabled: Color,
    public val borders: Color,
    public val inputBackground: Color,
    public val appBackground: Color,
    public val barsBackground: Color,
    public val linkBackground: Color,
    public val overlay: Color,
    public val overlayDark: Color,
    public val primaryAccent: Color,
    public val errorAccent: Color,
    public val infoAccent: Color,
    public val highlight: Color,
    public val screenSharingBackground: Color,
    public val screenSharingTooltipBackground: Color,
    public val screenSharingTooltipContent: Color,
    public val avatarInitials: Color,
    public val soundLevels: Color,
    public val connectionQualityBackground: Color,
    public val connectionQualityBar: Color,
    public val connectionQualityBarFilled: Color,
    public val participantLabelBackground: Color,
    public val infoMenuOverlayColor: Color,
    public val callFocusedBorder: Color,
    public val callGradientStart: Color,
    public val callGradientEnd: Color,
    public val callDescription: Color,
    public val callActionIconEnabledBackground: Color,
    public val callActionIconDisabledBackground: Color,
    public val callActionIconEnabled: Color,
    public val callActionIconDisabled: Color,
    public val callLobbyBackground: Color,
    public val audioLeaveButton: Color
) {

    public companion object {
        /**
         * Provides the default colors for the light mode of the app.
         *
         * @return A [StreamColors] instance holding our color palette.
         */
        @Composable
        public fun defaultColors(): StreamColors = StreamColors(
            textHighEmphasis = colorResource(R.color.stream_video_text_high_emphasis),
            textLowEmphasis = colorResource(R.color.stream_video_text_low_emphasis),
            disabled = colorResource(R.color.stream_video_disabled),
            borders = colorResource(R.color.stream_video_borders),
            inputBackground = colorResource(R.color.stream_video_input_background),
            appBackground = colorResource(R.color.stream_video_app_background),
            barsBackground = colorResource(R.color.stream_video_bars_background),
            linkBackground = colorResource(R.color.stream_video_link_background),
            overlay = colorResource(R.color.stream_video_overlay_regular),
            overlayDark = colorResource(R.color.stream_video_overlay_dark),
            primaryAccent = colorResource(R.color.stream_video_primary_accent),
            errorAccent = colorResource(R.color.stream_video_error_accent),
            infoAccent = colorResource(R.color.stream_video_info_accent),
            highlight = colorResource(R.color.stream_video_highlight),
            avatarInitials = colorResource(id = R.color.stream_video_text_avatar_initials),
            screenSharingBackground = colorResource(R.color.stream_video_app_background),
            screenSharingTooltipBackground = colorResource(R.color.stream_video_screen_sharing_tooltip_background),
            screenSharingTooltipContent = colorResource(id = R.color.stream_video_screen_sharing_tooltip_content),
            soundLevels = colorResource(id = R.color.stream_video_primary_accent),
            connectionQualityBackground = colorResource(id = R.color.stream_video_connection_quality_background),
            connectionQualityBarFilled = colorResource(id = R.color.stream_video_primary_accent),
            connectionQualityBar = colorResource(id = R.color.stream_video_connection_quality_bar_background),
            participantLabelBackground = colorResource(id = R.color.stream_video_participant_label_background),
            infoMenuOverlayColor = Color.LightGray.copy(alpha = 0.7f),
            callFocusedBorder = colorResource(id = R.color.stream_video_focused_border_color),
            callGradientStart = colorResource(id = R.color.stream_video_call_gradient_start),
            callGradientEnd = colorResource(id = R.color.stream_video_call_gradient_end),
            callDescription = colorResource(id = R.color.stream_video_call_description),
            callActionIconEnabled = colorResource(id = R.color.stream_video_action_icon_enabled),
            callActionIconDisabled = colorResource(id = R.color.stream_video_action_icon_disabled),
            callActionIconEnabledBackground = colorResource(id = R.color.stream_video_action_icon_enabled_background),
            callActionIconDisabledBackground = colorResource(id = R.color.stream_video_action_icon_disabled_background),
            callLobbyBackground = colorResource(id = R.color.stream_video_lobby_background),
            audioLeaveButton = colorResource(id = R.color.stream_video_audio_leave),
        )

        /**
         * Provides the default colors for the dark mode of the app.
         *
         * @return A [StreamColors] instance holding our color palette.
         */
        @Composable
        public fun defaultDarkColors(): StreamColors = StreamColors(
            textHighEmphasis = colorResource(R.color.stream_video_text_high_emphasis_dark),
            textLowEmphasis = colorResource(R.color.stream_video_text_low_emphasis_dark),
            disabled = colorResource(R.color.stream_video_disabled_dark),
            borders = colorResource(R.color.stream_video_borders_dark),
            inputBackground = colorResource(R.color.stream_video_input_background_dark),
            appBackground = colorResource(R.color.stream_video_app_background_dark),
            barsBackground = colorResource(R.color.stream_video_bars_background_dark),
            linkBackground = colorResource(R.color.stream_video_link_background_dark),
            overlay = colorResource(R.color.stream_video_overlay_regular_dark),
            overlayDark = colorResource(R.color.stream_video_overlay_dark_dark),
            primaryAccent = colorResource(R.color.stream_video_primary_accent_dark),
            errorAccent = colorResource(R.color.stream_video_error_accent_dark),
            infoAccent = colorResource(R.color.stream_video_info_accent_dark),
            highlight = colorResource(R.color.stream_video_highlight_dark),
            screenSharingBackground = colorResource(R.color.stream_video_app_background_dark),
            screenSharingTooltipBackground = colorResource(R.color.stream_video_screen_sharing_tooltip_background),
            screenSharingTooltipContent = colorResource(id = R.color.stream_video_screen_sharing_tooltip_content),
            avatarInitials = colorResource(id = R.color.stream_video_text_avatar_initials),
            soundLevels = colorResource(id = R.color.stream_video_primary_accent),
            connectionQualityBackground = colorResource(id = R.color.stream_video_connection_quality_background),
            connectionQualityBarFilled = colorResource(id = R.color.stream_video_primary_accent),
            connectionQualityBar = colorResource(id = R.color.stream_video_connection_quality_bar_background),
            participantLabelBackground = colorResource(id = R.color.stream_video_participant_label_background),
            infoMenuOverlayColor = Color.LightGray.copy(alpha = 0.7f),
            callFocusedBorder = colorResource(id = R.color.stream_video_focused_border_color),
            callGradientStart = colorResource(id = R.color.stream_video_call_gradient_start),
            callGradientEnd = colorResource(id = R.color.stream_video_call_gradient_end),
            callDescription = colorResource(id = R.color.stream_video_call_description_dark),
            callActionIconEnabled = colorResource(id = R.color.stream_video_action_icon_enabled_dark),
            callActionIconDisabled = colorResource(id = R.color.stream_video_action_icon_disabled_dark),
            callActionIconEnabledBackground = colorResource(id = R.color.stream_video_action_icon_enabled_background_dark),
            callActionIconDisabledBackground = colorResource(id = R.color.stream_video_action_icon_disabled_background_dark),
            callLobbyBackground = colorResource(id = R.color.stream_video_lobby_background_dark),
            audioLeaveButton = colorResource(id = R.color.stream_video_audio_leave_dark),
        )
    }
}
