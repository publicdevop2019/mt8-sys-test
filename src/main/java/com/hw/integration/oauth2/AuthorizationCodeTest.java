package com.hw.integration.oauth2;

import com.hw.helper.OutgoingReqInterceptor;
import com.hw.helper.ResourceOwnerAuthorityEnum;
import com.hw.helper.ServiceUtility;
import com.hw.helper.UserAction;
import com.jayway.jsonpath.JsonPath;
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
import java.util.List;
import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class AuthorizationCodeTest {

    private String password = "password";
    private String authorization_code = "authorization_code";
    private String valid_clientId = "login-id";
    private String valid_third_party = "mgfb-id";
    private String invalid_third_party = UUID.randomUUID().toString();
    private String valid_empty_secret = "";
    private String valid_username_root = "haolinwei2015@gmail.com";
    private String valid_username_admin = "haolinwei2017@gmail.com";
    private String valid_username_user = "haolinwei2018@gmail.com";
    private String valid_pwd = "root";
    private String valid_redirect_uri = "http://localhost:4200";
    private String state = "login";
    private String response_type = "code";
    @Autowired
    private UserAction action;
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
    public void should_get_authorize_code_after_pwd_login_for_user() {
        ResponseEntity<DefaultOAuth2AccessToken> defaultOAuth2AccessTokenResponseEntity = pwdFlowLogin(password, valid_username_user, valid_pwd, valid_clientId, valid_empty_secret);
        String accessToken = defaultOAuth2AccessTokenResponseEntity.getBody().getValue();
        ResponseEntity<String> code = getCodeResp(valid_third_party, accessToken);
        String body = code.getBody();
        String read = JsonPath.read(body, "$.authorize_code");
        Assert.assertNotNull(read);
    }

    @Test
    public void should_authorize_token_has_right_role_for_admin() {
        ResponseEntity<DefaultOAuth2AccessToken> defaultOAuth2AccessTokenResponseEntity = pwdFlowLogin(password, valid_username_admin, valid_pwd, valid_clientId, valid_empty_secret);
        String accessToken = defaultOAuth2AccessTokenResponseEntity.getBody().getValue();
        ResponseEntity<String> codeResp = getCodeResp(valid_third_party, accessToken);
        String code = JsonPath.read(codeResp.getBody(), "$.authorize_code");

        Assert.assertNotNull(code);

        ResponseEntity<DefaultOAuth2AccessToken> authorizationToken = getAuthorizationToken(authorization_code, code, valid_redirect_uri, valid_third_party);

        Assert.assertEquals(HttpStatus.OK, authorizationToken.getStatusCode());
        Assert.assertNotNull(authorizationToken.getBody());
        DefaultOAuth2AccessToken body = authorizationToken.getBody();
        List<String> authorities = ServiceUtility.getAuthority(body.getValue());
        Assert.assertEquals(1, authorities.stream().filter(e -> e.equals(ResourceOwnerAuthorityEnum.ROLE_USER.toString())).count());
        Assert.assertEquals(1, authorities.stream().filter(e -> e.equals(ResourceOwnerAuthorityEnum.ROLE_ADMIN.toString())).count());
        Assert.assertEquals(0, authorities.stream().filter(e -> e.equals(ResourceOwnerAuthorityEnum.ROLE_ROOT.toString())).count());
    }

    @Test
    public void should_authorize_token_has_right_role_for_root() {
        ResponseEntity<DefaultOAuth2AccessToken> defaultOAuth2AccessTokenResponseEntity = pwdFlowLogin(password, valid_username_root, valid_pwd, valid_clientId, valid_empty_secret);
        String accessToken = defaultOAuth2AccessTokenResponseEntity.getBody().getValue();
        ResponseEntity<String> codeResp = getCodeResp(valid_third_party, accessToken);
        String code = JsonPath.read(codeResp.getBody(), "$.authorize_code");

        Assert.assertNotNull(code);

        ResponseEntity<DefaultOAuth2AccessToken> authorizationToken = getAuthorizationToken(authorization_code, code, valid_redirect_uri, valid_third_party);

        Assert.assertEquals(HttpStatus.OK, authorizationToken.getStatusCode());
        Assert.assertNotNull(authorizationToken.getBody());
        DefaultOAuth2AccessToken body = authorizationToken.getBody();
        List<String> authorities = ServiceUtility.getAuthority(body.getValue());
        Assert.assertEquals(1, authorities.stream().filter(e -> e.equals(ResourceOwnerAuthorityEnum.ROLE_USER.toString())).count());
        Assert.assertEquals(1, authorities.stream().filter(e -> e.equals(ResourceOwnerAuthorityEnum.ROLE_ADMIN.toString())).count());
        Assert.assertEquals(1, authorities.stream().filter(e -> e.equals(ResourceOwnerAuthorityEnum.ROLE_ROOT.toString())).count());

    }

    @Test
    public void should_authorize_token_has_right_role_for_user() {
        ResponseEntity<DefaultOAuth2AccessToken> defaultOAuth2AccessTokenResponseEntity = pwdFlowLogin(password, valid_username_user, valid_pwd, valid_clientId, valid_empty_secret);
        String accessToken = defaultOAuth2AccessTokenResponseEntity.getBody().getValue();
        ResponseEntity<String> codeResp = getCodeResp(valid_third_party, accessToken);
        String code = JsonPath.read(codeResp.getBody(), "$.authorize_code");

        Assert.assertNotNull(code);

        ResponseEntity<DefaultOAuth2AccessToken> authorizationToken = getAuthorizationToken(authorization_code, code, valid_redirect_uri, valid_third_party);

        Assert.assertEquals(HttpStatus.OK, authorizationToken.getStatusCode());
        Assert.assertNotNull(authorizationToken.getBody());
        DefaultOAuth2AccessToken body = authorizationToken.getBody();
        List<String> authorities = ServiceUtility.getAuthority(body.getValue());
        Assert.assertEquals(1, authorities.stream().filter(e -> e.equals(ResourceOwnerAuthorityEnum.ROLE_USER.toString())).count());
        Assert.assertEquals(0, authorities.stream().filter(e -> e.equals(ResourceOwnerAuthorityEnum.ROLE_ADMIN.toString())).count());
        Assert.assertEquals(0, authorities.stream().filter(e -> e.equals(ResourceOwnerAuthorityEnum.ROLE_ROOT.toString())).count());

    }


    @Test
    public void use_wrong_authorize_code_after_user_grant_access() {
        ResponseEntity<DefaultOAuth2AccessToken> defaultOAuth2AccessTokenResponseEntity = pwdFlowLogin(password, valid_username_root, valid_pwd, valid_clientId, valid_empty_secret);
        String accessToken = defaultOAuth2AccessTokenResponseEntity.getBody().getValue();
        ResponseEntity<String> code = getCodeResp(valid_third_party, accessToken);
        ResponseEntity<DefaultOAuth2AccessToken> authorizationToken = getAuthorizationToken(authorization_code, UUID.randomUUID().toString(), valid_redirect_uri, valid_third_party);
        Assert.assertEquals(HttpStatus.BAD_REQUEST, authorizationToken.getStatusCode());

    }

    @Test
    public void client_use_wrong_redirect_url_during_authorization() {
        ResponseEntity<DefaultOAuth2AccessToken> defaultOAuth2AccessTokenResponseEntity = pwdFlowLogin(password, valid_username_root, valid_pwd, valid_clientId, valid_empty_secret);
        String accessToken = defaultOAuth2AccessTokenResponseEntity.getBody().getValue();
        ResponseEntity<String> codeResp = getCodeResp(valid_third_party, accessToken);
        String code = JsonPath.read(codeResp.getBody(), "$.authorize_code");
        ResponseEntity<DefaultOAuth2AccessToken> authorizationToken = getAuthorizationToken(authorization_code, code, UUID.randomUUID().toString(), valid_third_party);
        Assert.assertEquals(HttpStatus.BAD_REQUEST, authorizationToken.getStatusCode());

    }

    @Test
    public void client_use_wrong_grant_type_during_authorization() {
        ResponseEntity<DefaultOAuth2AccessToken> defaultOAuth2AccessTokenResponseEntity = pwdFlowLogin(password, valid_username_root, valid_pwd, valid_clientId, valid_empty_secret);
        String accessToken = defaultOAuth2AccessTokenResponseEntity.getBody().getValue();
        ResponseEntity<String> codeResp = getCodeResp(valid_third_party, accessToken);
        String code = JsonPath.read(codeResp.getBody(), "$.authorize_code");
        ResponseEntity<DefaultOAuth2AccessToken> authorizationToken = getAuthorizationToken(password, code, valid_redirect_uri, valid_third_party);
        Assert.assertEquals(HttpStatus.UNAUTHORIZED, authorizationToken.getStatusCode());

    }

    @Test
    public void client_use_wrong_client_id_during_authorization() {
        ResponseEntity<DefaultOAuth2AccessToken> defaultOAuth2AccessTokenResponseEntity = pwdFlowLogin(password, valid_username_root, valid_pwd, valid_clientId, valid_empty_secret);
        String accessToken = defaultOAuth2AccessTokenResponseEntity.getBody().getValue();
        ResponseEntity<String> codeResp = getCodeResp(valid_third_party, accessToken);
        String code = JsonPath.read(codeResp.getBody(), "$.authorize_code");
        ResponseEntity<DefaultOAuth2AccessToken> authorizationToken = getAuthorizationToken(authorization_code, code, valid_redirect_uri, valid_clientId);
        Assert.assertEquals(HttpStatus.UNAUTHORIZED, authorizationToken.getStatusCode());

    }

    @Test
    public void client_use_wrong_client_id_w_credential_during_authorization() {
        ResponseEntity<DefaultOAuth2AccessToken> defaultOAuth2AccessTokenResponseEntity = pwdFlowLogin(password, valid_username_root, valid_pwd, valid_clientId, valid_empty_secret);
        String accessToken = defaultOAuth2AccessTokenResponseEntity.getBody().getValue();
        ResponseEntity<String> codeResp = getCodeResp(valid_third_party, accessToken);
        String code = JsonPath.read(codeResp.getBody(), "$.authorize_code");
        ResponseEntity<DefaultOAuth2AccessToken> authorizationToken = getAuthorizationTokenSecret(authorization_code, code, valid_redirect_uri, valid_clientId, UUID.randomUUID().toString());
        Assert.assertEquals(HttpStatus.UNAUTHORIZED, authorizationToken.getStatusCode());

    }

    @Test
    public void wrong_client_id_passed_during_authorization_code_call() {
        ResponseEntity<DefaultOAuth2AccessToken> defaultOAuth2AccessTokenResponseEntity = pwdFlowLogin(password, valid_username_root, valid_pwd, valid_clientId, valid_empty_secret);
        String accessToken = defaultOAuth2AccessTokenResponseEntity.getBody().getValue();
        ResponseEntity<String> codeResp = getCodeResp(invalid_third_party, accessToken);
        Assert.assertEquals(HttpStatus.BAD_REQUEST, codeResp.getStatusCode());
    }


    private ResponseEntity<String> getCodeResp(String clientId, String bearerToken) {
        String url = UserAction.proxyUrl + UserAction.AUTH_SVC + "/authorize";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("response_type", response_type);
        params.add("client_id", clientId);
        params.add("state", state);
        params.add("redirect_uri", valid_redirect_uri);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearerToken);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        return action.restTemplate.exchange(url, HttpMethod.POST, request, String.class);
    }

    private ResponseEntity<DefaultOAuth2AccessToken> pwdFlowLogin(String grantType, String username, String userPwd, String clientId, String clientSecret) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", grantType);
        params.add("username", username);
        params.add("password", userPwd);
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        return action.restTemplate.exchange(UserAction.PROXY_URL_TOKEN, HttpMethod.POST, request, DefaultOAuth2AccessToken.class);
    }

    private ResponseEntity<DefaultOAuth2AccessToken> getAuthorizationToken(String grantType, String code, String redirect_uri, String clientId) {
        return getAuthorizationTokenSecret(grantType, code, redirect_uri, clientId, valid_empty_secret);
    }

    private ResponseEntity<DefaultOAuth2AccessToken> getAuthorizationTokenSecret(String grantType, String code, String redirect_uri, String clientId, String clientSecret) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", grantType);
        params.add("code", code);
        params.add("redirect_uri", redirect_uri);
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        return action.restTemplate.exchange(UserAction.PROXY_URL_TOKEN, HttpMethod.POST, request, DefaultOAuth2AccessToken.class);
    }
}
