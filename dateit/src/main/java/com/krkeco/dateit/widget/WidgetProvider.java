package com.krkeco.dateit.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.CalendarContract;
import android.widget.RemoteViews;

import com.krkeco.dateit.PrefHelper;
import com.krkeco.dateit.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by KC on 2/15/2017.
 */
public class WidgetProvider extends AppWidgetProvider {

    public static void setText(Context context){

        PrefHelper prefs = new PrefHelper(context);
        String name= prefs.getKeyString(prefs.EVENT_NAME_KEY);
        long start= prefs.getKey(prefs.EVENT_START_KEY);
        Date date = new Date(start);
        DateFormat formatter = new SimpleDateFormat("HH:mm");
        String dateFormatted = formatter.format(date);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        ComponentName thisWidget = new ComponentName(context, WidgetProvider.class);
        remoteViews.setTextViewText(R.id.widget_textview, name+"\n"+dateFormatted);
        appWidgetManager.updateAppWidget(thisWidget, remoteViews);
    }

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final int N = appWidgetIds.length;
  // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];

            PrefHelper prefs = new PrefHelper(context);
            long eventID = prefs.getKey(prefs.EVENT_KEY);

            Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventID);
            Intent intent = new Intent(Intent.ACTION_VIEW).setData(uri);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

            // Create an Intent to launch ExampleActivity
//            Intent intent = new Intent(context, ReturnActivity.class);
//            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

            // Get the layout for the App Widget and attach an on-click listener
            // to the button
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
            views.setOnClickPendingIntent(R.id.widget_textview, pendingIntent);


            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
    }
    }
}