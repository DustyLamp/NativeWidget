package net.realapps.nativewidget_example;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.RequiresApi;

import com.google.gson.Gson;

import net.realapps.nativewidget.NativeWidgetService;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static net.realapps.nativewidget_example.NativeWidgetExampleRemoveViewsFactory.WORD_NAME;

/**
 * Implementation of App Widget functionality.
 */
public class NativeWidgetExampleAppWidget extends AppWidgetProvider {

    private static final String TAG = "NativeWidgetAppWidget";
    static String NATIVE_ITEM_TAPPED = "NATIVE_ITEM_TAPPED";
    static String FLUTTER_ITEM_TAPPED = "FLUTTER_ITEM_TAPPED";
    static String GET_WORDS = "GET_WORDS";
    static String REFRESH_WORDS = "REFRESH_WORDS";
    static String RECEIVE_WORDS = "RECEIVE_WORDS";
    static String PRESSED_WORDS = "PRESSED_WORDS";
    static String NEW_WORD = "NEW_WORD";

    static String APP_WIDGET_ID = "APP_WIDGET_ID";

    static String NATIVE_STORAGE = "NATIVE_STORAGE";
    static String WORD_BUTTONS = "WORD_BUTTONS";

    @Override
    public void onDeleted(Context context, int[] appWidgetIds){
        Log.d(TAG, "onDeleted: ");
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onDisabled(Context context) {
        Log.d(TAG, "onDisabled: ");
        super.onDisabled(context);
    }

    @Override
    public void onEnabled(Context context) {
        Log.d(TAG, "onEnabled: ");
        Intent sendActionIntent = new Intent(context, NativeWidgetService.class);
        sendActionIntent.setAction(GET_WORDS);

        super.onEnabled(context);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {
        Log.d(TAG, "onAppWidgetOptionsChanged: ");

        storeAppWidgetId(appWidgetId, context);

        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onReceive(Context context, Intent intent){
        Log.d(TAG, "onReceive: On Receive Event! Action: " + intent.getAction());

        int appWidgetId = getAppWidgetId(context);

        if(appWidgetId == 0){
            Log.d(TAG, "onReceive: AppWidgetId was zero!");
        }

        if(intent.getAction().equals(NATIVE_ITEM_TAPPED)){
            String word = intent.getStringExtra(WORD_NAME);

            handleNativeItemTapped(context, word);
        } else if(intent.getAction().equals(FLUTTER_ITEM_TAPPED)){
            String word = intent.getSerializableExtra(NativeWidgetService.PAYLOAD_KEY).toString();

            handleFlutterItemTapped(context, word);

            Log.d(TAG, "onReceive: Got word tapped: " + word);
        } else if(intent.getAction().equals(RECEIVE_WORDS)){
            Serializable words = intent.getSerializableExtra(NativeWidgetService.PAYLOAD_KEY);

            Log.d(TAG, "onReceive: Got words: " + words);

            if(words != null){
                storeWordsInNativeStorage((ArrayList) words, context);
            }
        } else if(intent.getAction().equals(PRESSED_WORDS)){
            Serializable words = intent.getSerializableExtra(NativeWidgetService.PAYLOAD_KEY);

            Log.d(TAG, "onReceive: Got words: " + words);

            if(words != null){
                storePressedWordsInNativeStorage((ArrayList) words, context);
            }
        } else if(intent.getAction().equals(NEW_WORD)){
            String word = intent.getSerializableExtra(NativeWidgetService.PAYLOAD_KEY).toString();

            Log.d(TAG, "onReceive: Got new word: " + word);

            if(word != null){
                storeWordInNativeStorage(word, context);
            }
        } else if(intent.getAction().equals(REFRESH_WORDS)){
            refreshWords(context);
        }

        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate: Updating AppWidget");

//        getWords(context);
        refreshWords(context);

        for (int appWidgetId : appWidgetIds) {
            storeAppWidgetId(appWidgetId, context);

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.native_widget_example_app_widget);

            Intent refreshIntent = new Intent(context, NativeWidgetExampleAppWidget.class);
            refreshIntent.setAction(REFRESH_WORDS);

            PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(context, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.refresh_button, refreshPendingIntent);

            Intent serviceIntent = new Intent(context, NativeWidgetExampleRemoteViewsService.class);
            serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME)));
            views.setRemoteAdapter(R.id.native_widget_grid, serviceIntent);

            Intent actionIntent = new Intent(context, NativeWidgetExampleAppWidget.class);
            actionIntent.setAction(NativeWidgetExampleAppWidget.NATIVE_ITEM_TAPPED);
            actionIntent.setData(Uri.parse(actionIntent.toUri(Intent.URI_INTENT_SCHEME)));

            PendingIntent actionPendingIntent = PendingIntent.getBroadcast(context, 0, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setPendingIntentTemplate(R.id.native_widget_grid, actionPendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
            notifyAppWidgetDataChanged(context);

        }

        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    void getWords(Context context){
        Intent retrieveWordsIntent = new Intent(context, this.getClass());
        retrieveWordsIntent.setAction(GET_WORDS);

        NativeWidgetService.enqueueWork(context, retrieveWordsIntent);
    }

    void refreshWords(Context context){
        Log.d(TAG, "refreshWords: Requesting Word Refresh");
        Intent sendActionIntent = new Intent(context, NativeWidgetService.class);
        sendActionIntent.setAction(REFRESH_WORDS);
        NativeWidgetService.enqueueWork(context, sendActionIntent);

        setRefreshWordsAlarm(context);
    }

    void setRefreshWordsAlarm(Context context){
        Log.d(TAG, "setRefreshWordsAlarm: Setting refresh words alarm");

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, 20);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent updateIntent = new Intent(context, NativeWidgetExampleAppWidget.class);
        updateIntent.setAction(REFRESH_WORDS);
        PendingIntent updatePendingIntent = PendingIntent.getBroadcast(context, 0, updateIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager.cancel(updatePendingIntent);
        alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), updatePendingIntent);
    }

