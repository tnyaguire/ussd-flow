package zw.co.getsol.ussd.engine.spi;

import zw.co.getsol.ussd.engine.FlowContext;

@FunctionalInterface
public interface FlowRouter {

    RouteResult route(FlowContext context);
}
