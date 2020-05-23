package net.realapps.nativewidget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import io.flutter.plugin.common.PluginRegistry;

public class NativeWidgetService extends JobIntentService {

    private static String TAG = "NativeWidgetService";
    private static final List<Intent> queue = Collections.synchronizedList(new LinkedList<Intent>());
    static private int JOB_ID = (int) UUID.randomUUID().getMostSignificantBits();

    static String SHARED_PREFERENCES_KEY = "native_widget_prefs";
    static String ACTION_CALLBACK_HANDLE_KEY = "action_callback_handle";

    public static String PAYLOAD_KEY = "data_payload";

    private static NativeWidgetBackgroundExecutor nativeWidgetBackgroundExecutor;


    public static void enqueueWork(Context context, Intent intent){
        Log.d(TAG, "enqueueWork: Enqueuing Work");

        String action = intent.getAction();
        SharedPreferences prefs = context.getSharedPreferences(NativeWidgetService.SHARED_PREFERENCES_KEY, 0);
        Long actionCallbackHandle = prefs.getLong(action, -1);

        if(actionCallbackHandle == -1){
            Log.d(TAG, "enqueueWork: Couldn't find actionCallbackHandle in Shared Preferences. Was it registered?");
            return;
        }

        intent.putExtra(ACTION_CALLBACK_HANDLE_KEY, actionCallbackHandle);

        NativeWidgetService.enqueueWork(context, NativeWidgetService.class, JOB_ID, intent);
    }

//    public static void registerReceiverForAction(Context context, String action, BroadcastReceiver receiver){
//        nativeWidgetBackgroundExecutor.registerReceiverForAction(context, action, receiver);
//    }

    public static void startBackgroundIsolate(Context context, long callbackHandle) {
        Log.d(TAG, "startBackgroundIsolate: ");
        if (nativeWidgetBackgroundExecutor != null) {
            Log.w(TAG, "Attempted to start a duplicate background isolate. Returning...");
            return;
        }
        nativeWidgetBackgroundExecutor = new NativeWidgetBackgroundExecutor();
        nativeWidgetBackgroundExecutor.startBackgroundIsolate(context, callbackHandle);
    }

    static void onInitialized() {
        Log.i(TAG, "NativeWidgetService started!");
        synchronized (queue) {
            Iterator<Intent> i = queue.iterator();
            while (i.hasNext()) {
                nativeWidgetBackgroundExecutor.executeDartCallbackInBackgroundIsolate(i.next(), null);
            }
            queue.clear();
        }
    }

    public static void setCallbackDispatcher(Context context, long callbackHandle) {
        Log.d(TAG, "setCallbackDispatcher:");
        NativeWidgetBackgroundExecutor.setCallbackDispatcher(context, callbackHandle);
    }

    public static void registerActionCallback(Context context, long actionCallbackHandle, String actionCallbackhandleKey){
        Log.d(TAG, "registerActionCallback: ");
        NativeWidgetBackgroundExecutor.registerActionCallback(context, actionCallbackHandle, actionCallbackhandleKey);
    }

    public static void setPluginRegistrant(PluginRegistry.PluginRegistrantCallback callback) {
        Log.d(TAG, "setPluginRegistrant: ");
        NativeWidgetBackgroundExecutor.setPluginRegistrant(callback);
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate: Creating Native Widget Service");
        super.onCreate();
        if (nativeWidgetBackgroundExecutor == null) {
            Log.d(TAG, "onCreate: Creating new NativeWidgetBackgroundExecutor");
            nativeWidgetBackgroundExecutor = new NativeWidgetBackgroundExecutor();
        }
        Context context = getApplicationContext();
        nativeWidgetBackgroundExecutor.startBackgroundIsolate(context);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: Destroying NativeWidgetService");
        super.onDestroy();
    }

    @Override
    protected void onHandleWork(@NonNull final Intent intent) {
        Log.d(TAG, "onHandleWork: Handling Work");

        synchronized (queue) {
            if (!nativeWidgetBackgroundExecutor.isRunning()) {
                Log.i(TAG, "NativeWidgetService has not yet started.");
                queue.add(intent);
                return;
            }
        }

        final CountDownLatch latch = new CountDownLatch(1);
        new Handler(getMainLooper())
                .post(
                        new Runnable() {
                            @Override
                            public void run() {
                                nativeWidgetBackgroundExecutor.executeDartCallbackInBackgroundIsolate(intent, latch);
                            }
                        });

        try {
            latch.await();
        } catch (InterruptedException ex) {
            Log.i(TAG, "Exception waiting to execute Dart callback", ex);
        }
    }
}
