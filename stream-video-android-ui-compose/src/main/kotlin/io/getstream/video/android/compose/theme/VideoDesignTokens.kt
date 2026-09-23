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
import io.getstream.video.android.compose.theme.design.StreamPrimitiveColors

// Video-only semantic tokens. They derive from the shared [StreamDesign.Colors] and stay in this
// package so the shared layer in `theme.design` can move to a common module unchanged.

/** Background for the accept call action. Green, so accept and decline read as opposites. */
internal val StreamDesign.Colors.controlAcceptCallButtonBg: Color
    get() = accentSuccess

/** Icon or label on the accept call action, sitting on the success background. */
internal val StreamDesign.Colors.controlAcceptCallButtonText: Color
    get() = textOnAccent

/** Background for the decline and cancel call actions. Red, so accept and decline read as opposites. */
internal val StreamDesign.Colors.controlDeclineCallButtonBg: Color
    get() = accentError

/** Icon or label on the decline and cancel call actions, sitting on the error background. */
internal val StreamDesign.Colors.controlDeclineCallButtonText: Color
    get() = textOnAccent

/**
 * Background for the error badge on a call control. Yellow with no border, so it separates from
 * a red control and from video underneath in both modes.
 */
internal val StreamDesign.Colors.controlCallControlErrorBadgeBg: Color
    get() = accentWarning

/**
 * Text on the call control error badge. Pinned to black because the yellow background does not
 * invert between modes, so a mode-aware text token would turn near-white on yellow in dark mode.
 */
internal val StreamDesign.Colors.controlCallControlErrorBadgeText: Color
    get() = StreamPrimitiveColors.baseBlack

/** Connection quality indicator at its strongest level. */
internal val StreamDesign.Colors.indicatorConnectionQualityGreat: Color
    get() = accentSuccess

/** Connection quality indicator at its middle level. */
internal val StreamDesign.Colors.indicatorConnectionQualityFair: Color
    get() = accentWarning

/** Connection quality indicator at its weakest level. */
internal val StreamDesign.Colors.indicatorConnectionQualityPoor: Color
    get() = accentError

/** Bar of the microphone level meter that the current input level has reached. */
internal val StreamDesign.Colors.indicatorMicrophoneLevelBarActive: Color
    get() = brand.s400

/** Bar of the microphone level meter above the current input level. */
internal val StreamDesign.Colors.indicatorMicrophoneLevelBarInactive: Color
    get() = chrome.s200

/** Active speaking indicator on a participant tile. Driven by voice activity, not mute state. */
internal val StreamDesign.Colors.indicatorSoundIndicatorSpeaking: Color
    get() = brand.s400
