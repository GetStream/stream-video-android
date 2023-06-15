Push notifications are delivered as data payloads that the SDK can use to convert into the same data types that are received when working with the APIs.

When a call is started, Stream Server kicks a job that sends a regular data message (as below) to configured push providers on your app. When a device receives the payload, it's passed to the SDK which connects to Stream Video Server to process the the call and show the notification to the final user.

This is the main payload which will be sent to each configured provider:

```javascript
{
  "sender": "stream.video",
  "type": "call.ring | call.notification | call.live_started",
  "call_display_name": "Jc Miñarro",
  "call_cid": "default:77501ea4-0bd7-47d1-917a-e8dc7387b87f",
  "version": "v2",
}
```
