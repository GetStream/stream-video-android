import { StreamChatClient } from './StreamChatClient';
import { StreamVideoClient } from './StreamVideoClient';
import {
  APIError,
  BanRequest,
  BlockUsersRequest,
  CheckPushRequest,
  CreateDeviceRequest,
  CreateGuestRequest,
  CreateRoleRequest,
  DeactivateUserRequest,
  DeactivateUsersRequest,
  DeleteDeviceRequest,
  DeletePushProviderRequest,
  DeleteRoleRequest,
  DeleteUsersRequest,
  ExportUserRequest,
  ExportUsersRequest,
  FlagRequest,
  GetBlockedUsersRequest,
  GetPermissionRequest,
  GetTaskRequest,
  ListDevicesRequest,
  MuteUserRequest,
  ProductchatApi,
  PushProvider,
  QueryBannedUsersRequest,
  QueryUsersPayload,
  ReactivateUserRequest,
  ReactivateUsersRequest,
  RestoreUsersRequest,
  UnbanRequest,
  UnblockUsersRequest,
  UnmuteUserRequest,
  UpdateAppRequest,
  UpdateUserPartialRequest,
  UpdateUsersRequest,
  UserCustomEventRequest,
} from './gen/chat';
import {
  Configuration,
  ErrorContext,
  FetchError,
  HTTPQuery,
  JSONApiResponse,
  RequestContext,
  ResponseContext,
} from './gen/video';
import { v4 as uuidv4 } from 'uuid';
import { JWTServerToken, JWTUserToken } from './utils/create-token';
import crypto from 'crypto';
import { CallTokenPayload, UserTokenPayload } from './types';

export interface StreamClientOptions {
  timeout?: number;
  basePath?: string;
}

export class StreamClient {
  public readonly video: StreamVideoClient;
  public readonly chat: StreamChatClient;
  public readonly options: StreamClientOptions = {};
  private readonly chatApi: ProductchatApi;
  private readonly token: string;
  private static readonly DEFAULT_TIMEOUT = 3000;

  /**
   *
   * @param apiKey
   * @param secret
   * @param config can be a string, which will be interpreted as base path (deprecated), or a config object
   */
  constructor(
    private readonly apiKey: string,
    private readonly secret: string,
    readonly config?: string | StreamClientOptions,
  ) {
    this.token = JWTServerToken(this.secret);

    if (typeof config === 'string') {
      this.options.basePath = config;
      this.options.timeout = StreamClient.DEFAULT_TIMEOUT;
    } else {
      if (config) {
        this.options = config;
      }
      this.options.timeout = config?.timeout ?? StreamClient.DEFAULT_TIMEOUT;
    }

    this.video = new StreamVideoClient(this);
    this.chat = new StreamChatClient(this);

    const chatConfiguration = this.getConfiguration();
    /** @ts-expect-error */
    this.chatApi = new ProductchatApi(chatConfiguration);
  }

  /**
   *
   * @param userID
   * @param exp
   * @param iat deprecated, the default date will be set internally
   * @param call_cids this parameter is deprecated use `createCallToken` for call tokens
   * @returns
   */
  createToken(
    userID: string,
    exp = Math.round(Date.now() / 1000) + 60 * 60,
    iat = Math.floor((Date.now() - 1000) / 1000),
    call_cids?: string[],
  ) {
    const payload: UserTokenPayload = {
      user_id: userID,
      exp,
      iat,
    };

    if (call_cids) {
      console.warn(
        `Use createCallToken method for creating call tokens, the "call_cids" param will be removed from the createToken method with version 0.2.0`,
      );
      payload.call_cids = call_cids;
    }

    return JWTUserToken(this.secret, payload);
  }

  /**
   *
   * @param userID
   * @param call_cids
   * @param exp
   * @param iat this is deprecated, the current date will be set internally
   * @returns
   */
  createCallToken(
    userIdOrObject: string | { user_id: string; role?: string },
    call_cids: string[],
    exp = Math.round(Date.now() / 1000) + 60 * 60,
    iat = Math.floor((Date.now() - 1000) / 1000),
  ) {
    const payload: CallTokenPayload = {
      exp,
      iat,
      call_cids,
      user_id:
        typeof userIdOrObject === 'string'
          ? userIdOrObject
          : userIdOrObject.user_id,
    };

    if (typeof userIdOrObject === 'object' && userIdOrObject.role) {
      payload.role = userIdOrObject.role;
    }

    return JWTUserToken(this.secret, payload);
  }

