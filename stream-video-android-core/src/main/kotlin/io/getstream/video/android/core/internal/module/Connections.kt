package io.getstream.video.android.core.internal.module

import io.getstream.video.android.core.socket.common.scope.ClientScope
import io.getstream.video.android.core.socket.common.scope.UserScope
import kotlinx.coroutines.CoroutineScope
import okhttp3.OkHttpClient

interface ConnectionModuleDeclaration<Api, SocketConnection, Http: OkHttpClient> {
    val api: Api
    val http: Http
    val scope: CoroutineScope get() {
        return UserScope(ClientScope())
    }
}