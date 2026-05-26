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

package io.getstream.video.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.getstream.result.Error
import io.getstream.video.android.R
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.failureinjector.FailureInjector
import io.getstream.video.android.core.failureinjector.FailureKey
import io.getstream.video.android.core.internal.InternalStreamVideoApi

@OptIn(InternalStreamVideoApi::class)
@Composable
fun FailureInjectorUi(
    modifier: Modifier = Modifier,
    failureInjector: FailureInjector,
    onClose: () -> Unit,
) {
    val checkedState = remember {
        mutableStateMapOf<FailureKey, Boolean>().apply {
            FailureKey.entries.forEach { key -> put(key, failureInjector.isEnabled(key)) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VideoTheme.colors.baseSheetPrimary),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(VideoTheme.colors.baseSheetSecondary)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(id = R.string.failure_injection),
                style = VideoTheme.typography.subtitleM,
                color = VideoTheme.colors.basePrimary,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(id = R.string.clear_all),
                    modifier = Modifier.clickable {
                        failureInjector.clear()
                        FailureKey.entries.forEach { key -> checkedState[key] = false }
                    },
                    style = VideoTheme.typography.bodyS,
                    color = VideoTheme.colors.brandPrimary,
                )

                Box(
                    modifier = Modifier
                        .background(
                            color = VideoTheme.colors.baseSheetTertiary,
                            shape = RoundedCornerShape(999.dp),
                        )
                        .clickable(onClick = onClose)
                        .padding(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(id = R.string.close),
                        tint = VideoTheme.colors.basePrimary,
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(FailureKey.entries) { key ->
                val checked = checkedState[key] ?: false
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val next = !checked
                            checkedState[key] = next
                            failureInjector.setEnabled(key, next)
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { next ->
                            checkedState[key] = next
                            failureInjector.setEnabled(key, next)
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = VideoTheme.colors.brandPrimary,
                            uncheckedColor = VideoTheme.colors.baseSecondary,
                            checkmarkColor = VideoTheme.colors.baseSheetPrimary,
                        ),
                    )
                    Text(
                        text = key.name,
                        style = VideoTheme.typography.bodyM,
                        color = VideoTheme.colors.basePrimary,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(VideoTheme.colors.baseSheetSecondary)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = VideoTheme.colors.brandPrimary,
                    contentColor = VideoTheme.colors.baseSheetPrimary,
                ),
            ) {
                Text(
                    text = stringResource(id = R.string.ok),
                    style = VideoTheme.typography.labelL,
                )
            }
        }
    }
}

@Preview
@Composable
fun FaultInjectorUiDemo() {
    VideoTheme {
        FailureInjectorUi(
            Modifier,
            object : FailureInjector {
                override fun enable(key: FailureKey) {}

                override fun disable(key: FailureKey) {}

                override fun setEnabled(
                    key: FailureKey,
                    enabled: Boolean,
                ) {}

                override fun isEnabled(key: FailureKey): Boolean = false

                override fun clear() {}

                override fun throwDebugFault(key: FailureKey) {}
                override fun sendFailResult(
                    key: FailureKey,
                ): io.getstream.result.Result.Failure {
                    return io.getstream.result.Result.Failure(
                        Error.GenericError("Failure injected: $key"),
                    )
                }
            },
        ) {}
    }
}
