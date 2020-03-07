package integration.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import helper.Category;
import helper.UserAction;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.List;

public class CategoryTest {
    private int randomServerPort = 8083;
    private TestRestTemplate restTemplate = new TestRestTemplate();
    UserAction action = new UserAction();
    public ObjectMapper mapper = new ObjectMapper().configure(MapperFeature.USE_ANNOTATIONS, false).setSerializationInclusion(JsonInclude.Include.NON_NULL);

    @Test
    public void shop_get_all_category() {
        ResponseEntity<List<Category>> categories = action.getCategories();
        Assert.assertEquals(HttpStatus.OK, categories.getStatusCode());
        Assert.assertNotEquals(0, categories.getBody().size());
    }

    @Test
    public void shop_create_category() {
        Category randomCategory = action.getRandomCategory();
        String s = null;
        try {
            s = mapper.writeValueAsString(randomCategory);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        String s1 = action.getDefaultAdminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(s1);
        HttpEntity<String> request = new HttpEntity<>(s, headers);

        String url = "http://localhost:" + randomServerPort + "/v1/api/" + "categories";
        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
        Assert.assertNotEquals(0, exchange.getHeaders().get("Location"));
    }

    @Test
    public void shop_update_category() {
        Category randomCategory = action.getRandomCategory();
        String s = null;
        try {
            s = mapper.writeValueAsString(randomCategory);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        String s1 = action.getDefaultAdminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(s1);
        HttpEntity<String> request = new HttpEntity<>(s, headers);

        String url = "http://localhost:" + randomServerPort + "/v1/api/" + "categories";
        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
        Assert.assertNotEquals(0, exchange.getHeaders().get("Location"));
        Category randomCategory2 = action.getRandomCategory();
        String s21 = null;
        try {
            s21 = mapper.writeValueAsString(randomCategory2);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        HttpHeaders headers2 = new HttpHeaders();
        headers2.setContentType(MediaType.APPLICATION_JSON);
        headers2.setBearerAuth(s1);
        HttpEntity<String> request2 = new HttpEntity<>(s21, headers2);
        ResponseEntity<String> exchange2 = restTemplate.exchange(url + "/" + exchange.getHeaders().getLocation().toString(), HttpMethod.PUT, request2, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange2.getStatusCode());
    }

    @Test
    public void shop_delete_category() {
        Category randomCategory = action.getRandomCategory();
        String s = null;
        try {
            s = mapper.writeValueAsString(randomCategory);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        String s1 = action.getDefaultAdminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(s1);
        HttpEntity<String> request = new HttpEntity<>(s, headers);

        String url = "http://localhost:" + randomServerPort + "/v1/api/" + "categories";
        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
        Assert.assertNotEquals(0, exchange.getHeaders().get("Location"));
        HttpHeaders headers2 = new HttpHeaders();
        headers2.setContentType(MediaType.APPLICATION_JSON);
        headers2.setBearerAuth(s1);
        HttpEntity<String> request2 = new HttpEntity<>(headers2);
        ResponseEntity<String> exchange2 = restTemplate.exchange(url + "/" + exchange.getHeaders().getLocation().toString(), HttpMethod.DELETE, request2, String.class);
        Assert.assertEquals(HttpStatus.OK, exchange2.getStatusCode());
    }
}
