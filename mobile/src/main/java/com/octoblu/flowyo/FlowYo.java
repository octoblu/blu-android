package com.octoblu.flowyo;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class FlowYo extends Activity implements AdapterView.OnItemClickListener {
    final static String TAG = "FlowYo";
    final String flowsUrl = "http://app.octoblu.com/api/flows";
    final String messageDeviceUrl = "http://meshblu.octoblu.com/messages";
    final String uuid  = "9a55e7f0-2329-11e4-91e7-271d432ec4ce";
    final String token = "4969eddcf93ad6f685c041cb62193df7870311c9";
    private ArrayAdapter<String> flowListAdapter;
    private ArrayList<Trigger> triggers;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flow_yo);

        ListView flowList = (ListView) findViewById(R.id.flowList);

        flowListAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        flowList.setAdapter(flowListAdapter);
        flowList.setOnItemClickListener(this);

        requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(getFlowsRequest());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.flow_yo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private JsonArrayRequest getFlowsRequest() {
        return new JsonArrayRequest(flowsUrl, new Response.Listener<JSONArray>(){
            @Override
            public void onResponse(JSONArray jsonArray) {
                flowListAdapter.clear();
                triggers = parseTriggers(jsonArray);

                for(Trigger trigger : triggers){
                    flowListAdapter.add(trigger.getTriggerName());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.e(TAG, volleyError.getMessage());
                Log.w(TAG, volleyError.getStackTrace().toString());
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
            Log.e(TAG, jsonException.getMessage());
            Log.w(TAG, jsonException.getStackTrace().toString());
        }

        return triggers;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        final Trigger trigger = triggers.get(i);
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
                    Log.e(TAG, e.getMessage());
                    Log.w(TAG, e.getStackTrace().toString());
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
}
