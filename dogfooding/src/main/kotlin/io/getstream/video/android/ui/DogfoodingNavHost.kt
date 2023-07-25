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

package io.getstream.video.android.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.getstream.video.android.RingCallActivity
import io.getstream.video.android.ui.join.CallJoinScreen
import io.getstream.video.android.ui.lobby.CallLobbyScreen
import io.getstream.video.android.ui.login.LoginScreen
import io.getstream.video.android.ui.outgoing.DebugCallScreen

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = AppScreens.Login.destination
) {
    NavHost(
        modifier = modifier.fillMaxSize(),
        navController = navController,
        startDestination = startDestination
    ) {
        composable(AppScreens.Login.destination) {
            LoginScreen(
                navigateToCallJoin = {
                    navController.navigate(AppScreens.CallJoin.destination) {
                        popUpTo(AppScreens.Login.destination) { inclusive = true }
                    }
                }
            )
        }
        composable(AppScreens.CallJoin.destination) {
            CallJoinScreen(
                navigateToCallLobby = { cid ->
                    navController.navigate("${AppScreens.CallLobby.destination}/$cid")
                },
                navigateUpToLogin = {
                    navController.navigate(AppScreens.Login.destination) {
                        popUpTo(AppScreens.CallJoin.destination) { inclusive = true }
                    }
                },
                navigateToRingTest = {
                    navController.navigate(AppScreens.DebugCall.destination)
                }
            )
        }
        composable(
            "${AppScreens.CallLobby.destination}/{cid}",
            arguments = listOf(navArgument("cid") { type = NavType.StringType })
        ) {
            CallLobbyScreen(
                navigateUpToLogin = {
                    navController.navigate(AppScreens.Login.destination) {
                        popUpTo(AppScreens.CallJoin.destination) { inclusive = true }
                    }
                }
            )
        }
        composable(AppScreens.DebugCall.destination) {
            val context = LocalContext.current
            DebugCallScreen(
                navigateToRingCall = { callId, members ->
                    context.startActivity(RingCallActivity.createIntent(
                        context,
                        members = members.split(","),
                        callId = callId
                    ))
                }
            )
        }
    }
}

enum class AppScreens(val destination: String) {
    Login("login"),
    CallJoin("call_join"),
    CallLobby("call_preview"),
    DebugCall("debug_call")
}
