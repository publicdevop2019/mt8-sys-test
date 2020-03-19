package com.hw.helper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hw.entity.FailedRecord;
import com.hw.repo.FailedRecordRepo;
import org.junit.runner.Description;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.util.*;

@Component
public class UserAction {
    @Autowired
    FailedRecordRepo failedRecordRepo;
    public static String PASSWORD = "password";
    public static String CLIENT_CREDENTIALS = "client_credentials";
    public static String AUTHORIZATION_CODE = "authorization_code";
    public static String LOGIN_ID = "login-id";
    public static String REGISTER_ID = "register-id";
    public static String USER_PROFILE_ID = "user-profile";
    public static String USER_PROFILE_SECRET = "root";
    public static String CLIENT_SECRET = "";
    public static String AUTHORIZE_STATE = "login";
    public static String AUTHORIZE_RESPONSE_TYPE = "code";
    public static String REDIRECT_URI = "http://localhost:4200";
    public static String ROOT_USERNAME = "haolinwei2015@gmail.com";
    public static String ROOT_PASSWORD = "root";
    public static String ADMIN_USERNAME = "haolinwei2017@gmail.com";
    public static String ADMIN_PASSWORD = "root";
    public static String USER_USERNAME = "haolinwei2018@gmail.com";
    public static String USER_PASSWORD = "root";
    public ObjectMapper mapper = new ObjectMapper().configure(MapperFeature.USE_ANNOTATIONS, false).setSerializationInclusion(JsonInclude.Include.NON_NULL);
    public TestRestTemplate restTemplate = new TestRestTemplate();
    //    public static String proxyUrl = "http://www.ngform.com:" + 8111;
    public static String proxyUrl = "http://localhost:" + 8111;

    public void saveResult(Description description, UUID uuid) {
        FailedRecord failedRecord = new FailedRecord();
        failedRecord.setFailedTestMethod(description.getMethodName());
        failedRecord.setUuid(uuid.toString());
        failedRecordRepo.save(failedRecord);
    }

    public ResourceOwner registerResourceOwner() {
        ResourceOwner randomResourceOwner = getRandomResourceOwner();
        ResponseEntity<DefaultOAuth2AccessToken> registerTokenResponse = getRegisterTokenResponse();
        registerResourceOwner(randomResourceOwner, registerTokenResponse.getBody().getValue());
        return randomResourceOwner;
    }

    public OrderDetail createOrderDetailForUser(String defaultUserToken, String profileId1) {
        ResponseEntity<List<ProductSimple>> randomProducts = getRandomProducts();

        ProductSimple productSimple = randomProducts.getBody().get(new Random().nextInt(randomProducts.getBody().size()));
        while (productSimple.getActualStorage() <= 0 || productSimple.getOrderStorage() <= 0) {
            productSimple = randomProducts.getBody().get(new Random().nextInt(randomProducts.getBody().size()));
        }
        String url = proxyUrl + "/api/" + "productDetails/" + productSimple.getId();
        ResponseEntity<ProductDetail> exchange = restTemplate.exchange(url, HttpMethod.GET, null, ProductDetail.class);
        ProductDetail body = exchange.getBody();
        SnapshotProduct snapshotProduct = selectProduct(body);
        String url2 = proxyUrl + "/api/profiles/" + profileId1 + "/cart";
        restTemplate.exchange(url2, HttpMethod.POST, getHttpRequest(defaultUserToken, snapshotProduct), String.class);

        ParameterizedTypeReference<List<SnapshotProduct>> responseType = new ParameterizedTypeReference<>() {
        };
        ResponseEntity<List<SnapshotProduct>> exchange5 = restTemplate.exchange(url2, HttpMethod.GET, getHttpRequest(defaultUserToken), responseType);

        OrderDetail orderDetail = new OrderDetail();
        SnapshotAddress snapshotAddress = new SnapshotAddress();
        BeanUtils.copyProperties(getRandomAddress(), snapshotAddress);
        orderDetail.setAddress(snapshotAddress);
        orderDetail.setProductList(exchange5.getBody());
        orderDetail.setPaymentType("wechatpay");
        BigDecimal reduce = orderDetail.getProductList().stream().map(e -> BigDecimal.valueOf(Double.parseDouble(e.getFinalPrice()))).reduce(BigDecimal.valueOf(0), BigDecimal::add);
        orderDetail.setPaymentAmt(reduce);
        return orderDetail;
    }

