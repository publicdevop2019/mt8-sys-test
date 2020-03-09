package com.hw.integration.profile;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hw.helper.Address;
import com.hw.helper.UserAction;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Random;

public class AddressTest {
    private TestRestTemplate restTemplate = new TestRestTemplate();
    UserAction action = new UserAction();
    public ObjectMapper mapper = new ObjectMapper().configure(MapperFeature.USE_ANNOTATIONS, false).setSerializationInclusion(JsonInclude.Include.NON_NULL);

    @Test
    public void shop_read_all_addresses() {
        String defaultUserToken = action.registerResourceOwnerThenLogin();
        String profileId1 = action.getProfileId(defaultUserToken);
        String url = UserAction.proxyUrl + "/api/profiles/" + profileId1 + "/addresses";
        ParameterizedTypeReference<List<Address>> responseType = new ParameterizedTypeReference<>() {
        };
        ResponseEntity<List<Address>> exchange = restTemplate.exchange(url, HttpMethod.GET, action.getHttpRequest(defaultUserToken), responseType);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
        Assert.assertNotEquals(-1, exchange.getBody().size());
    }

    @Test
    public void shop_read_address_details() {
        String defaultUserToken = action.getDefaultRootToken();
        String profileId1 = action.getProfileId(defaultUserToken);
        String url2 = UserAction.proxyUrl + "/api/profiles/" + profileId1 + "/addresses";
        ParameterizedTypeReference<List<Address>> responseType = new ParameterizedTypeReference<>() {
        };
        ResponseEntity<List<Address>> exchange2 = restTemplate.exchange(url2, HttpMethod.GET, action.getHttpRequest(defaultUserToken), responseType);
        int i = new Random().nextInt(exchange2.getBody().size());
        Long id = exchange2.getBody().get(i).getId();
        String url = UserAction.proxyUrl + "/api/profiles/" + profileId1 + "/addresses/" + id;
        ResponseEntity<Address> exchange = restTemplate.exchange(url, HttpMethod.GET, action.getHttpRequest(defaultUserToken), Address.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());

    }

    @Test
    public void shop_update_address_details() {
        String defaultUserToken = action.registerResourceOwnerThenLogin();
        String profileId1 = action.getProfileId(defaultUserToken);
        String url = UserAction.proxyUrl + "/api/profiles/" + profileId1 + "/addresses";
        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.POST, action.getHttpRequest(defaultUserToken, action.getRandomAddress()), String.class);
        String s = exchange.getHeaders().getLocation().toString();
        ResponseEntity<String> exchange2 = restTemplate.exchange(url + "/" + s, HttpMethod.PUT, action.getHttpRequest(defaultUserToken, action.getRandomAddress()), String.class);
        Assert.assertEquals(HttpStatus.OK, exchange2.getStatusCode());
    }

    @Test
    public void shop_create_address() {
        String defaultUserToken = action.registerResourceOwnerThenLogin();
        String profileId1 = action.getProfileId(defaultUserToken);
        String url = UserAction.proxyUrl + "/api/profiles/" + profileId1 + "/addresses";
        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.POST, action.getHttpRequest(defaultUserToken, action.getRandomAddress()), String.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
        Assert.assertNotNull(exchange.getHeaders().getLocation().toString());
    }

    @Test
    public void shop_create_same_address() {
        String defaultUserToken = action.registerResourceOwnerThenLogin();
        String profileId1 = action.getProfileId(defaultUserToken);
        String url = UserAction.proxyUrl + "/api/profiles/" + profileId1 + "/addresses";
        Address randomAddress = action.getRandomAddress();
        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.POST, action.getHttpRequest(defaultUserToken, randomAddress), String.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
        Assert.assertNotNull(exchange.getHeaders().getLocation().toString());
        ResponseEntity<String> exchange2 = restTemplate.exchange(url, HttpMethod.POST, action.getHttpRequest(defaultUserToken, randomAddress), String.class);
        Assert.assertEquals(HttpStatus.BAD_REQUEST, exchange2.getStatusCode());
    }

    @Test
    public void shop_delete_address() {
        String defaultUserToken = action.registerResourceOwnerThenLogin();
        String profileId1 = action.getProfileId(defaultUserToken);
        String url = UserAction.proxyUrl + "/api/profiles/" + profileId1 + "/addresses";
        Address randomAddress = action.getRandomAddress();
        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.POST, action.getHttpRequest(defaultUserToken, randomAddress), String.class);
        String s = exchange.getHeaders().getLocation().toString();
        ResponseEntity<String> exchange2 = restTemplate.exchange(url + "/" + s, HttpMethod.DELETE, action.getHttpRequest(defaultUserToken, randomAddress), String.class);
        Assert.assertEquals(HttpStatus.OK, exchange2.getStatusCode());
    }
}
