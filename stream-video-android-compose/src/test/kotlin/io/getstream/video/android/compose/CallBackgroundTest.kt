package io.getstream.video.android.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.Paparazzi
import io.getstream.video.android.common.util.mockUsers
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.avatar.Avatar
import io.getstream.video.android.compose.ui.components.background.CallBackground
import io.getstream.video.android.core.model.CallType
import io.getstream.video.android.core.model.CallUser
import io.getstream.video.android.core.model.CallUserState
import org.junit.Rule
import org.junit.Test

internal class CallBackgroundTest {
    @get:Rule
    val paparazzi = Paparazzi()

    @Test
    fun `snapshot CallBackground composable with an Avatar`() {
        paparazzi.snapshot {
            VideoTheme(isInDarkMode = false) {
                CallBackground(participants = listOf(mockUsers.first().let {
                    CallUser(
                        id = it.id,
                        name = it.name,
                        imageUrl = it.profileImageURL ?: "",
                        role = it.role,
                        teams = emptyList(),
                        updatedAt = null,
                        createdAt = null,
                        state = CallUserState("", false, false, false)
                    )
                }), callType = CallType.VIDEO, isIncoming = true) {
                    Box(modifier = Modifier.align(Alignment.Center)) {
                        Avatar(
                            modifier = Modifier.size(56.dp), imageUrl = "", initials = "CC"
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `snapshot CallBackground composable with an Avatar dark theme`() {
        paparazzi.snapshot {
            VideoTheme(isInDarkMode = true) {
                CallBackground(participants = listOf(mockUsers.first().let {
                    CallUser(
                        id = it.id,
                        name = it.name,
                        imageUrl = it.profileImageURL ?: "",
                        role = it.role,
                        teams = emptyList(),
                        updatedAt = null,
                        createdAt = null,
                        state = CallUserState("", false, false, false)
                    )
                }), callType = CallType.VIDEO, isIncoming = true) {
                    Box(modifier = Modifier.align(Alignment.Center)) {
                        Avatar(
                            modifier = Modifier.size(56.dp), imageUrl = "", initials = "CC"
                        )
                    }
                }
            }
        }
    }
}