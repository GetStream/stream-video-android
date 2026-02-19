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

package io.getstream.video.android.compose.ui.components.base.styling

/**
 * Composite styles provider providing various components styles.
 */
public open class CompositeStyleProvider(
    public val iconStyles: IconStyleProvider = IconStyles,
    public val textFieldStyles: TextFieldStyleProvider = StreamTextFieldStyles,
    public val textStyles: TextStyleProvider = StreamTextStyles,
    public val buttonStyles: ButtonStyleProvider = ButtonStyles,
    public val dialogStyles: DialogStyleProvider = StreamDialogStyles,
    public val badgeStyles: BadgeStyleProvider = StreamBadgeStyles,
)
