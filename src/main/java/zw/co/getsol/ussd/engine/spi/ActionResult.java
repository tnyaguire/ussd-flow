package zw.co.getsol.ussd.engine.spi;

import java.util.Collections;
import java.util.Map;

public class ActionResult {

    private final boolean success;
    private final Map<String, Object> data;
    private final String errorState;
    private final String errorMessage;

    private ActionResult(boolean success, Map<String, Object> data, String errorState, String errorMessage) {
        this.success = success;
        this.data = data != null ? data : Collections.emptyMap();
        this.errorState = errorState;
        this.errorMessage = errorMessage;
    }

    public boolean isSuccess() {
        return success;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public String getErrorState() {
        return errorState;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public static ActionResult success(Map<String, Object> data) {
        return new ActionResult(true, data, null, null);
    }

    public static ActionResult success() {
        return new ActionResult(true, null, null, null);
    }

    public static ActionResult failure(String errorMessage) {
        return new ActionResult(false, null, null, errorMessage);
    }

    public static ActionResult failure(String errorMessage, String errorState) {
        return new ActionResult(false, null, errorState, errorMessage);
    }
}
