package zw.co.getsol.ussd.engine;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExpressionResolver {

    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    /**
     * Guard against a placeholder whose value is itself a placeholder pointing back:
     * {@code a -> "${b}"}, {@code b -> "${a}"} would otherwise loop forever. Six passes
     * is enough for every real chain the flow YAMLs express (catalog value referencing
     * an action-result field, at worst one hop through another catalog key).
     */
    private static final int MAX_PASSES = 6;

    public String resolve(String template, Map<String, Object> data) {
        if (template == null || data == null) {
            return template;
        }

        // Iterate so a substituted value that itself contains ${...} - the shape every
        // catalog string uses when it references action-result fields like ${amount}
        // and ${reference} - keeps getting resolved. Bail early when a pass makes no
        // change (nothing left to substitute, or every remaining expression is unknown
        // and would just come out as "").
        String current = template;
        for (int i = 0; i < MAX_PASSES; i++) {
            String next = resolveOnce(current, data);
            if (next.equals(current)) {
                return next;
            }
            current = next;
        }
        return current;
    }

    private String resolveOnce(String template, Map<String, Object> data) {
        Matcher matcher = EXPRESSION_PATTERN.matcher(template);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String expression = matcher.group(1);
            String value = resolveExpression(expression, data);
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    @SuppressWarnings("unchecked")
    private String resolveExpression(String expression, Map<String, Object> data) {
        String[] parts = expression.split("\\.", 2);
        Object value = data.get(parts[0]);

        if (value == null) {
            return "";
        }

        if (parts.length == 2 && value instanceof Map) {
            Map<String, Object> nested = (Map<String, Object>) value;
            return resolveExpression(parts[1], nested);
        }

        return value.toString();
    }
}
