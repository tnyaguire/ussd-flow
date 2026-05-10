package zw.co.getsol.ussd.gateway;

public class UssdRequest {

    private String sessionId;
    private String msisdn;
    private String userInput;

    public UssdRequest() {
    }

    public UssdRequest(String sessionId, String msisdn, String userInput) {
        this.sessionId = sessionId;
        this.msisdn = msisdn;
        this.userInput = userInput;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public String getUserInput() {
        return userInput;
    }

    public void setUserInput(String userInput) {
        this.userInput = userInput;
    }

    @Override
    public String toString() {
        return "UssdRequest{" +
                "sessionId='" + sessionId + '\'' +
                ", msisdn='" + msisdn + '\'' +
                ", userInput='" + userInput + '\'' +
                '}';
    }
}
