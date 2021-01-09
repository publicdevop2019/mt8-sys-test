package com.hw.integration.identityaccess.proxy;

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
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;
import java.util.UUID;

/**
 * this test suits requires cors profile to be added
 */
@RunWith(SpringRunner.class)
@Slf4j
@SpringBootTest
public class CORSTest {
    @Autowired
    private UserAction action;

    private String thirdPartyOrigin = "http://localhost:4300";

    private String[] corsUris = {"/oauth/token", "/oauth/token_key", "/client", "/client/0", "/clients",
            "/authorize", "/resourceOwner", "/resourceOwner/0", "/resourceOwner/pwd", "/resourceOwners"};
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
    public void cors_oauthToken() {
        String url = UserAction.proxyUrl + UserAction.SVC_NAME_AUTH + corsUris[0];
        ResponseEntity<?> res = sendValidCorsForTokenUri(url);
        corsAssertToken(res);
    }

    @Test
    public void cors_oauthTokenKey() {
        String url = UserAction.proxyUrl + UserAction.SVC_NAME_AUTH + corsUris[1];
        ResponseEntity<?> res = sendValidCorsForTokenUri(url);
        corsAssertToken(res);

    }

    @Test
    public void cors_client() {
        String url = UserAction.proxyUrl +  corsUris[2];
        ResponseEntity<?> res = sendValidCorsForNonTokenUri(url, HttpMethod.POST);
        corsAssertNonToken(res);

    }

    @Test
    public void cors_client_w_id_put() {
        String url = UserAction.proxyUrl +  corsUris[3];
        ResponseEntity<?> res = sendValidCorsForNonTokenUri(url, HttpMethod.PUT);
        corsAssertNonToken(res);

    }

    @Test
    public void cors_client_w_id_delete() {
        String url = UserAction.proxyUrl +  corsUris[3];
        ResponseEntity<?> res = sendValidCorsForNonTokenUri(url, HttpMethod.DELETE);
        corsAssertNonToken(res);

    }

    @Test
    public void cors_clients() {
        String url = UserAction.proxyUrl +  corsUris[4];
        ResponseEntity<?> res = sendValidCorsForNonTokenUri(url, HttpMethod.GET);
        corsAssertNonToken(res);

    }

    @Test
    public void cors_authorize() {
        String url = UserAction.proxyUrl +  corsUris[5];
        ResponseEntity<?> res = sendValidCorsForNonTokenUri(url, HttpMethod.POST);
        corsAssertNonToken(res);

    }

    @Test
    public void cors_resourceOwner() {
        String url = UserAction.proxyUrl +  corsUris[6];
        ResponseEntity<?> res = sendValidCorsForNonTokenUri(url, HttpMethod.POST);
        corsAssertNonToken(res);

    }

    @Test
    public void cors_resourceOwner_id_put() {
        String url = UserAction.proxyUrl +  corsUris[7];
        ResponseEntity<?> res = sendValidCorsForNonTokenUri(url, HttpMethod.PUT);
        corsAssertNonToken(res);

    }

    @Test
    public void cors_resourceOwner_id_delete() {
        String url = UserAction.proxyUrl +  corsUris[7];
        ResponseEntity<?> res = sendValidCorsForNonTokenUri(url, HttpMethod.DELETE);
        corsAssertNonToken(res);

    }

    @Test
    public void cors_resourceOwner_id_pwd() {
        String url = UserAction.proxyUrl +  corsUris[8];
        ResponseEntity<?> res = sendValidCorsForNonTokenUri(url, HttpMethod.PATCH);
        corsAssertNonToken(res);

    }

    @Test
    public void cors_resourceOwners() {
        String url = UserAction.proxyUrl +  corsUris[9];
        ResponseEntity<?> res = sendValidCorsForNonTokenUri(url, HttpMethod.GET);
        corsAssertNonToken(res);

    }

    private ResponseEntity<?> sendValidCorsForTokenUri(String uri) {
        /**
         * origin etc restricted headers will not be set by HttpUrlConnection,
         * ref:https://stackoverflow.com/questions/41699608/resttemplate-not-passing-origin-header
         */
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        HttpHeaders headers = new HttpHeaders();
        headers.add("Origin", thirdPartyOrigin);
        headers.add("Access-Control-Request-Method", "POST");
        headers.add("Access-Control-Request-Headers", "authorization,x-requested-with");
        HttpEntity<String> request = new HttpEntity<>(headers);
        return action.restTemplate.exchange(uri, HttpMethod.OPTIONS, request, String.class);

    }

    private void corsAssertToken(ResponseEntity res) {
        Assert.assertEquals(HttpStatus.OK, res.getStatusCode());
        Assert.assertEquals("http://localhost:4300", res.getHeaders().getAccessControlAllowOrigin());
        Assert.assertEquals("[POST, PATCH, GET, DELETE, PUT, OPTIONS]", res.getHeaders().getAccessControlAllowMethods().toString());
        Assert.assertEquals("[authorization, x-requested-with]", res.getHeaders().getAccessControlAllowHeaders().toString());
        Assert.assertEquals(86400, res.getHeaders().getAccessControlMaxAge());
    }

    private ResponseEntity<?> sendValidCorsForNonTokenUri(String uri, HttpMethod method) {
        /**
         * origin etc restricted headers will not be set by HttpUrlConnection,
         * ref:https://stackoverflow.com/questions/41699608/resttemplate-not-passing-origin-header
         */
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        HttpHeaders headers = new HttpHeaders();
        headers.add("Origin", thirdPartyOrigin);
        headers.add("Access-Control-Allow-Origin", "http://localhost:4300");
        headers.add("Access-Control-Request-Method", method.toString());
        headers.add("Access-Control-Request-Headers", "authorization");
        HttpEntity<String> request = new HttpEntity<>(headers);
        return action.restTemplate.exchange(uri, HttpMethod.OPTIONS, request, String.class);

    }

    private void corsAssertNonToken(ResponseEntity res) {
        Assert.assertEquals(HttpStatus.OK, res.getStatusCode());
        Assert.assertEquals("http://localhost:4300", res.getHeaders().getAccessControlAllowOrigin());
        Assert.assertEquals("[POST, PATCH, GET, DELETE, PUT, OPTIONS]", res.getHeaders().getAccessControlAllowMethods().toString());
        Assert.assertEquals("[authorization]", res.getHeaders().getAccessControlAllowHeaders().toString());
        Assert.assertEquals(86400, res.getHeaders().getAccessControlMaxAge());
    }

}
