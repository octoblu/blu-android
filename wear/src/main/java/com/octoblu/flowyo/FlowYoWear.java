package com.octoblu.flowyo;

import android.app.Activity;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

public class FlowYoWear extends Activity implements DataApi.DataListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, AdapterView.OnItemClickListener {

    public static final String TAG = "FlowYoWear";
    private TextView mTextView;
    private ArrayList<DataMap> triggers;
    private ArrayAdapter<String> arrayAdapter;
    private GoogleApiClient googleApiClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("FlowYoWear", "onCreate");
        setContentView(R.layout.activity_flow_yo);

        triggers = new ArrayList<DataMap>();
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        googleApiClient.connect();

        final FlowYoWear self = this;

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                ListView triggerList = (ListView) stub.findViewById(R.id.triggerList);
                triggerList.setAdapter(arrayAdapter);
                triggerList.setOnItemClickListener(self);
            }
        });

    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "onDataChanged");

        for(DataEvent event : dataEvents) {
            final DataMap dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    triggers = dataMap.getDataMapArrayList("triggers");
                    for(DataMap trigger : triggers) {
                        arrayAdapter.add(trigger.getString("triggerName"));
                    }
                }
            });

        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "Connected to Google Api Service");
        Wearable.DataApi.addListener(googleApiClient, this);

        PendingResult<DataItemBuffer> pendingResult = Wearable.DataApi.getDataItems(googleApiClient);
        pendingResult.setResultCallback(new ResultCallback<DataItemBuffer>() {
            @Override
            public void onResult(DataItemBuffer dataItems) {
                Log.d(TAG, "getDataItems onResult");
                for(DataItem dataItem : dataItems) {
                    DataMap dataMap = DataMapItem.fromDataItem(dataItem).getDataMap();
                    triggers = dataMap.getDataMapArrayList("triggers");
                    for(DataMap trigger : triggers) {
                        arrayAdapter.add(trigger.getString("triggerName"));
                    }
                }
            }
        });
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
        PendingResult<NodeApi.GetConnectedNodesResult> nodesResult = getNodes();
        nodesResult.setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult nodes) {
                for(Node node : nodes.getNodes()) {
                    Wearable.MessageApi.sendMessage(googleApiClient, node.getId(), "FlowYoActivity", trigger.getString("triggerId").getBytes());
                }
            }
        });
    }

    private PendingResult<NodeApi.GetConnectedNodesResult> getNodes() {
        HashSet<String> results= new HashSet<String>();

        PendingResult<NodeApi.GetConnectedNodesResult> nodesResult = Wearable.NodeApi.getConnectedNodes(googleApiClient);
        return nodesResult;
    }
}
