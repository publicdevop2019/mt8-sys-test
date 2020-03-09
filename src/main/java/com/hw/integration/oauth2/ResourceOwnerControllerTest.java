package com.hw.integration.oauth2;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hw.helper.*;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.UUID;

@RunWith(SpringRunner.class)
public class ResourceOwnerControllerTest {
    private String password = "password";
    private String client_credentials = "client_credentials";
    private String valid_login_clientId = "login-id";
    private String valid_register_clientId = "register-id";
    private String invalid_clientId = "rightRoleNotSufficientResourceId";
    private String valid_empty_secret = "";
    private String valid_username_root = "haolinwei2015@gmail.com";
    private String valid_username_admin = "haolinwei2017@gmail.com";
    private String valid_pwd = "root";
    private Long root_index = 0L;
    public ObjectMapper mapper = new ObjectMapper().configure(MapperFeature.USE_ANNOTATIONS, false).setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private TestRestTemplate restTemplate = new TestRestTemplate();

    @Test
    public void create_user_with_right_authority_is_user_only() throws JsonProcessingException {
        ResourceOwner user = getUser();
        ResponseEntity<DefaultOAuth2AccessToken> user1 = createUser(user);

        Assert.assertEquals(HttpStatus.OK, user1.getStatusCode());

        Assert.assertNotNull(user1.getHeaders().getLocation());

        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse12 = getTokenResponse(password, user.getEmail(), user.getPassword(), valid_login_clientId, valid_empty_secret);

        Assert.assertEquals(HttpStatus.OK, tokenResponse12.getStatusCode());

        List<String> authorities = ServiceUtility.getAuthority(tokenResponse12.getBody().getValue());

        Assert.assertEquals(1, authorities.stream().filter(e -> e.equals(ResourceOwnerAuthorityEnum.ROLE_USER.toString())).count());
        Assert.assertEquals(0, authorities.stream().filter(e -> e.equals(ResourceOwnerAuthorityEnum.ROLE_ADMIN.toString())).count());
        Assert.assertEquals(0, authorities.stream().filter(e -> e.equals(ResourceOwnerAuthorityEnum.ROLE_ROOT.toString())).count());

    }

    @Test
    public void should_not_able_to_create_user_with_user_name_not_email() throws JsonProcessingException {
        ResourceOwner user = getUser_wrong_username();
        ResponseEntity<DefaultOAuth2AccessToken> user1 = createUser(user);

        Assert.assertEquals(HttpStatus.BAD_REQUEST, user1.getStatusCode());

    }

    @Test
    @Ignore
    public void able_to_create_user_with_wrong_client_if_directly_call_bypassing_proxy() throws JsonProcessingException {
        ResourceOwner user = getUser();
        ResponseEntity<DefaultOAuth2AccessToken> user1 = createUser(user, invalid_clientId);

        Assert.assertEquals(HttpStatus.OK, user1.getStatusCode());
    }

    @Test
    public void update_user_password_without_current_pwd() throws JsonProcessingException {
        ResourceOwner user = getUser();
        createUser(user);
        /** Location is not used in this case, root/admin/user can only update their password */
        String url = UserAction.proxyUrl + "/api" + "/resourceOwner/pwd";
        String newPassword = UUID.randomUUID().toString().replace("-", "");
        /** Login */
        String oldPassword = user.getPassword();
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(this.password, user.getEmail(), user.getPassword(), valid_login_clientId, valid_empty_secret);
        String bearer = tokenResponse.getBody().getValue();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(bearer);
        user.setPassword(newPassword);
        String s1 = mapper.writeValueAsString(user);
        HttpEntity<String> request = new HttpEntity<>(s1, headers);
        ResponseEntity<Object> exchange = restTemplate.exchange(url, HttpMethod.PATCH, request, Object.class);

        Assert.assertEquals(HttpStatus.BAD_REQUEST, exchange.getStatusCode());
    }

