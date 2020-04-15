package com.hw.integration.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hw.helper.*;
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
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
public class ProductTest {
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
    public void shop_get_products_by_category() {
        ResponseEntity<List<ProductSimple>> randomProducts = action.getRandomProducts();
        Assert.assertEquals(HttpStatus.OK, randomProducts.getStatusCode());
    }

    @Test
    public void shop_get_product_detail() {
        ResponseEntity<List<ProductSimple>> randomProducts = action.getRandomProducts();
        ProductSimple productSimple = randomProducts.getBody().get(new Random().nextInt(randomProducts.getBody().size()));
        String url = UserAction.proxyUrl + UserAction.PRODUCT_SVC + "/productDetails/" + productSimple.getId();
        ResponseEntity<ProductDetail> exchange = action.restTemplate.exchange(url, HttpMethod.GET, null, ProductDetail.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
    }

    @Test
    public void shop_read_all_products() {
        String url = UserAction.proxyUrl + UserAction.PRODUCT_SVC + "/categories/all" + "?pageNum=0&pageSize=20&sortBy=price&sortOrder=asc";
        ResponseEntity<ProductTotalResponse> exchange = action.restTemplate.exchange(url, HttpMethod.GET, null, ProductTotalResponse.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
        Assert.assertNotEquals(0L, exchange.getBody().getTotalProductCount().longValue());
    }

    @Test
    public void shop_create_product() {
        ResponseEntity<List<Category>> categories = action.getCategories();
        List<Category> body = categories.getBody();
        int i = new Random().nextInt(body.size());
        Category category = body.get(i);
        ProductDetail randomProduct = action.getRandomProduct(category.getTitle());

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
        ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.POST, request, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
        Assert.assertNotEquals(0, exchange.getHeaders().get("Location"));
    }

    @Test
    public void shop_update_product() {
        ResponseEntity<List<Category>> categories = action.getCategories();
        List<Category> body = categories.getBody();
        int i = new Random().nextInt(body.size());
        Category category = body.get(i);
        ProductDetail randomProduct = action.getRandomProduct(category.getTitle());

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
        ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.POST, request, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
        Assert.assertNotEquals(0, exchange.getHeaders().getLocation().toString());
        // update price
        randomProduct.setPrice(BigDecimal.valueOf(new Random().nextDouble()));
        randomProduct.setActualStorage(null);
        randomProduct.setOrderStorage(null);
        String url2 = UserAction.proxyUrl + UserAction.PRODUCT_SVC + "/productDetails/" + exchange.getHeaders().getLocation().toString();
        String s2 = null;
        try {
            s2 = mapper.writeValueAsString(randomProduct);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        HttpEntity<String> request2 = new HttpEntity<>(s2, headers);
        ResponseEntity<String> exchange2 = action.restTemplate.exchange(url2, HttpMethod.PUT, request2, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange2.getStatusCode());
    }

    @Test
    public void shop_update_product_w_wrong_field() {
        ResponseEntity<List<Category>> categories = action.getCategories();
        List<Category> body = categories.getBody();
        int i = new Random().nextInt(body.size());
        Category category = body.get(i);
        ProductDetail randomProduct = action.getRandomProduct(category.getTitle());

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
        ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.POST, request, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
        Assert.assertNotEquals(0, exchange.getHeaders().getLocation().toString());
        // update price
        Integer orderStorage = randomProduct.getOrderStorage();
        randomProduct.setOrderStorage(randomProduct.getOrderStorage() + 100);
        String url2 = UserAction.proxyUrl + UserAction.PRODUCT_SVC + "/productDetails/" + exchange.getHeaders().getLocation().toString();
        String s2 = null;
        try {
            s2 = mapper.writeValueAsString(randomProduct);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        HttpEntity<String> request2 = new HttpEntity<>(s2, headers);
        ResponseEntity<String> exchange2 = action.restTemplate.exchange(url2, HttpMethod.PUT, request2, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange2.getStatusCode());

        String url3 = UserAction.proxyUrl + UserAction.PRODUCT_SVC + "/productDetails/" + exchange.getHeaders().getLocation().toString();
        ResponseEntity<ProductDetail> exchange3 = action.restTemplate.exchange(url3, HttpMethod.GET, null, ProductDetail.class);
        Assert.assertEquals(HttpStatus.OK, exchange3.getStatusCode());
        Assert.assertEquals(orderStorage, exchange3.getBody().getOrderStorage());
    }

    @Test
    public void shop_update_product_storage() {
        ResponseEntity<List<Category>> categories = action.getCategories();
        List<Category> body = categories.getBody();
        int i = new Random().nextInt(body.size());
        Category category = body.get(i);
        ProductDetail randomProduct = action.getRandomProduct(category.getTitle());

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
        ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.POST, request, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
        Assert.assertNotEquals(0, exchange.getHeaders().getLocation().toString());
        // update price
        randomProduct.setIncreaseActualStorageBy(new Random().nextInt());
        randomProduct.setActualStorage(null);
        randomProduct.setOrderStorage(null);
        String url2 = UserAction.proxyUrl + UserAction.PRODUCT_SVC + "/productDetails/" + exchange.getHeaders().getLocation().toString();
        String s2 = null;
        try {
            s2 = mapper.writeValueAsString(randomProduct);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        HttpEntity<String> request2 = new HttpEntity<>(s2, headers);
        ResponseEntity<String> exchange2 = action.restTemplate.exchange(url2, HttpMethod.PUT, request2, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange2.getStatusCode());
    }


    @Test
    public void shop_delete_product() {
        ResponseEntity<List<Category>> categories = action.getCategories();
        List<Category> body = categories.getBody();
        int i = new Random().nextInt(body.size());
        Category category = body.get(i);
        ProductDetail randomProduct = action.getRandomProduct(category.getTitle());

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
        ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.POST, request, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
        Assert.assertNotEquals(0, exchange.getHeaders().getLocation().toString());
        String url2 = UserAction.proxyUrl + UserAction.PRODUCT_SVC + "/productDetails/" + exchange.getHeaders().getLocation().toString();
        HttpEntity<String> request2 = new HttpEntity<>(headers);
        ResponseEntity<String> exchange2 = action.restTemplate.exchange(url2, HttpMethod.DELETE, request2, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange2.getStatusCode());
    }
}
