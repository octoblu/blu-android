package com.octoblu.blu;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import com.octoblue.blu.shared.Trigger;
import com.octoblue.blu.shared.ColorListAdapter;

import java.util.ArrayList;

public class BluWearActivity extends Activity implements DataApi.DataListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, AdapterView.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener {

    public static final String TAG = "FlowYoWear:FlowYoWear";
    private ArrayList<DataMap> triggers;
    private ColorListAdapter colorListAdapter;
    private GoogleApiClient googleApiClient;
    private SwipeRefreshLayout refreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flow_yo);

        triggers = new ArrayList<DataMap>();
        colorListAdapter = new ColorListAdapter(this, R.layout.trigger_list_item, R.id.triggerName, -1);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        googleApiClient.connect();

        final BluWearActivity self = this;

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                Log.d(TAG,"onLayoutInflated");
                ListView triggerList = (ListView) stub.findViewById(R.id.triggerList);
                triggerList.setAdapter(colorListAdapter);
                triggerList.setOnItemClickListener(self);

                refreshLayout = (SwipeRefreshLayout) findViewById(R.id.refreshContainer);
                refreshLayout.setOnRefreshListener(self);
                refreshLayout.setColorSchemeColors(R.color.blue, R.color.purple, R.color.green, R.color.orange);
            }
        });

        Log.d(TAG, "end of onCreate");
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG,"onDataChanged loading data from data api");
        loadItemsFromDataApi();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG,"onConnected loading data from data api");
        Wearable.DataApi.addListener(googleApiClient, this);

        loadItemsFromDataApi();
        sendMessageToPhone("Refresh", null);
    }


    @Override
    protected void onStop() {
        if (null != googleApiClient && googleApiClient.isConnected()) {
            Wearable.DataApi.removeListener(googleApiClient, this);
            googleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.w(TAG, "Google API Service connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "Failed to connect to Google API Service");
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        final DataMap trigger = this.triggers.get(i);
        String flowId = trigger.getString("flowId");
        String triggerId = trigger.getString("triggerId");
        sendMessageToPhone("Trigger", (flowId + "/" + triggerId).getBytes());
    }

    @Override
    public void onRefresh() {
        refreshLayout.setRefreshing(true);
        sendMessageToPhone("Refresh", null);
    }

    private void loadItemsFromDataApi() {
        final PendingResult<DataItemBuffer> pendingResult = Wearable.DataApi.getDataItems(googleApiClient);
        pendingResult.setResultCallback(new ResultCallback<DataItemBuffer>() {
            @Override
            public void onResult(DataItemBuffer dataItems) {
                Log.d(TAG,"loadingItemsFromDataApi " + dataItems.getCount());

                triggers.clear();
                colorListAdapter.clear();
                for(DataItem dataItem : dataItems) {
                    DataMap dataMap = DataMapItem.fromDataItem(dataItem).getDataMap();
                    Log.d(TAG,"dataMap size " + dataMap.size());
                    ArrayList<DataMap> dataTriggers = dataMap.getDataMapArrayList("triggers");
                    Log.d(TAG,"triggers size " + dataTriggers.size());

                    for(DataMap trigger : dataTriggers) {
                        triggers.add(trigger);
                        colorListAdapter.add(trigger.getString("triggerName"));
                    }
                }
                if(refreshLayout != null){
                    refreshLayout.setRefreshing(false);
                }
                dataItems.release();
            }
        });
    }

    public void sendMessageToPhone(final String path, final byte[] message) {
        Wearable.NodeApi.getConnectedNodes(googleApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                for(Node node : getConnectedNodesResult.getNodes()) {
                    Wearable.MessageApi.sendMessage(googleApiClient, node.getId(), path, message);
                }
            }
        });
    }
}
