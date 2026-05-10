package zw.co.getsol.ussd.exception;

public class UssdException extends RuntimeException {

    private final String userMessage;

    public UssdException(String message) {
        super(message);
        this.userMessage = null;
    }

    public UssdException(String message, String userMessage) {
        super(message);
        this.userMessage = userMessage;
    }

    public UssdException(String message, Throwable cause) {
        super(message, cause);
        this.userMessage = null;
    }

    public String getUserMessage() {
        return userMessage != null ? userMessage : "Something went wrong. Please try again.";
    }
}
