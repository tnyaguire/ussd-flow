# ussd-flow

A Spring Boot starter for building USSD applications with declarative YAML flows. Define screens, transitions, and business logic without writing framework code.

## Quick Start

### 1. Add dependency

```xml
<dependency>
    <groupId>zw.co.getsol</groupId>
    <artifactId>ussd-flow</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. Configure

```yaml
sol:
  ussd:
    screen-char-limit: 159
    flow-paths: "classpath:flows/*.yml"
    redis-prefix: "myapp"
    entry-router: "entryRouter"
    msisdn:
      country-code: "263"
      operators:
        econet:
          prefixes: ["77", "78"]
          name: "Econet"
```

### 3. Define flows

```yaml
# src/main/resources/flows/main.yml
flow:
  id: "main"
  name: "Main Menu"
  ttl: "5m"

  states:
    welcome:
      type: STATIC_MENU
      screen:
        header: "Welcome to MyApp\nBal: $${balance}"
        options:
          1: "Check balance"
          2: "Send money"
        footer: "0. Exit"
      transitions:
        "1": { goto: "balance" }
        "2": { goto: "flow:transfer.start" }
        "0": { terminate: true }

    balance:
      type: TERMINAL
      screen:
        header: "Your balance is $${balance}"
```

### 4. Implement the entry router

```java
@Service("entryRouter")
public class EntryRouter implements FlowRouter {
    @Override
    public RouteResult route(FlowContext context) {
        return new RouteResult("main", "welcome",
            Map.of("balance", "10.00"));
    }
}
```

### 5. Run

The app auto-configures everything. Test via the simulator endpoint:

```bash
curl -X POST http://localhost:8080/ussd/simulator \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"s1","msisdn":"0771234567","userInput":""}'
```

## How It Works

### Flow Engine

Every USSD request goes through:

```
Telco Gateway → UssdGatewayController → FlowEngine → Redis (state) → Response
```

1. **New dial** (no saved state): FlowEngine calls the `entryRouter` bean to determine the starting flow and state.
2. **Continuation** (same session ID): FlowEngine looks up the current state, matches user input to a transition, executes any action, and renders the next state.
3. **Resume** (new session ID, saved state exists): FlowEngine calls the `entryRouter` which can inspect the saved state and decide whether to resume or start fresh.

### State Types

| Type | Purpose | Example |
|------|---------|---------|
| `STATIC_MENU` | Fixed options with `${var}` placeholders | Main menu, confirmation screens |
| `DYNAMIC_MENU` | Options loaded from a `DataLoader` bean | Top bets, match lists |
| `INPUT` | Free text input with optional validation | Amount entry, ID number |
| `TERMINAL` | End screen (session terminates if no transitions defined) | Success message |
| `ROUTER` | No screen — routes conditionally via a `FlowRouter` bean | Entry point, resume logic |

### YAML Schema

```yaml
flow:
  id: "flow-id"           # unique identifier
  name: "Human Name"       # for logging/debugging
  ttl: "30m"               # Redis TTL for saved state (s/m/h)

  states:
    state-id:
      type: STATIC_MENU    # see state types above
      data-loader: "bean"  # for DYNAMIC_MENU — Spring bean name
      validation: "bean"   # for INPUT — Spring bean name
      router-bean: "bean"  # for ROUTER — Spring bean name
      screen:
        header: "Text with ${variables}"
        options:            # numbered options (LinkedHashMap)
          1: "Option one"
          2: "Option two"
        footer: "0. Back"
        options-from: "data"          # for DYNAMIC_MENU
        option-format: "${name}"      # template per data item
        extra-options:                # additional fixed options
          9: { label: "More" }
      transitions:
        "1": { goto: "next-state" }
        "2": { goto: "flow:other-flow.state", save: { key: "value" } }
        "3": { action: "actionBean", goto: "after-action" }
        "1-5": { goto: "target", save: { item: "data[input]" } }
        "0": { terminate: true, clear-state: true }
        "_valid": { goto: "next" }    # for INPUT states
        "_invalid": { goto: "retry" } # for INPUT states
        "_default": { goto: "error" } # fallback
      save-point: true                # persist to Redis after this state
      clear-saved-state: true         # clear Redis on reaching this state
```

### Transition Options

| Field | Type | Purpose |
|-------|------|---------|
| `goto` | String | Next state. Use `flow:id.state` for cross-flow navigation |
| `action` | String | Spring bean name implementing `FlowAction`. Runs before navigation |
| `save` | Map | Key-value pairs to save to context. `"input"` = user input, `"data[input]"` = selected dynamic item |
| `terminate` | boolean | End the USSD session |
| `clear-state` | boolean | Delete saved state from Redis |

### SPI Interfaces

Implement these in your app as `@Service` beans:

```java
// Business logic (place bet, deposit, withdraw)
@FunctionalInterface
public interface FlowAction {
    ActionResult execute(FlowContext context);
}

// Dynamic data (match lists, bet options)
@FunctionalInterface
public interface DataLoader {
    DataResult load(FlowContext context);
}

// Input validation (amounts, IDs)
@FunctionalInterface
public interface InputValidator {
    ValidationResult validate(String input, FlowContext context);
}

// Conditional routing (entry point, resume logic)
@FunctionalInterface
public interface FlowRouter {
    RouteResult route(FlowContext context);
}
```

### Session Persistence

State is stored in Redis keyed by `{prefix}:ctx:{msisdn}`:

- **Save points**: State is persisted when a screen with `save-point: true` is rendered
- **TTL**: Each flow defines its TTL. Default is 5 minutes.
- **Resume**: When a user re-dials after a session drop, saved state enables 1-tap recovery
- **Clear**: State is deleted on `clear-state: true` transitions or when `clear-saved-state: true` states are reached

### Gateway Adapters

Built-in endpoints for telco integration:

| Gateway | Endpoint | Format |
|---------|----------|--------|
| Simulator | `POST /ussd/simulator` | JSON |
| Econet | `POST /ussd/econet` | XML |
| 6D | `POST /ussd/sixd` | XML |
| Flares | `GET /ussd/flares` | Query params |

All paths are configurable via `sol.ussd.gateway.*` properties.

## Configuration Reference

```yaml
sol:
  ussd:
    enabled: true                           # enable/disable auto-configuration
    screen-char-limit: 159                  # max characters per screen
    flow-paths: "classpath:flows/*.yml"     # where to find YAML flow files
    redis-prefix: "ussd"                    # Redis key prefix
    entry-router: "entryRouter"             # bean name of the FlowRouter for entry
    session:
      default-ttl: 5m                       # default TTL for flow context
    gateway:
      simulator-path: /ussd/simulator
      econet-path: /ussd/econet
      sixd-path: /ussd/sixd
      flares-path: /ussd/flares
    msisdn:
      country-code: "263"                   # country code for phone number parsing
      operators:                            # operator prefix mapping
        econet:
          prefixes: ["77", "78"]
          name: "Econet"
```

## Requirements

- Java 21+
- Spring Boot 3.x
- Redis (for session persistence)

## Building

```bash
mvn clean install
```
