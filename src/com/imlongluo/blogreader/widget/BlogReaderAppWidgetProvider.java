package com.imlongluo.blogreader.widget;

import com.imlongluo.blogreader.MainTabActivity;
import com.imlongluo.blogreader.R;
import com.imlongluo.blogreader.Constants;
import com.imlongluo.blogreader.provider.FeedData;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.RemoteViews;


public class BlogReaderAppWidgetProvider extends AppWidgetProvider {
	private static final String LIMIT = " limit ";
	
	private static final int[] IDS = {R.id.news_1, R.id.news_2, R.id.news_3, R.id.news_4, R.id.news_5, R.id.news_6, R.id.news_7, R.id.news_8, R.id.news_9, R.id.news_10};
	
	private static final int[] ICON_IDS = {R.id.news_icon_1, R.id.news_icon_2, R.id.news_icon_3, R.id.news_icon_4, R.id.news_icon_5, R.id.news_icon_6, R.id.news_icon_7, R.id.news_icon_8, R.id.news_icon_9, R.id.news_icon_10};
	
	public static final int STANDARD_BACKGROUND = 0x7c000000;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		
		onUpdate(context, appWidgetManager, appWidgetManager.getAppWidgetIds(new ComponentName(context, BlogReaderAppWidgetProvider.class)));
	}
	
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		SharedPreferences preferences = context.getSharedPreferences(BlogReaderAppWidgetProvider.class.getName(), 0);
		
		for (int n = 0, i = appWidgetIds.length; n < i; n++) {
			updateAppWidget(context, appWidgetManager, appWidgetIds[n], preferences.getBoolean(appWidgetIds[n]+".hideread", false), preferences.getString(appWidgetIds[n]+".entrycount", "10"), preferences.getString(appWidgetIds[n]+".feeds", Constants.EMPTY), preferences.getInt(appWidgetIds[n]+".background", STANDARD_BACKGROUND));
		}
	}
	
	static void updateAppWidget(Context context, int appWidgetId, boolean hideRead, String entryCount, String feedIds, int backgroundColor) {
		updateAppWidget(context, AppWidgetManager.getInstance(context), appWidgetId, hideRead, entryCount, feedIds, backgroundColor);
	}
	
	private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, boolean hideRead, String entryCount, String feedIds, int backgroundColor) {
		StringBuilder selection = new StringBuilder();
		
		if (hideRead) {
			selection.append(FeedData.EntryColumns.READDATE).append(Constants.DB_ISNULL);
		}
		
		if (feedIds.length() > 0) {
			if (selection.length() > 0) {
				selection.append(Constants.DB_AND);
			}
			selection.append(FeedData.EntryColumns.FEED_ID).append(" IN ("+feedIds).append(')');
		}
		
		Cursor cursor = context.getContentResolver().query(FeedData.EntryColumns.CONTENT_URI, new String[] {FeedData.EntryColumns.TITLE, FeedData.EntryColumns._ID, FeedData.FeedColumns.ICON}, selection.toString(), null, new StringBuilder(FeedData.EntryColumns.DATE).append(Constants.DB_DESC).append(LIMIT).append(entryCount).toString());
		
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.homescreenwidget);
		
		views.setOnClickPendingIntent(R.id.feed_icon, PendingIntent.getActivity(context, 0, new Intent(context, MainTabActivity.class), 0));
		
		int k = 0;
		
		while (cursor.moveToNext() && k < IDS.length) {
			views.setViewVisibility(IDS[k], View.VISIBLE);
			if (!cursor.isNull(2)) {
				try {
					byte[] iconBytes = cursor.getBlob(2);
					
					if (iconBytes != null && iconBytes.length > 0) {
						Bitmap bitmap = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);
						
						if (bitmap != null) {
							views.setBitmap(ICON_IDS[k], "setImageBitmap", bitmap);
							views.setViewVisibility(ICON_IDS[k], View.VISIBLE);
							views.setTextViewText(IDS[k], " "+cursor.getString(0)); // bad style
						} else {
							views.setViewVisibility(ICON_IDS[k], View.GONE);
							views.setTextViewText(IDS[k], cursor.getString(0));
						}
					} else {
						views.setViewVisibility(ICON_IDS[k], View.GONE);
						views.setTextViewText(IDS[k], cursor.getString(0));
					}
				} catch (Throwable e) {
					views.setViewVisibility(ICON_IDS[k], View.GONE);
					views.setTextViewText(IDS[k], cursor.getString(0));
				}
			} else {
				views.setViewVisibility(ICON_IDS[k], View.GONE);
				views.setTextViewText(IDS[k], cursor.getString(0));
			}
			views.setOnClickPendingIntent(IDS[k++], PendingIntent.getActivity(context, 0, new Intent(Intent.ACTION_VIEW, FeedData.EntryColumns.ENTRY_CONTENT_URI(cursor.getString(1))), PendingIntent.FLAG_CANCEL_CURRENT));
		}
		cursor.close();
		for (; k < IDS.length; k++) {
			views.setViewVisibility(ICON_IDS[k], View.GONE);
			views.setViewVisibility(IDS[k], View.GONE);
			views.setTextViewText(IDS[k], Constants.EMPTY);
		}
		views.setInt(R.id.widgetlayout, "setBackgroundColor", backgroundColor);
		appWidgetManager.updateAppWidget(appWidgetId, views);
	}

}
