package zw.co.getsol.ussd.engine.spi;

import java.util.Collections;
import java.util.Map;

public class RouteResult {

    private final String flowId;
    private final String stateId;
    private final Map<String, Object> data;

    public RouteResult(String flowId, String stateId) {
        this(flowId, stateId, Collections.emptyMap());
    }

    public RouteResult(String flowId, String stateId, Map<String, Object> data) {
        this.flowId = flowId;
        this.stateId = stateId;
        this.data = data != null ? data : Collections.emptyMap();
    }

    public String getFlowId() {
        return flowId;
    }

    public String getStateId() {
        return stateId;
    }

    public Map<String, Object> getData() {
        return data;
    }
}
