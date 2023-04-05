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
 * The discriminator object for all websocket events, you should use this to map event payloads to their own type
 *
 * @param callCid * @param createdAt * @param type The type of event: \"call.permissions_updated\" in this case
 * @param user * @param call * @param members the members added to this call
 * @param ringing true when the call was created with ring enabled
 * @param reaction * @param capabilitiesByRole The capabilities by role for this call
 * @param custom Custom data for this object
 * @param connectionId The connection_id for this client
 * @param permissions The list of permissions requested by the user
 * @param ownCapabilities The capabilities of the current user
 * @param blockedByUser * @param me */

interface WSEvent {

    @Json(name = "call_cid")
    val callCid: kotlin.String
    @Json(name = "created_at")
    val createdAt: java.time.OffsetDateTime
    /* The type of event: \"call.permissions_updated\" in this case */
    @Json(name = "type")
    val type: kotlin.String
    @Json(name = "user")
    val user: UserResponse
    @Json(name = "call")
    val call: CallResponse
    /* the members added to this call */
    @Json(name = "members")
    val members: kotlin.collections.List<MemberResponse>
    /* true when the call was created with ring enabled */
    @Json(name = "ringing")
    val ringing: kotlin.Boolean
    @Json(name = "reaction")
    val reaction: ReactionResponse
    /* The capabilities by role for this call */
    @Json(name = "capabilities_by_role")
    val capabilitiesByRole: kotlin.collections.Map<kotlin.String, kotlin.collections.List<kotlin.String>>
    /* Custom data for this object */
    @Json(name = "custom")
    val custom: kotlin.collections.Map<kotlin.String, kotlin.Any>
    /* The connection_id for this client */
    @Json(name = "connection_id")
    val connectionId: kotlin.String
    /* The list of permissions requested by the user */
    @Json(name = "permissions")
    val permissions: kotlin.collections.List<kotlin.String>
    /* The capabilities of the current user */
    @Json(name = "own_capabilities")
    val ownCapabilities: kotlin.collections.List<OwnCapability>
    @Json(name = "blocked_by_user")
    val blockedByUser: UserResponse?
    @Json(name = "me")
    val me: OwnUserResponse?
}
