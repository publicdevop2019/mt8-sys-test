package com.hw.integration.profile;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hw.helper.OrderDetail;
import com.hw.helper.OutgoingReqInterceptor;
import com.hw.helper.UserAction;
import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.junit.*;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class OrderTest {
    @Autowired
    UserAction action;
    public ObjectMapper mapper = new ObjectMapper().configure(MapperFeature.USE_ANNOTATIONS, false).setSerializationInclusion(JsonInclude.Include.NON_NULL);

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
    public void shop_create_then_confirm_payment_for_an_order() {
        String defaultUserToken = action.registerResourceOwnerThenLogin();
        String profileId1 = action.getProfileId(defaultUserToken);
        OrderDetail orderDetailForUser = action.createOrderDetailForUser(defaultUserToken, profileId1);
        String url3 = UserAction.proxyUrl + UserAction.PROFILE_SVC + "/profiles/" + profileId1 + "/orders";
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

    @Test
    public void shop_create_same_order_again() {
        String defaultUserToken = action.registerResourceOwnerThenLogin();
        String profileId1 = action.getProfileId(defaultUserToken);
        OrderDetail orderDetailForUser = action.createOrderDetailForUser(defaultUserToken, profileId1);
        String url3 = UserAction.proxyUrl + UserAction.PROFILE_SVC + "/profiles/" + profileId1 + "/orders";
        action.restTemplate.exchange(url3, HttpMethod.POST, action.getHttpRequest(defaultUserToken, orderDetailForUser), String.class);
        ResponseEntity<String> exchange7 = action.restTemplate.exchange(url3, HttpMethod.POST, action.getHttpRequest(defaultUserToken, orderDetailForUser), String.class);
        Assert.assertEquals(HttpStatus.OK, exchange7.getStatusCode());
        Assert.assertNotNull(exchange7.getHeaders().getLocation().toString());
    }

    @Test
    public void shop_read_history_orders() {
        String defaultUserToken = action.registerResourceOwnerThenLogin();
        String profileId1 = action.getProfileId(defaultUserToken);
        String url = UserAction.proxyUrl + UserAction.PROFILE_SVC + "/profiles/" + profileId1 + "/orders";
        ParameterizedTypeReference<List<OrderDetail>> responseType = new ParameterizedTypeReference<>() {
        };
        ResponseEntity<List<OrderDetail>> exchange = action.restTemplate.exchange(url, HttpMethod.GET, action.getHttpRequest(defaultUserToken), responseType);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
    }

    @Test
    public void shop_read_order_details() {
        String defaultUserToken = action.registerResourceOwnerThenLogin();
        String profileId1 = action.getProfileId(defaultUserToken);
        OrderDetail orderDetailForUser = action.createOrderDetailForUser(defaultUserToken, profileId1);
        String url3 = UserAction.proxyUrl + UserAction.PROFILE_SVC + "/profiles/" + profileId1 + "/orders";
        ResponseEntity<String> exchange = action.restTemplate.exchange(url3, HttpMethod.POST, action.getHttpRequest(defaultUserToken, orderDetailForUser), String.class);
        String orderId = action.getOrderId(exchange.getHeaders());
        ResponseEntity<OrderDetail> exchange2 = action.restTemplate.exchange(url3 + "/" + orderId, HttpMethod.GET, action.getHttpRequest(defaultUserToken), OrderDetail.class);
        Assert.assertEquals(HttpStatus.OK, exchange2.getStatusCode());
        Assert.assertNotNull(exchange2.getBody());


    }

    @Test
    @Ignore
    public void shop_place_order_but_insufficient_order_storage() {

    }


}
