package net.realapps.nativewidget;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.Serializable;
import java.lang.annotation.Native;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import static net.realapps.nativewidget.NativeWidgetService.PAYLOAD_KEY;

public class NativeWidgetPlugin implements FlutterPlugin, MethodCallHandler {
  private static NativeWidgetPlugin instance;
  private static String TAG = "NativeWidgetPlugin";
  private Context context;
  private Object initializationLock = new Object();
  private MethodChannel nativeWidgetPluginChannel;

  static String ACTION_BROADCAST_RECEIVERS = "ACTION_BROADCAST_RECEIVERS";

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    onAttachedToEngine(flutterPluginBinding.getApplicationContext(), flutterPluginBinding.getFlutterEngine().getDartExecutor());
  }

  public static void registerWith(Registrar registrar) {
    if(instance == null){
      instance = new NativeWidgetPlugin();
    }

    instance.onAttachedToEngine(registrar.context(), registrar.messenger());
  }

  public void onAttachedToEngine(Context applicationContext, BinaryMessenger messenger) {
    synchronized (initializationLock) {
      if (nativeWidgetPluginChannel != null) {
        return;
      }

      Log.i(TAG, "onAttachedToEngine");
      this.context = applicationContext;

      nativeWidgetPluginChannel =
          new MethodChannel(
              messenger, "net.realapps.nativewidgetplugin");

      nativeWidgetPluginChannel.setMethodCallHandler(this);
    }
  }

  private static void storeActionBroadCastReceivers(Map<String, String> actionBroadcastReceivers, Context context){
    SharedPreferences prefs = context.getSharedPreferences(NativeWidgetService.SHARED_PREFERENCES_KEY, 0);

    Gson gson = new Gson();
    String json = gson.toJson(actionBroadcastReceivers);
    prefs.edit().putString(ACTION_BROADCAST_RECEIVERS, json).apply();
  }

  private static Map<String, String> getActionBroadcastReceivers(Context context){

  Map<String, String> actionBroadcastReceivers = new HashMap<>();

    SharedPreferences prefs = context.getSharedPreferences(NativeWidgetService.SHARED_PREFERENCES_KEY, 0);

    String json = prefs.getString(ACTION_BROADCAST_RECEIVERS, "");

    if(json.equals("")){
      return actionBroadcastReceivers;
    }

    Gson gson = new Gson(); //gsonBuilder.create();

    actionBroadcastReceivers = gson.fromJson(json, HashMap.class);

    return actionBroadcastReceivers;

  }

  private static Class<BroadcastReceiver> getActionBroadcastReceiverClass(Context context, String action){

      Map<String, String> actionBroadcastReceivers = getActionBroadcastReceivers(context);

      String className = actionBroadcastReceivers.get(action);

      try{
          return (Class<BroadcastReceiver>) Class.forName(className);
      } catch (Exception e){
          Log.e(TAG, "getActionBroadcastReceiverClass: " + e.getMessage() );
      }

      return null;
  }

  @RequiresApi(api = Build.VERSION_CODES.KITKAT)
  static public void registerReceiverForAction(Context context, String action, Class receiver) {

    Map<String, String> actionBroadcastReceivers = getActionBroadcastReceivers(context);

    if(actionBroadcastReceivers.get(action) != null){
      actionBroadcastReceivers.remove(action);
    }

    try {
        actionBroadcastReceivers.put(action, receiver.getName());
        storeActionBroadCastReceivers(actionBroadcastReceivers, context);
    } catch (Exception e) {
        Log.e(TAG, "registerReceiverForAction: " + e.getMessage());
      }
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    String method = call.method;
    ArrayList args = call.arguments();


    try {
      if (method.equals("NativeWidget.start")) {

        Object callbackHandleObject = args.get(0);
        Long callbackHandle = Long.valueOf(callbackHandleObject.toString());
        NativeWidgetService.setCallbackDispatcher(context, callbackHandle);
        NativeWidgetService.startBackgroundIsolate(context, callbackHandle);
        result.success(true);
      } else if(method.equals("NativeWidget.registerCallback")){

        Object callbackHandleObject = args.get(0);
        Long actionCallbackHandle = Long.valueOf(callbackHandleObject.toString());
//        long actionCallbackHandle = ((JSONArray) arguments).getLong(0);

        Object callbackHandleObjectKey = args.get(1);
        String actionCallbackHandleKey = callbackHandleObjectKey.toString();
        NativeWidgetService.registerActionCallback(context, actionCallbackHandle, actionCallbackHandleKey);
        result.success(true);
      } else if (method.equals("NativeWidget.sendData")){
        String action = args.get(0).toString();
        Object data = args.get(1);

        Log.d(TAG, "onMethodCall: what's in the box?" + data);
        sendDataToBroadcastReceiver(action, data);

      } else if (method.equals("NativeWidget.sendAction")){
        String action = args.get(0).toString();
        Log.d(TAG, "onMethodCall: act on: " + action);
        sendActionToBroadcastReceiver(action);
      }

    } catch (PluginRegistrantException e) {
      result.error("error", "NativeWidget error: " + e.getMessage(), null);
    }
  }

  private void sendActionToBroadcastReceiver(String action){

    Class<BroadcastReceiver> actionBroadcastReceiver = getActionBroadcastReceiverClass(context, action);

    if(actionBroadcastReceiver == null || context == null){
      Log.d(TAG, "sendDataToBroadcastReceiver: Don't have the means to broadcast the action to the receiver");
      return;
    }

    Intent broadcastIntent = new Intent(context, actionBroadcastReceiver);
    broadcastIntent.setAction(action);

    sendIntentToBroadcastReceiver(broadcastIntent);
  }

  private void sendDataToBroadcastReceiver(String action, Object data){

    Class<BroadcastReceiver> actionBroadcastReceiver = getActionBroadcastReceiverClass(context, action);

    if(actionBroadcastReceiver == null || context == null){
      Log.d(TAG, "sendDataToBroadcastReceiver: Don't have the means to broadcast the action to the receiver");
      return;
    }

    Intent broadcastIntent = new Intent(context, actionBroadcastReceiver);
    broadcastIntent.putExtra(PAYLOAD_KEY, (Serializable) data);
    broadcastIntent.setAction(action);

    sendIntentToBroadcastReceiver(broadcastIntent);
  }

  private void sendIntentToBroadcastReceiver(Intent intent){
    PendingIntent actionPendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    try {
      actionPendingIntent.send();
    } catch (PendingIntent.CanceledException e) {
      Log.e(TAG, "sendDataToBroadcastReceiver: Error: " + e.getMessage());
      e.printStackTrace();
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    Log.i(TAG, "onDetachedFromEngine");
    context = null;
    nativeWidgetPluginChannel.setMethodCallHandler(null);
    nativeWidgetPluginChannel = null;
  }
}
