package zw.co.getsol.ussd.engine.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;

public class StateDefinition {

    private StateType type;

    private ScreenDefinition screen;

    @JsonProperty("data-loader")
    private String dataLoader;

    private String validation;

    @JsonProperty("router-bean")
    private String routerBean;

    private LinkedHashMap<String, TransitionDefinition> transitions;

    @JsonProperty("save-point")
    private boolean savePoint;

    @JsonProperty("clear-saved-state")
    private boolean clearSavedState;

    public StateType getType() {
        return type;
    }

    public void setType(StateType type) {
        this.type = type;
    }

    public ScreenDefinition getScreen() {
        return screen;
    }

    public void setScreen(ScreenDefinition screen) {
        this.screen = screen;
    }

    public String getDataLoader() {
        return dataLoader;
    }

    public void setDataLoader(String dataLoader) {
        this.dataLoader = dataLoader;
    }

    public String getValidation() {
        return validation;
    }

    public void setValidation(String validation) {
        this.validation = validation;
    }

    public String getRouterBean() {
        return routerBean;
    }

    public void setRouterBean(String routerBean) {
        this.routerBean = routerBean;
    }

    public LinkedHashMap<String, TransitionDefinition> getTransitions() {
        return transitions;
    }

    public void setTransitions(LinkedHashMap<String, TransitionDefinition> transitions) {
        this.transitions = transitions;
    }

    public boolean isSavePoint() {
        return savePoint;
    }

    public void setSavePoint(boolean savePoint) {
        this.savePoint = savePoint;
    }

    public boolean isClearSavedState() {
        return clearSavedState;
    }

    public void setClearSavedState(boolean clearSavedState) {
        this.clearSavedState = clearSavedState;
    }
}
