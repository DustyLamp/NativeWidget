package net.realapps.nativewidget_example;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import net.realapps.nativewidget.NativeWidgetPlugin;
import net.realapps.nativewidget.NativeWidgetService;

import io.flutter.app.FlutterApplication;
import io.flutter.plugin.common.PluginRegistry;

import static net.realapps.nativewidget_example.NativeWidgetExampleAppWidget.FLUTTER_ITEM_TAPPED;
import static net.realapps.nativewidget_example.NativeWidgetExampleAppWidget.NEW_WORD;
import static net.realapps.nativewidget_example.NativeWidgetExampleAppWidget.PRESSED_WORDS;
import static net.realapps.nativewidget_example.NativeWidgetExampleAppWidget.RECEIVE_WORDS;

public class Application extends FlutterApplication implements PluginRegistry.PluginRegistrantCallback{
    private static final String TAG = "RealTrackerApplication";

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onCreate() {
        super.onCreate();
        NativeWidgetService.setPluginRegistrant(this);
        NativeWidgetPlugin.registerReceiverForAction(getApplicationContext(), FLUTTER_ITEM_TAPPED, NativeWidgetExampleAppWidget.class);
        NativeWidgetPlugin.registerReceiverForAction(getApplicationContext(), RECEIVE_WORDS, NativeWidgetExampleAppWidget.class);
        NativeWidgetPlugin.registerReceiverForAction(getApplicationContext(), NEW_WORD, NativeWidgetExampleAppWidget.class);
        NativeWidgetPlugin.registerReceiverForAction(getApplicationContext(), PRESSED_WORDS, NativeWidgetExampleAppWidget.class);
    }

    @Override
    public void registerWith(PluginRegistry registry) {
        Log.d(TAG, "registerWith: ");
        if(registry != null){
            NativeWidgetPlugin.registerWith(
                registry.registrarFor("net.realapps.nativewidget.NativeWidgetPlugin"));
        }
    }
}