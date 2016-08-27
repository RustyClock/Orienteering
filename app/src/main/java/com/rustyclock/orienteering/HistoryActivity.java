package com.rustyclock.orienteering;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.j256.ormlite.dao.Dao;
import com.rustyclock.orienteering.custom.ToolbarActivity;
import com.rustyclock.orienteering.db.DbHelper;
import com.rustyclock.orienteering.model.Checkpoint;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ItemClick;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.androidannotations.ormlite.annotations.OrmLiteDao;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * Created by Mateusz Jablonski
 * on 2015-05-27.
 */

@EActivity(R.layout.activity_history)
@OptionsMenu(R.menu.history)
public class HistoryActivity extends ToolbarActivity {

    private static final String TAG = HistoryActivity.class.getSimpleName();

    @ViewById ListView listView;
    @ViewById(R.id.swiperefresh) SwipeRefreshLayout swipeRefreshLayout;

    @Pref Prefs_ prefs;

    @OrmLiteDao(helper = DbHelper.class)
    Dao<Checkpoint, Integer> checkpointsDao;

    @OptionsItem(R.id.action_clear)
    void clear() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.clear_history_title))
                .setMessage(getString(R.string.clear_history_text))
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            List<Checkpoint> checkpoints = checkpointsDao.queryForAll();
                            checkpointsDao.delete(checkpoints);
                            reloadHistory();
                            Snackbar.make(findViewById(android.R.id.content), getString(R.string.history_clear_success), Snackbar.LENGTH_LONG).show();
                        } catch (SQLException e) {
                            Snackbar.make(findViewById(android.R.id.content), getString(R.string.history_clear_failed), Snackbar.LENGTH_LONG).show();
                            e.printStackTrace();
                        }
                    }
                })
                .create();

        dialog.show();
    }

    @ItemClick(R.id.listView)
    public void checkPointClicked(Checkpoint cp) {

        if(cp.getStatus()==Checkpoint.STATUS_SENT || cp.getStatus()==Checkpoint.STATUS_DELIVERED)
            return;

        Intent n = new Intent(Intent.ACTION_VIEW);
        n.setType("vnd.android-dir/mms-sms");
        n.putExtra("address", prefs.phoneNo().get());
        startActivity(n);
    }

    @AfterViews
    void aftrerViews() {
        setupToolbar(true);
        listView.setEmptyView(findViewById(R.id.tv_empty));
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                reloadHistory();
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        reloadHistory();
    }

    private void reloadHistory() {
        try {
            List<Checkpoint> checkpoints = checkpointsDao.queryForAll();
            Collections.sort(checkpoints);
            listView.setAdapter(new HistoryAdapter(checkpoints));

        } catch (SQLException e) {
            Log.e(TAG, "Error reading history data", e);
        }
    }

    private class HistoryAdapter extends ArrayAdapter<Checkpoint> {

        public HistoryAdapter(List<Checkpoint> checkpoints) {
            super(HistoryActivity.this, R.layout.item_checkpoint_history, checkpoints);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.item_checkpoint_history, parent, false);
            TextView tvChekpoint = (TextView) view.findViewById(R.id.tv_checkpoint);
            TextView tvChekpointCode = (TextView) view.findViewById(R.id.tv_checkpoint_code);
            TextView tvDate = (TextView) view.findViewById(R.id.tv_date);
            TextView tvHour = (TextView) view.findViewById(R.id.tv_hour);
            ImageView status = (ImageView) view.findViewById(R.id.view_sms_status);

            Checkpoint cp = getItem(position);

            DrawableCompat.setTint(status.getDrawable(), cp.getStatusColor(HistoryActivity.this));

            cp.displayChekpointData(tvChekpoint, tvChekpointCode);
            cp.displayDate(tvDate, tvHour);

            return view;
        }
    }
}
