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

import com.squareup.moshi.Json

/**
 * *
 * @param statusCode Response HTTP status code
 * @param code API error code
 * @param details Additional error-specific information
 * @param duration Request duration
 * @param message Message describing an error
 * @param moreInfo URL with additional information
 * @param exceptionFields Additional error info
 */

data class APIError(

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

) {

    /**
     * API error code
     *
     * Values: internalError,accessKeyError,inputError,authFailed,duplicateUsername,rateLimited,notFound,notAllowed,eventNotSupported,channelFeatureNotSupported,messageTooLong,multipleNestingLevel,payloadTooBig,expiredToken,tokenNotValidYet,tokenUsedBeforeIat,invalidTokenSignature,customCommandEndpointMissing,customCommandEndpointEqualCallError,connectionIdNotFound,coolDown,queryChannelPermissionsMismatch,tooManyConnections,notSupportedInPushV1,moderationFailed,videoProviderNotConfigured,videoInvalidCallId,videoCreateCallFailed,appSuspended,videoNoDatacentersAvailable,videoJoinCallFailure,queryCallsPermissionsMismatch
     */
    enum class Code(val value: kotlin.String) {
        @Json(name = "internal-error") internalError("internal-error"),
        @Json(name = "access-key-error") accessKeyError("access-key-error"),
        @Json(name = "input-error") inputError("input-error"),
        @Json(name = "auth-failed") authFailed("auth-failed"),
        @Json(name = "duplicate-username") duplicateUsername("duplicate-username"),
        @Json(name = "rate-limited") rateLimited("rate-limited"),
        @Json(name = "not-found") notFound("not-found"),
        @Json(name = "not-allowed") notAllowed("not-allowed"),
        @Json(name = "event-not-supported") eventNotSupported("event-not-supported"),
        @Json(name = "channel-feature-not-supported") channelFeatureNotSupported("channel-feature-not-supported"),
        @Json(name = "message-too-long") messageTooLong("message-too-long"),
        @Json(name = "multiple-nesting-level") multipleNestingLevel("multiple-nesting-level"),
        @Json(name = "payload-too-big") payloadTooBig("payload-too-big"),
        @Json(name = "expired-token") expiredToken("expired-token"),
        @Json(name = "token-not-valid-yet") tokenNotValidYet("token-not-valid-yet"),
        @Json(name = "token-used-before-iat") tokenUsedBeforeIat("token-used-before-iat"),
        @Json(name = "invalid-token-signature") invalidTokenSignature("invalid-token-signature"),
        @Json(name = "custom-command-endpoint-missing") customCommandEndpointMissing("custom-command-endpoint-missing"),
        @Json(name = "custom-command-endpoint=call-error") customCommandEndpointEqualCallError("custom-command-endpoint=call-error"),
        @Json(name = "connection-id-not-found") connectionIdNotFound("connection-id-not-found"),
        @Json(name = "cool-down") coolDown("cool-down"),
        @Json(name = "query-channel-permissions-mismatch") queryChannelPermissionsMismatch("query-channel-permissions-mismatch"),
        @Json(name = "too-many-connections") tooManyConnections("too-many-connections"),
        @Json(name = "not-supported-in-push-v1") notSupportedInPushV1("not-supported-in-push-v1"),
        @Json(name = "moderation-failed") moderationFailed("moderation-failed"),
        @Json(name = "video-provider-not-configured") videoProviderNotConfigured("video-provider-not-configured"),
        @Json(name = "video-invalid-call-id") videoInvalidCallId("video-invalid-call-id"),
        @Json(name = "video-create-call-failed") videoCreateCallFailed("video-create-call-failed"),
        @Json(name = "app-suspended") appSuspended("app-suspended"),
        @Json(name = "video-no-datacenters-available") videoNoDatacentersAvailable("video-no-datacenters-available"),
        @Json(name = "video-join-call-failure") videoJoinCallFailure("video-join-call-failure"),
        @Json(name = "query-calls-permissions-mismatch") queryCallsPermissionsMismatch("query-calls-permissions-mismatch");
    }
}
