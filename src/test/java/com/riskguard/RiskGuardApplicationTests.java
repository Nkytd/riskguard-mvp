package com.riskguard;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "riskguard.mq.listener-auto-startup=false")
class RiskGuardApplicationTests {

    @Test
    void contextLoads() {
    }
}
