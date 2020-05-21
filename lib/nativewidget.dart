import 'dart:io';
import 'dart:isolate';
import 'dart:ui';

import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';

const MethodChannel _backgroundChannel =
    MethodChannel('net.realapps.realtracker/native_widget_service_background');

typedef CallbackHandle _GetCallbackHandle(Function callback);

class NativeWidget {
  static String tag = "Native Widget: ";
  static String isolateName = "native_widget_isolate";
  static const MethodChannel _channel =
      const MethodChannel('net.realapps.nativewidgetplugin');
  
  static _GetCallbackHandle _getCallbackHandle =
      (Function callback) => PluginUtilities.getCallbackHandle(callback);
  
  static NativeWidget _nativeWidget = NativeWidget._internal();

  final ReceivePort port = ReceivePort();

  factory NativeWidget() {
    if(_nativeWidget == null){
      _nativeWidget = NativeWidget._internal();
    }
    return _nativeWidget;
  }

  NativeWidget._internal(){
    print(tag + "Constructing Native Widget");
    WidgetsFlutterBinding.ensureInitialized();

    print(tag + "Registering port with name: " + isolateName);
    IsolateNameServer.registerPortWithName(
      port.sendPort,
      isolateName,
    );

    port.listen(_handleIsolateData);
  }

  static Future<bool> initialize() async {
    print(tag + "Initializing... ");
    WidgetsFlutterBinding.ensureInitialized();
    
    final CallbackHandle handle =
        _getCallbackHandle(callbackDispatcher);
    if (handle == null) {
      return false;
    }
    final bool r = await _channel.invokeMethod<bool>(
        'NativeWidget.start', <dynamic>[handle.toRawHandle()]);
    return r ?? false;
  }

  void _handleIsolateData(dynamic callbackDetails){
    print(tag + "Handling data from isolate");
    List callbackArgsList = callbackDetails.cast<dynamic>();

    Function callback = callbackArgsList[0] as Function;
    dynamic args = callbackArgsList[1];

    callback(args);
  }

  static void registerActionCallbacks(Map<String, Function(dynamic)> callbackDetails) async {
    callbackDetails.forEach((String action, Function(dynamic) callback) async {
      await registerActionCallback(callback, action);
    });
  }

  static Future<bool> registerActionCallback(/* Must be static or top level */ Function(dynamic) callback, String action) async {
    if(Platform.isAndroid){
      final CallbackHandle handle = _getCallbackHandle(callback);
      if(handle == null){
        return false;
      }

      final bool r = await _channel.invokeMethod('NativeWidget.registerCallback', <dynamic>[handle.toRawHandle(), action]);

      return r ?? false;
    }

    return false;
  }

  static Future<bool> sendData(String action, dynamic data) async {
    if(Platform.isAndroid){
      final bool r = await _channel.invokeMethod("NativeWidget.sendData", <dynamic>[action, data]);

      return r ?? false;
    }

    return false;
  }
}

void callbackDispatcher() {
  print(NativeWidget.tag +
      "Callback Dispatcher preparing background method channel");

  WidgetsFlutterBinding.ensureInitialized();

  _backgroundChannel.setMethodCallHandler((MethodCall call) async {
    print(NativeWidget.tag + "Handling Native Request");

    try {
      final args = call.arguments;

      final Function callback = PluginUtilities.getCallbackFromHandle(
          CallbackHandle.fromRawHandle(args[0]));

      SendPort uiSendPort =
          IsolateNameServer.lookupPortByName(NativeWidget.isolateName);
      print(
        NativeWidget.tag +
            "UI Send Port is null: " +
            (uiSendPort == null).toString(),
      );

      if (uiSendPort == null) {
        print(NativeWidget.tag + "calling callback: " + callback.toString());
        callback(args[1]);
      } else {
        print(NativeWidget.tag + "sending to ui port: " + uiSendPort.toString());
        uiSendPort?.send([callback, args[1]]);
      }

    } catch (error) {
      print(
          NativeWidget.tag + "Error handling callback dispatcher method call handler" +
              error.toString());
    }
  });

  _backgroundChannel.invokeMethod<bool>('NativeWidget.initialized');
}
