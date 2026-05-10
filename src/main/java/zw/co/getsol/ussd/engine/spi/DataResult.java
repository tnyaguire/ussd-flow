package zw.co.getsol.ussd.engine.spi;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DataResult {

    private final List<Map<String, String>> items;

    public DataResult(List<Map<String, String>> items) {
        this.items = items != null ? items : List.of();
    }

    public List<Map<String, String>> getItems() {
        return items;
    }

    public int size() {
        return items.size();
    }

    public Map<String, String> get(int index) {
        if (index < 0 || index >= items.size()) {
            return null;
        }
        return items.get(index);
    }

    public static <T> DataResult from(List<T> source, Function<T, Map<String, String>> mapper) {
        List<Map<String, String>> items = source.stream()
                .map(mapper)
                .collect(Collectors.toList());
        return new DataResult(items);
    }
}
