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
            id = "filip_babic",
            name = "Filip",
            image = "https://avatars.githubusercontent.com/u/17215808?v=4",
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiZmlsaXBfYmFiaWMifQ.y-9wv1_yuG41crxTWL9V5wb33du3DXv-m-EFeIH92wk",
            sfuToken = "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJhcHBfaWQiOjQyLCJjYWxsX2lkIjoiY2FsbDoxMjMiLCJ1c2VyIjp7ImlkIjoiZmlsaXAiLCJpbWFnZV91cmwiOiJodHRwczovL2dldHN0cmVhbS5pby9zdGF0aWMvNzZjZGE0OTY2OWJlMzhiOTIzMDZjZmM5M2NhNzQyZjEvODAyZDIvZmlsaXAtYmFiaSVDNCU4Ny53ZWJwIn0sImdyYW50cyI6eyJjYW5fam9pbl9jYWxsIjp0cnVlLCJjYW5fcHVibGlzaF92aWRlbyI6dHJ1ZSwiY2FuX3B1Ymxpc2hfYXVkaW8iOnRydWUsImNhbl9zY3JlZW5fc2hhcmUiOnRydWUsImNhbl9tdXRlX3ZpZGVvIjp0cnVlLCJjYW5fbXV0ZV9hdWRpbyI6dHJ1ZX0sImlzcyI6ImRldi1vbmx5LnB1YmtleS5lY2RzYTI1NiIsImF1ZCI6WyJsb2NhbGhvc3QiXX0.XmvDAtIAjnWMETVun0Vffcrp9Tk7xujXZS8GawVdBY8R8yxec4asziTUKHJCkXq6GjeJtEVMtrzoJs9qP0xtDQ"
        ),
        UserCredentials(
            id = "mile_kralj",
            name = "Mile Kitic",
            image = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e3/Mile_Kitic_from_BISO0675.jpg/300px-Mile_Kitic_from_BISO0675.jpg",
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoibWlsZV9rcmFsaiJ9.SWkPo6Z2EOGlFgQdKafIygyrheGeSXi6D9JDv-zJ7rY",
            sfuToken = "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJhcHBfaWQiOjQyLCJjYWxsX2lkIjoiY2FsbDoxMjMiLCJ1c2VyIjp7ImlkIjoidGhpZXJyeSIsImltYWdlX3VybCI6Imh0dHBzOi8vZ2V0c3RyZWFtLmlvL3N0YXRpYy8yMzdmNDVmMjg2OTA2OTZhZDhmZmY5MjcyNmY0NTEwNi9jNTlkZS90aGllcnJ5LndlYnAifSwiZ3JhbnRzIjp7ImNhbl9qb2luX2NhbGwiOnRydWUsImNhbl9wdWJsaXNoX3ZpZGVvIjp0cnVlLCJjYW5fcHVibGlzaF9hdWRpbyI6dHJ1ZSwiY2FuX3NjcmVlbl9zaGFyZSI6dHJ1ZSwiY2FuX211dGVfdmlkZW8iOnRydWUsImNhbl9tdXRlX2F1ZGlvIjp0cnVlfSwiaXNzIjoiZGV2LW9ubHkucHVia2V5LmVjZHNhMjU2IiwiYXVkIjpbImxvY2FsaG9zdCJdfQ.pmaz5REWBAWLSJsycIkKcpJlCPr9eyUCB4Pa3ij5Mt5yai39ZZC8zsweR_mKlP-yYo4Zb69zfodA3PWwRhEUCg"
        ),
        UserCredentials(
            id = "toma_zdravkovic",
            name = "Toma Zdravkovic",
            image = "https://upload.wikimedia.org/wikipedia/commons/d/da/Toma_Zdravkovi%C4%87.jpg",
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoidG9tYV96ZHJhdmtvdmljIn0.NzcC4Y9DvSUEES3NkvqutNBOtKtZAQzovOoASzAZcG4",
            sfuToken = "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJhcHBfaWQiOjQyLCJjYWxsX2lkIjoiY2FsbDoxMjMiLCJ1c2VyIjp7ImlkIjoibWFydGluIiwiaW1hZ2VfdXJsIjoiaHR0cHM6Ly9nZXRzdHJlYW0uaW8vc3RhdGljLzI3OTZhMzA1ZGQwNzY1MWZjY2ViNDcyMWE5NGY0NTA1LzgwMmQyL21hcnRpbi1taXRyZXZza2kud2VicCJ9LCJncmFudHMiOnsiY2FuX2pvaW5fY2FsbCI6dHJ1ZSwiY2FuX3B1Ymxpc2hfdmlkZW8iOnRydWUsImNhbl9wdWJsaXNoX2F1ZGlvIjp0cnVlLCJjYW5fc2NyZWVuX3NoYXJlIjp0cnVlLCJjYW5fbXV0ZV92aWRlbyI6dHJ1ZSwiY2FuX211dGVfYXVkaW8iOnRydWV9LCJpc3MiOiJkZXYtb25seS5wdWJrZXkuZWNkc2EyNTYiLCJhdWQiOlsibG9jYWxob3N0Il19.9mHCY3tF4qFYbNcWaHoF0Azs9-r7mNdgefxdw3B56m_27nqLZYgjcyVG9Tqv3LT_5L766FE6tPIZ_ZQ1-_ONwA"
        )
    )
}
