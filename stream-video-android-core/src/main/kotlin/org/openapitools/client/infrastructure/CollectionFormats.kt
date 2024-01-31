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

package org.openapitools.client.infrastructure

class CollectionFormats {

    open class CSVParams {

        var params: List<String>

        constructor(params: List<String>) {
            this.params = params
        }

        constructor(vararg params: String) {
            this.params = listOf(*params)
        }

        override fun toString(): String {
            return params.joinToString(",")
        }
    }

    open class SSVParams : CSVParams {

        constructor(params: List<String>) : super(params)

        constructor(vararg params: String) : super(*params)

        override fun toString(): String {
            return params.joinToString(" ")
        }
    }

    class TSVParams : CSVParams {

        constructor(params: List<String>) : super(params)

        constructor(vararg params: String) : super(*params)

        override fun toString(): String {
            return params.joinToString("\t")
        }
    }

    class PIPESParams : CSVParams {

        constructor(params: List<String>) : super(params)

        constructor(vararg params: String) : super(*params)

        override fun toString(): String {
            return params.joinToString("|")
        }
    }

    class SPACEParams : SSVParams()
}
