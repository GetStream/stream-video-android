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

package io.getstream.video.android.app.utils

import io.getstream.video.android.app.model.UserCredentials

fun getUsers(): List<UserCredentials> {
    return listOf(
        UserCredentials(
            id = "filip",
            name = "Filip",
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiZmlsaXAifQ.qjL9ajhHQgtrlKDvINmN7Kmf8uuZv52d4j8elnqN2iM"
        ),
        UserCredentials(
            id = "mile_kitic",
            name = "Mile Kitic",
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoibWlsZV9raXRpYyJ9.BW7ckfQexMNS_aag9b7iNEfihffR2q8UxBRGtx3NL-8"
        ),
        UserCredentials(
            id = "jedini_toma",
            name = "Toma Zdravkovic",
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoidG9tYSJ9.0ZhjX_HCFE6cQ55znQeNi6DLXJ1g_mgww8idyQIeWMA"
        )
    )
}
