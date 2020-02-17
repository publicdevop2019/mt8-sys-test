package integration.oauth2;

import com.jayway.jsonpath.JsonPath;
import helper.ClientAuthorityEnum;
import helper.ResourceOwnerAuthorityEnum;
import helper.ServiceUtility;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RunWith(SpringRunner.class)
public class AuthorizationCodeTest {

    private String password = "password";
    private String authorization_code = "authorization_code";
    private String client_credentials = "client_credentials";
    private String valid_clientId = "login-id";
    private String valid_third_party = "mgfb-id";
    private String invalid_third_party = UUID.randomUUID().toString();
    private String valid_clientId_no_refersh = "test-id";
    private String valid_empty_secret = "";
    private String valid_username_root = "haolinwei2015@gmail.com";
    private String valid_username_admin = "haolinwei2017@gmail.com";
    private String valid_username_user = "haolinwei2018@gmail.com";
    private String valid_pwd = "root";
    private String invalid_username = "root2@gmail.com";
    private String invalid_clientId = "root2";
    private String valid_redirect_uri = "http://localhost:4200";
    private String state = "login";
    private String response_type = "code";
    private String invalid_accessToken = UUID.randomUUID().toString();

    private TestRestTemplate restTemplate = new TestRestTemplate();

    int randomServerPort = 8080;

    @Test
    public void happy_getAuthorizationCode_root() {
        ResponseEntity<DefaultOAuth2AccessToken> defaultOAuth2AccessTokenResponseEntity = pwdFlowLogin(password, valid_username_root, valid_pwd, valid_clientId, valid_empty_secret);
        String accessToken = defaultOAuth2AccessTokenResponseEntity.getBody().getValue();
        ResponseEntity<String> code = getCodeResp(valid_third_party, accessToken);
        String body = code.getBody();
        String read = JsonPath.read(body, "$.authorize_code");
        Assert.assertNotNull(read);

    }

    @Test
    public void happy_getAuthorizationCode_admin() {
        ResponseEntity<DefaultOAuth2AccessToken> defaultOAuth2AccessTokenResponseEntity = pwdFlowLogin(password, valid_username_admin, valid_pwd, valid_clientId, valid_empty_secret);
        String accessToken = defaultOAuth2AccessTokenResponseEntity.getBody().getValue();
        ResponseEntity<String> codeResp = getCodeResp(valid_third_party, accessToken);
        String code = JsonPath.read(codeResp.getBody(), "$.authorize_code");

        Assert.assertNotNull(code);

        ResponseEntity<DefaultOAuth2AccessToken> authorizationToken = getAuthorizationToken(authorization_code, code, valid_redirect_uri, valid_third_party);

        Assert.assertEquals(HttpStatus.OK, authorizationToken.getStatusCode());
        Assert.assertNotNull(authorizationToken.getBody());
        DefaultOAuth2AccessToken body = authorizationToken.getBody();
        List<String> authorities = ServiceUtility.getAuthority(body.getValue());
        Assert.assertEquals(1, authorities.stream().filter(e -> e.equals(ResourceOwnerAuthorityEnum.ROLE_USER.toString())).count());
        Assert.assertEquals(1, authorities.stream().filter(e -> e.equals(ResourceOwnerAuthorityEnum.ROLE_ADMIN.toString())).count());
        Assert.assertEquals(0, authorities.stream().filter(e -> e.equals(ResourceOwnerAuthorityEnum.ROLE_ROOT.toString())).count());
        Assert.assertEquals(0, authorities.stream().map(e -> {
                    try {
                        return ClientAuthorityEnum.valueOf(e);
                    } catch (IllegalArgumentException ex) {
                        return null;
                    }
                }


        ).filter(Objects::nonNull).count());

    }

    @Test
    public void happy_getAuthorizationCode_user() {
        ResponseEntity<DefaultOAuth2AccessToken> defaultOAuth2AccessTokenResponseEntity = pwdFlowLogin(password, valid_username_user, valid_pwd, valid_clientId, valid_empty_secret);
        String accessToken = defaultOAuth2AccessTokenResponseEntity.getBody().getValue();
        ResponseEntity<String> codeResp = getCodeResp(valid_third_party, accessToken);
        String read = JsonPath.read(codeResp.getBody(), "$.authorize_code");
        Assert.assertNotNull(read);

    }


    @Test
    public void sad_invalid_code() {
        ResponseEntity<DefaultOAuth2AccessToken> defaultOAuth2AccessTokenResponseEntity = pwdFlowLogin(password, valid_username_root, valid_pwd, valid_clientId, valid_empty_secret);
        String accessToken = defaultOAuth2AccessTokenResponseEntity.getBody().getValue();
        ResponseEntity<String> code = getCodeResp(valid_third_party, accessToken);
        ResponseEntity<DefaultOAuth2AccessToken> authorizationToken = getAuthorizationToken(authorization_code, UUID.randomUUID().toString(), valid_redirect_uri, valid_third_party);
        Assert.assertEquals(HttpStatus.BAD_REQUEST, authorizationToken.getStatusCode());

    }

