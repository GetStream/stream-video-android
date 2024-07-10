import { StreamClient } from './StreamClient';
import {
  ChannelGetOrCreateRequest,
  DeleteChannelRequest,
  DeleteMessageRequest,
  DeleteReactionRequest,
  EventRequest,
  GetManyMessagesRequest,
  GetMessageRequest,
  GetOGRequest,
  GetReactionsRequest,
  GetRepliesRequest,
  HideChannelRequest,
  MarkReadRequest,
  MarkUnreadRequest,
  MuteChannelRequest,
  ProductchatApi,
  QueryMembersRequest,
  SendMessageRequest,
  SendReactionRequest,
  ShowChannelRequest,
  TranslateMessageRequest,
  TruncateChannelRequest,
  UnmuteChannelRequest,
  UpdateChannelPartialRequest,
  UpdateChannelRequest,
  UpdateMessagePartialRequest,
  UpdateMessageRequest,
} from './gen/chat';
import { OmitTypeId } from './types';

export class StreamChannel {
  private readonly chatApi: ProductchatApi;

  constructor(
    private readonly streamClient: StreamClient,
    public readonly type: string,
    public id?: string,
  ) {
    const configuration = this.streamClient.getConfiguration();
    /** @ts-expect-error */
    this.chatApi = new ProductchatApi(configuration);
  }

  get cid() {
    return `${this.baseRequest.type}:${this.baseRequest.id}`;
  }

  delete = (request?: OmitTypeId<DeleteChannelRequest>) => {
    return this.chatApi.deleteChannel({
      ...this.baseRequest,
      ...(request ?? {}),
    });
  };

  update = (updateChannelRequest: OmitTypeId<UpdateChannelRequest>) => {
    return this.chatApi.updateChannel({
      ...this.baseRequest,
      updateChannelRequest,
    });
  };

  updatePartial = (
    updateChannelPartialRequest: OmitTypeId<UpdateChannelPartialRequest>,
  ) => {
    return this.chatApi.updateChannelPartial({
      ...this.baseRequest,
      updateChannelPartialRequest,
    });
  };

  getOrCreate = async (
    channelGetOrCreateRequest?: ChannelGetOrCreateRequest,
  ) => {
    if (this.id) {
      return await this.chatApi.getOrCreateChannel({
        ...this.baseRequest,
        channelGetOrCreateRequest: channelGetOrCreateRequest ?? {},
      });
    } else {
      if (!channelGetOrCreateRequest?.data?.members) {
        throw new Error(
          'You need to provide members to create a channel without ID',
        );
      }
      const response = await this.chatApi.getOrCreateDistinctChannel({
        type: this.type,
        channelGetOrCreateRequest,
      });
      this.id = response.channel?.id;
      return response;
    }
  };

  markRead = (markReadRequest: MarkReadRequest) => {
    return this.chatApi.markRead({ ...this.baseRequest, markReadRequest });
  };

  markUnread = (markUnreadRequest: MarkUnreadRequest) => {
    return this.chatApi.markUnread({
      ...this.baseRequest,
      markUnreadRequest,
    });
  };

  show = (showChannelRequest: ShowChannelRequest) => {
    return this.chatApi.showChannel({
      ...this.baseRequest,
      showChannelRequest,
    });
  };

  hide = (hideChannelRequest: HideChannelRequest) => {
    return this.chatApi.hideChannel({
      ...this.baseRequest,
      hideChannelRequest,
    });
  };

  truncate = (truncateChannelRequest: TruncateChannelRequest) => {
    return this.chatApi.truncateChannel({
      ...this.baseRequest,
      truncateChannelRequest,
    });
  };

  queryMembers = (request: OmitTypeId<QueryMembersRequest>) => {
    return this.chatApi.queryMembers({
      payload: { ...this.baseRequest, ...request },
    });
  };

  mute = (muteChannelRequest: Omit<MuteChannelRequest, 'channel_cids'>) => {
    return this.chatApi.muteChannel({
      muteChannelRequest: { ...muteChannelRequest, channel_cids: [this.cid] },
    });
  };

  unmute = (
    unmuteChannelRequest: Omit<
      UnmuteChannelRequest,
      'channel_cids' | 'channel_cid'
    >,
  ) => {
    return this.chatApi.unmuteChannel({
      unmuteChannelRequest: {
        ...unmuteChannelRequest,
        channel_cids: [this.cid],
      },
    });
  };

  // TODO: there is probably an issue with the generated code here
  // uploadFile = (options: Omit<OmitTypeId<UploadFileRequest>, 'file'>, file: Buffer) => {
  //   return this.chatApi.uploadFile({...options, ...this.baseRequest, file: file as any as string});
  // }

  // deleteFile = (request: OmitTypeId<DeleteFileRequest>) => {
  //   return this.chatApi.deleteFile({...request, ...this.baseRequest});
  // }

  // uploadImage = (request: OmitTypeId<UploadImageRequest>) => {
  //   return this.chatApi.uploadImage({...request, ...this.baseRequest});
  // }

  // deleteImage = (request: OmitTypeId<DeleteImageRequest>) => {
  //   return this.chatApi.deleteImage({...request, ...this.baseRequest});
  // }

  sendMessage = (sendMessageRequest: SendMessageRequest) => {
    return this.chatApi.sendMessage({
      ...this.baseRequest,
      sendMessageRequest,
    });
  };

  deleteMessage = (request: DeleteMessageRequest) => {
    return this.chatApi.deleteMessage(request);
  };

  updateMessage = (id: string, updateMessageRequest: UpdateMessageRequest) => {
    return this.chatApi.updateMessage({ id, updateMessageRequest });
  };

  updateMessagePartial = (
    id: string,
    updateMessagePartialRequest: UpdateMessagePartialRequest,
  ) => {
    return this.chatApi.updateMessagePartial({
      id,
      updateMessagePartialRequest,
    });
  };

  getMessage = (request: GetMessageRequest) => {
    return this.chatApi.getMessage(request);
  };

  getManyMessages = (request: OmitTypeId<GetManyMessagesRequest>) => {
    return this.chatApi.getManyMessages({
      ...request,
      ...this.baseRequest,
    });
  };

  translateMessage = (
    id: string,
    translateMessageRequest: TranslateMessageRequest,
  ) => {
    return this.chatApi.translateMessage({ id, translateMessageRequest });
  };

  getMessagesAround = (request: GetRepliesRequest) => {
    return this.chatApi.getReplies(request);
  };

  getOpenGraphData = (request: GetOGRequest) => {
    return this.chatApi.getOG(request);
  };

  sendMessageReaction = (
    messageId: string,
    sendReactionRequest: SendReactionRequest,
  ) => {
    return this.chatApi.sendReaction({
      id: messageId,
      sendReactionRequest,
    });
  };

  deleteMessageReaction = (
    messageId: string,
    request: Omit<DeleteReactionRequest, 'id'>,
  ) => {
    return this.chatApi.deleteReaction({ ...request, id: messageId });
  };

  getMessageReactions = (
    messageId: string,
    request?: Omit<GetReactionsRequest, 'id'>,
  ) => {
    return this.chatApi.getReactions({ ...(request ?? {}), id: messageId });
  };

  sendCustomEvent = (event: EventRequest) => {
    return this.chatApi.sendEvent({
      ...this.baseRequest,
      sendEventRequest: { event },
    });
  };

  private get baseRequest() {
    if (!this.id) {
      throw new Error('You need to initialize the channel with `getOrCreate`');
    }

    return {
      id: this.id,
      type: this.type,
    };
  }
}
