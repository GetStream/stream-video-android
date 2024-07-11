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

package io.getstream.video.android.ui.common

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.annotation.CallSuper
import androidx.lifecycle.lifecycleScope
import io.getstream.log.taggedLogger
import io.getstream.result.Result
import io.getstream.result.flatMap
import io.getstream.result.onErrorSuspend
import io.getstream.result.onSuccessSuspend
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.EventSubscription
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.call.state.AcceptCall
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.CancelCall
import io.getstream.video.android.core.call.state.DeclineCall
import io.getstream.video.android.core.call.state.FlipCamera
import io.getstream.video.android.core.call.state.LeaveCall
import io.getstream.video.android.core.call.state.ToggleCamera
import io.getstream.video.android.core.call.state.ToggleMicrophone
import io.getstream.video.android.core.call.state.ToggleSpeakerphone
import io.getstream.video.android.core.events.ParticipantLeftEvent
import io.getstream.video.android.core.model.RejectReason
import io.getstream.video.android.core.notifications.NotificationHandler
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.model.streamCallId
import io.getstream.video.android.ui.common.util.StreamCallActivityDelicateApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.openapitools.client.models.CallEndedEvent
import org.openapitools.client.models.CallSessionParticipantLeftEvent
import org.openapitools.client.models.OwnCapability
import org.openapitools.client.models.VideoEvent

@OptIn(StreamCallActivityDelicateApi::class)
public abstract class StreamCallActivity : ComponentActivity() {
    // Factory and creation
    public companion object {
        // Extra keys
        private const val EXTRA_LEAVE_WHEN_LAST: String = "leave_when_last"
        private const val EXTRA_MEMBERS_ARRAY: String = "members_extra"

        // Extra default values
        private const val DEFAULT_LEAVE_WHEN_LAST: Boolean = true
        private val defaultExtraMembers = emptyList<String>()
        private val logger by taggedLogger("DefaultCallActivity")

        /**
         * Factory method used to build an intent to start this activity.
         *
         * @param context the context.
         * @param cid the Call id
         * @param members list of members
         * @param action android action.
         * @param clazz the class of the Activity
         * @param configuration the configuration object
         * @param extraData extra data to pass to the activity
         */
        public fun <T : StreamCallActivity> callIntent(
            context: Context,
            cid: StreamCallId,
            members: List<String> = defaultExtraMembers,
            leaveWhenLastInCall: Boolean = DEFAULT_LEAVE_WHEN_LAST,
            action: String? = null,
            clazz: Class<T>,
            configuration: StreamCallActivityConfiguration = StreamCallActivityConfiguration(),
            extraData: Bundle? = null,
        ): Intent {
            return Intent(context, clazz).apply {
                // Setup the outgoing call action
                action?.let {
                    this.action = it
                }
                val config = configuration.toBundle()
                // Add the config
                putExtra(StreamCallActivityConfigStrings.EXTRA_STREAM_CONFIG, config)
                // Add the generated call ID and other params
                putExtra(NotificationHandler.INTENT_EXTRA_CALL_CID, cid)
                putExtra(EXTRA_LEAVE_WHEN_LAST, leaveWhenLastInCall)
                // Setup the members to transfer to the new activity
                val membersArrayList = ArrayList<String>()
                members.forEach { membersArrayList.add(it) }
                putStringArrayListExtra(EXTRA_MEMBERS_ARRAY, membersArrayList)
                extraData?.also {
                    putExtras(it)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                logger.d { "Created [${clazz.simpleName}] intent. -> $this" }
            }
        }
    }

    // Internal state
    private var callEventSubscription: EventSubscription? = null
    private var callSocketConnectionMonitor: Job? = null
    private lateinit var cachedCall: Call
    private lateinit var config: StreamCallActivityConfiguration
    protected val onSuccessFinish: suspend (Call) -> Unit = { call ->
        logger.w { "The call was successfully finished! Closing activity" }
        onEnded(call)
        if (configuration.closeScreenOnCallEnded) {
            finish()
        }
    }
    protected val onErrorFinish: suspend (Exception) -> Unit = { error ->
        logger.e(error) { "Something went wrong, finishing the activity!" }
        onFailed(error)
        if (configuration.closeScreenOnError) {
            finish()
        }
    }

    // Public values
    /**
     * Each activity needs its onw ui delegate that will set content to the activity.
     *
     * Any extending activity must provide its own ui delegate that manages the UI.
     */
    public abstract val uiDelegate: StreamActivityUiDelegate<StreamCallActivity>

    /**
     * A configuration object returned for this activity controlling various behaviors of the activity.
     * Can be overridden for further control on the behavior.
     *
     * This is delicate API because it loads the config from the extra, you can pass the
     * configuration object in the [callIntent] method and it will be correctly passed here.
     * You can override it and return a custom configuration all the time, in which case
     * the configuration passed in [callIntent] is ignored.
     */
    @StreamCallActivityDelicateApi
    public open val configuration: StreamCallActivityConfiguration
        get() {
            if (!::config.isInitialized) {
                try {
                    val bundledConfig =
                        intent.getBundleExtra(StreamCallActivityConfigStrings.EXTRA_STREAM_CONFIG)
                    config =
                        bundledConfig?.extractStreamActivityConfig()
                            ?: StreamCallActivityConfiguration()
                } catch (e: Exception) {
                    config = StreamCallActivityConfiguration()
                    logger.e(e) { "Failed to load config using default!" }
                }
            }
            return config
        }

    // Platform restriction
    public final override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onPreCreate(savedInstanceState, null)
        logger.d { "Entered [onCreate(Bundle?)" }
        initializeCallOrFail(
            savedInstanceState,
            null,
            onSuccess = { instanceState, persistentState, call, action ->
                logger.d { "Calling [onCreate(Call)], because call is initialized $call" }
                onIntentAction(call, action, onError = onErrorFinish) { successCall ->
                    onCreate(instanceState, persistentState, successCall)
                }
            },
            onError = {
                // We are not calling onErrorFinish here on purpose
                // we want to crash if we cannot initialize the call
                logger.e(it) { "Failed to initialize call." }
                throw it
            },
        )
    }

