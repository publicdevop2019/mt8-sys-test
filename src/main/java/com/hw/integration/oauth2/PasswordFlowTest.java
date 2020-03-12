package com.hw.integration.oauth2;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Collections;
import java.util.List;
import java.util.UUID;


@RunWith(SpringRunner.class)
@Slf4j
public class PasswordFlowTest {
    private String password = "password";
    private String client_credentials = "client_credentials";
    private String valid_clientId = "login-id";
    private String valid_register_clientId = "register-id";
    private String valid_clientId_no_refersh = "test-id";
    private String valid_empty_secret = "";
    private String valid_username_root = "haolinwei2015@gmail.com";
    private String valid_username_admin = "haolinwei2017@gmail.com";
    private String valid_username_user = "haolinwei2018@gmail.com";
    private String valid_pwd = "root";
    private String invalid_username = "root2@gmail.com";
    private String invalid_clientId = "root2";
    public ObjectMapper mapper = new ObjectMapper().configure(MapperFeature.USE_ANNOTATIONS, false).setSerializationInclusion(JsonInclude.Include.NON_NULL);
    private UserAction action = new UserAction();
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
    public void create_user_then_login() {
        ResourceOwner user = getUser();
        ResponseEntity<DefaultOAuth2AccessToken> user1 = createUser(user, valid_register_clientId);
        Assert.assertEquals(HttpStatus.OK, user1.getStatusCode());
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(password, valid_username_root, valid_pwd, valid_clientId, valid_empty_secret);
        Assert.assertEquals(HttpStatus.OK, tokenResponse.getStatusCode());
    }

    @Test
    public void get_access_token_and_refresh_token_for_clients_with_refresh_configured() {
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(password, valid_username_root, valid_pwd, valid_clientId, valid_empty_secret);
        Assert.assertNotNull(tokenResponse.getBody().getValue());
        Assert.assertNotNull(tokenResponse.getBody().getRefreshToken().getValue());
    }

    @Test
    public void get_access_token_only_for_clients_without_refresh_configured() {
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(password, valid_username_root, valid_pwd, valid_clientId_no_refersh, valid_empty_secret);
        Assert.assertNotNull(tokenResponse.getBody().getValue());
        Assert.assertNull(tokenResponse.getBody().getRefreshToken());
    }

    @Test
    public void check_jwt_authorities_for_root_account() {
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(password, valid_username_root, valid_pwd, valid_clientId, valid_empty_secret);
        Assert.assertNotNull(tokenResponse.getBody().getValue());
        Assert.assertNotNull(tokenResponse.getBody().getRefreshToken().getValue());
        List<String> authorities = ServiceUtility.getAuthority(tokenResponse.getBody().getValue());
        Assert.assertEquals(1, authorities.stream().filter(e -> e.equals(ResourceOwnerAuthorityEnum.ROLE_USER.toString())).count());
        Assert.assertEquals(1, authorities.stream().filter(e -> e.equals(ResourceOwnerAuthorityEnum.ROLE_ADMIN.toString())).count());
        Assert.assertEquals(1, authorities.stream().filter(e -> e.equals(ResourceOwnerAuthorityEnum.ROLE_ROOT.toString())).count());
    }

    @Test
    public void check_jwt_authorities_for_admin_account() {
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(password, valid_username_admin, valid_pwd, valid_clientId, valid_empty_secret);
        Assert.assertNotNull(tokenResponse.getBody().getValue());
        Assert.assertNotNull(tokenResponse.getBody().getRefreshToken().getValue());
        List<String> authorities = ServiceUtility.getAuthority(tokenResponse.getBody().getValue());
        Assert.assertEquals(1, authorities.stream().filter(e -> e.equals(ResourceOwnerAuthorityEnum.ROLE_USER.toString())).count());
        Assert.assertEquals(1, authorities.stream().filter(e -> e.equals(ResourceOwnerAuthorityEnum.ROLE_ADMIN.toString())).count());
        Assert.assertEquals(0, authorities.stream().filter(e -> e.equals(ResourceOwnerAuthorityEnum.ROLE_ROOT.toString())).count());
    }