    @Test
    public void sad_invalid_redirectUri() {
        ResponseEntity<DefaultOAuth2AccessToken> defaultOAuth2AccessTokenResponseEntity = pwdFlowLogin(password, valid_username_root, valid_pwd, valid_clientId, valid_empty_secret);
        String accessToken = defaultOAuth2AccessTokenResponseEntity.getBody().getValue();
        ResponseEntity<String> codeResp = getCodeResp(valid_third_party, accessToken);
        String code = JsonPath.read(codeResp.getBody(), "$.authorize_code");
        ResponseEntity<DefaultOAuth2AccessToken> authorizationToken = getAuthorizationToken(authorization_code, code, UUID.randomUUID().toString(), valid_third_party);
        Assert.assertEquals(HttpStatus.BAD_REQUEST, authorizationToken.getStatusCode());

    }

    @Test
    public void sad_invalid_grantType() {
        ResponseEntity<DefaultOAuth2AccessToken> defaultOAuth2AccessTokenResponseEntity = pwdFlowLogin(password, valid_username_root, valid_pwd, valid_clientId, valid_empty_secret);
        String accessToken = defaultOAuth2AccessTokenResponseEntity.getBody().getValue();
        ResponseEntity<String> codeResp = getCodeResp(valid_third_party, accessToken);
        String code = JsonPath.read(codeResp.getBody(), "$.authorize_code");
        ResponseEntity<DefaultOAuth2AccessToken> authorizationToken = getAuthorizationToken(password, code, valid_redirect_uri, valid_third_party);
        Assert.assertEquals(HttpStatus.UNAUTHORIZED, authorizationToken.getStatusCode());

    }

    @Test
    public void sad_invalid_clientId_after_code_gen() {
        ResponseEntity<DefaultOAuth2AccessToken> defaultOAuth2AccessTokenResponseEntity = pwdFlowLogin(password, valid_username_root, valid_pwd, valid_clientId, valid_empty_secret);
        String accessToken = defaultOAuth2AccessTokenResponseEntity.getBody().getValue();
        ResponseEntity<String> codeResp = getCodeResp(valid_third_party, accessToken);
        String code = JsonPath.read(codeResp.getBody(), "$.authorize_code");
        ResponseEntity<DefaultOAuth2AccessToken> authorizationToken = getAuthorizationToken(authorization_code, code, valid_redirect_uri, valid_clientId);
        Assert.assertEquals(HttpStatus.UNAUTHORIZED, authorizationToken.getStatusCode());

    }

    @Test
    public void sad_invalid_client_credential() {
        ResponseEntity<DefaultOAuth2AccessToken> defaultOAuth2AccessTokenResponseEntity = pwdFlowLogin(password, valid_username_root, valid_pwd, valid_clientId, valid_empty_secret);
        String accessToken = defaultOAuth2AccessTokenResponseEntity.getBody().getValue();
        ResponseEntity<String> codeResp = getCodeResp(valid_third_party, accessToken);
        String code = JsonPath.read(codeResp.getBody(), "$.authorize_code");
        ResponseEntity<DefaultOAuth2AccessToken> authorizationToken = getAuthorizationTokenSecret(authorization_code, code, valid_redirect_uri, valid_clientId, UUID.randomUUID().toString());
        Assert.assertEquals(HttpStatus.UNAUTHORIZED, authorizationToken.getStatusCode());

    }

    @Test
    public void sad_invalid_authorize_clientId() {
        ResponseEntity<DefaultOAuth2AccessToken> defaultOAuth2AccessTokenResponseEntity = pwdFlowLogin(password, valid_username_root, valid_pwd, valid_clientId, valid_empty_secret);
        String accessToken = defaultOAuth2AccessTokenResponseEntity.getBody().getValue();
        ResponseEntity<String> codeResp = getCodeResp(invalid_third_party, accessToken);
        Assert.assertEquals(HttpStatus.BAD_REQUEST, codeResp.getStatusCode());
    }


    private ResponseEntity<String> getCodeResp(String clientId, String bearerToken) {
        String url = "http://localhost:" + randomServerPort + "/" + "v1/api/" + "authorize";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("response_type", response_type);
        params.add("client_id", clientId);
        params.add("state", state);
        params.add("redirect_uri", valid_redirect_uri);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearerToken);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        return restTemplate.exchange(url, HttpMethod.POST, request, String.class);
    }

    private ResponseEntity<DefaultOAuth2AccessToken> pwdFlowLogin(String grantType, String username, String userPwd, String clientId, String clientSecret) {
        String url = "http://localhost:" + randomServerPort + "/" + "oauth/token";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", grantType);
        params.add("username", username);
        params.add("password", userPwd);
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        return restTemplate.exchange(url, HttpMethod.POST, request, DefaultOAuth2AccessToken.class);
    }

    private ResponseEntity<DefaultOAuth2AccessToken> getAuthorizationToken(String grantType, String code, String redirect_uri, String clientId) {
        return getAuthorizationTokenSecret(grantType, code, redirect_uri, clientId, valid_empty_secret);
    }

    private ResponseEntity<DefaultOAuth2AccessToken> getAuthorizationTokenSecret(String grantType, String code, String redirect_uri, String clientId, String clientSecret) {
        String url = "http://localhost:" + randomServerPort + "/" + "oauth/token";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", grantType);
        params.add("code", code);
        params.add("redirect_uri", redirect_uri);
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        return restTemplate.exchange(url, HttpMethod.POST, request, DefaultOAuth2AccessToken.class);
    }
}
