package com.octoblu.blu;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.octoblue.blu.shared.Trigger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlowService extends IntentService {
    private static final String TAG = "Flow:Service";
    private static final String UUID_KEY = "uuid";
    private static final String TOKEN_KEY = "token";
    private static final String MESSAGE_DEVICE_URL = "http://meshblu.octoblu.com/messages";
    private static final String FLOWS_URL = "http://app.octoblu.com/api/flows";
    public static final String TRIGGERS_REFRESH_REQUEST = "com.octoblu.blu.TRIGGERS_REFRESH_REQUEST";
    public static final String TRIGGERS_UPDATE_PKG = "com.octoblu.blu.TRIGGERS_UPDATE_PKG";
    public static final String TRIGGER_PRESSED = "com.octoblu.blu.TRIGGER_PRESSED";
    public static final String TRIGGER_RESULT = "com.octoblu.blu.TRIGGER_RESULT";

    private List<Trigger> triggers;
    private RequestQueue requestQueue;

    public FlowService() {
        super("FlowService");
    }

    public FlowService(String name) {
        super(name);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        requestQueue = Volley.newRequestQueue(this);
        triggers = new ArrayList<Trigger>();
    }

    @Override
    public void onHandleIntent(Intent intent) {
        if (intent!=null) {
            if (intent.getAction().equals(TRIGGERS_REFRESH_REQUEST)) {
                refresh();
            }
            if (intent.getAction().equals(TRIGGER_PRESSED)) {
                fireTrigger(intent.getStringExtra("flowId"),
                        intent.getStringExtra("triggerId"),
                        intent.getIntExtra("index", 0));
            }
        }
    }

    private void fireTrigger(final String flowId, final String triggerId, final int i) {
        SharedPreferences preferences = getSharedPreferences(LoginActivity.PREFERENCES_FILE_NAME, 0);
        if(!preferences.contains(UUID_KEY) || !preferences.contains(TOKEN_KEY)) {
            Log.e(TAG,"Missing uuid or token");
            return;
        }

        final String uuid = preferences.getString(UUID_KEY, null);
        final String token = preferences.getString(TOKEN_KEY, null);

        requestQueue.add(getTriggerRequest(flowId, triggerId, uuid, token, i));
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
                if (volleyError!=null) {
                    if(volleyError.networkResponse!=null && volleyError.networkResponse.statusCode != 200)
                        Log.e(TAG, volleyError.getMessage(), volleyError);
                } else
                    Log.e(TAG,"Unknown volley error");
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return getAuthHeaders(uuid, token);
            }
        };
    }

    private StringRequest getTriggerRequest(final String flowId, final String triggerId, final String uuid, final String token, int i) {

        final Intent intent = new Intent(TRIGGER_RESULT);
        intent.putExtra("flowId",flowId);
        intent.putExtra("triggerId",triggerId);
        intent.putExtra("index",i);

        return new StringRequest(Request.Method.POST, MESSAGE_DEVICE_URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String s) {
                intent.putExtra("result",s);
                sendBroadcast(intent);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                intent.putExtra("error",(volleyError!=null ? volleyError.getMessage() : null));
                sendBroadcast(intent);
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

    private void syncTriggers(ArrayList<Trigger> triggers) {
        this.triggers.clear();
        this.triggers.addAll(triggers);

        Intent intent = new Intent(TRIGGERS_UPDATE_PKG);
        intent.putParcelableArrayListExtra("triggers", triggers);

        if (triggers.size()>0) {
            sendBroadcast(intent);
            intent.setClass(this,FlowWearService.class);
            startService(intent);
        }
    }

}
