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

public class TriggerService extends IntentService {
    private static final String TAG = "Flow:Service";
    private static final String UUID_KEY = "uuid";
    private static final String TOKEN_KEY = "token";

    public static final String TRIGGERS_REFRESH_REQUEST = "com.octoblu.blu.TRIGGERS_REFRESH_REQUEST";
    public static final String TRIGGERS_UPDATE_PKG = "com.octoblu.blu.TRIGGERS_UPDATE_PKG";
    public static final String TRIGGER_PRESSED = "com.octoblu.blu.TRIGGER_PRESSED";
    public static final String TRIGGER_RESULT = "com.octoblu.blu.TRIGGER_RESULT";

    private RequestQueue requestQueue;

    @SuppressWarnings("unused") // Required for Android to call this Service
    public TriggerService(){
        super("TriggerService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        requestQueue = Volley.newRequestQueue(this);
    }

    @Override
    public void onHandleIntent(Intent intent) {
        if (intent!=null) {
            if (intent.getAction().equals(TRIGGERS_REFRESH_REQUEST)) {
                refresh();
            }
            if (intent.getAction().equals(TRIGGER_PRESSED)) {
                String triggerJSON = intent.getStringExtra("trigger");
                Trigger trigger = Trigger.fromJSON(triggerJSON);
                fireTrigger(trigger);
            }
        }
    }

    private void fireTrigger(final Trigger trigger) {
        SharedPreferences preferences = getSharedPreferences(LoginActivity.PREFERENCES_FILE_NAME, 0);
        if(!preferences.contains(UUID_KEY) || !preferences.contains(TOKEN_KEY)) {
            Log.e(TAG,"Missing uuid or token");
            return;
        }

        final String uuid = preferences.getString(UUID_KEY, null);
        final String token = preferences.getString(TOKEN_KEY, null);

        requestQueue.add(getTriggerRequest(trigger, uuid, token));
    }

    private Map<String, String> getAuthHeaders(String uuid, String token) {
        HashMap<String, String> headers = new HashMap<String, String>(2);
        headers.put("meshblu_auth_uuid", uuid);
        headers.put("meshblu_auth_token", token);
        return headers;
    }

    private StringRequest getTriggersRequest(final String uuid, final String token) {
        return new StringRequest(BluConfig.TRIGGERS_URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String triggersJSONString) {
                TriggerService.this.syncTriggers(triggersJSONString);
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

    private StringRequest getTriggerRequest(final Trigger trigger, final String uuid, final String token) {

        final Intent intent = new Intent(TRIGGER_RESULT);
        intent.putExtra("trigger", trigger.toJSON());
        final Intent errorIntent = new Intent(intent);
        errorIntent.putExtra("error", "Error firing Trigger");

        return new StringRequest(Request.Method.POST, trigger.getUri(), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                TriggerService.this.sendBroadcast(intent);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                TriggerService.this.sendBroadcast(errorIntent);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = getAuthHeaders(uuid, token);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };
    }

    private void refresh() {
        SharedPreferences preferences = getSharedPreferences(LoginActivity.PREFERENCES_FILE_NAME, 0);

        if(!preferences.contains(UUID_KEY) || !preferences.contains(TOKEN_KEY)) {
            return;
        }

        String uuid = preferences.getString(UUID_KEY, null);
        String token = preferences.getString(TOKEN_KEY, null);

        requestQueue.add(getTriggersRequest(uuid, token));
    }

    private void syncTriggers(String triggersJSONString) {
        Intent intent = new Intent(TRIGGERS_UPDATE_PKG);
        intent.putExtra("triggers", triggersJSONString);

        sendBroadcast(intent);
        intent.setClass(this,FlowWearService.class);
        startService(intent);
    }

}