    @Test
    public void update_user_password_with_current_pwd() throws JsonProcessingException {
        ResourceOwner user = getUser();
        ResourceOwnerUpdatePwd resourceOwnerUpdatePwd = new ResourceOwnerUpdatePwd();
        resourceOwnerUpdatePwd.setCurrentPwd(user.getPassword());
        resourceOwnerUpdatePwd.setEmail(user.getEmail());
        resourceOwnerUpdatePwd.setPassword(UUID.randomUUID().toString());
        createUser(user);
        /** Location is not used in this case, root/admin/user can only update their password */
        String url = UserAction.proxyUrl + "/api" + "/resourceOwner/pwd";
        /** Login */
        String oldPassword = user.getPassword();
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(this.password, user.getEmail(), user.getPassword(), valid_login_clientId, valid_empty_secret);
        String bearer = tokenResponse.getBody().getValue();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(bearer);

        String s1 = mapper.writeValueAsString(resourceOwnerUpdatePwd);
        HttpEntity<String> request = new HttpEntity<>(s1, headers);
        ResponseEntity<Object> exchange = restTemplate.exchange(url, HttpMethod.PATCH, request, Object.class);

        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());

        ResponseEntity<DefaultOAuth2AccessToken> resp3 = getTokenResponse(this.password, user.getEmail(), oldPassword, valid_login_clientId, valid_empty_secret);

        Assert.assertEquals(HttpStatus.BAD_REQUEST, resp3.getStatusCode());

        ResponseEntity<DefaultOAuth2AccessToken> resp4 = getTokenResponse(this.password, user.getEmail(), resourceOwnerUpdatePwd.getPassword(), valid_login_clientId, valid_empty_secret);

