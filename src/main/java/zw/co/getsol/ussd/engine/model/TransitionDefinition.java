package zw.co.getsol.ussd.engine.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class TransitionDefinition {

    @JsonProperty("goto")
    private String gotoState;

    private String action;

    private Map<String, String> save;

    private boolean terminate;

    @JsonProperty("clear-state")
    private boolean clearState;

    private boolean error;

    public String getGotoState() {
        return gotoState;
    }

    public void setGotoState(String gotoState) {
        this.gotoState = gotoState;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Map<String, String> getSave() {
        return save;
    }

    public void setSave(Map<String, String> save) {
        this.save = save;
    }

    public boolean isTerminate() {
        return terminate;
    }

    public void setTerminate(boolean terminate) {
        this.terminate = terminate;
    }

    public boolean isClearState() {
        return clearState;
    }

    public void setClearState(boolean clearState) {
        this.clearState = clearState;
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }
}