  createDevice = (createDeviceRequest: CreateDeviceRequest) => {
    return this.chatApi.createDevice({ createDeviceRequest });
  };

  deleteDevice = (requestParameters: DeleteDeviceRequest) => {
    return this.chatApi.deleteDevice(requestParameters);
  };

  listDevices = (requestParameters: ListDevicesRequest) => {
    return this.chatApi.listDevices(requestParameters);
  };

  listPushProviders = () => {
    return this.chatApi.listPushProviders();
  };

  deletePushProvider = (request: DeletePushProviderRequest) => {
    return this.chatApi.deletePushProvider(request);
  };

  upsertPushProvider = (request: PushProvider) => {
    return this.chatApi.upsertPushProvider({
      upsertPushProviderRequest: { push_provider: request },
    });
  };

  checkPush = (checkPushRequest: CheckPushRequest) => {
    return this.chatApi.checkPush({ checkPushRequest });
  };

  createGuest = (createGuestRequest: CreateGuestRequest) => {
    return this.chatApi.createGuest({ createGuestRequest });
  };

  banUser = (banRequest: BanRequest) => {
    return this.chatApi.ban({ banRequest });
  };

  deactivateUser = (
    deactivateUserRequest: DeactivateUserRequest & { user_id: string },
  ) => {
    return this.chatApi.deactivateUser({
      deactivateUserRequest,
      userId: deactivateUserRequest.user_id,
    });
  };

  deactivateUsers = (deactivateUsersRequest: DeactivateUsersRequest) => {
    return this.chatApi.deactivateUsers({ deactivateUsersRequest });
  };

  deleteUsers = (deleteUsersRequest: DeleteUsersRequest) => {
    return this.chatApi.deleteUsers({ deleteUsersRequest });
  };

  exportUser = (request: ExportUserRequest) => {
    return this.chatApi.exportUser(request);
  };

  exportUsers = (exportUsersRequest: ExportUsersRequest) => {
    return this.chatApi.exportUsers({ exportUsersRequest });
  };

  flag = (flagRequest: FlagRequest) => {
    return this.chatApi.flag({ flagRequest });
  };

  queryBannedUsers = (payload: QueryBannedUsersRequest) => {
    return this.chatApi.queryBannedUsers({ payload });
  };

  queryUsers = (payload: QueryUsersPayload) => {
    return this.chatApi.queryUsers({ payload });
  };

  reactivateUser = (
    reactivateUserRequest: ReactivateUserRequest & { user_id: string },
  ) => {
    return this.chatApi.reactivateUser({
      reactivateUserRequest,
      userId: reactivateUserRequest.user_id,
    });
  };

  reactivateUsers = (reactivateUsersRequest: ReactivateUsersRequest) => {
    return this.chatApi.reactivateUsers({ reactivateUsersRequest });
  };

  restoreUsers = (restoreUsersRequest: RestoreUsersRequest) => {
    return this.chatApi.restoreUsers({ restoreUsersRequest });
  };

  unbanUser = (request: UnbanRequest) => {
    return this.chatApi.unban(request);
  };

  upsertUsers = (updateUsersRequest: UpdateUsersRequest) => {
    return this.chatApi.updateUsers({ updateUsersRequest });
  };

  updateUsersPartial = (request: { users: UpdateUserPartialRequest[] }) => {
    return this.chatApi.updateUsersPartial({
      updateUsersPartialRequest: request,
    });
  };

  muteUser = (muteUserRequest: MuteUserRequest) => {
    return this.chatApi.muteUser({ muteUserRequest });
  };

  unmuteUser = (unmuteUserRequest: UnmuteUserRequest) => {
    return this.chatApi.unmuteUser({ unmuteUserRequest });
  };

  sendCustomEventToUser = (userId: string, event: UserCustomEventRequest) => {
    return this.chatApi.sendUserCustomEvent({
      userId,
      sendUserCustomEventRequest: { event },
    });
  };

  createRole = (createRoleRequest: CreateRoleRequest) => {
    return this.chatApi.createRole({ createRoleRequest });
  };

