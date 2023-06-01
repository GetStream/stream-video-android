/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.dogfooding.ui.call

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import io.getstream.video.android.common.permission.PermissionManager
import io.getstream.video.android.common.viewmodel.CallViewModel
import io.getstream.video.android.common.viewmodel.CallViewModelFactory
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.CallContainer
import io.getstream.video.android.compose.ui.components.call.controls.ControlActions
import io.getstream.video.android.compose.ui.components.call.controls.actions.FlipCameraAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.LeaveCallAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleCameraAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleMicrophoneAction
import io.getstream.video.android.compose.ui.components.call.renderer.CallSingleVideoRenderer
import io.getstream.video.android.compose.ui.components.call.renderer.ParticipantLabel
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.call.state.ToggleCamera
import io.getstream.video.android.core.call.state.ToggleMicrophone
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.model.streamCallId
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

class CallActivity : ComponentActivity() {

    private val factory by lazy { callViewModelFactory() }
    private val vm by viewModels<CallViewModel> { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        vm.setPermissionManager(getPermissionManager())
        vm.setOnLeaveCall { finish() }

        setContent {
            VideoTheme {
                CallContainer(
                    modifier = Modifier.background(color = VideoTheme.colors.appBackground),
                    callViewModel = vm,
                    onBackPressed = { finish() },
                    videoRenderer = { modifier, call, participant, style ->
                        CallSingleVideoRenderer(
                            modifier = modifier,
                            call = call,
                            participant = participant,
                            style = style,
                            labelContent = {
                                val fakeAudio by fakeAudioState().collectAsState()
                                ParticipantLabel(
                                    participant = participant,
                                    soundIndicatorContent = {
                                        AudioVolumeIndicator(fakeAudio)
                                    }
                                )
                            }
                        )
                    },
                    callControlsContent = {
                        ControlActions(
                            callViewModel = vm,
                            actions = listOf(
                                {
                                    ToggleCameraAction(
                                        modifier = Modifier.size(52.dp),
                                        isCameraEnabled = vm.callDeviceState.value.isCameraEnabled,
                                        onCallAction = vm::onCallAction
                                    )
                                },
                                {
                                    ToggleMicrophoneAction(
                                        modifier = Modifier.size(52.dp),
                                        isMicrophoneEnabled = vm.callDeviceState.value.isMicrophoneEnabled,
                                        onCallAction = vm::onCallAction
                                    )
                                },
                                {
                                    FlipCameraAction(
                                        modifier = Modifier.size(52.dp),
                                        onCallAction = vm::onCallAction
                                    )
                                },
                                {
                                    LeaveCallAction(
                                        modifier = Modifier.size(52.dp),
                                        onCallAction = vm::onCallAction
                                    )
                                },
                            ),
                            onCallAction = vm::onCallAction
                        )
                    }
                )
            }
        }
    }

    val call by lazy {
        val (type, id) =
            intent.streamCallId(EXTRA_CALL_ID)
                ?: throw IllegalArgumentException("You must pass correct channel id.")
        StreamVideo.instance().call(type = type, id = id)
    }

    private fun callViewModelFactory(): CallViewModelFactory {
        return CallViewModelFactory(call = call)
    }

    override fun onPause() {
        super.onPause()
        call.camera.pause()
    }

    override fun onResume() {
        super.onResume()
        call.camera.resume()
    }

    private fun getPermissionManager(): PermissionManager {
        return PermissionManager.create(
            activity = this,
            onPermissionResult = { permission, isGranted ->
                when (permission) {
                    android.Manifest.permission.CAMERA -> vm.onCallAction(ToggleCamera(isGranted))
                    android.Manifest.permission.RECORD_AUDIO -> vm.onCallAction(
                        ToggleMicrophone(
                            isGranted
                        )
                    )
                }
            },
            onShowRequestPermissionRationale = {}
        )
    }

    companion object {
        internal const val EXTRA_CALL_ID = "EXTRA_CALL_ID"

        fun getIntent(
            context: Context,
            callId: StreamCallId
        ): Intent {
            return Intent(context, CallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(EXTRA_CALL_ID, callId)
            }
        }
    }
}


fun fakeAudioState(): StateFlow<List<Float>> {
    val audioFlow = flow {
        val audioLevels = mutableListOf(0f, 0f, 0f, 0f, 0f)
        while (true) {
            val newValue = Random.nextFloat()
            audioLevels.removeAt(0)
            audioLevels.add(newValue)
            emit(audioLevels.toList())
            delay(300)
        }
    }
    return audioFlow.stateIn(
        scope = CoroutineScope(Dispatchers.Default),
        started = SharingStarted.Eagerly,
        initialValue = listOf(0f, 0f, 0f, 0f, 0f)
    )
}

@Composable
fun AudioVolumeIndicator(audioState: List<Float>) {
    // based on this fun blogpost: https://proandroiddev.com/jetpack-compose-tutorial-replicating-dribbble-audio-app-part-1-513ac91c02e3
    val infiniteAnimation = rememberInfiniteTransition()
    val animations = mutableListOf<State<Float>>()

    repeat(5) {
        val durationMillis = Random.nextInt(500, 1000)
        animations += infiniteAnimation.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis),
                repeatMode = RepeatMode.Reverse,
            )
        )
    }

    Canvas(modifier = Modifier.width(45.dp).padding(horizontal = 12.dp)) {
        val canvasCenterY = 0
        var startOffset = 0f
        val barWidthFloat = 10f
        val barMinHeight = 0f
        val barMaxHeight = 150f
        val gapWidthFloat = 1f

        repeat(5) { index ->
            val currentSize = animations[index % animations.size].value
            var barHeightPercent = audioState[index] + currentSize
            if (barHeightPercent > 1.0f) {
                val diff = barHeightPercent - 1.0f
                barHeightPercent = 1.0f - diff
            }
            val barHeight = barMinHeight + (barMaxHeight - barMinHeight) * barHeightPercent
            drawLine(
                color = Color(0xFF9CCC65),
                start = Offset(startOffset, canvasCenterY - barHeight / 2),
                end = Offset(startOffset, canvasCenterY + barHeight / 2),
                strokeWidth = barWidthFloat,
                cap = StrokeCap.Round,
            )
            startOffset += barWidthFloat + gapWidthFloat
        }
    }
}