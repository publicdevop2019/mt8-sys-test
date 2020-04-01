package com.hw;

import com.hw.entity.TestResult;
import com.hw.helper.UserAction;
import com.hw.repo.TestResultRepo;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PreDestroy;

@Slf4j
@SpringBootApplication
@EnableScheduling
public class TestRunner {
    @Autowired
    TestResultRepo testResultRepo;

    public static void main(String[] args) {
        SpringApplication.run(TestRunner.class, args);
    }

    @Scheduled(fixedRate = 300 * 1000)
    public void runTest() {
        log.info("test started");
        TestResult testResult = new TestResult();
        testResult.setStatus("just started");
        StringBuilder stringBuilder = new StringBuilder();
        testResultRepo.save(testResult);
        Result result = JUnitCore.runClasses(IntegrationTestSuite.class);
        for (Failure failure : result.getFailures()) {
            log.error(failure.toString());
            stringBuilder.append(failure.toString());
        }
        log.info("Tests executed {} ignored {} failed {} elapse {}ms", result.getRunCount(), result.getIgnoreCount(), result.getFailureCount(), result.getRunTime());
        testResult.setTestExecuted(result.getRunCount());
        testResult.setIgnored(result.getIgnoreCount());
        testResult.setFailed(result.getFailureCount());
        testResult.setElapse(result.getRunTime());
        if (result.wasSuccessful()) {
            log.info("Tests all passed");
            testResult.setStatus("success");
        } else {
            testResult.setFailedMsg(stringBuilder.toString());
            testResult.setStatus("failed");
            log.error("Tests failed, check log");
        }
        testResultRepo.save(testResult);
    }
    @PreDestroy
    public void onExit() {
        log.info("Closing application..");
        LogManager.shutdown();
    }
}