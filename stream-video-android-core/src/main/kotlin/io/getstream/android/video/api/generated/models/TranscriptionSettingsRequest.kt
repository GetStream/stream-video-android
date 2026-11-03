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

package io.getstream.android.video.api.generated.models

import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

/**
 * 
 */
public data class TranscriptionSettingsRequest (
    @Json(name = "closed_caption_mode")
    public val closedCaptionMode: ClosedCaptionMode? = null,

    @Json(name = "language")
    public val language: Language? = null,

    @Json(name = "mode")
    public val mode: Mode? = null,

    @Json(name = "speech_segment_config")
    public val speechSegmentConfig: io.getstream.android.video.api.generated.models.SpeechSegmentConfig? = null,

    @Json(name = "translation")
    public val translation: io.getstream.android.video.api.generated.models.TranslationSettings? = null
)
{
    
    /**
    * ClosedCaptionMode Enum
    */
    public sealed class ClosedCaptionMode(public val value: String) {
            override fun toString(): String = value

            public companion object {
                public fun fromString(s: String): ClosedCaptionMode = when (s) {
                    "auto-on" -> AutoOn
                    "available" -> Available
                    "disabled" -> Disabled
                    else -> Unknown(s)
                }
            }
            public object AutoOn : ClosedCaptionMode("auto-on")
            public object Available : ClosedCaptionMode("available")
            public object Disabled : ClosedCaptionMode("disabled")
            public data class Unknown(val unknownValue: String) : ClosedCaptionMode(unknownValue)
        

        public class ClosedCaptionModeAdapter : JsonAdapter<ClosedCaptionMode>() {
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
    public sealed class Language(public val value: String) {
            override fun toString(): String = value

            public companion object {
                public fun fromString(s: String): Language = when (s) {
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
            public object Ar : Language("ar")
            public object Auto : Language("auto")
            public object Bg : Language("bg")
            public object Ca : Language("ca")
            public object Cs : Language("cs")
            public object Da : Language("da")
            public object De : Language("de")
            public object El : Language("el")
            public object En : Language("en")
            public object Es : Language("es")
            public object Et : Language("et")
            public object Fi : Language("fi")
            public object Fr : Language("fr")
            public object He : Language("he")
            public object Hi : Language("hi")
            public object Hr : Language("hr")
            public object Hu : Language("hu")
            public object Id : Language("id")
            public object It : Language("it")
            public object Ja : Language("ja")
            public object Ko : Language("ko")
            public object Ms : Language("ms")
            public object Nl : Language("nl")
            public object No : Language("no")
            public object Pl : Language("pl")
            public object Pt : Language("pt")
            public object Ro : Language("ro")
            public object Ru : Language("ru")
            public object Sk : Language("sk")
            public object Sl : Language("sl")
            public object Sv : Language("sv")
            public object Ta : Language("ta")
            public object Th : Language("th")
            public object Tl : Language("tl")
            public object Tr : Language("tr")
            public object Uk : Language("uk")
            public object Zh : Language("zh")
            public data class Unknown(val unknownValue: String) : Language(unknownValue)
        

        public class LanguageAdapter : JsonAdapter<Language>() {
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
    /**
    * Mode Enum
    */
    public sealed class Mode(public val value: String) {
            override fun toString(): String = value

            public companion object {
                public fun fromString(s: String): Mode = when (s) {
                    "auto-on" -> AutoOn
                    "available" -> Available
                    "disabled" -> Disabled
                    else -> Unknown(s)
                }
            }
            public object AutoOn : Mode("auto-on")
            public object Available : Mode("available")
            public object Disabled : Mode("disabled")
            public data class Unknown(val unknownValue: String) : Mode(unknownValue)
        

        public class ModeAdapter : JsonAdapter<Mode>() {
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
}