    public final override fun onCreate(
        savedInstanceState: Bundle?,
        persistentState: PersistableBundle?,
    ) {
        super.onCreate(savedInstanceState)
        onPreCreate(savedInstanceState, persistentState)
        logger.d { "Entered [onCreate(Bundle, PersistableBundle?)" }
        initializeCallOrFail(
            savedInstanceState,
            persistentState,
            onSuccess = { instanceState, persistedState, call, action ->
                logger.d { "Calling [onCreate(Call)], because call is initialized $call" }
                onIntentAction(call, action, onError = onErrorFinish) { successCall ->
                    onCreate(instanceState, persistedState, successCall)
                }
            },
            onError = {
                // We are not calling onErrorFinish here on purpose
                // we want to crash if we cannot initialize the call
                logger.e(it) { "Failed to initialize call." }
                throw it
            },
        )
    }

    public final override fun onResume() {
        super.onResume()
        withCachedCall {
            onResume(it)
        }
    }

    public final override fun onPause() {
        withCachedCall {
            onPause(it)
            super.onPause()
        }
    }

    public final override fun onStop() {
        withCachedCall {
            onStop(it)
            super.onStop()
        }
    }

    // Lifecycle methods

    /**
     * Handles action from the intent.
     *
     * @param call the call
     * @param action the action.
     *
     * @see NotificationHandler
     */
    public open fun onIntentAction(
        call: Call,
        action: String?,
        onError: (suspend (Exception) -> Unit)? = onErrorFinish,
        onSuccess: (suspend (Call) -> Unit)? = null,
    ) {
        logger.d { "[onIntentAction] #ringing; action: $action, call.cid: ${call.cid}" }
        when (action) {
            NotificationHandler.ACTION_ACCEPT_CALL -> {
                logger.v { "[onIntentAction] #ringing; Action ACCEPT_CALL, ${call.cid}" }
                accept(call, onError = onError, onSuccess = onSuccess)
            }

            NotificationHandler.ACTION_REJECT_CALL -> {
                logger.v { "[onIntentAction] #ringing; Action REJECT_CALL, ${call.cid}" }
                reject(call, onError = onError, onSuccess = onSuccess)
            }

            NotificationHandler.ACTION_INCOMING_CALL -> {
                logger.v { "[onIntentAction] #ringing; Action INCOMING_CALL, ${call.cid}" }
                get(call, onError = onError, onSuccess = onSuccess)
            }

            NotificationHandler.ACTION_OUTGOING_CALL -> {
                logger.v { "[onIntentAction] #ringing; Action OUTGOING_CALL, ${call.cid}" }
                // Extract the members and the call ID and place the outgoing call
                val members = intent.getStringArrayListExtra(EXTRA_MEMBERS_ARRAY) ?: emptyList()
                create(
                    call,
                    members = members,
                    ring = true,
                    onSuccess = onSuccess,
                    onError = onError,
                )
            }

            else -> {
                logger.w {
                    "[onIntentAction] #ringing; No action provided to the intent will try to join call by default [action: $action], [cid: ${call.cid}]"
                }
                val members = intent.getStringArrayListExtra(EXTRA_MEMBERS_ARRAY) ?: emptyList()
                // If the call does not exist it will be created.
                create(
                    call,
                    members = members,
                    ring = false,
                    onSuccess = {
                        join(call, onError = onError)
                        onSuccess?.invoke(call)
                    },
                    onError = onError,
                )
            }
        }
    }

