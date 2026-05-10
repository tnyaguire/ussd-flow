package zw.co.getsol.ussd.engine.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;

public class ScreenDefinition {

    private String header;

    private LinkedHashMap<String, String> options;

    private String footer;

    @JsonProperty("options-from")
    private String optionsFrom;

    @JsonProperty("option-format")
    private String optionFormat;

    @JsonProperty("extra-options")
    private LinkedHashMap<String, ExtraOption> extraOptions;

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public LinkedHashMap<String, String> getOptions() {
        return options;
    }

    public void setOptions(LinkedHashMap<String, String> options) {
        this.options = options;
    }

    public String getFooter() {
        return footer;
    }

    public void setFooter(String footer) {
        this.footer = footer;
    }

    public String getOptionsFrom() {
        return optionsFrom;
    }

    public void setOptionsFrom(String optionsFrom) {
        this.optionsFrom = optionsFrom;
    }

    public String getOptionFormat() {
        return optionFormat;
    }

    public void setOptionFormat(String optionFormat) {
        this.optionFormat = optionFormat;
    }

    public LinkedHashMap<String, ExtraOption> getExtraOptions() {
        return extraOptions;
    }

    public void setExtraOptions(LinkedHashMap<String, ExtraOption> extraOptions) {
        this.extraOptions = extraOptions;
    }

    public static class ExtraOption {
        private String label;
        private String goto_;

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        @JsonProperty("goto")
        public String getGoto() {
            return goto_;
        }

        @JsonProperty("goto")
        public void setGoto(String goto_) {
            this.goto_ = goto_;
        }
    }
}
