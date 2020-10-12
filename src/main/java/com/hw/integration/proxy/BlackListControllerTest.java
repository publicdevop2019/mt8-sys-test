package com.hw.integration.proxy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.*;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

import static com.hw.helper.UserAction.ACCESS_ROLE_APP;
import static com.hw.helper.UserAction.ACCESS_ROLE_ROOT;

/**
 * this integration auth requires oauth2service to be running
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class BlackListControllerTest {
    public static final String PROXY_BLACKLIST = "/proxy/revoke-tokens";
    public static final String USERS_ADMIN = "/users/admin";
    private String client_credentials = "client_credentials";
    private String password = "password";
    private String valid_clientId = "838330249904134";
    private String should_block_clientId_root = "838330249904134";
    private String should_block_clientId_non_root = "838330249904133";
    private String wrong_clientId = "838330249904135";
    private String login_clientId = "838330249904133";
    private String valid_clientSecret = "root";

    private ObjectMapper mapper = new ObjectMapper();
    @Autowired
    private UserAction action;
    private String username = "haolinwei2017@gmail.com";
    private String userPwd = "root";
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
    public void receive_request_blacklist_client_then_block_client_old_request_which_trying_to_access_proxy_internal_endpoints() throws JsonProcessingException, InterruptedException {
        String url = UserAction.proxyUrl + PROXY_BLACKLIST + ACCESS_ROLE_ROOT;
        String url2 = UserAction.proxyUrl + PROXY_BLACKLIST + ACCESS_ROLE_APP;
        /**
         * before client get blacklisted, client is able to access proxy endpoints
         */
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse1 = getTokenResponse(client_credentials, should_block_clientId_root, valid_clientSecret);
        String bearer1 = tokenResponse1.getBody().getValue();

        HttpHeaders headers1 = new HttpHeaders();
        headers1.setBearerAuth(bearer1);
        headers1.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> hashMapHttpEntity1 = new HttpEntity<>(null, headers1);
        ResponseEntity<String> exchange1 = action.restTemplate.exchange(url, HttpMethod.GET, hashMapHttpEntity1, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange1.getStatusCode());


        /**
         * block client
         */
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(client_credentials, valid_clientId, valid_clientSecret);
        String bearer = tokenResponse.getBody().getValue();

        HashMap<String, String> blockBody = new HashMap<>();
        blockBody.put("id", should_block_clientId_root);
        blockBody.put("type", "CLIENT");
        String body = mapper.writeValueAsString(blockBody);
        HttpHeaders blockCallHeader = new HttpHeaders();
        blockCallHeader.setBearerAuth(bearer);
        blockCallHeader.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> blockReqEntity = new HttpEntity<>(body, blockCallHeader);
        ResponseEntity<String> exchange = action.restTemplate.exchange(url2, HttpMethod.POST, blockReqEntity, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());

        /**
         * after client get blacklisted, client with old token will get 401
         */
        ResponseEntity<String> exchange2 = action.restTemplate.exchange(url, HttpMethod.POST, hashMapHttpEntity1, String.class);
        Assert.assertEquals(HttpStatus.UNAUTHORIZED, exchange2.getStatusCode());

        /**
         * after client obtain new token from auth server, it can access resource again
         * add thread sleep to prevent token get revoked and generate within a second
         */
        Thread.sleep(1000);
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse3 = getTokenResponse(client_credentials, should_block_clientId_root, valid_clientSecret);
        String bearer3 = tokenResponse3.getBody().getValue();
        headers1.setBearerAuth(bearer3);
        HttpEntity<String> hashMapHttpEntity3 = new HttpEntity<>(null, headers1);
        ResponseEntity<String> exchange3 = action.restTemplate.exchange(url, HttpMethod.GET, hashMapHttpEntity3, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange3.getStatusCode());
    }

    @Test
    public void receive_request_blacklist_client_then_block_client_old_request_which_trying_to_access_proxy_external_endpoints() throws JsonProcessingException, InterruptedException {

        String url = UserAction.proxyUrl + PROXY_BLACKLIST + ACCESS_ROLE_ROOT;
        String url2 = UserAction.proxyUrl + UserAction.AUTH_SVC + USERS_ADMIN;
        /**
         * before client get blacklisted, client is able to access auth server non token endpoint
         */
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse1 = getPwdTokenResponse(password, should_block_clientId_non_root, "", username, userPwd);
        String bearer1 = tokenResponse1.getBody().getValue();
        HttpHeaders headers1 = new HttpHeaders();
        headers1.setBearerAuth(bearer1);
        HttpEntity<Object> hashMapHttpEntity1 = new HttpEntity<>(headers1);
        ResponseEntity<String> exchange1 = action.restTemplate.exchange(url2, HttpMethod.GET, hashMapHttpEntity1, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange1.getStatusCode());


        /**
         * block client
         */
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(client_credentials, valid_clientId, valid_clientSecret);
        String bearer = tokenResponse.getBody().getValue();

        HashMap<String, String> stringStringHashMap = new HashMap<>();
        stringStringHashMap.put("id", should_block_clientId_non_root);
        stringStringHashMap.put("type", "CLIENT");
        String s = mapper.writeValueAsString(stringStringHashMap);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearer);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> hashMapHttpEntity = new HttpEntity<>(s, headers);
        ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.POST, hashMapHttpEntity, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());

        /**
         * after client get blacklisted, client with old token will get 401
         */
        ResponseEntity<String> exchange2 = action.restTemplate.exchange(url2, HttpMethod.GET, hashMapHttpEntity1, String.class);
        Assert.assertEquals(HttpStatus.UNAUTHORIZED, exchange2.getStatusCode());

        /**
         * after client obtain new token from auth server, it can access resource again
         * add thread sleep to prevent token get revoked and generate within a second
         */
        Thread.sleep(1000);
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse3 = getPwdTokenResponse(password, should_block_clientId_non_root, "", username, userPwd);
        String bearer3 = tokenResponse3.getBody().getValue();
        headers1.setBearerAuth(bearer3);
        HttpEntity<Object> hashMapHttpEntity3 = new HttpEntity<>(headers1);
        ResponseEntity<String> exchange3 = action.restTemplate.exchange(url2, HttpMethod.GET, hashMapHttpEntity3, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange3.getStatusCode());
    }

    @Test
    public void only_root_client_and_trusted_client_can_add_blacklist() throws JsonProcessingException {
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(client_credentials, wrong_clientId, "");
        String bearer = tokenResponse.getBody().getValue();

        String url = UserAction.proxyUrl + PROXY_BLACKLIST + ACCESS_ROLE_ROOT;
        HashMap<String, String> stringStringHashMap = new HashMap<>();
        stringStringHashMap.put("id", should_block_clientId_non_root);
        stringStringHashMap.put("type", "CLIENT");
        String s = mapper.writeValueAsString(stringStringHashMap);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearer);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> hashMapHttpEntity = new HttpEntity<>(s, headers);
        ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.POST, hashMapHttpEntity, String.class);
        Assert.assertEquals(HttpStatus.FORBIDDEN, exchange.getStatusCode());
    }

    @Test
    public void receive_request_blacklist_resourceOwner_then_block_resourceOwner_old_request() throws JsonProcessingException, InterruptedException {
        String url2 = UserAction.proxyUrl + UserAction.AUTH_SVC + USERS_ADMIN;
        /**
         * admin user can login & call resourceOwner api
         */
        ResponseEntity<DefaultOAuth2AccessToken> pwdTokenResponse = getPwdTokenResponse(password, login_clientId, "", username, userPwd);

        String bearer0 = pwdTokenResponse.getBody().getValue();
        String refreshToken = pwdTokenResponse.getBody().getRefreshToken().getValue();
        String userId = (String) pwdTokenResponse.getBody().getAdditionalInformation().get("uid");
        HttpHeaders headers1 = new HttpHeaders();
        headers1.setBearerAuth(bearer0);
        HttpEntity<Object> hashMapHttpEntity1 = new HttpEntity<>(headers1);
        ResponseEntity<String> exchange2 = action.restTemplate.exchange(url2, HttpMethod.GET, hashMapHttpEntity1, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange2.getStatusCode());

        /**
         * blacklist admin account
         */
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(client_credentials, valid_clientId, valid_clientSecret);
        String bearer = tokenResponse.getBody().getValue();

        String url = UserAction.proxyUrl + PROXY_BLACKLIST + ACCESS_ROLE_ROOT;
        HashMap<String, String> stringStringHashMap = new HashMap<>();
        stringStringHashMap.put("id", userId);
        stringStringHashMap.put("type", "USER");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearer);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> hashMapHttpEntity = new HttpEntity<>(mapper.writeValueAsString(stringStringHashMap), headers);
        ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.POST, hashMapHttpEntity, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());

        /**
         * resourceOwner request get blocked, even refresh token should not work
         */
        ResponseEntity<String> exchange1 = action.restTemplate.exchange(url2, HttpMethod.GET, hashMapHttpEntity1, String.class);
        Assert.assertEquals(HttpStatus.UNAUTHORIZED, exchange1.getStatusCode());

        ResponseEntity<DefaultOAuth2AccessToken> refreshTokenResponse = getRefreshTokenResponse(refreshToken, login_clientId, "");
        Assert.assertEquals(HttpStatus.UNAUTHORIZED, refreshTokenResponse.getStatusCode());

        /**
         * after resourceOwner obtain new token, access is permitted
         * add thread sleep to prevent token get revoked and generate within a second
         */
        Thread.sleep(1000);
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse3 = getPwdTokenResponse(password, login_clientId, "", username, userPwd);
        String bearer3 = tokenResponse3.getBody().getValue();
        headers1.setBearerAuth(bearer3);
        HttpEntity<Object> hashMapHttpEntity3 = new HttpEntity<>(headers1);
        ResponseEntity<String> exchange3 = action.restTemplate.exchange(url2, HttpMethod.GET, hashMapHttpEntity3, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange3.getStatusCode());

    }


    private ResponseEntity<DefaultOAuth2AccessToken> getTokenResponse(String grantType, String clientId, String clientSecret) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", grantType);
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        return action.restTemplate.exchange(UserAction.PROXY_URL_TOKEN, HttpMethod.POST, request, DefaultOAuth2AccessToken.class);
    }

    private ResponseEntity<DefaultOAuth2AccessToken> getRefreshTokenResponse(String refreshToken, String clientId, String clientSecret) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "refresh_token");
        params.add("refresh_token", refreshToken);
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        return action.restTemplate.exchange(UserAction.PROXY_URL_TOKEN, HttpMethod.POST, request, DefaultOAuth2AccessToken.class);
    }

    private ResponseEntity<DefaultOAuth2AccessToken> getPwdTokenResponse(String grantType, String clientId, String clientSecret, String username, String pwd) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", grantType);
        params.add("username", username);
        params.add("password", pwd);
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        return action.restTemplate.exchange(UserAction.PROXY_URL_TOKEN, HttpMethod.POST, request, DefaultOAuth2AccessToken.class);
    }
}
