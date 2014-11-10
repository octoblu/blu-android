package com.octoblu.blu;

import android.app.Service;
import android.content.Intent;
import android.os.Parcelable;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.octoblue.blu.shared.Trigger;

import java.util.ArrayList;

public class FlowWearService extends WearableListenerService {

    private static final String TAG = "FlowWear:WearableService";
    private GoogleApiClient googleApiClient;

    @Override
    public void onCreate() {
        super.onCreate();
        googleApiClient = new GoogleApiClient.Builder(this).addApi(Wearable.API).build();
        googleApiClient.connect();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || !intent.getAction().equals(FlowService.TRIGGERS_UPDATE_PKG)) {
            return super.onStartCommand(intent, flags, startId);
        }

        ArrayList<DataMap> triggers = parseTriggersFromIntent(intent);
        pushTriggersToWatch(triggers);

        return Service.START_NOT_STICKY;

    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        Log.d(TAG, "onMessageReceived");
        if(messageEvent.getPath().equals("Refresh")){
            startService(new Intent(FlowService.TRIGGERS_REFRESH_REQUEST, null, this, FlowService.class));
        } else if(messageEvent.getPath().equals("Trigger")) {
            String[] strings = new String(messageEvent.getData()).split("/");

            Intent intent = new Intent(FlowService.TRIGGER_PRESSED);
            intent.putExtra("flowId", strings[0]);
            intent.putExtra("triggerId", strings[1]);
            intent.setClass(this, FlowService.class);

            startService(intent);
        }
    }

    private ArrayList<DataMap> parseTriggersFromIntent(Intent intent) {
        ArrayList<Parcelable> parcelables = intent.getParcelableArrayListExtra("triggers");
        ArrayList<DataMap> triggers = new ArrayList<DataMap>(parcelables.size());

        for (Parcelable pTrigger : parcelables) {
            Trigger trigger = (Trigger) pTrigger;
            DataMap dataMap = new DataMap();
            dataMap.putString("flowId", trigger.getFlowId());
            dataMap.putString("triggerId", trigger.getTriggerId());
            dataMap.putString("triggerName", trigger.getTriggerName());
            triggers.add(dataMap);
        }

        return triggers;
    }

    private void pushTriggersToWatch(ArrayList<DataMap> triggers){
        Log.d(TAG, "pushTriggersToWatch: " + triggers.size());
        PutDataMapRequest dataMapRequest = PutDataMapRequest.create("/triggers");
        dataMapRequest.getDataMap().putDataMapArrayList("triggers", triggers);

        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(googleApiClient, dataMapRequest.asPutDataRequest());
        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(DataApi.DataItemResult dataItemResult) {
                Log.d(TAG, "Watch received data");
            }
        });
    }
}
