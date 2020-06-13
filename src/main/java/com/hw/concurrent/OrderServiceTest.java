package com.hw.concurrent;

import com.hw.helper.OrderDetail;
import com.hw.helper.OutgoingReqInterceptor;
import com.hw.helper.UserAction;
import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.IntStream;

import static com.hw.concurrent.ProductServiceTest.assertConcurrent;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class OrderServiceTest {
    @Autowired
    UserAction action;
    int numOfConcurrent = 10;
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
        uuid = UUID.randomUUID();
        action.restTemplate.getRestTemplate().setInterceptors(Collections.singletonList(new OutgoingReqInterceptor(uuid)));
    }

    @Test
    public void place_then_pay_an_order_current() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                String defaultUserToken = action.registerResourceOwnerThenLogin();
                String profileId1 = action.getProfileId(defaultUserToken);
                OrderDetail orderDetailForUser = action.createOrderDetailForUser(defaultUserToken, profileId1);
                String preorderId = action.getOrderId(defaultUserToken, profileId1);
                String url3 = UserAction.proxyUrl + UserAction.PROFILE_SVC + "/profiles/" + profileId1 + "/orders/" + preorderId;
                ResponseEntity<String> exchange = action.restTemplate.exchange(url3, HttpMethod.POST, action.getHttpRequest(defaultUserToken, orderDetailForUser), String.class);
                Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
                Assert.assertNotNull(exchange.getHeaders().getLocation().toString());
                String orderId = action.getOrderId(exchange.getHeaders());
                String url4 = UserAction.proxyUrl + UserAction.PROFILE_SVC + "/profiles/" + profileId1 + "/orders/" + orderId + "/confirm";
                ResponseEntity<String> exchange7 = action.restTemplate.exchange(url4, HttpMethod.GET, action.getHttpRequest(defaultUserToken), String.class);
                Assert.assertEquals(HttpStatus.OK, exchange7.getStatusCode());
                Boolean read = JsonPath.read(exchange7.getBody(), "$.paymentStatus");
                Assert.assertEquals(true, read);
            }
        };
        ArrayList<Runnable> runnables = new ArrayList<>();
        IntStream.range(0, numOfConcurrent).forEach(e -> {
            runnables.add(runnable);
        });
        try {
            assertConcurrent("", runnables, 30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
