package zw.co.getsol.ussd.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import zw.co.getsol.ussd.engine.*;
import zw.co.getsol.ussd.gateway.UssdGatewayController;
import zw.co.getsol.ussd.msisdn.MsisdnParser;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@ConditionalOnProperty(name = "sol.ussd.enabled", matchIfMissing = true)
@EnableConfigurationProperties(UssdProperties.class)
public class UssdAutoConfiguration {

    @Bean("ussdObjectMapper")
    public ObjectMapper ussdObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Bean
    @ConditionalOnMissingBean
    public MsisdnParser msisdnParser(UssdProperties properties) {
        return new MsisdnParser(properties.getMsisdn());
    }

    @Bean
    @ConditionalOnMissingBean
    public FlowRegistry flowRegistry(UssdProperties properties) {
        return new FlowRegistry(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public FlowContextRepository flowContextRepository(StringRedisTemplate redisTemplate,
                                                        @Qualifier("ussdObjectMapper") ObjectMapper ussdObjectMapper,
                                                        UssdProperties properties) {
        return new FlowContextRepository(redisTemplate, ussdObjectMapper, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ScreenRenderer screenRenderer(UssdProperties properties) {
        return new ScreenRenderer(properties.getScreenCharLimit());
    }

    @Bean
    @ConditionalOnMissingBean
    public FlowEngine flowEngine(FlowRegistry flowRegistry,
                                  FlowContextRepository flowContextRepository,
                                  ScreenRenderer screenRenderer,
                                  ApplicationContext applicationContext,
                                  UssdProperties properties,
                                  StringRedisTemplate redisTemplate) {
        return new FlowEngine(flowRegistry, flowContextRepository, screenRenderer,
                applicationContext, properties.getEntryRouter(), redisTemplate, properties.getRedisPrefix());
    }

    @Bean
    @ConditionalOnMissingBean
    public UssdGatewayController ussdGatewayController(FlowEngine flowEngine, MsisdnParser msisdnParser) {
        return new UssdGatewayController(flowEngine, msisdnParser);
    }
}
