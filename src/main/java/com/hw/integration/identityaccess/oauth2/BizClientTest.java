package com.hw.integration.identityaccess.oauth2;

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

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static com.hw.helper.UserAction.*;

@RunWith(SpringRunner.class)
@Slf4j
@SpringBootTest
public class BizClientTest {

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
        Client client = action.getInvalidClientAsResource();
        ResponseEntity<String> exchange = action.createClient(client);

        Assert.assertEquals(HttpStatus.BAD_REQUEST, exchange.getStatusCode());
    }

    @Test
    public void create_non_resource_client_with_valid_resource_ids_then_able_to_use_this_client_to_login() throws JsonProcessingException {
        Client client = action.getClientAsNonResource(CLIENT_ID_RESOURCE_ID);
        ResponseEntity<String> exchange = action.createClient(client);

        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
        Assert.assertNotNull(exchange.getHeaders().getLocation());

        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse1 = action.getTokenResponse(GRANT_TYPE_PASSWORD, ACCOUNT_USERNAME_ROOT, ACCOUNT_PASSWORD_ROOT, exchange.getHeaders().getLocation().toString(), client.getClientSecret());

        Assert.assertEquals(HttpStatus.OK, tokenResponse1.getStatusCode());
        Assert.assertNotNull(tokenResponse1.getBody().getValue());
    }

    @Test
    public void create_client_which_is_resource_itself_with_valid_resource_ids_then_able_to_use_this_client_to_login() throws JsonProcessingException {
        Client client = action.getClientAsResource(CLIENT_ID_RESOURCE_ID);
        ResponseEntity<String> exchange = action.createClient(client);

        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
        Assert.assertNotNull(exchange.getHeaders().getLocation());

        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse1 = action.getTokenResponse(GRANT_TYPE_PASSWORD, ACCOUNT_USERNAME_ROOT, ACCOUNT_PASSWORD_ROOT, exchange.getHeaders().getLocation().toString(), client.getClientSecret());

        Assert.assertEquals(HttpStatus.OK, tokenResponse1.getStatusCode());
        Assert.assertNotNull(tokenResponse1.getBody().getValue());
    }

    @Test
    public void should_not_able_to_create_client_which_is_resource_itself_with_wrong_resource_ids() throws JsonProcessingException {
        Client client = action.getClientAsResource(CLIENT_ID_TEST_ID);
        ResponseEntity<String> exchange = action.createClient(client);
        Assert.assertEquals(HttpStatus.BAD_REQUEST, exchange.getStatusCode());
    }

    @Test
    public void should_not_able_to_create_client_which_is_resource_itself_with_wrong_not_existing_resource_ids() throws JsonProcessingException {
        Client client = action.getClientAsNonResource(UUID.randomUUID().toString());
        ResponseEntity<String> exchange = action.createClient(client);
        Assert.assertEquals(HttpStatus.BAD_REQUEST, exchange.getStatusCode());
    }

    @Test
    public void root_account_can_read_client() {
        String url = UserAction.proxyUrl + UserAction.SVC_NAME_AUTH + CLIENTS + ACCESS_ROLE_ROOT;
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = action.getJwtPassword(ACCOUNT_USERNAME_ROOT, ACCOUNT_PASSWORD_ROOT);
        String bearer = tokenResponse.getBody().getValue();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearer);
        HttpEntity<String> request = new HttpEntity<>(null, headers);
        ResponseEntity<SumTotalClient> exchange = action.restTemplate.exchange(url, HttpMethod.GET, request, SumTotalClient.class);
        Assert.assertNotSame(0, exchange.getBody().getData().size());
    }

    @Test
    public void create_client_then_replace_it_with_different_client_only_password_is_empty_then_login_with_new_client_but_password_should_be_old_one() {
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = action.getJwtPassword(ACCOUNT_USERNAME_ROOT, ACCOUNT_PASSWORD_ROOT);
        String bearer = tokenResponse.getBody().getValue();
        Client oldClient = action.getClientAsNonResource(CLIENT_ID_RESOURCE_ID);
        ResponseEntity<String> client1 = action.createClient(oldClient);
        String url = UserAction.proxyUrl + UserAction.SVC_NAME_AUTH + CLIENTS + ACCESS_ROLE_ROOT + "/" + client1.getHeaders().getLocation().toString();
        Client newClient = action.getClientAsNonResource(CLIENT_ID_RESOURCE_ID);
        newClient.setClientSecret(" ");
        newClient.setVersion(0);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(bearer);
        HttpEntity<Client> request = new HttpEntity<>(newClient, headers);
        ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());

        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse1 = action.getTokenResponse(GRANT_TYPE_PASSWORD, ACCOUNT_USERNAME_ROOT, ACCOUNT_PASSWORD_ROOT, client1.getHeaders().getLocation().toString(), oldClient.getClientSecret());

        Assert.assertEquals(HttpStatus.OK, tokenResponse1.getStatusCode());
        Assert.assertNotNull(tokenResponse1.getBody().getValue());

    }

    @Test
    public void create_client_then_replace_it_with_same_client_info_only_password_is_empty_N_times_then_client_version_should_not_increase() {
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = action.getJwtPassword(ACCOUNT_USERNAME_ROOT, ACCOUNT_PASSWORD_ROOT);
        String bearer = tokenResponse.getBody().getValue();
        Client oldClient = action.getClientAsNonResource(CLIENT_ID_RESOURCE_ID);
        ResponseEntity<String> client1 = action.createClient(oldClient);
        String url = UserAction.proxyUrl + UserAction.SVC_NAME_AUTH + CLIENTS + ACCESS_ROLE_ROOT + "/" + client1.getHeaders().getLocation().toString();
        oldClient.setClientSecret(" ");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(bearer);
        HttpEntity<Client> request = new HttpEntity<>(oldClient, headers);
        ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
        ResponseEntity<String> exchange2 = action.restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
        Assert.assertEquals(HttpStatus.OK, exchange2.getStatusCode());
        ResponseEntity<Client> exchange1 = action.restTemplate.exchange(url, HttpMethod.GET, request, Client.class);

        Assert.assertEquals(HttpStatus.OK, exchange1.getStatusCode());
        Assert.assertEquals(0, (int) exchange1.getBody().getVersion());
    }

    @Test
    public void create_client_then_update_it_tobe_resource() throws JsonProcessingException {
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = action.getJwtPassword(ACCOUNT_USERNAME_ROOT, ACCOUNT_PASSWORD_ROOT);
        String bearer = tokenResponse.getBody().getValue();
        Client oldClient = action.getClientAsResource(CLIENT_ID_RESOURCE_ID);
        ResponseEntity<String> client1 = action.createClient(oldClient);
        String url = UserAction.proxyUrl + UserAction.SVC_NAME_AUTH + CLIENTS + ACCESS_ROLE_ROOT + "/" + client1.getHeaders().getLocation().toString();
        String clientSecret = oldClient.getClientSecret();
        oldClient.setResourceIndicator(true);
        oldClient.setClientSecret(" ");
        oldClient.setVersion(0);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(bearer);
        HttpEntity<Client> request = new HttpEntity<>(oldClient, headers);
        ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());

        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse1 = action.getTokenResponse(GRANT_TYPE_PASSWORD, ACCOUNT_USERNAME_ROOT, ACCOUNT_PASSWORD_ROOT, client1.getHeaders().getLocation().toString(), clientSecret);

        Assert.assertEquals(HttpStatus.OK, tokenResponse1.getStatusCode());
        Assert.assertNotNull(tokenResponse1.getBody().getValue());

    }

    @Test
    public void should_not_be_able_to_create_client_then_replace_it_with_different_client_which_cannot_be_resource() throws JsonProcessingException {
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = action.getJwtPassword(ACCOUNT_USERNAME_ROOT, ACCOUNT_PASSWORD_ROOT);
        String bearer = tokenResponse.getBody().getValue();
        Client oldClient = action.getClientAsNonResource(CLIENT_ID_RESOURCE_ID);
        ResponseEntity<String> client1 = action.createClient(oldClient);
        String url = UserAction.proxyUrl + UserAction.SVC_NAME_AUTH + CLIENTS + ACCESS_ROLE_ROOT + "/" + client1.getHeaders().getLocation().toString();
        Client newClient = action.getInvalidClientAsResource(CLIENT_ID_RESOURCE_ID);
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
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = action.getJwtPassword(ACCOUNT_USERNAME_ROOT, ACCOUNT_PASSWORD_ROOT);
        String bearer = tokenResponse.getBody().getValue();
        Client oldClient = action.getClientAsNonResource(CLIENT_ID_RESOURCE_ID);
        ResponseEntity<String> client1 = action.createClient(oldClient);
        String url = UserAction.proxyUrl + UserAction.SVC_NAME_AUTH + CLIENTS + ACCESS_ROLE_ROOT + "/" + client1.getHeaders().getLocation().toString();
        Client newClient = action.getClientAsNonResource(CLIENT_ID_RESOURCE_ID);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(bearer);
        newClient.setVersion(0);
        HttpEntity<Client> request = new HttpEntity<>(newClient, headers);
        ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());

        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse1 = action.getTokenResponse(GRANT_TYPE_PASSWORD, ACCOUNT_USERNAME_ROOT, ACCOUNT_PASSWORD_ROOT, client1.getHeaders().getLocation().toString(), newClient.getClientSecret());

        Assert.assertEquals(HttpStatus.OK, tokenResponse1.getStatusCode());
        Assert.assertNotNull(tokenResponse1.getBody().getValue());
    }

    @Test
    public void delete_client() {
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = action.getJwtPassword(ACCOUNT_USERNAME_ROOT, ACCOUNT_PASSWORD_ROOT);
        String bearer = tokenResponse.getBody().getValue();
        Client oldClient = action.getClientAsNonResource(CLIENT_ID_RESOURCE_ID);
        ResponseEntity<String> client1 = action.createClient(oldClient);
        String url = UserAction.proxyUrl + UserAction.SVC_NAME_AUTH + CLIENTS + ACCESS_ROLE_ROOT + "/" + client1.getHeaders().getLocation().toString();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearer);
        HttpEntity<String> request = new HttpEntity<>(null, headers);
        ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.DELETE, request, String.class);

        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());

        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse1 = action.getTokenResponse(GRANT_TYPE_PASSWORD, ACCOUNT_USERNAME_ROOT, ACCOUNT_PASSWORD_ROOT, client1.getHeaders().getLocation().toString(), oldClient.getClientSecret());

        Assert.assertEquals(HttpStatus.UNAUTHORIZED, tokenResponse1.getStatusCode());
    }

    @Test
    public void root_client_is_not_deletable() throws JsonProcessingException {
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = action.getJwtPassword(ACCOUNT_USERNAME_ROOT, ACCOUNT_PASSWORD_ROOT);
        String bearer = tokenResponse.getBody().getValue();
        Client oldClient = action.getClientAsNonResource(CLIENT_ID_RESOURCE_ID);
        /**
         * add ROLE_ROOT so it can not be deleted
         */
        oldClient.setGrantedAuthorities(Arrays.asList(ClientAuthorityEnum.ROLE_BACKEND, ClientAuthorityEnum.ROLE_ROOT));
        ResponseEntity<String> client1 = action.createClient(oldClient);

        String url = UserAction.proxyUrl + UserAction.SVC_NAME_AUTH + CLIENTS + ACCESS_ROLE_ROOT + "/" + client1.getHeaders().getLocation().toString();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearer);
        HttpEntity<String> request = new HttpEntity<>(null, headers);
        ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.DELETE, request, String.class);

        Assert.assertEquals(HttpStatus.BAD_REQUEST, exchange.getStatusCode());

        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse1 = action.getTokenResponse(GRANT_TYPE_PASSWORD, ACCOUNT_USERNAME_ROOT, ACCOUNT_PASSWORD_ROOT, client1.getHeaders().getLocation().toString(), oldClient.getClientSecret());

        Assert.assertEquals(HttpStatus.OK, tokenResponse1.getStatusCode());
    }

}