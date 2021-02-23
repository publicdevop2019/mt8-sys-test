package com.hw.integration.mall;

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

import static com.hw.helper.UserAction.*;
import static com.hw.integration.mall.ProductConcurrentTest.URL_2;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
public class ProductTest {
    public static final String PRODUCTS_ADMIN = "/products/admin";
    public static final String PRODUCTS_PUBLIC = "/products/public";
    public static final String PRODUCTS_CHANGE_APP = "/products/change/app";
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
        ResponseEntity<ProductCustomerSummaryPaginatedRepresentation> randomProducts = action.readProductsByQuery();
        Assert.assertEquals(HttpStatus.OK, randomProducts.getStatusCode());
    }

    @Test
    public void shop_get_product_detail_customer() {
        ResponseEntity<String> productDetailForCatalog = action.createRandomProductDetail(null);
        Assert.assertEquals(HttpStatus.OK, productDetailForCatalog.getStatusCode());
        Assert.assertNotEquals(null, productDetailForCatalog.getHeaders().get("Location"));
        ResponseEntity<ProductDetailCustomRepresentation> exchange = action.readRandomProductDetail();
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
        Assert.assertNotNull(exchange.getBody());
    }

    @Test
    public void shop_get_product_detail_admin() {
        ResponseEntity<ProductCustomerSummaryPaginatedRepresentation> randomProducts = action.readProductsByQuery();
        ProductCustomerSummaryPaginatedRepresentation.ProductSearchRepresentation productSimple = randomProducts.getBody().getData().get(new Random().nextInt(randomProducts.getBody().getData().size()));
        String url = UserAction.proxyUrl + UserAction.SVC_NAME_PRODUCT + PRODUCTS_ADMIN + "/" + productSimple.getId();
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
        String url = UserAction.proxyUrl + UserAction.SVC_NAME_PRODUCT + PRODUCTS_ADMIN + "?page=num:0,size:20";
        ResponseEntity<ProductTotalResponse> exchange = action.restTemplate.exchange(url, HttpMethod.GET, request, ProductTotalResponse.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
        Assert.assertNotEquals(0L, exchange.getBody().getTotalItemCount().longValue());
    }

    @Test
    public void shop_create_product() {
        ResponseEntity<String> productDetailForCatalog = action.createRandomProductDetail(null);
        Assert.assertEquals(HttpStatus.OK, productDetailForCatalog.getStatusCode());
        Assert.assertNotEquals(null, productDetailForCatalog.getHeaders().get("Location"));
    }

    @Test
    public void shop_update_product() {
        ResponseEntity<String> exchange = action.createRandomProductDetail(null);
        String s1 = action.getDefaultAdminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(s1);
        UpdateProductAdminCommand command = new UpdateProductAdminCommand();
        command.setVersion(0);
        UpdateProductAdminSkuCommand productSku = new UpdateProductAdminSkuCommand();
        productSku.setVersion(0);
        productSku.setPrice(BigDecimal.valueOf(new Random().nextDouble()).abs());
        productSku.setAttributesSales(new HashSet<>(List.of(TEST_TEST_VALUE)));
        command.setDescription(action.getRandomStr());
        command.setSkus(new ArrayList<>(List.of(productSku)));
        command.setStatus(ProductStatus.UNAVAILABLE);
        command.setName(action.getRandomStr());
        command.setImageUrlSmall("http://www.test.com/" + action.getRandomStr());
        Set<String> strings = new HashSet<>();
        strings.add(TEST_TEST_VALUE);
        command.setAttributesKey(strings);
        String url2 = UserAction.proxyUrl + UserAction.SVC_NAME_PRODUCT + PRODUCTS_ADMIN + "/" + exchange.getHeaders().getLocation().toString();
        HttpEntity<UpdateProductAdminCommand> request2 = new HttpEntity<>(command, headers);
        ResponseEntity<String> exchange2 = action.restTemplate.exchange(url2, HttpMethod.PUT, request2, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange2.getStatusCode());
    }

    @Test
    public void shop_add_product_attr_key_then_remove() {
        ResponseEntity<String> exchange = action.createRandomProductDetail(null);
        String s1 = action.getDefaultAdminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(s1);
        UpdateProductAdminCommand command = new UpdateProductAdminCommand();
        command.setVersion(0);
        UpdateProductAdminSkuCommand productSku = new UpdateProductAdminSkuCommand();
        productSku.setVersion(0);
        productSku.setPrice(BigDecimal.valueOf(new Random().nextDouble()).abs());
        productSku.setAttributesSales(new HashSet<>(List.of(TEST_TEST_VALUE)));
        command.setDescription(action.getRandomStr());
        command.setSkus(new ArrayList<>(List.of(productSku)));
        command.setStatus(ProductStatus.UNAVAILABLE);
        command.setName(action.getRandomStr());
        command.setImageUrlSmall("http://www.test.com/" + action.getRandomStr());
        Set<String> strings = new HashSet<>();
        strings.add(TEST_TEST_VALUE);
        strings.add(TEST_TEST_VALUE_2);
        command.setAttributesKey(strings);
        String url2 = UserAction.proxyUrl + UserAction.SVC_NAME_PRODUCT + PRODUCTS_ADMIN + "/" + exchange.getHeaders().getLocation().toString();
        HttpEntity<UpdateProductAdminCommand> request2 = new HttpEntity<>(command, headers);
        ResponseEntity<String> exchange2 = action.restTemplate.exchange(url2, HttpMethod.PUT, request2, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange2.getStatusCode());

        HttpEntity<UpdateProductAdminCommand> request3 = new HttpEntity<>(null, headers);
        ResponseEntity<ProductDetailAdminRepresentation> exchange3 = action.restTemplate.exchange(url2, HttpMethod.GET, request3, ProductDetailAdminRepresentation.class);

        Assert.assertEquals(HttpStatus.OK, exchange3.getStatusCode());
        Assert.assertEquals(2, exchange3.getBody().getAttributesKey().size());
        Assert.assertEquals(TEST_TEST_VALUE, exchange3.getBody().getAttributesKey().toArray()[0]);
        Assert.assertTrue(exchange3.getBody().getAttributesKey().contains(TEST_TEST_VALUE_2));
        //remove tag
        Set<String> strings2 = new HashSet<>();
        strings2.add(TEST_TEST_VALUE_2);
        command.setAttributesKey(strings2);
        HttpEntity<UpdateProductAdminCommand> request4 = new HttpEntity<>(command, headers);
        ResponseEntity<String> exchange4 = action.restTemplate.exchange(url2, HttpMethod.PUT, request4, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange4.getStatusCode());
        HttpEntity<UpdateProductAdminCommand> request5 = new HttpEntity<>(null, headers);
        ResponseEntity<ProductDetailAdminRepresentation> exchange5 = action.restTemplate.exchange(url2, HttpMethod.GET, request5, ProductDetailAdminRepresentation.class);
        Assert.assertEquals(HttpStatus.OK, exchange5.getStatusCode());
        Assert.assertEquals(1, exchange5.getBody().getAttributesKey().size());
        Assert.assertEquals(TEST_TEST_VALUE_2, exchange3.getBody().getAttributesKey().toArray()[0]);
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
        productSku.setAttributesSales(new HashSet<>(List.of(TEST_TEST_VALUE)));
        int i = new Random().nextInt(1000);
        productSku.setStorageOrder(i);
        productSku.setStorageActual(i + new Random().nextInt(1000));
        command.setDescription(action.getRandomStr());
        command.setSkus(new ArrayList<>(List.of(productSku)));
        command.setStatus(ProductStatus.UNAVAILABLE);
        String url2 = UserAction.proxyUrl + UserAction.SVC_NAME_PRODUCT + PRODUCTS_ADMIN + "/" + exchange.getHeaders().getLocation().toString();
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
        productSku.setAttributesSales(new HashSet<>(List.of(TEST_TEST_VALUE)));
        int i = new Random().nextInt(1000);
        productSku.setIncreaseActualStorage(i);
        productSku.setIncreaseOrderStorage(i + new Random().nextInt(1000));
        productSku.setVersion(0);
        command.setDescription(action.getRandomStr());
        command.setSkus(new ArrayList<>(List.of(productSku)));
        command.setStatus(ProductStatus.UNAVAILABLE);
        command.setName(action.getRandomStr());
        command.setImageUrlSmall("http://www.test.com/" + action.getRandomStr());
        Set<String> strings = new HashSet<>();
        strings.add(TEST_TEST_VALUE);
        command.setAttributesKey(strings);
        command.setVersion(0);
        String url2 = UserAction.proxyUrl + UserAction.SVC_NAME_PRODUCT + PRODUCTS_ADMIN + "/" + exchange.getHeaders().getLocation().toString();
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
        command.setVersion(0);
        UpdateProductAdminSkuCommand productSku = new UpdateProductAdminSkuCommand();
        productSku.setVersion(0);
        BigDecimal abs = BigDecimal.valueOf(new Random().nextDouble()).abs();
        productSku.setPrice(abs);
        productSku.setAttributesSales(new HashSet<>(List.of(TEST_TEST_VALUE)));
        command.setDescription(action.getRandomStr());
        command.setSkus(new ArrayList<>(List.of(productSku)));
        command.setStartAt(new Date().getTime());
        command.setName(action.getRandomStr());
        command.setImageUrlSmall("http://www.test.com/" + action.getRandomStr());
        Set<String> strings = new HashSet<>();
        strings.add(TEST_TEST_VALUE);
        command.setAttributesKey(strings);
        String id = exchange.getHeaders().getLocation().toString();
        String url2 = UserAction.proxyUrl + UserAction.SVC_NAME_PRODUCT + PRODUCTS_ADMIN + "/" + id;
        HttpEntity<UpdateProductAdminCommand> request2 = new HttpEntity<>(command, headers);
        action.restTemplate.exchange(url2, HttpMethod.PUT, request2, String.class);
        ResponseEntity<ProductDetailCustomRepresentation> productDetailCustomRepresentationResponseEntity = action.readProductDetailById(id);
        List<ProductSkuCustomerRepresentation> skus = productDetailCustomRepresentationResponseEntity.getBody().getSkus();
        ProductSkuCustomerRepresentation productSkuCustomerRepresentation = skus.get(0);
        Assert.assertEquals(abs.round(new MathContext(2)).doubleValue(), productSkuCustomerRepresentation.getPrice().doubleValue(), 0d);
    }


    @Test
    public void shop_delete_product() {
        ResponseEntity<String> productDetailForCatalog = action.createRandomProductDetail(null);
        String s1 = action.getDefaultAdminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(s1);
        String url2 = UserAction.proxyUrl + UserAction.SVC_NAME_PRODUCT + PRODUCTS_ADMIN + "/" + productDetailForCatalog.getHeaders().getLocation().toString();
        HttpEntity<String> request2 = new HttpEntity<>(headers);
        ResponseEntity<String> exchange2 = action.restTemplate.exchange(url2, HttpMethod.DELETE, request2, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange2.getStatusCode());
        ResponseEntity<String> exchange3 = action.restTemplate.exchange(url2, HttpMethod.GET, request2, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange3.getStatusCode());
        Assert.assertNull(exchange3.getBody());
    }

    @Test
    public void shop_delete_product_by_query_id() {
        ResponseEntity<String> productDetailForCatalog = action.createRandomProductDetail(null);
        String p1Id = productDetailForCatalog.getHeaders().getLocation().toString();
        ResponseEntity<String> productDetailForCatalog2 = action.createRandomProductDetail(null);
        String p2Id = productDetailForCatalog2.getHeaders().getLocation().toString();
        String query = "id:" + p1Id + "." + p2Id;
        String s1 = action.getDefaultAdminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(s1);
        String url2 = UserAction.proxyUrl + UserAction.SVC_NAME_PRODUCT + PRODUCTS_ADMIN + "/" + query;
        String url3 = UserAction.proxyUrl + UserAction.SVC_NAME_PRODUCT + PRODUCTS_ADMIN + "/";
        HttpEntity<String> request2 = new HttpEntity<>(headers);
        ResponseEntity<String> exchange2 = action.restTemplate.exchange(url2, HttpMethod.DELETE, request2, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange2.getStatusCode());
        ResponseEntity<String> exchange3 = action.restTemplate.exchange(url3 + p1Id, HttpMethod.GET, request2, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange3.getStatusCode());
        Assert.assertNull(exchange3.getBody());
        ResponseEntity<String> exchange4 = action.restTemplate.exchange(url3 + p2Id, HttpMethod.GET, request2, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange4.getStatusCode());
        Assert.assertNull(exchange4.getBody());
    }

    @Test
    public void shop_delete_product_by_query_name() {
        String url = UserAction.proxyUrl + UserAction.SVC_NAME_PRODUCT + PRODUCTS_ADMIN + "/";
        String s1 = action.getDefaultAdminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(s1);
        HttpEntity<String> request2 = new HttpEntity<>(headers);
        ResponseEntity<String> productDetailForCatalog = action.createRandomProductDetail(null);
        String p1Id = productDetailForCatalog.getHeaders().getLocation().toString();
        ResponseEntity<String> productDetailForCatalog2 = action.createRandomProductDetail(null);
        String p2Id = productDetailForCatalog2.getHeaders().getLocation().toString();
        ResponseEntity<ProductDetail> exchange4 = action.restTemplate.exchange(url + p1Id, HttpMethod.GET, request2, ProductDetail.class);
        String name1 = exchange4.getBody().getName();
        ResponseEntity<ProductDetail> exchange5 = action.restTemplate.exchange(url + p2Id, HttpMethod.GET, request2, ProductDetail.class);
        String name2 = exchange5.getBody().getName();

        ResponseEntity<String> exchange2 = action.restTemplate.exchange(url + "name:" + name1 + "." + name2, HttpMethod.DELETE, request2, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange2.getStatusCode());

        ResponseEntity<String> exchange6 = action.restTemplate.exchange(url + p1Id, HttpMethod.GET, request2, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange6.getStatusCode());
        Assert.assertNull(exchange6.getBody());
        ResponseEntity<String> exchange7 = action.restTemplate.exchange(url + p2Id, HttpMethod.GET, request2, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange7.getStatusCode());
        Assert.assertNull(exchange7.getBody());
    }

    @Test
    public void shop_customer_should_not_query_all() {
        String url = UserAction.proxyUrl + UserAction.SVC_NAME_PRODUCT + PRODUCTS_PUBLIC;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(null, headers);
        ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.GET, request, String.class);
        Assert.assertEquals(HttpStatus.BAD_REQUEST, exchange.getStatusCode());
    }

    @Test
    public void change_product_sku_then_rollback() {
        String url = UserAction.proxyUrl + UserAction.SVC_NAME_PRODUCT + PRODUCTS_CHANGE_APP;
        ResponseEntity<String> exchange = action.createRandomProductDetail(null, 1000);
        String productId = exchange.getHeaders().getLocation().toString();
        PatchCommand patchCommand = new PatchCommand();
        patchCommand.setOp("diff");
        patchCommand.setExpect(1);
        patchCommand.setValue(1);
        patchCommand.setPath("/" + productId + "/skus?query=" + "attributesSales:" + TEST_TEST_VALUE.replace(":", "-") + "/storageOrder");
        ArrayList<PatchCommand> patchCommands = new ArrayList<>();
        patchCommands.add(patchCommand);
        HttpHeaders headers2 = new HttpHeaders();
        String changeId = UUID.randomUUID().toString();
        headers2.set("changeId", changeId);
        headers2.setBearerAuth(action.getJwtClientCredential(CLIENT_ID_SAGA_ID, COMMON_CLIENT_SECRET).getBody().getValue());
        HttpEntity<ArrayList<PatchCommand>> listHttpEntity = new HttpEntity<>(patchCommands, headers2);
        ResponseEntity<Object> exchange2 = action.restTemplate.exchange(URL_2, HttpMethod.PATCH, listHttpEntity, Object.class);
        Assert.assertEquals(200, exchange2.getStatusCode().value());
        ResponseEntity<ProductDetailAdminRepresentation> productDetailByIdAdmin = action.readProductDetailByIdAdmin(productId);
        Assert.assertEquals(999, productDetailByIdAdmin.getBody().getSkus().get(0).getStorageOrder().intValue());

        //rollback change
        ResponseEntity<Void> exchange3 = action.restTemplate.exchange(url + "/" + changeId, HttpMethod.DELETE, listHttpEntity, Void.class);
        Assert.assertEquals(200, exchange3.getStatusCode().value());
        ResponseEntity<ProductDetailAdminRepresentation> productDetailByIdAdmin2 = action.readProductDetailByIdAdmin(productId);
        Assert.assertEquals(1000, productDetailByIdAdmin.getBody().getSkus().get(0).getStorageOrder().intValue());

    }


}