    public String registerResourceOwnerThenLogin() {
        ResourceOwner randomResourceOwner = getRandomResourceOwner();
        ResponseEntity<DefaultOAuth2AccessToken> registerTokenResponse = getRegisterTokenResponse();
        registerResourceOwner(randomResourceOwner, registerTokenResponse.getBody().getValue());
        ResponseEntity<DefaultOAuth2AccessToken> loginTokenResponse = getLoginTokenResponse(randomResourceOwner.getEmail(), randomResourceOwner.getPassword());
        return loginTokenResponse.getBody().getValue();
    }

    public ResponseEntity<List<ProductSimple>> getRandomProducts() {
        ResponseEntity<List<Category>> categories = getCategories();
        List<Category> body = categories.getBody();
        int i = new Random().nextInt(body.size());
        Category category = body.get(i);
        String url = proxyUrl + "/api/" + "categories/" + category.getTitle() + "?pageNum=0&pageSize=20&sortBy=price&sortOrder=asc";
        ParameterizedTypeReference<List<ProductSimple>> responseType = new ParameterizedTypeReference<>() {
        };
        ResponseEntity<List<ProductSimple>> exchange = restTemplate.exchange(url, HttpMethod.GET, null, responseType);
        while (exchange.getBody().size() == 0 || exchange.getBody().size() == 1) {
            exchange = getRandomProducts();
        }
        return exchange;
    }

    public SnapshotProduct selectProduct(ProductDetail productDetail) {
        List<ProductOption> selectedOptions = productDetail.getSelectedOptions();
        List<String> priceVarCollection = new ArrayList<>();
        if (selectedOptions != null && selectedOptions.size() != 0) {
            /*
            pick first option
            * */
            selectedOptions.forEach(productOption -> {
                OptionItem optionItem = productOption.options.stream().findFirst().get();
                productOption.setOptions(List.of(optionItem));
                priceVarCollection.add(optionItem.getPriceVar());
            });
        }
        SnapshotProduct snapshotProduct = new SnapshotProduct();
        snapshotProduct.setName(productDetail.getName());
        snapshotProduct.setProductId(productDetail.getId().toString());
        snapshotProduct.setSelectedOptions(productDetail.getSelectedOptions());

        BigDecimal calc = new BigDecimal(0);
        for (String priceVar : priceVarCollection) {
            if (priceVar.contains("+")) {
                double v = Double.parseDouble(priceVar.replace("+", ""));
                BigDecimal bigDecimal = BigDecimal.valueOf(v);
                calc = calc.add(bigDecimal);
            } else if (priceVar.contains("-")) {
                double v = Double.parseDouble(priceVar.replace("-", ""));
                BigDecimal bigDecimal = BigDecimal.valueOf(v);
                calc = calc.subtract(bigDecimal);

            } else if (priceVar.contains("*")) {
                double v = Double.parseDouble(priceVar.replace("*", ""));
                BigDecimal bigDecimal = BigDecimal.valueOf(v);
                calc = calc.multiply(bigDecimal);
            } else {
            }
        }
        snapshotProduct.setFinalPrice(calc.add(productDetail.getPrice()).toString());
        return snapshotProduct;
    }

    public ResponseEntity<List<Category>> getCategories() {
        String url = proxyUrl + "/api/" + "categories";
        ParameterizedTypeReference<List<Category>> responseType = new ParameterizedTypeReference<>() {
        };
        return restTemplate.exchange(url, HttpMethod.GET, null, responseType);
    }

    public String getDefaultRootToken() {
        ResponseEntity<DefaultOAuth2AccessToken> loginTokenResponse = getLoginTokenResponse(ROOT_USERNAME, ROOT_PASSWORD);
        return loginTokenResponse.getBody().getValue();
    }

    public String getDefaultAdminToken() {
        ResponseEntity<DefaultOAuth2AccessToken> loginTokenResponse = getLoginTokenResponse(ADMIN_USERNAME, ADMIN_PASSWORD);
        return loginTokenResponse.getBody().getValue();
    }

