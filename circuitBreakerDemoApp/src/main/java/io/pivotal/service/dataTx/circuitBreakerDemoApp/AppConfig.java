package io.pivotal.service.dataTx.circuitBreakerDemoApp;

import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.client.Pool;
import org.apache.geode.cache.client.PoolFactory;
import org.apache.geode.cache.client.PoolManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.gemfire.client.ClientCacheFactoryBean;
import org.springframework.data.gemfire.client.ClientRegionFactoryBean;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.data.gemfire.config.annotation.ClientCacheConfigurer;
import org.springframework.data.gemfire.config.annotation.EnableLogging;
import org.springframework.data.gemfire.config.annotation.EnableSecurity;
import org.springframework.data.gemfire.support.ConnectionEndpoint;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Greogyr Green
 *
 */
@Configuration
@ClientCacheApplication
@EnableSecurity
@EnableLogging
public class AppConfig
{
    private static final Pattern regExpPattern = Pattern.compile("(.*)\\[(\\d*)\\].*");

    @Bean
    public ClientCacheConfigurer configurer(
            @Value("${spring.data.gemfire.locators}")
                    String locators)
    {
        ConnectionEndpoint locatorsConnection = ConnectionEndpoint.parse(locators);

        ClientCacheConfigurer configurerBean = new ClientCacheConfigurer() {
            @Override
            public void configure(String beanName, ClientCacheFactoryBean clientCacheFactoryBean) {
                clientCacheFactoryBean.addLocators(locatorsConnection);
            }
        };

        return configurerBean;
    }//-------------------------------------------

    @Bean("primaryPool")
    public Pool primaryPool(GemFireCache cache, @Value("${primaryLocators}") String locators)
    {
        PoolFactory factory = PoolManager.createFactory();
        constructConnection(locators,factory);


        return factory.create("A");
    }//-------------------------------------------

    private static void constructConnection(String locators, PoolFactory factory)
    {
        if(locators == null || locators.length() == 0)
            throw new IllegalArgumentException("locators is required");

        String[] parsedLocators = locators.split(",");

        String host,portText;
        int port;
        for (String hostPort : parsedLocators)
        {
            Matcher m = regExpPattern.matcher(hostPort);
            if (!m.matches())
            {
                throw new IllegalStateException("Unexpected locator format. expected host[port], but got:" +  hostPort);
            }

            host = m.group(1);
            portText = m.group(2);

            try{
                port = Integer.parseInt(portText);
            }
            catch(NumberFormatException e){
                throw new IllegalStateException("Invalid port expected host[port], but got:" +  hostPort);
            }

            factory.addLocator(host,port );
        }
    }//------------------------------------------------
    @Bean
    public Pool secondaryPool(GemFireCache cache, @Value("${secondaryLocators}") String secondaryLocators)
    {

        PoolFactory factory = PoolManager.createFactory();

        constructConnection(secondaryLocators,factory);

        return factory.create("B");
    }//------------------------------------------------------

    @Bean
    public CircuitBreaker circuitBreaker(GemFireCache cache,
                                         Pool primaryPool,
                                         Pool secondaryPool,
                                         @Value("${primaryLocators}") String primaryLocators,
                                         @Value("${secondaryLocators}") String secondaryLocators,
                                         @Value("${sleepPeriodMs:2000}") long sleepPeriodMs,
                                         @Value("${spring.data.gemfire.locators}")
                                                     String locators
    )
    {
        //Pool primaryPool, Pool backupPool, String secondaryLocators,
        //                          long sleepPeriodMs)
        CircuitBreaker circuitBreaker = new CircuitBreaker(primaryPool.getName(),secondaryPool.getName(),
                primaryLocators,secondaryLocators,sleepPeriodMs);


        if(locators.equals(secondaryLocators))
        {
            circuitBreaker.startCloseCircuitRunner();
        }

        return circuitBreaker;
    }

    @Bean("test")
    public ClientRegionFactoryBean testRegion(GemFireCache cache)
    {
        ClientRegionFactoryBean<?,?> testRegion = new ClientRegionFactoryBean<>();

        testRegion.setCache(cache);
        testRegion.setDataPolicy(DataPolicy.EMPTY);
        return testRegion;
    }//-----------------------------------------

}
