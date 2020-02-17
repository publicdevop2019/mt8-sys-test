package integration.proxy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.UUID;

/**
 * this integration auth requires oauth2service to be running
 */
@RunWith(SpringRunner.class)
public class BlackListControllerTest {
    private String client_credentials = "client_credentials";
    private String password = "password";
    private String valid_clientId = "oauth2-id";
    private String should_block_clientId_root = "block-id";
    private String should_block_clientId_non_root = "login-id";
    private String wrong_clientId = "register-id";
    private String login_clientId = "login-id";
    private String valid_clientSecret = "root";
    private String oauth2service = "8080";

    int randomServerPort = 8111;

    private ObjectMapper mapper = new ObjectMapper();

    private TestRestTemplate restTemplate = new TestRestTemplate();
    private String username = "haolinwei2017@gmail.com";
    private String userPwd = "root";

    @Test
    public void sad_receive_request_blacklist_client_then_block_client_old_request_access_proxy_endpoints() throws JsonProcessingException, InterruptedException {
        String url = "http://localhost:" + randomServerPort + "/proxy/blacklist" + "/client";
        /**
         * before client get blacklisted, client is able to access proxy endpoints
         */
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse1 = getTokenResponse(client_credentials, should_block_clientId_root, valid_clientSecret);
        String bearer1 = tokenResponse1.getBody().getValue();

        HashMap<String, String> stringStringHashMap = new HashMap<>();
        stringStringHashMap.put("name", UUID.randomUUID().toString());
        String s = mapper.writeValueAsString(stringStringHashMap);
        HttpHeaders headers1 = new HttpHeaders();
        headers1.setBearerAuth(bearer1);
        headers1.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> hashMapHttpEntity1 = new HttpEntity<>(s, headers1);
        ResponseEntity<String> exchange1 = restTemplate.exchange(url, HttpMethod.POST, hashMapHttpEntity1, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange1.getStatusCode());


        /**
         * block client
         */
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(client_credentials, valid_clientId, valid_clientSecret);
        String bearer = tokenResponse.getBody().getValue();

        HashMap<String, String> blockBody = new HashMap<>();
        blockBody.put("name", should_block_clientId_root);
        String body = mapper.writeValueAsString(blockBody);
        HttpHeaders blockCallHeader = new HttpHeaders();
        blockCallHeader.setBearerAuth(bearer);
        blockCallHeader.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> blockReqEntity = new HttpEntity<>(body, blockCallHeader);
        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.POST, blockReqEntity, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());

        /**
         * after client get blacklisted, client with old token will get 401
         */
        ResponseEntity<String> exchange2 = restTemplate.exchange(url, HttpMethod.POST, hashMapHttpEntity1, String.class);
        Assert.assertEquals(HttpStatus.UNAUTHORIZED, exchange2.getStatusCode());

