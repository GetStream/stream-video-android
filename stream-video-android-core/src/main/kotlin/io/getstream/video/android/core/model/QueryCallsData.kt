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

import org.openapitools.client.models.QueryCallsRequest

/**
 * Represents the data used to query calls according to the presented filters and sorting.
 *
 * @param filters The filters to be used to select calls.
 * @param sort Sorting information, such as direction and sort field.
 * @param limit The limit of items to fetch.
 * @param next What the next value by sort is.
 * @param previous What the previous value by sort is.
 */
public data class QueryCallsData(
    public val filters: Map<String, Any>,
    public val sort: SortData = SortData(sortField = DEFAULT_QUERY_CALLS_SORT),
    public val limit: Int = DEFAULT_QUERY_CALLS_LIMIT,
    public val next: String? = null,
    public val previous: String? = null
)

/**
 * Maps the data to the request for the BE.
 */
public fun QueryCallsData.toRequest(): QueryCallsRequest {
    return QueryCallsRequest(
        filterConditions = filters,
        sort = listOf(sort.toRequest()),
        limit = limit,
        next = next,
        prev = previous
    )
}

// TODO: have a better default
private const val DEFAULT_QUERY_CALLS_SORT = "cid"
private const val DEFAULT_QUERY_CALLS_LIMIT = 25
