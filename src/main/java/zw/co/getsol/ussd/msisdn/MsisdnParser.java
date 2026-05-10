package zw.co.getsol.ussd.msisdn;

import zw.co.getsol.ussd.config.UssdProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class MsisdnParser {

    private static final Logger log = LoggerFactory.getLogger(MsisdnParser.class);

    private final String countryCode;
    private final Map<String, CompiledOperator> operators = new LinkedHashMap<>();

    public MsisdnParser(UssdProperties.Msisdn msisdnConfig) {
        this.countryCode = msisdnConfig.getCountryCode();

        for (Map.Entry<String, UssdProperties.Msisdn.Operator> entry : msisdnConfig.getOperators().entrySet()) {
            UssdProperties.Msisdn.Operator op = entry.getValue();
            if (op.getPattern() != null && !op.getPattern().isBlank()) {
                operators.put(entry.getKey(), new CompiledOperator(
                        Pattern.compile(op.getPattern()),
                        op.getName() != null ? op.getName() : entry.getKey(),
                        op.getProvider()
                ));
            }
        }

        if (!operators.isEmpty()) {
            log.info("Loaded {} operator pattern(s): {}", operators.size(), operators.keySet());
        }
    }

    /**
     * Normalizes any MSISDN format to international format (e.g., 263771234567).
     */
    public String normalize(String rawMsisdn) {
        if (rawMsisdn == null || rawMsisdn.isBlank()) {
            throw new IllegalArgumentException("MSISDN cannot be null or empty");
        }

        String digits = rawMsisdn.replaceAll("[^0-9]", "");

        if (digits.length() >= 9) {
            return countryCode + digits.substring(digits.length() - 9);
        }

        log.warn("Could not normalize MSISDN: {}", maskMsisdn(rawMsisdn));
        return countryCode + digits;
    }

    /**
     * Detects the operator name from an MSISDN using configured regex patterns.
     *
     * @return operator name (e.g., "Econet", "Safaricom") or null if no match.
     */
    public String detectOperator(String msisdn) {
        if (msisdn == null || msisdn.isBlank()) return null;

        for (CompiledOperator op : operators.values()) {
            if (op.pattern.matcher(msisdn).matches()) {
                return op.name;
            }
        }
        return null;
    }

    /**
     * Detects the default mobile money provider from an MSISDN using configured regex patterns.
     *
     * @return provider name (e.g., "EcoCash", "M-Pesa") or null if no match or no provider configured.
     */
    public String detectProvider(String msisdn) {
        if (msisdn == null || msisdn.isBlank()) return null;

        for (CompiledOperator op : operators.values()) {
            if (op.pattern.matcher(msisdn).matches()) {
                return op.provider;
            }
        }
        return null;
    }

    public static String maskMsisdn(String msisdn) {
        if (msisdn == null || msisdn.length() < 4) {
            return "****";
        }
        return "***" + msisdn.substring(msisdn.length() - 4);
    }

    private record CompiledOperator(Pattern pattern, String name, String provider) {}
}