        /**
         * after client obtain new token from auth server, it can access resource again
         * add thread sleep to prevent token get revoked and generate within a second
         */
        Thread.sleep(1000);
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse3 = getTokenResponse(client_credentials, should_block_clientId_root, valid_clientSecret);
        String bearer3 = tokenResponse3.getBody().getValue();
        headers1.setBearerAuth(bearer3);
        HttpEntity<String> hashMapHttpEntity3 = new HttpEntity<>(s, headers1);
        ResponseEntity<String> exchange3 = restTemplate.exchange(url, HttpMethod.POST, hashMapHttpEntity3, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange3.getStatusCode());
    }

    @Test
    public void sad_receive_request_blacklist_client_then_block_client_old_request() throws JsonProcessingException, InterruptedException {

        String url = "http://localhost:" + randomServerPort + "/proxy/blacklist" + "/client";
        String url2 = "http://localhost:" + randomServerPort + "/api" + "/resourceOwners";
        /**
         * before client get blacklisted, client is able to access auth server non token endpoint
         */
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse1 = getPwdTokenResponse(password, should_block_clientId_non_root, "", username, userPwd);
        String bearer1 = tokenResponse1.getBody().getValue();
        HttpHeaders headers1 = new HttpHeaders();
        headers1.setBearerAuth(bearer1);
        HttpEntity<Object> hashMapHttpEntity1 = new HttpEntity<>(headers1);
        ResponseEntity<String> exchange1 = restTemplate.exchange(url2, HttpMethod.GET, hashMapHttpEntity1, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange1.getStatusCode());


        /**
         * block client
         */
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(client_credentials, valid_clientId, valid_clientSecret);
        String bearer = tokenResponse.getBody().getValue();

        HashMap<String, String> stringStringHashMap = new HashMap<>();
        stringStringHashMap.put("name", should_block_clientId_non_root);
        String s = mapper.writeValueAsString(stringStringHashMap);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearer);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> hashMapHttpEntity = new HttpEntity<>(s, headers);
        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.POST, hashMapHttpEntity, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());

        /**
         * after client get blacklisted, client with old token will get 401
         */
        ResponseEntity<String> exchange2 = restTemplate.exchange(url2, HttpMethod.GET, hashMapHttpEntity1, String.class);
        Assert.assertEquals(HttpStatus.UNAUTHORIZED, exchange2.getStatusCode());

        /**
         * after client obtain new token from auth server, it can access resource again
         * add thread sleep to prevent token get revoked and generate within a second
         */
        Thread.sleep(1000);
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse3 = getPwdTokenResponse(password, should_block_clientId_non_root, "", username, userPwd);
        String bearer3 = tokenResponse3.getBody().getValue();
        headers1.setBearerAuth(bearer3);
        HttpEntity<Object> hashMapHttpEntity3 = new HttpEntity<>(headers1);
        ResponseEntity<String> exchange3 = restTemplate.exchange(url2, HttpMethod.GET, hashMapHttpEntity3, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange3.getStatusCode());
    }

    /**
     * only root client and trusted client can add blacklist
     */
    @Test
    public void sad_receive_request_blacklist_from_wrong_client() throws JsonProcessingException {
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(client_credentials, wrong_clientId, "");
        String bearer = tokenResponse.getBody().getValue();

        String url = "http://localhost:" + randomServerPort + "/proxy/blacklist" + "/client";
        HashMap<String, String> stringStringHashMap = new HashMap<>();
        stringStringHashMap.put("name", UUID.randomUUID().toString());
        String s = mapper.writeValueAsString(stringStringHashMap);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearer);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> hashMapHttpEntity = new HttpEntity<>(s, headers);
        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.POST, hashMapHttpEntity, String.class);
        Assert.assertEquals(HttpStatus.FORBIDDEN, exchange.getStatusCode());
    }

    @Test
    public void happy_receive_request_blacklist_resourceOwner_then_block_resourceOwner_old_request() throws JsonProcessingException, InterruptedException {
        String url2 = "http://localhost:" + randomServerPort + "/api" + "/resourceOwners";
        /**
         * admin user can login & call resourceOwner api
         */
        ResponseEntity<DefaultOAuth2AccessToken> pwdTokenResponse = getPwdTokenResponse(password, login_clientId, "", username, userPwd);

        String bearer0 = pwdTokenResponse.getBody().getValue();
        String refreshToken = pwdTokenResponse.getBody().getRefreshToken().getValue();
        String user_name = (String) pwdTokenResponse.getBody().getAdditionalInformation().get("uid");
        HttpHeaders headers1 = new HttpHeaders();
        headers1.setBearerAuth(bearer0);
        HttpEntity<Object> hashMapHttpEntity1 = new HttpEntity<>(headers1);
        ResponseEntity<String> exchange2 = restTemplate.exchange(url2, HttpMethod.GET, hashMapHttpEntity1, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange2.getStatusCode());

        /**
         * blacklist admin account
         */
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(client_credentials, valid_clientId, valid_clientSecret);
        String bearer = tokenResponse.getBody().getValue();

        String url = "http://localhost:" + randomServerPort + "/proxy/blacklist" + "/resourceOwner";
        HashMap<String, String> stringStringHashMap = new HashMap<>();
        stringStringHashMap.put("name", user_name);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearer);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> hashMapHttpEntity = new HttpEntity<>(mapper.writeValueAsString(stringStringHashMap), headers);
        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.POST, hashMapHttpEntity, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());

        /**
         * resourceOwner request get blocked, even refresh token should not work
         */
        ResponseEntity<String> exchange1 = restTemplate.exchange(url2, HttpMethod.GET, hashMapHttpEntity1, String.class);
        Assert.assertEquals(HttpStatus.UNAUTHORIZED, exchange1.getStatusCode());

        ResponseEntity<DefaultOAuth2AccessToken> refreshTokenResponse = getRefreshTokenResponse(refreshToken, login_clientId, "");
        Assert.assertEquals(HttpStatus.UNAUTHORIZED, refreshTokenResponse.getStatusCode());

        /**
         * after resourceOwner obtain new token, access is permitted
         * add thread sleep to prevent token get revoked and generate within a second
         */
        Thread.sleep(1000);
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse3 = getPwdTokenResponse(password, login_clientId, "", username, userPwd);
        String bearer3 = tokenResponse3.getBody().getValue();
        headers1.setBearerAuth(bearer3);
        HttpEntity<Object> hashMapHttpEntity3 = new HttpEntity<>(headers1);
        ResponseEntity<String> exchange3 = restTemplate.exchange(url2, HttpMethod.GET, hashMapHttpEntity3, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange3.getStatusCode());

    }


    private ResponseEntity<DefaultOAuth2AccessToken> getTokenResponse(String grantType, String clientId, String clientSecret) {
        String url = "http://localhost:" + randomServerPort + "/" + "oauth/token";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", grantType);
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        return restTemplate.exchange(url, HttpMethod.POST, request, DefaultOAuth2AccessToken.class);
    }

    private ResponseEntity<DefaultOAuth2AccessToken> getRefreshTokenResponse(String refreshToken, String clientId, String clientSecret) {
        String url = "http://localhost:" + randomServerPort + "/" + "oauth/token";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "refresh_token");
        params.add("refresh_token", refreshToken);
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        return restTemplate.exchange(url, HttpMethod.POST, request, DefaultOAuth2AccessToken.class);
    }

    private ResponseEntity<DefaultOAuth2AccessToken> getPwdTokenResponse(String grantType, String clientId, String clientSecret, String username, String pwd) {
        String url = "http://localhost:" + randomServerPort + "/" + "oauth/token";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", grantType);
        params.add("username", username);
        params.add("password", pwd);
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        return restTemplate.exchange(url, HttpMethod.POST, request, DefaultOAuth2AccessToken.class);
    }
}
