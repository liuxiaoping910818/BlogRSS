package com.imlongluo.blogreader.service;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import com.imlongluo.blogreader.BASE64;
import com.imlongluo.blogreader.MainTabActivity;
import com.imlongluo.blogreader.R;
import com.imlongluo.blogreader.Constants;
import com.imlongluo.blogreader.handler.RSSHandler;
import com.imlongluo.blogreader.provider.FeedData;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Xml;

public class FetcherService extends IntentService {
    private static final int FETCHMODE_DIRECT = 1;

    private static final int FETCHMODE_REENCODE = 2;

    private static final String KEY_USERAGENT = "User-agent";

    private static final String VALUE_USERAGENT = "Mozilla/5.0";

    private static final String CHARSET = "charset=";

    private static final String COUNT = "COUNT(*)";

    private static final String CONTENT_TYPE_TEXT_HTML = "text/html";

    private static final String HREF = "href=\"";

    private static final String HTML_BODY = "<body";

    private static final String ENCODING = "encoding=\"";

    private static final String SERVICENAME = "RssFetcherService";

    private static final String ZERO = "0";

    private static final String GZIP = "gzip";

    /*
     * Allow different positions of the "rel" attribute w.r.t. the "href"
     * attribute
     */
    private static final Pattern feedLinkPattern = Pattern
            .compile(
                    "[.]*<link[^>]* ((rel=alternate|rel=\"alternate\")[^>]* href=\"[^\"]*\"|href=\"[^\"]*\"[^>]* (rel=alternate|rel=\"alternate\"))[^>]*>",
                    Pattern.CASE_INSENSITIVE);

    /* Case insensitive */
    private static final Pattern feedIconPattern = Pattern
            .compile(
                    "[.]*<link[^>]* (rel=(\"shortcut icon\"|\"icon\"|icon)[^>]* href=\"[^\"]*\"|href=\"[^\"]*\"[^>]* rel=(\"shortcut icon\"|\"icon\"|icon))[^>]*>",
                    Pattern.CASE_INSENSITIVE);

    private NotificationManager notificationManager;

    private static SharedPreferences preferences = null;

    private static Proxy proxy;

    private boolean destroyed;

    private RSSHandler handler;

    public FetcherService() {
        super(SERVICENAME);
        destroyed = false;
        HttpURLConnection.setFollowRedirects(true);
    }

