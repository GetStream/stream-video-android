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
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Rational
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.annotation.CallSuper
import androidx.lifecycle.lifecycleScope
import io.getstream.android.video.generated.models.CallEndedEvent
import io.getstream.android.video.generated.models.CallSessionEndedEvent
import io.getstream.android.video.generated.models.OwnCapability
import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.log.taggedLogger
import io.getstream.result.Result
import io.getstream.result.extractCause
import io.getstream.result.flatMap
import io.getstream.result.onErrorSuspend
import io.getstream.result.onSuccessSuspend
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.DeviceStatus
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
import io.getstream.video.android.core.events.CallEndedSfuEvent
import io.getstream.video.android.core.model.RejectReason
import io.getstream.video.android.core.notifications.NotificationHandler
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.model.streamCallId
import io.getstream.video.android.ui.common.models.StreamCallActivityException
import io.getstream.video.android.ui.common.util.StreamCallActivityDelicateApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(StreamCallActivityDelicateApi::class)
public abstract class StreamCallActivity : ComponentActivity(), ActivityCallOperations {
    // Factory and creation
    public companion object {
        // Extra keys
        private const val EXTRA_LEAVE_WHEN_LAST: String = "leave_when_last"
        private const val EXTRA_MEMBERS_ARRAY: String = "members_extra"

        // Extra default values
        private const val DEFAULT_LEAVE_WHEN_LAST: Boolean = false
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

        public fun callIntentBundle(
            cid: StreamCallId,
            members: List<String> = defaultExtraMembers,
            leaveWhenLastInCall: Boolean = DEFAULT_LEAVE_WHEN_LAST,
            configuration: StreamCallActivityConfiguration = StreamCallActivityConfiguration(),
            extraData: Bundle? = null,
        ): Bundle {
            return Bundle().apply {
                // Setup the outgoing call action

                val config = configuration.toBundle()
                // Add the config

                putBundle(StreamCallActivityConfigStrings.EXTRA_STREAM_CONFIG, config)
                // Add the generated call ID and other params
                putParcelable(NotificationHandler.INTENT_EXTRA_CALL_CID, cid)
                putBoolean(EXTRA_LEAVE_WHEN_LAST, leaveWhenLastInCall)
                // Setup the members to transfer to the new activity
                val membersArrayList = ArrayList<String>()
                members.forEach { membersArrayList.add(it) }
                putStringArrayList(EXTRA_MEMBERS_ARRAY, membersArrayList)
                extraData?.also {
                    putAll(it)
                }

                logger.d { "Created Bundle. -> $this" }
            }
        }
    }

    // Internal state
    private var callSocketConnectionMonitor: Job? = null
    private lateinit var cachedCall: Call

    @Deprecated(
        "Use configurationMap instead",
        replaceWith = ReplaceWith("configurationMap"),
        level = DeprecationLevel.WARNING,
    )
    protected lateinit var config: StreamCallActivityConfiguration
        private set

    /**
     * Map of a call id with StreamCallActivityConfiguration
     * You can get Call id via
     * `intent?.streamCallId(NotificationHandler.INTENT_EXTRA_CALL_CID)?.id`
     */
    public val configurationMap: HashMap<StreamCallIdString, StreamCallActivityConfiguration> =
        HashMap()

    private var cachedCallEventJob: Job? = null
    private var participantCountJob: Job? = null
    private val supervisorJob = SupervisorJob()
    private var isFinishingSafely = false

    protected val onSuccessFinish: suspend (Call) -> Unit = { call ->
        logger.d { "[onSuccessFinish]" }
        onEnded(call)
        if (isCurrentAcceptedCall(call)) {
            val configuration = configurationMap[call.id]
            if (configuration?.closeScreenOnCallEnded == true) {
                logger.w {
                    "[onSuccessFinish], The call was successfully finished! Closing activity, call_cid:${call.cid}"
                }
                safeFinish()
            }
        } else {
            logger.d { "[onSuccessFinish] for non-active call" }
        }
    }

