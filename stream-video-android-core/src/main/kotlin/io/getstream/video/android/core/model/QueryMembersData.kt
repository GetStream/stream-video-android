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

package io.getstream.video.android.core.model

import org.openapitools.client.models.QueryMembersRequest

/**
 * Represents the data to query members.
 *
 * @param streamCallCid The CID of the call in which the users are members.
 * @param filters A map of filter keys and values to use for querying.
 * @param sort Sorting field and direction information.
 * @param limit The number of items to fetch.
 * @param next The value of the next item in pagination as described in [sort].
 * @param previous The value of the previous item in pagination as described in [sort].
 */
public data class QueryMembersData(
    public val streamCallCid: StreamCallCid,
    public val filters: Map<String, Any>,
    public val sort: SortData = SortData(sortField = DEFAULT_QUERY_MEMBER_SORT),
    public val limit: Int = DEFAULT_QUERY_MEMBERS_LIMIT,
    public val next: String? = null,
    public val previous: String? = null
)

/**
 * Maps the data to the request for the BE.
 *
 * @param id The ID of the call.
 * @param type The type of the call.
 */
public fun QueryMembersData.toRequest(
    id: String,
    type: String
): QueryMembersRequest {
    return QueryMembersRequest(
        filterConditions = filters,
        sort = listOf(sort.toRequest()),
        id = id,
        type = type,
        limit = limit,
        next = next,
        prev = previous
    )
}

private const val DEFAULT_QUERY_MEMBER_SORT = "user_id"
private const val DEFAULT_QUERY_MEMBERS_LIMIT = 30
