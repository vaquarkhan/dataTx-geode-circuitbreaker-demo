package io.pivotal.service.dataTx.circuitBreakerDemoApp;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.NoAvailableLocatorsException;
import org.apache.geode.cache.client.NoAvailableServersException;
import org.apache.geode.cache.client.Pool;
import org.apache.geode.cache.client.PoolManager;
import org.apache.geode.cache.execute.*;
import org.apache.geode.distributed.PoolCancelledException;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

import java.net.ConnectException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CircuitBreaker
{

    private final String primaryPoolName;
    private final String secondaryPoolName;
    private final String secondaryLocators;
    private final long sleepPeriodMs;
    private static final String FUNCTION_NAME ="CircuitBreakerStatusFunc";

    private final ExecutorService executor;

    private static ConfigurableApplicationContext context=null;


    public CircuitBreaker(String primaryPool, String backupPool, String secondaryLocators,
                          long sleepPeriodMs)
    {
        this.secondaryLocators = secondaryLocators;
        this.primaryPoolName = primaryPool;
        this.secondaryPoolName = backupPool;
        this.sleepPeriodMs = sleepPeriodMs;
        this.executor = Executors.newCachedThreadPool();
    }

    public boolean isPrimaryUp()
    throws Exception
    {
        return doCheck(PoolManager.find(this.primaryPoolName));

    }

    /**
     * Determine if the secondary cluster is up and running
     * @return
     */
    public boolean isSecondaryUp()
    throws Exception
    {
          return doCheck(PoolManager.find(this.secondaryPoolName));
    }

    private boolean doCheck(Pool secondaryPool)
    throws Exception
    {
        try
        {
            ResultCollector rc = FunctionService
                    .onServer(secondaryPool)
                    .execute(FUNCTION_NAME);

            return checkResultsOK(rc);

        }
      catch(ConnectException | NoAvailableLocatorsException | NoAvailableServersException | PoolCancelledException e){
            e.printStackTrace();
            return false;
        }


    }

    private boolean checkResultsOK(ResultCollector<?, ?> resultCollector)
    throws Exception
    {
        if(resultCollector == null)
            throw new IllegalArgumentException("resultCollector required");


        Object resultsObject = resultCollector.getResult();
        if(resultsObject == null)
            throw new RuntimeException("No results found");

        //Return a result in collection (for a single response)
        Collection<Object> collectionResults = (Collection<Object>)resultsObject;

        //if empty return null
        if(collectionResults.isEmpty())
            throw new RuntimeException("No results found");


        for (Object inputObj : collectionResults)
        {
            if(inputObj instanceof Exception )
                throw (Exception)inputObj;

            if(inputObj == null)
                continue;

            if(inputObj instanceof Collection)
            {
                Collection<Object> innerCollection = (Collection)inputObj;
                for (Object obj:innerCollection) {
                    if( obj instanceof Exception)
                        throw (Exception)obj;
                }
            }
        }

        return true;

    }

    public static void setContext(ConfigurableApplicationContext appContext)
    {
        CircuitBreaker.context = appContext;
    }//------------------------------------
    public void closeToPrimary()
    {
        Runnable r = () -> {
            CircuitBreaker.context.close();
            String[] sourceArgs = {};

            CircuitBreaker.context = SpringApplication.run(DemoApp.class,sourceArgs );
        };

        executor.submit(r);

    }//-----------------------------------------------

    public void openCircuit()
    throws Exception
    {
        System.out.println("CHECKING IF primaur is UP");

        if(this.isPrimaryUp()){
            System.out.println("Primary is UP, so NOT SWITHING");
            return;
        }

        this.openToSecondary();
    }

    public void openToSecondary()
    {
        System.out.println("Opening to secondary");

        Runnable openAndSwitch = () -> {
            CircuitBreaker.context.close();

            String[] sourceArgs = {"--spring.data.gemfire.locators="+this.secondaryLocators};

            CircuitBreaker.context = SpringApplication.run(DemoApp.class,sourceArgs );
        };

        this.executor.submit(openAndSwitch);

       // startCloseCircuitRunner();


    }

    public void startCloseCircuitRunner()
    {
        Runnable closerRunner = new CircuitCloserRunner(this,sleepPeriodMs);
        this.executor.submit(closerRunner);
    }
}
