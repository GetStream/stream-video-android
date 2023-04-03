package org.openapitools.client.apis

import org.openapitools.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.squareup.moshi.Json

import org.openapitools.client.models.APIError
import org.openapitools.client.models.BlockUserRequest
import org.openapitools.client.models.BlockUserResponse
import org.openapitools.client.models.MuteUsersRequest
import org.openapitools.client.models.MuteUsersResponse
import org.openapitools.client.models.RequestPermissionRequest
import org.openapitools.client.models.RequestPermissionResponse
import org.openapitools.client.models.UnblockUserRequest
import org.openapitools.client.models.UnblockUserResponse
import org.openapitools.client.models.UpdateUserPermissionsRequest
import org.openapitools.client.models.UpdateUserPermissionsResponse

interface ModerationApi {
    /**
     * Block user on a call
     * Block a user, preventing them from joining the call until they are unblocked.  Sends events: - call.blocked_user  Required permissions: - BlockUser 
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type 
     * @param id 
     * @param blockUserRequest 
     * @return [BlockUserResponse]
     */
    @POST("/video/call/{type}/{id}/block")
    suspend fun blockUser(@Path("type") type: String, @Path("id") id: String, @Body blockUserRequest: BlockUserRequest): BlockUserResponse

    /**
     * Mute users
     * Mutes users in a call  Required permissions: - MuteUsers 
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type 
     * @param id 
     * @param muteUsersRequest 
     * @return [MuteUsersResponse]
     */
    @POST("/video/call/{type}/{id}/mute_users")
    suspend fun muteUsers(@Path("type") type: String, @Path("id") id: String, @Body muteUsersRequest: MuteUsersRequest): MuteUsersResponse

    /**
     * Request permission
     * Request permission to perform an action  Sends events: - call.permission_request 
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type 
     * @param id 
     * @param requestPermissionRequest 
     * @return [RequestPermissionResponse]
     */
    @POST("/video/call/{type}/{id}/request_permission")
    suspend fun requestPermission(@Path("type") type: String, @Path("id") id: String, @Body requestPermissionRequest: RequestPermissionRequest): RequestPermissionResponse

    /**
     * Unblocks user on a call
     * Removes the block for a user on a call. The user will be able to join the call again.  Sends events: - call.unblocked_user  Required permissions: - BlockUser 
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type 
     * @param id 
     * @param unblockUserRequest 
     * @return [UnblockUserResponse]
     */
    @POST("/video/call/{type}/{id}/unblock")
    suspend fun unblockUser(@Path("type") type: String, @Path("id") id: String, @Body unblockUserRequest: UnblockUserRequest): UnblockUserResponse

    /**
     * Update user permissions
     * Updates user permissions  Sends events: - call.permissions_updated  Required permissions: - UpdateCallPermissions 
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type 
     * @param id 
     * @param updateUserPermissionsRequest 
     * @return [UpdateUserPermissionsResponse]
     */
    @POST("/video/call/{type}/{id}/user_permissions")
    suspend fun updateUserPermissions(@Path("type") type: String, @Path("id") id: String, @Body updateUserPermissionsRequest: UpdateUserPermissionsRequest): UpdateUserPermissionsResponse

}
