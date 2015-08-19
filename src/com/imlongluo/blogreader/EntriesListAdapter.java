package com.imlongluo.blogreader;

import java.text.DateFormat;
import java.util.Date;
import java.util.Vector;

import com.imlongluo.blogreader.provider.FeedData;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

public class EntriesListAdapter extends ResourceCursorAdapter {
    private static final String TAG = EntriesListAdapter.class.getSimpleName();

    private static final int STATE_NEUTRAL = 0;
    private static final int STATE_ALLREAD = 1;
    private static final int STATE_ALLUNREAD = 2;

    private int titleColumnPosition;
    private int dateColumn;
    private int readDateColumn;
    private int favoriteColumn;
    private int idColumn;
    private int feedIconColumn;
    private int feedNameColumn;
    private int linkColumn;

    private static final String SQLREAD = "length(readdate) ASC, ";
    public static final String READDATEISNULL = FeedData.EntryColumns.READDATE + Constants.DB_ISNULL;

    private boolean hideRead;
    private Activity context;
    private Uri uri;
    private boolean showFeedInfo;
    private int forcedState;

    private Vector<Long> markedAsRead;
    private Vector<Long> markedAsUnread;
    private Vector<Long> favorited;
    private Vector<Long> unfavorited;

    private DateFormat dateFormat;
    private DateFormat timeFormat;

