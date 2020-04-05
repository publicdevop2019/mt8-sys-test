package com.hw.integration.oauth2;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Collections;
import java.util.UUID;

@RunWith(SpringRunner.class)
@Slf4j
@SpringBootTest
public class ClientCredentialsTest {
    private String password = "password";
    private String client_credentials = "client_credentials";
    private String valid_clientId = "oauth2-id";
    private String valid_clientId_no_secret = "register-id";
    private String valid_clientSecret = "root";
    private String valid_empty_secret = "";
    @Autowired
    private UserAction action;
    UUID uuid;
    @Rule
    public TestWatcher watchman = new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
            action.saveResult(description,uuid);
            log.error("test failed, method {}, uuid {}", description.getMethodName(), uuid);
        }
    };

    @Before
    public void setUp() {
        uuid = UUID.randomUUID();
        action.restTemplate.getRestTemplate().setInterceptors(Collections.singletonList(new OutgoingReqInterceptor(uuid)));
    }
    @Test
    public void use_client_with_secret() {
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(client_credentials, valid_clientId, valid_clientSecret);
        Assert.assertNotNull(tokenResponse.getBody().getValue());
    }

    @Test
    public void use_client_with_empty_secret() {
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(client_credentials, valid_clientId_no_secret, valid_empty_secret);
        Assert.assertNotNull(tokenResponse.getBody().getValue());

    }

    @Test
    public void use_client_with_wrong_credentials() {
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(client_credentials, valid_clientId, valid_empty_secret);
        Assert.assertEquals(HttpStatus.UNAUTHORIZED, tokenResponse.getStatusCode());

    }

    @Test
    public void use_client_with_wrong_grant_type() {
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(password, valid_clientId, valid_clientSecret);
        Assert.assertEquals(HttpStatus.UNAUTHORIZED, tokenResponse.getStatusCode());

    }

    @Test
    public void trying_to_login_with_not_exist_client() {
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(password, UUID.randomUUID().toString(), UUID.randomUUID().toString());
        Assert.assertEquals(tokenResponse.getStatusCode(), HttpStatus.UNAUTHORIZED);

    }

    private ResponseEntity<DefaultOAuth2AccessToken> getTokenResponse(String grantType, String clientId, String clientSecret) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", grantType);
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        return action.restTemplate.exchange(UserAction.PROXY_URL_TOKEN, HttpMethod.POST, request, DefaultOAuth2AccessToken.class);
    }
}
