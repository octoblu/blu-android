package com.octoblu.blu;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.melnykov.fab.FloatingActionButton;

import java.util.ArrayList;

public class BluActivity extends Activity implements AdapterView.OnItemClickListener, GoogleApiClient.ConnectionCallbacks, SwipeRefreshLayout.OnRefreshListener, MessageApi.MessageListener, GoogleApiClient.OnConnectionFailedListener {
    final static String TAG = "FlowYo";
    public static final String UUID_KEY = "uuid";
    public static final String TOKEN_KEY = "token";


    private ColorListAdapter colorListAdapter;
    private GoogleApiClient googleApiClient;
    private SwipeRefreshLayout refreshLayout;
    private ArrayList<DataMap> triggers;


    @Override
    public void onConnected(Bundle bundle) {
        Wearable.MessageApi.addListener(googleApiClient, this);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "onConnectionFailed: " + connectionResult.getErrorCode());
    }

    @Override
    public void onConnectionSuspended(int i) {}


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flow_yo);

        triggers = new ArrayList<DataMap>();

        googleApiClient = new GoogleApiClient.Builder(this).addApi(Wearable.API).addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();
        googleApiClient.connect();

        ActionBar actionBar = getActionBar();
        if(actionBar != null) {
            actionBar.hide();
        }

        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.refreshContainer);
        refreshLayout.setOnRefreshListener(this);

        ListView flowList = (ListView) findViewById(R.id.flowList);

        colorListAdapter = new ColorListAdapter(this, R.layout.trigger_list_item);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.flow_yo, menu);
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        final DataMap trigger = this.triggers.get(i);

        String flowId = trigger.getString("flowId");
        String triggerId = trigger.getString("triggerId");

        sendMessageToService("Trigger", (flowId + "/" + triggerId).getBytes());
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if(!messageEvent.getPath().equals("Refreshed")) {
            return;
        }

        loadItemsFromDataApi();
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
        loadItemsFromDataApi();
    }

    private PendingResult<NodeApi.GetLocalNodeResult> getLocalNode() {
        PendingResult<NodeApi.GetLocalNodeResult> nodesResult = Wearable.NodeApi.getLocalNode(googleApiClient);
        return nodesResult;
    }

    private void loadItemsFromDataApi(){
        PendingResult<DataItemBuffer> pendingResult = Wearable.DataApi.getDataItems(googleApiClient);
        pendingResult.setResultCallback(new ResultCallback<DataItemBuffer>() {
            @Override
            public void onResult(DataItemBuffer dataItems) {
                colorListAdapter.clear();

                for (DataItem dataItem : dataItems) {
                    DataMap dataMap = DataMapItem.fromDataItem(dataItem).getDataMap();
                    triggers = dataMap.getDataMapArrayList("triggers");
                    for (DataMap trigger : triggers) {
                        colorListAdapter.add(trigger.getString("triggerName"));
                    }
                }
                refreshLayout.setRefreshing(false);
                dataItems.release();
            }
        });
    }

    public void refreshTriggers() {
        refreshLayout.setRefreshing(true);
        sendMessageToService("Refresh", null);
    }

    public void sendMessageToService(final String path, final byte[] message) {
        getLocalNode().setResultCallback(new ResultCallback<NodeApi.GetLocalNodeResult>() {
            @Override
            public void onResult(NodeApi.GetLocalNodeResult localNodeResult) {
                Node node = localNodeResult.getNode();
                Wearable.MessageApi.sendMessage(googleApiClient, node.getId(), path, message);
            }
        });
    }
}
