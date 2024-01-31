# Demo App

Demo demonstrates Stream Video SDK for Android with modern Android tech stacks, such as Compose, Hilt, and Coroutines.

## 📱 Previews

<p align="center">
<img src="https://play-lh.googleusercontent.com/bB3zR64wlxSUptwVz526HWPhkaf_sH-DRlczl3ZS2ftUaWLeHJ0N4lIahsa1sY92Kg=w2560-h1440-rw" alt="drawing" width="330" />
<img src="https://play-lh.googleusercontent.com/pMACrkXDVb3AJUv96mhKUmw32EV3A4DdyjUWxwYO1ZKvnbB0500y3RBwMQuNAs8r7uZp=w2560-h1440-rw" alt="drawing" width="330" />
<img src="https://play-lh.googleusercontent.com/cVPjKIq12TSKGSxHQO0dg4AA-VL1luf7b5rwYksovULoCuLzBAmGxX4ah6o9bkuowZAj=w2560-h1440-rw" alt="drawing" width="330" />
</p>

## Download

Demo app is available on **[Google Play](https://play.google.com/store/apps/details?id=io.getstream.video.android)**. If you have any feedback on this, please [create an issue on GitHub](https://github.com/GetStream/stream-video-android/issues/new/choose).

## Build Setup

If you want to build and run the [demo app](https://github.com/GetStream/stream-video-android/tree/develop/demo-app) on your computer, you can follow the instructions below:

1. Get your Stream API KEY on the [Stream dashboard](https://dashboard.getstream.io?utm_source=Github&utm_medium=DevRel_GitHub_Repo_Jaewoong&utm_content=Developer&utm_campaign=Github_Sep2023_Jaewoong_StreamVideoSDK&utm_term=DevRelOss).

<details>
 <summary> If you don't have your Stream account, you can follow the steps below::</summary>

1. Go to the __[Stream login page](https://getstream.io/try-for-free?utm_source=Github&utm_medium=DevRel_GitHub_Repo_Jaewoong&utm_content=Developer&utm_campaign=Github_Sep2023_Jaewoong_StreamVideoSDK&utm_term=DevRelOss)__.
2. If you have your GitHub account, click the **SIGN UP WITH GITHUB** button and you can sign up within a couple of seconds.

![stream](https://github.com/GetStream/meeting-room-compose/raw/main/figures/stream0.png)

3. If you don't have a GitHub account, fill in the inputs and click the **START FREE TRIAL** button.
4. Go to the __[Dashboard](https://dashboard.getstream.io?utm_source=Github&utm_medium=DevRel_GitHub_Repo_Jaewoong&utm_content=Developer&utm_campaign=Github_Sep2023_Jaewoong_StreamVideoSDK&utm_term=DevRelOss)__ and click the **Create App** button like the below.

![stream](https://github.com/GetStream/meeting-room-compose/raw/main/figures/stream1.png)

5. Fill in the blanks like the below and click the **Create App** button.

![stream](https://github.com/GetStream/meeting-room-compose/raw/main/figures/stream2.png)

6. You will see the **Key** like the figure below and then copy it.

![stream](https://github.com/GetStream/meeting-room-compose/raw/main/figures/stream3.png)

</details>

7. Next, create a file named **.env.properties** on the root project with the formats below:

```
# Environment Variable for dogfooding app
DOGFOODING_BUILD_CONFIG_API_KEY=YOUR_API_KEY
DOGFOODING_BUILD_CONFIG_BENCHMARK=true
DOGFOODING_RES_CONFIG_DEEPLINKING_HOST=stream-calls-dogfood.vercel.app
DOGFOODING_RES_CONFIG_DEEPLINKING_PATH_PREFIX=/
```

Make sure that you properly copy-pasted the Stream API key to the `DOGFOODING_BUILD_CONFIG_API_KEY` property.

8. Finally, run the demo project on your Android Studio.

## License

```
Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.

Licensed under the Stream License;
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   https://github.com/GetStream/stream-video-android/blob/main/LICENSE

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
