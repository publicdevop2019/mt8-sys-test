package com.hw.integration.oauth2;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.*;

import static com.hw.helper.UserAction.ACCESS_ROLE_ROOT;

@RunWith(SpringRunner.class)
@Slf4j
@SpringBootTest
public class ClientControllerTest {

    public static final String CLIENTS = "/clients";
    private String password = "password";
    private String valid_clientId = "838330249904133";
    private String valid_resourceId = "838330249904139";
    private String invalid_resourceId = "test-id";
    private String invalid_resourceId_not_found = "test-idasdf";
    private String valid_empty_secret = "";
    private String valid_username_root = "haolinwei2015@gmail.com";
    private String valid_username_admin = "haolinwei2017@gmail.com";
    private String valid_pwd = "root";
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
    public void only_client_w_first_party_n_backend_role_can_be_create_as_resource() throws JsonProcessingException {
        Client client = getInvalidClientAsResource();
        ResponseEntity<String> exchange = createClient(client);

        Assert.assertEquals(HttpStatus.BAD_REQUEST, exchange.getStatusCode());
    }

    @Test
    public void create_non_resource_client_with_valid_resource_ids_then_able_to_use_this_client_to_login() throws JsonProcessingException {
        Client client = getClientAsNonResource(valid_resourceId);
        ResponseEntity<String> exchange = createClient(client);

        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
        Assert.assertNotNull(exchange.getHeaders().getLocation());

        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse1 = getTokenResponse(password, valid_username_root, valid_pwd, exchange.getHeaders().getLocation().toString(), client.getClientSecret());

        Assert.assertEquals(HttpStatus.OK, tokenResponse1.getStatusCode());
        Assert.assertNotNull(tokenResponse1.getBody().getValue());
    }

    @Test
    public void create_client_which_is_resource_itself_with_valid_resource_ids_then_able_to_use_this_client_to_login() throws JsonProcessingException {
        Client client = getClientAsResource(valid_resourceId);
        ResponseEntity<String> exchange = createClient(client);

        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
        Assert.assertNotNull(exchange.getHeaders().getLocation());

        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse1 = getTokenResponse(password, valid_username_root, valid_pwd, exchange.getHeaders().getLocation().toString(), client.getClientSecret());

        Assert.assertEquals(HttpStatus.OK, tokenResponse1.getStatusCode());
        Assert.assertNotNull(tokenResponse1.getBody().getValue());
    }

    @Test
    public void should_not_able_to_create_client_which_is_resource_itself_with_wrong_resource_ids() throws JsonProcessingException {
        Client client = getClientAsResource(invalid_resourceId);
        ResponseEntity<String> exchange = createClient(client);
        Assert.assertEquals(HttpStatus.BAD_REQUEST, exchange.getStatusCode());
    }

    @Test
    public void should_not_able_to_create_client_which_is_resource_itself_with_wrong_not_existing_resource_ids() throws JsonProcessingException {
        Client client = getClientAsNonResource(invalid_resourceId_not_found);
        ResponseEntity<String> exchange = createClient(client);
        Assert.assertEquals(HttpStatus.BAD_REQUEST, exchange.getStatusCode());
    }

