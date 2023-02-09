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

package io.getstream.video.android.core.socket.internal

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.reflect.KClass

/**
 * This class represents a Finite State Machine. It can be only in one possible state at a time
 * out of the set of possible states [S]. It can handle events from the set [E].
 *
 * @param initialState The initial state.
 * @property stateFunctions A map of states and possible event handlers for them.
 * @property defaultEventHandler Called when [stateFunctions] has no handler for
 *                               a given state/event combination.
 */
internal class FiniteStateMachine<S : Any, E : Any>(
    initialState: S,
    private val stateFunctions: Map<KClass<out S>, Map<KClass<out E>, StateFunction<S, E>>>,
    private val enterListeners: MutableMap<KClass<out S>, List<(S, E) -> Unit>>,
    private val defaultEventHandler: (S, E) -> S,
) {
    private val mutex = Mutex()
    private var _state: S = initialState

    private suspend inline fun <T> Mutex.withLockIfNotLocked(action: () -> T): T {
        return if (isLocked.not()) {
            withLock { action() }
        } else {
            action()
        }
    }

    /**
     * The current state.
     */
    val state: S
        get() = runBlocking {
            mutex.withLockIfNotLocked { _state }
        }

    /**
     * Sends an event to the state machine. The entry point to change state.
     */
    fun sendEvent(event: E) {
        runBlocking {
            val shouldNotify = mutex.withLock {
                val oldState = _state
                val functions = stateFunctions[oldState::class]
                val handler = functions?.getHandler(event) ?: defaultEventHandler
                _state = handler(oldState, event)
                _state != oldState
            }
            if (shouldNotify) {
                with(_state) {
                    notifyOnEnter(event)
                }
            }
        }
    }

    private fun Map<KClass<out E>, (S, E) -> S>.getHandler(event: E): (S, E) -> S {
        val handler = this[event::class]
        if (handler != null) {
            return handler
        }

        var eventHandler = defaultEventHandler
        for ((clazz: KClass<out E>, evHandler: (S, E) -> S) in this) {
            if (clazz.isInstance(event)) {
                eventHandler = evHandler
                break
            }
        }
        return eventHandler
    }

    /**
     * Keeps the FSM in its current state.
     * Usually used when handling events that don't need to make a transition.
     *
     * ```kotlin
     * onEvent<SomeEvent> { state, event -> stay() }
     * ```
     *
     * @return the current state value
     */
    fun stay(): S = state

    companion object {
        @FSMBuilderMarker
        public operator fun <S : Any, E : Any> invoke(builder: FSMBuilder<S, E>.() -> Unit): FiniteStateMachine<S, E> {
            return FSMBuilder<S, E>().apply(builder).build()
        }
    }

    private fun S.notifyOnEnter(event: E) {
        enterListeners[this::class]?.forEach { it(this, event) }
    }
}

internal class FSMBuilder<STATE : Any, EVENT : Any> {
    private lateinit var _initialState: STATE
    val stateFunctions: MutableMap<KClass<out STATE>, Map<KClass<out EVENT>, StateFunction<STATE, EVENT>>> =
        mutableMapOf()
    val enterListeners: MutableMap<KClass<out STATE>, List<(STATE, EVENT) -> Unit>> = mutableMapOf()

    private var _defaultHandler: (STATE, EVENT) -> STATE = { s, _ -> s }

    @FSMBuilderMarker
    fun initialState(state: STATE) {
        _initialState = state
    }

    @FSMBuilderMarker
    fun defaultHandler(defaultHandler: (STATE, EVENT) -> STATE) {
        _defaultHandler = defaultHandler
    }

    @FSMBuilderMarker
    inline fun <reified S : STATE> state(stateHandlerBuilder: StateHandlerBuilder<STATE, EVENT, S>.() -> Unit) {
        val newBuilder = StateHandlerBuilder<STATE, EVENT, S>().apply(stateHandlerBuilder)
        stateFunctions[S::class] = newBuilder.get()
        enterListeners[S::class] = newBuilder.getEnterListeners()
    }

    internal fun build(): FiniteStateMachine<STATE, EVENT> {
        check(this::_initialState.isInitialized) { "Initial state must be set!" }
        return FiniteStateMachine(_initialState, stateFunctions, enterListeners, _defaultHandler)
    }
}

@DslMarker
internal annotation class FSMBuilderMarker

internal typealias StateFunction<S, E> = (S, E) -> S

@FSMBuilderMarker
internal class StateHandlerBuilder<STATE : Any, EVENT : Any, S : STATE> {

    @PublishedApi
    internal val eventHandlers: MutableMap<KClass<out EVENT>, StateFunction<STATE, EVENT>> = mutableMapOf()

    @PublishedApi
    internal val onEnterListeners: MutableList<(STATE, EVENT) -> Unit> = mutableListOf()

    @FSMBuilderMarker
    inline fun <reified E : EVENT> onEvent(noinline func: S.(E) -> STATE) {
        @Suppress("UNCHECKED_CAST")
        eventHandlers[E::class] = func as (STATE, EVENT) -> STATE
    }

    @FSMBuilderMarker
    inline fun onEnter(crossinline listener: S.(EVENT) -> Unit) {
        onEnterListeners.add { state, cause ->
            @Suppress("UNCHECKED_CAST")
            listener(state as S, cause)
        }
    }

    @PublishedApi
    internal fun get(): Map<KClass<out EVENT>, StateFunction<STATE, EVENT>> = eventHandlers

    @PublishedApi
    internal fun getEnterListeners(): MutableList<(STATE, EVENT) -> Unit> = onEnterListeners
}
