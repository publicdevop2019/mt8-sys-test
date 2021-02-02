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
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static com.hw.concurrent.ProductServiceTest.assertConcurrent;
import static com.hw.helper.UserAction.*;
import static com.hw.integration.identityaccess.proxy.EndpointTest.*;
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
    public void should_get_cache_control_for_get_resources() {
        String url2 = UserAction.proxyUrl + UserAction.SVC_NAME_AUTH + USERS_ADMIN;
        ResponseEntity<DefaultOAuth2AccessToken> pwdTokenResponse = action.getJwtPasswordAdmin();
        String bearer0 = pwdTokenResponse.getBody().getValue();
        HttpHeaders headers1 = new HttpHeaders();
        headers1.setBearerAuth(bearer0);
        HttpEntity<Object> hashMapHttpEntity1 = new HttpEntity<>(headers1);
        ResponseEntity<String> exchange2 = action.restTemplate.exchange(url2, HttpMethod.GET, hashMapHttpEntity1, String.class);
        String cacheControl = exchange2.getHeaders().getCacheControl();
        Assert.assertNotNull(cacheControl);
        long expires = exchange2.getHeaders().getExpires();
        Assert.assertEquals(-1L,expires);
        String pragma = exchange2.getHeaders().getPragma();
        Assert.assertNull(pragma);
    }

    @Test
    public void should_get_gzip_for_get_resources() {
        String url2 = UserAction.proxyUrl + UserAction.SVC_NAME_AUTH + CLIENTS + ACCESS_ROLE_ROOT;
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
    public void should_get_304_when_etag_present() {
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

    @Test
    public void should_cut_off_api_when_max_limit_reach() {
        String url = UserAction.proxyUrl + SVC_NAME_TEST + "/test/delay/" + "15000";
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> request = new HttpEntity<>(null, headers);
        ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.GET, request, String.class);
        Assert.assertEquals(HttpStatus.GATEWAY_TIMEOUT, exchange.getStatusCode());
    }

    @Test
    public void should_has_no_response_body_when_500() {
        String url = UserAction.proxyUrl + SVC_NAME_TEST + "/test/status/500";
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> request = new HttpEntity<>(null, headers);
        ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.GET, request, String.class);
        Assert.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exchange.getStatusCode());
        Assert.assertNull(exchange.getBody());
    }

    @Test
    public void should_ask_for_csrf_token_when_post() {
        SecurityProfile securityProfile1 = new SecurityProfile();
        securityProfile1.setResourceId("0C8AZTODP4HT");
        securityProfile1.setUserRoles(new HashSet<>(List.of("ROLE_ADMIN")));
        securityProfile1.setClientRoles(new HashSet<>(List.of("TRUST")));
        securityProfile1.setUserOnly(true);
        securityProfile1.setMethod("GET");
        securityProfile1.setPath("/test/" + UUID.randomUUID().toString().replace("-", "").replaceAll("\\d", "") + "/abc");
        ResponseEntity<DefaultOAuth2AccessToken> pwdTokenResponse2 = action.getJwtPasswordRoot();
        String bearer1 = pwdTokenResponse2.getBody().getValue();
        String url = UserAction.proxyUrl + SVC_NAME_AUTH + ENDPOINTS + ACCESS_ROLE_ROOT;
        HttpHeaders headers1 = new HttpHeaders();
        headers1.setBearerAuth(bearer1);
        headers1.set("testId", uuid.toString());
        headers1.set("changeId", UUID.randomUUID().toString());
        TestRestTemplate restTemplate = new TestRestTemplate();
        HttpEntity<SecurityProfile> hashMapHttpEntity1 = new HttpEntity<>(securityProfile1, headers1);
        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.POST, hashMapHttpEntity1, String.class);
        Assert.assertEquals(HttpStatus.FORBIDDEN, exchange.getStatusCode());
        headers1.set("X-XSRF-TOKEN", "123");
        headers1.add(HttpHeaders.COOKIE, "XSRF-TOKEN=123");
        HttpEntity<SecurityProfile> hashMapHttpEntity2 = new HttpEntity<>(securityProfile1, headers1);
        ResponseEntity<String> exchange2 = restTemplate.exchange(url, HttpMethod.POST, hashMapHttpEntity2, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange2.getStatusCode());
    }

    @Test
    public void should_get_too_many_request_exceed_burst_rate_limit() {
        String url2 = UserAction.proxyUrl + UserAction.SVC_NAME_AUTH + CLIENTS + ACCESS_ROLE_ROOT;
        ResponseEntity<DefaultOAuth2AccessToken> pwdTokenResponse = action.getJwtPasswordRoot();
        String bearer0 = pwdTokenResponse.getBody().getValue();
        HttpHeaders headers1 = new HttpHeaders();
        headers1.setBearerAuth(bearer0);
        HttpEntity<Object> hashMapHttpEntity1 = new HttpEntity<>(headers1);
        AtomicReference<Integer> count = new AtomicReference<>(0);
        Runnable runnable2 = () -> {
            ResponseEntity<String> exchange = action.restTemplate.exchange(url2, HttpMethod.GET, hashMapHttpEntity1, String.class);
            if (exchange.getStatusCode().equals(HttpStatus.TOO_MANY_REQUESTS)) {
                count.getAndSet(count.get() + 1);
            }
        };
        ArrayList<Runnable> runnables = new ArrayList<>();
        runnables.add(runnable2);
        runnables.add(runnable2);
        runnables.add(runnable2);
        runnables.add(runnable2);
        runnables.add(runnable2);
        runnables.add(runnable2);
        runnables.add(runnable2);
        runnables.add(runnable2);
        runnables.add(runnable2);
        runnables.add(runnable2);
        runnables.add(runnable2);
        runnables.add(runnable2);
        try {
            assertConcurrent("", runnables, 30000);
            Assert.assertNotEquals(0, count.get().intValue());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
