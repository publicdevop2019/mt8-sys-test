package com.hw.integration.identityaccess.proxy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hw.helper.Client;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static com.hw.helper.UserAction.*;
import static com.hw.integration.identityaccess.proxy.EndpointTest.createProfile;
import static com.hw.integration.identityaccess.proxy.EndpointTest.readProfile;
import static com.hw.integration.identityaccess.proxy.RevokeTokenTest.USERS_ADMIN;

@RunWith(SpringRunner.class)
@Slf4j
@SpringBootTest
public class GatewayFilterTest {
    public ObjectMapper mapper = new ObjectMapper().configure(MapperFeature.USE_ANNOTATIONS, false).setSerializationInclusion(JsonInclude.Include.NON_NULL);
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
    public void should_get_etag_for_get_resources() {
        String url2 = UserAction.proxyUrl + UserAction.SVC_NAME_AUTH + USERS_ADMIN;
        ResponseEntity<DefaultOAuth2AccessToken> pwdTokenResponse = action.getJwtPasswordAdmin();
        String bearer0 = pwdTokenResponse.getBody().getValue();
        HttpHeaders headers1 = new HttpHeaders();
        headers1.setBearerAuth(bearer0);
        HttpEntity<Object> hashMapHttpEntity1 = new HttpEntity<>(headers1);
        ResponseEntity<String> exchange2 = action.restTemplate.exchange(url2, HttpMethod.GET, hashMapHttpEntity1, String.class);
        String eTag = exchange2.getHeaders().getETag();
        Assert.assertNotNull(eTag);
    }

    @Test
    public void should_get_gzip_for_get_resources() {
        String url2 = UserAction.proxyUrl + UserAction.SVC_NAME_AUTH + USERS_ADMIN;
        ResponseEntity<DefaultOAuth2AccessToken> pwdTokenResponse = action.getJwtPasswordRoot();
        String bearer0 = pwdTokenResponse.getBody().getValue();
        HttpHeaders headers1 = new HttpHeaders();
        headers1.setBearerAuth(bearer0);
        HttpEntity<Object> hashMapHttpEntity1 = new HttpEntity<>(headers1);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> exchange2 = restTemplate.exchange(url2, HttpMethod.GET, hashMapHttpEntity1, String.class);
        String eTag = exchange2.getHeaders().get("Content-Encoding").get(0);
        Assert.assertEquals("gzip", eTag);
    }

    @Test
    public void should_get_302_when_etag_present() {
        String url2 = UserAction.proxyUrl + UserAction.SVC_NAME_AUTH + USERS_ADMIN;
        ResponseEntity<DefaultOAuth2AccessToken> pwdTokenResponse = action.getJwtPasswordAdmin();
        String bearer0 = pwdTokenResponse.getBody().getValue();
        HttpHeaders headers1 = new HttpHeaders();
        headers1.setBearerAuth(bearer0);
        HttpEntity<Object> hashMapHttpEntity1 = new HttpEntity<>(headers1);
        ResponseEntity<String> exchange2 = action.restTemplate.exchange(url2, HttpMethod.GET, hashMapHttpEntity1, String.class);
        String eTag = exchange2.getHeaders().getETag();
        headers1.setIfNoneMatch(eTag);
        HttpEntity<Object> hashMapHttpEntity2 = new HttpEntity<>(headers1);
        ResponseEntity<String> exchange3 = action.restTemplate.exchange(url2, HttpMethod.GET, hashMapHttpEntity2, String.class);
        Assert.assertEquals(HttpStatus.NOT_MODIFIED, exchange3.getStatusCode());
    }

    @Test
    public void should_sanitize_request_json() {
        SecurityProfile securityProfile1 = new SecurityProfile();
        securityProfile1.setResourceId("0C8AZTODP4HT");
        securityProfile1.setUserRoles(new HashSet<>(List.of("ROLE_ADMIN")));
        securityProfile1.setClientRoles(new HashSet<>(List.of("TRUST")));
        securityProfile1.setUserOnly(true);
        securityProfile1.setMethod("GET");
        securityProfile1.setDescription("<script>test</script>");
        securityProfile1.setPath("/test/" + UUID.randomUUID().toString().replace("-", "").replaceAll("\\d", "") + "/abc");
        ResponseEntity<String> profile = createProfile(securityProfile1, action);
        String s = profile.getHeaders().getLocation().toString();
        Assert.assertEquals(HttpStatus.OK, profile.getStatusCode());
        ResponseEntity<SecurityProfile> securityProfileResponseEntity = readProfile(action, s);
        Assert.assertEquals("&lt;script&gt;test&lt;/script&gt;", securityProfileResponseEntity.getBody().getDescription());
    }

    @Test
    public void should_sanitize_response_json() {
        String url = UserAction.proxyUrl + UserAction.SVC_NAME_AUTH + CLIENTS + ACCESS_ROLE_ROOT + "/" + CLIENT_ID_RIGHT_ROLE_NOT_SUFFICIENT_RESOURCE_ID;
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = action.getJwtPassword(ACCOUNT_USERNAME_ROOT, ACCOUNT_PASSWORD_ROOT);
        String bearer = tokenResponse.getBody().getValue();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearer);
        HttpEntity<String> request = new HttpEntity<>(null, headers);
        ResponseEntity<Client> exchange = action.restTemplate.exchange(url, HttpMethod.GET, request, Client.class);
        Assert.assertEquals("&lt;script&gt;test&lt;/script&gt;", exchange.getBody().getDescription());
    }

    @Test
    public void should_allow_public_access() {
        String url = UserAction.proxyUrl + UserAction.SVC_NAME_FILE_UPLOAD + FILES + ACCESS_ROLE_PUBLIC + "/" + "845181169475584";
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> request = new HttpEntity<>(null, headers);
        ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.GET, request, String.class);
        Assert.assertNotEquals(HttpStatus.FORBIDDEN, exchange.getStatusCode());
    }
}
