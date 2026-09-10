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

package io.getstream.video.android.core.call

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import stream.video.sfu.models.TrackType
import java.util.concurrent.atomic.AtomicBoolean

class TrackKeyedJobsTest {

    @Test
    fun `launching a second track does not cancel the first track's job`() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job())
        val jobs = TrackKeyedJobs()
        val release = CompletableDeferred<Unit>()
        val audioDone = AtomicBoolean(false)
        val videoDone = AtomicBoolean(false)

        try {
            jobs.launch(scope, TrackType.TRACK_TYPE_AUDIO) {
                release.await()
                audioDone.set(true)
            }
            jobs.launch(scope, TrackType.TRACK_TYPE_VIDEO) {
                release.await()
                videoDone.set(true)
            }

            release.complete(Unit)

            assertThat(audioDone.get()).isTrue()
            assertThat(videoDone.get()).isTrue()
        } finally {
            scope.coroutineContext[Job]?.cancel()
        }
    }

    @Test
    fun `launching the same track cancels the previous job`() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job())
        val jobs = TrackKeyedJobs()
        val firstHold = CompletableDeferred<Unit>()
        val firstDone = AtomicBoolean(false)
        val secondDone = AtomicBoolean(false)

        try {
            jobs.launch(scope, TrackType.TRACK_TYPE_AUDIO) {
                firstHold.await()
                firstDone.set(true)
            }
            jobs.launch(scope, TrackType.TRACK_TYPE_AUDIO) {
                secondDone.set(true)
            }
            firstHold.complete(Unit)

            assertThat(firstDone.get()).isFalse()
            assertThat(secondDone.get()).isTrue()
        } finally {
            scope.coroutineContext[Job]?.cancel()
        }
    }

    @Test
    fun `cancelAll cancels every track job and clears the map`() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job())
        val jobs = TrackKeyedJobs()
        val audioHold = CompletableDeferred<Unit>()
        val videoHold = CompletableDeferred<Unit>()
        val audioDone = AtomicBoolean(false)
        val videoDone = AtomicBoolean(false)

        try {
            jobs.launch(scope, TrackType.TRACK_TYPE_AUDIO) {
                audioHold.await()
                audioDone.set(true)
            }
            jobs.launch(scope, TrackType.TRACK_TYPE_VIDEO) {
                videoHold.await()
                videoDone.set(true)
            }

            jobs.cancelAll()
            audioHold.complete(Unit)
            videoHold.complete(Unit)

            assertThat(audioDone.get()).isFalse()
            assertThat(videoDone.get()).isFalse()
        } finally {
            scope.coroutineContext[Job]?.cancel()
        }
    }
}
