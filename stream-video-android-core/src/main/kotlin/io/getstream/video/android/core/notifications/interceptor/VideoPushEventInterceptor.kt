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

package io.getstream.video.android.core.notifications.interceptor

import io.getstream.android.push.PushDevice

public interface VideoPushEventInterceptor {
    /**
     * Called just before Push SDK forwards the new FCM/Xiaomi/Huawei token
     * to the Video SDK.
     *
     * Return true to proceed, false to block.
     */
    public fun registerPushDeviceHook(pushDevice: PushDevice): Boolean

    /**
     * Called just before Push SDK forwards a remote message
     * to the Video SDK.
     *
     * Return true to proceed, false to block.
     */
    public fun onRemoteMessageHook(metadata: Map<String, Any?>, payload: Map<String, Any?>): Boolean
}
