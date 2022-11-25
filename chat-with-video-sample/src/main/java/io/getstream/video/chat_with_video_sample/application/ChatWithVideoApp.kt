package io.getstream.video.chat_with_video_sample.application


import android.app.Application
import android.content.Context
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.logger.ChatLogLevel
import io.getstream.chat.android.client.models.UploadAttachmentsNetworkType
import io.getstream.chat.android.compose.ui.attachments.StreamAttachmentFactories
import io.getstream.chat.android.offline.plugin.factory.StreamOfflinePluginFactory
import io.getstream.chat.android.state.plugin.config.StatePluginConfig
import io.getstream.chat.android.state.plugin.factory.StreamStatePluginFactory
import io.getstream.video.android.StreamVideo
import io.getstream.video.android.StreamVideoBuilder
import io.getstream.video.android.StreamVideoConfig
import io.getstream.video.android.input.CallActivityInput
import io.getstream.video.android.input.CallServiceInput
import io.getstream.video.android.logging.LoggingLevel
import io.getstream.video.android.token.CredentialsProvider
import io.getstream.video.android.user.UsersProvider
import io.getstream.video.chat_with_video_sample.BuildConfig
import io.getstream.video.chat_with_video_sample.ui.call.CallActivity
import io.getstream.video.chat_with_video_sample.ui.call.CallService
import io.getstream.video.chat_with_video_sample.ui.messages.attachment.CallAttachmentFactory
import io.getstream.video.chat_with_video_sample.users.FakeUsersProvider

class ChatWithVideoApp : Application() {

    private val videoConfig: StreamVideoConfig = object : StreamVideoConfig {
        override val dropTimeout: Long = 30_000L
        override val cancelOnTimeout: Boolean = true
        override val joinOnAcceptedByCallee: Boolean = true
        override val createCallClientInternally: Boolean = true
    }

    val usersLoginProvider: UsersProvider by lazy { FakeUsersProvider() }

    val chatClient: ChatClient by lazy {
        val offlinePlugin = StreamOfflinePluginFactory(this)

        val statePluginFactory = StreamStatePluginFactory(
            config = StatePluginConfig(
                backgroundSyncEnabled = true,
                userPresence = true,
            ),
            appContext = this
        )

        val logLevel = if (BuildConfig.DEBUG) ChatLogLevel.ALL else ChatLogLevel.NOTHING

        ChatClient.Builder("tp8sef43xcpc", this)
            .withPlugins(offlinePlugin, statePluginFactory)
            .logLevel(logLevel)
            .uploadAttachmentsNetworkType(UploadAttachmentsNetworkType.NOT_ROAMING)
            .build()
    }

    val attachmentFactories by lazy {
        listOf(CallAttachmentFactory()) + StreamAttachmentFactories.defaultFactories()
    }

    lateinit var credentialsProvider: CredentialsProvider
        private set

    lateinit var streamVideo: StreamVideo
        private set

    fun initializeStreamVideo(
        credentialsProvider: CredentialsProvider,
        loggingLevel: LoggingLevel
    ): StreamVideo {
        if (this::credentialsProvider.isInitialized) {
            this.credentialsProvider.updateUser(
                credentialsProvider.getUserCredentials(),
                credentialsProvider.getCachedUserToken()
            )
        } else {
            this.credentialsProvider = credentialsProvider
        }

        return StreamVideoBuilder(
            context = this,
            credentialsProvider = credentialsProvider,
            androidInputs = setOf(
                CallServiceInput.from(CallService::class),
                CallActivityInput.from(CallActivity::class),
            ),
            loggingLevel = loggingLevel,
            config = videoConfig
        ).build().also {
            streamVideo = it
        }
    }

    fun logOut() {
        chatClient.disconnect(true).enqueue()
        streamVideo.clearCallState()
    }
}

internal val Context.chatWithVideoApp get() = applicationContext as ChatWithVideoApp