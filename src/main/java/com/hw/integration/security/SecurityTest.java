package com.hw.integration.security;

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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;
import java.util.UUID;

@RunWith(SpringRunner.class)
@Slf4j
@SpringBootTest
public class SecurityTest {
    @Autowired
    UserAction action;
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
    public void user_modify_jwt_token_after_login() {
        String defaultUserToken = action.registerResourceOwnerThenLogin();
        String defaultUserToken2 = action.registerResourceOwnerThenLogin();
        String profileId1 = action.getProfileId(defaultUserToken);
        String url = UserAction.proxyUrl + UserAction.PROFILE_SVC + "/profiles/" + profileId1 + "/addresses";
        ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.GET, action.getHttpRequest(defaultUserToken2), String.class);
        Assert.assertEquals(HttpStatus.BAD_REQUEST, exchange.getStatusCode());
    }

    @Test
    public void use_modify_jwt_token_after_login_trying_to_access_other_profile() {
        String defaultUserToken = action.registerResourceOwnerThenLogin();
        String defaultUserToken2 = action.registerResourceOwnerThenLogin();
        String profileId2 = action.getProfileId(defaultUserToken2);
        String url = UserAction.proxyUrl + UserAction.PROFILE_SVC + "/profiles/" + profileId2 + "/addresses";
        ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.GET, action.getHttpRequest(defaultUserToken), String.class);
        Assert.assertEquals(HttpStatus.BAD_REQUEST, exchange.getStatusCode());
    }

    @Test
    public void trying_access_protected_api_without_jwt_token() {
        String defaultUserToken = action.registerResourceOwnerThenLogin();
        String profileId1 = action.getProfileId(defaultUserToken);
        String url = UserAction.proxyUrl + UserAction.PROFILE_SVC + "/profiles/" + profileId1 + "/addresses";
        ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.GET, action.getHttpRequest(null), String.class);
        Assert.assertEquals(HttpStatus.UNAUTHORIZED, exchange.getStatusCode());
    }


}