  deleteRole = (request: DeleteRoleRequest) => {
    return this.chatApi.deleteRole(request);
  };

  getPermission = (request: GetPermissionRequest) => {
    return this.chatApi.getPermission(request);
  };

  listPermissions = () => {
    return this.chatApi.listPermissions();
  };

  listRoles = () => {
    return this.chatApi.listRoles();
  };

  getAppSettings = () => {
    return this.chatApi.getApp();
  };

  updateAppSettings = (updateAppRequest: UpdateAppRequest) => {
    return this.chatApi.updateApp({ updateAppRequest });
  };

  getRateLimits = () => {
    return this.chatApi.getRateLimits();
  };

  getTaskStatus = (request: GetTaskRequest) => {
    return this.chatApi.getTask(request);
  };

  verifyWebhook = (requestBody: string | Buffer, xSignature: string) => {
    const key = Buffer.from(this.secret, 'utf8');
    const hash = crypto
      .createHmac('sha256', key)
      .update(requestBody)
      .digest('hex');

    try {
      return crypto.timingSafeEqual(Buffer.from(hash), Buffer.from(xSignature));
    } catch (err) {
      return false;
    }
  };

  blockUsers = (blockUsersRequest: BlockUsersRequest) => {
    return this.chatApi.blockUsers({ blockUsersRequest });
  };

  unblockUsers = (unblockUsersRequest: UnblockUsersRequest) => {
    return this.chatApi.unblockUsers({ unblockUsersRequest });
  };

  getBlockedUsers = (request: GetBlockedUsersRequest) => {
    return this.chatApi.getBlockedUsers(request);
  };

  getConfiguration = (product: 'chat' | 'video' = 'chat') => {
    return new Configuration({
      apiKey: (name: string) => {
        const mapping: Record<string, string> = {
          'Stream-Auth-Type': 'jwt',
          api_key: this.apiKey,
          Authorization: this.token,
        };

        return mapping[name];
      },
      basePath:
        this.options.basePath ??
        (product === 'chat'
          ? 'https://chat.stream-io-api.com'
          : 'https://video.stream-io-api.com'),
      headers: {
        'X-Stream-Client': 'stream-node-' + process.env.PKG_VERSION,
      },
      middleware: [
        {
          pre: (context: RequestContext) => {
            context.init.headers = {
              ...context.init.headers,
              'x-client-request-id': uuidv4(),
              'Accept-Encoding': 'gzip',
            };

            return Promise.resolve(context);
          },
        },
        {
          // This should be the last post middleware because that will throw an error
          // The Fetch API won't throw an error for HTTP error responses, which means the "onError" middleware won't be called so we need to throw error from "post" middleware
          post: async (context: ResponseContext) => {
            if (
              (context.response && context.response.status < 200) ||
              context.response.status >= 300
            ) {
              const response = new JSONApiResponse(context.response);
              const value = (await response.value()) as APIError;
              throw new Error(
                `Stream error code ${value.code}: ${value.message}`,
              );
            }
          },
        },
        {
          pre: (context: RequestContext) => {
            context.init.signal = AbortSignal.timeout(this.options.timeout!);

            return Promise.resolve(context);
          },
          onError: (context: ErrorContext) => {
            const error = context.error as DOMException;
            if (error.name === 'AbortError' || error.name === 'TimeoutError') {
              throw new FetchError(
                error,
                `The request was aborted due to to the ${this.options.timeout}ms timeout, you can set the timeout in the StreamClient constructor`,
              );
            }

            return Promise.resolve(context.response);
          },
        },
      ],
      // https://github.com/OpenAPITools/openapi-generator/issues/13222
      queryParamsStringify: (params: HTTPQuery) => {
        const newParams = [];
        for (const k in params) {
          const param = params[k];
          if (Array.isArray(param)) {
            newParams.push(`${k}=${encodeURIComponent(param.join(','))}`);
          } else if (typeof param === 'object') {
            newParams.push(`${k}=${encodeURIComponent(JSON.stringify(param))}`);
          } else {
            if (
              typeof param === 'string' ||
              typeof param === 'number' ||
              typeof param === 'boolean'
            ) {
              newParams.push(`${k}=${encodeURIComponent(param)}`);
            }
          }
        }

        return newParams.join('&');
      },
    });
  };
}
