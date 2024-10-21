/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.tutorial.livestream

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.model.User

@Composable
fun LiveNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = LiveScreens.Main.destination,
) {
    NavHost(
        modifier = modifier
            .fillMaxSize()
            .background(VideoTheme.colors.baseSheetPrimary),
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(LiveScreens.Main.destination) {
            LiveMain(navController = navController)
        }

        composable(LiveScreens.Host.destination, LiveScreens.Host.args) {
            LiveHost(callId = LiveScreens.Host.getCallId(it))
        }

        composable(LiveScreens.Guest.destination, LiveScreens.Guest.args) {
            LiveAudience(callId = LiveScreens.Guest.getCallId(it))
        }
    }
}

sealed class LiveScreens(val destination: String) {
    data object Main : LiveScreens(destination = "main")

    sealed class HasCallId(destination: String) : LiveScreens(destination) {
        private val argCallId: String = "call_id"
        val args = listOf(navArgument(argCallId) { type = NavType.StringType })

        fun getCallId(backStackEntry: NavBackStackEntry): String {
            return backStackEntry.arguments?.getString(argCallId) ?: error("Call ID not found")
        }
    }

    data object Host : HasCallId(destination = "host/{call_id}") {
        fun destination(callId: String): String {
            return "host/$callId"
        }
    }
    data object Guest : HasCallId(destination = "guest/{call_id}") {
        fun destination(callId: String): String {
            return "guest/$callId"
        }
    }
}
