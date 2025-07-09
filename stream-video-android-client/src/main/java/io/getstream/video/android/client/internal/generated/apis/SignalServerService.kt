package io.getstream.video.android.client.internal.generated.apis
        
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import stream.video.sfu.signal.SetPublisherRequest
import stream.video.sfu.signal.SendAnswerRequest
import stream.video.sfu.models.ICETrickle
import stream.video.sfu.signal.UpdateSubscriptionsRequest
import stream.video.sfu.signal.UpdateMuteStatesRequest
import stream.video.sfu.signal.ICERestartRequest
import stream.video.sfu.signal.SendStatsRequest
import stream.video.sfu.signal.StartNoiseCancellationRequest
import stream.video.sfu.signal.StopNoiseCancellationRequest
import stream.video.sfu.signal.SetPublisherResponse
import stream.video.sfu.signal.SendAnswerResponse
import stream.video.sfu.signal.ICETrickleResponse
import stream.video.sfu.signal.UpdateSubscriptionsResponse
import stream.video.sfu.signal.UpdateMuteStatesResponse
import stream.video.sfu.signal.ICERestartResponse
import stream.video.sfu.signal.SendStatsResponse
import stream.video.sfu.signal.StartNoiseCancellationResponse
import stream.video.sfu.signal.StopNoiseCancellationResponse
        
public interface SignalServerService {
            
    @Headers("Content-Type: application/protobuf")
    @POST("/twirp/stream.video.sfu.signal.SignalServer/SetPublisher")
    public suspend fun setPublisher(@Body setPublisherRequest: SetPublisherRequest): SetPublisherResponse

    @Headers("Content-Type: application/protobuf")
    @POST("/twirp/stream.video.sfu.signal.SignalServer/SendAnswer")
    public suspend fun sendAnswer(@Body sendAnswerRequest: SendAnswerRequest): SendAnswerResponse

    @Headers("Content-Type: application/protobuf")
    @POST("/twirp/stream.video.sfu.signal.SignalServer/IceTrickle")
    public suspend fun iceTrickle(@Body iCETrickle: ICETrickle): ICETrickleResponse

    @Headers("Content-Type: application/protobuf")
    @POST("/twirp/stream.video.sfu.signal.SignalServer/UpdateSubscriptions")
    public suspend fun updateSubscriptions(@Body updateSubscriptionsRequest: UpdateSubscriptionsRequest): UpdateSubscriptionsResponse

    @Headers("Content-Type: application/protobuf")
    @POST("/twirp/stream.video.sfu.signal.SignalServer/UpdateMuteStates")
    public suspend fun updateMuteStates(@Body updateMuteStatesRequest: UpdateMuteStatesRequest): UpdateMuteStatesResponse

    @Headers("Content-Type: application/protobuf")
    @POST("/twirp/stream.video.sfu.signal.SignalServer/IceRestart")
    public suspend fun iceRestart(@Body iCERestartRequest: ICERestartRequest): ICERestartResponse

    @Headers("Content-Type: application/protobuf")
    @POST("/twirp/stream.video.sfu.signal.SignalServer/SendStats")
    public suspend fun sendStats(@Body sendStatsRequest: SendStatsRequest): SendStatsResponse

    @Headers("Content-Type: application/protobuf")
    @POST("/twirp/stream.video.sfu.signal.SignalServer/StartNoiseCancellation")
    public suspend fun startNoiseCancellation(@Body startNoiseCancellationRequest: StartNoiseCancellationRequest): StartNoiseCancellationResponse

    @Headers("Content-Type: application/protobuf")
    @POST("/twirp/stream.video.sfu.signal.SignalServer/StopNoiseCancellation")
    public suspend fun stopNoiseCancellation(@Body stopNoiseCancellationRequest: StopNoiseCancellationRequest): StopNoiseCancellationResponse
            
}