    /**
     * The Exception is [StreamCallActivityException]. We will update the args in next major release
     */
    protected val onErrorFinish: suspend (Exception) -> Unit = { error ->
        onFailed(error)
        if (error is StreamCallActivityException) {
            logger.e(error) { "[onErrorFinish] Something went wrong, call_id:${error.call.id}" }
            if (isCurrentAcceptedCall(error.call)) {
                val configuration = configurationMap[error.call.id]
                if (configuration?.closeScreenOnError == true) {
                    logger.e(error) { "Finishing the activity" }
                    safeFinish()
                }
            } else {
                logger.e(error) { "[onErrorFinish] for non-active call" }
            }
        } else {
            /**
             * This will execute when we got a error before creating the call object
             */
            if (config.closeScreenOnError) {
                logger.e(error) { "Finishing the activity" }
                safeFinish()
            }
        }
    }

    /**
     * The call which is accepted
     */
    public open fun isCurrentAcceptedCall(call: Call): Boolean =
        (::cachedCall.isInitialized) && (cachedCall.id == call.id)

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
    @Deprecated(
        "Accessing configuration before onCreate may lead to unintended behavior. Use configurationMap instead.",
        level = DeprecationLevel.WARNING,
    )
    @StreamCallActivityDelicateApi
    public open val configuration: StreamCallActivityConfiguration
        get() {
            if (!::config.isInitialized) {
                logger.w {
                    "Deprecated configuration getter accessed before onCreate. Use onCreate-initialized configurationMap instead."
                }
                config = loadConfigFromIntent(intent) // safe fallback
            }
            return config
        }

    protected open fun loadConfigFromIntent(intent: Intent?): StreamCallActivityConfiguration {
        val bundledConfig = intent?.getBundleExtra(
            StreamCallActivityConfigStrings.EXTRA_STREAM_CONFIG,
        )
        return runCatching {
            bundledConfig?.extractStreamActivityConfig() ?: StreamCallActivityConfiguration()
        }.getOrElse { e ->
            logger.e(e) { "Failed to load config. Using default." }
            StreamCallActivityConfiguration()
        }
    }

    protected open var callHandlerDelegate: IncomingCallHandlerDelegate? = null

    // Default implementation that rejects new calls when there's an ongoing call
    private val defaultCallHandler = object : IncomingCallHandlerDelegate {
        override fun shouldAcceptNewCall(activeCall: Call, intent: Intent) = true

        override fun onAcceptCall(intent: Intent) {
            finish()
            startActivity(intent)
        }

        override fun onIgnoreCall(intent: Intent, reason: IgnoreReason) {
            val newCallCid = intent.streamCallId(NotificationHandler.INTENT_EXTRA_CALL_CID)?.cid
            when (reason) {
                IgnoreReason.SameCall -> {
                    logger.d { "Ignoring duplicate call_cid:$newCallCid" }
                }

                IgnoreReason.DelegateDeclined -> {
                    logger.d { "Call declined by delegate declined call_cid:$newCallCid" }
                }
                else -> {}
            }
        }
    }

