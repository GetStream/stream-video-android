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

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.base.styling.StreamTextFieldStyles
import io.getstream.video.android.compose.ui.components.base.styling.TextFieldStyle
import io.getstream.video.android.compose.ui.components.base.styling.styleState

@Composable
public fun StreamOutlinedTextField(
    modifier: Modifier = Modifier,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    style: TextFieldStyle,
    placeholder: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
): Unit = OutlinedTextField(
    leadingIcon = leadingIcon,
    trailingIcon = trailingIcon,
    modifier = modifier,
    enabled = enabled,
    readOnly = readOnly,
    placeholder = placeholder,
    isError = isError,
    visualTransformation = visualTransformation,
    keyboardActions = keyboardActions,
    keyboardOptions = keyboardOptions,
    maxLines = maxLines,
    minLines = minLines,
    interactionSource = interactionSource,
    shape = style.shape,
    value = value,
    colors = style.colors,
    onValueChange = onValueChange,
)

@Composable
public fun StreamTextField(
    modifier: Modifier = Modifier,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    style: TextFieldStyle = VideoTheme.styles.textFieldStyles.defaultTextField(),
    placeholder: String? = null,
    error: Boolean = false,
    maxLines: Int = 1,
    minLines: Int = 1,
    icon: ImageVector? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
): Unit = StreamOutlinedTextField(
    modifier = modifier,
    enabled = enabled,
    readOnly = readOnly,
    placeholder = @Composable {
        val textStyle = style.placeholderStyle.of(
            state = styleState(
                interactionSource = interactionSource,
                enabled = enabled,
            ),
        )
        Text(
            text = placeholder ?: "",
            style = textStyle.value.platform,
        )
    },
    leadingIcon = icon?.let {
        @Composable {
            val iconStyle = style.iconStyle.of(
                state = styleState(
                    interactionSource = interactionSource,
                    enabled = enabled,
                ),
            )
            Icon(imageVector = it, contentDescription = it.name, tint = iconStyle.value.color)
        }
    },
    trailingIcon = error.takeIf { it }?.let {
        @Composable {
            Icon(
                tint = style.colors.trailingIconColor(enabled = enabled, isError = true).value,
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = Icons.Default.ErrorOutline.name,
            )
        }
    },
    style = style,
    isError = error,
    visualTransformation = visualTransformation,
    keyboardActions = keyboardActions,
    keyboardOptions = keyboardOptions,
    singleLine = maxLines == 1,
    maxLines = Math.max(minLines, maxLines),
    minLines = minLines,
    interactionSource = interactionSource,
    value = value,
    onValueChange = onValueChange,
)

@Preview
@Composable
private fun StreamInputFieldsPreviews() {
    VideoTheme {
        Column {
            // Empty
            StreamTextField(
                value = TextFieldValue(""),
                placeholder = "Call ID",
                onValueChange = { },
                style = StreamTextFieldStyles.defaultTextField(),
            )
            Spacer(modifier = Modifier.size(16.dp))
            StreamTextField(
                value = TextFieldValue(""),
                onValueChange = { },
                style = StreamTextFieldStyles.defaultTextField(),
            )
            Spacer(modifier = Modifier.size(16.dp))

            StreamTextField(
                icon = Icons.Outlined.Phone,
                value = TextFieldValue(""),
                onValueChange = { },
                style = StreamTextFieldStyles.defaultTextField(),
            )
            Spacer(modifier = Modifier.size(16.dp))

            // Not empty
            StreamTextField(
                value = TextFieldValue("Some value"),
                placeholder = "Call ID",
                onValueChange = { },
                style = StreamTextFieldStyles.defaultTextField(),
            )
            Spacer(modifier = Modifier.size(16.dp))

            StreamTextField(
                icon = Icons.Outlined.Phone,
                value = TextFieldValue("+ 123 456 789"),
                placeholder = "Call ID",
                onValueChange = { },
                style = StreamTextFieldStyles.defaultTextField(),
            )
            Spacer(modifier = Modifier.size(16.dp))

            // Disabled
            StreamTextField(
                enabled = false,
                value = TextFieldValue(""),
                placeholder = "Call ID",
                onValueChange = { },
                style = StreamTextFieldStyles.defaultTextField(),
            )
            Spacer(modifier = Modifier.size(16.dp))

            // Error
            StreamTextField(
                error = true,
                value = TextFieldValue("Wrong data"),
                placeholder = "Call ID",
                onValueChange = { },
                style = StreamTextFieldStyles.defaultTextField(),
            )
            Spacer(modifier = Modifier.size(16.dp))

            StreamTextField(
                value = TextFieldValue(""),
                placeholder = "Message",
                onValueChange = { },
                minLines = 8,
                style = StreamTextFieldStyles.defaultTextField(),
            )
            Spacer(modifier = Modifier.size(16.dp))
        }
    }
}
