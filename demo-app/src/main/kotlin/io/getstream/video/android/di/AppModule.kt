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

package io.getstream.video.android.di

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.getstream.video.android.R
import io.getstream.video.android.data.datasource.local.InMemoryStore
import io.getstream.video.android.data.datasource.local.InMemoryStoreImpl
import io.getstream.video.android.data.datasource.local.NoopMemoryStoreImpl
import io.getstream.video.android.data.repositories.GoogleAccountRepository
import io.getstream.video.android.datastore.delegate.StreamUserDataStore
import io.getstream.video.android.tooling.util.StreamBuildFlavor
import io.getstream.video.android.tooling.util.StreamBuildFlavorUtil
import io.getstream.video.android.util.NetworkMonitor
import javax.inject.Singleton

@dagger.Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideUserDataStore(): StreamUserDataStore {
        return StreamUserDataStore.instance()
    }

    @Provides
    @Singleton
    fun provideGoogleSignInClient(
        @ApplicationContext context: Context,
    ): GoogleSignInClient = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestIdToken(context.getString(R.string.default_web_client_id))
        .requestScopes(Scope("https://www.googleapis.com/auth/directory.readonly"))
        .build()
        .let { gso -> GoogleSignIn.getClient(context, gso) }

    @Provides
    fun provideGoogleAccountRepository(
        @ApplicationContext context: Context,
        googleSignInClient: GoogleSignInClient,
    ) = GoogleAccountRepository(context, googleSignInClient)

    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context) =
        NetworkMonitor(context)

    @Provides
    @Singleton
    fun provideInMemoryStore(): InMemoryStore {
        val buildFlavour = StreamBuildFlavorUtil.current
        return when (buildFlavour) {
            StreamBuildFlavor.E2eTesting -> InMemoryStoreImpl()
            else -> NoopMemoryStoreImpl()
        }
    }
}
