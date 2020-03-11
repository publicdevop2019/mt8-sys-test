package com.hw.concurrent;

import com.hw.helper.OrderDetail;
import com.hw.helper.UserAction;
import com.jayway.jsonpath.JsonPath;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.stream.IntStream;

import static com.hw.concurrent.ProductServiceTest.assertConcurrent;

@RunWith(SpringRunner.class)
public class OrderTest {
    UserAction action = new UserAction();
    TestRestTemplate restTemplate = new TestRestTemplate();
    int numOfConcurrent = 10;

    @Test
    public void place_then_pay_an_order_current() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                String defaultUserToken = action.registerResourceOwnerThenLogin();
                String profileId1 = action.getProfileId(defaultUserToken);
                OrderDetail orderDetailForUser = action.createOrderDetailForUser(defaultUserToken, profileId1);
                String url3 = UserAction.proxyUrl + "/api/profiles/" + profileId1 + "/orders";
                ResponseEntity<String> exchange = restTemplate.exchange(url3, HttpMethod.POST, action.getHttpRequest(defaultUserToken, orderDetailForUser), String.class);
                Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
                Assert.assertNotNull(exchange.getHeaders().getLocation().toString());
                String orderId = action.getOrderId(exchange.getHeaders());
                String url4 = UserAction.proxyUrl + "/api/profiles/" + profileId1 + "/orders/" + orderId + "/confirm";
                ResponseEntity<String> exchange7 = restTemplate.exchange(url4, HttpMethod.GET, action.getHttpRequest(defaultUserToken), String.class);
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