    @Override
    public synchronized void onHandleIntent(Intent intent) {
        if (preferences == null) {
            try {
                preferences = PreferenceManager.getDefaultSharedPreferences(createPackageContext(
                        Constants.PACKAGE, 0));
            } catch (NameNotFoundException e) {
                preferences = PreferenceManager.getDefaultSharedPreferences(FetcherService.this);
            }
        }

        if (intent.getBooleanExtra(Constants.SCHEDULED, false)) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putLong(Constants.PREFERENCE_LASTSCHEDULEDREFRESH, SystemClock.elapsedRealtime());
            editor.commit();
        }

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.getState() == NetworkInfo.State.CONNECTED
                && intent != null) {
            if (preferences.getBoolean(Constants.SETTINGS_PROXYENABLED, false)
                    && (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || !preferences
                            .getBoolean(Constants.SETTINGS_PROXYWIFIONLY, false))) {
                try {
                    proxy = new Proxy(ZERO.equals(preferences.getString(Constants.SETTINGS_PROXYTYPE,
                            ZERO)) ? Proxy.Type.HTTP : Proxy.Type.SOCKS, new InetSocketAddress(
                            preferences.getString(Constants.SETTINGS_PROXYHOST, Constants.EMPTY),
                            Integer.parseInt(preferences.getString(Constants.SETTINGS_PROXYPORT,
                                    Constants.DEFAULTPROXYPORT))));
                } catch (Exception e) {
                    proxy = null;
                }
            } else {
                proxy = null;
            }

            int newCount = refreshFeeds(FetcherService.this, intent.getStringExtra(Constants.FEEDID),
                    networkInfo, intent.getBooleanExtra(Constants.SETTINGS_OVERRIDEWIFIONLY, false));

            if (newCount > 0) {
                if (preferences.getBoolean(Constants.SETTINGS_NOTIFICATIONSENABLED, false)) {
                    Cursor cursor = getContentResolver().query(
                            FeedData.EntryColumns.CONTENT_URI,
                            new String[] { COUNT },
                            new StringBuilder(FeedData.EntryColumns.READDATE).append(
                                    Constants.DB_ISNULL).toString(), null, null);

                    cursor.moveToFirst();
                    newCount = cursor.getInt(0);
                    cursor.close();

                    String text = new StringBuilder().append(newCount).append(' ')
                            .append(getString(R.string.newentries)).toString();

                    Notification notification = new Notification(R.drawable.ic_statusbar_rss, text,
                            System.currentTimeMillis());

                    Intent notificationIntent = new Intent(FetcherService.this,
                            MainTabActivity.class);

                    PendingIntent contentIntent = PendingIntent.getActivity(FetcherService.this, 0,
                            notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                    if (preferences.getBoolean(Constants.SETTINGS_NOTIFICATIONSVIBRATE, false)) {
                        notification.defaults = Notification.DEFAULT_VIBRATE;
                    }
                    notification.flags = Notification.FLAG_AUTO_CANCEL
                            | Notification.FLAG_SHOW_LIGHTS;
                    notification.ledARGB = 0xffffffff;
                    notification.ledOnMS = 300;
                    notification.ledOffMS = 1000;

                    String ringtone = preferences.getString(Constants.SETTINGS_NOTIFICATIONSRINGTONE,
                            null);

                    if (ringtone != null && ringtone.length() > 0) {
                        notification.sound = Uri.parse(ringtone);
                    }
                    notification.setLatestEventInfo(FetcherService.this,
                            getString(R.string.rss_feeds), text, contentIntent);
                    notificationManager.notify(0, notification);
                } else {
                    notificationManager.cancel(0);
                }
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public void onDestroy() {
        if (MainTabActivity.INSTANCE != null) {
            MainTabActivity.INSTANCE.internalSetProgressBarIndeterminateVisibility(false);
        }
        destroyed = true;
        if (handler != null) {
            handler.cancel();
        }
        super.onDestroy();
    }

    private int refreshFeeds(Context context, String feedId, NetworkInfo networkInfo,
            boolean overrideWifiOnly) {
        String selection = null;

        if (!overrideWifiOnly && networkInfo.getType() != ConnectivityManager.TYPE_WIFI) {
            selection = new StringBuilder(FeedData.FeedColumns.WIFIONLY).append("=0 or ")
                    .append(FeedData.FeedColumns.WIFIONLY).append(" IS NULL").toString(); // "IS NOT 1"
                                                                                          // does
                                                                                          // not
                                                                                          // work
                                                                                          // on
                                                                                          // 2.1
        }

        Cursor cursor = context.getContentResolver().query(
                feedId == null ? FeedData.FeedColumns.CONTENT_URI
                        : FeedData.FeedColumns.CONTENT_URI(feedId), null, selection, null, null); // no
                                                                                                  // managed
                                                                                                  // query
                                                                                                  // here

        int urlPosition = cursor.getColumnIndex(FeedData.FeedColumns.URL);

        int idPosition = cursor.getColumnIndex(FeedData.FeedColumns._ID);

        int lastUpdatePosition = cursor.getColumnIndex(FeedData.FeedColumns.REALLASTUPDATE);

        int titlePosition = cursor.getColumnIndex(FeedData.FeedColumns.NAME);

        int fetchmodePosition = cursor.getColumnIndex(FeedData.FeedColumns.FETCHMODE);

        int iconPosition = cursor.getColumnIndex(FeedData.FeedColumns.ICON);

        int imposeUseragentPosition = cursor.getColumnIndex(FeedData.FeedColumns.IMPOSE_USERAGENT);

        boolean followHttpHttpsRedirects = preferences.getBoolean(
                Constants.SETTINGS_HTTPHTTPSREDIRECTS, false);

        int result = 0;

        if (handler == null) {
            handler = new RSSHandler(context);
        }
        handler.setEfficientFeedParsing(preferences.getBoolean(
                Constants.SETTINGS_EFFICIENTFEEDPARSING, true));
        handler.setFetchImages(preferences.getBoolean(Constants.SETTINGS_FETCHPICTURES, false));

        while (!destroyed && cursor.moveToNext()) {
            String id = cursor.getString(idPosition);

            boolean imposeUserAgent = !cursor.isNull(imposeUseragentPosition)
                    && cursor.getInt(imposeUseragentPosition) == 1;

            HttpURLConnection connection = null;

            try {
                String feedUrl = cursor.getString(urlPosition);

                connection = setupConnection(feedUrl, imposeUserAgent, followHttpHttpsRedirects);

                String redirectHost = connection.getURL().getHost(); // Feed
                                                                     // icon
                                                                     // should
                                                                     // be
                                                                     // fetched
                                                                     // from
                                                                     // target
                                                                     // site,
                                                                     // not from
                                                                     // feedburner,
                                                                     // so we're
                                                                     // tracking
                                                                     // all
                                                                     // redirections

                String contentType = connection.getContentType();

                int fetchMode = cursor.getInt(fetchmodePosition);

                String iconUrl = null;

                handler.init(new Date(cursor.getLong(lastUpdatePosition)), id,
                        cursor.getString(titlePosition), feedUrl);
                if (fetchMode == 0) {
                    if (contentType != null && contentType.startsWith(CONTENT_TYPE_TEXT_HTML)) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(
                                getConnectionInputStream(connection)));

                        String line = null;

                        String newFeedUrl = null;

                        while ((line = reader.readLine()) != null) {
                            if (line.indexOf(HTML_BODY) > -1) {
                                break;
                            } else {
                                if (newFeedUrl == null) {
                                    Matcher matcher = feedLinkPattern.matcher(line);

                                    if (matcher.find()) { // not "while" as only
                                                          // one link is needed
                                        newFeedUrl = getHref(matcher.group(), feedUrl);
                                    }
                                }
                                if (iconUrl == null) {
                                    Matcher matcher = feedIconPattern.matcher(line);

                                    if (matcher.find()) { // not "while" as only
                                                          // one link is needed
                                        iconUrl = getHref(matcher.group(), feedUrl);
                                    }
                                }
                                if (newFeedUrl != null && iconUrl != null) {
                                    break;
                                }
                            }
                        }

                        if (newFeedUrl != null) {
                            redirectHost = connection.getURL().getHost();
                            connection.disconnect();
                            connection = setupConnection(newFeedUrl, imposeUserAgent,
                                    followHttpHttpsRedirects);
                            contentType = connection.getContentType();
                            handler.initFeedBaseUrl(newFeedUrl);

                            ContentValues values = new ContentValues();

                            values.put(FeedData.FeedColumns.URL, newFeedUrl);
                            context.getContentResolver().update(
                                    FeedData.FeedColumns.CONTENT_URI(id), values, null, null);
                        }
                    }

                    if (contentType != null) {
                        int index = contentType.indexOf(CHARSET);

                        if (index > -1) {
                            int index2 = contentType.indexOf(';', index);

                            try {
                                Xml.findEncodingByName(index2 > -1 ? contentType.substring(
                                        index + 8, index2) : contentType.substring(index + 8));
                                fetchMode = FETCHMODE_DIRECT;
                            } catch (UnsupportedEncodingException usee) {
                                fetchMode = FETCHMODE_REENCODE;
                            }
                        } else {
                            fetchMode = FETCHMODE_REENCODE;
                        }

                    } else {
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                                getConnectionInputStream(connection)));

                        char[] chars = new char[20];

                        int length = bufferedReader.read(chars);

                        String xmlDescription = new String(chars, 0, length);

                        redirectHost = connection.getURL().getHost();
                        connection.disconnect();
                        connection = setupConnection(connection.getURL(), imposeUserAgent,
                                followHttpHttpsRedirects);

                        int start = xmlDescription != null ? xmlDescription.indexOf(ENCODING) : -1;

                        if (start > -1) {
                            try {
                                Xml.findEncodingByName(xmlDescription.substring(start + 10,
                                        xmlDescription.indexOf('"', start + 11)));
                                fetchMode = FETCHMODE_DIRECT;
                            } catch (UnsupportedEncodingException usee) {
                                fetchMode = FETCHMODE_REENCODE;
                            }
                        } else {
                            fetchMode = FETCHMODE_DIRECT; // absolutely no
                                                          // encoding
                                                          // information found
                        }
                    }

                    ContentValues values = new ContentValues();

                    values.put(FeedData.FeedColumns.FETCHMODE, fetchMode);
                    context.getContentResolver().update(FeedData.FeedColumns.CONTENT_URI(id),
                            values, null, null);
                }

                /* check and optionally find favicon */
                byte[] iconBytes = cursor.getBlob(iconPosition);

                if (iconBytes == null) {
                    if (iconUrl == null) {
                        String baseUrl = new StringBuilder(connection.getURL().getProtocol())
                                .append(Constants.PROTOCOL_SEPARATOR).append(redirectHost).toString();
                        HttpURLConnection iconURLConnection = setupConnection(new URL(baseUrl),
                                imposeUserAgent, followHttpHttpsRedirects);
                        try {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(
                                    getConnectionInputStream(iconURLConnection)));
                            String line = null;
                            while ((line = reader.readLine()) != null) {
                                if (line.indexOf(HTML_BODY) > -1) {
                                    break;
                                } else {
                                    Matcher matcher = feedIconPattern.matcher(line);
                                    if (matcher.find()) { // not "while" as only
                                                          // one link is needed
                                        iconUrl = getHref(matcher.group(), baseUrl);
                                        if (iconUrl != null) {
                                            break;
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                        } finally {
                            iconURLConnection.disconnect();
                        }

                        if (iconUrl == null) {
                            iconUrl = new StringBuilder(baseUrl).append(Constants.FILE_FAVICON)
                                    .toString();
                        }
                    }
                    HttpURLConnection iconURLConnection = setupConnection(new URL(iconUrl),
                            imposeUserAgent, followHttpHttpsRedirects);

                    try {
                        iconBytes = getBytes(getConnectionInputStream(iconURLConnection));
                        ContentValues values = new ContentValues();

                        values.put(FeedData.FeedColumns.ICON, iconBytes);
                        context.getContentResolver().update(FeedData.FeedColumns.CONTENT_URI(id),
                                values, null, null);
                    } catch (Exception e) {
                        ContentValues values = new ContentValues();

                        values.put(FeedData.FeedColumns.ICON, new byte[0]); // no
                                                                            // icon
                                                                            // found
                                                                            // or
                                                                            // error
                        context.getContentResolver().update(FeedData.FeedColumns.CONTENT_URI(id),
                                values, null, null);
                    } finally {
                        iconURLConnection.disconnect();
                    }

                }
                switch (fetchMode) {
                    default:
                    case FETCHMODE_DIRECT: {
                        if (contentType != null) {
                            int index = contentType.indexOf(CHARSET);

                            int index2 = contentType.indexOf(';', index);

                            InputStream inputStream = getConnectionInputStream(connection);

                            handler.setInputStream(inputStream);
                            Xml.parse(inputStream, Xml.findEncodingByName(index2 > -1 ? contentType
                                    .substring(index + 8, index2) : contentType
                                    .substring(index + 8)), handler);
                        } else {
                            InputStreamReader reader = new InputStreamReader(
                                    getConnectionInputStream(connection));

                            handler.setReader(reader);
                            Xml.parse(reader, handler);
                        }
                        break;
                    }
                    case FETCHMODE_REENCODE: {
                        ByteArrayOutputStream ouputStream = new ByteArrayOutputStream();

                        InputStream inputStream = getConnectionInputStream(connection);

                        byte[] byteBuffer = new byte[4096];

                        int n;

                        while ((n = inputStream.read(byteBuffer)) > 0) {
                            ouputStream.write(byteBuffer, 0, n);
                        }

                        String xmlText = ouputStream.toString();

                        int start = xmlText != null ? xmlText.indexOf(ENCODING) : -1;

                        if (start > -1) {
                            Xml.parse(
                                    new StringReader(new String(ouputStream.toByteArray(),
                                            xmlText.substring(start + 10,
                                                    xmlText.indexOf('"', start + 11)))), handler);
                        } else {
                            // use content type
                            if (contentType != null) {

                                int index = contentType.indexOf(CHARSET);

                                if (index > -1) {
                                    int index2 = contentType.indexOf(';', index);

                                    try {
                                        StringReader reader = new StringReader(new String(
                                                ouputStream.toByteArray(),
                                                index2 > -1 ? contentType.substring(index + 8,
                                                        index2) : contentType.substring(index + 8)));

                                        handler.setReader(reader);
                                        Xml.parse(reader, handler);
                                    } catch (Exception e) {

                                    }
                                } else {
                                    StringReader reader = new StringReader(new String(
                                            ouputStream.toByteArray()));

                                    handler.setReader(reader);
                                    Xml.parse(reader, handler);

                                }
                            }
                        }
                        break;
                    }
                }
                connection.disconnect();
            } catch (FileNotFoundException e) {
                if (!handler.isDone() && !handler.isCancelled()) {
                    ContentValues values = new ContentValues();
                    values.put(FeedData.FeedColumns.FETCHMODE, 0); // resets the
                                                                   // fetchmode
                                                                   // to
                                                                   // determine
                                                                   // it again
                                                                   // later
                    values.put(FeedData.FeedColumns.ERROR,
                            context.getString(R.string.error_feederror));
                    context.getContentResolver().update(FeedData.FeedColumns.CONTENT_URI(id),
                            values, null, null);
                }
            } catch (Throwable e) {
                if (!handler.isDone() && !handler.isCancelled()) {
                    ContentValues values = new ContentValues();
                    values.put(FeedData.FeedColumns.FETCHMODE, 0); // resets the
                                                                   // fetchmode
                                                                   // to
                                                                   // determine
                                                                   // it again
                                                                   // later
                    values.put(FeedData.FeedColumns.ERROR, e.getMessage());
                    context.getContentResolver().update(FeedData.FeedColumns.CONTENT_URI(id),
                            values, null, null);
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            result += handler.getNewCount();
        }
        cursor.close();

        if (result > 0) {
            context.sendBroadcast(new Intent(Constants.ACTION_UPDATEWIDGET).putExtra(Constants.COUNT,
                    result));
        }
        return result;
    }

    private static String getHref(String line, String baseUrl) {
        int posStart = line.indexOf(HREF);

        if (posStart > -1) {
            String url = line.substring(posStart + 6, line.indexOf('"', posStart + 10)).replace(
                    Constants.AMP_SG, Constants.AMP);

            if (url.startsWith(Constants.SLASH)) {
                int index = baseUrl.indexOf('/', 8);

                if (index > -1) {
                    url = baseUrl.substring(0, index) + url;
                } else {
                    url = baseUrl + url;
                }
            } else if (!url.startsWith(Constants.HTTP) && !url.startsWith(Constants.HTTPS)) {
                url = new StringBuilder(baseUrl).append('/').append(url).toString();
            }
            return url;
        } else {
            return null;
        }
    }

    private static final HttpURLConnection setupConnection(String url, boolean imposeUseragent,
            boolean followHttpHttpsRedirects) throws IOException, NoSuchAlgorithmException,
            KeyManagementException {
        return setupConnection(new URL(url), imposeUseragent, followHttpHttpsRedirects);
    }

    private static final HttpURLConnection setupConnection(URL url, boolean imposeUseragent,
            boolean followHttpHttpsRedirects) throws IOException, NoSuchAlgorithmException,
            KeyManagementException {
        return setupConnection(url, imposeUseragent, followHttpHttpsRedirects, 0);
    }

    private static final HttpURLConnection setupConnection(URL url, boolean imposeUseragent,
            boolean followHttpHttpsRedirects, int cycle) throws IOException,
            NoSuchAlgorithmException, KeyManagementException {
        HttpURLConnection connection = proxy == null ? (HttpURLConnection) url.openConnection()
                : (HttpURLConnection) url.openConnection(proxy);

        connection.setDoInput(true);
        connection.setDoOutput(false);
        if (imposeUseragent) {
            connection.setRequestProperty(KEY_USERAGENT, VALUE_USERAGENT); // some
                                                                           // feeds
                                                                           // need
                                                                           // this
                                                                           // to
                                                                           // work
                                                                           // properly
        }
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);
        connection.setUseCaches(false);

        if (url.getUserInfo() != null) {
            connection.setRequestProperty("Authorization",
                    "Basic " + BASE64.encode(url.getUserInfo().getBytes()));
        }
        connection.setRequestProperty("connection", "close"); // Workaround for
                                                              // android issue
                                                              // 7786
        connection.setRequestProperty("accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        connection.connect();

        String location = connection.getHeaderField("Location");

        if (location != null
                && (url.getProtocol().equals(Constants._HTTP) && location.startsWith(Constants.HTTPS) || url
                        .getProtocol().equals(Constants._HTTPS) && location.startsWith(Constants.HTTP))) {
            // if location != null, the system-automatic redirect has failed
            // which indicates a protocol change
            if (followHttpHttpsRedirects) {
                connection.disconnect();

                if (cycle < 5) {
                    return setupConnection(new URL(location), imposeUseragent,
                            followHttpHttpsRedirects, cycle + 1);
                } else {
                    throw new IOException("Too many redirects.");
                }
            } else {
                throw new IOException("https<->http redirect - enable in settings");
            }
        }
        return connection;
    }

    public static byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        byte[] buffer = new byte[4096];

        int n;

        while ((n = inputStream.read(buffer)) > 0) {
            output.write(buffer, 0, n);
        }

        byte[] result = output.toByteArray();

        output.close();
        inputStream.close();
        return result;
    }

    /**
     * This is a small wrapper for getting the properly encoded inputstream if
     * is is gzip compressed and not properly recognized.
     */
    private static InputStream getConnectionInputStream(HttpURLConnection connection)
            throws IOException {
        InputStream inputStream = connection.getInputStream();

        if (GZIP.equals(connection.getContentEncoding())
                && !(inputStream instanceof GZIPInputStream)) {
            return new GZIPInputStream(inputStream);
        } else {
            return inputStream;
        }
    }
}
