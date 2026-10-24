package com.agrosphere.estate.adapter.out.messaging;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.amqp.core.TopicExchange;
@Configuration @EnableScheduling
public class MessagingConfiguration {
    @Bean @ConditionalOnProperty(name="estate.outbox.enabled",havingValue="true")
    TopicExchange domainExchange() { return new TopicExchange("agrosphere.domain",true,false); }
}