    @Test
    public void root_account_can_create_client() {
        String url = UserAction.proxyUrl + UserAction.SVC_NAME_AUTH + CLIENTS + ACCESS_ROLE_ROOT;
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(password, valid_username_root, valid_pwd, valid_clientId, valid_empty_secret);
        String bearer = tokenResponse.getBody().getValue();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearer);
        HttpEntity<String> request = new HttpEntity<>(null, headers);
        ResponseEntity<SumTotalClient> exchange = action.restTemplate.exchange(url, HttpMethod.GET, request, SumTotalClient.class);
        Assert.assertNotSame(0, exchange.getBody().getData().size());
    }

    @Test
    public void create_client_then_replace_it_with_different_client_only_password_is_empty_then_login_with_new_client_but_password_should_be_old_one() throws JsonProcessingException {
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(password, valid_username_root, valid_pwd, valid_clientId, valid_empty_secret);
        String bearer = tokenResponse.getBody().getValue();
        Client oldClient = getClientAsNonResource(valid_resourceId);
        ResponseEntity<String> client1 = createClient(oldClient);
        String url = UserAction.proxyUrl + UserAction.SVC_NAME_AUTH + CLIENTS + ACCESS_ROLE_ROOT + "/" + client1.getHeaders().getLocation().toString();
        Client newClient = getClientAsNonResource(valid_resourceId);
        newClient.setClientSecret(" ");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(bearer);
        String s1 = mapper.writeValueAsString(newClient);
        HttpEntity<String> request = new HttpEntity<>(s1, headers);
        ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());

        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse1 = getTokenResponse(password, valid_username_root, valid_pwd,client1.getHeaders().getLocation().toString(), oldClient.getClientSecret());

        Assert.assertEquals(HttpStatus.OK, tokenResponse1.getStatusCode());
        Assert.assertNotNull(tokenResponse1.getBody().getValue());

    }

    @Test
    public void create_client_then_update_it_tobe_resource() throws JsonProcessingException {
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(password, valid_username_root, valid_pwd, valid_clientId, valid_empty_secret);
        String bearer = tokenResponse.getBody().getValue();
        Client oldClient = getClientAsResource(valid_resourceId);
        ResponseEntity<String> client1 = createClient(oldClient);
        String url = UserAction.proxyUrl + UserAction.SVC_NAME_AUTH + CLIENTS + ACCESS_ROLE_ROOT + "/" + client1.getHeaders().getLocation().toString();
        String clientSecret = oldClient.getClientSecret();
        oldClient.setResourceIndicator(true);
        oldClient.setClientSecret(" ");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(bearer);
        String s1 = mapper.writeValueAsString(oldClient);
        HttpEntity<String> request = new HttpEntity<>(s1, headers);
        ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());

        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse1 = getTokenResponse(password, valid_username_root, valid_pwd, client1.getHeaders().getLocation().toString(), clientSecret);

        Assert.assertEquals(HttpStatus.OK, tokenResponse1.getStatusCode());
        Assert.assertNotNull(tokenResponse1.getBody().getValue());

    }

    @Test
    public void should_not_be_able_to_create_client_then_replace_it_with_different_client_which_cannot_be_resource() throws JsonProcessingException {
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(password, valid_username_root, valid_pwd, valid_clientId, valid_empty_secret);
        String bearer = tokenResponse.getBody().getValue();
        Client oldClient = getClientAsNonResource(valid_resourceId);
        ResponseEntity<String> client1 = createClient(oldClient);
        String url = UserAction.proxyUrl + UserAction.SVC_NAME_AUTH + CLIENTS + ACCESS_ROLE_ROOT + "/" + client1.getHeaders().getLocation().toString();
        Client newClient = getInvalidClientAsResource(valid_resourceId);
        newClient.setClientSecret(" ");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(bearer);
        String s1 = mapper.writeValueAsString(newClient);
        HttpEntity<String> request = new HttpEntity<>(s1, headers);
        ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
        Assert.assertEquals(HttpStatus.BAD_REQUEST, exchange.getStatusCode());

    }

    @Test
    public void create_client_then_update_it_secret() throws JsonProcessingException {
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(password, valid_username_root, valid_pwd, valid_clientId, valid_empty_secret);
        String bearer = tokenResponse.getBody().getValue();
        Client oldClient = getClientAsNonResource(valid_resourceId);
        ResponseEntity<String> client1 = createClient(oldClient);
        String url = UserAction.proxyUrl + UserAction.SVC_NAME_AUTH + CLIENTS + ACCESS_ROLE_ROOT + "/" + client1.getHeaders().getLocation().toString();
        Client newClient = getClientAsNonResource(valid_resourceId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(bearer);
        String s1 = mapper.writeValueAsString(newClient);
        HttpEntity<String> request = new HttpEntity<>(s1, headers);
        ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());

        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse1 = getTokenResponse(password, valid_username_root, valid_pwd, client1.getHeaders().getLocation().toString(), newClient.getClientSecret());

        Assert.assertEquals(HttpStatus.OK, tokenResponse1.getStatusCode());
        Assert.assertNotNull(tokenResponse1.getBody().getValue());
    }

    @Test
    public void delete_client() throws JsonProcessingException {
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(password, valid_username_root, valid_pwd, valid_clientId, valid_empty_secret);
        String bearer = tokenResponse.getBody().getValue();
        Client oldClient = getClientAsNonResource(valid_resourceId);
        ResponseEntity<String> client1 = createClient(oldClient);
        String url = UserAction.proxyUrl + UserAction.SVC_NAME_AUTH + CLIENTS + ACCESS_ROLE_ROOT + "/" + client1.getHeaders().getLocation().toString();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearer);
        HttpEntity<String> request = new HttpEntity<>(null, headers);
        ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.DELETE, request, String.class);

        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());

        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse1 = getTokenResponse(password, valid_username_root, valid_pwd, client1.getHeaders().getLocation().toString(), oldClient.getClientSecret());

        Assert.assertEquals(HttpStatus.UNAUTHORIZED, tokenResponse1.getStatusCode());
    }

    @Test
    public void root_client_is_not_deletable() throws JsonProcessingException {
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(password, valid_username_root, valid_pwd, valid_clientId, valid_empty_secret);
        String bearer = tokenResponse.getBody().getValue();
        Client oldClient = getClientAsNonResource(valid_resourceId);
        /**
         * add ROLE_ROOT so it can not be deleted
         */
        oldClient.setGrantedAuthorities(Arrays.asList(ClientAuthorityEnum.ROLE_BACKEND, ClientAuthorityEnum.ROLE_ROOT));
        ResponseEntity<String> client1 = createClient(oldClient);

        String url = UserAction.proxyUrl + UserAction.SVC_NAME_AUTH + CLIENTS + ACCESS_ROLE_ROOT + "/" + client1.getHeaders().getLocation().toString();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearer);
        HttpEntity<String> request = new HttpEntity<>(null, headers);
        ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.DELETE, request, String.class);

        Assert.assertEquals(HttpStatus.BAD_REQUEST, exchange.getStatusCode());

        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse1 = getTokenResponse(password, valid_username_root, valid_pwd, client1.getHeaders().getLocation().toString(), oldClient.getClientSecret());

        Assert.assertEquals(HttpStatus.OK, tokenResponse1.getStatusCode());
    }


    private ResponseEntity<DefaultOAuth2AccessToken> getTokenResponse(String grantType, String username, String userPwd, String clientId, String clientSecret) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", grantType);
        params.add("username", username);
        params.add("password", userPwd);
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        return action.restTemplate.exchange(UserAction.PROXY_URL_TOKEN, HttpMethod.POST, request, DefaultOAuth2AccessToken.class);
    }

    /**
     * @return different password client obj
     */
    private Client getClientAsNonResource(String... resourceIds) {
        Client client = getClientRaw(resourceIds);
        client.setGrantedAuthorities(Collections.singletonList(ClientAuthorityEnum.ROLE_BACKEND));
        client.setResourceIndicator(false);
        return client;
    }

    private Client getClientAsResource(String... resourceIds) {
        Client client = getClientRaw(resourceIds);
        client.setGrantedAuthorities(Arrays.asList(ClientAuthorityEnum.ROLE_BACKEND,ClientAuthorityEnum.ROLE_FIRST_PARTY));
        client.setResourceIndicator(true);
        return client;
    }

    private Client getInvalidClientAsResource(String... resourceIds) {
        Client client = getClientRaw(resourceIds);
        client.setGrantedAuthorities(Arrays.asList(ClientAuthorityEnum.ROLE_THIRD_PARTY));
        client.setResourceIndicator(true);
        return client;
    }

    private Client getClientRaw(String... resourceIds) {
        Client client = new Client();
        client.setClientSecret(UUID.randomUUID().toString().replace("-", ""));
        client.setGrantTypeEnums(new HashSet<>(Arrays.asList(GrantTypeEnum.PASSWORD)));
        client.setScopeEnums(new HashSet<>(Arrays.asList(ScopeEnum.READ)));
        client.setAccessTokenValiditySeconds(1800);
        client.setRefreshTokenValiditySeconds(null);
        client.setHasSecret(true);
        client.setResourceIds(new HashSet<>(Arrays.asList(resourceIds)));
        return client;
    }

    public ResponseEntity<String> createClient(Client client) throws JsonProcessingException {
        String url = UserAction.proxyUrl + UserAction.SVC_NAME_AUTH + CLIENTS + ACCESS_ROLE_ROOT;
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(password, valid_username_root, valid_pwd, valid_clientId, valid_empty_secret);
        String bearer = tokenResponse.getBody().getValue();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(bearer);
        String s = mapper.writeValueAsString(client);
        HttpEntity<String> request = new HttpEntity<>(s, headers);
        return action.restTemplate.exchange(url, HttpMethod.POST, request, String.class);
    }
}