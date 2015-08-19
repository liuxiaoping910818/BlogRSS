package com.imlongluo.blogreader.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.widget.Toast;

import com.imlongluo.blogreader.Constants;
import com.imlongluo.blogreader.R;
import com.imlongluo.blogreader.provider.FeedData;

public class Utils {
    private static final String TAG = "BlogReader/Utils";

    public static void insertMyBlogRecord(Context context) {
        String url = Constants.MY_FEED_URL;

        if (!url.startsWith(Constants.HTTP) && !url.startsWith(Constants.HTTPS)) {
            url = Constants.HTTP + url;
        }

        Log.d(TAG, "url=" + url);

        Cursor cursor = context.getContentResolver().query(
                FeedData.FeedColumns.CONTENT_URI,
                null,
                new StringBuilder(FeedData.FeedColumns.URL).append(Constants.DB_ARG)
                        .toString(), new String[] { url }, null);

        if (cursor.moveToFirst()) {
            cursor.close();
//            Toast.makeText(context, R.string.error_feedurlexists,
//                    Toast.LENGTH_LONG).show();
        } else {
            cursor.close();
            ContentValues values = new ContentValues();

            values.put(FeedData.FeedColumns.WIFIONLY, 1);
            values.put(FeedData.FeedColumns.IMPOSE_USERAGENT, 1);
            values.put(FeedData.FeedColumns.HIDE_READ, 0);
            values.put(FeedData.FeedColumns.URL, url);
            values.put(FeedData.FeedColumns.ERROR, (String) null);

            String name = Constants.MY_FEED_NAME;

            if (name.trim().length() > 0) {
                values.put(FeedData.FeedColumns.NAME, name);
            }

            context.getContentResolver().insert(FeedData.FeedColumns.CONTENT_URI, values);
    }
    }
}
