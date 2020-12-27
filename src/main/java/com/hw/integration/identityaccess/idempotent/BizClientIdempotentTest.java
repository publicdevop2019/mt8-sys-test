package com.hw.integration.identityaccess.idempotent;

import com.hw.helper.Client;
import com.hw.helper.UserAction;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.UUID;

import static com.hw.concurrent.ProductServiceTest.assertConcurrent;
import static com.hw.helper.UserAction.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class BizClientIdempotentTest {
    private UserAction action = new UserAction();

    @Test
    public void create_client_w_same_changeId_two_times() {
        Client oldClient = action.getClientAsResource(CLIENT_ID_RESOURCE_ID);
        String s = UUID.randomUUID().toString();
        ResponseEntity<String> client1 = action.createClient(oldClient, s);
        Assert.assertEquals(HttpStatus.OK, client1.getStatusCode());
        ResponseEntity<String> client2 = action.createClient(oldClient, s);
        Assert.assertEquals(HttpStatus.OK, client2.getStatusCode());
    }

    @Test
    public void create_client_then_update_w_same_changeId_two_times() {
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = action.getJwtPassword(ACCOUNT_USERNAME_ROOT, ACCOUNT_PASSWORD_ROOT);
        String bearer = tokenResponse.getBody().getValue();
        String s = UUID.randomUUID().toString();
        Client oldClient = action.getClientAsResource(CLIENT_ID_RESOURCE_ID);
        ResponseEntity<String> client1 = action.createClient(oldClient, s);
        Assert.assertEquals(HttpStatus.OK, client1.getStatusCode());
        oldClient.setAccessTokenValiditySeconds(120);
        String url = UserAction.proxyUrl + UserAction.SVC_NAME_AUTH + CLIENTS + ACCESS_ROLE_ROOT + "/" + client1.getHeaders().getLocation().toString();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(bearer);
        String s2 = UUID.randomUUID().toString();
        headers.set("changeId", s2);
        oldClient.setVersion(0);
        HttpEntity<Client> request = new HttpEntity<>(oldClient, headers);
        ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
        oldClient.setVersion(1);
        ResponseEntity<String> exchange2 = action.restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange2.getStatusCode());
    }

    @Test
    public void create_client_then_delete_w_same_changeId_two_times() {
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = action.getJwtPassword(ACCOUNT_USERNAME_ROOT, ACCOUNT_PASSWORD_ROOT);
        String bearer = tokenResponse.getBody().getValue();
        String s = UUID.randomUUID().toString();
        Client oldClient = action.getClientAsResource(CLIENT_ID_RESOURCE_ID);
        ResponseEntity<String> client1 = action.createClient(oldClient, s);
        Assert.assertEquals(HttpStatus.OK, client1.getStatusCode());
        oldClient.setAccessTokenValiditySeconds(120);
        String url = UserAction.proxyUrl + UserAction.SVC_NAME_AUTH + CLIENTS + ACCESS_ROLE_ROOT + "/" + client1.getHeaders().getLocation().toString();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(bearer);
        String s2 = UUID.randomUUID().toString();
        headers.set("changeId", s2);
        HttpEntity<Client> request = new HttpEntity<>(oldClient, headers);
        ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.DELETE, request, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
        ResponseEntity<String> exchange2 = action.restTemplate.exchange(url, HttpMethod.DELETE, request, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange2.getStatusCode());
    }

    @Test
    public void create_client_w_same_changeId_two_times_concurrent() {
        Client oldClient = action.getClientAsResource(CLIENT_ID_RESOURCE_ID);
        String s = UUID.randomUUID().toString();
        Runnable runnable2 = () -> {
            ResponseEntity<String> client1 = action.createClient(oldClient, s);
            Assert.assertEquals(HttpStatus.OK, client1.getStatusCode());
        };
        ArrayList<Runnable> runnables = new ArrayList<>();
        runnables.add(runnable2);
        runnables.add(runnable2);
        try {
            assertConcurrent("", runnables, 30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void create_client_then_update_w_same_changeId_two_times_concurrent() {
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = action.getJwtPassword(ACCOUNT_USERNAME_ROOT, ACCOUNT_PASSWORD_ROOT);
        String bearer = tokenResponse.getBody().getValue();
        String s = UUID.randomUUID().toString();
        Client oldClient = action.getClientAsResource(CLIENT_ID_RESOURCE_ID);
        ResponseEntity<String> client1 = action.createClient(oldClient, s);
        Assert.assertEquals(HttpStatus.OK, client1.getStatusCode());
        oldClient.setAccessTokenValiditySeconds(120);
        String url = UserAction.proxyUrl + UserAction.SVC_NAME_AUTH + CLIENTS + ACCESS_ROLE_ROOT + "/" + client1.getHeaders().getLocation().toString();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(bearer);
        String s2 = UUID.randomUUID().toString();
        headers.set("changeId", s2);
        oldClient.setVersion(0);
        HttpEntity<Client> request = new HttpEntity<>(oldClient, headers);

        Runnable runnable2 = () -> {
            ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
            Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
        };
        ArrayList<Runnable> runnables = new ArrayList<>();
        runnables.add(runnable2);
        runnables.add(runnable2);
        try {
            assertConcurrent("", runnables, 30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void create_client_then_delete_w_same_changeId_two_times_concurrent() {
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = action.getJwtPassword(ACCOUNT_USERNAME_ROOT, ACCOUNT_PASSWORD_ROOT);
        String bearer = tokenResponse.getBody().getValue();
        String s = UUID.randomUUID().toString();
        Client oldClient = action.getClientAsResource(CLIENT_ID_RESOURCE_ID);
        ResponseEntity<String> client1 = action.createClient(oldClient, s);
        Assert.assertEquals(HttpStatus.OK, client1.getStatusCode());
        oldClient.setAccessTokenValiditySeconds(120);
        String url = UserAction.proxyUrl + UserAction.SVC_NAME_AUTH + CLIENTS + ACCESS_ROLE_ROOT + "/" + client1.getHeaders().getLocation().toString();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(bearer);
        String s2 = UUID.randomUUID().toString();
        headers.set("changeId", s2);
        HttpEntity<Client> request = new HttpEntity<>(oldClient, headers);

        Runnable runnable2 = () -> {
            ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.DELETE, request, String.class);
            Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
        };
        ArrayList<Runnable> runnables = new ArrayList<>();
        runnables.add(runnable2);
        runnables.add(runnable2);
        try {
            assertConcurrent("", runnables, 30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
