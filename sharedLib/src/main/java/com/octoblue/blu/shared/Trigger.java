package com.octoblue.blu.shared;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class Trigger implements Parcelable {

    private static final String TAG = "Trigger";

    private String flowId;
    private String flowName;
    private String triggerId;
    private String triggerName;

    public Trigger() {}

    public Trigger(String flowId, String flowName, String triggerId, String triggerName) {
        this.flowId = flowId;
        this.flowName = flowName;
        this.triggerId = triggerId;
        this.triggerName = triggerName;
    }

    public Trigger(Parcel in) {
        flowId = in.readString();
        flowName = in.readString();
        triggerId = in.readString();
        triggerName = in.readString();
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(flowId);
        dest.writeString(flowName);
        dest.writeString(triggerId);
        dest.writeString(triggerName);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Trigger> CREATOR = new Parcelable.Creator<Trigger>() {
        @Override
        public Trigger createFromParcel(Parcel in) {
            return new Trigger(in);
        }

        @Override
        public Trigger[] newArray(int size) {
            return new Trigger[size];
        }
    };
}