    /**
     * Called when the activity is created, but the SDK is not yet initialized.
     * Initializes the configuration and UI delegates.
     */
    @CallSuper
    @StreamCallActivityDelicateApi
    public open fun onPreCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        logger.d { "Pre-create" }
        val config = configuration // Called before the delegate
        logger.d { "Activity pre-created with configuration [$config]" }
        uiDelegate.loadingContent(this)
    }

    /**
     * Same as [onCreate] but with the [Call] as parameter.
     * The [onCreate] method will crash with [IllegalArgumentException] if the call cannot be loaded
     * or if the intent was created incorrectly, thus arriving in this method means that the call
     * is already available.
     */
    public open fun onCreate(
        savedInstanceState: Bundle?,
        persistentState: PersistableBundle?,
        call: Call,
    ) {
        logger.d { "[onCreate(Bundle,PersistableBundle,Call)] setting up compose delegate." }
        uiDelegate.setContent(this, call)
    }

    /**
     * Called when the activity is resumed. Makes sure the call object is available.
     *
     * @param call
     */
    public open fun onResume(call: Call) {
        // No - op
        logger.d { "DefaultCallActivity - Resumed (call -> $call)" }
    }

    /**
     * Called when the activity is paused. Makes sure the call object is available.
     *
     * @param call the call
     */
    public open fun onPause(call: Call) {
        if (isVideoCall(call) && !isInPictureInPictureMode) {
            enterPictureInPicture()
        }
        logger.d { "DefaultCallActivity - Paused (call -> $call)" }
    }

    /**
     * Called when the activity has failed for any reason.
     * The activity will finish after this call, to prevent the `finish()` provide a different [StreamCallActivityConfiguration].
     */
    public open fun onFailed(exception: Exception) {
        // No - op
    }

    /**
     * Called when call has ended successfully, either by action from the user or
     * backend event. The activity will finish after this call.
     * To prevent it, provide a different [StreamCallActivityConfiguration]
     */
    public open fun onEnded(call: Call) {
        // No-op
    }

    /**
     * Called when the activity is stopped. Makes sure the call object is available.
     * Will leave the call if [onStop] is called while in Picture-in-picture mode.
     * @param call the call
     */
    public open fun onStop(call: Call) {
        logger.d { "Default activity - stopped (call -> $call)" }
        if (isVideoCall(call) && !isInPictureInPictureMode) {
            logger.d { "Default activity - stopped: No PiP detected, will leave call. (call -> $call)" }
            leave(call) // Already finishing
        }
    }

    /**
     * Invoked when back button is pressed. Will leave the call and finish the activity.
     * Override to change this behavior
     *
     * @param call the call.
     */
    public open fun onBackPressed(call: Call) {
        leave(call, onSuccessFinish, onErrorFinish)
    }

    // Decision making
    @StreamCallActivityDelicateApi
    public open fun isVideoCall(call: Call): Boolean =
        call.hasCapability(OwnCapability.SendVideo)

    // Picture in picture (for Video calls)
    /**
     * Enter picture in picture mode. By default supported for video calls.
     */
    @StreamCallActivityDelicateApi
    public fun enterPictureInPicture(): Unit = withCachedCall { call ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val currentOrientation = resources.configuration.orientation
            val screenSharing = call.state.screenSharingSession.value

            val aspect =
                if (currentOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT && (screenSharing == null || screenSharing.participant.isLocal)) {
                    Rational(9, 16)
                } else {
                    Rational(16, 9)
                }

            enterPictureInPictureMode(
                PictureInPictureParams.Builder().setAspectRatio(aspect).apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        this.setAutoEnterEnabled(true)
                    }
                }.build(),
            )
        } else {
            @Suppress("DEPRECATION")
            enterPictureInPictureMode()
        }
    }

    // Call API
    /**
     * Get a call instance with the members list and call ID.
     *
     * Note: Callbacks are posted on [Dispatchers.Main] dispatcher.
     *
     * @param cid the call ID
     * @param members the call members
     * @param onSuccess callback where the [Call] object is returned
     * @param onError callback when the [Call] was not returned.
     */
    @StreamCallActivityDelicateApi
    public open fun call(
        cid: StreamCallId,
        onSuccess: ((Call) -> Unit)? = null,
        onError: ((Exception) -> Unit)? = null,
    ) {
        val sdkInstance = StreamVideo.instance()
        val call = sdkInstance.call(cid.type, cid.id)
        onSuccess?.invoke(call)
    }

    /**
     * Create (or get) the call.
     * @param call the call object
     * @param ring if the call should ring for other participants (sends push)
     * @param members members of the call
     * @param onSuccess invoked when operation is sucessfull
     * @param onError invoked when operation failed.
     */
    @StreamCallActivityDelicateApi
    public open fun create(
        call: Call,
        ring: Boolean,
        members: List<String>,
        onSuccess: (suspend (Call) -> Unit)? = null,
        onError: (suspend (Exception) -> Unit)? = null,
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            val instance = StreamVideo.instance()
            val result = call.create(
                // List of all users, containing the caller also
                memberIds = members + instance.userId,
                // If other users will get push notification.
                ring = ring,
            )
            result.onOutcome(call, onSuccess, onError)
        }
    }

    /**
     * Get a call. Used in cases like "incoming call" where you are sure that the call is already created.
     *
     * @param call the call
     * @param onSuccess invoked when operation is sucessfull
     * @param onError invoked when operation failed.
     */
    @StreamCallActivityDelicateApi
    public open fun get(
        call: Call,
        onSuccess: (suspend (Call) -> Unit)? = null,
        onError: (suspend (Exception) -> Unit)? = null,
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            val result = call.get()
            result.onOutcome(call, onSuccess, onError)
        }
    }

    /**
     * Accept an incoming call.
     *
     * @param call the call to accept.
     * @param onSuccess invoked when the [Call.join] has finished.
     * @param onError invoked when operation failed.
     * */
    @StreamCallActivityDelicateApi
    public open fun join(
        call: Call,
        onSuccess: (suspend (Call) -> Unit)? = null,
        onError: (suspend (Exception) -> Unit)? = null,
    ) {
        acceptOrJoinNewCall(call, onSuccess, onError) {
            logger.d { "Join call, ${call.cid}" }
            it.join()
        }
    }

    /**
     * Accept an incoming call.
     *
     * @param call the call to accept.
     * @param onSuccess invoked when the [Call.join] has finished.
     * @param onError invoked when operation failed.
     * */
    @StreamCallActivityDelicateApi
    public open fun accept(
        call: Call,
        onSuccess: (suspend (Call) -> Unit)? = null,
        onError: (suspend (Exception) -> Unit)? = null,
    ) {
        logger.d { "[accept] #ringing; call.cid: ${call.cid}" }
        acceptOrJoinNewCall(call, onSuccess, onError) {
            call.acceptThenJoin()
        }
    }

    /**
     * Reject the call in the parameter.
     *
     * Invokes [Call.reject] then [Call.leave]. Optionally callbacks are invoked.
     *
     * @param call the call to reject
     * @param onSuccess optionally get notified if the operation was success.
     * @param onError optionally get notified if the operation failed.
     */
    @StreamCallActivityDelicateApi
    public open fun reject(
        call: Call,
        reason: RejectReason? = null,
        onSuccess: (suspend (Call) -> Unit)? = null,
        onError: (suspend (Exception) -> Unit)? = null,
    ) {
        logger.d { "[reject] #ringing; rejectReason: $reason, call.cid: ${call.cid}" }
        lifecycleScope.launch(Dispatchers.IO) {
            val result = call.reject(reason)
            result.onOutcome(call, onSuccess, onError)
            // Leave regardless of outcome
            call.leave()
        }
    }

    /**
     * Cancel an outgoing call if any.
     * Same as [reject] but can be overridden for different behavior on Outgoing calls.
     *
     * @param call the [Call] to cancel.
     * @param onSuccess optionally get notified if the operation was success.
     * @param onError optionally get notified if the operation failed.
     */
    @StreamCallActivityDelicateApi
    public open fun cancel(
        call: Call,
        onSuccess: (suspend (Call) -> Unit)? = null,
        onError: (suspend (Exception) -> Unit)? = null,
    ) {
        logger.d { "[cancel] #ringing; call.cid: ${call.cid}" }
        reject(call, RejectReason.Cancel, onSuccess, onError)
    }

    /**
     * Leave the call from the parameter.
     *
     * @param call the call object.
     * @param onSuccess optionally get notified if the operation was success.
     * @param onError optionally get notified if the operation failed.
     */
    @StreamCallActivityDelicateApi
    public open fun leave(
        call: Call,
        onSuccess: (suspend (Call) -> Unit)? = null,
        onError: (suspend (Exception) -> Unit)? = null,
    ) {
        logger.d { "Leave call, ${call.cid}" }
        lifecycleScope.launch(Dispatchers.IO) {
            // Will quietly leave the call, leaving it intact for the other participants.
            try {
                call.leave()
                onSuccess?.invoke(call)
            } catch (e: Exception) {
                onError?.invoke(e)
            }
        }
    }

    /**
     * End the call.
     *
     * @param call the call
     * @param onSuccess optionally get notified if the operation was success.
     * @param onError optionally get notified if the operation failed.
     */
    @StreamCallActivityDelicateApi
    public open fun end(
        call: Call,
        onSuccess: (suspend (Call) -> Unit)? = null,
        onError: (suspend (Exception) -> Unit)? = null,
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            val result = call.end()
            result.onOutcome(call, { leave(call, onSuccess, onError) }, onError)
        }
    }

    // Event and action callbacks

    /**
     * Called when there was an UI action invoked for the call.
     *
     * @param call the call
     * @param action the action.
     */
    @CallSuper
    public open fun onCallAction(call: Call, action: CallAction) {
        logger.i { "[onCallAction] #ringing; action: $action, call.cid: ${call.cid}" }
        when (action) {
            is LeaveCall -> {
                leave(call, onSuccessFinish, onErrorFinish)
            }

            is DeclineCall -> {
                reject(call, RejectReason.Decline, onSuccessFinish, onErrorFinish)
            }

            is CancelCall -> {
                cancel(call, onSuccessFinish, onErrorFinish)
            }

            is AcceptCall -> {
                accept(call, onError = onErrorFinish)
            }

            is ToggleCamera -> {
                call.camera.setEnabled(action.isEnabled)
            }

            is ToggleMicrophone -> {
                call.microphone.setEnabled(action.isEnabled)
            }

            is ToggleSpeakerphone -> {
                call.speaker.setEnabled(action.isEnabled)
            }

            is FlipCamera -> {
                call.camera.flip()
            }

            else -> Unit
        }
    }

    /**
     * Call events handler.
     *
     * @param call the call.
     * @param event the event.
     */
    @CallSuper
    public open fun onCallEvent(call: Call, event: VideoEvent) {
        when (event) {
            is CallEndedEvent -> {
                // In any case finish the activity, the call is done for
                leave(call, onSuccess = onSuccessFinish, onError = onErrorFinish)
            }

            is ParticipantLeftEvent, is CallSessionParticipantLeftEvent -> {
                val total = call.state.participantCounts.value?.total
                logger.d { "Participant left, remaining: $total" }
                if (total != null && total <= 2) {
                    onLastParticipant(call)
                }
            }
        }
    }

    /**
     * Called when current device has become the last participant in the call.
     * Invokes [LeaveCall] action for audio calls.
     *
     * @param call the call.
     */
    @CallSuper
    public open fun onLastParticipant(call: Call) {
        logger.d { "You are the last participant." }
        val leaveWhenLastInCall =
            intent.getBooleanExtra(EXTRA_LEAVE_WHEN_LAST, DEFAULT_LEAVE_WHEN_LAST)
        if (leaveWhenLastInCall) {
            onCallAction(call, LeaveCall)
        }
    }

    /**
     * Get notified when the activity enters picture in picture.
     *
     */
    @CallSuper
    public open fun onPictureInPicture(call: Call) {
        // No-op by default
    }

    /**
     * Called when there has been a new event from the socket.
     *
     * @param call the call
     * @param state the state
     */
    @CallSuper
    @StreamCallActivityDelicateApi
    public open fun onConnectionEvent(call: Call, state: RealtimeConnection) {
        when (state) {
            RealtimeConnection.Disconnected -> {
                lifecycleScope.launch {
                    onSuccessFinish.invoke(call)
                }
            }
            is RealtimeConnection.Failed -> {
                lifecycleScope.launch {
                    val conn = state as? RealtimeConnection.Failed
                    val throwable = Exception("${conn?.error}")
                    logger.e(throwable) { "Call connection failed." }
                    onErrorFinish.invoke(throwable)
                }
            }
            else -> {
                // No-op
            }
        }
    }

    // Internal logic
    private fun initializeCallOrFail(
        savedInstanceState: Bundle?,
        persistentState: PersistableBundle?,
        onSuccess: ((Bundle?, PersistableBundle?, Call, action: String?) -> Unit)? = null,
        onError: ((Exception) -> Unit)? = null,
    ) {
        // Have a call ID or crash
        val cid = intent.streamCallId(NotificationHandler.INTENT_EXTRA_CALL_CID)

        if (cid == null) {
            val e = IllegalArgumentException("CallActivity started without call ID.")
            logger.e(
                e,
            ) { "Failed to initialize call because call ID is not found in the intent. $intent" }
            onError?.let {
            } ?: throw e
            // Finish
            return
        }

        call(
            cid,
            onSuccess = { call ->
                cachedCall = call
                callEventSubscription?.dispose()
                callEventSubscription = cachedCall.subscribe { event ->
                    onCallEvent(cachedCall, event)
                }
                callSocketConnectionMonitor = lifecycleScope.launch(Dispatchers.IO) {
                    cachedCall.state.connection.collectLatest {
                        onConnectionEvent(call, it)
                    }
                }
                onSuccess?.invoke(
                    savedInstanceState,
                    persistentState,
                    cachedCall,
                    intent.action,
                )
            },
            onError = onError,
        )
    }

    private fun withCachedCall(action: (Call) -> Unit) {
        if (!::cachedCall.isInitialized) {
            initializeCallOrFail(null, null, onSuccess = { _, _, call, _ ->
                action(call)
            }, onError = {
                // Call is missing, we need to crash, no other way
                throw it
            })
        } else {
            action(cachedCall)
        }
    }

    private fun acceptOrJoinNewCall(
        call: Call,
        onSuccess: (suspend (Call) -> Unit)?,
        onError: (suspend (Exception) -> Unit)?,
        what: suspend (Call) -> Result<RtcSession>,
    ) {
        logger.d { "Accept or join, ${call.cid}" }
        lifecycleScope.launch(Dispatchers.IO) {
            // Since its an incoming call, we can safely assume it is already created
            // no need to set create = true, or ring = true or anything.
            // Accept then join.
            // Check if we are already in a call
            val activeCall = StreamVideo.instance().state.activeCall.value
            if (activeCall != null) {
                if (activeCall.id != call.id) {
                    // If the call id is different leave the previous call
                    activeCall.leave()
                    logger.d { "Leave active call, ${call.cid}" }
                    // Join the call, only if accept succeeds
                    val result = what(call)
                    result.onOutcome(call, onSuccess, onError)
                } else {
                    // Already accepted and joined
                    logger.d { "Already joined, ${call.cid}" }
                    withContext(Dispatchers.Main) {
                        onSuccess?.invoke(call)
                    }
                }
            } else {
                // No active call, safe to join
                val result = what(call)
                result.onOutcome(call, onSuccess, onError)
            }
        }
    }

    private suspend fun <A : Any> Result<A>.onOutcome(
        call: Call,
        onSuccess: (suspend (Call) -> Unit)? = null,
        onError: (suspend (Exception) -> Unit)? = null,
    ) = withContext(Dispatchers.Main) {
        onSuccess?.let {
            onSuccessSuspend {
                onSuccess(call)
            }
        }
        onError?.let {
            onErrorSuspend {
                onError(Exception(it.message))
            }
        }
    }

    private suspend fun Call.acceptThenJoin() =
        withContext(Dispatchers.IO) { accept().flatMap { join() } }
}
