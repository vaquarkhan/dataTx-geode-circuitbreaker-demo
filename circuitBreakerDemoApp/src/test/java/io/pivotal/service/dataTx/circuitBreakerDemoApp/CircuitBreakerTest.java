package io.pivotal.service.dataTx.circuitBreakerDemoApp;

import io.pivotal.services.dataTx.geode.functions.JvmExecution;
import nyla.solutions.core.net.Networking;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.Pool;
import org.apache.geode.cache.client.PoolManager;
import org.apache.geode.cache.execute.Execution;
import org.apache.geode.cache.execute.Function;
import org.apache.geode.cache.execute.ResultCollector;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.Before; 
import org.junit.After;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.junit4.SpringRunner;

import static org.mockito.Mockito.*;

import static org.junit.Assert.*;

/** 
* CircuitBreaker Tester. 
* 
* @author <Authors name> 
* @since <pre>Aug 5, 2019</pre> 
* @version 1.0 
*/
@RunWith(SpringRunner.class)
@SpringBootTest
@IfProfileValue(name = "integration")
public class CircuitBreakerTest { 

    @Value("${host}")
    String host;

    @Value("${primaryPort:10334}")
    int primaryPort;

    @Value("${secondaryPort}")
    int secondaryPort;

    @Test
    public void test_self_maintenance(){
        CircuitBreaker cb = createCircuitBreaker();

        ConfigurableApplicationContext applicationContext = Mockito.mock(ConfigurableApplicationContext.class);

        ApplicationArguments appArgs = mock(ApplicationArguments.class);

        when(applicationContext.getBean(ApplicationArguments.class)).thenReturn(appArgs);
        CircuitBreaker.setContext(applicationContext);

        cb.openToSecondary();

        Mockito.verify(applicationContext).close();



    }

    @Test
    public void test_is_primary_up ()
    throws Exception
    {
        CircuitBreaker cb = createCircuitBreaker();


        assertTrue(cb.isPrimaryUp());

    }//----------------------------------------------------

    private CircuitBreaker createCircuitBreaker()
    {
        return new CircuitBreaker("A","B",host+"["+20334+"]",2000);
    }

    /**
     *
     * @throws Exception when the secondard pool is up down
     */
    @Test
    public void test_is_secondard_up ()
    throws Exception
    {
        CircuitBreaker cb = createCircuitBreaker();

        assertTrue(cb.isSecondaryUp());
    }
} 
