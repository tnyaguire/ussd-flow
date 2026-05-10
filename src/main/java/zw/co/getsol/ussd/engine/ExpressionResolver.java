package zw.co.getsol.ussd.engine;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExpressionResolver {

    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    public String resolve(String template, Map<String, Object> data) {
        if (template == null || data == null) {
            return template;
        }

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