    public String getDefaultUserToken() {
        ResponseEntity<DefaultOAuth2AccessToken> loginTokenResponse = getLoginTokenResponse(USER_USERNAME, USER_PASSWORD);
        return loginTokenResponse.getBody().getValue();
    }

    public String getProfileId(String authorizeToken) {
        String url = proxyUrl + "/api/" + "profiles/search";
        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.GET, getHttpRequest(authorizeToken), String.class);
        String profileId;
        if (exchange.getStatusCode() == HttpStatus.BAD_REQUEST) {
            profileId = createProfile(authorizeToken);
        } else {
            profileId = exchange.getBody();
        }
        return profileId;
    }

    public HttpEntity getHttpRequest(String authorizeToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(authorizeToken);
        return new HttpEntity<>(headers);
    }

    public HttpEntity getHttpRequest(String authorizeToken, Object object) {
        String s = null;
        try {
            s = mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(authorizeToken);
        return new HttpEntity<>(s, headers);
    }

    public String createProfile(String authorizeToken) {
        String url = proxyUrl + "/api/" + "profiles";
        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.POST, getHttpRequest(authorizeToken), String.class);
        return exchange.getHeaders().getLocation().toString();
    }

    public ResourceOwner getRandomResourceOwner() {
        ResourceOwner resourceOwner = new ResourceOwner();
        resourceOwner.setPassword(UUID.randomUUID().toString().replace("-", ""));
        resourceOwner.setEmail(UUID.randomUUID().toString().replace("-", "") + "@gmail.com");
        return resourceOwner;
    }

    public Category getRandomCategory() {
        Category category = new Category();
        category.setTitle(UUID.randomUUID().toString().replace("-", ""));
        category.setUrl(UUID.randomUUID().toString().replace("-", ""));
        return category;
    }

    public Address getRandomAddress() {
        Address address = new Address();
        address.setCity(UUID.randomUUID().toString().replace("-", ""));
        address.setCountry(UUID.randomUUID().toString().replace("-", ""));
        address.setFullName(UUID.randomUUID().toString().replace("-", ""));
        address.setLine1(UUID.randomUUID().toString().replace("-", ""));
        address.setLine2(UUID.randomUUID().toString().replace("-", ""));
        address.setPhoneNumber(UUID.randomUUID().toString().replace("-", ""));
        address.setPostalCode(UUID.randomUUID().toString().replace("-", ""));
        address.setProvince(UUID.randomUUID().toString().replace("-", ""));
        return address;
    }

    public ProductDetail getRandomProduct(String category) {
        ProductDetail productDetail = new ProductDetail();
        productDetail.setImageUrlSmall(UUID.randomUUID().toString().replace("-", ""));
        HashSet<String> objects = new HashSet<>();
        objects.add(UUID.randomUUID().toString().replace("-", ""));
        objects.add(UUID.randomUUID().toString().replace("-", ""));
        productDetail.setSpecification(objects);
        productDetail.setName(UUID.randomUUID().toString().replace("-", ""));
        productDetail.setCategory(category);
        productDetail.setOrderStorage(new Random().nextInt());
        productDetail.setActualStorage(new Random().nextInt());
        productDetail.setPrice(BigDecimal.valueOf(new Random().nextDouble()));
        return productDetail;
    }

    public ResponseEntity<DefaultOAuth2AccessToken> getRegisterTokenResponse(String grantType, String clientId, String clientSecret) {
        String url = proxyUrl + "/" + "oauth/token";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", grantType);
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        return restTemplate.exchange(url, HttpMethod.POST, request, DefaultOAuth2AccessToken.class);
    }

    public ResponseEntity<DefaultOAuth2AccessToken> getRegisterTokenResponse() {
        String url = proxyUrl + "/" + "oauth/token";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", CLIENT_CREDENTIALS);
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(REGISTER_ID, CLIENT_SECRET);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        return restTemplate.exchange(url, HttpMethod.POST, request, DefaultOAuth2AccessToken.class);
    }

    public ResponseEntity<DefaultOAuth2AccessToken> registerResourceOwner(ResourceOwner user, String registerToken) {
        String urlRegister = proxyUrl + "/api" + "/resourceOwners/register";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(registerToken);
        String s = null;
        PendingResourceOwner pendingResourceOwner = new PendingResourceOwner();
        pendingResourceOwner.setEmail(user.getEmail());
        try {
            s = mapper.writeValueAsString(pendingResourceOwner);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        HttpEntity<String> request = new HttpEntity<>(s, headers);
        restTemplate.exchange(urlRegister, HttpMethod.POST, request, DefaultOAuth2AccessToken.class);

        String url = proxyUrl + "/api" + "/resourceOwners";
        HttpHeaders headers1 = new HttpHeaders();
        headers1.setContentType(MediaType.APPLICATION_JSON);
        headers1.setBearerAuth(registerToken);
        String s2 = null;
        pendingResourceOwner.setPassword(user.getPassword());
        pendingResourceOwner.setActivationCode("123456");
        try {
            s2 = mapper.writeValueAsString(pendingResourceOwner);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        HttpEntity<String> request1 = new HttpEntity<>(s2, headers1);
        return restTemplate.exchange(url, HttpMethod.POST, request1, DefaultOAuth2AccessToken.class);
    }


    public ResponseEntity<DefaultOAuth2AccessToken> getLoginTokenResponse(String grantType, String username, String userPwd, String clientId, String clientSecret) {
        String url = proxyUrl + "/" + "oauth/token";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", grantType);
        params.add("username", username);
        params.add("password", userPwd);
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        return restTemplate.exchange(url, HttpMethod.POST, request, DefaultOAuth2AccessToken.class);
    }

    public ResponseEntity<DefaultOAuth2AccessToken> getLoginTokenResponse(String username, String userPwd) {
        String url = proxyUrl + "/" + "oauth/token";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", PASSWORD);
        params.add("username", username);
        params.add("password", userPwd);
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(LOGIN_ID, CLIENT_SECRET);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        return restTemplate.exchange(url, HttpMethod.POST, request, DefaultOAuth2AccessToken.class);
    }

    public ResponseEntity<String> getAuthorizationCodeResponseForClient(String clientId, String bearerToken, String response_type, String state, String redirectUri) {
        String url = proxyUrl + "/api/" + "authorize";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("response_type", response_type);
        params.add("client_id", clientId);
        params.add("state", state);
        params.add("redirect_uri", redirectUri);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearerToken);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        return restTemplate.exchange(url, HttpMethod.POST, request, String.class);
    }

    public ResponseEntity<String> getAuthorizationCodeResponseForClient(String clientId, String bearerToken) {
        String url = proxyUrl + "/api/" + "authorize";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("response_type", AUTHORIZE_RESPONSE_TYPE);
        params.add("client_id", clientId);
        params.add("state", AUTHORIZE_STATE);
        params.add("redirect_uri", REDIRECT_URI);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearerToken);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        return restTemplate.exchange(url, HttpMethod.POST, request, String.class);
    }

    public ResponseEntity<DefaultOAuth2AccessToken> getAuthorizationTokenForClient(String code, String redirectUri, String clientId, String clientSecret, String grantType) {
        String url = proxyUrl + "/" + "oauth/token";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", grantType);
        params.add("code", code);
        params.add("redirect_uri", redirectUri);
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        return restTemplate.exchange(url, HttpMethod.POST, request, DefaultOAuth2AccessToken.class);
    }

    public ResponseEntity<DefaultOAuth2AccessToken> getAuthorizationTokenForClient(String code, String redirectUri, String clientId, String clientSecret) {
        String url = proxyUrl + "/" + "oauth/token";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", AUTHORIZATION_CODE);
        params.add("code", code);
        params.add("redirect_uri", redirectUri);
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        return restTemplate.exchange(url, HttpMethod.POST, request, DefaultOAuth2AccessToken.class);
    }

    public ResponseEntity<DefaultOAuth2AccessToken> getClientCredentialResponse(String clientId, String clientSecret) {
        String url = UserAction.proxyUrl + "/" + "oauth/token";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", CLIENT_CREDENTIALS);
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        return restTemplate.exchange(url, HttpMethod.POST, request, DefaultOAuth2AccessToken.class);
    }

    public String getOrderId(HttpHeaders headers) {
        String s = headers.getLocation().toString();
        Integer start = s.indexOf("product_id");
        String searchStr = s.substring(start);
        return searchStr.substring(searchStr.indexOf('=') + 1, searchStr.indexOf('&'));
    }
}
