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

package io.getstream.video.android.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.getstream.video.android.app.user.AppUser
import io.getstream.video.android.compose.ui.components.avatar.Avatar
import io.getstream.video.android.utils.initials

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun UserItem(
    appUser: AppUser,
    onClick: (AppUser) -> Unit
) {
    val user = appUser.user
    val isSelected = appUser.isSelected

    val buttonColor = if (isSelected) {
        MaterialTheme.colors.primary
    } else {
        MaterialTheme.colors.secondary
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(color = buttonColor),
                onClick = {
                    onClick(appUser)
                }
            ),
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            val colorState = animateColorAsState(
                targetValue = when (isSelected) {
                    true -> MaterialTheme.colors.secondary
                    else -> Color.Transparent
                },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            )
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .align(alignment = Alignment.CenterVertically)
                    .clip(CircleShape)
                    .background(colorState.value)
            ) {
                Avatar(
                    modifier = Modifier
                        .size(40.dp)
                        .align(alignment = Alignment.Center),
                    imageUrl = user.imageUrl ?: "",
                    initials = user.name.initials()
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .weight(1f),
                text = user.name,
                fontSize = 16.sp,
            )

            AnimatedVisibility(
                visible = isSelected,
                modifier = Modifier.align(Alignment.CenterVertically),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Checkbox(
                    checked = true,
                    onCheckedChange = null
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}
