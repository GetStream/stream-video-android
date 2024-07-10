1. Run the command line below and get your local ip address.

```
ifconfig | grep "inet " | grep -Fv 127.0.0.1 | awk '{print $2}' 
```

2. Put your local ip address to the BASE_URL property inside StreamService.kt file.

3. Go to the Dashboard https://dashboard.getstream.io/app/1324119/video/overview and get the API & Secret keys.

4. Go to the index.js file https://github.com/GetStream/stream-video-android/blob/test/server/demo-app/server/index.js and replace the fields with the keys.qw

5. Go to the `server` directory on your terminal and run the command line below:

```
node index.js
```
