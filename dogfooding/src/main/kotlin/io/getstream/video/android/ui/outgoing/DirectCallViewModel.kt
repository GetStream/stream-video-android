/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.ui.outgoing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.getstream.video.android.data.repositories.GoogleAccountRepository
import io.getstream.video.android.models.StreamUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class DirectCallViewModel @Inject constructor(
    private val googleAccountRepository: GoogleAccountRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(DirectCallUiState())
    val uiState = _uiState.asStateFlow()

    fun getAllUsers() {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    users = googleAccountRepository.getAllAccounts()?.map { user ->
                        UserUiState(
                            isSelected = false,
                            user = user
                        )
                    } ?: emptyList()
                )
            }
        }
    }

    fun toggleUserSelection(selectedIndex: Int) {
        _uiState.update {
            it.copy(
                users = it.users.mapIndexed { index, userUiState ->
                    if (index == selectedIndex) {
                        UserUiState(isSelected = !userUiState.isSelected, user = userUiState.user)
                    } else {
                        userUiState
                    }
                }
            )
        }
    }
}

data class DirectCallUiState(
    val isLoading: Boolean = false,
    val users: List<UserUiState> = emptyList(),
)

data class UserUiState(
    val isSelected: Boolean = false,
    val user: StreamUser
)