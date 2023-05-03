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

package io.getstream.video.android.dogfooding.ui.join

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.user.UserPreferences
import io.getstream.video.android.dogfooding.dogfoodingApp
import javax.inject.Inject

@HiltViewModel
class CallJoinViewModel @Inject constructor(
    userPreferences: UserPreferences
) : ViewModel() {

    val user: User? = userPreferences.getUserCredentials()

    fun startNewCall() {
    }

    fun joinCall(callId: String) {
    }

    fun signOut(context: Context) {
        context.dogfoodingApp.logOut()
    }
}
