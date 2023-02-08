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

package io.getstream.video.android.tutorial_final.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.getstream.video.android.tutorial_final.user.AppUser

@Composable
fun UserList(
    userItems: List<AppUser>,
    modifier: Modifier = Modifier,
    onClick: (AppUser) -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        userItems.forEachIndexed { index, user ->
            if (index > 0) {
                Divider(startIndent = 16.dp, thickness = 0.5.dp, color = Color.LightGray)
            }
            UserItem(
                appUser = user,
                onClick = onClick
            )
        }
    }
}
