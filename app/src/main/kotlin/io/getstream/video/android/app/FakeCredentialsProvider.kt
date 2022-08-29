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

package io.getstream.video.android.app

import io.getstream.video.android.token.CredentialsProvider

class FakeCredentialsProvider(
    private val token: String,
    private val apiKey: String
) : CredentialsProvider {

    override fun loadToken(): String {
        return token
    }

    override fun getCachedToken(): String {
        return token
    }

    override fun getCachedApiKey(): String {
        return apiKey
    }

    override fun loadApiKey(): String {
        return apiKey
    }

    override fun getSfuToken(): String {
        return TEST_TOKEN
    }

    companion object {
        private const val TEST_TOKEN = "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJhcHBfaWQiOjQyLCJjYWxsX2lkIjoiY2FsbDoxMjMiLCJ1c2VyIjp7ImlkIjoiZmlsaXAiLCJpbWFnZV91cmwiOiJodHRwczovL2dldHN0cmVhbS5pby9zdGF0aWMvNzZjZGE0OTY2OWJlMzhiOTIzMDZjZmM5M2NhNzQyZjEvODAyZDIvZmlsaXAtYmFiaSVDNCU4Ny53ZWJwIn0sImdyYW50cyI6eyJjYW5fam9pbl9jYWxsIjp0cnVlLCJjYW5fcHVibGlzaF92aWRlbyI6dHJ1ZSwiY2FuX3B1Ymxpc2hfYXVkaW8iOnRydWUsImNhbl9zY3JlZW5fc2hhcmUiOnRydWUsImNhbl9tdXRlX3ZpZGVvIjp0cnVlLCJjYW5fbXV0ZV9hdWRpbyI6dHJ1ZX0sImlzcyI6ImRldi1vbmx5LnB1YmtleS5lY2RzYTI1NiIsImF1ZCI6WyJsb2NhbGhvc3QiXX0.XmvDAtIAjnWMETVun0Vffcrp9Tk7xujXZS8GawVdBY8R8yxec4asziTUKHJCkXq6GjeJtEVMtrzoJs9qP0xtDQ"
    }
}
