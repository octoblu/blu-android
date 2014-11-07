package com.octoblu.blu;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlowService extends WearableListenerService implements GoogleApiClient.ConnectionCallbacks {
    private static final String TAG = "FlowYoWear:WearableService";
    private static final String UUID_KEY = "uuid";
    private static final String TOKEN_KEY = "token";
    private static final String MESSAGE_DEVICE_URL = "http://meshblu.octoblu.com/messages";
    private static final String FLOWS_URL = "http://app.octoblu.com/api/flows";

    private List<Trigger> triggers;
    private RequestQueue requestQueue;
    private GoogleApiClient googleApiClient;


    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "Connected to Google Api Service");
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onCreate() {
        super.onCreate();

        googleApiClient = new GoogleApiClient.Builder(this).addApi(Wearable.API).addConnectionCallbacks(this).build();
        googleApiClient.connect();

        requestQueue = Volley.newRequestQueue(this);
        triggers = new ArrayList<Trigger>();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        Log.d(TAG, "onMessageReceived");
        if(messageEvent.getPath().equals("Refresh")){
            refresh();
            return;
        }

        if(messageEvent.getPath().equals("Trigger")) {
            String data = new String(messageEvent.getData());
            String[] strings = data.split("/");
            String flowId = strings[0];
            String triggerId = strings[1];
            fireTrigger(flowId, triggerId);
        }
    }

    private void fireTrigger(final String flowId, final String triggerId) {
        SharedPreferences preferences = getSharedPreferences(LoginActivity.PREFERENCES_FILE_NAME, 0);

        if(!preferences.contains(UUID_KEY) || !preferences.contains(TOKEN_KEY)) {
            return;
        }

        final String uuid = preferences.getString(UUID_KEY, null);
        final String token = preferences.getString(TOKEN_KEY, null);

        requestQueue.add(getTriggerRequest(flowId, triggerId, uuid, token));
    }



    private Map<String, String> getAuthHeaders(String uuid, String token) {
        HashMap<String, String> headers = new HashMap<String, String>(2);
        headers.put("skynet_auth_uuid", uuid);
        headers.put("skynet_auth_token", token);
        return headers;
    }

    private JsonArrayRequest getFlowsRequest(final String uuid, final String token) {
        return new JsonArrayRequest(FLOWS_URL, new Response.Listener<JSONArray>(){
            @Override
            public void onResponse(JSONArray jsonArray) {
                syncTriggers(parseTriggers(jsonArray));
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.e(TAG, volleyError.getMessage(), volleyError);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return getAuthHeaders(uuid, token);
            }
        };
    }

    private PendingResult<NodeApi.GetLocalNodeResult> getLocalNode() {
        PendingResult<NodeApi.GetLocalNodeResult> nodesResult = Wearable.NodeApi.getLocalNode(googleApiClient);
        return nodesResult;
    }

    private StringRequest getTriggerRequest(final String flowId, final String triggerId, final String uuid, final String token) {
        return new StringRequest(Request.Method.POST, MESSAGE_DEVICE_URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String s) {

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {

            }
        }) {
            @Override
            public byte[] getBody() throws AuthFailureError {
                JSONObject json = new JSONObject();
                JSONObject payload = new JSONObject();

                try {
                    json.put("devices", flowId);
                    json.put("topic", "button");
                    payload.put("from", triggerId);
                    json.put("payload", payload);
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage(), e);
                }

                return json.toString().getBytes();
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = getAuthHeaders(uuid, token);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };
    }

    private ArrayList<Trigger> parseTriggers(JSONArray flows){
        ArrayList<Trigger> triggers = new ArrayList<Trigger>();

        try {
            for(int i = 0; i < flows.length(); i++) {
                JSONObject flow = flows.getJSONObject(i);
                JSONArray nodes = flow.getJSONArray("nodes");

                for(int j = 0; j < nodes.length(); j++) {
                    JSONObject node = nodes.getJSONObject(j);
                    if(!node.getString("type").equals("operation:trigger")) {
                        continue;
                    }

                    Trigger trigger = new Trigger(flow.getString("flowId"), flow.getString("name"), node.getString("id"), node.getString("name"));
                    triggers.add(trigger);

                }
            }
        } catch (JSONException jsonException) {
            Log.e(TAG, jsonException.getMessage(), jsonException);
        }

        return triggers;
    }

    private void refresh() {
        SharedPreferences preferences = getSharedPreferences(LoginActivity.PREFERENCES_FILE_NAME, 0);

        if(!preferences.contains(UUID_KEY) || !preferences.contains(TOKEN_KEY)) {
            return;
        }

        String uuid = preferences.getString(UUID_KEY, null);
        String token = preferences.getString(TOKEN_KEY, null);

        requestQueue.add(getFlowsRequest(uuid, token));
    }

    public void sendMessageToUI(final String path, final byte[] message) {
        getLocalNode().setResultCallback(new ResultCallback<NodeApi.GetLocalNodeResult>() {
            @Override
            public void onResult(NodeApi.GetLocalNodeResult localNodeResult) {
                Node node = localNodeResult.getNode();
                Wearable.MessageApi.sendMessage(googleApiClient, node.getId(), path, message);
            }
        });
    }

    private void syncTriggers(List<Trigger> triggers) {
        this.triggers.clear();
        this.triggers.addAll(triggers);

        ArrayList<DataMap> dataMaps = new ArrayList<DataMap>(triggers.size());
        for(Trigger trigger: triggers){
            DataMap dataMap = new DataMap();
            dataMap.putString("flowId", trigger.getFlowId());
            dataMap.putString("triggerId", trigger.getTriggerId());
            dataMap.putString("triggerName", trigger.getTriggerName());
            dataMaps.add(dataMap);
        }
        PutDataMapRequest dataMap = PutDataMapRequest.create("/triggers");
        dataMap.getDataMap().putDataMapArrayList("triggers", dataMaps);

        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(googleApiClient, dataMap.asPutDataRequest());
        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(DataApi.DataItemResult dataItemResult) {
                Log.d(TAG, "Watch received data");
            }
        });
        sendMessageToUI("Refreshed", null);
    }
}
