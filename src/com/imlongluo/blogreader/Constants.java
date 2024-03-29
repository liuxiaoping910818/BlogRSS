package com.imlongluo.blogreader;

import com.imlongluo.blogreader.provider.FeedData;

public final class Constants {
    public static final String PACKAGE = "com.imlongluo.blogreader";

    public static final String MY_WEBSITE = "http://www.imlongluo.com";

    public static final String FEED_URL = "http://geek.csdn.net/news/rss";

    public static final String MY_FEED_URL = "http://blog.csdn.net/tcpipstack/rss/list";

    public static final String MY_FEED_NAME = "Mine Blog";

    public static final String SETTINGS_REFRESHINTERVAL = "refresh.interval";

    public static final String SETTINGS_NOTIFICATIONSENABLED = "notifications.enabled";

    public static final String SETTINGS_REFRESHENABLED = "refresh.enabled";

    public static final String SETTINGS_REFRESHONPENENABLED = "refreshonopen.enabled";

    public static final String SETTINGS_NOTIFICATIONSRINGTONE = "notifications.ringtone";

    public static final String SETTINGS_NOTIFICATIONSVIBRATE = "notifications.vibrate";

    public static final String SETTINGS_PRIORITIZE = "contentpresentation.prioritize";

    public static final String SETTINGS_SHOWTABS = "tabs.show";

    public static final String SETTINGS_FETCHPICTURES = "pictures.fetch";

    public static final String SETTINGS_PROXYENABLED = "proxy.enabled";

    public static final String SETTINGS_PROXYPORT = "proxy.port";

    public static final String SETTINGS_PROXYHOST = "proxy.host";

    public static final String SETTINGS_PROXYWIFIONLY = "proxy.wifionly";

    public static final String SETTINGS_PROXYTYPE = "proxy.type";

    public static final String SETTINGS_KEEPTIME = "keeptime";

    public static final String SETTINGS_BLACKTEXTONWHITE = "blacktextonwhite";

    public static final String SETTINGS_LIGHTTHEME = "lighttheme";

    public static final String SETTINGS_FONTSIZE = "fontsize";

    public static final String SETTINGS_STANDARDUSERAGENT = "standarduseragent";

    public static final String SETTINGS_DISABLEPICTURES = "pictures.disable";

    public static final String SETTINGS_HTTPHTTPSREDIRECTS = "httphttpsredirects";

    public static final String SETTINGS_OVERRIDEWIFIONLY = "overridewifionly";

    public static final String SETTINGS_GESTURESENABLED = "gestures.enabled";

    public static final String SETTINGS_ENCLOSUREWARNINGSENABLED = "enclosurewarnings.enabled";

    public static final String SETTINGS_EFFICIENTFEEDPARSING = "efficientfeedparsing";

    public static final String ACTION_REFRESHFEEDS = "com.imlongluo.blogreader.REFRESH";

    public static final String ACTION_STOPREFRESHFEEDS = "com.imlongluo.blogreader.STOPREFRESH";

    public static final String ACTION_UPDATEWIDGET = "com.imlongluo.blogreader.FEEDUPDATED";

    public static final String ACTION_RESTART = "com.imlongluo.blogreader.RESTART";

    public static final String FEEDID = "feedid";

    public static final String DB_ISNULL = " IS NULL";

    public static final String DB_DESC = " DESC";

    public static final String DB_ARG = "=?";

    public static final String DB_AND = " AND ";

    public static final String DB_EXCUDEFAVORITE = new StringBuilder(FeedData.EntryColumns.FAVORITE)
            .append(Constants.DB_ISNULL).append(" OR ").append(FeedData.EntryColumns.FAVORITE)
            .append("=0").toString();

    public static final String EMPTY = "";

    public static final String HTTP = "http://";

    public static final String HTTPS = "https://";

    public static final String _HTTP = "http";

    public static final String _HTTPS = "https";

    public static final String PROTOCOL_SEPARATOR = "://";

    public static final String FILE_FAVICON = "/favicon.ico";

    public static final String SPACE = " ";

    public static final String TWOSPACE = "  ";

    public static final String HTML_TAG_REGEX = "<(.|\n)*?>";

    public static final String FILEURL = "file://";

    public static final String IMAGEFILE_IDSEPARATOR = "__";

    public static final String IMAGEID_REPLACEMENT = "##ID##";

    public static final String DEFAULTPROXYPORT = "8080";

    public static final String URL_SPACE = "%20";

    public static final String HTML_SPAN_REGEX = "<[/]?[ ]?span(.|\n)*?>";

    public static final String HTML_IMG_REGEX = "<[/]?[ ]?img(.|\n)*?>";

    public static final String ONE = "1";

    public static final Object THREENEWLINES = "\n\n\n";

    public static final String PREFERENCE_LICENSEACCEPTED = "license.accepted";

    public static final String PREFERENCE_LASTSCHEDULEDREFRESH = "lastscheduledrefresh";

    public static final String PREFERENCE_LASTTAB = "lasttab";

    public static final String HTML_LT = "&lt;";

    public static final String HTML_GT = "&gt;";

    public static final String LT = "<";

    public static final String GT = ">";

    protected static final String TRUE = "true";

    protected static final String FALSE = "false";

    public static final String READDATE_GREATERZERO = FeedData.EntryColumns.READDATE + ">0";

    public static final String COUNT = "count";

    public static final String ENCLOSURE_SEPARATOR = "[@]"; // exactly three
                                                            // characters!

    public static final String QUESTIONMARKS = "'??'";

    public static final String HTML_QUOT = "&quot;";

    public static final String QUOT = "\"";

    public static final String HTML_APOS = "&apos;";

    public static final String APOSTROPHE = "'";

    public static final String AMP = "&";

    public static final String AMP_SG = "&amp;";

    public static final String SLASH = "/";

    public static final String COMMASPACE = ", ";

    public static final String SCHEDULED = "scheduled";

}
