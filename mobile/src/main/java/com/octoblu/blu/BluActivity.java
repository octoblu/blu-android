package com.octoblu.blu;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
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
                if (intent.getAction().equals(FlowService.TRIGGERS_UPDATE_PKG)) {
                    loadItemsFromIntent(intent);
                }
                if (intent.getAction().equals(FlowService.TRIGGER_RESULT)) {
                    colorListAdapter.setLoading(intent.getIntExtra("index",0),View.GONE);
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(FlowService.TRIGGERS_UPDATE_PKG);
        filter.addAction(FlowService.TRIGGER_RESULT);
        registerReceiver(receiver,filter);

        ActionBar actionBar = getActionBar();
        if(actionBar != null) {
            actionBar.hide();
        }

        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.refreshContainer);
        refreshLayout.setOnRefreshListener(this);

        ListView flowList = (ListView) findViewById(R.id.flowList);

        colorListAdapter = new ColorListAdapter(this, R.layout.trigger_list_item, R.id.triggerName, R.id.triggerLoading);

        flowList.setEmptyView(findViewById(R.id.noTriggersText));
        flowList.setAdapter(colorListAdapter);
        flowList.setOnItemClickListener(this);

        FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.floatingActionButton);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor preferences = getSharedPreferences(LoginActivity.PREFERENCES_FILE_NAME, 0).edit();
                preferences.clear();
                preferences.commit();
                onStart();
            }
        });
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.flow_yo, menu);
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        final Trigger trigger = this.triggers.get(i);
        Intent intent = new Intent(FlowService.TRIGGER_PRESSED);
        intent.putExtra("flowId", trigger.getFlowId());
        intent.putExtra("triggerId", trigger.getTriggerId());
        intent.putExtra("uri", trigger.getUri());
        intent.putExtra("index",i);
        intent.setClass(this, FlowService.class);
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
        if (intent == null)
            return;

        itemsLoaded = true;
        refreshLayout.setRefreshing(false);

        if ( !intent.hasExtra("triggers")  ) {
            Log.e(TAG, "Missing required extra on loadItemsFromIntent " +
                    (intent.getExtras() != null ? TextUtils.join(", ", intent.getExtras().keySet()) : "null extras"));
            return;
        }

        colorListAdapter.clear();
        triggers.clear();

        for (Parcelable pTrigger : intent.getParcelableArrayListExtra("triggers")) {
            Trigger trigger = (Trigger)pTrigger;
            triggers.add(trigger);
            colorListAdapter.add(trigger.getTriggerName());
        }
    }

    public void refreshTriggers() {
        itemsLoaded = false;
        handler.postDelayed(refreshRunnable, 50);
        startService(new Intent(FlowService.TRIGGERS_REFRESH_REQUEST, null, this, FlowService.class));
    }
}
