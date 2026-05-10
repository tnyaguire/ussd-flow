package zw.co.getsol.ussd.exception;

public class FlowNotFoundException extends UssdException {

    public FlowNotFoundException(String flowId) {
        super("Flow not found: " + flowId);
    }

    public FlowNotFoundException(String flowId, String stateId) {
        super("State '" + stateId + "' not found in flow '" + flowId + "'");
    }
}
