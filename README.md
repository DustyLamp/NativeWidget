
# NativeWidget
Please note: 
**This plugin is intended to work with Android only**

A helper plugin allowing headless communication between your Android Widgets and Dart code so you can interact with the AppWidget without needing a Flutter Application open.

## Overview
You can use this plugin to 'plug' your Android App Widget into your Flutter application using *action* strings common to both your Flutter and Native code registered to static or top level dart functions that are stored in shared memory.

You'll need a set of *action* strings for sending from Native to Dart and another set to send from Dart to Native and a set of corresponding functions in dart and `onReceive` handlers in Android

## Getting Started

1. **Add the latest version of this package to pubspec.yaml**
2. **Create the NativeWidget Plugin instance**

Call `NativeWidget();` as soon as possible in your application to create the instance. This is the first operation in the example in `main()`

3. **Create actions as strings for each use case that will be sent to and from your native code in your dart code**
```dart  
//Native to Dart
const  String nativeItemTapped = "NATIVE_ITEM_TAPPED";
const  String refreshWords = "REFRESH_WORDS";
const  String getWords = "GET_WORDS";

//Dart to Native
const  String flutterItemTapped = "FLUTTER_ITEM_TAPPED";
const  String receiveWords = "RECEIVE_WORDS";
const  String newWord = "NEW_WORD";
const  String pressedWords = "PRESSED_WORDS";
```
4. **Create *static or top level* functions for each action that will be *sent from* native code**
```dart
static void handleGetWords(dynamic args) async {
	print("Handling Get Words Request");
	
	//Do some code
}
```
5. **Register these functions as callbacks that will be called when the corresponding  action is received from native code**
```dart
NativeWidget.registerActionCallbacks({
	getWords: _MyAppState.handleGetWords,
	refreshWords: _MyAppState.handleRefreshWords,
	nativeItemTapped: _MyAppState.handleItemTapped,
});
```
6. **Initialize the NativeWidget plugin**

	Add:
```dart
NativeWidget.initialize();
```
I recommend to add this after you've registered your callback functions and actions in step 5.

7. **Register your AppWidgets as the receivers for actions *sent to* native code**
```java
@Override  
protected void onCreate(Bundle savedInstanceState) {  
  super.onCreate(savedInstanceState);  
  
  //Registering Widget as receiver for actions that are coming from Flutter  
  NativeWidgetPlugin.registerReceiverForAction(getApplicationContext(), FLUTTER_ITEM_TAPPED, NativeWidgetExampleAppWidget.class);  
  NativeWidgetPlugin.registerReceiverForAction(getApplicationContext(), RECEIVE_WORDS, NativeWidgetExampleAppWidget.class);  
  NativeWidgetPlugin.registerReceiverForAction(getApplicationContext(), NEW_WORD, NativeWidgetExampleAppWidget.class);  
  NativeWidgetPlugin.registerReceiverForAction(getApplicationContext(), PRESSED_WORDS, NativeWidgetExampleAppWidget.class);
  
  GeneratedPluginRegistrant.registerWith(this.getFlutterEngine());  //This should be automatically generated.
}
```

*Note: Currently each action can only be registered to one App Widget as action are considered unique.*

8. **Update the android manifest to use the NativeWidgetService**
In `AndroidManifest.xml` change `android:name` to ".Application" like so:

```xml
<application
        android:name=".Application"
		...>
	...
</application>
```

See the example for... an example.

## Sending Data to Native Code
Send the action along with the data that you'd like to send to `NativeWidget.sendData(String action, dynamic data)`
```dart
void  _sendWordTapped(String word) {
	print("Sending word tapped");
	NativeWidget.sendData(flutterItemTapped, word);
}
```

## Receiving Data from Native Code
Handle the actions in your AppWidget's `onReceive(...)` function. The data that is sent is wrapped up ast a serializable in the intent with the key `PAYLOAD_KEY`. 
```dart
if(intent.getAction().equals(FLUTTER_ITEM_TAPPED)){  
  String word = intent.getSerializableExtra(NativeWidgetService.PAYLOAD_KEY).toString();
  Log.d(TAG, "onReceive: Got word tapped: " + word);
  
  handleFlutterItemTapped(context, word, AppWidgetManager.getInstance(context), appWidgetId);
}
```

## Example
Check out the example code showing how to hook everything up.

Run it and press some words in an App Widget or in Flutter until your heart is content. 

Some people think pressing words is for losers... but really, *it's for Heroes.*

## Dependencies and References
This plugin is heavily based on [AndroidAlarmManager](https://github.com/flutter/plugins/tree/master/packages/android_alarm_manager) of which I would be completely lost without.

[gson](https://github.com/google/gson) and [shared_preferences](https://pub.dev/packages/shared_preferences) are used in the example.

## Known Issues

### Reboot Delay
There is a delay when hooking everything up automatically again after a phone reboot. Eventually it settles.

You can see this in the example if you restart your phone and refresh / press words in the widget straight away. After some time it'll sync up again.

I'm working on it, but if you can suggest how to fix this, please let me know!

### registerWith not called in older Flutter applications

If you get an error like: 

"Unhandled Exception: MissingPluginException(No implementation found for ... " then you might need to add the `meta-data` below to your `AndroidManifest.xml` 

```xml
<application>
	
	...

	<meta-data
		android:name="flutterEmbedding"
		android:value="2" />
	
</application>
```

### App has to open at least once
Your Flutter application must be opened at least once in order to use this Plugin so that the actions and static functions can be registered.

### Static / Top level dart function names
Don't change these names if you can avoid it - strange things may happen as each function handle is registered in shared memory as a *long*. The reference may be lost if the name changes

### Other problems?
There are probably a bunch other other issues - I just whipped this up for myself so **use at your own peril**!

Let me know if you find other issues.
