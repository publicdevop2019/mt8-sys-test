package com.hw.integration.security;

import com.hw.helper.UserAction;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class SecurityTest {
    UserAction action = new UserAction();
    private TestRestTemplate restTemplate = new TestRestTemplate();

    @Test
    public void user_modify_jwt_token_after_login() {
        String defaultUserToken = action.registerResourceOwnerThenLogin();
        String defaultUserToken2 = action.registerResourceOwnerThenLogin();
        String profileId1 = action.getProfileId(defaultUserToken);
        String url = UserAction.proxyUrl + "/api/profiles/" + profileId1 + "/addresses";
        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.GET, action.getHttpRequest(defaultUserToken2), String.class);
        Assert.assertEquals(HttpStatus.BAD_REQUEST, exchange.getStatusCode());
    }

    @Test
    public void use_modify_jwt_token_after_login_trying_to_access_other_profile() {
        String defaultUserToken = action.registerResourceOwnerThenLogin();
        String defaultUserToken2 = action.registerResourceOwnerThenLogin();
        String profileId2 = action.getProfileId(defaultUserToken2);
        String url = UserAction.proxyUrl + "/api/profiles/" + profileId2 + "/addresses";
        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.GET, action.getHttpRequest(defaultUserToken), String.class);
        Assert.assertEquals(HttpStatus.BAD_REQUEST, exchange.getStatusCode());
    }

    @Test
    public void trying_access_protected_api_without_jwt_token() {
        String defaultUserToken = action.registerResourceOwnerThenLogin();
        String profileId1 = action.getProfileId(defaultUserToken);
        String url = UserAction.proxyUrl + "/api/profiles/" + profileId1 + "/addresses";
        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.GET, action.getHttpRequest(null), String.class);
        Assert.assertEquals(HttpStatus.UNAUTHORIZED, exchange.getStatusCode());
    }


}
