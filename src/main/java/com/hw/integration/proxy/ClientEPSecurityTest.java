package com.hw.integration.proxy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hw.helper.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

/**
 * this integration auth requires oauth2service to be running
 */
@RunWith(SpringRunner.class)
public class ClientEPSecurityTest {
    private String password = "password";
    private String valid_clientId = "login-id";
    private String valid_resourceId = "resource-id";
    private String valid_empty_secret = "";
    private String valid_username_admin = "haolinwei2017@gmail.com";
    private String valid_pwd = "root";
    private TestRestTemplate restTemplate = new TestRestTemplate();
    public ObjectMapper mapper = new ObjectMapper();

    @Test
    public void should_not_able_to_create_client_w_admin_account_when_going_through_proxy() throws JsonProcessingException {
        Client client = getClientAsNonResource(valid_resourceId);
        String url = UserAction.proxyUrl + "/api" + "/clients";
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(password, valid_username_admin, valid_pwd, valid_clientId, valid_empty_secret);
        String bearer = tokenResponse.getBody().getValue();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(bearer);
        String s = mapper.writeValueAsString(client);
        HttpEntity<String> request = new HttpEntity<>(s, headers);
        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
        Assert.assertEquals(HttpStatus.FORBIDDEN, exchange.getStatusCode());
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

    private ResponseEntity<DefaultOAuth2AccessToken> getTokenResponse(String grantType, String username, String userPwd, String clientId, String clientSecret) {
        String url = UserAction.proxyUrl + "/" + "oauth/token";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", grantType);
        params.add("username", username);
        params.add("password", userPwd);
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        return restTemplate.exchange(url, HttpMethod.POST, request, DefaultOAuth2AccessToken.class);
    }
}
