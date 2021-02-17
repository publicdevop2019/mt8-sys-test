package com.hw.concurrent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hw.helper.ForgetPasswordRequest;
import com.hw.helper.OutgoingReqInterceptor;
import com.hw.helper.UserAction;
import lombok.extern.slf4j.Slf4j;
import org.junit.*;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static com.hw.helper.UserAction.assertConcurrent;
import static org.junit.Assert.assertTrue;

/**
 * need to verify through log or a real email
 */
@RunWith(SpringRunner.class)
@Slf4j
@SpringBootTest
public class MessengerServiceTest {
    @Autowired
    UserAction action;
    ObjectMapper mapper = new ObjectMapper();
    int numOfConcurrent = 10;
    private String valid_register_clientId = "register-id";
    private String valid_empty_secret = "";
    UUID uuid;
    @Rule
    public TestWatcher watchman = new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
            action.saveResult(description, uuid);
            log.error("test failed, method {}, uuid {}", description.getMethodName(), uuid);
        }
    };

    @Before
    public void setUp() {
        action.initTestUser();
        uuid = UUID.randomUUID();
        action.restTemplate.getRestTemplate().setInterceptors(Collections.singletonList(new OutgoingReqInterceptor(uuid)));
    }

    @Test
    public void concurrent_reset_pwd_for_same_user() {
        AtomicInteger emailSuccessCount = new AtomicInteger(0);
        AtomicInteger emailFailedCount = new AtomicInteger(0);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                ArrayList<Integer> integers = new ArrayList<>();
                integers.add(200);
                integers.add(400);
                ResponseEntity<DefaultOAuth2AccessToken> registerTokenResponse = action.getJwtClientCredential(valid_register_clientId, valid_empty_secret);
                action.testUser.get(0).getEmail();
                String value = registerTokenResponse.getBody().getValue();
                String url = UserAction.proxyUrl + UserAction.SVC_NAME_AUTH + "/resourceOwners/forgetPwd";
                ForgetPasswordRequest forgetPasswordRequest = new ForgetPasswordRequest();
                forgetPasswordRequest.setEmail(action.testUser.get(0).getEmail());
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(value);
                String s1 = null;
                try {
                    s1 = mapper.writeValueAsString(forgetPasswordRequest);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
                HttpEntity<String> request = new HttpEntity<>(s1, headers);
                ResponseEntity<Object> exchange = action.restTemplate.exchange(url, HttpMethod.POST, request, Object.class);
                log.info("response " + exchange.toString());
                Assert.assertTrue("expected status code but is " + exchange.getStatusCodeValue(), integers.contains(exchange.getStatusCodeValue()));
                if (exchange.getStatusCodeValue() == 200) {
                    emailSuccessCount.incrementAndGet();
                }
                if (exchange.getStatusCodeValue() == 400) {
                    emailFailedCount.incrementAndGet();
                }
            }
        };
        ArrayList<Runnable> runnables = new ArrayList<>();
        IntStream.range(0, numOfConcurrent).forEach(e -> {
            runnables.add(runnable);
        });
        try {
            assertConcurrent("", runnables, 30000);
            assertTrue("success email should be 1 but is " + emailSuccessCount.get(), emailSuccessCount.get() == 1);
            assertTrue("failed email should be 9 but is " + emailFailedCount.get(), emailFailedCount.get() == numOfConcurrent - 1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    @Ignore
    public void concurrent_reset_pwd_for_dif_user() {

    }


}
