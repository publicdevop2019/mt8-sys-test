package com.hw.concurrent;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hw.helper.*;
import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;
import java.util.stream.IntStream;

import static com.hw.concurrent.ProductServiceTest.assertConcurrent;
import static com.hw.integration.profile.OrderTest.getOrderIdFromPaymentLink;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class OrderServiceTest {
    public ObjectMapper mapper = new ObjectMapper().configure(MapperFeature.USE_ANNOTATIONS, false).setSerializationInclusion(JsonInclude.Include.NON_NULL);
    @Autowired
    UserAction action;
    int numOfConcurrent = 1;
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
                OrderDetail orderDetailForUser = action.createOrderDetailForUser(defaultUserToken);
                String url3 = UserAction.proxyUrl + UserAction.PROFILE_SVC +  "/orders/user";
                ResponseEntity<String> exchange = action.restTemplate.exchange(url3, HttpMethod.POST, action.getHttpRequest(defaultUserToken, orderDetailForUser), String.class);
                Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
                Assert.assertNotNull(exchange.getHeaders().getLocation().toString());
                String url4 = UserAction.proxyUrl + UserAction.PROFILE_SVC +  "/orders/user/" + getOrderIdFromPaymentLink(exchange.getHeaders().getLocation().toString()) + "/confirm";
                ResponseEntity<String> exchange7 = action.restTemplate.exchange(url4, HttpMethod.PUT, action.getHttpRequest(defaultUserToken), String.class);
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

    /**
     * this test need to send reserve request right after payment confirmed
     * and autoConfirm is not finished
     *
     * @assert need to search for transaction_task see if there's task in
     * started status, expected is zero task found
     */
    @Test
    public void place_order_then_confirm_pay_and_reserve_at_same_time() {
        String defaultUserToken = action.registerResourceOwnerThenLogin();
        OrderDetail orderDetailForUser = action.createOrderDetailForUser(defaultUserToken);
        String url3 = UserAction.proxyUrl + UserAction.PROFILE_SVC +  "/orders/user";
        ResponseEntity<String> exchange = action.restTemplate.exchange(url3, HttpMethod.POST, action.getHttpRequest(defaultUserToken, orderDetailForUser), String.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
        Assert.assertNotNull(exchange.getHeaders().getLocation().toString());
        String orderIdFromPaymentLink = getOrderIdFromPaymentLink(exchange.getHeaders().getLocation().toString());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                String url4 = UserAction.proxyUrl + UserAction.PROFILE_SVC +  "/orders/user/" + orderIdFromPaymentLink + "/confirm";
                ResponseEntity<String> exchange7 = action.restTemplate.exchange(url4, HttpMethod.PUT, action.getHttpRequest(defaultUserToken), String.class);
                Assert.assertEquals(HttpStatus.OK, exchange7.getStatusCode());
                Boolean read = JsonPath.read(exchange7.getBody(), "$.paymentStatus");
                Assert.assertEquals(true, read);
            }
        };
        Runnable runnable2 = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                SnapshotAddress snapshotAddress = new SnapshotAddress();
                BeanUtils.copyProperties(action.getRandomAddress(), snapshotAddress);
                orderDetailForUser.setAddress(snapshotAddress);

                String url4 = UserAction.proxyUrl + UserAction.PROFILE_SVC +  "/orders/user/" + orderIdFromPaymentLink + "/reserve";
                ResponseEntity<String> exchange7 = action.restTemplate.exchange(url4, HttpMethod.PUT, action.getHttpRequest(defaultUserToken, orderDetailForUser), String.class);
                Assert.assertEquals(HttpStatus.OK, exchange7.getStatusCode());
            }
        };
        ArrayList<Runnable> runnables = new ArrayList<>();
        runnables.add(runnable);
        runnables.add(runnable2);
        try {
            assertConcurrent("", runnables, 30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