    void handleNativeItemTapped(Context context, String word){
        Log.d(TAG, "handleNativeItemTapped: ");

        List<WordButton> wordButtons = getWordButtonsFromStorage(context);

        for(int iWordButton = 0; iWordButton < wordButtons.size(); ++iWordButton){
            WordButton wordButton = wordButtons.get(iWordButton);
            if(wordButton.mWord.equals(word)){
                wordButton.mActive = !wordButton.mActive;
            }
        }

        storeWordButtons(wordButtons, context);
        notifyAppWidgetDataChanged(context);

        Intent sendActionIntent = new Intent(context, NativeWidgetService.class);
        sendActionIntent.setAction(NATIVE_ITEM_TAPPED);
        sendActionIntent.putExtra(NativeWidgetService.PAYLOAD_KEY, (Serializable) word);

        NativeWidgetService.enqueueWork(context, sendActionIntent);
    }
    void handleFlutterItemTapped(Context context, String word){
        Log.d(TAG, "handleFlutterItemTapped: ");

        List<WordButton> wordButtons = getWordButtonsFromStorage(context);

        for(int iWordButton = 0; iWordButton < wordButtons.size(); ++iWordButton){
            WordButton wordButton = wordButtons.get(iWordButton);
            if(wordButton.mWord.equals(word)){
                wordButton.mActive = !wordButton.mActive;
            }
        }

        storeWordButtons(wordButtons, context);
        notifyAppWidgetDataChanged(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    void storeWordsInNativeStorage(ArrayList<String> words, Context context){
        Log.d(TAG, "storeWordsInNativeStorage: ");
        List<WordButton> wordButtons = new ArrayList<>();

        for(int iWord = 0; iWord < words.size(); ++iWord){
            wordButtons.add(new WordButton(words.get(iWord), false));
        }

        storeWordButtons(wordButtons, context);
        notifyAppWidgetDataChanged(context);
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    void storePressedWordsInNativeStorage(ArrayList<String> words, Context context){
        Log.d(TAG, "storePressedWordsInNativeStorage: ");
        List<WordButton> wordButtons = getWordButtonsFromStorage(context);

        if(wordButtons == null){
            wordButtons = new ArrayList<>();
        }

        for(int iWord = 0; iWord < words.size(); ++iWord){
            boolean found = false;
            for(int iWordButton = 0; iWordButton < wordButtons.size(); ++iWordButton){
                if(wordButtons.get(iWordButton).mWord.equals(words.get(iWord))){
                    found = true;
                    wordButtons.get(iWordButton).mActive = true;
                    break;
                }
            }

            if(!found){
                wordButtons.add(new WordButton(words.get(iWord), true));
            }
        }

        storeWordButtons(wordButtons, context);
        notifyAppWidgetDataChanged(context);
    }

    void storeWordInNativeStorage(String word, Context context){
        Log.d(TAG, "storeWordInNativeStorage: ");
        List<WordButton> wordButtons = getWordButtonsFromStorage(context);

        if(wordButtons == null){
            wordButtons = new ArrayList<>();
        }

            boolean found = false;
            for(int iWordButton = 0; iWordButton < wordButtons.size(); ++iWordButton){
                if(wordButtons.get(iWordButton).mWord.equals(word)){
                    found = true;
                    break;
                }
            }

            if(!found){
                wordButtons.add(new WordButton(word, false));
            }

        storeWordButtons(wordButtons, context);
        notifyAppWidgetDataChanged(context);

    }

    void notifyAppWidgetDataChanged(Context context){
        int appWidgetId = getAppWidgetId(context);

        Log.d(TAG, "notifyAppWidgetDataChanged: AppWidgetId: " + appWidgetId);
        AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(appWidgetId, R.id.native_widget_grid);
    }

    static List<WordButton> getWordButtonsFromStorage(Context context){

//        Log.d(TAG, "getWordButtonsFromStorage: ");
        SharedPreferences prefs = context.getSharedPreferences(NATIVE_STORAGE, 0);

        Gson gson = new Gson();
        String existingWordButtonsString = prefs.getString(WORD_BUTTONS, "");

        List<Map<String, Object>> existingWordButtonObjects = gson.fromJson(existingWordButtonsString, ArrayList.class);

        if(existingWordButtonObjects == null){
            return new ArrayList<>();
        }

        List<WordButton> existingWordButtons = new ArrayList<>();
        for(int iExistingWordButton = 0; iExistingWordButton < existingWordButtonObjects.size(); ++iExistingWordButton){
            existingWordButtons.add(new WordButton(existingWordButtonObjects.get(iExistingWordButton)));
        }

        return existingWordButtons;
    }

    static void storeWordButtons(List<WordButton> wordButtons, Context context){
        Log.d(TAG, "storeWordButtons: ");
        Gson gson = new Gson();

        List<Map<String, Object>> wordButtonMaps = new ArrayList<>();
        for(int iWordButton = 0; iWordButton < wordButtons.size(); ++iWordButton){
            wordButtonMaps.add(wordButtons.get(iWordButton).getMap());
        }
        String wordButtonsString = gson.toJson(wordButtonMaps);

        SharedPreferences prefs = context.getSharedPreferences(NATIVE_STORAGE, 0);
        prefs.edit().putString(WORD_BUTTONS, wordButtonsString).apply();

    }

    static void storeAppWidgetId(int appWidgetId, Context context){
        Log.d(TAG, "storeAppWidgetId: ");
        SharedPreferences prefs = context.getSharedPreferences(NATIVE_STORAGE, 0);
        prefs.edit().putInt(APP_WIDGET_ID, appWidgetId).apply();
    }

    static int getAppWidgetId(Context context){
        Log.d(TAG, "getAppWidgetId: ");
        SharedPreferences prefs = context.getSharedPreferences(NATIVE_STORAGE, 0);
        return prefs.getInt(APP_WIDGET_ID, 0);
    }
}

