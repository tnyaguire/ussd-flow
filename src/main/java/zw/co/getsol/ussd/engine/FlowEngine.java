package zw.co.getsol.ussd.engine;

import zw.co.getsol.ussd.engine.model.*;
import zw.co.getsol.ussd.engine.spi.*;
import zw.co.getsol.ussd.exception.InvalidInputException;
import zw.co.getsol.ussd.exception.UssdException;
import zw.co.getsol.ussd.gateway.UssdRequest;
import zw.co.getsol.ussd.gateway.UssdResponse;
import zw.co.getsol.ussd.msisdn.MsisdnParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class FlowEngine {

    private static final Logger log = LoggerFactory.getLogger(FlowEngine.class);
    private static final int MAX_ROUTER_DEPTH = 10;
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);

    private final FlowRegistry flowRegistry;
    private final FlowContextRepository contextRepository;
    private final ScreenRenderer screenRenderer;
    private final ApplicationContext applicationContext;
    private final String entryRouterBean;
    private final ExpressionResolver expressionResolver;
    private final StringRedisTemplate redisTemplate;
    private final String lockPrefix;

    public FlowEngine(FlowRegistry flowRegistry, FlowContextRepository contextRepository,
                       ScreenRenderer screenRenderer, ApplicationContext applicationContext,
                       String entryRouterBean, StringRedisTemplate redisTemplate, String redisPrefix) {
        this.flowRegistry = flowRegistry;
        this.contextRepository = contextRepository;
        this.screenRenderer = screenRenderer;
        this.applicationContext = applicationContext;
        this.entryRouterBean = entryRouterBean;
        this.expressionResolver = new ExpressionResolver();
        this.redisTemplate = redisTemplate;
        this.lockPrefix = redisPrefix + ":lock:";
    }

    public UssdResponse process(UssdRequest request) {
        String msisdn = request.getMsisdn();
        String sessionId = request.getSessionId();
        String userInput = request.getUserInput();

        String lockKey = lockPrefix + msisdn;
        boolean lockAcquired = false;
        try {
            lockAcquired = Boolean.TRUE.equals(
                    redisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_TTL));
            if (!lockAcquired) {
                log.warn("Could not acquire lock for MSISDN {}", MsisdnParser.maskMsisdn(msisdn));
                return UssdResponse.endSession("Please wait and try again.");
            }

            FlowContext context = contextRepository.findByMsisdn(msisdn).orElse(null);

            if (context == null || context.getFlowId() == null) {
                // New user or expired state — route via entry router
                return handleNewSession(msisdn, sessionId);
            }

            if (!Objects.equals(sessionId, context.getSessionId())) {
                // Re-dial — new telco session, but saved state exists (resume scenario)
                return handleResume(context, sessionId);
            }

            // Continuation — same telco session, process user input
            return handleInput(context, userInput);

        } catch (UssdException e) {
            log.error("USSD error for MSISDN {}: {}", MsisdnParser.maskMsisdn(msisdn), e.getMessage());
            return UssdResponse.endSession(e.getUserMessage());
        } catch (Exception e) {
            log.error("Unexpected error for MSISDN {}", MsisdnParser.maskMsisdn(msisdn), e);
            return UssdResponse.endSession("Something went wrong. Please try again.");
        } finally {
            if (lockAcquired) {
                redisTemplate.delete(lockKey);
            }
        }
    }

    private UssdResponse handleNewSession(String msisdn, String sessionId) {
        FlowContext context = new FlowContext(msisdn);
        context.setSessionId(sessionId);

        FlowRouter router = applicationContext.getBean(entryRouterBean, FlowRouter.class);
        RouteResult route = router.route(context);

        context.navigateTo(route.getFlowId(), route.getStateId());
        context.putAll(route.getData());

        return renderState(context, 0);
    }

    private UssdResponse handleResume(FlowContext context, String sessionId) {
        context.setSessionId(sessionId);

        FlowRouter router = applicationContext.getBean(entryRouterBean, FlowRouter.class);
        RouteResult route = router.route(context);

        context.navigateTo(route.getFlowId(), route.getStateId());
        context.putAll(route.getData());

        return renderState(context, 0);
    }

    private UssdResponse handleInput(FlowContext context, String userInput) {
        StateDefinition currentState = flowRegistry.getState(context.getFlowId(), context.getStateId());

        // Handle INPUT state with validation
        if (currentState.getType() == StateType.INPUT && currentState.getValidation() != null) {
            return handleValidatedInput(context, currentState, userInput);
        }

        // Resolve transition for user input
        TransitionDefinition transition = resolveTransition(currentState, userInput);
        if (transition == null) {
            throw new InvalidInputException("Invalid input: " + userInput);
        }

        return executeTransition(context, transition, userInput);
    }

    private UssdResponse handleValidatedInput(FlowContext context, StateDefinition state, String userInput) {
        InputValidator validator = applicationContext.getBean(state.getValidation(), InputValidator.class);
        ValidationResult result = validator.validate(userInput, context);

        if (result.isValid()) {
            TransitionDefinition transition = state.getTransitions().get("_valid");
            if (transition != null) {
                return executeTransition(context, transition, userInput);
            }
        } else {
            TransitionDefinition errorTransition = state.getTransitions().get("_invalid");
            if (errorTransition != null) {
                context.put("_error", result.getErrorMessage());
                return executeTransition(context, errorTransition, userInput);
            }
        }

        throw new InvalidInputException("Validation failed", result.getErrorMessage());
    }

    private UssdResponse executeTransition(FlowContext context, TransitionDefinition transition, String userInput) {
        // Save values from transition
        if (transition.getSave() != null) {
            for (Map.Entry<String, String> entry : transition.getSave().entrySet()) {
                Object value = resolveTransitionValue(entry.getValue(), userInput, context);
                context.put(entry.getKey(), value);
            }
        }

        // Execute action if specified
        if (transition.getAction() != null) {
            FlowAction action = applicationContext.getBean(transition.getAction(), FlowAction.class);
            ActionResult result = action.execute(context);

            if (!result.isSuccess()) {
                if (result.getErrorState() != null) {
                    navigateToTarget(context, result.getErrorState());
                    return renderState(context, 0);
                }
                return UssdResponse.endSession(
                        result.getErrorMessage() != null ? result.getErrorMessage() : "Action failed. Please try again.");
            }

            context.putAll(result.getData());
        }

        // Clear state if specified
        if (transition.isClearState()) {
            contextRepository.delete(context.getMsisdn());
            context.setCleared(true);
        }

        // Terminate if specified
        if (transition.isTerminate()) {
            contextRepository.delete(context.getMsisdn());
            context.setCleared(true);
            if (transition.getGotoState() != null) {
                navigateToTarget(context, transition.getGotoState());
                StateDefinition targetState = flowRegistry.getState(context.getFlowId(), context.getStateId());
                String text = screenRenderer.render(targetState.getScreen(), context.getData(), null);
                return UssdResponse.endSession(text);
            }
            return UssdResponse.endSession("Thank you for using BWinners!");
        }

        // Navigate to next state
        if (transition.getGotoState() != null) {
            navigateToTarget(context, transition.getGotoState());
        }

        return renderState(context, 0);
    }

    private UssdResponse renderState(FlowContext context, int depth) {
        if (depth >= MAX_ROUTER_DEPTH) {
            throw new UssdException("Maximum router depth exceeded (" + MAX_ROUTER_DEPTH + ")");
        }

        StateDefinition state = flowRegistry.getState(context.getFlowId(), context.getStateId());

        // ROUTER state — no screen, evaluate and recurse
        if (state.getType() == StateType.ROUTER) {
            FlowRouter router = applicationContext.getBean(state.getRouterBean(), FlowRouter.class);
            RouteResult route = router.route(context);
            context.navigateTo(route.getFlowId(), route.getStateId());
            context.putAll(route.getData());
            return renderState(context, depth + 1);
        }

        // Load dynamic data if needed
        DataResult dynamicData = null;
        if (state.getDataLoader() != null) {
            DataLoader loader = applicationContext.getBean(state.getDataLoader(), DataLoader.class);
            dynamicData = loader.load(context);
            context.put("_dynamicData", dynamicData.getItems());
        }

        // Render screen
        String text = screenRenderer.render(state.getScreen(), context.getData(), dynamicData);

        // Save state at save-point (skip if context was cleared)
        if (!context.isCleared()) {
            if (state.isSavePoint()) {
                contextRepository.save(context, flowRegistry.getFlowTtl(context.getFlowId()));
            } else {
                // Still save session tracking (short TTL) even without save-point
                contextRepository.save(context);
            }
        }

        boolean terminate = state.getType() == StateType.TERMINAL
                && (state.getTransitions() == null || state.getTransitions().isEmpty());

        if (terminate) {
            if (state.isClearSavedState()) {
                contextRepository.delete(context.getMsisdn());
            }
            return UssdResponse.endSession(text);
        }

        return UssdResponse.continueSession(text);
    }

    private void navigateToTarget(FlowContext context, String target) {
        // Resolve ${} expressions in goto targets (used by resume flow)
        if (target.contains("${")) {
            target = expressionResolver.resolve(target, context.getData());
        }

        if (target.startsWith("flow:")) {
            String ref = target.substring(5);
            String[] parts = ref.split("\\.", 2);
            String flowId = parts[0];
            String stateId = parts.length == 2 ? parts[1] : flowRegistry.getFirstStateId(flowRegistry.getFlow(flowId));
            context.navigateTo(flowId, stateId);
        } else {
            context.setStateId(target);
            context.touch();
        }
    }

    private TransitionDefinition resolveTransition(StateDefinition state, String userInput) {
        if (state.getTransitions() == null) {
            return null;
        }

        // Exact match first
        TransitionDefinition transition = state.getTransitions().get(userInput);
        if (transition != null) {
            return transition;
        }

        // Range match (e.g., "1-8")
        for (Map.Entry<String, TransitionDefinition> entry : state.getTransitions().entrySet()) {
            String key = entry.getKey();
            if (key.contains("-") && !key.startsWith("_")) {
                String[] range = key.split("-", 2);
                try {
                    int low = Integer.parseInt(range[0]);
                    int high = Integer.parseInt(range[1]);
                    int input = Integer.parseInt(userInput);
                    if (input >= low && input <= high) {
                        return entry.getValue();
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // Default/wildcard match
        return state.getTransitions().get("_default");
    }

    @SuppressWarnings("unchecked")
    private Object resolveTransitionValue(String valueExpr, String userInput, FlowContext context) {
        if ("input".equals(valueExpr)) {
            return userInput;
        }

        if (valueExpr.startsWith("data[input]")) {
            // Resolve to the selected item from dynamic data as a Map (not toString)
            try {
                int index = Integer.parseInt(userInput) - 1; // 1-based to 0-based
                Object dynamicData = context.get("_dynamicData");
                if (dynamicData instanceof java.util.List) {
                    java.util.List<Map<String, String>> items = (java.util.List<Map<String, String>>) dynamicData;
                    if (index >= 0 && index < items.size()) {
                        return items.get(index);
                    }
                }
            } catch (NumberFormatException ignored) {
            }
            return userInput;
        }

        return valueExpr;
    }
}
