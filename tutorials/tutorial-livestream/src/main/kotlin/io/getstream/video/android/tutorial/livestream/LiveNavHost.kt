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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.getstream.log.Priority
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.GEO
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfigRegistry
import io.getstream.video.android.core.notifications.internal.service.DefaultCallConfigurations
import io.getstream.video.android.model.User
import kotlinx.coroutines.delay

@Composable
fun LiveNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = LiveScreens.Main.destination,
) {
    val viewModel = viewModel<LivestreamViewModel>()

    val context = LocalContext.current
    val userId = "liviu-host"
//    val userId = "liviu-guest-1"
//    val userId = "liviu"
//    val userToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoibGl2aXUtaG9zdCJ9.wkm7B7DAnXKfg_ojlh7FGSr4RsVj2HsurysHSdz2kbw" // host
    val userToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoibGl2aXUtZ3Vlc3QtMSJ9.YqhYLm1z24uQSgPbvo_h7vC5dmW4QgXSTC-bF9S9hi4" // guest-1
//    val userToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoibGl2aXUifQ.MOIrZ6DARGNwRXGD3e5r5B2R7_B9MQJrbnxfih6YPlA" alfagift

    // step1 - create a user.
    val user = User(
        id = userId, // any string
        role = "user",
    )

    val callServiceConfigRegistry = CallServiceConfigRegistry()
    callServiceConfigRegistry.register(DefaultCallConfigurations.getLivestreamCallServiceConfig())

    // step2 - initialize StreamVideo. For a production app we recommend adding the client to your Application class or di module.
    val client = StreamVideoBuilder(
        context = context,
        apiKey = "par8f5s3gn2j",
        geo = GEO.GlobalEdgeNetwork,
        user = user,
        token = userToken,
        ensureSingleInstance = false,
        callServiceConfigRegistry = callServiceConfigRegistry,
        loggingLevel = LoggingLevel(priority = Priority.VERBOSE),
    ).build()

    NavHost(
        modifier = modifier
            .fillMaxSize()
            .background(VideoTheme.colors.baseSheetPrimary),
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(LiveScreens.Main.destination) {
            LiveMain(navController = navController, viewModel = viewModel)
        }

        composable(LiveScreens.Host.destination, LiveScreens.Host.args) {
            LiveHost(navController = navController, callId = LiveScreens.Host.getCallId(it), client)
        }

        composable(LiveScreens.Guest.destination, LiveScreens.Guest.args) {
            val callId = LiveScreens.Guest.getCallId(it)

            LaunchedEffect(Unit) {
                // Setting both just for testing
                viewModel.setInViewerScreen(true)
                viewModel.isInViewerScreenState.value = true

                delay(200)
            }
            // Maybe merge these two effects
            DisposableEffect(callId) {
                viewModel.setCurrentCallId(callId)

                onDispose {
                    // Setting both just for testing
                    viewModel.setInViewerScreen(false)
                    viewModel.isInViewerScreenState.value = false
                }
            }

            LiveAudience(
                navController = navController,
                callId = LiveScreens.Guest.getCallId(it),
                client,
                viewModel,
            )
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
