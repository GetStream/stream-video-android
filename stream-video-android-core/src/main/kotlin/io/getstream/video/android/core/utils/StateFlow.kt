package io.getstream.video.android.core.utils

import io.getstream.video.android.core.dispatchers.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

// TODO: review how the scope interacts with the viewmodel
// https://proandroiddev.com/clean-stateflow-transformations-in-kotlin-608f4c7de5ab
// probably makes sense to pass down a call scope and cancel that all at once

fun <T, K> StateFlow<T>.mapState(
    scope: CoroutineScope = CoroutineScope(context = DispatcherProvider.IO),
    transform: (data: T) -> K
): StateFlow<K> {
    return mapLatest {
        transform(it)
    }
        .stateIn(scope, SharingStarted.Eagerly, transform(value))
}

fun <T, K> StateFlow<T>.mapState(
    scope: CoroutineScope,
    initialValue: K,
    transform: suspend (data: T) -> K
): StateFlow<K> {
    return mapLatest {
        transform(it)
    }
        .stateIn(scope, SharingStarted.Eagerly, initialValue)
}