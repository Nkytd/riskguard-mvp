package com.riskguard.mq.publisher;

import com.riskguard.common.utils.ClockUtils;
import com.riskguard.decision.dto.RiskDecisionRequest;
import com.riskguard.decision.dto.RiskDecisionResponse;
import com.riskguard.mq.config.RiskMqConstants;
import com.riskguard.mq.event.RiskCaseCreatedEvent;
import com.riskguard.mq.event.RiskDecisionCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class RiskEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RiskEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public RiskEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishDecisionCreated(RiskDecisionRequest request, RiskDecisionResponse response) {
        RiskDecisionCreatedEvent event = RiskDecisionCreatedEvent.from(request, response, ClockUtils.now());
        publishAfterCommit(RiskMqConstants.ROUTING_DECISION_CREATED, event);
    }

    public void publishCaseCreated(RiskCaseCreatedEvent event) {
        publishAfterCommit(RiskMqConstants.ROUTING_CASE_CREATED, event);
    }

    private void publishAfterCommit(String routingKey, Object event) {
        Runnable publishTask = () -> publish(routingKey, event);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publishTask.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publishTask.run();
            }
        });
    }

    private void publish(String routingKey, Object event) {
        try {
            rabbitTemplate.convertAndSend(RiskMqConstants.EVENT_EXCHANGE, routingKey, event);
        } catch (AmqpException ex) {
            log.warn("Failed to publish risk event, routingKey={}, event={}", routingKey, event, ex);
        }
    }
}
