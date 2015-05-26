package com.octoblu.blu;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.melnykov.fab.FloatingActionButton;
import com.octoblue.blu.shared.ColorListAdapter;
import com.octoblue.blu.shared.Trigger;

import java.util.ArrayList;

public class BluActivity extends Activity implements AdapterView.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener {

    final static String TAG = "FlowYo";
    public static final String UUID_KEY = "uuid";
    public static final String TOKEN_KEY = "token";

    private ColorListAdapter colorListAdapter;
    private SwipeRefreshLayout refreshLayout;
    private SwipeRefreshLayout refreshLayoutEmptyState;
    private ArrayList<Trigger> triggers;
    private boolean itemsLoaded = false;

    private BroadcastReceiver receiver;
    private Handler handler = new Handler();
    private Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (!itemsLoaded)
                refreshLayout.setRefreshing(true);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flow_yo);

        triggers = new ArrayList<Trigger>();

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case TriggerService.TRIGGERS_UPDATE_PKG:
                        loadItemsFromIntent(intent);
                        break;
                    case TriggerService.TRIGGER_RESULT:
                        stopItemLoadingFromIntent(intent);
                        break;
                    case TriggerService.TRIGGER_ERROR_MESSAGE:
                        showErrorFromIntent(intent);
                        break;
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(TriggerService.TRIGGERS_UPDATE_PKG);
        filter.addAction(TriggerService.TRIGGER_RESULT);
        filter.addAction(TriggerService.TRIGGER_ERROR_MESSAGE);
        registerReceiver(receiver, filter);

        ActionBar actionBar = getActionBar();
        if(actionBar != null) {
            actionBar.hide();
        }

        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.refreshContainer);
        refreshLayout.setOnRefreshListener(this);
        refreshLayoutEmptyState = (SwipeRefreshLayout) findViewById(R.id.refreshContainerEmptyState);
        refreshLayoutEmptyState.setOnRefreshListener(this);

        ListView flowList = (ListView) findViewById(R.id.flowList);

        colorListAdapter = new ColorListAdapter(this, R.layout.trigger_list_item, R.id.triggerName, R.id.triggerLoading);

        flowList.setEmptyView(findViewById(R.id.refreshContainerEmptyState));
        flowList.setAdapter(colorListAdapter);
        flowList.setOnItemClickListener(this);

        FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.floatingActionButton);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickLogout();
            }
        });
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    private void onClickLogout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Are you sure?");
        builder.setMessage("This will log you out of your account.");
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.setPositiveButton("Logout", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences.Editor preferences = getSharedPreferences(LoginActivity.PREFERENCES_FILE_NAME, 0).edit();
                preferences.clear();
                preferences.commit();
                onStart();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.flow_yo, menu);
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        final Trigger trigger = this.triggers.get(i);
        trigger.setIndex(i);
        Intent intent = new Intent(TriggerService.TRIGGER_PRESSED);
        intent.putExtra("trigger", trigger.toJSON());
        intent.setClass(this, TriggerService.class);
        colorListAdapter.setLoading(i,View.VISIBLE);
        startService(intent);
    }

    @Override
    public void onRefresh() {
        refreshTriggers();
    }

    @Override
    protected void onStart() {
        super.onStart();

        SharedPreferences preferences = getSharedPreferences(LoginActivity.PREFERENCES_FILE_NAME, 0);

        if(!preferences.contains(UUID_KEY) || !preferences.contains(TOKEN_KEY)) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            return;
        }

        refreshTriggers();
    }

    private void loadItemsFromIntent(Intent intent) {
        itemsLoaded = true;
        refreshLayout.setRefreshing(false);
        refreshLayoutEmptyState.setRefreshing(false);

        colorListAdapter.clear();
        this.triggers.clear();

        String triggersJSONString = intent.getStringExtra("triggers");
        for(Trigger trigger : Trigger.triggersFromJSON(triggersJSONString)) {
            this.triggers.add(trigger);
            colorListAdapter.add(trigger.getTriggerName());
        }
    }

    private void showErrorFromIntent(Intent intent) {
        String message = intent.getStringExtra("message");
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void stopItemLoadingFromIntent(Intent intent) {
        Trigger trigger = Trigger.fromJSON(intent.getStringExtra("trigger"));
        Integer index = trigger.getIndex();
        colorListAdapter.setLoading(index, View.GONE);
    }

    public void refreshTriggers() {
        itemsLoaded = false;
        handler.postDelayed(refreshRunnable, 50);
        startService(new Intent(TriggerService.TRIGGERS_REFRESH_REQUEST, null, this, TriggerService.class));
    }
}
