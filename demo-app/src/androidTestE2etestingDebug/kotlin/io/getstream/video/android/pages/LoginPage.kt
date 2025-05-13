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

package io.getstream.video.android.pages

import androidx.test.uiautomator.By

class LoginPage {

    companion object {
        val builtInUserSignInButton = By.res("Stream_BuiltInUserSignIn")
        val googleSignInButton = By.res("Stream_GoogleSignIn")
        val randomUserSignInButton = By.res("Stream_RandomUserSignIn")
        val emailSignIn = By.res("Stream_EmailSignIn")
        val loginButton = By.text("Login")
    }
}
