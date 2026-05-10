package zw.co.getsol.ussd.gateway;

public class UssdResponse {

    private String responseText;
    private boolean terminateSession;

    public UssdResponse() {
    }

    public UssdResponse(String responseText, boolean terminateSession) {
        this.responseText = responseText;
        this.terminateSession = terminateSession;
    }

    public String getResponseText() {
        return responseText;
    }

    public void setResponseText(String responseText) {
        this.responseText = responseText;
    }

    public boolean isTerminateSession() {
        return terminateSession;
    }

    public void setTerminateSession(boolean terminateSession) {
        this.terminateSession = terminateSession;
    }

    public static UssdResponse continueSession(String text) {
        return new UssdResponse(text, false);
    }

    public static UssdResponse endSession(String text) {
        return new UssdResponse(text, true);
    }

    @Override
    public String toString() {
        return "UssdResponse{" +
                "responseText='" + responseText + '\'' +
                ", terminateSession=" + terminateSession +
                '}';
    }
}
