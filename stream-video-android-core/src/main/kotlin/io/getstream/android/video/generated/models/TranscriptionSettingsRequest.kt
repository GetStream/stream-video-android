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

@file:Suppress(
    "ArrayInDataClass",
    "EnumEntryName",
    "RemoveRedundantQualifierName",
    "UnusedImport"
)

package io.getstream.android.video.generated.models

import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.*
import kotlin.io.*
import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

/**
 *
 */

data class TranscriptionSettingsRequest (
    @Json(name = "mode")
    val mode: Mode,

    @Json(name = "closed_caption_mode")
    val closedCaptionMode: ClosedCaptionMode? = null,

    @Json(name = "language")
    val language: Language? = null
)
{

    /**
    * Mode Enum
    */
    sealed class Mode(val value: kotlin.String) {
            override fun toString(): String = value

            companion object {
                fun fromString(s: kotlin.String): Mode = when (s) {
                    "auto-on" -> AutoOn
                    "available" -> Available
                    "disabled" -> Disabled
                    else -> Unknown(s)
                }
            }
            object AutoOn : Mode("auto-on")
            object Available : Mode("available")
            object Disabled : Mode("disabled")
            data class Unknown(val unknownValue: kotlin.String) : Mode(unknownValue)


        class ModeAdapter : JsonAdapter<Mode>() {
            @FromJson
            override fun fromJson(reader: JsonReader): Mode? {
                val s = reader.nextString() ?: return null
                return Mode.fromString(s)
            }

            @ToJson
            override fun toJson(writer: JsonWriter, value: Mode?) {
                writer.value(value?.value)
            }
        }
    }
    /**
    * ClosedCaptionMode Enum
    */
    sealed class ClosedCaptionMode(val value: kotlin.String) {
            override fun toString(): String = value

            companion object {
                fun fromString(s: kotlin.String): ClosedCaptionMode = when (s) {
                    "auto-on" -> AutoOn
                    "available" -> Available
                    "disabled" -> Disabled
                    else -> Unknown(s)
                }
            }
            object AutoOn : ClosedCaptionMode("auto-on")
            object Available : ClosedCaptionMode("available")
            object Disabled : ClosedCaptionMode("disabled")
            data class Unknown(val unknownValue: kotlin.String) : ClosedCaptionMode(unknownValue)


        class ClosedCaptionModeAdapter : JsonAdapter<ClosedCaptionMode>() {
            @FromJson
            override fun fromJson(reader: JsonReader): ClosedCaptionMode? {
                val s = reader.nextString() ?: return null
                return ClosedCaptionMode.fromString(s)
            }

            @ToJson
            override fun toJson(writer: JsonWriter, value: ClosedCaptionMode?) {
                writer.value(value?.value)
            }
        }
    }
    /**
    * Language Enum
    */
    sealed class Language(val value: kotlin.String) {
            override fun toString(): String = value

            companion object {
                fun fromString(s: kotlin.String): Language = when (s) {
                    "ar" -> Ar
                    "auto" -> Auto
                    "ca" -> Ca
                    "cs" -> Cs
                    "da" -> Da
                    "de" -> De
                    "el" -> El
                    "en" -> En
                    "es" -> Es
                    "fi" -> Fi
                    "fr" -> Fr
                    "he" -> He
                    "hi" -> Hi
                    "hr" -> Hr
                    "hu" -> Hu
                    "id" -> Id
                    "it" -> It
                    "ja" -> Ja
                    "ko" -> Ko
                    "ms" -> Ms
                    "nl" -> Nl
                    "no" -> No
                    "pl" -> Pl
                    "pt" -> Pt
                    "ro" -> Ro
                    "ru" -> Ru
                    "sv" -> Sv
                    "ta" -> Ta
                    "th" -> Th
                    "tl" -> Tl
                    "tr" -> Tr
                    "uk" -> Uk
                    "zh" -> Zh
                    else -> Unknown(s)
                }
            }
            object Ar : Language("ar")
            object Auto : Language("auto")
            object Ca : Language("ca")
            object Cs : Language("cs")
            object Da : Language("da")
            object De : Language("de")
            object El : Language("el")
            object En : Language("en")
            object Es : Language("es")
            object Fi : Language("fi")
            object Fr : Language("fr")
            object He : Language("he")
            object Hi : Language("hi")
            object Hr : Language("hr")
            object Hu : Language("hu")
            object Id : Language("id")
            object It : Language("it")
            object Ja : Language("ja")
            object Ko : Language("ko")
            object Ms : Language("ms")
            object Nl : Language("nl")
            object No : Language("no")
            object Pl : Language("pl")
            object Pt : Language("pt")
            object Ro : Language("ro")
            object Ru : Language("ru")
            object Sv : Language("sv")
            object Ta : Language("ta")
            object Th : Language("th")
            object Tl : Language("tl")
            object Tr : Language("tr")
            object Uk : Language("uk")
            object Zh : Language("zh")
            data class Unknown(val unknownValue: kotlin.String) : Language(unknownValue)


        class LanguageAdapter : JsonAdapter<Language>() {
            @FromJson
            override fun fromJson(reader: JsonReader): Language? {
                val s = reader.nextString() ?: return null
                return Language.fromString(s)
            }

            @ToJson
            override fun toJson(writer: JsonWriter, value: Language?) {
                writer.value(value?.value)
            }
        }
    }
}
