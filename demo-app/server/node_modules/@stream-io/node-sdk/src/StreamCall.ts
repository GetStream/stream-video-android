import { StreamClient } from './StreamClient';
import {
  DeleteRecordingRequest,
  DeleteTranscriptionRequest,
  GetCallRequest,
  GetCallStatsRequest,
  ProductvideoApi,
  VideoBlockUserRequest,
  VideoDeleteCallRequest,
  VideoGetOrCreateCallRequest,
  VideoGoLiveRequest,
  VideoMuteUsersRequest,
  VideoPinRequest,
  VideoQueryCallMembersRequest,
  VideoSendCallEventRequest,
  VideoStartRecordingRequest,
  VideoStartTranscriptionRequest,
  VideoUnblockUserRequest,
  VideoUnpinRequest,
  VideoUpdateCallMembersRequest,
  VideoUpdateCallRequest,
  VideoUpdateUserPermissionsRequest,
} from './gen/video';
import { OmitTypeId } from './types';

export class StreamCall {
  private readonly baseRequest: { type: string; id: string };
  private readonly apiClient: ProductvideoApi;

  constructor(
    private readonly streamClient: StreamClient,
    public readonly type: string,
    public readonly id: string,
  ) {
    this.baseRequest = { id: this.id, type: this.type };
    const configuration = this.streamClient.getConfiguration('video');
    this.apiClient = new ProductvideoApi(configuration);
  }

  get cid() {
    return `${this.type}:${this.id}`;
  }

  blockUser = (videoBlockUserRequest: VideoBlockUserRequest) => {
    return this.apiClient.blockUser({
      ...this.baseRequest,
      videoBlockUserRequest,
    });
  };

  endCall = () => {
    return this.apiClient.endCall({ ...this.baseRequest });
  };

  get = (request?: OmitTypeId<GetCallRequest>) => {
    return this.apiClient.getCall({ ...(request ?? {}), ...this.baseRequest });
  };

  delete = (videoDeleteCallRequest?: VideoDeleteCallRequest) => {
    return this.apiClient.deleteCall({
      ...this.baseRequest,
      videoDeleteCallRequest: videoDeleteCallRequest ?? null,
    });
  };

  getOrCreate = (videoGetOrCreateCallRequest?: VideoGetOrCreateCallRequest) => {
    return this.apiClient.getOrCreateCall({
      ...this.baseRequest,
      videoGetOrCreateCallRequest: videoGetOrCreateCallRequest ?? {},
    });
  };

  getSessionStatistics = (request: OmitTypeId<GetCallStatsRequest>) => {
    return this.apiClient.getCallStats({ ...this.baseRequest, ...request });
  };

  create = (getOrCreateCallRequest?: VideoGetOrCreateCallRequest) => {
    return this.getOrCreate(getOrCreateCallRequest);
  };

  goLive = (videoGoLiveRequest?: VideoGoLiveRequest) => {
    return this.apiClient.goLive({
      ...this.baseRequest,
      videoGoLiveRequest: videoGoLiveRequest ?? {},
    });
  };

  listRecordings = () => {
    return this.apiClient.listRecordings({
      ...this.baseRequest,
    });
  };

  deleteRecording = (request: OmitTypeId<DeleteRecordingRequest>) => {
    return this.apiClient.deleteRecording({ ...this.baseRequest, ...request });
  };

  listTranscriptions = () => {
    return this.apiClient.listTranscriptions({
      ...this.baseRequest,
    });
  };

  muteUsers = (videoMuteUsersRequest: VideoMuteUsersRequest) => {
    return this.apiClient.muteUsers({
      ...this.baseRequest,
      videoMuteUsersRequest,
    });
  };

  queryMembers = (request?: OmitTypeId<VideoQueryCallMembersRequest>) => {
    return this.apiClient.queryCallMembers({
      videoQueryCallMembersRequest: { ...(request ?? {}), ...this.baseRequest },
    });
  };

  sendCustomEvent = (videoSendCallEventRequest: VideoSendCallEventRequest) => {
    return this.apiClient.sendCallEvent({
      videoSendCallEventRequest,
      ...this.baseRequest,
    });
  };

  startHLSBroadcasting = () => {
    return this.apiClient.startHLSBroadcasting({ ...this.baseRequest });
  };

  startRecording = (request?: VideoStartRecordingRequest) => {
    return this.apiClient.startRecording({
      ...this.baseRequest,
      videoStartRecordingRequest: request ?? {},
    });
  };

  startTranscription = (
    videoStartTranscriptionRequest: VideoStartTranscriptionRequest = {},
  ) => {
    return this.apiClient.startTranscription({
      ...this.baseRequest,
      videoStartTranscriptionRequest,
    });
  };

  deleteTranscription = (request: OmitTypeId<DeleteTranscriptionRequest>) => {
    return this.apiClient.deleteTranscription({
      ...this.baseRequest,
      ...request,
    });
  };

  stopHLSBroadcasting = () => {
    return this.apiClient.stopHLSBroadcasting({ ...this.baseRequest });
  };

  stopLive = () => {
    return this.apiClient.stopLive({ ...this.baseRequest });
  };

  stopRecording = () => {
    return this.apiClient.stopRecording({ ...this.baseRequest });
  };

  stopTranscription = () => {
    return this.apiClient.stopTranscription({ ...this.baseRequest });
  };

  unblockUser = (videoUnblockUserRequest: VideoUnblockUserRequest) => {
    return this.apiClient.unblockUser({
      videoUnblockUserRequest,
      ...this.baseRequest,
    });
  };

  update = (videoUpdateCallRequest: VideoUpdateCallRequest) => {
    return this.apiClient.updateCall({
      videoUpdateCallRequest,
      ...this.baseRequest,
    });
  };

  updateCallMembers = (
    videoUpdateCallMembersRequest: VideoUpdateCallMembersRequest,
  ) => {
    return this.apiClient.updateCallMembers({
      videoUpdateCallMembersRequest,
      ...this.baseRequest,
    });
  };

  updateUserPermissions = (
    videoUpdateUserPermissionsRequest: VideoUpdateUserPermissionsRequest,
  ) => {
    return this.apiClient.updateUserPermissions({
      videoUpdateUserPermissionsRequest,
      ...this.baseRequest,
    });
  };

  pinVideo = (videoPinRequest: VideoPinRequest) => {
    return this.apiClient.videoPin({ videoPinRequest, ...this.baseRequest });
  };

  unpinVideo = (videoUnpinRequest: VideoUnpinRequest) => {
    return this.apiClient.videoUnpin({
      videoUnpinRequest,
      ...this.baseRequest,
    });
  };
}
