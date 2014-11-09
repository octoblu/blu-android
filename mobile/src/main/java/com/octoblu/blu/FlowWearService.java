package com.octoblu.blu;

import android.app.Service;
import android.content.Intent;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
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
        if (intent != null) {
            Log.d(TAG, "onStartCommand " + intent.getAction());
            if (intent.getAction().equals(FlowService.TRIGGERS_UPDATE_PKG)) {
                loadItemsFromIntent(intent);
            } else
                return super.onStartCommand(intent, flags, startId);
        } else {
            Log.w(TAG,"onStartCommand got null intent");
        }
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        Log.d(TAG, "onMessageReceived");
        if(messageEvent.getPath().equals("Refresh")){
            startService(new Intent(FlowService.TRIGGERS_REFRESH_REQUEST, null, this, FlowService.class));
        } else if(messageEvent.getPath().equals("Trigger")) {
            Intent intent = new Intent(FlowService.TRIGGER_PRESSED);
            intent.putExtra("trigger", messageEvent.getData());
            intent.setClass(this, FlowService.class);
            startService(intent);
        }
    }

    private static Asset createAsset(Trigger trigger) {
        return Asset.createFromRef(trigger.getFlowId()+"\0"+trigger.getFlowName()+"\0"+trigger.getTriggerId()+"\0"+trigger.getTriggerName());
    }

    private void loadItemsFromIntent(Intent intent) {
        Log.d(TAG, "Sending watch data");

        if ( !intent.hasExtra("triggers")  ) {
            Log.e(TAG,"Missing required extra on loadItemsFromIntent " +
                    TextUtils.join(",", intent.getExtras().keySet()));
            return;
        }

        ArrayList<Parcelable> parcelables = intent.getParcelableArrayListExtra("triggers");
        PutDataMapRequest dataMapRequest = PutDataMapRequest.create("/triggers");
        ArrayList<DataMap> dataMaps = new ArrayList<DataMap>(parcelables.size());

        for (Parcelable pTrigger : parcelables) {
            Trigger trigger = (Trigger)pTrigger;
            DataMap dataMap = new DataMap();
            dataMap.putAsset("trigger",createAsset(trigger));
            dataMap.putLong("time",System.currentTimeMillis());
            dataMaps.add(dataMap);
        }

        dataMapRequest.getDataMap().putDataMapArrayList("triggers", dataMaps);

        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(googleApiClient, dataMapRequest.asPutDataRequest());
        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(DataApi.DataItemResult dataItemResult) {
                Log.d(TAG, "Watch received data");
            }
        });
    }
}
