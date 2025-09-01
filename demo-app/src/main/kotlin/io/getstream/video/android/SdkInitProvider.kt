package io.getstream.video.android

import android.util.Log
import io.getstream.video.android.contentprovider.BaseContentProvider
import io.getstream.video.android.datastore.delegate.StreamUserDataStore
import io.getstream.video.android.util.StreamVideoInitHelper
import kotlinx.coroutines.runBlocking

class SdkInitProvider : BaseContentProvider() {

    private val TAG = "SdkInitProvider"

    override fun onCreate(): Boolean {
        Log.d(TAG, "onCreate")
        // We use the provided StreamUserDataStore in the demo app for user data storage.
        // This is a convenience class provided for storage but the SDK itself is not aware of
        // this instance and doesn't use it. You can use it to store the logged in user and then
        // retrieve the information for SDK initialisation.
        val ctx = context

        if (ctx != null) {
            StreamUserDataStore.install(ctx, isEncrypted = true)

            // Demo helper for initialising the Video and Chat SDK instances from one place.
            // For simpler code we "inject" the Context manually instead of using DI.
            StreamVideoInitHelper.init(ctx)

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
            Log.d(TAG, "Successfully load sdk")
            return true
        } else {
            Log.d(TAG, "Failed to load sdk")
            return false
        }

    }


}