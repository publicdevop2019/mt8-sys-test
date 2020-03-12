package com.hw.integration.oauth2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hw.helper.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.*;
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

import java.util.*;

@RunWith(SpringRunner.class)
@Slf4j
public class ClientControllerTest {

    private String password = "password";
    private String valid_clientId = "login-id";
    private String valid_resourceId = "resource-id";
    private String invalid_resourceId = "test-id";
    private String invalid_resourceId_not_found = "test-idasdf";
    private String valid_empty_secret = "";
    private String valid_username_root = "haolinwei2015@gmail.com";
    private String valid_username_admin = "haolinwei2017@gmail.com";
    private String valid_pwd = "root";
    public ObjectMapper mapper = new ObjectMapper().configure(MapperFeature.USE_ANNOTATIONS, false);
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
    public void all_client_should_have_resource_id_field() throws JsonProcessingException {
        Client client = getClientAsNonResource();
        ResponseEntity<String> exchange = createClient(client);
        Assert.assertEquals(HttpStatus.BAD_REQUEST, exchange.getStatusCode());
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

        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse1 = getTokenResponse(password, valid_username_root, valid_pwd, client.getClientId(), client.getClientSecret());

        Assert.assertEquals(HttpStatus.OK, tokenResponse1.getStatusCode());
        Assert.assertNotNull(tokenResponse1.getBody().getValue());
    }

    @Test
    public void create_client_which_is_resource_itself_with_valid_resource_ids_then_able_to_use_this_client_to_login() throws JsonProcessingException {
        Client client = getClientAsResource(valid_resourceId);
        ResponseEntity<String> exchange = createClient(client);

        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
        Assert.assertNotNull(exchange.getHeaders().getLocation());

        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse1 = getTokenResponse(password, valid_username_root, valid_pwd, client.getClientId(), client.getClientSecret());

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
    @Ignore
    public void when_bypass_proxy_even_admin_can_create_client_if_directly_call_oauth2() throws JsonProcessingException {
        Client client = getClientAsNonResource(valid_resourceId);
        String url = UserAction.proxyUrl + "/v1/api" + "/clients";
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(password, valid_username_admin, valid_pwd, valid_clientId, valid_empty_secret);
        String bearer = tokenResponse.getBody().getValue();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(bearer);
        String s = mapper.writeValueAsString(client);
        HttpEntity<String> request = new HttpEntity<>(s, headers);
        ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.POST, request, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
    }

    @Test
    public void root_account_can_create_client() {
        String url = UserAction.proxyUrl + "/api" + "/clients";
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(password, valid_username_root, valid_pwd, valid_clientId, valid_empty_secret);
        String bearer = tokenResponse.getBody().getValue();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearer);
        HttpEntity<String> request = new HttpEntity<>(null, headers);
        ParameterizedTypeReference<List<Client>> responseType = new ParameterizedTypeReference<>() {
        };
        ResponseEntity<List<Client>> exchange = action.restTemplate.exchange(url, HttpMethod.GET, request, responseType);
        Assert.assertNotSame(0, exchange.getBody().size());
    }

    @Test
    public void create_client_then_replace_it_with_different_client_only_password_is_empty_then_login_with_new_client_but_password_should_be_old_one() throws JsonProcessingException {
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(password, valid_username_root, valid_pwd, valid_clientId, valid_empty_secret);
        String bearer = tokenResponse.getBody().getValue();
        Client oldClient = getClientAsNonResource(valid_resourceId);
        ResponseEntity<String> client1 = createClient(oldClient);
        String url = UserAction.proxyUrl + "/api" + "/clients/" + client1.getHeaders().getLocation().toString();
        Client newClient = getClientAsNonResource(valid_resourceId);
        newClient.setClientSecret(" ");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(bearer);
        String s1 = mapper.writeValueAsString(newClient);
        HttpEntity<String> request = new HttpEntity<>(s1, headers);
        ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());

        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse1 = getTokenResponse(password, valid_username_root, valid_pwd, newClient.getClientId(), oldClient.getClientSecret());

