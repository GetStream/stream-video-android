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
            id = "filip2",
            name = "Filip",
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiZmlsaXAyIn0.FHap2f5AC0nkxmvSJnM4iN_jmMMYfhRb4_S2Rn10hvw"
        ),
        UserCredentials(
            id = "mile_kralj",
            name = "Mile Kitic",
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoibWlsZV9rcmFsaiJ9.QTuoQufYuyJkkcaCCSr3ECojHzQGCF1S0eEPMEgekjE"
        ),
        UserCredentials(
            id = "jedini_toma",
            name = "Toma Zdravkovic",
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiamVkaW5pX3RvbWEifQ.BosMY_OaCL7Yz4gGt62Vyc96v7RhV88X6liFpbTEWTE"
        )
    )
}
