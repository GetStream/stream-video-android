package org.openapitools.client.models

import com.squareup.moshi.Json

data class StopClosedCaptionResponse(/* Duration of the request in human-readable format */
                                      @Json(name = "duration")
                                      val duration: kotlin.String
)