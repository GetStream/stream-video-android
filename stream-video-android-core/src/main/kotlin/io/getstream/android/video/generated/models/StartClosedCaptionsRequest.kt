/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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

data class StartClosedCaptionsRequest (
    @Json(name = "enable_transcription")
    val enableTranscription: kotlin.Boolean? = null,

    @Json(name = "external_storage")
    val externalStorage: kotlin.String? = null,

    @Json(name = "language")
    val language: Language? = null,

    @Json(name = "speech_segment_config")
    val speechSegmentConfig: io.getstream.android.video.generated.models.SpeechSegmentConfig? = null
)
{
    
    /**
    * Language Enum
    */
    sealed class Language(val value: kotlin.String) {
            override fun toString(): String = value

            companion object {
                fun fromString(s: kotlin.String): Language = when (s) {
                    "ar" -> Ar
                    "auto" -> Auto
                    "bg" -> Bg
                    "ca" -> Ca
                    "cs" -> Cs
                    "da" -> Da
                    "de" -> De
                    "el" -> El
                    "en" -> En
                    "es" -> Es
                    "et" -> Et
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
                    "sk" -> Sk
                    "sl" -> Sl
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
            object Bg : Language("bg")
            object Ca : Language("ca")
            object Cs : Language("cs")
            object Da : Language("da")
            object De : Language("de")
            object El : Language("el")
            object En : Language("en")
            object Es : Language("es")
            object Et : Language("et")
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
            object Sk : Language("sk")
            object Sl : Language("sl")
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
