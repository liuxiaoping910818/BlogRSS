package com.imlongluo.blogreader;

import com.imlongluo.blogreader.provider.FeedData;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class FeedConfigActivity extends Activity {
    private static final String TAG = "BlogReader/FeedConfigActivity";
            //FeedConfigActivity.class.getSimpleName();

    private static final String WASACTIVE = "wasactive";

    private static final String[] PROJECTION = new String[] {
        FeedData.FeedColumns.NAME,
        FeedData.FeedColumns.URL,
        FeedData.FeedColumns.WIFIONLY,
        FeedData.FeedColumns.IMPOSE_USERAGENT,
        FeedData.FeedColumns.HIDE_READ };

    private EditText nameEditText;
    private EditText urlEditText;

    private CheckBox refreshOnlyWifiCheckBox;
    private CheckBox standardUseragentCheckBox;
    private CheckBox hideReadCheckBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.feedsettings);
        setResult(RESULT_CANCELED);

        Intent intent = getIntent();

        nameEditText = (EditText) findViewById(R.id.feed_title);
        urlEditText = (EditText) findViewById(R.id.feed_url);

        refreshOnlyWifiCheckBox = (CheckBox) findViewById(R.id.wifionlycheckbox);
        standardUseragentCheckBox = (CheckBox) findViewById(R.id.standarduseragentcheckbox);
        hideReadCheckBox = (CheckBox) findViewById(R.id.hidereadcheckbox);

        Log.d(TAG, "action=" + intent.getAction());

        if (intent.getAction().equals(Intent.ACTION_INSERT)) {
            setTitle(R.string.newfeed_title);
            restoreInstanceState(savedInstanceState);

            ((Button) findViewById(R.id.button_ok)).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    String url = urlEditText.getText().toString();

                    if (!url.startsWith(Constants.HTTP) && !url.startsWith(Constants.HTTPS)) {
                        url = Constants.HTTP + url;
                    }

                    Log.d(TAG, "url=" + url);

                    Cursor cursor = getContentResolver().query(
                            FeedData.FeedColumns.CONTENT_URI,
                            null,
                            new StringBuilder(FeedData.FeedColumns.URL).append(Constants.DB_ARG)
                                    .toString(), new String[] { url }, null);

                    if (cursor.moveToFirst()) {
                        cursor.close();
                        Toast.makeText(FeedConfigActivity.this, R.string.error_feedurlexists,
                                Toast.LENGTH_LONG).show();
                    } else {
                        cursor.close();
                        ContentValues values = new ContentValues();

                        values.put(FeedData.FeedColumns.WIFIONLY,
                                refreshOnlyWifiCheckBox.isChecked() ? 1 : 0);
                        values.put(FeedData.FeedColumns.IMPOSE_USERAGENT,
                                standardUseragentCheckBox.isChecked() ? 0 : 1);
                        values.put(FeedData.FeedColumns.HIDE_READ, hideReadCheckBox.isChecked() ? 1
                                : 0);
                        values.put(FeedData.FeedColumns.URL, url);
                        values.put(FeedData.FeedColumns.ERROR, (String) null);

                        String name = nameEditText.getText().toString();

                        if (name.trim().length() > 0) {
                            values.put(FeedData.FeedColumns.NAME, name);
                        }
                        getContentResolver().insert(FeedData.FeedColumns.CONTENT_URI, values);
                        setResult(RESULT_OK);
                        finish();
                    }
                }
            });
        } else {
            setTitle(R.string.editfeed_title);

            if (!restoreInstanceState(savedInstanceState)) {
                Cursor cursor = getContentResolver().query(intent.getData(), PROJECTION, null,
                        null, null);

                if (cursor.moveToNext()) {
                    nameEditText.setText(cursor.getString(0));
                    urlEditText.setText(cursor.getString(1));
                    refreshOnlyWifiCheckBox.setChecked(cursor.getInt(2) == 1);
                    standardUseragentCheckBox.setChecked(cursor.isNull(3) || cursor.getInt(3) == 0);
                    hideReadCheckBox.setChecked(cursor.getInt(4) == 1);
                    cursor.close();
                } else {
                    cursor.close();
                    Toast.makeText(FeedConfigActivity.this, R.string.error, Toast.LENGTH_LONG)
                            .show();
                    finish();
                }
            }
            ((Button) findViewById(R.id.button_ok)).setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    String url = urlEditText.getText().toString();

                    Cursor cursor = getContentResolver().query(
                            FeedData.FeedColumns.CONTENT_URI,
                            new String[] { FeedData.FeedColumns._ID },
                            new StringBuilder(FeedData.FeedColumns.URL).append(Constants.DB_ARG)
                                    .toString(), new String[] { url }, null);

                    if (cursor.moveToFirst()
                            && !getIntent().getData().getLastPathSegment()
                                    .equals(cursor.getString(0))) {
                        cursor.close();
                        Toast.makeText(FeedConfigActivity.this, R.string.error_feedurlexists,
                                Toast.LENGTH_LONG).show();
                    } else {
                        cursor.close();

                        ContentValues values = new ContentValues();

                        if (!url.startsWith(Constants.HTTP) && !url.startsWith(Constants.HTTPS)) {
                            url = Constants.HTTP + url;
                        }
                        values.put(FeedData.FeedColumns.URL, url);

                        String name = nameEditText.getText().toString();

                        values.put(FeedData.FeedColumns.NAME, name.trim().length() > 0 ? name
                                : null);
                        values.put(FeedData.FeedColumns.FETCHMODE, 0);
                        values.put(FeedData.FeedColumns.WIFIONLY,
                                refreshOnlyWifiCheckBox.isChecked() ? 1 : 0);
                        values.put(FeedData.FeedColumns.IMPOSE_USERAGENT,
                                standardUseragentCheckBox.isChecked() ? 0 : 1);
                        values.put(FeedData.FeedColumns.HIDE_READ, hideReadCheckBox.isChecked() ? 1
                                : 0);
                        values.put(FeedData.FeedColumns.ERROR, (String) null);
                        getContentResolver().update(getIntent().getData(), values, null, null);

                        setResult(RESULT_OK);
                        finish();
                    }
                }

            });

        }

        ((Button) findViewById(R.id.button_cancel)).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
    }

    private boolean restoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.getBoolean(WASACTIVE, false)) {
            nameEditText.setText(savedInstanceState.getCharSequence(FeedData.FeedColumns.NAME));
            urlEditText.setText(savedInstanceState.getCharSequence(FeedData.FeedColumns.URL));
            refreshOnlyWifiCheckBox.setChecked(savedInstanceState
                    .getBoolean(FeedData.FeedColumns.WIFIONLY));
            standardUseragentCheckBox.setChecked(!savedInstanceState
                    .getBoolean(FeedData.FeedColumns.IMPOSE_USERAGENT));
            // we don't have to negate this here, if we would not negate it in
            // the OnSaveInstanceStage, but lets do it for the sake of
            // readability
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(WASACTIVE, true);
        outState.putCharSequence(FeedData.FeedColumns.NAME, nameEditText.getText());
        outState.putCharSequence(FeedData.FeedColumns.URL, urlEditText.getText());
        outState.putBoolean(FeedData.FeedColumns.WIFIONLY, refreshOnlyWifiCheckBox.isChecked());
        outState.putBoolean(FeedData.FeedColumns.IMPOSE_USERAGENT,
                !standardUseragentCheckBox.isChecked());
    }
}
