package com.rustyclock.orienteering;

import android.Manifest;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.logger.Logger;
import com.rustyclock.orienteering.custom.ToolbarActivity;
import com.rustyclock.orienteering.db.DbHelper;
import com.rustyclock.orienteering.model.Checkpoint;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.ormlite.annotations.OrmLiteDao;

import java.sql.SQLException;


@EActivity(R.layout.activity_main)
@OptionsMenu(R.menu.menu_main)
public class MainActivity extends ToolbarActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_SEND_SMS = 34;

    Prefs_ prefs;

    @OrmLiteDao(helper = DbHelper.class)
    Dao<Checkpoint, Integer> checkpointsDao;

    @AfterViews
    void afterViews() {
        setupToolbar(false);
        setToolbarIcon(R.mipmap.ic_launcher);
    }

    @Click(R.id.btn_scan)
    void scan() {
        String phoneNo = prefs.phoneNo().get();
        if(TextUtils.isEmpty(phoneNo)) {
            Snackbar.make(findViewById(android.R.id.content), R.string.fill_up_settings, Snackbar.LENGTH_LONG).show();
            return;
        }

        IntentIntegrator integrator = new IntentIntegrator(MainActivity.this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        integrator.setPrompt("");
        integrator.initiateScan();
    }

    @Click(R.id.btn_send)
    void showSendDialog() {
        View v = getLayoutInflater().inflate(R.layout.dialog_send_code, null);
        final EditText et = (EditText) v.findViewById(R.id.et_code);
        new AlertDialog.Builder(this)
                .setTitle(R.string.send_code)
                .setView(v)
                .setPositiveButton(R.string.send, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String s = et.getText().toString();
                        if(TextUtils.isEmpty(s) || s.length()<2)
                            return;

                        Checkpoint cp = new Checkpoint();
                        cp.setManual(true);
                        cp.setCode(s);

                        try {
                            checkpointsDao.create(cp);
                            sendCheckpointSMS(cp);
                        } catch (SQLException e) {
                            Log.e(TAG, e.getMessage());
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create().show();
    }

    @Click(R.id.btn_history)
    void history() {
        HistoryActivity_.intent(this).start();
    }

    @OptionsItem(R.id.action_settings)
    void settings() {
        SettingsActivity_.intent(this).start();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.SEND_SMS)) {

                new AlertDialog.Builder(this)
                        .setTitle(R.string.sending_sms)
                        .setMessage(R.string.sending_sms_text)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.SEND_SMS}, REQUEST_SEND_SMS);
                            }
                        })
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.SEND_SMS}, REQUEST_SEND_SMS);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        prefs = new Prefs_(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Log.d("TAG", "onRequest");
        switch (requestCode) {
            case REQUEST_SEND_SMS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {

                    Snackbar.make(findViewById(android.R.id.content), R.string.no_sms_permission, Snackbar.LENGTH_LONG)
                            .setAction(R.string.settings, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Intent intent = new Intent();
                                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    Uri uri = Uri.fromParts("package", MainActivity.this.getPackageName(), null);
                                    intent.setData(uri);
                                    startActivity(intent);
                                }
                            })
                            .show();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode!=RESULT_OK)
            return;

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result==null)
            return;

        Checkpoint cp = new Checkpoint(result.getContents());

        try {
            checkpointsDao.create(cp);
            sendCheckpointSMS(cp);

        } catch (Exception e) {
            e.printStackTrace();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void sendCheckpointSMS(Checkpoint cp) {

        Intent sentIntent = new Intent(SmsStatusReceiver.SENT_INTENT);
        sentIntent.putExtra(SmsStatusReceiver.EXTRA_ID_SENT, cp.getDbId());

        Intent deliveryIntent = new Intent(SmsStatusReceiver.DELIVERED_INTENT);
        deliveryIntent.putExtra(SmsStatusReceiver.EXTRA_ID_DELIVER, cp.getDbId());

        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,sentIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0, deliveryIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        try {
            cp.setStatus(Checkpoint.STATUS_SENDING);
            checkpointsDao.update(cp);

            Log.d(TAG, "Sending SMS of checkpoint" + cp.getDbId());

            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(prefs.phoneNo().get(), null, cp.getSmsMesssage(), sentPI, deliveredPI);
        } catch (Exception e) {
            Snackbar.make(findViewById(android.R.id.content), R.string.sms_sending_failed, Snackbar.LENGTH_LONG).show();
            Log.e(TAG, e.getLocalizedMessage());
        }
    }
}