    // Platform restriction
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onPreCreate(savedInstanceState, null)
        logger.d { "Entered [onCreate(Bundle?)" }
        initializeCallOrFail(
            savedInstanceState,
            null,
            intent,
            onSuccess = { instanceState, persistentState, call, action ->
                logger.d { "Calling [onCreate(Call)], because call is initialized $call" }
                onIntentAction(call, action, onError = onErrorFinish) { successCall ->
                    applyDashboardSettings(successCall)
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

    public override fun onCreate(
        savedInstanceState: Bundle?,
        persistentState: PersistableBundle?,
    ) {
        super.onCreate(savedInstanceState)
        onPreCreate(savedInstanceState, persistentState)
        logger.d { "Entered [onCreate(Bundle, PersistableBundle?)" }
        initializeCallOrFail(
            savedInstanceState,
            persistentState,
            intent,
            onSuccess = { instanceState, persistedState, call, action ->
                logger.d { "Calling [onCreate(Call)], because call is initialized $call" }
                onIntentAction(call, action, onError = onErrorFinish) { successCall ->
                    applyDashboardSettings(successCall)
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        /**
         * Necessary because the intent is read during the activity's lifecycle methods.
         */
        setIntent(intent)
        initializeConfig(intent)
        handleOnNewIntentAction(intent)
    }

    protected open fun handleOnNewIntentAction(intent: Intent) {
        logger.d { "[handleOnNewIntentAction], intent action = ${intent.action}" }
        when (intent.action) {
            NotificationHandler.ACTION_ACCEPT_CALL -> {
                handleOnNewIncomingCallAcceptAction()
            }
            NotificationHandler.ACTION_INCOMING_CALL -> {
                handleOnNewIncomingCallAction()
            }
            else -> {}
        }
    }

    protected open fun handleOnNewIncomingCallAction() {
        val activeCall = StreamVideo.instance().state.activeCall.value
        if (activeCall == null) {
            initializeCallOrFail(
                null,
                null,
                intent,
                onSuccess = { instanceState, persistedState, call, action ->
                    logger.d { "Calling [handleOnNewIncomingCallAction], because call id: ${call.id}" }
                    onIntentAction(call, action, onError = onErrorFinish) { successCall ->
                        applyDashboardSettings(successCall)
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
    }

    protected open fun handleOnNewIncomingCallAcceptAction() {
        val handler = callHandlerDelegate ?: defaultCallHandler
        val newCallCid = intent.streamCallId(NotificationHandler.INTENT_EXTRA_CALL_CID)?.cid
        val activeCall = StreamVideo.instance().state.activeCall.value

        when {
            // No ongoing call, so we accept the new call
            activeCall == null -> {
                handler.onAcceptCall(intent)
            }

            // Incoming call is same as active call
            activeCall.cid == newCallCid -> {
                handler.onIgnoreCall(intent, IgnoreReason.SameCall)
            }

            else -> {
                if (handler.shouldAcceptNewCall(activeCall, intent)) {
                    // We want to leave the ongoing active call
                    leave(activeCall, onSuccessFinish, onErrorFinish)
                    lifecycleScope.launch(Dispatchers.Default) {
                        delay(
                            getCallTransitionTime(),
                        ) // Grace time for call related things to be cleared safely
                        withContext(Dispatchers.Main) {
                            handler.onAcceptCall(intent)
                        }
                    }
                } else {
                    handler.onIgnoreCall(intent, IgnoreReason.DelegateDeclined)
                }
            }
        }
    }

    public override fun onResume() {
        super.onResume()
        withCachedCall {
            onResume(it)
        }
    }

    public override fun onUserLeaveHint() {
        withCachedCall {
            onUserLeaveHint(it)
            super.onUserLeaveHint()
        }
    }

    public override fun onPause() {
        withCachedCall {
            onPause(it)
            super.onPause()
        }
    }

    public override fun onStop() {
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
        initializeConfig(intent)
        logger.d { "Activity pre-created with configuration [$config]" }
        uiDelegate.loadingContent(this)
    }

    private fun initializeConfig(intent: Intent?) {
        val configurationFromIntent = loadConfigFromIntent(intent)
        config = configuration.copy(
            closeScreenOnCallEnded = configurationFromIntent.closeScreenOnCallEnded,
            closeScreenOnError = configurationFromIntent.closeScreenOnError,
            canSkipPermissionRationale = configurationFromIntent.canSkipPermissionRationale,
            canKeepScreenOn = configurationFromIntent.canKeepScreenOn,
        )

        val streamCallId = intent?.streamCallId(NotificationHandler.INTENT_EXTRA_CALL_CID)
        streamCallId?.let {
            val sb = StringBuilder()
            sb.append("closeScreenOnError=${config.closeScreenOnError}\n")
            sb.append("closeScreenOnCallEnded=${config.closeScreenOnCallEnded}\n")
            sb.append("canKeepScreenOn=${config.canKeepScreenOn}\n")
            sb.append("canSkipPermissionRationale=${config.canSkipPermissionRationale}\n")
            sb.append("StreamVideo is null=${StreamVideo.instanceOrNull() == null}\n")
            val leaveWhenLastInCall = intent.getBooleanExtra(
                EXTRA_LEAVE_WHEN_LAST,
                DEFAULT_LEAVE_WHEN_LAST,
            )
            logger.d {
                "[initializeConfig], call_id: ${it.id}, activity hashcode=${this.hashCode()}, Configuration: $sb, Leave when last in call: $leaveWhenLastInCall"
            }
            configurationMap[it.id] = config
        }
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
        if (config.canKeepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        logger.d { "DefaultCallActivity - Resumed (call -> $call)" }
    }

    /**
     * Called when the activity is about to go into the background as the result of user choice. Makes sure the call object is available.
     *
     * @param call the call
     */
    public open fun onUserLeaveHint(call: Call) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // Default PiP behavior
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) &&
            isConnected(call) &&
            !isChangingConfigurations &&
            isVideoCall(call) &&
            !isInPictureInPictureMode
        ) {
            try {
                enterPictureInPicture()
            } catch (e: Exception) {
                logger.e(e) { "[onUserLeaveHint] Something went wrong when entering PiP." }
            }
        }

        logger.d { "DefaultCallActivity - Leave Hinted (call -> $call)" }
    }

    /**
     * Called when the activity is paused. Makes sure the call object is available.
     *
     * @param call the call
     */
    public open fun onPause(call: Call) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
        // No-op
        logger.d { "Default activity - stopped (call -> $call)" }
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
    public open fun isVideoCall(call: Call): Boolean {
        return call.hasCapability(OwnCapability.SendVideo) || call.isVideoEnabled()
    }

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
     * @param onSuccess callback where the [Call] object is returned
     * @param onError callback when the [Call] was not returned.
     */
    @StreamCallActivityDelicateApi
    override fun call(
        cid: StreamCallId,
        onSuccess: ((Call) -> Unit)?,
        onError: ((Exception) -> Unit)?,
    ) {
        // TODO Rahul, need testing
        val sdkInstance = StreamVideo.instanceOrNull()
        if (sdkInstance != null) {
            val call = sdkInstance.call(cid.type, cid.id)
            onSuccess?.invoke(call)
        } else {
            onError?.invoke(
                IllegalStateException(
                    "StreamVideoBuilder.build() must be called before obtaining StreamVideo instance.",
                ),
            )
        }
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
    override fun create(
        call: Call,
        ring: Boolean,
        members: List<String>,
        onSuccess: (suspend (Call) -> Unit)?,
        onError: (suspend (Exception) -> Unit)?,
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
    override fun get(
        call: Call,
        onSuccess: (suspend (Call) -> Unit)?,
        onError: (suspend (Exception) -> Unit)?,
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
    override fun join(
        call: Call,
        onSuccess: (suspend (Call) -> Unit)?,
        onError: (suspend (Exception) -> Unit)?,
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
    override fun accept(
        call: Call,
        onSuccess: (suspend (Call) -> Unit)?,
        onError: (suspend (Exception) -> Unit)?,
    ) {
        logger.d { "[accept] #ringing; call.cid: ${call.cid}" }
        acceptOrJoinNewCall(call, onSuccess, onError) {
            // TODO Rahul need to be tested
            val result = call.acceptThenJoin()
            result.onError { error ->
                lifecycleScope.launch {
                    onError?.invoke(Exception(error.message))
                }
            }
            result
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
    @Suppress("DeferredResultUnused")
    @StreamCallActivityDelicateApi
    override fun reject(
        call: Call,
        reason: RejectReason?,
        onSuccess: (suspend (Call) -> Unit)?,
        onError: (suspend (Exception) -> Unit)?,
    ) {
        logger.d { "[reject] #ringing; rejectReason: $reason, call_id: ${call.id}" }
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        call.state.cancelTimeout()
        call.state.updateRejectedBy(mutableSetOf(StreamVideo.instance().userId))
        appScope.async {
            val result = call.reject(reason)
            if (lifecycleScope.isActive) {
                lifecycleScope.launch {
                    result.onOutcome(call, onSuccess, onError)
                }
            }

            // Leave regardless of outcome
            call.leave()
        }
    }

    /**
     * Cancel an outgoing call if any.
     * Same as [rejectCall] but can be overridden for different behavior on Outgoing calls.
     *
     * @param call the [Call] to cancel.
     * @param onSuccess optionally get notified if the operation was success.
     * @param onError optionally get notified if the operation failed.
     */
    @StreamCallActivityDelicateApi
    override fun cancel(
        call: Call,
        onSuccess: (suspend (Call) -> Unit)?,
        onError: (suspend (Exception) -> Unit)?,
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
    override fun leave(
        call: Call,
        onSuccess: (suspend (Call) -> Unit)?,
        onError: (suspend (Exception) -> Unit)?,
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
    override fun end(
        call: Call,
        onSuccess: (suspend (Call) -> Unit)?,
        onError: (suspend (Exception) -> Unit)?,
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
            is CallEndedEvent, is CallEndedSfuEvent, is CallSessionEndedEvent -> {
                // In any case finish the activity, the call is done for
                leave(call, onSuccess = onSuccessFinish, onError = onErrorFinish)
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
        logger.d { "leaveWhenLastInCall = $leaveWhenLastInCall" }
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
                    onErrorFinish.invoke(
                        StreamCallActivityException(
                            call,
                            throwable.message,
                            throwable,
                        ),
                    )
                }
            }

            else -> {
                // No-op
            }
        }
    }

    /**
     * Used to apply dashboard settings to the call.
     * By default, it enables or disables the microphone and camera based on the settings.
     *
     * @param call the call
     */
    public open fun applyDashboardSettings(call: Call) {
        logger.d { "[applyDashboardSettings] for call_cid: ${call.cid}" }
        val callSettings = call.state.settings.value
        val microphoneStatus = call.microphone.status.value
        val cameraStatus = call.camera.status.value

        if (microphoneStatus == DeviceStatus.NotSelected) {
            call.microphone.setEnabled(callSettings?.audio?.micDefaultOn == true)
        }
        if (cameraStatus == DeviceStatus.NotSelected) {
            call.camera.setEnabled(callSettings?.video?.cameraDefaultOn == true)
        }
    }

    // Internal logic
    private fun initializeCallOrFail(
        savedInstanceState: Bundle?,
        persistentState: PersistableBundle?,
        intent: Intent,
        onSuccess: ((Bundle?, PersistableBundle?, Call, action: String?) -> Unit)? = null,
        onError: ((Exception) -> Unit)? = null,
    ) {
        // Have a call ID or crash
        val cid = intent.streamCallId(NotificationHandler.INTENT_EXTRA_CALL_CID)

        if (cid == null) {
            val e = IllegalArgumentException("CallActivity started without call ID.")

            logger.e(e) {
                "Failed to initialize call because call ID is not found in the intent. $intent"
            }

            onError?.invoke(e) ?: throw e

            // Finish
            return
        }

        call(
            cid,
            onSuccess = { call ->
                cachedCall = call
                cachedCallEventJob?.cancel()
                cachedCallEventJob = lifecycleScope.launch(supervisorJob) {
                    cachedCall.events.collect { event ->
                        onCallEvent(cachedCall, event)
                    }
                }

                participantCountJob?.cancel()
                participantCountJob = lifecycleScope.launch(supervisorJob) {
                    cachedCall.state.participants.collect {
                        logger.d { "Participant left, remaining: ${it.size}" }
                        lifecycleScope.launch(Dispatchers.Default) {
                            it.forEachIndexed { i, v ->
                                logger.d { "Participant [$i]=${v.name.value}" }
                            }
                        }
                        if (it.size <= 1) {
                            onLastParticipant(call)
                        }
                    }
                }

                callSocketConnectionMonitor?.cancel()
                callSocketConnectionMonitor =
                    lifecycleScope.launch(Dispatchers.IO + supervisorJob) {
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
            initializeCallOrFail(null, null, intent, onSuccess = { _, _, call, _ ->
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

    private fun isConnected(call: Call): Boolean =
        when (call.state.connection.value) {
            RealtimeConnection.Disconnected -> false
            RealtimeConnection.PreJoin -> false
            is RealtimeConnection.Failed -> false
            else -> true
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
                onError(StreamCallActivityException(call, it.message, it.extractCause()))
            }
        }
    }

    /**
     * Used when there is an active call and user wants to
     * switch to new call. After rejecting current call, the user
     * needs to wait for a transition time
     */
    protected open fun getCallTransitionTime(): Long = 0L

    private suspend fun Call.acceptThenJoin() =
        withContext(Dispatchers.IO) {
            accept().flatMap { join() }
        }

    public fun safeFinish() {
        if (!this.isFinishing && !isFinishingSafely) {
            isFinishingSafely = true
            logger.d { "[safeFinish] call_id:${cachedCall.cid}" }
            finish()
        }
    }
}

public typealias StreamCallIdString = String
