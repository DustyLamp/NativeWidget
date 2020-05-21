package net.realapps.nativewidget_example;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.util.ArrayList;
import java.util.List;

public class NativeWidgetExampleRemoveViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    NativeWidgetExampleRemoveViewsFactory instance;

    static String WORD_NAME = "WORD_NAME";
    static String VIEW_INDEX = "VIEW_INDEX";

    String TAG = "NativeWidgetFactory";

    private Context context;
    private int appWidgetId;


//    String[] wordList = {
//            "Cry",
//            "Lick",
//            "Laugh",
//            "Pop",
//            "Press",};

    NativeWidgetExampleRemoveViewsFactory(Context context, Intent intent) throws Exception {
        super();

        this.context = context;
        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    public NativeWidgetExampleRemoveViewsFactory(){
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate: ");
    }

    @Override
    public void onDataSetChanged() {
        Log.d(TAG, "onDataSetChanged: ");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: ");
    }

    @Override
    public int getCount() {
        Log.d(TAG, "getCount: Getting Count");
        List<WordButton> wordList = NativeWidgetExampleAppWidget.getWordButtonsFromStorage(context);
        if (wordList == null) {
            return 0;
        }
        return wordList.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        Log.d(TAG, "getViewAt: " + position);
        List<WordButton> wordList = NativeWidgetExampleAppWidget.getWordButtonsFromStorage(context);

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.native_widget_button);

        WordButton wordButton = wordList.get(position);

        String word = wordButton.mWord;
        remoteViews.setTextViewText(R.id.native_widget_button, word);

        int background = R.drawable.button_inactive_background;

        if(wordButton.mActive){
            background = R.drawable.button_active_background;
        }

        remoteViews.setInt(R.id.native_widget_button, "setBackgroundResource", background);

        Bundle extras = new Bundle();
        extras.putString(WORD_NAME, word);
        extras.putInt(VIEW_INDEX, position);
        Intent fillInIntent = new Intent();
        fillInIntent.putExtras(extras);

        remoteViews.setOnClickFillInIntent(R.id.native_widget_button, fillInIntent);

        return remoteViews;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
