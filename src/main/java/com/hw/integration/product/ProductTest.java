package com.hw.integration.product;

import com.fasterxml.jackson.annotation.JsonInclude;
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
import java.math.MathContext;
import java.util.*;

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
    public void shop_get_products_by_catalog() {
        ResponseEntity<ProductCustomerSummaryPaginatedRepresentation> randomProducts = action.readRandomProducts();
        Assert.assertEquals(HttpStatus.OK, randomProducts.getStatusCode());
    }

    @Test
    public void shop_get_product_detail_customer() {
        ResponseEntity<ProductDetailCustomRepresentation> exchange = action.readRandomProductDetail();
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
        Assert.assertNotNull(exchange.getBody());
    }

    @Test
    public void shop_get_product_detail_admin() {
        ResponseEntity<ProductCustomerSummaryPaginatedRepresentation> randomProducts = action.readRandomProducts();
        ProductCustomerSummaryPaginatedRepresentation.ProductSearchRepresentation productSimple = randomProducts.getBody().getData().get(new Random().nextInt(randomProducts.getBody().getData().size()));
        String url = UserAction.proxyUrl + UserAction.PRODUCT_SVC + "/admin/productDetails/" + productSimple.getId();
        String s1 = action.getDefaultAdminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(s1);
        HttpEntity<String> request = new HttpEntity<>(null, headers);
        ResponseEntity<ProductDetail> exchange = action.restTemplate.exchange(url, HttpMethod.GET, request, ProductDetail.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
    }

    @Test
    public void shop_read_all_products() {
        String s1 = action.getDefaultAdminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(s1);
        HttpEntity<String> request = new HttpEntity<>(null, headers);
        String url = UserAction.proxyUrl + UserAction.PRODUCT_SVC + "/admin/productDetails" + "?pageNum=0&pageSize=20&sortBy=price&sortOrder=asc";
        ResponseEntity<ProductTotalResponse> exchange = action.restTemplate.exchange(url, HttpMethod.GET, request, ProductTotalResponse.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
        Assert.assertNotEquals(0L, exchange.getBody().getTotalProductCount().longValue());
    }

    @Test
    public void shop_create_product() {
        ResponseEntity<String> productDetailForCatalog = action.createRandomProductDetail(null);
        Assert.assertEquals(HttpStatus.OK, productDetailForCatalog.getStatusCode());
        Assert.assertNotEquals(0, productDetailForCatalog.getHeaders().get("Location"));
    }

    @Test
    public void shop_update_product() {
        ResponseEntity<String> exchange = action.createRandomProductDetail(null);
        String s1 = action.getDefaultAdminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(s1);
        UpdateProductAdminCommand command = new UpdateProductAdminCommand();
        UpdateProductAdminSkuCommand productSku = new UpdateProductAdminSkuCommand();
        productSku.setPrice(BigDecimal.valueOf(new Random().nextDouble()).abs());
        productSku.setAttributesSales(new HashSet<>(List.of("test:testValue")));
        command.setDescription(action.getRandomStr());
        command.setSkus(new ArrayList<>(List.of(productSku)));
        command.setStatus(ProductStatus.UNAVAILABLE);
        String url2 = UserAction.proxyUrl + UserAction.PRODUCT_SVC + "/admin/productDetails/" + exchange.getHeaders().getLocation().toString();
        HttpEntity<UpdateProductAdminCommand> request2 = new HttpEntity<>(command, headers);
        ResponseEntity<String> exchange2 = action.restTemplate.exchange(url2, HttpMethod.PUT, request2, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange2.getStatusCode());
    }

    @Test
    public void shop_update_product_w_wrong_field() {
        ResponseEntity<String> exchange = action.createRandomProductDetail(null);
        String s1 = action.getDefaultAdminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(s1);
        UpdateProductAdminCommand command = new UpdateProductAdminCommand();
        UpdateProductAdminSkuCommand productSku = new UpdateProductAdminSkuCommand();
        productSku.setPrice(BigDecimal.valueOf(new Random().nextDouble()).abs());
        productSku.setAttributesSales(new HashSet<>(List.of("test:testValue")));
        int i = new Random().nextInt(1000);
        productSku.setStorageOrder(i);
        productSku.setStorageActual(i + new Random().nextInt(1000));
        command.setDescription(action.getRandomStr());
        command.setSkus(new ArrayList<>(List.of(productSku)));
        command.setStatus(ProductStatus.UNAVAILABLE);
        String url2 = UserAction.proxyUrl + UserAction.PRODUCT_SVC + "/admin/productDetails/" + exchange.getHeaders().getLocation().toString();
        HttpEntity<UpdateProductAdminCommand> request2 = new HttpEntity<>(command, headers);
        ResponseEntity<String> exchange2 = action.restTemplate.exchange(url2, HttpMethod.PUT, request2, String.class);
        Assert.assertEquals(HttpStatus.BAD_REQUEST, exchange2.getStatusCode());
    }

    @Test
    public void shop_update_product_storage() {
        ResponseEntity<String> exchange = action.createRandomProductDetail(null);
        String s1 = action.getDefaultAdminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(s1);
        UpdateProductAdminCommand command = new UpdateProductAdminCommand();
        UpdateProductAdminSkuCommand productSku = new UpdateProductAdminSkuCommand();
        productSku.setPrice(BigDecimal.valueOf(new Random().nextDouble()).abs());
        productSku.setAttributesSales(new HashSet<>(List.of("test:testValue")));
        int i = new Random().nextInt(1000);
        productSku.setIncreaseActualStorage(i);
        productSku.setIncreaseOrderStorage(i + new Random().nextInt(1000));
        command.setDescription(action.getRandomStr());
        command.setSkus(new ArrayList<>(List.of(productSku)));
        command.setStatus(ProductStatus.UNAVAILABLE);
        String url2 = UserAction.proxyUrl + UserAction.PRODUCT_SVC + "/admin/productDetails/" + exchange.getHeaders().getLocation().toString();
        HttpEntity<UpdateProductAdminCommand> request2 = new HttpEntity<>(command, headers);
        ResponseEntity<String> exchange2 = action.restTemplate.exchange(url2, HttpMethod.PUT, request2, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange2.getStatusCode());
    }

    @Test
    public void shop_update_product_price() {
        ResponseEntity<String> exchange = action.createRandomProductDetail(null);
        String s1 = action.getDefaultAdminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(s1);
        UpdateProductAdminCommand command = new UpdateProductAdminCommand();
        UpdateProductAdminSkuCommand productSku = new UpdateProductAdminSkuCommand();
        BigDecimal abs = BigDecimal.valueOf(new Random().nextDouble()).abs();
        productSku.setPrice(abs);
        productSku.setAttributesSales(new HashSet<>(List.of("test:testValue")));
        command.setDescription(action.getRandomStr());
        command.setSkus(new ArrayList<>(List.of(productSku)));
        command.setStatus(ProductStatus.AVAILABLE);
        String id = exchange.getHeaders().getLocation().toString();
        String url2 = UserAction.proxyUrl + UserAction.PRODUCT_SVC + "/admin/productDetails/" + id;
        HttpEntity<UpdateProductAdminCommand> request2 = new HttpEntity<>(command, headers);
        action.restTemplate.exchange(url2, HttpMethod.PUT, request2, String.class);
        ResponseEntity<ProductDetailCustomRepresentation> productDetailCustomRepresentationResponseEntity = action.readProductDetailById(Long.parseLong(id));
        List<ProductSkuCustomerRepresentation> skus = productDetailCustomRepresentationResponseEntity.getBody().getSkus();
        ProductSkuCustomerRepresentation productSkuCustomerRepresentation = skus.get(0);
        Assert.assertEquals(abs.round(new MathContext(2)), productSkuCustomerRepresentation.getPrice());
    }


    @Test
    public void shop_delete_product() {
        ResponseEntity<String> productDetailForCatalog = action.createRandomProductDetail(null);
        String s1 = action.getDefaultAdminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(s1);
        String url2 = UserAction.proxyUrl + UserAction.PRODUCT_SVC + "/admin/productDetails/" + productDetailForCatalog.getHeaders().getLocation().toString();
        HttpEntity<String> request2 = new HttpEntity<>(headers);
        ResponseEntity<String> exchange2 = action.restTemplate.exchange(url2, HttpMethod.DELETE, request2, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange2.getStatusCode());
    }
}
