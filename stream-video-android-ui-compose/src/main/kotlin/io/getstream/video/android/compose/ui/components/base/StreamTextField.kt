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

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import io.getstream.video.android.compose.R
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.theme.design.StreamTokens

/**
 * A single or multi line text input with an outlined field and an optional error message below it.
 *
 * @param value The current text and selection.
 * @param onValueChange Called with the new value when the user edits the text.
 * @param modifier The modifier applied to the whole input, field and error message included.
 * @param enabled Whether the field accepts input. Disabled fields use the disabled text color.
 * @param readOnly Whether the text can be selected but not edited.
 * @param placeholder The hint shown while [value] is empty, or null for none.
 * @param errorText The error shown below the field, or null when the input is valid.
 * @param leadingIcon The icon shown before the text, or null for none.
 * @param trailingIcon The icon shown after the text, or null for none.
 * @param maxLines The maximum number of visible lines. One line makes the field single line.
 * @param minLines The minimum number of visible lines.
 * @param visualTransformation The transformation applied to the displayed text, such as password masking.
 * @param keyboardOptions The software keyboard options.
 * @param keyboardActions The callbacks for the keyboard IME actions.
 * @param interactionSource The interaction source that reports the focus state of the field.
 */
@Composable
public fun StreamTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    placeholder: String? = null,
    errorText: String? = null,
    leadingIcon: Painter? = null,
    trailingIcon: Painter? = null,
    maxLines: Int = 1,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val colors = VideoTheme.colors
    val focused by interactionSource.collectIsFocusedAsState()
    val borderColor = when {
        errorText != null -> colors.borderUtilityError
        focused -> colors.borderUtilityFocused
        else -> colors.borderCoreDefault
    }
    val textColor = if (enabled) colors.inputTextDefault else colors.inputTextDisabled
    val iconColor = if (focused) colors.inputTextIconActive else colors.inputTextIcon
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(StreamTokens.spacingXs),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { errorText?.let { error(it) } },
            enabled = enabled,
            readOnly = readOnly,
            textStyle = VideoTheme.typography.bodyDefault.copy(color = textColor),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = maxLines == 1 && minLines == 1,
            maxLines = maxOf(minLines, maxLines),
            minLines = minLines,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            cursorBrush = SolidColor(colors.accentPrimary),
        ) { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = StreamTokens.size48)
                    .border(
                        width = StreamTokens.strokeW100,
                        color = borderColor,
                        shape = RoundedCornerShape(StreamTokens.inputRadiusTextInput),
                    )
                    .padding(
                        horizontal = StreamTokens.spacingMd,
                        vertical = StreamTokens.spacingSm,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(StreamTokens.spacingXs),
            ) {
                leadingIcon?.let { FieldIcon(icon = it, tint = iconColor) }
                Box(modifier = Modifier.weight(1f)) {
                    if (value.text.isEmpty() && placeholder != null) {
                        Text(
                            text = placeholder,
                            style = VideoTheme.typography.bodyDefault,
                            color = colors.inputTextPlaceholder,
                            maxLines = 1,
                        )
                    }
                    innerTextField()
                }
                trailingIcon?.let { FieldIcon(icon = it, tint = iconColor) }
            }
        }
        errorText?.let { ErrorMessage(text = it) }
    }
}

@Composable
private fun FieldIcon(icon: Painter, tint: Color) {
    Icon(
        painter = icon,
        contentDescription = null,
        modifier = Modifier.size(StreamTokens.iconSizeMd),
        tint = tint,
    )
}

@Composable
private fun ErrorMessage(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(StreamTokens.spacingXs),
    ) {
        Icon(
            painter = painterResource(R.drawable.stream_design_ic_exclamation_circle),
            contentDescription = null,
            modifier = Modifier.size(StreamTokens.iconSizeMd),
            tint = VideoTheme.colors.accentError,
        )
        Text(
            text = text,
            style = VideoTheme.typography.captionDefault,
            color = VideoTheme.colors.accentError,
        )
    }
}
