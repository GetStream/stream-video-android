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

package io.getstream.video.android.compose.ui.components.base

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.theme.design.StreamTokens

/**
 * A modal bottom sheet with rounded top corners, a drag handle and an optional centered title.
 *
 * @param onDismissRequest Called when the user dismisses the sheet by swiping, tapping the scrim or pressing back.
 * @param modifier The modifier applied to the sheet.
 * @param title The heading shown below the drag handle, or null for none.
 * @param content The sheet content, laid out as a column below the header.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StreamBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        // The sheet renders in its own window, so the app-level testTagsAsResourceId does not
        // reach it; re-enable it here for UiAutomator access to the content tags.
        modifier = modifier.semantics { testTagsAsResourceId = true },
        sheetState = rememberStreamSheetState(),
        shape = SheetShape,
        containerColor = VideoTheme.colors.backgroundCoreElevation1,
        scrimColor = VideoTheme.colors.backgroundCoreScrim,
        dragHandle = { SheetDragHandle() },
    ) {
        title?.let { SheetTitle(text = it) }
        content()
    }
}

@Composable
private fun SheetDragHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(StreamTokens.spacingMd),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = StreamTokens.size32, height = StreamTokens.size4)
                .background(
                    color = VideoTheme.colors.accentNeutral,
                    shape = RoundedCornerShape(percent = 50),
                ),
        )
    }
}

@Composable
private fun SheetTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = StreamTokens.spacingSm,
                end = StreamTokens.spacingSm,
                bottom = StreamTokens.spacingSm,
            ),
        style = VideoTheme.typography.headingSmall,
        color = VideoTheme.colors.textPrimary,
        textAlign = TextAlign.Center,
    )
}

/**
 * Remembers a sheet state that starts expanded in inspection mode.
 *
 * A sheet that starts hidden animates in and snapshots blank, so previews and screenshot tests
 * get a state that is already at its resting position.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberStreamSheetState(): SheetState {
    if (!LocalInspectionMode.current) {
        return rememberModalBottomSheetState()
    }
    val density = LocalDensity.current
    val confirmValueChange: (SheetValue) -> Boolean = { true }
    return rememberSaveable(
        saver = SheetState.Saver(
            skipPartiallyExpanded = false,
            confirmValueChange = confirmValueChange,
            density = density,
            skipHiddenState = true,
        ),
    ) {
        SheetState(
            skipPartiallyExpanded = false,
            density = density,
            initialValue = SheetValue.PartiallyExpanded,
            confirmValueChange = confirmValueChange,
            skipHiddenState = true,
        )
    }
}

private val SheetShape = RoundedCornerShape(
    topStart = StreamTokens.radius4xl,
    topEnd = StreamTokens.radius4xl,
    bottomStart = CornerSize(0),
    bottomEnd = CornerSize(0),
)
