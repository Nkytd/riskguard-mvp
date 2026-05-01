package com.riskguard;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.riskguard")
@SpringBootApplication
public class RiskGuardApplication {

    public static void main(String[] args) {
        SpringApplication.run(RiskGuardApplication.class, args);
    }
}
