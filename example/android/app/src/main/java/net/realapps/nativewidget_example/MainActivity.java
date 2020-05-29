package net.realapps.nativewidget_example;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.RequiresApi;

import net.realapps.nativewidget.NativeWidgetPlugin;

import io.flutter.embedding.android.FlutterActivity;

import static net.realapps.nativewidget_example.NativeWidgetExampleAppWidget.FLUTTER_ITEM_TAPPED;
import static net.realapps.nativewidget_example.NativeWidgetExampleAppWidget.NEW_WORD;
import static net.realapps.nativewidget_example.NativeWidgetExampleAppWidget.PRESSED_WORDS;
import static net.realapps.nativewidget_example.NativeWidgetExampleAppWidget.RECEIVE_WORDS;

public class MainActivity extends FlutterActivity {
    public static final String TAG = "NativeWidgetExampleMA";

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: ");

        super.onCreate(savedInstanceState);
        NativeWidgetPlugin.registerReceiverForAction(getApplicationContext(), FLUTTER_ITEM_TAPPED, NativeWidgetExampleAppWidget.class);
        NativeWidgetPlugin.registerReceiverForAction(getApplicationContext(), RECEIVE_WORDS, NativeWidgetExampleAppWidget.class);
        NativeWidgetPlugin.registerReceiverForAction(getApplicationContext(), NEW_WORD, NativeWidgetExampleAppWidget.class);
        NativeWidgetPlugin.registerReceiverForAction(getApplicationContext(), PRESSED_WORDS, NativeWidgetExampleAppWidget.class);
    }
}
