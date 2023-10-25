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
import io.getstream.video.android.datastore.delegate.StreamUserDataStore
import io.getstream.video.android.model.User
import io.getstream.video.android.models.GoogleAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DirectCallViewModel @Inject constructor(
    private val userDataStore: StreamUserDataStore,
    private val googleAccountRepository: GoogleAccountRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DirectCallUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(currentUser = userDataStore.user.firstOrNull()) }
        }
    }

    fun getGoogleAccounts() {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    googleAccounts = googleAccountRepository.getAllAccounts()?.map { user ->
                        GoogleAccountUiState(
                            isSelected = false,
                            account = user,
                        )
                    },
                )
            }
        }
    }

    fun toggleGoogleAccountSelection(selectedIndex: Int) {
        _uiState.update {
            it.copy(
                googleAccounts = it.googleAccounts?.mapIndexed { index, accountUiState ->
                    if (index == selectedIndex) {
                        GoogleAccountUiState(
                            isSelected = !accountUiState.isSelected,
                            account = accountUiState.account,
                        )
                    } else {
                        accountUiState
                    }
                },
            )
        }
    }
}

data class DirectCallUiState(
    val isLoading: Boolean = false,
    val currentUser: User? = null,
    val googleAccounts: List<GoogleAccountUiState>? = emptyList(),
)

data class GoogleAccountUiState(
    val isSelected: Boolean = false,
    val account: GoogleAccount,
)
