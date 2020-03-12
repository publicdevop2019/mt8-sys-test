package com.hw.integration.proxy;

import com.hw.helper.OutgoingReqInterceptor;
import com.hw.helper.SecurityProfile;
import com.hw.helper.UserAction;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * this integration auth requires oauth2service to be running
 */
@RunWith(SpringRunner.class)
@Slf4j
public class SecurityProfileControllerTest {
    private String password = "password";
    private String login_clientId = "login-id";
    private String username_admin = "haolinwei2017@gmail.com";
    private String username_root = "haolinwei2015@gmail.com";
    private String userPwd = "root";
    private UserAction action=new UserAction();
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
    public void modify_existing_profile_to_prevent_access() {
        String url2 = UserAction.proxyUrl + "/api" + "/resourceOwners";
        /**
         * before modify, admin is able to access resourceOwner apis
         */
        ResponseEntity<DefaultOAuth2AccessToken> pwdTokenResponse = getPwdTokenResponse(password, login_clientId, "", username_admin, userPwd);
        String bearer1 = pwdTokenResponse.getBody().getValue();
        HttpHeaders headers1 = new HttpHeaders();
        headers1.setBearerAuth(bearer1);
        HttpEntity<Object> hashMapHttpEntity1 = new HttpEntity<>(headers1);
        ResponseEntity<String> exchange1 = action.restTemplate.exchange(url2, HttpMethod.GET, hashMapHttpEntity1, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange1.getStatusCode());

        /**
         * modify profile to prevent admin access
         */
        ResponseEntity<List<SecurityProfile>> listResponseEntity = readProfile();
        SecurityProfile securityProfile = listResponseEntity.getBody().get(6);
        securityProfile.setExpression("hasRole('ROLE_ROOT') and #oauth2.hasScope('trust') and #oauth2.isUser()");
        ResponseEntity<String> stringResponseEntity = updateProfile(securityProfile, 6L);
        Assert.assertEquals(HttpStatus.OK, stringResponseEntity.getStatusCode());

        /**
         * after modify, admin is not able to access resourceOwner apis
         */
        ResponseEntity<String> exchange = action.restTemplate.exchange(url2, HttpMethod.GET, hashMapHttpEntity1, String.class);
        Assert.assertEquals(HttpStatus.FORBIDDEN, exchange.getStatusCode());

        /**
         * modify profile to allow access
         */
        securityProfile.setExpression("hasRole('ROLE_ADMIN') and #oauth2.hasScope('trust') and #oauth2.isUser()");
        ResponseEntity<String> stringResponseEntity1 = updateProfile(securityProfile, 6L);
        Assert.assertEquals(HttpStatus.OK, stringResponseEntity1.getStatusCode());

        ResponseEntity<String> exchange2 = action.restTemplate.exchange(url2, HttpMethod.GET, hashMapHttpEntity1, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange2.getStatusCode());
    }

    private ResponseEntity<DefaultOAuth2AccessToken> getPwdTokenResponse(String grantType, String clientId, String clientSecret, String username, String pwd) {
        String url = UserAction.proxyUrl + "/" + "oauth/token";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", grantType);
        params.add("username", username);
        params.add("password", pwd);
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        return action.restTemplate.exchange(url, HttpMethod.POST, request, DefaultOAuth2AccessToken.class);
    }

    private ResponseEntity<List<SecurityProfile>> readProfile() {
        ResponseEntity<DefaultOAuth2AccessToken> pwdTokenResponse2 = getPwdTokenResponse(password, login_clientId, "", username_root, userPwd);
        String bearer1 = pwdTokenResponse2.getBody().getValue();
        String url = UserAction.proxyUrl + "/proxy/security" + "/profiles";
        HttpHeaders headers1 = new HttpHeaders();
        headers1.setBearerAuth(bearer1);
        HttpEntity<SecurityProfile> hashMapHttpEntity1 = new HttpEntity<>(headers1);
        ParameterizedTypeReference<List<SecurityProfile>> responseType = new ParameterizedTypeReference<>() {
        };
        return action.restTemplate.exchange(url, HttpMethod.GET, hashMapHttpEntity1, responseType);
    }

    private ResponseEntity<String> createProfile(SecurityProfile securityProfile) {
        ResponseEntity<DefaultOAuth2AccessToken> pwdTokenResponse2 = getPwdTokenResponse(password, login_clientId, "", username_root, userPwd);
        String bearer1 = pwdTokenResponse2.getBody().getValue();
        String url = UserAction.proxyUrl + "/proxy/security" + "/profile";
        HttpHeaders headers1 = new HttpHeaders();
        headers1.setBearerAuth(bearer1);
        HttpEntity<SecurityProfile> hashMapHttpEntity1 = new HttpEntity<>(securityProfile, headers1);
        return action.restTemplate.exchange(url, HttpMethod.POST, hashMapHttpEntity1, String.class);
    }

    private ResponseEntity<String> updateProfile(SecurityProfile securityProfile, Long id) {
        ResponseEntity<DefaultOAuth2AccessToken> pwdTokenResponse2 = getPwdTokenResponse(password, login_clientId, "", username_root, userPwd);
        String bearer1 = pwdTokenResponse2.getBody().getValue();
        String url = UserAction.proxyUrl + "/proxy/security" + "/profile/" + id;
        HttpHeaders headers1 = new HttpHeaders();
        headers1.setBearerAuth(bearer1);
        HttpEntity<SecurityProfile> hashMapHttpEntity1 = new HttpEntity<>(securityProfile, headers1);
        return action.restTemplate.exchange(url, HttpMethod.PUT, hashMapHttpEntity1, String.class);
    }

    private ResponseEntity<String> deleteProfile(Long id) {
        ResponseEntity<DefaultOAuth2AccessToken> pwdTokenResponse2 = getPwdTokenResponse(password, login_clientId, "", username_root, userPwd);
        String bearer1 = pwdTokenResponse2.getBody().getValue();
        String url = UserAction.proxyUrl + "/proxy/security" + "/profile/" + id;
        HttpHeaders headers1 = new HttpHeaders();
        headers1.setBearerAuth(bearer1);
        HttpEntity<Object> hashMapHttpEntity1 = new HttpEntity<>(headers1);
        return action.restTemplate.exchange(url, HttpMethod.DELETE, hashMapHttpEntity1, String.class);
    }
}
