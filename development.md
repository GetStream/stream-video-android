
## For Go devs

```
cp env.properties.sample .env.properties
```

Edit the file and set CORE_TEST_LOCAL = 1 to run against your local infra.

## Build vars

You can copy environment variables like this:
```
cp env.properties.sample .env.properties
```

build.gradle.kts for each repo reads the .env.properties file and translated it into
build variables. So CORE_TEST_LOCAL in the stream-video-android-core is turned into

```kotlin
BuildConfig.CORE_TEST_LOCAL
```

## Style guidelines

* Keep it simple. Keep it simple. Keep it simple.
* Interfaces can help make your code easier to test. That doesn't mean you should create an interface for everything though.
* Factories are nice, but many times a constructor works just as well
* Only create an interface if you need it for testing, start without it. Note that android studio allows you to extract an interface.
* Integration testing and unit testing are important. It's often more productive to write tests than to try to validate things work by opening up the API
* Our customers should be able to build any type of video/audio experience. Keep that in mind when working on the APIs. They need to be flexible and easy to use
* Kotlin has excellent support for functional programming. The functional style is easy to write, but hard to read. Don't overdo it

## OpenAPI build


## Writing integration tests

The base for integration testing is the `IntegrationTestBase` class.
It offers convenient helpers for test data, client and clientImpl access, and testing events.

* We use Truth and Mockk for testing

## Architecture

* StreamVideoImpl makes the API calls to the coordinator
* Call object maintains state and is updated based on events