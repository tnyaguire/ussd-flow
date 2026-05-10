package zw.co.getsol.ussd.engine;

import zw.co.getsol.ussd.engine.model.ScreenDefinition;
import zw.co.getsol.ussd.engine.spi.DataResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ScreenRenderer {

    private static final Logger log = LoggerFactory.getLogger(ScreenRenderer.class);

    private final int charLimit;
    private final ExpressionResolver expressionResolver;

    public ScreenRenderer(int charLimit) {
        this.charLimit = charLimit;
        this.expressionResolver = new ExpressionResolver();
    }

    public String render(ScreenDefinition screen, Map<String, Object> contextData, DataResult dynamicData) {
        StringBuilder sb = new StringBuilder();

        // Header
        if (screen.getHeader() != null) {
            String header = expressionResolver.resolve(screen.getHeader(), contextData);
            sb.append(header);
        }

        // Static options
        if (screen.getOptions() != null && !screen.getOptions().isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            for (Map.Entry<String, String> entry : screen.getOptions().entrySet()) {
                String optionText = expressionResolver.resolve(entry.getValue(), contextData);
                sb.append("\n").append(entry.getKey()).append(". ").append(optionText);
            }
        }

        // Dynamic options (from data loader)
        if (dynamicData != null && "data".equals(screen.getOptionsFrom())) {
            if (sb.length() > 0) sb.append("\n");
            String format = screen.getOptionFormat();
            for (int i = 0; i < dynamicData.size(); i++) {
                Map<String, String> item = dynamicData.get(i);
                String optionText = resolveOptionFormat(format, item);
                sb.append("\n").append(i + 1).append(". ").append(optionText);
            }
        }

        // Extra options (e.g., "9. More")
        if (screen.getExtraOptions() != null) {
            for (Map.Entry<String, ScreenDefinition.ExtraOption> entry : screen.getExtraOptions().entrySet()) {
                String label = expressionResolver.resolve(entry.getValue().getLabel(), contextData);
                sb.append("\n").append(entry.getKey()).append(". ").append(label);
            }
        }

        // Footer
        if (screen.getFooter() != null) {
            String footer = expressionResolver.resolve(screen.getFooter(), contextData);
            sb.append("\n").append(footer);
        }

        String result = sb.toString().trim();

        if (result.length() > charLimit) {
            log.warn("Screen exceeds {} char limit ({} chars). Truncating.", charLimit, result.length());
            result = result.substring(0, charLimit);
        }

        return result;
    }

    private String resolveOptionFormat(String format, Map<String, String> item) {
        if (format == null || item == null) {
            return item != null ? item.getOrDefault("name", "") : "";
        }

        String result = format;
        for (Map.Entry<String, String> entry : item.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "");
        }
        return result;
    }
}
