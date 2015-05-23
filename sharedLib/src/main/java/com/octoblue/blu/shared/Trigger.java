package com.octoblue.blu.shared;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Trigger {

    private static final String TAG = "Trigger";

    private Integer index;
    private String flowId;
    private String flowName;
    private String triggerId;
    private String triggerName;
    private String uri;

    public Trigger(String flowId, String flowName, String triggerId, String triggerName, String uri, Integer index) {
        this.index = index;
        this.flowId = flowId;
        this.flowName = flowName;
        this.triggerId = triggerId;
        this.triggerName = triggerName;
        this.uri = uri;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index){
        this.index = index;
    }

    public String getFlowId() {
        return flowId;
    }

    public String getFlowName() {
        return flowName;
    }

    public String getTriggerId() {
        return triggerId;
    }

    public String getTriggerName() {
        return triggerName;
    }

    public String getUri() {
        return uri;
    }

    public String toJSON() {
        return toJSONObject().toString();
    }

    private JSONObject toJSONObject() {
        JSONObject triggerJSON = new JSONObject();

        try {
            triggerJSON.put("index", index);
            triggerJSON.put("flowId", flowId);
            triggerJSON.put("flowName", flowName);
            triggerJSON.put("id", triggerId);
            triggerJSON.put("name", triggerName);
            triggerJSON.put("uri", uri);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return triggerJSON;
    }


    public static Trigger fromJSON(String triggerJSONString) {
        JSONObject triggerJSON = new JSONObject();
        try {
            triggerJSON = new JSONObject(triggerJSONString);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return fromJSON(triggerJSON);
    }


    public static Trigger fromJSON(JSONObject triggerJSON) {
        Integer index = getIntegerForKeyOrNull(triggerJSON, "index");
        String flowId = getStringForKeyOrNull(triggerJSON, "flowId");
        String flowName = getStringForKeyOrNull(triggerJSON, "flowName");
        String triggerId = getStringForKeyOrNull(triggerJSON, "id");
        String triggerName = getStringForKeyOrNull(triggerJSON, "name");
        String uri = getStringForKeyOrNull(triggerJSON, "uri");

        return new Trigger(flowId, flowName, triggerId, triggerName, uri, index);
    }

    public static String toJSONArrayString(ArrayList<Trigger> triggers) {
        JSONArray triggersJSON = new JSONArray();

        for(Trigger trigger : triggers) {
            triggersJSON.put(trigger.toJSONObject());
        }

        return triggersJSON.toString();
    }

    public static ArrayList<Trigger> triggersFromJSON(JSONArray triggersJSON) {
        ArrayList<Trigger> triggers = new ArrayList<Trigger>();

        for(int i = 0; i < triggersJSON.length(); i++) {
            try {
                JSONObject triggerJSON = triggersJSON.getJSONObject(i);
                triggers.add(fromJSON(triggerJSON));
            } catch (JSONException jsonException) {
                Log.e(TAG, jsonException.getMessage(), jsonException);
            }
        }

        return triggers;
    }

    public static ArrayList<Trigger> triggersFromJSON(String triggersJSONString) {
        JSONArray triggersJSON = new JSONArray();
        try {
            triggersJSON = new JSONArray(triggersJSONString);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return triggersFromJSON(triggersJSON);
    }

    private static Integer getIntegerForKeyOrNull(JSONObject json, String key) {
        try {
            return json.getInt(key);
        } catch (JSONException e) {
            return null;
        }
    }

    public static String getStringForKeyOrNull(JSONObject json, String key) {
        try {
            return json.getString(key);
        } catch (JSONException e) {
            return null;
        }
    }
}