package com.octoblu.blu;

/**
 * Created by roy on 11/5/14.
 */
public class Trigger {
    private String flowId, flowName, triggerId, triggerName;

    public Trigger(String flowId, String flowName, String triggerId, String triggerName) {
        this.flowId = flowId;
        this.flowName = flowName;
        this.triggerId = triggerId;
        this.triggerName = triggerName;
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
}
