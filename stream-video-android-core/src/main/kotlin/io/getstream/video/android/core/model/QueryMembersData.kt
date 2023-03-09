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
