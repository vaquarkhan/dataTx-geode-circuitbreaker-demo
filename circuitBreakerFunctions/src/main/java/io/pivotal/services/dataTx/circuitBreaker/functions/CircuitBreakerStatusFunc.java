package io.pivotal.services.dataTx.circuitBreaker.functions;

import org.apache.geode.cache.Declarable;
import org.apache.geode.cache.execute.Function;
import org.apache.geode.cache.execute.FunctionContext;

public class CircuitBreakerStatusFunc implements Function, Declarable
{
    @Override
    public boolean isHA()
    {
        return false;
    }

    @Override
    public void execute(FunctionContext context)
    {
        context.getResultSender().lastResult(true);
    }

    /**
     *
     * @return simple class name
     */
    @Override
    public String getId()
    {
        return CircuitBreakerStatusFunc.class.getSimpleName();
    }
}