        Assert.assertEquals(HttpStatus.OK, resp4.getStatusCode());

    }

    @Test
    public void read_all_users_with_root_account() {
        ParameterizedTypeReference<List<ResourceOwner>> responseType = new ParameterizedTypeReference<>() {
        };
        String url = UserAction.proxyUrl + "/api" + "/resourceOwners";
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(password, valid_username_root, valid_pwd, valid_login_clientId, valid_empty_secret);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenResponse.getBody().getValue());
        HttpEntity<String> request = new HttpEntity<>(null, headers);
        ResponseEntity<List<ResourceOwner>> exchange = restTemplate.exchange(url, HttpMethod.GET, request, responseType);

        Assert.assertNotSame(0, exchange.getBody().size());
    }


    @Test
    public void update_user_authority_to_admin_with_root_account() throws JsonProcessingException {
        ResourceOwner user = getUser();
        ResponseEntity<DefaultOAuth2AccessToken> createResp = createUser(user);
        String s = createResp.getHeaders().getLocation().toString();
        String url = UserAction.proxyUrl + "/api" + "/resourceOwners/" + s;

        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(password, valid_username_root, valid_pwd, valid_login_clientId, valid_empty_secret);
        String bearer = tokenResponse.getBody().getValue();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(bearer);
        GrantedAuthorityImpl<ResourceOwnerAuthorityEnum> resourceOwnerAuthorityEnumGrantedAuthority = new GrantedAuthorityImpl<>();
        resourceOwnerAuthorityEnumGrantedAuthority.setGrantedAuthority(ResourceOwnerAuthorityEnum.ROLE_ADMIN);
        GrantedAuthorityImpl<ResourceOwnerAuthorityEnum> resourceOwnerAuthorityEnumGrantedAuthority2 = new GrantedAuthorityImpl<>();
        resourceOwnerAuthorityEnumGrantedAuthority2.setGrantedAuthority(ResourceOwnerAuthorityEnum.ROLE_USER);
        user.setGrantedAuthorities(List.of(resourceOwnerAuthorityEnumGrantedAuthority, resourceOwnerAuthorityEnumGrantedAuthority2));
        String s1 = mapper.writeValueAsString(user);
        HttpEntity<String> request = new HttpEntity<>(s1, headers);
        ResponseEntity<DefaultOAuth2AccessToken> exchange = restTemplate.exchange(url, HttpMethod.PUT, request, DefaultOAuth2AccessToken.class);

        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());

        /**
         * login to verify grantedAuthorities has been changed
         */
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse1 = getTokenResponse(password, user.getEmail(), user.getPassword(), valid_login_clientId, valid_empty_secret);
        List<String> authorities = ServiceUtility.getAuthority(tokenResponse1.getBody().getValue());
        Assert.assertEquals(1, authorities.stream().filter(e -> e.equals(ResourceOwnerAuthorityEnum.ROLE_USER.toString())).count());
        Assert.assertEquals(1, authorities.stream().filter(e -> e.equals(ResourceOwnerAuthorityEnum.ROLE_ADMIN.toString())).count());
        Assert.assertEquals(0, authorities.stream().filter(e -> e.equals(ResourceOwnerAuthorityEnum.ROLE_ROOT.toString())).count());
    }

    @Test
    public void should_not_able_to_update_user_authority_to_root_with_admin_account() throws JsonProcessingException {
        ResourceOwner user = getUser();
        String url = UserAction.proxyUrl + "/api" + "/resourceOwners/" + root_index;

        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(password, valid_username_admin, valid_pwd, valid_login_clientId, valid_empty_secret);
        String bearer = tokenResponse.getBody().getValue();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(bearer);

        GrantedAuthorityImpl<ResourceOwnerAuthorityEnum> resourceOwnerAuthorityEnumGrantedAuthority = new GrantedAuthorityImpl<>();
        resourceOwnerAuthorityEnumGrantedAuthority.setGrantedAuthority(ResourceOwnerAuthorityEnum.ROLE_ADMIN);
        GrantedAuthorityImpl<ResourceOwnerAuthorityEnum> resourceOwnerAuthorityEnumGrantedAuthority2 = new GrantedAuthorityImpl<>();
        resourceOwnerAuthorityEnumGrantedAuthority2.setGrantedAuthority(ResourceOwnerAuthorityEnum.ROLE_USER);
        user.setGrantedAuthorities(List.of(resourceOwnerAuthorityEnumGrantedAuthority, resourceOwnerAuthorityEnumGrantedAuthority2));
        String s1 = mapper.writeValueAsString(user);
        HttpEntity<String> request = new HttpEntity<>(s1, headers);
        ResponseEntity<DefaultOAuth2AccessToken> exchange = restTemplate.exchange(url, HttpMethod.PUT, request, DefaultOAuth2AccessToken.class);

        Assert.assertEquals(HttpStatus.BAD_REQUEST, exchange.getStatusCode());

    }

    @Test
    public void should_not_able_to_update_user_authority_to_root_with_root_account() throws JsonProcessingException {
        ResourceOwner user = getUser();
        ResponseEntity<DefaultOAuth2AccessToken> createResp = createUser(user);
        String s = createResp.getHeaders().getLocation().toString();
        String url = UserAction.proxyUrl + "/api" + "/resourceOwners/" + s;

        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(password, valid_username_root, valid_pwd, valid_login_clientId, valid_empty_secret);
        String bearer = tokenResponse.getBody().getValue();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(bearer);
        GrantedAuthorityImpl<ResourceOwnerAuthorityEnum> resourceOwnerAuthorityEnumGrantedAuthority = new GrantedAuthorityImpl<>();
        resourceOwnerAuthorityEnumGrantedAuthority.setGrantedAuthority(ResourceOwnerAuthorityEnum.ROLE_ROOT);
        GrantedAuthorityImpl<ResourceOwnerAuthorityEnum> resourceOwnerAuthorityEnumGrantedAuthority2 = new GrantedAuthorityImpl<>();
        resourceOwnerAuthorityEnumGrantedAuthority2.setGrantedAuthority(ResourceOwnerAuthorityEnum.ROLE_USER);
        GrantedAuthorityImpl<ResourceOwnerAuthorityEnum> resourceOwnerAuthorityEnumGrantedAuthority3 = new GrantedAuthorityImpl<>();
        resourceOwnerAuthorityEnumGrantedAuthority3.setGrantedAuthority(ResourceOwnerAuthorityEnum.ROLE_ADMIN);
        user.setGrantedAuthorities(List.of(resourceOwnerAuthorityEnumGrantedAuthority, resourceOwnerAuthorityEnumGrantedAuthority2, resourceOwnerAuthorityEnumGrantedAuthority3));
        String s1 = mapper.writeValueAsString(user);
        HttpEntity<String> request = new HttpEntity<>(s1, headers);
        ResponseEntity<DefaultOAuth2AccessToken> exchange = restTemplate.exchange(url, HttpMethod.PUT, request, DefaultOAuth2AccessToken.class);

        Assert.assertEquals(HttpStatus.BAD_REQUEST, exchange.getStatusCode());

    }

    @Test
    public void should_not_able_to_update_user_authority_to_admin_with_admin_account() throws JsonProcessingException {
        ResourceOwner user = getUser();
        ResponseEntity<DefaultOAuth2AccessToken> createResp = createUser(user);
        String s = createResp.getHeaders().getLocation().toString();
        String url = UserAction.proxyUrl + "/api" + "/resourceOwners/" + s;

        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(password, valid_username_admin, valid_pwd, valid_login_clientId, valid_empty_secret);
        String bearer = tokenResponse.getBody().getValue();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(bearer);
        GrantedAuthorityImpl<ResourceOwnerAuthorityEnum> resourceOwnerAuthorityEnumGrantedAuthority = new GrantedAuthorityImpl<>();
        resourceOwnerAuthorityEnumGrantedAuthority.setGrantedAuthority(ResourceOwnerAuthorityEnum.ROLE_ADMIN);
        GrantedAuthorityImpl<ResourceOwnerAuthorityEnum> resourceOwnerAuthorityEnumGrantedAuthority2 = new GrantedAuthorityImpl<>();
        resourceOwnerAuthorityEnumGrantedAuthority2.setGrantedAuthority(ResourceOwnerAuthorityEnum.ROLE_USER);
        user.setGrantedAuthorities(List.of(resourceOwnerAuthorityEnumGrantedAuthority, resourceOwnerAuthorityEnumGrantedAuthority2));
        String s1 = mapper.writeValueAsString(user);
        HttpEntity<String> request = new HttpEntity<>(s1, headers);
        ResponseEntity<DefaultOAuth2AccessToken> exchange = restTemplate.exchange(url, HttpMethod.PUT, request, DefaultOAuth2AccessToken.class);

        Assert.assertEquals(HttpStatus.BAD_REQUEST, exchange.getStatusCode());

    }

    @Test
    public void lock_then_unlock_user() throws JsonProcessingException {
        ResourceOwner user = getUser();
        ResponseEntity<DefaultOAuth2AccessToken> createResp = createUser(user);
        String s = createResp.getHeaders().getLocation().toString();
        String url = UserAction.proxyUrl + "/api" + "/resourceOwners/" + s;

        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(password, valid_username_root, valid_pwd, valid_login_clientId, valid_empty_secret);
        String bearer = tokenResponse.getBody().getValue();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(bearer);
        GrantedAuthorityImpl<ResourceOwnerAuthorityEnum> resourceOwnerAuthorityEnumGrantedAuthority2 = new GrantedAuthorityImpl<>();
        resourceOwnerAuthorityEnumGrantedAuthority2.setGrantedAuthority(ResourceOwnerAuthorityEnum.ROLE_USER);
        user.setGrantedAuthorities(List.of(resourceOwnerAuthorityEnumGrantedAuthority2));
        user.setLocked(true);
        String s1 = mapper.writeValueAsString(user);
        HttpEntity<String> request = new HttpEntity<>(s1, headers);
        ResponseEntity<DefaultOAuth2AccessToken> exchange = restTemplate.exchange(url, HttpMethod.PUT, request, DefaultOAuth2AccessToken.class);

        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());

        /**
         * login to verify account has been locked
         */
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse1 = getTokenResponse(password, user.getEmail(), user.getPassword(), valid_login_clientId, valid_empty_secret);
        Assert.assertEquals(HttpStatus.BAD_REQUEST, tokenResponse1.getStatusCode());

        user.setLocked(false);
        String s3 = mapper.writeValueAsString(user);
        HttpEntity<String> request22 = new HttpEntity<>(s3, headers);
        ResponseEntity<DefaultOAuth2AccessToken> exchange22 = restTemplate.exchange(url, HttpMethod.PUT, request22, DefaultOAuth2AccessToken.class);

        Assert.assertEquals(HttpStatus.OK, exchange22.getStatusCode());

        /**
         * login to verify account has been unlocked
         */
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse12 = getTokenResponse(password, user.getEmail(), user.getPassword(), valid_login_clientId, valid_empty_secret);
        Assert.assertEquals(HttpStatus.OK, tokenResponse12.getStatusCode());
    }

    @Test
    public void delete_user() throws JsonProcessingException {
        ResourceOwner user = getUser();
        ResponseEntity<DefaultOAuth2AccessToken> user1 = createUser(user);

        String s = user1.getHeaders().getLocation().toString();
        String url = UserAction.proxyUrl + "/api" + "/resourceOwners/" + s;

        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse12 = getTokenResponse(password, user.getEmail(), user.getPassword(), valid_login_clientId, valid_empty_secret);

        Assert.assertEquals(HttpStatus.OK, tokenResponse12.getStatusCode());

        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(password, valid_username_root, valid_pwd, valid_login_clientId, valid_empty_secret);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenResponse.getBody().getValue());
        HttpEntity<Object> request = new HttpEntity<>(null, headers);
        ResponseEntity<Object> exchange = restTemplate.exchange(url, HttpMethod.DELETE, request, Object.class);

        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());

        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse123 = getTokenResponse(password, user.getEmail(), user.getPassword(), valid_login_clientId, valid_empty_secret);

        Assert.assertEquals(HttpStatus.BAD_REQUEST, tokenResponse123.getStatusCode());

    }

    @Test
    public void should_not_able_to_delete_root_user() {

        String url = UserAction.proxyUrl + "/api" + "/resourceOwners/" + root_index;

        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse12 = getTokenResponse(password, valid_username_root, valid_pwd, valid_login_clientId, valid_empty_secret);

        Assert.assertEquals(HttpStatus.OK, tokenResponse12.getStatusCode());

        /**
         * try w root
         */
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(password, valid_username_root, valid_pwd, valid_login_clientId, valid_empty_secret);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenResponse.getBody().getValue());
        HttpEntity<Object> request = new HttpEntity<>(null, headers);
        ResponseEntity<Object> exchange = restTemplate.exchange(url, HttpMethod.DELETE, request, Object.class);

        Assert.assertEquals(HttpStatus.BAD_REQUEST, exchange.getStatusCode());
        /**
         * try w admin, admin can not delete user
         */
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse2 = getTokenResponse(password, valid_username_admin, valid_pwd, valid_login_clientId, valid_empty_secret);
        HttpHeaders headers2 = new HttpHeaders();
        headers.setBearerAuth(tokenResponse2.getBody().getValue());
        HttpEntity<Object> request2 = new HttpEntity<>(null, headers2);
        ResponseEntity<Object> exchange2 = restTemplate.exchange(url, HttpMethod.DELETE, request2, Object.class);
        /**
         * forbidden if using proxy instead of HttpStatus.BAD_REQUEST
         */
        Assert.assertEquals(HttpStatus.FORBIDDEN, exchange2.getStatusCode());

    }

    private ResourceOwner getUser() {
        ResourceOwner resourceOwner = new ResourceOwner();
        resourceOwner.setPassword(UUID.randomUUID().toString().replace("-", ""));
        resourceOwner.setEmail(UUID.randomUUID().toString().replace("-", "") + "@gmail.com");
        return resourceOwner;
    }

    private ResourceOwner getUser_wrong_username() {
        ResourceOwner resourceOwner = new ResourceOwner();
        resourceOwner.setPassword(UUID.randomUUID().toString().replace("-", ""));
        resourceOwner.setEmail(UUID.randomUUID().toString());
        return resourceOwner;
    }

    private ResponseEntity<DefaultOAuth2AccessToken> getRegisterTokenResponse(String grantType, String clientId, String clientSecret) {
        String url = UserAction.proxyUrl + "/" + "oauth/token";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", grantType);
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        return restTemplate.exchange(url, HttpMethod.POST, request, DefaultOAuth2AccessToken.class);
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

    private ResponseEntity<DefaultOAuth2AccessToken> createUser(ResourceOwner user) throws JsonProcessingException {
        return createUser(user, valid_register_clientId);
    }

    private ResponseEntity<DefaultOAuth2AccessToken> createUser(ResourceOwner user, String clientId) throws JsonProcessingException {
        String url = UserAction.proxyUrl + "/api" + "/resourceOwners";
        ResponseEntity<DefaultOAuth2AccessToken> registerTokenResponse = getRegisterTokenResponse(client_credentials, clientId, valid_empty_secret);
        String value = registerTokenResponse.getBody().getValue();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(value);
        String s = mapper.writeValueAsString(user);
        HttpEntity<String> request = new HttpEntity<>(s, headers);
        return restTemplate.exchange(url, HttpMethod.POST, request, DefaultOAuth2AccessToken.class);
    }
}