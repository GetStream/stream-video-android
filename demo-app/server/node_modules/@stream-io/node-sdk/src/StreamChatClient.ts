import { StreamChannel } from './StreamChannel';
import { StreamClient } from './StreamClient';
import {
  CreateBlockListRequest,
  CreateChannelTypeRequest,
  CreateCommandRequest,
  DeleteBlockListRequest,
  DeleteChannelTypeRequest,
  DeleteCommandRequest,
  ExportChannelsRequest,
  GetBlockListRequest,
  GetChannelTypeRequest,
  GetCommandRequest,
  GetExportChannelsStatusRequest,
  ProductchatApi,
  QueryChannelsRequest,
  SearchRequest,
  UpdateBlockListRequest,
  UpdateChannelTypeRequest,
  UpdateCommandRequest,
} from './gen/chat';

export class StreamChatClient {
  private readonly chatApi: ProductchatApi;

  constructor(private readonly streamClient: StreamClient) {
    const configuration = this.streamClient.getConfiguration();
    /** @ts-expect-error */
    this.chatApi = new ProductchatApi(configuration);
  }

  channel = (type: string, id?: string) => {
    return new StreamChannel(this.streamClient, type, id);
  };

  createBlockList = (createBlockListRequest: CreateBlockListRequest) => {
    return this.chatApi.createBlockList({ createBlockListRequest });
  };

  listBlockLists = () => {
    return this.chatApi.listBlockLists();
  };

  getBlockList = (request: GetBlockListRequest) => {
    return this.chatApi.getBlockList(request);
  };

  updateBlockList = (
    name: string,
    updateBlockListRequest: UpdateBlockListRequest,
  ) => {
    return this.chatApi.updateBlockList({ name, updateBlockListRequest });
  };

  deleteBlockList = (request: DeleteBlockListRequest) => {
    return this.chatApi.deleteBlockList(request);
  };

  createChannelType = (createChannelTypeRequest: CreateChannelTypeRequest) => {
    return this.chatApi.createChannelType({ createChannelTypeRequest });
  };

  deleteChannelType = (request: DeleteChannelTypeRequest) => {
    return this.chatApi.deleteChannelType(request);
  };

  getChannelType = (request: GetChannelTypeRequest) => {
    return this.chatApi.getChannelType(request);
  };

  listChannelTypes = () => {
    return this.chatApi.listChannelTypes();
  };

  updateChannelType = (
    name: string,
    updateChannelTypeRequest: UpdateChannelTypeRequest,
  ) => {
    return this.chatApi.updateChannelType({
      name,
      updateChannelTypeRequest,
    });
  };

  queryChannels = (queryChannelsRequest?: QueryChannelsRequest) => {
    return this.chatApi.queryChannels({
      queryChannelsRequest: queryChannelsRequest ?? {},
    });
  };

  searchMessages = (payload?: SearchRequest) => {
    return this.chatApi.search({ payload });
  };

  exportChannels = (exportChannelsRequest?: ExportChannelsRequest) => {
    return this.chatApi.exportChannels({
      exportChannelsRequest: exportChannelsRequest ?? null,
    });
  };

  getExportStatus = (request: GetExportChannelsStatusRequest) => {
    return this.chatApi.getExportChannelsStatus(request);
  };

  listCommands = () => {
    return this.chatApi.listCommands();
  };

  createCommand = (createCommandRequest: CreateCommandRequest) => {
    return this.chatApi.createCommand({ createCommandRequest });
  };

  getCommand = (getCommandRequest: GetCommandRequest) => {
    return this.chatApi.getCommand(getCommandRequest);
  };

  updateCommand = (
    name: string,
    updateCommandRequest: UpdateCommandRequest,
  ) => {
    return this.chatApi.updateCommand({ name, updateCommandRequest });
  };

  deleteCommand = (request: DeleteCommandRequest) => {
    return this.chatApi.deleteCommand(request);
  };
}
