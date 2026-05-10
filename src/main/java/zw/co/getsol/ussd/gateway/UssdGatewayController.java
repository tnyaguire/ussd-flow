package zw.co.getsol.ussd.gateway;

import zw.co.getsol.ussd.engine.FlowEngine;
import zw.co.getsol.ussd.msisdn.MsisdnParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
public class UssdGatewayController {

    private static final Logger log = LoggerFactory.getLogger(UssdGatewayController.class);

    private final FlowEngine flowEngine;
    private final MsisdnParser msisdnParser;

    public UssdGatewayController(FlowEngine flowEngine, MsisdnParser msisdnParser) {
        this.flowEngine = flowEngine;
        this.msisdnParser = msisdnParser;
    }

    /**
     * Simulator endpoint — simple JSON for local testing without a telco connection.
     */
    @PostMapping(value = "${sol.ussd.gateway.simulator-path:/ussd/simulator}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public UssdResponse simulator(@RequestBody UssdRequest request) {
        log.debug("Simulator request: {}", request);
        try {
            String normalizedMsisdn = msisdnParser.normalize(request.getMsisdn());
            request.setMsisdn(normalizedMsisdn);
            UssdResponse response = flowEngine.process(request);
            log.debug("Simulator response: {}", response);
            return response;
        } catch (Exception e) {
            log.error("Error processing simulator request", e);
            return UssdResponse.endSession("An error occurred. Please try again.");
        }
    }

    /**
     * BulkIT USSD Gateway — receives forwarded JSON requests from the bulkit-ussd-gateway.
     *
     * Request: { transactionID, sourceNumber, destinationNumber, message, stage, channel }
     * Response: { transactionID, message, stage, channel }
     *
     * stage: "START" or "session_active" = continue, anything else = end session.
     */
    @PostMapping(value = "${sol.ussd.gateway.bulkit-path:/ussd/bulkit}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> bulkit(@RequestBody Map<String, Object> payload) {
        String transactionId = String.valueOf(payload.getOrDefault("transactionID", ""));
        String sourceNumber = String.valueOf(payload.getOrDefault("sourceNumber", ""));
        String message = String.valueOf(payload.getOrDefault("message", ""));
        String stage = String.valueOf(payload.getOrDefault("stage", "START"));

        log.debug("BulkIT request: sessionId={}, msisdn={}, input={}, stage={}",
                transactionId, MsisdnParser.maskMsisdn(sourceNumber), message, stage);

        try {
            String normalizedMsisdn = msisdnParser.normalize(sourceNumber);

            UssdRequest request = new UssdRequest(transactionId, normalizedMsisdn, message);
            UssdResponse response = flowEngine.process(request);

            Map<String, Object> result = new HashMap<>();
            result.put("transactionID", transactionId);
            result.put("sourceNumber", sourceNumber);
            result.put("destinationNumber", payload.getOrDefault("destinationNumber", ""));
            result.put("message", response.getResponseText());
            result.put("stage", response.isTerminateSession() ? "END" : "session_active");
            result.put("channel", "USSD");

            log.debug("BulkIT response: sessionId={}, terminate={}", transactionId, response.isTerminateSession());
            return result;
        } catch (Exception e) {
            log.error("Error processing BulkIT request for sessionId={}", transactionId, e);

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("transactionID", transactionId);
            errorResult.put("message", "An error occurred. Please try again.");
            errorResult.put("stage", "END");
            errorResult.put("channel", "USSD");
            return errorResult;
        }
    }

