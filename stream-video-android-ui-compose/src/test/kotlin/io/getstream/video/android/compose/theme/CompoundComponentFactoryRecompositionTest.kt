/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.compose.theme

import android.os.Build
import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
internal class CompoundComponentFactoryRecompositionTest {

    private val baseFactory =
        mutableStateOf<VideoComponentFactory>(object : VideoComponentFactory {})
    private val key = mutableIntStateOf(0)
    private val capturedState = mutableIntStateOf(0)
    private val useOtherContent = mutableStateOf(false)
    private val extraKeys = mutableStateOf(listOf<Any?>(0))
    private val recorder = Recorder()

    @Test
    fun `factory is not rebuilt when only the content changes`() {
        runComposition(defaultContent()) { useOtherContent.value = true }

        assertEquals(1, recorder.built.size)
    }

    @Test
    fun `factory is rebuilt when a value it captures changes`() {
        runComposition(defaultContent()) { capturedState.intValue++ }

        assertEquals(2, recorder.built.size)
        assertEquals(1, recorder.built.last().capturedValue)
    }

    @Test
    fun `factory is rebuilt when compose state it reads changes`() {
        val capturedRecorder = recorder
        val content: @Composable () -> Unit = {
            CompositionLocalProvider(LocalComponentFactory provides baseFactory.value) {
                val readValue by remember { derivedStateOf { capturedState.intValue } }
                CompoundComponentFactory(
                    factory = { currentComponentFactory ->
                        CapturingComponentFactory(
                            currentComponentFactory,
                            readValue,
                        ).also(capturedRecorder.built::add)
                    },
                ) {
                    capturedRecorder.provided = LocalComponentFactory.current
                }
            }
        }

        runComposition(content) { capturedState.intValue++ }

        assertEquals(1, recorder.built.last().capturedValue)
        assertSame(recorder.built.last(), recorder.provided)
    }

    @Test
    fun `factory is rebuilt when a key changes`() {
        runComposition(defaultContent()) { key.intValue++ }

        assertEquals(2, recorder.built.size)
    }

    @Test
    fun `factory is rebuilt when the number of keys changes`() {
        runComposition(defaultContent()) { extraKeys.value = emptyList() }

        assertEquals(2, recorder.built.size)
        assertSame(recorder.built.last(), recorder.provided)
    }

    @Test
    fun `factory is rebuilt when the current component factory changes`() {
        val newBaseFactory = object : VideoComponentFactory {}

        runComposition(defaultContent()) { baseFactory.value = newBaseFactory }

        assertEquals(2, recorder.built.size)
        assertSame(newBaseFactory, recorder.built.last().delegate)
    }

    private fun defaultContent(): @Composable () -> Unit {
        // A stable capture, so the factory lambda below is reused across recompositions without strong skipping.
        val capturedRecorder = recorder
        val content: @Composable () -> Unit = { capturedRecorder.provided = LocalComponentFactory.current }
        val otherContent: @Composable () -> Unit = {
            capturedRecorder.provided = LocalComponentFactory.current
        }
        return {
            CompositionLocalProvider(LocalComponentFactory provides baseFactory.value) {
                val capturedValue = capturedState.intValue
                CompoundComponentFactory(
                    key.intValue,
                    *extraKeys.value.toTypedArray(),
                    factory = { currentComponentFactory ->
                        CapturingComponentFactory(
                            currentComponentFactory,
                            capturedValue,
                        ).also(capturedRecorder.built::add)
                    },
                    content = if (useOtherContent.value) otherContent else content,
                )
            }
        }
    }

    private fun runComposition(content: @Composable () -> Unit, update: () -> Unit) = runTest {
        val clock = BroadcastFrameClock()
        val recomposer = Recomposer(coroutineContext)
        launch(clock) { recomposer.runRecomposeAndApplyChanges() }
        runCurrent()
        val composition = Composition(UnitApplier(), recomposer)
        composition.setContent(content)

        update()
        Snapshot.sendApplyNotifications()
        repeat(2) {
            runCurrent()
            clock.sendFrame(0L)
        }
        runCurrent()

        composition.dispose()
        recomposer.cancel()
    }

    // Plain fields, not snapshot state, so a factory built inside derivedStateOf doesn't track them.
    @Stable
    private class Recorder {
        val built = mutableListOf<CapturingComponentFactory>()
        var provided: VideoComponentFactory? = null
    }

    private class CapturingComponentFactory(
        val delegate: VideoComponentFactory,
        val capturedValue: Int,
    ) : VideoComponentFactory by delegate

    private class UnitApplier : AbstractApplier<Unit>(Unit) {
        override fun insertTopDown(index: Int, instance: Unit) = Unit
        override fun insertBottomUp(index: Int, instance: Unit) = Unit
        override fun remove(index: Int, count: Int) = Unit
        override fun move(from: Int, to: Int, count: Int) = Unit
        override fun onClear() = Unit
    }
}
