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

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.compose.R
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.ShowCallInfo

/**
 * Represents the default AppBar that's shown in calls. Exposes handlers for the two default slot
 * component implementations (leading and trailing).
 *
 * Exposes slots required to customize the look and feel.
 *
 * @param modifier Modifier for styling.
 * @param onBackPressed Handler when the user taps on the default leading content slot.
 * @param leadingContent The leading content, by default [DefaultCallAppBarLeadingContent].
 * @param centerContent The center content, by default [DefaultCallAppBarCenterContent].
 * @param trailingContent The trailing content, by default [DefaultCallAppBarTrailingContent].
 * */
@Composable
public fun CallAppBar(
    modifier: Modifier = Modifier,
    isShowingOverlays: Boolean = false,
    onBackPressed: () -> Unit = {},
    onCallAction: (CallAction) -> Unit = {},
    title: String = stringResource(id = R.string.default_app_bar_title),
    leadingContent: @Composable () -> Unit = {
        DefaultCallAppBarLeadingContent(isShowingOverlays, onBackPressed)
    },
    centerContent: @Composable (RowScope.() -> Unit) = {
        DefaultCallAppBarCenterContent(title)
    },
    trailingContent: @Composable () -> Unit = {
        DefaultCallAppBarTrailingContent(
            onCallAction
        )
    }
) {
    val orientation = LocalConfiguration.current.orientation
    val height = if (orientation == ORIENTATION_LANDSCAPE) {
        VideoTheme.dimens.landscapeTopAppBarHeight
    } else {
        VideoTheme.dimens.topAppbarHeight
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(color = VideoTheme.colors.barsBackground)
            .padding(VideoTheme.dimens.callAppBarPadding),
        verticalAlignment = Alignment.CenterVertically,
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
internal fun DefaultCallAppBarLeadingContent(
    isShowingOverlays: Boolean,
    onBackButtonClicked: () -> Unit
) {
    IconButton(
        enabled = !isShowingOverlays,
        onClick = onBackButtonClicked,
        modifier = Modifier.padding(
            start = VideoTheme.dimens.callAppBarLeadingContentSpacingStart,
            end = VideoTheme.dimens.callAppBarLeadingContentSpacingEnd
        )
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_arrow_back),
            contentDescription = stringResource(id = R.string.back_button_content_description),
            tint = VideoTheme.colors.textHighEmphasis
        )
    }
}

/**
 * Default center slot, representing the call title.
 */
@Composable
internal fun RowScope.DefaultCallAppBarCenterContent(title: String) {
    Text(
        modifier = Modifier
            .weight(1f)
            .padding(
                start = VideoTheme.dimens.callAppBarCenterContentSpacingStart,
                end = VideoTheme.dimens.callAppBarCenterContentSpacingEnd
            ),
        text = title,
        fontSize = VideoTheme.dimens.topAppbarTextSize,
        color = VideoTheme.colors.textHighEmphasis,
        textAlign = TextAlign.Start,
    )
}

/**
 * Default trailing content slot, representing an icon to show the call participants menu.
 */
@Composable
internal fun DefaultCallAppBarTrailingContent(onCallAction: (CallAction) -> Unit) {
    IconButton(
        onClick = { onCallAction(ShowCallInfo) },
        modifier = Modifier.padding(
            start = VideoTheme.dimens.callAppBarLeadingContentSpacingStart,
            end = VideoTheme.dimens.callAppBarLeadingContentSpacingEnd
        )
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_participants),
            contentDescription = stringResource(id = R.string.call_participants_menu_content_description),
            tint = VideoTheme.colors.textHighEmphasis
        )
    }
}

@Preview
@Composable
private fun CallTopAppbarPreview() {
    VideoTheme {
        CallAppBar()
    }
}
