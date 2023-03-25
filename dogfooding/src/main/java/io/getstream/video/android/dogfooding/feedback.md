

The App example is longer than it needs to be since we don't handle the setup of the video object
inside of the video class

1. IE something like this would make this all easier..
app.video.authenticate(apiKey, user(optional), loggingLevel)

2. UserPreferencesManager is a bit weird to expose since it's our custom class

3. AbstractComposeCallActivity is too hidden

4. The deeplinking activity could be mostly generic/hidden i think

5. from a docs perspective it's not clear when you need a service

6. Client init, user should be optional

7. Some of the viewmodel code should probably be exposed by a Call class so its easier for people to build their own viewmodel

8. CallParticipants rendering logic isn't entirely ok. There is a landscape and a portrait version.
Most apps need a version that changes based on screensharing, portrait, landscape and number of participants

9. CallParticipant name is a confusing name for rendering video...

10. CompositionLocal. Android doesn't use the composition local approach to sharing the active call with the components. React uses contexts. Probably for performance. Its fine, either approach works. 

11. Do we need the callEngine local events?