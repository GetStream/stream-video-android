/*
 * Copyright (c) 2014-2025 Stream.io Inc. All rights reserved.
 *
 * Licensed under the Stream License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/GetStream/stream-android-base/blob/main/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.getstream.kotlin.base.annotation.processor

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Parses a date string in the format "dd.MM.yyyy" to a [LocalDate] object. */
object AnnnotationDateParser {

    /**
     * Parses a date string in the format "dd.MM.yyyy" to a [LocalDate] object.
     *
     * @param dateString The date string to parse.
     * @return The parsed [LocalDate] object.
     */
    fun parseDate(dateString: String): LocalDate {
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        return LocalDate.parse(dateString, formatter)
    }
}
