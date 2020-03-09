package com.hw;

import lombok.extern.slf4j.Slf4j;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@SpringBootApplication
@EnableScheduling
public class TestRunner {
    public static void main(String[] args) {
        SpringApplication.run(TestRunner.class, args);
    }

    @Scheduled(fixedRate = 600*1000)
    public void runTest() {
        Result result = JUnitCore.runClasses(IntegrationTestSuite.class);
        for (Failure failure : result.getFailures()) {
            log.error(failure.toString());
        }
        log.info("Tests executed {} ignored {} failed {} elapse {}ms", result.getRunCount(), result.getIgnoreCount(), result.getFailureCount(), result.getRunTime());
        if (result.wasSuccessful()) {
            log.info("Tests all passed");
        } else {
            log.info("Tests failed, check log ");
        }
    }
}