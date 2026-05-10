package zw.co.getsol.ussd.engine.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FlowDefinition {

    private String id;

    private String name;

    private String ttl;

    private boolean resumable = true;

    @JsonProperty("resume-state")
    private String resumeState;

    @JsonProperty("resume-router")
    private String resumeRouter;

    private LinkedHashMap<String, StateDefinition> states;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTtl() {
        return ttl;
    }

    public void setTtl(String ttl) {
        this.ttl = ttl;
    }

    public boolean isResumable() {
        return resumable;
    }

    public void setResumable(boolean resumable) {
        this.resumable = resumable;
    }

    public String getResumeState() {
        return resumeState;
    }

    public void setResumeState(String resumeState) {
        this.resumeState = resumeState;
    }

    public String getResumeRouter() {
        return resumeRouter;
    }

    public void setResumeRouter(String resumeRouter) {
        this.resumeRouter = resumeRouter;
    }

    public LinkedHashMap<String, StateDefinition> getStates() {
        return states;
    }

    public void setStates(LinkedHashMap<String, StateDefinition> states) {
        this.states = states;
    }
}
