package net.realapps.nativewidget_example;

import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViewsService;

public class NativeWidgetExampleRemoteViewsService extends RemoteViewsService {
    private static final String TAG = "NativeWidgetRVService";
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        Log.d(TAG, "onGetViewFactory: Getting Native Widget Remote Views Factory from Service");
        try {
            return new NativeWidgetExampleRemoveViewsFactory(this.getApplicationContext(), intent);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
