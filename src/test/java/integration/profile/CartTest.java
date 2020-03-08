package integration.profile;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import helper.*;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Random;

public class CartTest {
    private int randomServerPort = 8082;
    private int randomServerPortProduct = 8083;
    private TestRestTemplate restTemplate = new TestRestTemplate();
    UserAction action = new UserAction();
    public ObjectMapper mapper = new ObjectMapper().configure(MapperFeature.USE_ANNOTATIONS, false).setSerializationInclusion(JsonInclude.Include.NON_NULL);

    @Test
    public void shop_add_product_cart() {
        String defaultUserToken = action.registerResourceOwnerThenLogin();
        String profileId1 = action.getProfileId(defaultUserToken);
        ResponseEntity<List<ProductSimple>> randomProducts = action.getRandomProducts();
        ProductSimple productSimple = randomProducts.getBody().get(new Random().nextInt(randomProducts.getBody().size()));
        String url = "http://localhost:" + randomServerPortProduct + "/v1/api/" + "productDetails/" + productSimple.getId();
        ResponseEntity<ProductDetail> exchange = restTemplate.exchange(url, HttpMethod.GET, null, ProductDetail.class);
        ProductDetail body = exchange.getBody();
        SnapshotProduct snapshotProduct = action.selectProduct(body);
        String url2 = "http://localhost:" + randomServerPort + "/v1/api/profiles/" + profileId1 + "/cart";
        ResponseEntity<String> exchange3 = restTemplate.exchange(url2, HttpMethod.POST, action.getHttpRequest(defaultUserToken, snapshotProduct), String.class);
        Assert.assertEquals(HttpStatus.OK, exchange3.getStatusCode());
        Assert.assertNotEquals(-1, exchange3.getHeaders().getLocation().toString());
    }

    @Test
    public void shop_add_same_product_cart_multiple_times() {
        String defaultUserToken = action.registerResourceOwnerThenLogin();
        String profileId1 = action.getProfileId(defaultUserToken);
        ResponseEntity<List<ProductSimple>> randomProducts = action.getRandomProducts();
        ProductSimple productSimple = randomProducts.getBody().get(new Random().nextInt(randomProducts.getBody().size() ));
        String url = "http://localhost:" + randomServerPortProduct + "/v1/api/" + "productDetails/" + productSimple.getId();
        ResponseEntity<ProductDetail> exchange = restTemplate.exchange(url, HttpMethod.GET, null, ProductDetail.class);
        ProductDetail body = exchange.getBody();
        SnapshotProduct snapshotProduct = action.selectProduct(body);
        String url2 = "http://localhost:" + randomServerPort + "/v1/api/profiles/" + profileId1 + "/cart";
        ResponseEntity<String> exchange3 = restTemplate.exchange(url2, HttpMethod.POST, action.getHttpRequest(defaultUserToken, snapshotProduct), String.class);
        Assert.assertEquals(HttpStatus.OK, exchange3.getStatusCode());
        Assert.assertNotEquals(-1, exchange3.getHeaders().getLocation().toString());
        ResponseEntity<String> exchange4 = restTemplate.exchange(url2, HttpMethod.POST, action.getHttpRequest(defaultUserToken, snapshotProduct), String.class);
        Assert.assertEquals(HttpStatus.OK, exchange4.getStatusCode());
        Assert.assertNotEquals(-1, exchange4.getHeaders().getLocation().toString());
    }

    @Test
    public void shop_read_all_carts() {
        String defaultUserToken = action.registerResourceOwnerThenLogin();
        String profileId1 = action.getProfileId(defaultUserToken);
        String url = "http://localhost:" + randomServerPort + "/v1/api/profiles/" + profileId1 + "/cart";
        ParameterizedTypeReference<List<SnapshotProduct>> responseType = new ParameterizedTypeReference<>() {
        };
        ResponseEntity<List<SnapshotProduct>> exchange = restTemplate.exchange(url, HttpMethod.GET, action.getHttpRequest(defaultUserToken), responseType);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
        Assert.assertNotEquals(-1, exchange.getBody().size());
    }


    @Test
    public void shop_delete_cart_item_by_id() {
        String defaultUserToken = action.registerResourceOwnerThenLogin();
        String profileId1 = action.getProfileId(defaultUserToken);
        ResponseEntity<List<ProductSimple>> randomProducts = action.getRandomProducts();
        ProductSimple productSimple = randomProducts.getBody().get(new Random().nextInt(randomProducts.getBody().size()));
        String url = "http://localhost:" + randomServerPortProduct + "/v1/api/" + "productDetails/" + productSimple.getId();
        ResponseEntity<ProductDetail> exchange = restTemplate.exchange(url, HttpMethod.GET, null, ProductDetail.class);
        ProductDetail body = exchange.getBody();
        SnapshotProduct snapshotProduct = action.selectProduct(body);
        String url2 = "http://localhost:" + randomServerPort + "/v1/api/profiles/" + profileId1 + "/cart";
        ResponseEntity<String> exchange3 = restTemplate.exchange(url2, HttpMethod.POST, action.getHttpRequest(defaultUserToken, snapshotProduct), String.class);
        String s = exchange3.getHeaders().getLocation().toString();
        ResponseEntity<String> exchange4 = restTemplate.exchange(url2 + "/" + s, HttpMethod.DELETE, action.getHttpRequest(defaultUserToken), String.class);
        Assert.assertEquals(HttpStatus.OK, exchange4.getStatusCode());
    }

//    @Test
//    public void shop_read_carts_details() {
//    }
//
//    @Test
//    public void shop_update_cart_details() {
//
//    }

}
