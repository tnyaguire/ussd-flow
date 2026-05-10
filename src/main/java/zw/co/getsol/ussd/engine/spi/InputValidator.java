package zw.co.getsol.ussd.engine.spi;

import zw.co.getsol.ussd.engine.FlowContext;

@FunctionalInterface
public interface InputValidator {

    ValidationResult validate(String input, FlowContext context);
}
