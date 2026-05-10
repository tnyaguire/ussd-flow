package zw.co.getsol.ussd.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import zw.co.getsol.ussd.config.UssdProperties;
import zw.co.getsol.ussd.engine.model.FlowDefinition;
import zw.co.getsol.ussd.engine.model.FlowFile;
import zw.co.getsol.ussd.engine.model.StateDefinition;
import zw.co.getsol.ussd.engine.model.TransitionDefinition;
import zw.co.getsol.ussd.exception.FlowNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FlowRegistry {

    private static final Logger log = LoggerFactory.getLogger(FlowRegistry.class);

    private final Map<String, FlowDefinition> flows = new ConcurrentHashMap<>();
    private final ObjectMapper yamlMapper;

    public FlowRegistry(UssdProperties properties) {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.yamlMapper.findAndRegisterModules();
        loadFlows(properties.getFlowPaths());
    }

    private void loadFlows(String locationPattern) {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources(locationPattern);
            if (resources.length == 0) {
                log.warn("No flow files found at: {}", locationPattern);
                return;
            }

            for (Resource resource : resources) {
                loadFlow(resource);
            }

            log.info("Loaded {} flow(s): {}", flows.size(), flows.keySet());
            validateFlows();

        } catch (IOException e) {
            throw new IllegalStateException("Failed to load flow files from: " + locationPattern, e);
        }
    }

    private void loadFlow(Resource resource) throws IOException {
        try (InputStream is = resource.getInputStream()) {
            FlowFile flowFile = yamlMapper.readValue(is, FlowFile.class);
            FlowDefinition flow = flowFile.getFlow();

            if (flow == null || flow.getId() == null) {
                throw new IllegalStateException("Flow file " + resource.getFilename() + " is missing 'flow.id'");
            }

            if (flow.getStates() == null || flow.getStates().isEmpty()) {
                throw new IllegalStateException("Flow '" + flow.getId() + "' has no states defined");
            }

            if (flows.containsKey(flow.getId())) {
                throw new IllegalStateException("Duplicate flow ID: " + flow.getId());
            }

            flows.put(flow.getId(), flow);
            log.debug("Loaded flow '{}' with {} state(s) from {}", flow.getId(), flow.getStates().size(), resource.getFilename());
        }
    }

    private void validateFlows() {
        for (FlowDefinition flow : flows.values()) {
            for (Map.Entry<String, StateDefinition> stateEntry : flow.getStates().entrySet()) {
                String stateId = stateEntry.getKey();
                StateDefinition state = stateEntry.getValue();

                if (state.getType() == null) {
                    throw new IllegalStateException("State '" + stateId + "' in flow '" + flow.getId() + "' has no type");
                }

                if (state.getTransitions() != null) {
                    for (Map.Entry<String, TransitionDefinition> transEntry : state.getTransitions().entrySet()) {
                        String gotoTarget = transEntry.getValue().getGotoState();
                        if (gotoTarget != null && !transEntry.getValue().isTerminate()) {
                            validateGotoTarget(flow, stateId, gotoTarget);
                        }
                    }
                }
            }

            if (flow.getTtl() != null) {
                parseTtl(flow.getTtl());
            }
        }
        log.info("All flows validated successfully");
    }

    private void validateGotoTarget(FlowDefinition currentFlow, String fromState, String gotoTarget) {
        if (gotoTarget.startsWith("flow:")) {
            String ref = gotoTarget.substring(5);
            String[] parts = ref.split("\\.", 2);
            String targetFlowId = parts[0];
            if (!flows.containsKey(targetFlowId)) {
                throw new IllegalStateException(
                        "State '" + fromState + "' in flow '" + currentFlow.getId()
                                + "' references unknown flow: " + targetFlowId);
            }
            if (parts.length == 2) {
                String targetStateId = parts[1];
                FlowDefinition targetFlow = flows.get(targetFlowId);
                if (!targetFlow.getStates().containsKey(targetStateId)) {
                    throw new IllegalStateException(
                            "State '" + fromState + "' in flow '" + currentFlow.getId()
                                    + "' references unknown state '" + targetStateId + "' in flow '" + targetFlowId + "'");
                }
            }
        } else {
            if (!currentFlow.getStates().containsKey(gotoTarget)) {
                throw new IllegalStateException(
                        "State '" + fromState + "' in flow '" + currentFlow.getId()
                                + "' references unknown state: " + gotoTarget);
            }
        }
    }

    public FlowDefinition getFlow(String flowId) {
        FlowDefinition flow = flows.get(flowId);
        if (flow == null) {
            throw new FlowNotFoundException(flowId);
        }
        return flow;
    }

    public StateDefinition getState(String flowId, String stateId) {
        FlowDefinition flow = getFlow(flowId);
        StateDefinition state = flow.getStates().get(stateId);
        if (state == null) {
            throw new FlowNotFoundException(flowId, stateId);
        }
        return state;
    }

    public String getFirstStateId(FlowDefinition flow) {
        if (flow.getStates() == null || flow.getStates().isEmpty()) {
            throw new FlowNotFoundException("Flow '" + flow.getId() + "' has no states defined");
        }
        return flow.getStates().keySet().iterator().next();
    }

    public Duration getFlowTtl(String flowId) {
        FlowDefinition flow = getFlow(flowId);
        return flow.getTtl() != null ? parseTtl(flow.getTtl()) : Duration.ofMinutes(5);
    }

    public static Duration parseTtl(String ttl) {
        if (ttl == null || ttl.isBlank()) {
            return Duration.ofMinutes(5);
        }
        ttl = ttl.trim().toLowerCase();
        if (ttl.endsWith("s")) {
            return Duration.ofSeconds(Long.parseLong(ttl.substring(0, ttl.length() - 1)));
        } else if (ttl.endsWith("m")) {
            return Duration.ofMinutes(Long.parseLong(ttl.substring(0, ttl.length() - 1)));
        } else if (ttl.endsWith("h")) {
            return Duration.ofHours(Long.parseLong(ttl.substring(0, ttl.length() - 1)));
        } else {
            return Duration.ofMinutes(Long.parseLong(ttl));
        }
    }

    public boolean hasFlow(String flowId) {
        return flows.containsKey(flowId);
    }

    public Map<String, FlowDefinition> getAllFlows() {
        return Map.copyOf(flows);
    }
}
