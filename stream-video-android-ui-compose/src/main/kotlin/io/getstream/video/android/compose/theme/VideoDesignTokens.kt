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

package io.getstream.video.android.compose.theme

import androidx.compose.ui.graphics.Color
import io.getstream.video.android.compose.theme.design.StreamDesign

// Video-only semantic tokens. They derive from the shared [StreamDesign.Colors] and stay in this
// package so the shared layer in `theme.design` can move to a common module unchanged.

/** Background for the accept call action. Green, so accept and decline read as opposites. */
internal val StreamDesign.Colors.controlAcceptCallButtonBg: Color
    get() = accentSuccess

/** Icon or label on the accept call action, sitting on the success background. */
internal val StreamDesign.Colors.controlAcceptCallButtonText: Color
    get() = textOnAccent

/** Connection quality indicator at its strongest level. */
internal val StreamDesign.Colors.indicatorGreat: Color
    get() = accentSuccess

/** Connection quality indicator at its middle level. */
internal val StreamDesign.Colors.indicatorFair: Color
    get() = accentWarning

/** Connection quality indicator at its weakest level. */
internal val StreamDesign.Colors.indicatorPoor: Color
    get() = accentError

/** Active speaking indicator on a participant tile. Driven by voice activity, not mute state. */
internal val StreamDesign.Colors.indicatorSpeaking: Color
    get() = brand.s300
