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

package io.getstream.video.android.compose.ui.components.call

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.compose.R
import io.getstream.video.android.compose.theme.VideoTheme

/**
 * Represents the default AppBar that's shown in calls. Exposes handlers for the two default slot
 * component implementations (leading and trailing).
 *
 * Exposes slots required to customize the look and feel.
 *
 * @param modifier Modifier for styling.
 * @param onBackButtonClicked Handler when the user taps on the default leading content slot.
 * @param onParticipantsClicked Handler when the user taps on the default trailing content slot.
 * @param leadingContent The leading content, by default [DefaultCallAppBarLeadingContent].
 * @param centerContent The center content, by default [DefaultCallAppBarCenterContent].
 * @param trailingContent The trailing content, by default [DefaultCallAppBarTrailingContent].
 * */
@Composable
public fun CallAppBar(
    modifier: Modifier = Modifier,
    onBackButtonClicked: () -> Unit = {},
    onParticipantsClicked: () -> Unit = {},
    title: String = stringResource(id = R.string.default_app_bar_title),
    leadingContent: @Composable () -> Unit = {
        DefaultCallAppBarLeadingContent(onBackButtonClicked)
    },
    centerContent: @Composable () -> Unit = {
        DefaultCallAppBarCenterContent(title)
    },
    trailingContent: @Composable () -> Unit = {
        DefaultCallAppBarTrailingContent(
            onParticipantsClicked
        )
    }
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(VideoTheme.dimens.topAppbarHeightSize)
            .padding(VideoTheme.dimens.callAppBarPadding)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leadingContent()

        centerContent()

        trailingContent()
    }
}

/**
 * Default leading slot, representing the back button.
 */
@Composable
internal fun DefaultCallAppBarLeadingContent(onBackButtonClicked: () -> Unit) {
    Icon(
        modifier = Modifier
            .padding(
                start = VideoTheme.dimens.callAppBarLeadingContentSpacingStart,
                end = VideoTheme.dimens.callAppBarLeadingContentSpacingEnd
            )
            .clickable { onBackButtonClicked() },
        painter = VideoTheme.icons.arrowBack,
        contentDescription = stringResource(id = R.string.back_button_content_description),
        tint = VideoTheme.colors.textHighEmphasis
    )
}

/**
 * Default center slot, representing the call title.
 */
@Composable
internal fun DefaultCallAppBarCenterContent(title: String) {
    Text(
        modifier = Modifier.padding(
            start = VideoTheme.dimens.callAppBarCenterContentSpacingStart,
            end = VideoTheme.dimens.callAppBarCenterContentSpacingEnd
        ),
        text = title,
        fontSize = VideoTheme.dimens.topAppbarTextSize,
        color = VideoTheme.colors.textHighEmphasis,
        textAlign = TextAlign.Center,
    )
}

/**
 * Default trailing content slot, representing an icon to show the call participants menu.
 */
@Composable
internal fun DefaultCallAppBarTrailingContent(onParticipantsClicked: () -> Unit) {
    Icon(
        modifier = Modifier
            .padding(
                start = VideoTheme.dimens.callAppBarTrailingContentSpacingStart,
                end = VideoTheme.dimens.callAppBarTrailingContentSpacingEnd
            )
            .clickable { onParticipantsClicked() },
        painter = VideoTheme.icons.participants,
        contentDescription = stringResource(id = R.string.call_participants_menu_content_description),
        tint = VideoTheme.colors.textHighEmphasis
    )
}

@Preview
@Composable
private fun CallTopAppbarPreview() {
    VideoTheme {
        CallAppBar()
    }
}
