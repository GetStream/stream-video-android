package io.getstream.video.android.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.getstream.android.push.firebase.FirebaseMessagingDelegate
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.datastore.delegate.StreamUserDataStore
import io.getstream.video.android.util.StreamVideoInitHelper
import kotlinx.coroutines.runBlocking


class DemoAppMessagingService: FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        if (!StreamVideo.isInstalled) {
            reloadSdk()
        }
        FirebaseMessagingDelegate.handleRemoteMessage(message)
    }
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if(!StreamVideo.isInstalled){
            reloadSdk()
        }
        FirebaseMessagingDelegate.registerFirebaseToken(token, "firebase")
    }

    private fun reloadSdk() {
        StreamUserDataStore.install(this, isEncrypted = true)

        // Demo helper for initialising the Video and Chat SDK instances from one place.
        // For simpler code we "inject" the Context manually instead of using DI.
        StreamVideoInitHelper.init(this)

        // Prepare the Video SDK if we already have a user logged in the demo app.
        // If you need to receive push messages (incoming call) then the SDK must be initialised
        // in Application.onCreate. Otherwise it doesn't know how to init itself when push arrives
        // and will ignore the push messages.
        // If push messages are not used then you don't need to init here - you can init
        // on-demand (initialising here is usually less error-prone).
        runBlocking {
            StreamVideoInitHelper.loadSdk(
                dataStore = StreamUserDataStore.instance(),
                useRandomUserAsFallback = false,
            )
        }
    }
}