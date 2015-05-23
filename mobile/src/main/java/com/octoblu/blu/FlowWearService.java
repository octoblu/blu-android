package com.octoblu.blu;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Parcelable;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.octoblue.blu.shared.Trigger;

import java.util.ArrayList;

public class FlowWearService extends WearableListenerService {

    private static final String TAG = "FlowWearService";
    private GoogleApiClient googleApiClient;

    @Override
    public void onCreate() {
        super.onCreate();
        googleApiClient = new GoogleApiClient.Builder(this).addApi(Wearable.API).build();
        googleApiClient.connect();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || !intent.getAction().equals(TriggerService.TRIGGERS_UPDATE_PKG)) {
            return super.onStartCommand(intent, flags, startId);
        }

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, final Intent intent) {
                onTriggerResultIntent(intent);
            }
        }, new IntentFilter(TriggerService.TRIGGER_RESULT));

        pushTriggersToWatch(intent.getStringExtra("triggers"));

        return Service.START_NOT_STICKY;
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);

        switch (messageEvent.getPath()){
            case "TRIGGER":
                onFireTriggerMessage(messageEvent);
                break;
        }

    }

    private void onFireTriggerMessage(MessageEvent messageEvent) {
        Intent intent = new Intent(TriggerService.TRIGGER_PRESSED);
        intent.putExtra("trigger", new String(messageEvent.getData()));
        intent.setClass(this, TriggerService.class);

        startService(intent);
    }

    private void onTriggerResultIntent(final Intent intent) {
        Wearable.NodeApi.getConnectedNodes(googleApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                String message = intent.getStringExtra("trigger");
                for(Node node : getConnectedNodesResult.getNodes()) {
                    Wearable.MessageApi.sendMessage(googleApiClient, node.getId(), TriggerService.TRIGGER_RESULT, message.getBytes());
                }
            }
        });
    }

    private void pushTriggersToWatch(String triggersJSONString){
        PutDataMapRequest dataMapRequest = PutDataMapRequest.create("/triggers");
        dataMapRequest.getDataMap().putString("triggers", triggersJSONString);

        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(googleApiClient, dataMapRequest.asPutDataRequest());
        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(DataApi.DataItemResult dataItemResult) {
                Log.d(TAG, "Watch received data");
            }
        });
    }
}
