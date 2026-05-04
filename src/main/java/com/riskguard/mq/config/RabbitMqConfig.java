package com.riskguard.mq.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMqConfig {

    @Bean
    public DirectExchange riskEventExchange() {
        return new DirectExchange(RiskMqConstants.EVENT_EXCHANGE, true, false);
    }

    @Bean
    public Queue profileUpdateQueue() {
        return new Queue(RiskMqConstants.PROFILE_UPDATE_QUEUE, true);
    }

    @Bean
    public Queue dashboardAggregateQueue() {
        return new Queue(RiskMqConstants.DASHBOARD_AGGREGATE_QUEUE, true);
    }

    @Bean
    public Binding profileUpdateBinding(Queue profileUpdateQueue, DirectExchange riskEventExchange) {
        return BindingBuilder.bind(profileUpdateQueue)
                .to(riskEventExchange)
                .with(RiskMqConstants.ROUTING_DECISION_CREATED);
    }

    @Bean
    public Binding dashboardDecisionBinding(Queue dashboardAggregateQueue, DirectExchange riskEventExchange) {
        return BindingBuilder.bind(dashboardAggregateQueue)
                .to(riskEventExchange)
                .with(RiskMqConstants.ROUTING_DECISION_CREATED);
    }

    @Bean
    public Binding dashboardCaseBinding(Queue dashboardAggregateQueue, DirectExchange riskEventExchange) {
        return BindingBuilder.bind(dashboardAggregateQueue)
                .to(riskEventExchange)
                .with(RiskMqConstants.ROUTING_CASE_CREATED);
    }

    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
