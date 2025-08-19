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

package io.getstream.video.android.core.ringingstatetransition

import io.getstream.video.android.core.RingingState

internal open class ProcessingResult<State, Event>

internal sealed class ReducerOutput1<OriginalState, Event, Output>() :
    ProcessingResult<OriginalState, Event>() {

    class NoChange<OriginalState, Event, Output>(
        val value: OriginalState,
    ) : ReducerOutput1<OriginalState, Event, Output>()

    class Change<OriginalState, Event, Output>(
        val value: Output,
    ) : ReducerOutput1<OriginalState, Event, Output>()

    fun getOutput() = when (this) {
        is NoChange -> value
        is Change -> value
    }
}

internal sealed class ReducerOutput2<OriginalState, Event, Output1, Output2> :
    ProcessingResult<OriginalState, Event>() {

    class NoChange<OriginalState, Event, Output1, Output2>(
        val value: OriginalState,
    ) : ReducerOutput2<OriginalState, Event, Output1, Output2>()

    class FirstOption<OriginalState, Event, Output1, Output2>(
        val value: Output1,
    ) : ReducerOutput2<OriginalState, Event, Output1, Output2>()

    class SecondOption<OriginalState, Event, Output1, Output2>(
        val value: Output2,
    ) : ReducerOutput2<OriginalState, Event, Output1, Output2>()

    fun getOutput() = when (this) {
        is NoChange -> value
        is FirstOption -> value
        is SecondOption -> value
    }
}

internal sealed class ReducerOutput3<OriginalState, Event, Output1, Output2, Output3> :
    ProcessingResult<OriginalState, Event>() {

    class NoChange<OriginalState, Event, Output1, Output2, Output3>(
        val value: OriginalState,
    ) : ReducerOutput3<OriginalState, Event, Output1, Output2, Output3>()

    class FirstOption<OriginalState, Event, Output1, Output2, Output3>(
        val value: Output1,
    ) : ReducerOutput3<OriginalState, Event, Output1, Output2, Output3>()

    class SecondOption<OriginalState, Event, Output1, Output2, Output3>(
        val value: Output2,
    ) : ReducerOutput3<OriginalState, Event, Output1, Output2, Output3>()

    class ThirdOption<OriginalState, Event, Output1, Output2, Output3>(
        val value: Output3,
    ) : ReducerOutput3<OriginalState, Event, Output1, Output2, Output3>()

    fun getOutput() = when (this) {
        is NoChange -> value
        is FirstOption -> value
        is SecondOption -> value
        is ThirdOption -> value
    }
}

internal typealias RingingStateReducerOutput1<Event, Output> =
    ReducerOutput1<RingingState, Event, Output>

internal typealias RingingStateReducerOutput2<Event, Output1, Output2> =
    ReducerOutput2<RingingState, Event, Output1, Output2>

internal typealias RingingStateReducerOutput3<Event, Output1, Output2, Output3> =
    ReducerOutput3<RingingState, Event, Output1, Output2, Output3>

// internal sealed class RingingStateReducerOutput1<Event, Output>() :
//    ProcessingResult<RingingState, Event>() {
//
//    class Input<Event, Output>(
//        val value: RingingState
//    ) : RingingStateReducerOutput1<Event, Output>()
//
//    class First<Event, Output>(
//        val value: Output
//    ) : RingingStateReducerOutput1<Event, Output>()
//
//    fun getOutput() = when (this) {
//        is Input -> value
//        is First -> value
//    }
// }

// internal fun sample() {
//    TransformedOutput3.First<RingingState.Active, CallAcceptedEvent, RingingState.Active, RingingState.Idle, RingingState.Active>(
//        RingingState.Active
//    )
//    TransformedOutput3.Second<RingingState.Active, CallAcceptedEvent, RingingState.Active, RingingState.Idle, RingingState.Active>(
//        RingingState.Idle
//    )
//    TransformedOutput3.Third<RingingState.Active, CallAcceptedEvent, RingingState.Active, RingingState.Idle, RingingState.Active>(
//        RingingState.Active
//    )
// }

internal interface StateReducer1<OriginalState, Event, Output> {
    fun reduce(
        originalState: OriginalState,
        event: Event,
    ): ReducerOutput1<OriginalState, Event, Output>
}

internal interface StateReducer2<OriginalState, Event, Output1, Output2> {
    fun reduce(
        originalState: OriginalState,
        event: Event,
    ): ReducerOutput2<OriginalState, Event, Output1, Output2>
}

internal interface StateReducer3<OriginalState, Event, Output1, Output2, Output3> {
    fun reduce(
        originalState: OriginalState,
        event: Event,
    ): ReducerOutput3<OriginalState, Event, Output1, Output2, Output3>
}

internal interface RingingStateReducer1<Event, Output> : StateReducer1<RingingState, Event, Output>

internal interface RingingStateReducer2<Event, Output1, Output2> :
    StateReducer2<RingingState, Event, Output1, Output2>

internal interface RingingStateReducer3<Event, Output1, Output2, Output3> :
    StateReducer3<RingingState, Event, Output1, Output2, Output3>
