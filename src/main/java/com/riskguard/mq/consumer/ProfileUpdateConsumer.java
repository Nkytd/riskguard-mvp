package com.riskguard.mq.consumer;

import com.riskguard.mq.config.RiskMqConstants;
import com.riskguard.mq.event.RiskDecisionCreatedEvent;
import com.riskguard.profile.service.ProfileAsyncUpdateService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ProfileUpdateConsumer {

    private final ProfileAsyncUpdateService profileAsyncUpdateService;

    public ProfileUpdateConsumer(ProfileAsyncUpdateService profileAsyncUpdateService) {
        this.profileAsyncUpdateService = profileAsyncUpdateService;
    }

    @RabbitListener(queues = RiskMqConstants.PROFILE_UPDATE_QUEUE, autoStartup = "${riskguard.mq.listener-auto-startup:true}")
    public void onDecisionCreated(RiskDecisionCreatedEvent event) {
        profileAsyncUpdateService.applyDecisionEvent(event);
    }
}
