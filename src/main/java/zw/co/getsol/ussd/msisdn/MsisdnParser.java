package zw.co.getsol.ussd.msisdn;

import zw.co.getsol.ussd.config.UssdProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MsisdnParser {

    private static final Logger log = LoggerFactory.getLogger(MsisdnParser.class);

    private final String countryCode;

    public MsisdnParser(UssdProperties.Msisdn msisdnConfig) {
        this.countryCode = msisdnConfig.getCountryCode();
    }

    /**
     * Normalizes any MSISDN format to international format (e.g., 263771234567).
     * Handles: 0771234567, +263771234567, 263771234567, 771234567
     */
    public String normalize(String rawMsisdn) {
        if (rawMsisdn == null || rawMsisdn.isBlank()) {
            throw new IllegalArgumentException("MSISDN cannot be null or empty");
        }

        // Strip non-digits
        String digits = rawMsisdn.replaceAll("[^0-9]", "");

        // Take last 9 digits (subscriber number) and prepend country code
        if (digits.length() >= 9) {
            return countryCode + digits.substring(digits.length() - 9);
        }

        log.warn("Could not normalize MSISDN: {}", maskMsisdn(rawMsisdn));
        return countryCode + digits;
    }

    public static String maskMsisdn(String msisdn) {
        if (msisdn == null || msisdn.length() < 4) {
            return "****";
        }
        return "***" + msisdn.substring(msisdn.length() - 4);
    }
}