        Assert.assertEquals(HttpStatus.OK, tokenResponse1.getStatusCode());
        Assert.assertNotNull(tokenResponse1.getBody().getValue());

    }

    @Test
    public void create_client_then_update_it_tobe_resource() throws JsonProcessingException {
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(password, valid_username_root, valid_pwd, valid_clientId, valid_empty_secret);
        String bearer = tokenResponse.getBody().getValue();
        Client oldClient = getClientAsResource(valid_resourceId);
        ResponseEntity<String> client1 = createClient(oldClient);
        String url = UserAction.proxyUrl + "/api" + "/clients/" + client1.getHeaders().getLocation().toString();
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

        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse1 = getTokenResponse(password, valid_username_root, valid_pwd, oldClient.getClientId(), clientSecret);

        Assert.assertEquals(HttpStatus.OK, tokenResponse1.getStatusCode());
        Assert.assertNotNull(tokenResponse1.getBody().getValue());

    }

    @Test
    public void should_not_be_able_to_create_client_then_replace_it_with_different_client_which_cannot_be_resource() throws JsonProcessingException {
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(password, valid_username_root, valid_pwd, valid_clientId, valid_empty_secret);
        String bearer = tokenResponse.getBody().getValue();
        Client oldClient = getClientAsNonResource(valid_resourceId);
        ResponseEntity<String> client1 = createClient(oldClient);
        String url = UserAction.proxyUrl + "/api" + "/clients/" + client1.getHeaders().getLocation().toString();
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
        String url = UserAction.proxyUrl + "/api" + "/clients/" + client1.getHeaders().getLocation().toString();
        Client newClient = getClientAsNonResource(valid_resourceId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(bearer);
        String s1 = mapper.writeValueAsString(newClient);
        HttpEntity<String> request = new HttpEntity<>(s1, headers);
        ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());

        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse1 = getTokenResponse(password, valid_username_root, valid_pwd, newClient.getClientId(), newClient.getClientSecret());

        Assert.assertEquals(HttpStatus.OK, tokenResponse1.getStatusCode());
        Assert.assertNotNull(tokenResponse1.getBody().getValue());
    }

    @Test
    public void delete_client() throws JsonProcessingException {
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(password, valid_username_root, valid_pwd, valid_clientId, valid_empty_secret);
        String bearer = tokenResponse.getBody().getValue();
        Client oldClient = getClientAsNonResource(valid_resourceId);
        ResponseEntity<String> client1 = createClient(oldClient);
        String url = UserAction.proxyUrl + "/api" + "/clients/" + client1.getHeaders().getLocation().toString();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearer);
        HttpEntity<String> request = new HttpEntity<>(null, headers);
        ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.DELETE, request, String.class);

        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());

        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse1 = getTokenResponse(password, valid_username_root, valid_pwd, oldClient.getClientId(), oldClient.getClientSecret());

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
        GrantedAuthorityImpl<ClientAuthorityEnum> clientAuthorityEnumGrantedAuthority = new GrantedAuthorityImpl<>();
        clientAuthorityEnumGrantedAuthority.setGrantedAuthority(ClientAuthorityEnum.ROLE_BACKEND);
        GrantedAuthorityImpl<ClientAuthorityEnum> clientAuthorityEnumGrantedAuthority2 = new GrantedAuthorityImpl<>();
        clientAuthorityEnumGrantedAuthority2.setGrantedAuthority(ClientAuthorityEnum.ROLE_ROOT);
        oldClient.setGrantedAuthorities(Arrays.asList(clientAuthorityEnumGrantedAuthority, clientAuthorityEnumGrantedAuthority2));
        ResponseEntity<String> client1 = createClient(oldClient);

        String url = UserAction.proxyUrl + "/api" + "/clients/" + client1.getHeaders().getLocation().toString();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearer);
        HttpEntity<String> request = new HttpEntity<>(null, headers);
        ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.DELETE, request, String.class);

        Assert.assertEquals(HttpStatus.BAD_REQUEST, exchange.getStatusCode());

        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse1 = getTokenResponse(password, valid_username_root, valid_pwd, oldClient.getClientId(), oldClient.getClientSecret());

        Assert.assertEquals(HttpStatus.OK, tokenResponse1.getStatusCode());
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

    /**
     * @return different password client obj
     */
    private Client getClientAsNonResource(String... resourceIds) {
        Client client = getClientRaw(resourceIds);
        GrantedAuthorityImpl<ClientAuthorityEnum> clientAuthorityEnumGrantedAuthority = new GrantedAuthorityImpl<>();
        clientAuthorityEnumGrantedAuthority.setGrantedAuthority(ClientAuthorityEnum.ROLE_BACKEND);
        client.setGrantedAuthorities(Arrays.asList(clientAuthorityEnumGrantedAuthority));
        client.setResourceIndicator(false);
        return client;
    }

    private Client getClientAsResource(String... resourceIds) {
        Client client = getClientRaw(resourceIds);
        GrantedAuthorityImpl<ClientAuthorityEnum> clientAuthorityEnumGrantedAuthority = new GrantedAuthorityImpl<>();
        clientAuthorityEnumGrantedAuthority.setGrantedAuthority(ClientAuthorityEnum.ROLE_FIRST_PARTY);
        GrantedAuthorityImpl<ClientAuthorityEnum> clientAuthorityEnumGrantedAuthority2 = new GrantedAuthorityImpl<>();
        clientAuthorityEnumGrantedAuthority2.setGrantedAuthority(ClientAuthorityEnum.ROLE_BACKEND);
        client.setGrantedAuthorities(Arrays.asList(clientAuthorityEnumGrantedAuthority, clientAuthorityEnumGrantedAuthority2));
        client.setResourceIndicator(true);
        return client;
    }

    private Client getInvalidClientAsResource(String... resourceIds) {
        Client client = getClientRaw(resourceIds);
        GrantedAuthorityImpl<ClientAuthorityEnum> clientAuthorityEnumGrantedAuthority = new GrantedAuthorityImpl<>();
        clientAuthorityEnumGrantedAuthority.setGrantedAuthority(ClientAuthorityEnum.ROLE_THIRD_PARTY);
        GrantedAuthorityImpl<ClientAuthorityEnum> clientAuthorityEnumGrantedAuthority2 = new GrantedAuthorityImpl<>();
        clientAuthorityEnumGrantedAuthority2.setGrantedAuthority(ClientAuthorityEnum.ROLE_BACKEND);
        client.setGrantedAuthorities(Arrays.asList(clientAuthorityEnumGrantedAuthority));
        client.setResourceIndicator(true);
        return client;
    }

    private Client getClientRaw(String... resourceIds) {
        Client client = new Client();
        client.setClientId(UUID.randomUUID().toString().replace("-", ""));
        client.setClientSecret(UUID.randomUUID().toString().replace("-", ""));
        client.setGrantTypeEnums(new HashSet<>(Arrays.asList(GrantTypeEnum.password)));
        client.setScopeEnums(new HashSet<>(Arrays.asList(ScopeEnum.read)));
        client.setAccessTokenValiditySeconds(1800);
        client.setRefreshTokenValiditySeconds(null);
        client.setHasSecret(true);
        client.setResourceIds(new HashSet<>(Arrays.asList(resourceIds)));
        return client;
    }

    public ResponseEntity<String> createClient(Client client) throws JsonProcessingException {
        String url = UserAction.proxyUrl + "/api" + "/clients";
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