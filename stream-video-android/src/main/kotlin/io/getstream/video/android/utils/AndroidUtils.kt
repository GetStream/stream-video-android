package io.getstream.video.android.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow

internal suspend fun Context.registerReceiverAsFlow(vararg actions: String): Flow<Intent> {
    return callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySendBlocking(intent)
            }
        }
        registerReceiver(receiver, IntentFilter().apply {
            actions.forEach {
                addAction(it)
            }
        })

        awaitClose {
            unregisterReceiver(receiver)
        }
    }.buffer(capacity = Channel.UNLIMITED)
}