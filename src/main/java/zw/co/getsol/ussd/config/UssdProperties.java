package zw.co.getsol.ussd.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "sol.ussd")
public class UssdProperties {

    private int screenCharLimit = 159;

    private String flowPaths = "classpath:flows/*.yml";

    private String redisPrefix = "ussd";

    private String entryRouter = "entryRouter";

    private Session session = new Session();

    private Gateway gateway = new Gateway();

    private Msisdn msisdn = new Msisdn();

    public int getScreenCharLimit() {
        return screenCharLimit;
    }

    public void setScreenCharLimit(int screenCharLimit) {
        this.screenCharLimit = screenCharLimit;
    }

    public String getFlowPaths() {
        return flowPaths;
    }

    public void setFlowPaths(String flowPaths) {
        this.flowPaths = flowPaths;
    }

    public String getRedisPrefix() {
        return redisPrefix;
    }

    public void setRedisPrefix(String redisPrefix) {
        this.redisPrefix = redisPrefix;
    }

    public String getEntryRouter() {
        return entryRouter;
    }

    public void setEntryRouter(String entryRouter) {
        this.entryRouter = entryRouter;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public Gateway getGateway() {
        return gateway;
    }

    public void setGateway(Gateway gateway) {
        this.gateway = gateway;
    }

    public Msisdn getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(Msisdn msisdn) {
        this.msisdn = msisdn;
    }

    public static class Session {
        private Duration defaultTtl = Duration.ofMinutes(5);

        public Duration getDefaultTtl() {
            return defaultTtl;
        }

        public void setDefaultTtl(Duration defaultTtl) {
            this.defaultTtl = defaultTtl;
        }
    }

    public static class Gateway {
        private String econetPath = "/ussd/econet";
        private String sixdPath = "/ussd/sixd";
        private String flaresPath = "/ussd/flares";
        private String whatsappPath = "/ussd/whatsapp";
        private String simulatorPath = "/ussd/simulator";

        public String getEconetPath() {
            return econetPath;
        }

        public void setEconetPath(String econetPath) {
            this.econetPath = econetPath;
        }

        public String getSixdPath() {
            return sixdPath;
        }

        public void setSixdPath(String sixdPath) {
            this.sixdPath = sixdPath;
        }

        public String getFlaresPath() {
            return flaresPath;
        }

        public void setFlaresPath(String flaresPath) {
            this.flaresPath = flaresPath;
        }

        public String getWhatsappPath() {
            return whatsappPath;
        }

        public void setWhatsappPath(String whatsappPath) {
            this.whatsappPath = whatsappPath;
        }

        public String getSimulatorPath() {
            return simulatorPath;
        }

        public void setSimulatorPath(String simulatorPath) {
            this.simulatorPath = simulatorPath;
        }
    }

    public static class Msisdn {
        private String countryCode = "263";
        private Map<String, Operator> operators = new LinkedHashMap<>();

        public String getCountryCode() {
            return countryCode;
        }

        public void setCountryCode(String countryCode) {
            this.countryCode = countryCode;
        }

        public Map<String, Operator> getOperators() {
            return operators;
        }

        public void setOperators(Map<String, Operator> operators) {
            this.operators = operators;
        }

        public static class Operator {
            private List<String> prefixes;
            private String name;

            public List<String> getPrefixes() {
                return prefixes;
            }

            public void setPrefixes(List<String> prefixes) {
                this.prefixes = prefixes;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }
        }
    }
}
