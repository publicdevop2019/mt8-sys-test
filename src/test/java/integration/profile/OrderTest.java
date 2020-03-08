package integration.profile;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import helper.OrderDetail;
import helper.UserAction;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

public class OrderTest {
    private int randomServerPort = 8082;
    private int randomServerPortProduct = 8083;
    private TestRestTemplate restTemplate = new TestRestTemplate();
    UserAction action = new UserAction();
    public ObjectMapper mapper = new ObjectMapper().configure(MapperFeature.USE_ANNOTATIONS, false).setSerializationInclusion(JsonInclude.Include.NON_NULL);

    @Test
    public void shop_create_order() {
        String defaultUserToken = action.registerResourceOwnerThenLogin();
        String profileId1 = action.getProfileId(defaultUserToken);
        OrderDetail orderDetailForUser = action.createOrderDetailForUser(defaultUserToken, profileId1);
        String url3 = "http://localhost:" + randomServerPort + "/v1/api/profiles/" + profileId1 + "/orders";
        ResponseEntity<String> exchange = restTemplate.exchange(url3, HttpMethod.POST, action.getHttpRequest(defaultUserToken, orderDetailForUser), String.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
        Assert.assertNotNull(exchange.getHeaders().getLocation().toString());

    }

    @Test
    public void shop_create_same_order_again() {
        String defaultUserToken = action.registerResourceOwnerThenLogin();
        String profileId1 = action.getProfileId(defaultUserToken);
        OrderDetail orderDetailForUser = action.createOrderDetailForUser(defaultUserToken, profileId1);
        String url3 = "http://localhost:" + randomServerPort + "/v1/api/profiles/" + profileId1 + "/orders";
        restTemplate.exchange(url3, HttpMethod.POST, action.getHttpRequest(defaultUserToken, orderDetailForUser), String.class);
        ResponseEntity<String> exchange7 = restTemplate.exchange(url3, HttpMethod.POST, action.getHttpRequest(defaultUserToken, orderDetailForUser), String.class);
        Assert.assertEquals(HttpStatus.OK, exchange7.getStatusCode());
        Assert.assertNotNull(exchange7.getHeaders().getLocation().toString());
    }

    @Test
    public void shop_read_history_orders() {
        String defaultUserToken = action.registerResourceOwnerThenLogin();
        String profileId1 = action.getProfileId(defaultUserToken);
        String url = "http://localhost:" + randomServerPort + "/v1/api/profiles/" + profileId1 + "/orders";
        ParameterizedTypeReference<List<OrderDetail>> responseType = new ParameterizedTypeReference<>() {
        };
        ResponseEntity<List<OrderDetail>> exchange = restTemplate.exchange(url, HttpMethod.GET, action.getHttpRequest(defaultUserToken), responseType);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
    }

    @Test
    public void shop_read_order_details() {
        String defaultUserToken = action.registerResourceOwnerThenLogin();
        String profileId1 = action.getProfileId(defaultUserToken);
        OrderDetail orderDetailForUser = action.createOrderDetailForUser(defaultUserToken, profileId1);
        String url3 = "http://localhost:" + randomServerPort + "/v1/api/profiles/" + profileId1 + "/orders";
        ResponseEntity<String> exchange = restTemplate.exchange(url3, HttpMethod.POST, action.getHttpRequest(defaultUserToken, orderDetailForUser), String.class);
        String s = exchange.getHeaders().getLocation().toString();
        Integer start = s.indexOf("product_id");
        String searchStr = s.substring(start);
        String substring = searchStr.substring(searchStr.indexOf('=') + 1, searchStr.indexOf('&'));
        ResponseEntity<OrderDetail> exchange2 = restTemplate.exchange(url3 + "/" + substring, HttpMethod.GET, action.getHttpRequest(defaultUserToken), OrderDetail.class);
        Assert.assertEquals(HttpStatus.OK, exchange2.getStatusCode());
        Assert.assertNotNull(exchange2.getBody());


    }

    @Test
    public void shop_place_order_but_insufficient_order_storage() {

    }
}
