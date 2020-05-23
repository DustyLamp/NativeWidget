package net.realapps.nativewidget;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.embedding.engine.dart.DartExecutor;
import io.flutter.embedding.engine.plugins.shim.ShimPluginRegistry;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.view.FlutterCallbackInformation;
import io.flutter.view.FlutterMain;

import static net.realapps.nativewidget.NativeWidgetService.ACTION_CALLBACK_HANDLE_KEY;
import static net.realapps.nativewidget.NativeWidgetService.PAYLOAD_KEY;

public class NativeWidgetBackgroundExecutor implements MethodChannel.MethodCallHandler {
    private static final String TAG = "NativeWidgetExecutor";
    private static final String CALLBACK_HANDLE_KEY = "callback_handle";
    private static PluginRegistry.PluginRegistrantCallback pluginRegistrantCallback;

    private MethodChannel backgroundChannel;

    private FlutterEngine backgroundFlutterEngine;

    private AtomicBoolean isCallbackDispatcherReady = new AtomicBoolean(false);


    public static void setPluginRegistrant(PluginRegistry.PluginRegistrantCallback callback) {
        Log.d(TAG, "setPluginRegistrant: ");
        pluginRegistrantCallback = callback;
    }

    public static void setCallbackDispatcher(Context context, long callbackHandle) {
        Log.d(TAG, "setCallbackDispatcher: ");
        SharedPreferences prefs = context.getSharedPreferences(NativeWidgetService.SHARED_PREFERENCES_KEY, 0);
        prefs.edit().putLong(CALLBACK_HANDLE_KEY, callbackHandle).apply();
    }

    public static void registerActionCallback(Context context, long actionCallbackHandle, String actionCallbackHandleKey) {
        Log.d(TAG, "registerActionCallback: " + actionCallbackHandleKey);
        SharedPreferences prefs = context.getSharedPreferences(NativeWidgetService.SHARED_PREFERENCES_KEY, 0);
        prefs.edit().putLong(actionCallbackHandleKey, actionCallbackHandle).apply();
    }

    public boolean isRunning() {
        return isCallbackDispatcherReady.get();
    }

    private void onInitialized() {
        Log.d(TAG, "onInitialized: ");
        isCallbackDispatcherReady.set(true);
        NativeWidgetService.onInitialized();
    }

    @Override
    public void onMethodCall(MethodCall call, MethodChannel.Result result) {
        Log.d(TAG, "onMethodCall: ");
        String method = call.method;
        Log.d(TAG, "onMethodCall: Method is: " + method);
        ArrayList arguments = call.arguments();
        try {
            if (method.equals("NativeWidget.initialized")) {
                // This message is sent by the background method channel as soon as the background isolate
                // is running. From this point forward, the Android side of this plugin can send
                // callback handles through the background method channel, and the Dart side will execute
                // the Dart methods corresponding to those callback handles.
                onInitialized();
                result.success(true);
            } else {
                result.notImplemented();
            }
        } catch (PluginRegistrantException e) {
            result.error("error", "NativeWidget error: " + e.getMessage(), null);
        }
    }

    public void startBackgroundIsolate(Context context) {
        Log.d(TAG, "startBackgroundIsolate: ");
        if (!isRunning()) {
            SharedPreferences p = context.getSharedPreferences(NativeWidgetService.SHARED_PREFERENCES_KEY, 0);
            long callbackHandle = p.getLong(CALLBACK_HANDLE_KEY, 0);
            startBackgroundIsolate(context, callbackHandle);
        }
    }
    public void startBackgroundIsolate(Context context, long callbackHandle) {
        Log.d(TAG, "startBackgroundIsolate: with callback handle");
        if (backgroundFlutterEngine != null) {
            Log.e(TAG, "Background isolate already started");
            return;
        }

        Log.i(TAG, "Starting NativeWidget...");
        String appBundlePath = FlutterMain.findAppBundlePath(context);
        AssetManager assets = context.getAssets();
        if (appBundlePath != null && !isRunning()) {
            backgroundFlutterEngine = new FlutterEngine(context);

            // We need to create an instance of `FlutterEngine` before looking up the
            // callback. If we don't, the callback cache won't be initialized and the
            // lookup will fail.
            FlutterCallbackInformation flutterCallback =
                    FlutterCallbackInformation.lookupCallbackInformation(callbackHandle);
            if (flutterCallback == null) {
                Log.e(TAG, "Fatal: failed to find callback");
                return;
            }

            DartExecutor executor = backgroundFlutterEngine.getDartExecutor();
            initializeMethodChannel(executor);
            DartExecutor.DartCallback dartCallback = new DartExecutor.DartCallback(assets, appBundlePath, flutterCallback);

            executor.executeDartCallback(dartCallback);

            // The pluginRegistrantCallback should only be set in the V1 embedding as
            // plugin registration is done via reflection in the V2 embedding.
            if (pluginRegistrantCallback != null) {
                pluginRegistrantCallback.registerWith(new ShimPluginRegistry(backgroundFlutterEngine));
            }
        }
    }


    public void executeDartCallbackInBackgroundIsolate(Intent intent, final CountDownLatch latch) {
        Log.d(TAG, "executeDartCallbackInBackgroundIsolate: ");
        long actionCallbackHandle = intent.getLongExtra(ACTION_CALLBACK_HANDLE_KEY, -1);

        if(actionCallbackHandle == -1){
            Log.d(TAG, "executeDartCallbackInBackgroundIsolate: Action callback handle was not added to intent");
            return;
        }

        // If another thread is waiting, then wake that thread when the callback returns a result.
        MethodChannel.Result result = null;
        if (latch != null) {
            result =
                    new MethodChannel.Result() {
                        @Override
                        public void success(Object result) {
                            latch.countDown();
                        }

                        @Override
                        public void error(String errorCode, String errorMessage, Object errorDetails) {
                            latch.countDown();
                        }

                        @Override
                        public void notImplemented() {
                            latch.countDown();
                        }
                    };
        }

        Serializable payload = intent.getSerializableExtra(PAYLOAD_KEY);
        String action = intent.getAction();

        if(/*payload == null || */action == null){
            Log.d(TAG, "executeDartCallbackInBackgroundIsolate: action missing");
            return;
        }

        ArrayList arguments = new ArrayList();
        arguments.add(actionCallbackHandle);

        if(payload != null){
            arguments.add(payload);
        }

        arguments.add(action);

        Log.d(TAG, "executeDartCallbackInBackgroundIsolate: Invoking - to be received by callback dispatcher in dart.");
        backgroundChannel.invokeMethod(
                "",
                arguments,
                result);
    }

    private void initializeMethodChannel(BinaryMessenger isolate) {
        backgroundChannel =
                new MethodChannel(
                        isolate,
                        "net.realapps.realtracker/native_widget_service_background");
        backgroundChannel.setMethodCallHandler(this);
    }
}
