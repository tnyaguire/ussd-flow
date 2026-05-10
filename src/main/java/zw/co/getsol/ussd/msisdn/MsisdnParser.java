package zw.co.getsol.ussd.msisdn;

import zw.co.getsol.ussd.config.UssdProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MsisdnParser {

    private static final Logger log = LoggerFactory.getLogger(MsisdnParser.class);

    private final String countryCode;
    private final Map<String, UssdProperties.Msisdn.Operator> operators;
    private final Pattern msisdnPattern;

    public MsisdnParser(UssdProperties.Msisdn msisdnConfig) {
        this.countryCode = msisdnConfig.getCountryCode();
        this.operators = msisdnConfig.getOperators();
        // Matches: optional +/00 + country code, then the rest
        this.msisdnPattern = Pattern.compile("(?:(?:00|\\+)?" + countryCode + ")?0?(\\d+)");
    }

    public Msisdn parse(String rawMsisdn) {
        if (rawMsisdn == null || rawMsisdn.isBlank()) {
            throw new IllegalArgumentException("MSISDN cannot be null or empty");
        }

        String cleaned = rawMsisdn.replaceAll("[\\s\\-()]", "");
        Matcher matcher = msisdnPattern.matcher(cleaned);

        if (!matcher.matches()) {
            log.warn("Could not parse MSISDN: {}", maskMsisdn(rawMsisdn));
            return new Msisdn(countryCode, "", cleaned, "Unknown");
        }

        String subscriberNumber = matcher.group(1);

        for (Map.Entry<String, UssdProperties.Msisdn.Operator> entry : operators.entrySet()) {
            UssdProperties.Msisdn.Operator operator = entry.getValue();
            if (operator.getPrefixes() != null) {
                for (String prefix : operator.getPrefixes()) {
                    if (subscriberNumber.startsWith(prefix)) {
                        String operatorCode = prefix;
                        String number = subscriberNumber.substring(prefix.length());
                        return new Msisdn(countryCode, operatorCode, number, operator.getName());
                    }
                }
            }
        }

        // No matching operator — return with unknown operator
        String opCode = subscriberNumber.length() >= 2 ? subscriberNumber.substring(0, 2) : subscriberNumber;
        String number = subscriberNumber.length() >= 2 ? subscriberNumber.substring(2) : "";
        return new Msisdn(countryCode, opCode, number, "Unknown");
    }

    public String normalize(String rawMsisdn) {
        Msisdn msisdn = parse(rawMsisdn);
        return msisdn.toInternational();
    }

    public static String maskMsisdn(String msisdn) {
        if (msisdn == null || msisdn.length() < 4) {
            return "****";
        }
        return "***" + msisdn.substring(msisdn.length() - 4);
    }
}
