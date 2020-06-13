package com.hw.integration.profile;

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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;
import java.util.List;
import java.util.Random;
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
    public void shop_create_an_order_but_not_confirm() {
        String defaultUserToken = action.registerResourceOwnerThenLogin();
        String profileId1 = action.getProfileId(defaultUserToken);
        OrderDetail orderDetailForUser = action.createOrderDetailForUser(defaultUserToken, profileId1);
        String orderId = action.getOrderId(defaultUserToken, profileId1);
        String url3 = UserAction.proxyUrl + UserAction.PROFILE_SVC + "/profiles/" + profileId1 + "/orders/" + orderId;
        ResponseEntity<String> exchange = action.restTemplate.exchange(url3, HttpMethod.POST, action.getHttpRequest(defaultUserToken, orderDetailForUser), String.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
        Assert.assertNotNull(exchange.getHeaders().getLocation().toString());
    }

    @Test
    public void shop_create_then_confirm_payment_for_an_order() {
        String defaultUserToken = action.registerResourceOwnerThenLogin();
        String profileId1 = action.getProfileId(defaultUserToken);
        OrderDetail orderDetailForUser = action.createOrderDetailForUser(defaultUserToken, profileId1);
        String preorderId = action.getOrderId(defaultUserToken, profileId1);
        String url3 = UserAction.proxyUrl + UserAction.PROFILE_SVC + "/profiles/" + profileId1 + "/orders/" + preorderId;
        ResponseEntity<String> exchange = action.restTemplate.exchange(url3, HttpMethod.POST, action.getHttpRequest(defaultUserToken, orderDetailForUser), String.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
        Assert.assertNotNull(exchange.getHeaders().getLocation().toString());
        String url4 = UserAction.proxyUrl + UserAction.PROFILE_SVC + "/profiles/" + profileId1 + "/orders/" + preorderId + "/confirm";
        ResponseEntity<String> exchange7 = action.restTemplate.exchange(url4, HttpMethod.GET, action.getHttpRequest(defaultUserToken), String.class);
        Assert.assertEquals(HttpStatus.OK, exchange7.getStatusCode());
        Boolean read = JsonPath.read(exchange7.getBody(), "$.paymentStatus");
        Assert.assertEquals(true, read);
    }

    @Test
    public void shop_create_then_replace_an_order() {
        String defaultUserToken = action.registerResourceOwnerThenLogin();
        String profileId1 = action.getProfileId(defaultUserToken);
        OrderDetail orderDetailForUser = action.createOrderDetailForUser(defaultUserToken, profileId1);
        String preorderId = action.getOrderId(defaultUserToken, profileId1);
        String url3 = UserAction.proxyUrl + UserAction.PROFILE_SVC + "/profiles/" + profileId1 + "/orders/" + preorderId;
        ResponseEntity<String> exchange = action.restTemplate.exchange(url3, HttpMethod.POST, action.getHttpRequest(defaultUserToken, orderDetailForUser), String.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
        Assert.assertNotNull(exchange.getHeaders().getLocation().toString());

        SnapshotAddress snapshotAddress = new SnapshotAddress();
        BeanUtils.copyProperties(action.getRandomAddress(), snapshotAddress);
        orderDetailForUser.setAddress(snapshotAddress);

        String url4 = UserAction.proxyUrl + UserAction.PROFILE_SVC + "/profiles/" + profileId1 + "/orders/" + preorderId + "/replace";
        ResponseEntity<String> exchange7 = action.restTemplate.exchange(url4, HttpMethod.PUT, action.getHttpRequest(defaultUserToken, orderDetailForUser), String.class);
        Assert.assertEquals(HttpStatus.OK, exchange7.getStatusCode());
    }

    @Test
    public void shop_create_same_order_again() {
        String defaultUserToken = action.registerResourceOwnerThenLogin();
        String profileId1 = action.getProfileId(defaultUserToken);
        OrderDetail orderDetailForUser = action.createOrderDetailForUser(defaultUserToken, profileId1);
        String preorderId = action.getOrderId(defaultUserToken, profileId1);
        String url3 = UserAction.proxyUrl + UserAction.PROFILE_SVC + "/profiles/" + profileId1 + "/orders/" + preorderId;
        ResponseEntity<String> exchange = action.restTemplate.exchange(url3, HttpMethod.POST, action.getHttpRequest(defaultUserToken, orderDetailForUser), String.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
        ResponseEntity<String> exchange7 = action.restTemplate.exchange(url3, HttpMethod.POST, action.getHttpRequest(defaultUserToken, orderDetailForUser), String.class);
        Assert.assertEquals(HttpStatus.BAD_REQUEST, exchange7.getStatusCode());
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
        String preorderId = action.getOrderId(defaultUserToken, profileId1);
        String url3 = UserAction.proxyUrl + UserAction.PROFILE_SVC + "/profiles/" + profileId1 + "/orders/" + preorderId;
        ResponseEntity<String> exchange = action.restTemplate.exchange(url3, HttpMethod.POST, action.getHttpRequest(defaultUserToken, orderDetailForUser), String.class);
        ResponseEntity<OrderDetail> exchange2 = action.restTemplate.exchange(url3, HttpMethod.GET, action.getHttpRequest(defaultUserToken), OrderDetail.class);
        Assert.assertEquals(HttpStatus.OK, exchange2.getStatusCode());
        Assert.assertNotNull(exchange2.getBody());


    }

    @Test
    public void shop_place_order_but_insufficient_actual_storage() {
        //create a product with 100 order storage & 0 actual storage
        ResponseEntity<CategorySummaryCustomerRepresentation> categories = action.getCategories();
        List<CategorySummaryCardRepresentation> body = categories.getBody().getCategoryList();
        int i = new Random().nextInt(body.size());
        CategorySummaryCardRepresentation category = body.get(i);
        ProductDetail randomProduct = action.getRandomProduct(category.getTitle());
        randomProduct.setOrderStorage(100);
        randomProduct.setActualStorage(0);
        String s = null;
        try {
            s = mapper.writeValueAsString(randomProduct);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        String s1 = action.getDefaultAdminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(s1);
        HttpEntity<String> request = new HttpEntity<>(s, headers);

        String url = UserAction.proxyUrl + UserAction.PRODUCT_SVC + "/productDetails";
        ResponseEntity<String> exchange1 = action.restTemplate.exchange(url, HttpMethod.POST, request, String.class);
        randomProduct.setId(Long.parseLong(exchange1.getHeaders().get("Location").get(0)));
        // place an order for this product
        String defaultUserToken = action.registerResourceOwnerThenLogin();
        String profileId1 = action.getProfileId(defaultUserToken);
        OrderDetail orderDetailForUser = action.createBizOrderForUserAndProduct(defaultUserToken, profileId1, randomProduct);
        String preorderId = action.getOrderId(defaultUserToken, profileId1);
        String url3 = UserAction.proxyUrl + UserAction.PROFILE_SVC + "/profiles/" + profileId1 + "/orders/" + preorderId;
        ResponseEntity<String> exchange = action.restTemplate.exchange(url3, HttpMethod.POST, action.getHttpRequest(defaultUserToken, orderDetailForUser), String.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
        Assert.assertNotNull(exchange.getHeaders().getLocation().toString());
        String url4 = UserAction.proxyUrl + UserAction.PROFILE_SVC + "/profiles/" + profileId1 + "/orders/" + preorderId + "/confirm";
        ResponseEntity<String> exchange7 = action.restTemplate.exchange(url4, HttpMethod.GET, action.getHttpRequest(defaultUserToken), String.class);
        Assert.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exchange7.getStatusCode());
    }


}
