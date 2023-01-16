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

package io.getstream.video.chat_with_video_final.application

import android.app.Application
import android.content.Context
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.logger.ChatLogLevel
import io.getstream.chat.android.client.models.UploadAttachmentsNetworkType
import io.getstream.chat.android.compose.ui.attachments.StreamAttachmentFactories
import io.getstream.chat.android.offline.plugin.factory.StreamOfflinePluginFactory
import io.getstream.chat.android.state.plugin.config.StatePluginConfig
import io.getstream.chat.android.state.plugin.factory.StreamStatePluginFactory
import io.getstream.video.android.BuildConfig
import io.getstream.video.android.StreamVideo
import io.getstream.video.android.StreamVideoBuilder
import io.getstream.video.android.StreamVideoConfig
import io.getstream.video.android.input.CallActivityInput
import io.getstream.video.android.input.CallServiceInput
import io.getstream.video.android.logging.LoggingLevel
import io.getstream.video.android.token.CredentialsProvider
import io.getstream.video.android.user.UserCredentialsManager
import io.getstream.video.android.user.UsersProvider
import io.getstream.video.chat_with_video_final.ui.call.CallActivity
import io.getstream.video.chat_with_video_final.ui.call.CallService
import io.getstream.video.chat_with_video_final.ui.messages.attachment.CallAttachmentFactory
import io.getstream.video.chat_with_video_final.users.FakeUsersProvider

class ChatWithVideoApp : Application() {

    private val videoConfig: StreamVideoConfig = object : StreamVideoConfig {
        override val dropTimeout: Long = 30_000L
        override val cancelOnTimeout: Boolean = true
        override val joinOnAcceptedByCallee: Boolean = true
        override val createCallClientInternally: Boolean = true

        override val autoPublish: Boolean = true
        override val defaultAudioOn: Boolean = false
        override val defaultVideoOn: Boolean = true
        override val defaultSpeakerPhoneOn: Boolean = false
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
                credentialsProvider.getUserCredentials()
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
        val preferences = UserCredentialsManager.initialize(this)

        chatClient.disconnect(true).enqueue()
        streamVideo.clearCallState()
        streamVideo.removeDevices(preferences.getDevices())
        preferences.clear()
    }
}

internal const val API_KEY = "us83cfwuhy8n"

internal val Context.chatWithVideoApp get() = applicationContext as ChatWithVideoApp
