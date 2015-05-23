package com.octoblu.blu;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.octoblue.blu.shared.ColorListAdapter;
import com.octoblue.blu.shared.Trigger;

import java.util.ArrayList;

public class BluWearActivity extends Activity implements DataApi.DataListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, AdapterView.OnItemClickListener, MessageApi.MessageListener {

    public static final String TAG = "BluWearActivity";
    private ArrayList<Trigger> triggers;
    private ColorListAdapter colorListAdapter;
    private GoogleApiClient googleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flow_yo);

        triggers = new ArrayList<Trigger>();
        colorListAdapter = new ColorListAdapter(this, R.layout.trigger_list_item, R.id.triggerName, R.id.triggerLoading);

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
                ListView triggerList = (ListView) stub.findViewById(R.id.triggerList);
                triggerList.setEmptyView(findViewById(R.id.noTriggersText));
                triggerList.setAdapter(colorListAdapter);
                triggerList.setOnItemClickListener(self);
            }
        });
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        loadItemsFromDataApi();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        switch (messageEvent.getPath()) {
            case "com.octoblu.blu.TRIGGER_RESULT":
                onTriggerResultMessage(messageEvent);
                break;
            case "com.octoblu.blu.TRIGGER_ERROR_MESSAGE":
                onTriggerErrorMessage(messageEvent);
                break;
        }

    }

    private void onTriggerErrorMessage(final MessageEvent messageEvent) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String message = new String(messageEvent.getData());
                Toast.makeText(BluWearActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void onTriggerResultMessage(MessageEvent messageEvent) {
        final Trigger trigger = Trigger.fromJSON(new String(messageEvent.getData()));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
            colorListAdapter.setLoading(trigger.getIndex(), View.GONE);
            }
        });
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.DataApi.addListener(googleApiClient, this);
        Wearable.MessageApi.addListener(googleApiClient, this);

        loadItemsFromDataApi();
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
        final Trigger trigger = this.triggers.get(i);
        trigger.setIndex(i);
        colorListAdapter.setLoading(i,View.VISIBLE);
        sendMessageToPhone("TRIGGER", trigger.toJSON().getBytes());
    }

    private void loadItemsFromDataApi() {
        final PendingResult<DataItemBuffer> pendingResult = Wearable.DataApi.getDataItems(googleApiClient);
        pendingResult.setResultCallback(new ResultCallback<DataItemBuffer>() {
            @Override
            public void onResult(DataItemBuffer dataItems) {
                triggers.clear();
                colorListAdapter.clear();

                for(DataItem dataItem : dataItems) {
                    DataMap dataMap = DataMapItem.fromDataItem(dataItem).getDataMap();
                    String triggersJSONString = dataMap.getString("triggers");

                    for(Trigger trigger : Trigger.triggersFromJSON(triggersJSONString)) {
                        triggers.add(trigger);
                        colorListAdapter.add(trigger.getTriggerName());
                    }
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
