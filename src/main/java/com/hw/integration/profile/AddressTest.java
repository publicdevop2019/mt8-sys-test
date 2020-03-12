package com.hw.integration.profile;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hw.helper.Address;
import com.hw.helper.OutgoingReqInterceptor;
import com.hw.helper.UserAction;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@RunWith(SpringRunner.class)
@Slf4j
public class AddressTest {
    UserAction action = new UserAction();
    public ObjectMapper mapper = new ObjectMapper().configure(MapperFeature.USE_ANNOTATIONS, false).setSerializationInclusion(JsonInclude.Include.NON_NULL);
    UUID uuid;
    @Rule
    public TestWatcher watchman = new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
            log.error("test failed, method {}, uuid {}", description.getMethodName(), uuid);
        }
    };

    @Before
    public void setUp() {
        uuid = UUID.randomUUID();
        action.restTemplate.getRestTemplate().setInterceptors(Collections.singletonList(new OutgoingReqInterceptor(uuid)));
    }

    @Test
    public void shop_read_all_addresses() {
        String defaultUserToken = action.registerResourceOwnerThenLogin();
        String profileId1 = action.getProfileId(defaultUserToken);
        String url = UserAction.proxyUrl + "/api/profiles/" + profileId1 + "/addresses";
        ParameterizedTypeReference<List<Address>> responseType = new ParameterizedTypeReference<>() {
        };
        ResponseEntity<List<Address>> exchange = action.restTemplate.exchange(url, HttpMethod.GET, action.getHttpRequest(defaultUserToken), responseType);
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
        ResponseEntity<List<Address>> exchange2 = action.restTemplate.exchange(url2, HttpMethod.GET, action.getHttpRequest(defaultUserToken), responseType);
        int i = new Random().nextInt(exchange2.getBody().size());
        Long id = exchange2.getBody().get(i).getId();
        String url = UserAction.proxyUrl + "/api/profiles/" + profileId1 + "/addresses/" + id;
        ResponseEntity<Address> exchange = action.restTemplate.exchange(url, HttpMethod.GET, action.getHttpRequest(defaultUserToken), Address.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());

    }

    @Test
    public void shop_update_address_details() {
        String defaultUserToken = action.registerResourceOwnerThenLogin();
        String profileId1 = action.getProfileId(defaultUserToken);
        String url = UserAction.proxyUrl + "/api/profiles/" + profileId1 + "/addresses";
        ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.POST, action.getHttpRequest(defaultUserToken, action.getRandomAddress()), String.class);
        String s = exchange.getHeaders().getLocation().toString();
        ResponseEntity<String> exchange2 = action.restTemplate.exchange(url + "/" + s, HttpMethod.PUT, action.getHttpRequest(defaultUserToken, action.getRandomAddress()), String.class);
        Assert.assertEquals(HttpStatus.OK, exchange2.getStatusCode());
    }

    @Test
    public void shop_create_address() {
        String defaultUserToken = action.registerResourceOwnerThenLogin();
        String profileId1 = action.getProfileId(defaultUserToken);
        String url = UserAction.proxyUrl + "/api/profiles/" + profileId1 + "/addresses";
        ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.POST, action.getHttpRequest(defaultUserToken, action.getRandomAddress()), String.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
        Assert.assertNotNull(exchange.getHeaders().getLocation().toString());
    }

    @Test
    public void shop_create_same_address() {
        String defaultUserToken = action.registerResourceOwnerThenLogin();
        String profileId1 = action.getProfileId(defaultUserToken);
        String url = UserAction.proxyUrl + "/api/profiles/" + profileId1 + "/addresses";
        Address randomAddress = action.getRandomAddress();
        ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.POST, action.getHttpRequest(defaultUserToken, randomAddress), String.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
        Assert.assertNotNull(exchange.getHeaders().getLocation().toString());
        ResponseEntity<String> exchange2 = action.restTemplate.exchange(url, HttpMethod.POST, action.getHttpRequest(defaultUserToken, randomAddress), String.class);
        Assert.assertEquals(HttpStatus.BAD_REQUEST, exchange2.getStatusCode());
    }

    @Test
    public void shop_delete_address() {
        String defaultUserToken = action.registerResourceOwnerThenLogin();
        String profileId1 = action.getProfileId(defaultUserToken);
        String url = UserAction.proxyUrl + "/api/profiles/" + profileId1 + "/addresses";
        Address randomAddress = action.getRandomAddress();
        ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.POST, action.getHttpRequest(defaultUserToken, randomAddress), String.class);
        String s = exchange.getHeaders().getLocation().toString();
        ResponseEntity<String> exchange2 = action.restTemplate.exchange(url + "/" + s, HttpMethod.DELETE, action.getHttpRequest(defaultUserToken, randomAddress), String.class);
        Assert.assertEquals(HttpStatus.OK, exchange2.getStatusCode());
    }
}
