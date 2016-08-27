package com.rustyclock.orienteering.model;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.j256.ormlite.field.DatabaseField;
import com.rustyclock.orienteering.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Mateusz Jablonski
 * on 2015-05-27.
 */


public class Checkpoint implements Comparable<Checkpoint> {

    public static final String SPLITTER = ":";

    public static final int STATUS_RECOGNIZED = 1;
    public static final int STATUS_SENDING = 2;
    public static final int STATUS_SENT = 3;
    public static final int STATUS_DELIVERED = 4;
    public static final int STATUS_FAILED = 5;

    @DatabaseField(generatedId = true)
    int dbId;

    @DatabaseField
    private String scannedData;

    @DatabaseField
    private Date scanDate;

    @DatabaseField
    private int status;

    @DatabaseField
    private boolean manual;

    public Checkpoint() {
    }

    public Checkpoint(String data) {
        this.scannedData = data;

        this.scanDate = new Date();
        this.status = STATUS_RECOGNIZED;
    }

    public Date getScanDate() {
        return scanDate;
    }

    public String getFormattedDate(boolean withSeconds) {
        DateFormat writeFormat = new SimpleDateFormat("HH:mm" + (withSeconds ? ":ss" : "") + " dd-MM-yyyy", new Locale("pl_PL"));
        return writeFormat.format(scanDate);
    }

    public void displayChekpointData(TextView checkpoint, TextView code) {
        if(scannedData.contains(SPLITTER)) {
            String[] splits = scannedData.split(SPLITTER);
            checkpoint.setText(splits[0]);
            code.setText(splits[1]);
        } else {
            checkpoint.setText(manual ? "R" : "-");
            code.setText(scannedData);
        }
    }

    public void displayDate(TextView date, TextView hour) {
        String[] splits = getFormattedDate(true).split(" ");
        hour.setText(splits[0]);
        date.setText(splits[1]);
    }

    public String getSmsMesssage() {

        String checkPointCode = scannedData;

        if(scannedData.contains(SPLITTER)) {
            String[] splits = scannedData.split(SPLITTER);
            if(splits.length>1)
                checkPointCode = splits[1];
        }

        return checkPointCode + " " + getFormattedDate(false);
    }

    public int getStatus() {
        return status;
    }

    public int getStatusColor(Context ctx) {

        int color = R.color.divider;

        switch (status) {
            case STATUS_RECOGNIZED:
            case STATUS_SENDING: color = R.color.divider; break;
            case STATUS_SENT: color = R.color.yellow; break;
            case STATUS_DELIVERED: color = R.color.primary; break;
            case STATUS_FAILED: color = R.color.red; break;
        }

        return ContextCompat.getColor(ctx, color);
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean isManual() {
        return manual;
    }

    public void setManual(boolean manual) {
        this.manual = manual;
    }

    public int getDbId() {
        return dbId;
    }

    @Override
    public int compareTo(@NonNull Checkpoint ch) {
        return ch.getScanDate().compareTo(scanDate);
    }

    public void setCode(String code) {
        scannedData = code;
        scanDate = new Date();
        status = STATUS_RECOGNIZED;
    }
}
