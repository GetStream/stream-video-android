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

package io.getstream.video.android.banchmark

import android.content.Context
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.data.services.stream.StreamService
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.model.User
import io.getstream.video.android.util.UserHelper
import io.getstream.video.android.util.config.AppConfig
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex

data class BenchmarkConfig(
    val joinIntervalMin: Long = 5000,
    val joinInterval: Long = 10_000,
    val leaveIntervalMin: Long = 5000,
    val leaveInterval: Long = 10_000,
    val minUsers: Int = 150,
    val maxUsers: Int = 1500,
)

class Benchmark(val callId: StreamCallId, val config: BenchmarkConfig) {

    val logger by taggedLogger("Benchmark")

    val benchmarkScope = CoroutineScope(
        CoroutineExceptionHandler { ctx, t ->
            logger.e { t.message ?: "Unknown" }
        },
    ) + SupervisorJob()

    val users = MutableStateFlow<MutableMap<String, ParticipantState>>(mutableMapOf())
    val mutex = Mutex()

    fun run(context: Context) {
        benchmarkScope.launch {
            while (true) {
                delay(1000)
                if (users.value.size >= config.maxUsers) {
                    // Skip this frame if we are at the max users
                    continue
                } else {
                    // Create new client and join it
                    benchmarkScope.launch(Dispatchers.IO) {
                        val userId = UserHelper.generateRandomString()

                        val authData = StreamService.instance.getAuthData(
                            environment = AppConfig.currentEnvironment.value!!.env,
                            userId = userId,
                        )
                        // Build client
                        val client = StreamVideoBuilder(
                            context = context,
                            apiKey = authData.apiKey,
                            token = authData.token,
                            user = User(
                                id = userId,
                                name = userId,
                            ),
                        ).build()
                        logger.d { "Created new client." }

                        // Get call
                        val sessionId = MutableStateFlow<String?>(null)
                        val call = client.call(callId.type, callId.id)
                        logger.d { "[call] got call: ${call.cid}" }
                        delay(random(config.joinIntervalMin, config.joinInterval))

                        // Join after delay.
                        call.join().onSuccess { _ ->
                            logger.d { "Joined user: ${authData.userId}" }
                            benchmarkScope.launch {
                                call.state.me.collectLatest { participant ->
                                    mutex.lock()
                                    if (participant != null) {
                                        logger.d { "Joined call with session id: ${participant.sessionId}" }
                                        sessionId.value = participant.sessionId
                                        val currentUsers = users.value
                                        currentUsers[participant.sessionId] = participant
                                        users.value = currentUsers
                                    }
                                    mutex.unlock()
                                }
                            }

                            // Leave after delay
                            benchmarkScope.launch {
                                delay(random(config.leaveIntervalMin, config.leaveInterval))
                                call.leave()
                                client.cleanup()
                            }
                        }.onError {
                            logger.e { "Failed to join call: $it" }
                        }
                    }
                }
            }
        }
    }

    fun stop() {
        logger.d { "Stopping benchmark, $config" }
        benchmarkScope.cancel()
    }

    private fun random(min: Long, max: Long) = (min..max).random()
}