    public EntriesListAdapter(Activity context, Uri uri, boolean showFeedInfo, boolean autoreload,
            boolean hideRead) {
        super(context, R.layout.entrylistitem, createManagedCursor(context, uri, hideRead),
                autoreload);
        this.hideRead = hideRead;
        this.context = context;
        this.uri = uri;

        Cursor cursor = getCursor();

        titleColumnPosition = cursor.getColumnIndex(FeedData.EntryColumns.TITLE);
        dateColumn = cursor.getColumnIndex(FeedData.EntryColumns.DATE);
        readDateColumn = cursor.getColumnIndex(FeedData.EntryColumns.READDATE);
        favoriteColumn = cursor.getColumnIndex(FeedData.EntryColumns.FAVORITE);
        idColumn = cursor.getColumnIndex(FeedData.EntryColumns._ID);
        linkColumn = cursor.getColumnIndex(FeedData.EntryColumns.LINK);

        this.showFeedInfo = showFeedInfo;
        if (showFeedInfo) {
            feedIconColumn = cursor.getColumnIndex(FeedData.FeedColumns.ICON);
            feedNameColumn = cursor.getColumnIndex(FeedData.FeedColumns.NAME);
        }

        forcedState = STATE_NEUTRAL;
        markedAsRead = new Vector<Long>();
        markedAsUnread = new Vector<Long>();
        favorited = new Vector<Long>();
        unfavorited = new Vector<Long>();

        dateFormat = android.text.format.DateFormat.getDateFormat(context);
        timeFormat = android.text.format.DateFormat.getTimeFormat(context);
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        TextView textView = (TextView) view.findViewById(android.R.id.text1);

        String link = cursor.getString(linkColumn);
        String title = cursor.getString(titleColumnPosition);

        textView.setText(title == null || title.length() == 0 ? link : title);
        TextView dateTextView = (TextView) view.findViewById(android.R.id.text2);

        final ImageView imageView = (ImageView) view.findViewById(android.R.id.icon);

        final long id = cursor.getLong(idColumn);
        view.setTag(link);

        final boolean favorite = !unfavorited.contains(id)
                && (cursor.getInt(favoriteColumn) == 1 || favorited.contains(id));
        imageView.setImageResource(favorite ? android.R.drawable.star_on
                : android.R.drawable.star_off);
        imageView.setTag(favorite ? Constants.TRUE : Constants.FALSE);
        imageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean newFavorite = !Constants.TRUE.equals(view.getTag());

                if (newFavorite) {
                    view.setTag(Constants.TRUE);
                    imageView.setImageResource(android.R.drawable.star_on);
                    favorited.add(id);
                    unfavorited.remove(id);
                } else {
                    view.setTag(Constants.FALSE);
                    imageView.setImageResource(android.R.drawable.star_off);
                    unfavorited.add(id);
                    favorited.remove(id);
                }

                ContentValues values = new ContentValues();

                values.put(FeedData.EntryColumns.FAVORITE, newFavorite ? 1 : 0);
                view.getContext()
                        .getContentResolver()
                        .update(uri,
                                values,
                                new StringBuilder(FeedData.EntryColumns._ID).append(Constants.DB_ARG)
                                        .toString(), new String[] { Long.toString(id) });
                context.getContentResolver().notifyChange(
                        FeedData.EntryColumns.FAVORITES_CONTENT_URI, null);
            }
        });

        Date date = new Date(cursor.getLong(dateColumn));

        if (showFeedInfo && feedIconColumn > -1 && feedNameColumn > -1) {
            byte[] iconBytes = cursor.getBlob(feedIconColumn);

            if (iconBytes != null && iconBytes.length > 0) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);

                if (bitmap != null) {
                    int bitmapSizeInDip = (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, 18f, context.getResources()
                                    .getDisplayMetrics());
                    if (bitmap.getHeight() != bitmapSizeInDip) {
                        bitmap = Bitmap.createScaledBitmap(bitmap, bitmapSizeInDip,
                                bitmapSizeInDip, false);
                    }
                    dateTextView.setText(new StringBuilder().append(' ')
                            .append(dateFormat.format(date)).append(' ')
                            .append(timeFormat.format(date)).append(Constants.COMMASPACE)
                            .append(cursor.getString(feedNameColumn))); // bad
                                                                        // style
                } else {
                    dateTextView.setText(new StringBuilder(dateFormat.format(date)).append(' ')
                            .append(timeFormat.format(date)).append(Constants.COMMASPACE)
                            .append(cursor.getString(feedNameColumn)));
                }
                dateTextView.setCompoundDrawablesWithIntrinsicBounds(new BitmapDrawable(bitmap),
                        null, null, null);
            } else {
                dateTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                dateTextView.setText(new StringBuilder(dateFormat.format(date)).append(' ')
                        .append(timeFormat.format(date)).append(Constants.COMMASPACE)
                        .append(cursor.getString(feedNameColumn)));
            }

        } else {
            dateTextView.setText(new StringBuilder(dateFormat.format(date)).append(' ').append(
                    timeFormat.format(date)));
        }

        if (forcedState == STATE_ALLUNREAD
                && !markedAsRead.contains(id)
                || (forcedState != STATE_ALLREAD && cursor.isNull(readDateColumn) && !markedAsRead
                        .contains(id)) || markedAsUnread.contains(id)) {
            textView.setEnabled(true);
        } else {
            textView.setEnabled(false);
        }
    }

    public boolean isHideRead() {
        return hideRead;
    }

    public void setHideRead(boolean hideRead) {
        if (hideRead != this.hideRead) {
            this.hideRead = hideRead;
            reloadCursor();
        }
    }

    public void reloadCursor() {
        markedAsRead.clear();
        markedAsUnread.clear();
        favorited.clear();
        unfavorited.clear();
        context.stopManagingCursor(getCursor());
        forcedState = STATE_NEUTRAL;
        changeCursor(createManagedCursor(context, uri, hideRead));
        notifyDataSetInvalidated();
    }

    private static Cursor createManagedCursor(Activity context, Uri uri, boolean hideRead) {
        return context.managedQuery(uri, null, hideRead ? READDATEISNULL : null, null,
                new StringBuilder(PreferenceManager.getDefaultSharedPreferences(context)
                        .getBoolean(Constants.SETTINGS_PRIORITIZE, false) ? SQLREAD : Constants.EMPTY)
                        .append(FeedData.EntryColumns.DATE).append(Constants.DB_DESC).toString());
    }

    public void markAsRead() {
        if (hideRead) {
            // well, the cursor should be empty
            reloadCursor();
        } else {
            forcedState = STATE_ALLREAD;
            markedAsRead.clear();
            markedAsUnread.clear();
            notifyDataSetInvalidated();
        }
    }

    public void markAsUnread() {
        forcedState = STATE_ALLUNREAD;
        markedAsRead.clear();
        markedAsUnread.clear();
        notifyDataSetInvalidated();
    }

    public void neutralizeReadState() {
        forcedState = STATE_NEUTRAL;
    }

    public void markAsRead(long id) {
        if (hideRead) {
            reloadCursor();
        } else {
            markedAsRead.add(id);
            markedAsUnread.remove(id);
            notifyDataSetInvalidated();
        }
    }

    public void markAsUnread(long id) {
        markedAsUnread.add(id);
        markedAsRead.remove(id);
        notifyDataSetInvalidated();
    }

}
