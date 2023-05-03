/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

package org.openapitools.client.models





import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import org.openapitools.client.infrastructure.Serializer

/**
 *
 *
 * @param statusCode Response HTTP status code
 * @param code API error code
 * @param details Additional error-specific information
 * @param duration Request duration
 * @param message Message describing an error
 * @param moreInfo URL with additional information
 * @param exceptionFields Additional error info
 */


data class APIError (

    /* Response HTTP status code */
    @Json(name = "StatusCode")
    val statusCode: kotlin.Int,

    /* API error code */
    @Json(name = "code")
    val code: APIError.Code,

    /* Additional error-specific information */
    @Json(name = "details")
    val details: kotlin.collections.List<kotlin.Int>,

    /* Request duration */
    @Json(name = "duration")
    val duration: kotlin.String,

    /* Message describing an error */
    @Json(name = "message")
    val message: kotlin.String,

    /* URL with additional information */
    @Json(name = "more_info")
    val moreInfo: kotlin.String,

    /* Additional error info */
    @Json(name = "exception_fields")
    val exceptionFields: kotlin.collections.Map<kotlin.String, kotlin.String>? = null

)

{

    /**
     * API error code
     *
     * Values: internalError,accessKeyError,inputError,authFailed,duplicateUsername,rateLimited,notFound,notAllowed,eventNotSupported,channelFeatureNotSupported,messageTooLong,multipleNestingLevel,payloadTooBig,expiredToken,tokenNotValidYet,tokenUsedBeforeIat,invalidTokenSignature,customCommandEndpointMissing,customCommandEndpointEqualCallError,connectionIdNotFound,coolDown,queryChannelPermissionsMismatch,tooManyConnections,notSupportedInPushV1,moderationFailed,videoProviderNotConfigured,videoInvalidCallId,videoCreateCallFailed,appSuspended,videoNoDatacentersAvailable,videoJoinCallFailure,queryCallsPermissionsMismatch
     */

    sealed class Code(val value: kotlin.String) {
        override fun toString(): String = value

        companion object {
            fun fromString(s: kotlin.String): Code = when (s) {
                "internal-error" -> InternalError
                "access-key-error" -> AccessKeyError
                "input-error" -> InputError
                "auth-failed" -> AuthFailed
                "duplicate-username" -> DuplicateUsername
                "rate-limited" -> RateLimited
                "not-found" -> NotFound
                "not-allowed" -> NotAllowed
                "event-not-supported" -> EventNotSupported
                "channel-feature-not-supported" -> ChannelFeatureNotSupported
                "message-too-long" -> MessageTooLong
                "multiple-nesting-level" -> MultipleNestingLevel
                "payload-too-big" -> PayloadTooBig
                "expired-token" -> ExpiredToken
                "token-not-valid-yet" -> TokenNotValidYet
                "token-used-before-iat" -> TokenUsedBeforeIat
                "invalid-token-signature" -> InvalidTokenSignature
                "custom-command-endpoint-missing" -> CustomCommandEndpointMissing
                "custom-command-endpoint=call-error" -> CustomCommandEndpointEqualCallError
                "connection-id-not-found" -> ConnectionIdNotFound
                "cool-down" -> CoolDown
                "query-channel-permissions-mismatch" -> QueryChannelPermissionsMismatch
                "too-many-connections" -> TooManyConnections
                "not-supported-in-push-v1" -> NotSupportedInPushV1
                "moderation-failed" -> ModerationFailed
                "video-provider-not-configured" -> VideoProviderNotConfigured
                "video-invalid-call-id" -> VideoInvalidCallId
                "video-create-call-failed" -> VideoCreateCallFailed
                "app-suspended" -> AppSuspended
                "video-no-datacenters-available" -> VideoNoDatacentersAvailable
                "video-join-call-failure" -> VideoJoinCallFailure
                "query-calls-permissions-mismatch" -> QueryCallsPermissionsMismatch
                else -> Unknown(s)
            }
        }

        object InternalError : Code("internal-error")
        object AccessKeyError : Code("access-key-error")
        object InputError : Code("input-error")
        object AuthFailed : Code("auth-failed")
        object DuplicateUsername : Code("duplicate-username")
        object RateLimited : Code("rate-limited")
        object NotFound : Code("not-found")
        object NotAllowed : Code("not-allowed")
        object EventNotSupported : Code("event-not-supported")
        object ChannelFeatureNotSupported : Code("channel-feature-not-supported")
        object MessageTooLong : Code("message-too-long")
        object MultipleNestingLevel : Code("multiple-nesting-level")
        object PayloadTooBig : Code("payload-too-big")
        object ExpiredToken : Code("expired-token")
        object TokenNotValidYet : Code("token-not-valid-yet")
        object TokenUsedBeforeIat : Code("token-used-before-iat")
        object InvalidTokenSignature : Code("invalid-token-signature")
        object CustomCommandEndpointMissing : Code("custom-command-endpoint-missing")
        object CustomCommandEndpointEqualCallError : Code("custom-command-endpoint=call-error")
        object ConnectionIdNotFound : Code("connection-id-not-found")
        object CoolDown : Code("cool-down")
        object QueryChannelPermissionsMismatch : Code("query-channel-permissions-mismatch")
        object TooManyConnections : Code("too-many-connections")
        object NotSupportedInPushV1 : Code("not-supported-in-push-v1")
        object ModerationFailed : Code("moderation-failed")
        object VideoProviderNotConfigured : Code("video-provider-not-configured")
        object VideoInvalidCallId : Code("video-invalid-call-id")
        object VideoCreateCallFailed : Code("video-create-call-failed")
        object AppSuspended : Code("app-suspended")
        object VideoNoDatacentersAvailable : Code("video-no-datacenters-available")
        object VideoJoinCallFailure : Code("video-join-call-failure")
        object QueryCallsPermissionsMismatch : Code("query-calls-permissions-mismatch")
        data class Unknown(val unknownValue: kotlin.String) : Code(unknownValue)

        class CodeAdapter : JsonAdapter<Code>() {
            @FromJson
            override fun fromJson(reader: JsonReader): Code? {
                val s = reader.nextString() ?: return null
                return fromString(s)
            }

            @ToJson
            override fun toJson(writer: JsonWriter, value: Code?) {
                writer.value(value?.value)
            }
        }
    }



}
