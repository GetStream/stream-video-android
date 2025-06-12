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

package io.getstream.video.android.core.model

import androidx.compose.runtime.Stable
import io.getstream.android.push.PushDevice
import io.getstream.android.push.PushProvider
import io.getstream.android.video.generated.models.CreateDeviceRequest
import io.getstream.android.video.generated.models.SortParamRequest
import io.getstream.result.Error
import io.getstream.result.Result

/**
 * Represents the data required to apply a sorting method to queries.
 *
 * @param direction The direction of sorting. `-1` for descending, `1` for ascending.
 * @param sortField The property by which to apply sorting. Options depend on the object you're
 * querying.
 */
@Stable
public data class SortData(
    public val direction: Int = DEFAULT_SORT_DIRECTION,
    public val sortField: String,
)

@Stable
public sealed class SortField(public val field: String, public val ascending: Boolean = true) {

    @Stable
    public class Asc(field: String) : SortField(field, true)

    @Stable
    public class Desc(field: String) : SortField(field, false)
}

public fun SortField.toRequest(): SortParamRequest {
    val direction = if (ascending) 1 else -1
    return SortParamRequest(
        direction = direction,
        field = field,
    )
}

/**
 * Maps the data to the request for the BE.
 */
public fun SortData.toRequest(): SortParamRequest {
    return SortParamRequest(
        direction = direction,
        field = sortField,
    )
}

internal fun PushDevice.toCreateDeviceRequest(): Result<CreateDeviceRequest> =
    when (pushProvider) {
        PushProvider.FIREBASE -> Result.Success(CreateDeviceRequest.PushProvider.Firebase)
        PushProvider.HUAWEI -> Result.Success(CreateDeviceRequest.PushProvider.Huawei)
        PushProvider.XIAOMI -> Result.Success(CreateDeviceRequest.PushProvider.Xiaomi)
        PushProvider.UNKNOWN -> Result.Failure(Error.GenericError("Unsupported PushProvider"))
    }.map {
        CreateDeviceRequest(
            id = token,
            pushProvider = it,
            pushProviderName = providerName,
        )
    }

/**
 * Represents the default sorting direction.
 */
private const val DEFAULT_SORT_DIRECTION = 1
