package com.hw.integration.identityaccess.oauth2;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static com.hw.helper.UserAction.*;
import static com.hw.integration.identityaccess.oauth2.BIzUserTest.RESOURCE_OWNER;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class RefreshTokenTest {
    public ObjectMapper mapper = new ObjectMapper().configure(MapperFeature.USE_ANNOTATIONS, false);
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
    public void refresh_token_should_work() throws InterruptedException {
        //create client supports refresh token
        Client clientRaw = action.getClientRaw();
        String clientSecret = clientRaw.getClientSecret();
        HashSet<GrantTypeEnum> enums = new HashSet<>();
        enums.add(GrantTypeEnum.PASSWORD);
        enums.add(GrantTypeEnum.REFRESH_TOKEN);
        clientRaw.setResourceIds(Collections.singleton(CLIENT_ID_OAUTH2_ID));
        clientRaw.setGrantTypeEnums(enums);
        clientRaw.setGrantedAuthorities(List.of(ClientAuthorityEnum.ROLE_FRONTEND));
        HashSet<ScopeEnum> scopes = new HashSet<>();
        scopes.add(ScopeEnum.TRUST);
        clientRaw.setScopeEnums(scopes);
        clientRaw.setAccessTokenValiditySeconds(1);
        clientRaw.setRefreshTokenValiditySeconds(1000);
        ResponseEntity<String> client = action.createClient(clientRaw);
        String clientId = client.getHeaders().getLocation().toString();
        Assert.assertEquals(HttpStatus.OK, client.getStatusCode());
        //get jwt
        ResponseEntity<DefaultOAuth2AccessToken> jwtPasswordWithClient = action.getJwtPasswordWithClient(clientId, clientSecret, ACCOUNT_USERNAME_ADMIN, ACCOUNT_PASSWORD_ADMIN);
        Assert.assertEquals(HttpStatus.OK, jwtPasswordWithClient.getStatusCode());
        //access endpoint
        String url = UserAction.proxyUrl + UserAction.SVC_NAME_AUTH + RESOURCE_OWNER + UserAction.ACCESS_ROLE_ADMIN;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtPasswordWithClient.getBody().getValue());
        HttpEntity<String> request = new HttpEntity<>(null, headers);
        ResponseEntity<SumTotalUser> exchange = action.restTemplate.exchange(url, HttpMethod.GET, request, SumTotalUser.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
        Thread.sleep(60000+2000);//spring cloud gateway add 60S leeway
        //access access token should expire
        ResponseEntity<SumTotalUser> exchange2 = action.restTemplate.exchange(url, HttpMethod.GET, request, SumTotalUser.class);
        Assert.assertEquals(HttpStatus.UNAUTHORIZED, exchange2.getStatusCode());
        //get access token with refresh token
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "refresh_token");
        params.add("refresh_token", jwtPasswordWithClient.getBody().getRefreshToken().getValue());
        HttpHeaders headers2 = new HttpHeaders();
        headers2.setBasicAuth(clientId, clientSecret);
        headers2.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, String>> request2 = new HttpEntity<>(params, headers2);
        ResponseEntity<DefaultOAuth2AccessToken> exchange1 = action.restTemplate.exchange(PROXY_URL_TOKEN, HttpMethod.POST, request2, DefaultOAuth2AccessToken.class);
        Assert.assertEquals(HttpStatus.OK, exchange1.getStatusCode());
        //use new access token for api call
        HttpHeaders headers3 = new HttpHeaders();
        headers3.setBearerAuth(exchange1.getBody().getValue());
        HttpEntity<String> request3 = new HttpEntity<>(null, headers3);
        ResponseEntity<SumTotalUser> exchange3 = action.restTemplate.exchange(url, HttpMethod.GET, request3, SumTotalUser.class);
        Assert.assertEquals(HttpStatus.OK, exchange3.getStatusCode());
    }


}


































