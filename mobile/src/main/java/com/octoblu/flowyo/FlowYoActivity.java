package com.octoblu.flowyo;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

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
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.melnykov.fab.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FlowYoActivity extends Activity implements AdapterView.OnItemClickListener, GoogleApiClient.ConnectionCallbacks, MessageApi.MessageListener {
    final static String TAG = "FlowYo";
    public static final String UUID_KEY = "uuid";
    public static final String TOKEN_KEY = "token";

    final String flowsUrl = "http://app.octoblu.com/api/flows";
    final String messageDeviceUrl = "http://meshblu.octoblu.com/messages";
    private String uuid;
    private String token;
    private ColorListAdapter colorListAdapter;
    private ArrayList<Trigger> triggers;
    private RequestQueue requestQueue;
    private GoogleApiClient googleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flow_yo);

        googleApiClient = new GoogleApiClient.Builder(this).addApi(Wearable.API).addConnectionCallbacks(this).build();
        googleApiClient.connect();

        requestQueue = Volley.newRequestQueue(this);

        ActionBar actionBar = getActionBar();
        if(actionBar != null) {
            actionBar.hide();
        }

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
                uuid = null;
                token = null;
                triggers.clear();
                onStart();
            }
        });

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

        uuid = preferences.getString(UUID_KEY, null);
        token = preferences.getString(TOKEN_KEY, null);


        requestQueue.add(getFlowsRequest());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.flow_yo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    private JsonArrayRequest getFlowsRequest() {
        return new JsonArrayRequest(flowsUrl, new Response.Listener<JSONArray>(){
            @Override
            public void onResponse(JSONArray jsonArray) {
                colorListAdapter.clear();
                triggers = parseTriggers(jsonArray);

                for(Trigger trigger : triggers){
                    colorListAdapter.add(trigger.getTriggerName());
                }

                syncTriggersToWatch(triggers);

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.e(TAG, volleyError.getMessage(), volleyError);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return getAuthHeaders();
            }
        };
    }

    private Map<String, String> getAuthHeaders() {
        HashMap<String, String> headers = new HashMap<String, String>(2);
        headers.put("skynet_auth_uuid", uuid);
        headers.put("skynet_auth_token", token);
        return headers;
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

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        final Trigger trigger = triggers.get(i);
        fireTrigger(trigger);
    }

    private void fireTrigger(final Trigger trigger) {
        requestQueue.add(new StringRequest(Request.Method.POST, messageDeviceUrl, new Response.Listener<String>() {
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
                    json.put("devices", trigger.getFlowId());
                    json.put("topic", "button");
                    payload.put("from", trigger.getTriggerId());
                    json.put("payload", payload);
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage(), e);
                }

                return json.toString().getBytes();
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = getAuthHeaders();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        });
    }

    private void syncTriggersToWatch(ArrayList<Trigger> triggers) {
        ArrayList<DataMap> dataMaps = new ArrayList<DataMap>(triggers.size());
        for(Trigger trigger: triggers){
            DataMap dataMap = new DataMap();
            dataMap.putString("triggerId", trigger.getTriggerId());
            dataMap.putString("triggerName", trigger.getTriggerName());
            dataMaps.add(dataMap);
        }
        PutDataMapRequest dataMap = PutDataMapRequest.create("/triggers");
        dataMap.getDataMap().putDataMapArrayList("triggers", dataMaps);

        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(googleApiClient, dataMap.asPutDataRequest());
        Log.d(TAG, "sent data");
        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(DataApi.DataItemResult dataItemResult) {
                Log.d(TAG, "onResult resultCallback");
                Toast.makeText(getApplicationContext(), "data received by watch", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "Connected to Google Api Service");
        Wearable.MessageApi.addListener(googleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, "onMessageReceived");
        String triggerId = new String(messageEvent.getData());
        for(Trigger trigger : triggers){
            if(trigger.getTriggerId().equals(triggerId)) {
                fireTrigger(trigger);
                return;
            }
        }
    }
}
