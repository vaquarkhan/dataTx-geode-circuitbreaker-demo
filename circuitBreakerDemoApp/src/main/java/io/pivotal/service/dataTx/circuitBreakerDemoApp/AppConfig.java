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
import io.pivotal.services.dataTx.geode.client.GeodeSettings;


@Configuration
@ClientCacheApplication
@EnableSecurity
@EnableLogging
public class AppConfig
{
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
    }

    @Bean("primaryPool")
    public Pool primaryPool(@Value("${spring.data.gemfire.locators}") String locators)
    {
        PoolFactory factory = PoolManager.createFactory();
        GeodeSettings.constructLocators(locators,factory);

        return factory.create("A");
    }//-------------------------------------------

    @Bean
    public Pool secondaryPool(@Value("${secondaryLocators}") String secondaryLocators)
    {

        PoolFactory factory = PoolManager.createFactory();

        GeodeSettings.constructLocators(secondaryLocators,factory);

        return factory.create("B");
    }//------------------------------------------------------
    @Bean
    public CircuitBreaker circuitBreaker(GemFireCache cache,
                                         Pool primaryPool,
                                         Pool secondaryPool,
                                         @Value("${secondaryLocators}") String secondaryLocators,
                                         @Value("${sleepPeriodMs:2000}") long sleepPeriodMs,
                                         @Value("${spring.data.gemfire.locators}")
                                                     String locators
    )
    {
        //Pool primaryPool, Pool backupPool, String secondaryLocators,
        //                          long sleepPeriodMs)
        CircuitBreaker circuitBreaker = new CircuitBreaker(primaryPool.getName(),secondaryPool.getName(),secondaryLocators,sleepPeriodMs);


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
        //testRegion.setName("test");
        return testRegion;
    }//-----------------------------------------

}
