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
import io.getstream.video.android.DirectCallActivity
import io.getstream.video.android.ui.join.CallJoinScreen
import io.getstream.video.android.ui.lobby.CallLobbyScreen
import io.getstream.video.android.ui.login.LoginScreen
import io.getstream.video.android.ui.outgoing.DirectCallJoinScreen

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = AppScreens.Login.route,
) {
    NavHost(
        modifier = modifier.fillMaxSize(),
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(AppScreens.Login.route) { backStackEntry ->
            LoginScreen(
                autoLogIn = backStackEntry.arguments?.getString("auto_log_in")?.let { it.toBoolean() } ?: true,
                navigateToCallJoin = {
                    navController.navigate(AppScreens.CallJoin.route) {
                        popUpTo(AppScreens.Login.route) { inclusive = true }
                    }
                },
            )
        }
        composable(AppScreens.CallJoin.route) {
            CallJoinScreen(
                navigateToCallLobby = { cid ->
                    navController.navigate(AppScreens.CallLobby.routeWithArg(cid))
                },
                navigateUpToLogin = { autoLogIn ->
                    navController.navigate(AppScreens.Login.routeWithArg(autoLogIn)) {
                        popUpTo(AppScreens.CallJoin.route) { inclusive = true }
                    }
                },
                navigateToDirectCallJoin = {
                    navController.navigate(AppScreens.DirectCallJoin.route)
                },
            )
        }
        composable(
            AppScreens.CallLobby.route,
            arguments = listOf(navArgument("cid") { type = NavType.StringType }),
        ) {
            CallLobbyScreen(
                navigateUpToLogin = {
                    navController.navigate(AppScreens.Login.route) {
                        popUpTo(AppScreens.CallJoin.route) { inclusive = true }
                    }
                },
            )
        }
        composable(AppScreens.DirectCallJoin.route) {
            val context = LocalContext.current
            DirectCallJoinScreen(
                navigateToDirectCall = { members ->
                    context.startActivity(
                        DirectCallActivity.createIntent(
                            context,
                            members = members.split(","),
                        ),
                    )
                },
            )
        }
    }
}

enum class AppScreens(val route: String) {
    Login("login/{auto_log_in}"),
    CallJoin("call_join"),
    CallLobby("call_lobby/{cid}"),
    DirectCallJoin("direct_call_join");

    fun routeWithArg(argValue: Any): String = when (this) {
        Login -> this.route.replace("{auto_log_in}", argValue.toString())
        CallLobby -> this.route.replace("{cid}", argValue.toString())
        else -> this.route
    }
}