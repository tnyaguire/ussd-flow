package zw.co.getsol.ussd.exception;

public class InvalidInputException extends UssdException {

    public InvalidInputException(String message) {
        super(message, "Please reply with a number from the list above.");
    }

    public InvalidInputException(String message, String userMessage) {
        super(message, userMessage);
    }
}