    /**
     * Flares gateway — receives USSD requests as HTTP query parameters.
     */
    @GetMapping(value = "${sol.ussd.gateway.flares-path:/ussd/flares}",
            produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> flares(
            @RequestParam("MSISDN") String msisdn,
            @RequestParam("SESSION_ID") String sessionId,
            @RequestParam("INPUT") String input) {

        log.debug("Flares request: msisdn={}, sessionId={}, input={}",
                MsisdnParser.maskMsisdn(msisdn), sessionId, input);
        try {
            String normalizedMsisdn = msisdnParser.normalize(msisdn);

            UssdRequest request = new UssdRequest(sessionId, normalizedMsisdn, input);
            UssdResponse response = flowEngine.process(request);

            return ResponseEntity.ok()
                    .header("Freeflow", response.isTerminateSession() ? "FB" : "FC")
                    .body(response.getResponseText());
        } catch (Exception e) {
            log.error("Error processing flares request", e);
            return ResponseEntity.ok()
                    .header("Freeflow", "FB")
                    .body("An error occurred. Please try again.");
        }
    }

    /**
     * Econet USSD Router — receives XML requests.
     */
    @PostMapping(value = "${sol.ussd.gateway.econet-path:/ussd/econet}",
            consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> econet(@RequestBody String xml) {
        log.debug("Econet request: {}", xml);

        try {
            // Parse XML to extract sessionId, msisdn, userInput
            Map<String, String> parsed = parseEconetXml(xml);
            String normalizedMsisdn = msisdnParser.normalize(parsed.get("msisdn"));

            UssdRequest request = new UssdRequest(parsed.get("sessionId"), normalizedMsisdn, parsed.get("userInput"));
            UssdResponse response = flowEngine.process(request);

            String responseXml = buildEconetResponseXml(parsed.get("sessionId"), response);
            return ResponseEntity.ok(responseXml);
        } catch (Exception e) {
            log.error("Error processing econet request", e);
            String errorXml = buildEconetResponseXml("", UssdResponse.endSession("An error occurred. Please try again."));
            return ResponseEntity.ok(errorXml);
        }
    }

    /**
     * 6D USSD Router — receives XML requests.
     */
    @PostMapping(value = "${sol.ussd.gateway.sixd-path:/ussd/sixd}",
            consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sixd(@RequestBody String xml) {
        log.debug("6D request: {}", xml);

        try {
            Map<String, String> parsed = parseSixdXml(xml);
            String normalizedMsisdn = msisdnParser.normalize(parsed.get("msisdn"));

            UssdRequest request = new UssdRequest(parsed.get("sessionId"), normalizedMsisdn, parsed.get("userInput"));
            UssdResponse response = flowEngine.process(request);

            String responseXml = buildSixdResponseXml(parsed.get("sessionId"), response);
            return ResponseEntity.ok(responseXml);
        } catch (Exception e) {
            log.error("Error processing 6D request", e);
            String errorXml = buildSixdResponseXml("", UssdResponse.endSession("An error occurred. Please try again."));
            return ResponseEntity.ok(errorXml);
        }
    }

    // --- XML Helpers ---

    private Map<String, String> parseEconetXml(String xml) {
        String sessionId = extractXmlTag(xml, "transactionId");
        String msisdn = extractXmlTag(xml, "msisdn");
        String userInput = extractXmlTag(xml, "response");
        Map<String, String> result = new HashMap<>();
        result.put("sessionId", sessionId != null ? sessionId : "");
        result.put("msisdn", msisdn != null ? msisdn : "");
        result.put("userInput", userInput != null ? userInput : "");
        return result;
    }

    private String buildEconetResponseXml(String sessionId, UssdResponse response) {
        String responseType = response.isTerminateSession() ? "2" : "1";
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <messageResponse>
                    <transactionId>%s</transactionId>
                    <responseType>%s</responseType>
                    <message>%s</message>
                </messageResponse>
                """.formatted(escapeXml(sessionId), responseType, escapeXml(response.getResponseText()));
    }

    private Map<String, String> parseSixdXml(String xml) {
        String sessionId = extractXmlTag(xml, "requestId");
        if (sessionId == null) sessionId = extractXmlTag(xml, "sessionId");
        String msisdn = extractXmlTag(xml, "msisdn");
        String userInput = extractXmlTag(xml, "userData");
        Map<String, String> result = new HashMap<>();
        result.put("sessionId", sessionId != null ? sessionId : "");
        result.put("msisdn", msisdn != null ? msisdn : "");
        result.put("userInput", userInput != null ? userInput : "");
        return result;
    }

    private String buildSixdResponseXml(String sessionId, UssdResponse response) {
        String responseCode = response.isTerminateSession() ? "2" : "1";
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <USSDDynMenuResponse>
                    <requestId>%s</requestId>
                    <responseCode>%s</responseCode>
                    <message>%s</message>
                </USSDDynMenuResponse>
                """.formatted(escapeXml(sessionId), responseCode, escapeXml(response.getResponseText()));
    }

    private String extractXmlTag(String xml, String tagName) {
        String closeTag = "</" + tagName + ">";
        int start = xml.indexOf("<" + tagName);
        int end = xml.indexOf(closeTag);
        if (start >= 0 && end > start) {
            // Skip past the opening tag (which may have attributes)
            start = xml.indexOf(">", start);
            if (start >= 0 && start < end) {
                return xml.substring(start + 1, end).trim();
            }
        }
        return null;
    }

    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
