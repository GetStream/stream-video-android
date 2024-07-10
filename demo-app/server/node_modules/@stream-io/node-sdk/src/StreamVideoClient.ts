import { StreamCall } from './StreamCall';
import { StreamClient } from './StreamClient';
import {
  CheckExternalStorageRequest,
  DeleteCallTypeRequest,
  DeleteExternalStorageRequest,
  GetCallTypeRequest,
  ProductvideoApi,
  VideoCreateCallTypeRequest,
  VideoCreateExternalStorageRequest,
  VideoQueryCallStatsRequest,
  VideoQueryCallsRequest,
  VideoUpdateCallTypeRequest,
  VideoUpdateExternalStorageRequest,
} from './gen/video';

export class StreamVideoClient {
  private readonly apiClient: ProductvideoApi;

  constructor(private readonly streamClient: StreamClient) {
    const configuration = this.streamClient.getConfiguration('video');
    this.apiClient = new ProductvideoApi(configuration);
  }

  call = (type: string, id: string) => {
    return new StreamCall(this.streamClient, type, id);
  };

  queryCalls = (request?: VideoQueryCallsRequest) => {
    return this.apiClient.queryCalls({
      videoQueryCallsRequest: request ?? {},
    });
  };

  queryCallStatistics = (
    videoQueryCallStatsRequest?: VideoQueryCallStatsRequest,
  ) => {
    return this.apiClient.queryCallStats({
      videoQueryCallStatsRequest: videoQueryCallStatsRequest ?? {},
    });
  };

  createCallType = (videoCreateCallTypeRequest: VideoCreateCallTypeRequest) => {
    return this.apiClient.createCallType({
      videoCreateCallTypeRequest,
    });
  };

  deleteCallType = (request: DeleteCallTypeRequest) => {
    return this.apiClient.deleteCallType(request);
  };

  getCallType = (request: GetCallTypeRequest) => {
    return this.apiClient.getCallType(request);
  };

  listCallTypes = () => {
    return this.apiClient.listCallTypes();
  };

  updateCallType = (
    name: string,
    videoUpdateCallTypeRequest: VideoUpdateCallTypeRequest,
  ) => {
    return this.apiClient.updateCallType({
      name,
      videoUpdateCallTypeRequest,
    });
  };

  listExternalStorages = () => {
    return this.apiClient.listExternalStorage();
  };

  createExternalStorage = (
    videoCreateExternalStorageRequest: VideoCreateExternalStorageRequest,
  ) => {
    return this.apiClient.createExternalStorage({
      videoCreateExternalStorageRequest,
    });
  };

  deleteExternalStorage = (request: DeleteExternalStorageRequest) => {
    return this.apiClient.deleteExternalStorage(request);
  };

  updateExternalStorage = (
    name: string,
    videoUpdateExternalStorageRequest: VideoUpdateExternalStorageRequest,
  ) => {
    return this.apiClient.updateExternalStorage({
      name,
      videoUpdateExternalStorageRequest,
    });
  };

  checkExternalStorage = (request: CheckExternalStorageRequest) => {
    return this.apiClient.checkExternalStorage(request);
  };
}
