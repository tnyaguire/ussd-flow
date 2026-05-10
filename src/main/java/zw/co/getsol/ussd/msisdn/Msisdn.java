package zw.co.getsol.ussd.msisdn;

public class Msisdn {

    private String countryCode;
    private String operatorCode;
    private String number;
    private String operatorName;

    public Msisdn() {
    }

    public Msisdn(String countryCode, String operatorCode, String number, String operatorName) {
        this.countryCode = countryCode;
        this.operatorCode = operatorCode;
        this.number = number;
        this.operatorName = operatorName;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getOperatorCode() {
        return operatorCode;
    }

    public void setOperatorCode(String operatorCode) {
        this.operatorCode = operatorCode;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public String toInternational() {
        return countryCode + operatorCode + number;
    }

    public String toLocal() {
        return "0" + operatorCode + number;
    }

    public String getRawNumber() {
        return operatorCode + number;
    }
}