    @Test
    public void check_jwt_authorities_for_user_account() {
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(password, valid_username_user, valid_pwd, valid_clientId, valid_empty_secret);
        Assert.assertNotNull(tokenResponse.getBody().getValue());
        Assert.assertNotNull(tokenResponse.getBody().getRefreshToken().getValue());
        List<String> authorities = ServiceUtility.getAuthority(tokenResponse.getBody().getValue());
        Assert.assertEquals(1, authorities.stream().filter(e -> e.equals(ResourceOwnerAuthorityEnum.ROLE_USER.toString())).count());
        Assert.assertEquals(0, authorities.stream().filter(e -> e.equals(ResourceOwnerAuthorityEnum.ROLE_ADMIN.toString())).count());
        Assert.assertEquals(0, authorities.stream().filter(e -> e.equals(ResourceOwnerAuthorityEnum.ROLE_ROOT.toString())).count());
    }


    @Test
    public void should_not_get_token_when_user_credentials_are_wrong_even_client_is_valid() {
        ResponseEntity<?> tokenResponse = getTokenResponse(password, invalid_username, valid_pwd, valid_clientId, valid_empty_secret);
        Assert.assertEquals(HttpStatus.BAD_REQUEST, tokenResponse.getStatusCode());
    }

    @Test
    public void should_not_get_token_when_user_credentials_are_valid_but_client_is_wrong() {
        ResponseEntity<?> tokenResponse = getTokenResponse(password, valid_username_root, valid_pwd, invalid_clientId, valid_empty_secret);
        Assert.assertEquals(HttpStatus.UNAUTHORIZED, tokenResponse.getStatusCode());
    }

    @Test
    public void should_not_get_token_when_user_credentials_are_valid_and_client_is_valid_but_grant_type_is_wrong() {
        ResponseEntity<?> tokenResponse = getTokenResponse(client_credentials, valid_username_root, valid_pwd, valid_clientId, valid_empty_secret);
        Assert.assertEquals(tokenResponse.getStatusCode(), HttpStatus.UNAUTHORIZED);
    }

    private ResponseEntity<DefaultOAuth2AccessToken> getTokenResponse(String grantType, String username, String userPwd, String clientId, String clientSecret) {
        String url = UserAction.proxyUrl + "/" + "oauth/token";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", grantType);
        params.add("username", username);
        params.add("password", userPwd);
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        return action.restTemplate.exchange(url, HttpMethod.POST, request, DefaultOAuth2AccessToken.class);
    }

    private ResourceOwner getUser() {
        ResourceOwner resourceOwner = new ResourceOwner();
        resourceOwner.setPassword(UUID.randomUUID().toString().replace("-", ""));
        resourceOwner.setEmail(UUID.randomUUID().toString().replace("-", "") + "@gmail.com");
        return resourceOwner;
    }

    private ResponseEntity<DefaultOAuth2AccessToken> createUser(ResourceOwner user, String clientId) {
        String url = UserAction.proxyUrl + "/api" + "/resourceOwners";
        ResponseEntity<DefaultOAuth2AccessToken> registerTokenResponse = getRegisterTokenResponse(client_credentials, clientId, valid_empty_secret);
        String value = registerTokenResponse.getBody().getValue();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(value);
        String s = null;
        try {
            s = mapper.writeValueAsString(user);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        HttpEntity<String> request = new HttpEntity<>(s, headers);
        return action.restTemplate.exchange(url, HttpMethod.POST, request, DefaultOAuth2AccessToken.class);
    }

    private ResponseEntity<DefaultOAuth2AccessToken> getRegisterTokenResponse(String grantType, String clientId, String clientSecret) {
        String url = UserAction.proxyUrl + "/" + "oauth/token";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", grantType);
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        return action.restTemplate.exchange(url, HttpMethod.POST, request, DefaultOAuth2AccessToken.class);
    }


}
