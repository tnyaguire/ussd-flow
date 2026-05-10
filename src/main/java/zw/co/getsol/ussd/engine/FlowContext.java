package zw.co.getsol.ussd.engine;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FlowContext {

    private String msisdn;
    private String sessionId;
    private String flowId;
    private String stateId;
    private Map<String, Object> data = new HashMap<>();
    private Instant createdAt;
    private Instant updatedAt;
    private transient boolean cleared;

    public FlowContext() {
    }

    public FlowContext(String msisdn) {
        this.msisdn = msisdn;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    public String getStateId() {
        return stateId;
    }

    public void setStateId(String stateId) {
        this.stateId = stateId;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isCleared() {
        return cleared;
    }

    public void setCleared(boolean cleared) {
        this.cleared = cleared;
    }

    public void put(String key, Object value) {
        data.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) data.get(key);
    }

    public <T> T get(String key, Class<T> type) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        throw new ClassCastException("Value for key '" + key + "' is " + value.getClass().getName()
                + ", expected " + type.getName());
    }

    public String getString(String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }

    public void putAll(Map<String, Object> values) {
        if (values != null) {
            data.putAll(values);
        }
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }

    public void navigateTo(String flowId, String stateId) {
        this.flowId = flowId;
        this.stateId = stateId;
        touch();
    }
}
