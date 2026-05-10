package zw.co.getsol.ussd.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import zw.co.getsol.ussd.config.UssdProperties;
import zw.co.getsol.ussd.exception.UssdException;
import zw.co.getsol.ussd.msisdn.MsisdnParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Optional;

public class FlowContextRepository {

    private static final Logger log = LoggerFactory.getLogger(FlowContextRepository.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final String keyPrefix;
    private final Duration defaultTtl;

    public FlowContextRepository(StringRedisTemplate redis, ObjectMapper objectMapper, UssdProperties properties) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.keyPrefix = properties.getRedisPrefix() + ":ctx:";
        this.defaultTtl = properties.getSession().getDefaultTtl();
    }

    public Optional<FlowContext> findByMsisdn(String msisdn) {
        String key = keyPrefix + msisdn;
        String json = redis.opsForValue().get(key);
        if (json == null) {
            return Optional.empty();
        }
        try {
            FlowContext context = objectMapper.readValue(json, FlowContext.class);
            return Optional.of(context);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize FlowContext for MSISDN {}", MsisdnParser.maskMsisdn(msisdn), e);
            delete(msisdn);
            return Optional.empty();
        }
    }

    public void save(FlowContext context, Duration ttl) {
        String key = keyPrefix + context.getMsisdn();
        context.touch();
        try {
            String json = objectMapper.writeValueAsString(context);
            Duration effectiveTtl = ttl != null ? ttl : defaultTtl;
            redis.opsForValue().set(key, json, effectiveTtl);
            log.debug("Saved FlowContext for MSISDN {} with TTL {}", MsisdnParser.maskMsisdn(context.getMsisdn()), effectiveTtl);
        } catch (JsonProcessingException e) {
            throw new UssdException("Failed to save session state", e);
        }
    }

    public void save(FlowContext context) {
        save(context, defaultTtl);
    }

    public void delete(String msisdn) {
        String key = keyPrefix + msisdn;
        redis.delete(key);
        log.debug("Deleted FlowContext for MSISDN {}", MsisdnParser.maskMsisdn(msisdn));
    }
}
