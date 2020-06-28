package com.hw.integration.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hw.helper.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.*;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
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
        ProductSku productSku = new ProductSku();
        productSku.setPrice(BigDecimal.valueOf(new Random().nextDouble()).abs());
        productSku.setAttributesSales(new HashSet<>(List.of("test:testValue")));
        int i = new Random().nextInt(1000);
        productSku.setStorageOrder(i);
        productSku.setStorageActual(i + new Random().nextInt(1000));
        command.setDescription(action.getRandomStr());
        command.setSkus(new ArrayList<>(List.of(productSku)));
        String url2 = UserAction.proxyUrl + UserAction.PRODUCT_SVC + "/admin/productDetails/" + exchange.getHeaders().getLocation().toString();
        HttpEntity<UpdateProductAdminCommand> request2 = new HttpEntity<>(command, headers);
        ResponseEntity<String> exchange2 = action.restTemplate.exchange(url2, HttpMethod.PUT, request2, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange2.getStatusCode());
    }

    @Test
    @Ignore("wait until update storage complete")
    public void shop_update_product_w_wrong_field() {
//        CategorySummaryCardRepresentation catalogFromList = action.getCatalogFromList();
//        ResponseEntity<String> exchange = action.createProductDetailForCatalog(catalogFromList);
//        String s1 = action.getDefaultAdminToken();
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.setBearerAuth(s1);
//        // update price
//        String url2 = UserAction.proxyUrl + UserAction.PRODUCT_SVC + "/admin/productDetails/" + exchange.getHeaders().getLocation().toString();
//        String s2 = null;
//        try {
//            s2 = mapper.writeValueAsString(randomProduct);
//        } catch (JsonProcessingException e) {
//            e.printStackTrace();
//        }
//        HttpEntity<String> request2 = new HttpEntity<>(s2, headers);
//        ResponseEntity<String> exchange2 = action.restTemplate.exchange(url2, HttpMethod.PUT, request2, String.class);
//        Assert.assertEquals(HttpStatus.OK, exchange2.getStatusCode());
//
//        String url3 = UserAction.proxyUrl + UserAction.PRODUCT_SVC + "/public/productDetails/" + exchange.getHeaders().getLocation().toString();
//        ResponseEntity<ProductDetail> exchange3 = action.restTemplate.exchange(url3, HttpMethod.GET, null, ProductDetail.class);
//        Assert.assertEquals(HttpStatus.OK, exchange3.getStatusCode());
//        Assert.assertEquals(orderStorage, exchange3.getBody().getOrderStorage());
    }

    @Test
    @Ignore("wait until update storage complete")
    public void shop_update_product_storage() {
        ResponseEntity<CategorySummaryCustomerRepresentation> categories = action.getCatalogs();
        List<CategorySummaryCardRepresentation> body = categories.getBody().getData();
        int i = new Random().nextInt(body.size());
        CategorySummaryCardRepresentation category = body.get(i);
        ProductDetail randomProduct = action.getRandomProduct(category,null);

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

        String url = UserAction.proxyUrl + UserAction.PRODUCT_SVC + "/admin/productDetails";
        ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.POST, request, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
        Assert.assertNotEquals(0, exchange.getHeaders().getLocation().toString());
        // update price
//        randomProduct.setIncreaseActualStorageBy(new Random().nextInt(3000));
//        randomProduct.setActualStorage(null);
//        randomProduct.setOrderStorage(null);
        String url2 = UserAction.proxyUrl + UserAction.PRODUCT_SVC + "/admin/productDetails/" + exchange.getHeaders().getLocation().toString();
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
