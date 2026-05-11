package zw.co.getsol.ussd.engine;

import zw.co.getsol.ussd.engine.model.ScreenDefinition;
import zw.co.getsol.ussd.engine.spi.DataResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ScreenRenderer {

    private static final Logger log = LoggerFactory.getLogger(ScreenRenderer.class);
    private static final String MORE_LABEL = "More";

    private final int charLimit;
    private final ExpressionResolver expressionResolver;

    public ScreenRenderer(int charLimit) {
        this.charLimit = charLimit;
        this.expressionResolver = new ExpressionResolver();
    }

    public RenderResult render(ScreenDefinition screen, Map<String, Object> contextData,
                                DataResult dynamicData, int page) {
        String header = "";
        if (screen.getHeader() != null) {
            header = expressionResolver.resolve(screen.getHeader(), contextData);
        }

        String footer = "";
        if (screen.getFooter() != null) {
            footer = expressionResolver.resolve(screen.getFooter(), contextData);
        }

        // Build all options (static + dynamic + extra)
        List<String> allOptions = new ArrayList<>();

        if (screen.getOptions() != null) {
            for (Map.Entry<String, String> entry : screen.getOptions().entrySet()) {
                String text = expressionResolver.resolve(entry.getValue(), contextData);
                allOptions.add(entry.getKey() + ". " + text);
            }
        }

        if (dynamicData != null && "data".equals(screen.getOptionsFrom())) {
            for (int i = 0; i < dynamicData.size(); i++) {
                Map<String, String> item = dynamicData.get(i);
                String text = resolveOptionFormat(screen.getOptionFormat(), item);
                allOptions.add((i + 1) + ". " + text);
            }
        }

        // Extra options (e.g., "9. More" from YAML) are added after pagination
        List<String> extraOptions = new ArrayList<>();
        if (screen.getExtraOptions() != null) {
            for (Map.Entry<String, ScreenDefinition.ExtraOption> entry : screen.getExtraOptions().entrySet()) {
                String label = expressionResolver.resolve(entry.getValue().getLabel(), contextData);
                extraOptions.add(entry.getKey() + ". " + label);
            }
        }

        // Try rendering without pagination first
        String fullScreen = buildScreen(header, allOptions, extraOptions, footer);
        if (fullScreen.length() <= charLimit) {
            return new RenderResult(fullScreen, false, 0, 1);
        }

        // Need pagination — calculate how many options fit per page
        return renderPaginated(header, footer, allOptions, extraOptions, page);
    }

    // Backward-compatible render without pagination
    public String render(ScreenDefinition screen, Map<String, Object> contextData, DataResult dynamicData) {
        return render(screen, contextData, dynamicData, 0).text();
    }

    private RenderResult renderPaginated(String header, String footer,
                                          List<String> allOptions, List<String> extraOptions, int page) {
        // Reserve space for header, footer, and "N. More" line
        String moreLineTemplate = "99. " + MORE_LABEL; // worst case numbering
        int reservedChars = header.length() + 2; // header + newlines
        if (!footer.isEmpty()) {
            reservedChars += footer.length() + 1;
        }
        reservedChars += moreLineTemplate.length() + 1; // "N. More" line

        int availableChars = charLimit - reservedChars;

        // Calculate pages
        List<List<String>> pages = new ArrayList<>();
        List<String> currentPage = new ArrayList<>();
        int currentPageChars = 0;

        for (String option : allOptions) {
            int optionChars = option.length() + 1; // +1 for newline
            if (currentPageChars + optionChars > availableChars && !currentPage.isEmpty()) {
                pages.add(currentPage);
                currentPage = new ArrayList<>();
                currentPageChars = 0;
            }
            currentPage.add(option);
            currentPageChars += optionChars;
        }
        if (!currentPage.isEmpty()) {
            pages.add(currentPage);
        }

        int totalPages = pages.size();
        if (page >= totalPages) {
            page = totalPages - 1;
        }
        if (page < 0) {
            page = 0;
        }

        boolean isLastPage = (page >= totalPages - 1);
        List<String> pageOptions = pages.get(page);

        // Build the page
        StringBuilder sb = new StringBuilder();
        sb.append(header);

        for (String option : pageOptions) {
            sb.append("\n").append(option);
        }

        // Add "More" if not last page (use next option number)
        if (!isLastPage) {
            // Find the highest option number on this page to determine "More" number
            String lastOption = pageOptions.get(pageOptions.size() - 1);
            int lastNum = extractOptionNumber(lastOption);
            sb.append("\n").append(lastNum + 1).append(". ").append(MORE_LABEL);
        }

        // Add extra options on last page only
        if (isLastPage) {
            for (String extra : extraOptions) {
                sb.append("\n").append(extra);
            }
        }

        if (!footer.isEmpty()) {
            sb.append("\n").append(footer);
        }

        String result = sb.toString().trim();
        log.debug("Paginated: page {}/{}, {} chars", page + 1, totalPages, result.length());

        return new RenderResult(result, true, page, totalPages);
    }

    private String buildScreen(String header, List<String> options,
                                List<String> extraOptions, String footer) {
        StringBuilder sb = new StringBuilder();
        sb.append(header);
        for (String option : options) {
            sb.append("\n").append(option);
        }
        for (String extra : extraOptions) {
            sb.append("\n").append(extra);
        }
        if (!footer.isEmpty()) {
            sb.append("\n").append(footer);
        }
        return sb.toString().trim();
    }

    private int extractOptionNumber(String option) {
        int dotIndex = option.indexOf('.');
        if (dotIndex > 0) {
            try {
                return Integer.parseInt(option.substring(0, dotIndex).trim());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
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

    public record RenderResult(String text, boolean paginated, int page, int totalPages) {
        public int moreOptionNumber() {
            // The "More" option number is the last displayed option + 1
            // Caller should check paginated && page < totalPages - 1
            return 0; // Calculated from the rendered text
        }
    }
}
