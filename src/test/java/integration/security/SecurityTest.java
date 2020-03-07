package integration.security;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import helper.ResourceOwner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.UUID;

@RunWith(SpringRunner.class)
public class SecurityTest {
    private String password = "password";
    private String client_credentials = "client_credentials";
    private String valid_clientId = "login-id";
    private String valid_register_clientId = "register-id";
    private String valid_clientId_no_refersh = "test-id";
    private String valid_empty_secret = "";
    private String valid_username_root = "haolinwei2015@gmail.com";
    private String valid_username_admin = "haolinwei2017@gmail.com";
    private String valid_username_user = "haolinwei2018@gmail.com";
    private String valid_pwd = "root";
    private String invalid_username = "root2@gmail.com";
    private String invalid_clientId = "root2";
    public ObjectMapper mapper = new ObjectMapper().configure(MapperFeature.USE_ANNOTATIONS, false).setSerializationInclusion(JsonInclude.Include.NON_NULL);
    private TestRestTemplate restTemplate = new TestRestTemplate();
    int randomServerPort = 8111;

    @Test
    public void user_modify_jwt_token_after_login() {
    }

    @Test
    public void use_modify_jwt_token_after_login_trying_to_access_other_profile() {

    }

    @Test
    public void trying_access_protected_api_without_jwt_token() {


    }


}
