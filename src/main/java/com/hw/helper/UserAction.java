package com.hw.helper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.junit.runner.Description;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
@Slf4j
public class UserAction {
    //    @Autowired
//    FailedRecordRepo failedRecordRepo;
    public List<ResourceOwner> testUser = new ArrayList<>();
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
    public static String AUTH_SVC = "/auth-svc";
    public static String PRODUCT_SVC = "/product-svc";
    public static String PROFILE_SVC = "/profile-svc";
    public static String BBS_ID = "bbs-ui";
    public static String BBS_SVC = "/bbs-svc";
    public static String BBS_REDIRECT_URI = "http://localhost:3000/account";
    public ObjectMapper mapper = new ObjectMapper().configure(MapperFeature.USE_ANNOTATIONS, false).setSerializationInclusion(JsonInclude.Include.NON_NULL);
    public TestRestTemplate restTemplate = new TestRestTemplate();
    //        public static String proxyUrl = "http://api.manytreetechnology.com:" + 8111;
    public static String proxyUrl = "http://localhost:" + 8111;
    public static String PROXY_URL_TOKEN = proxyUrl + AUTH_SVC + "/oauth/token";

    public void saveResult(Description description, UUID uuid) {
//        FailedRecord failedRecord = new FailedRecord();
//        failedRecord.setFailedTestMethod(description.getMethodName());
//        failedRecord.setUuid(uuid.toString());
//        failedRecordRepo.save(failedRecord);
    }

    public UserAction() {
    }

    public void initTestUser() {
        if (testUser.size() == 0) {
            ResourceOwner resourceOwner1 = registerResourceOwner();
            ResourceOwner resourceOwner2 = registerResourceOwner();
            ResourceOwner resourceOwner3 = registerResourceOwner();
            ResourceOwner resourceOwner4 = registerResourceOwner();
            ResourceOwner resourceOwner5 = registerResourceOwner();
            testUser.add(resourceOwner1);
            testUser.add(resourceOwner2);
            testUser.add(resourceOwner3);
            testUser.add(resourceOwner4);
            testUser.add(resourceOwner5);
            String defaultUserToken1 = getPasswordFlowTokenResponse(resourceOwner1.getEmail(), resourceOwner1.getPassword()).getBody().getValue();
            String defaultUserToken2 = getPasswordFlowTokenResponse(resourceOwner2.getEmail(), resourceOwner2.getPassword()).getBody().getValue();
            String defaultUserToken3 = getPasswordFlowTokenResponse(resourceOwner3.getEmail(), resourceOwner3.getPassword()).getBody().getValue();
            String defaultUserToken4 = getPasswordFlowTokenResponse(resourceOwner4.getEmail(), resourceOwner4.getPassword()).getBody().getValue();
            String defaultUserToken5 = getPasswordFlowTokenResponse(resourceOwner5.getEmail(), resourceOwner5.getPassword()).getBody().getValue();
            getProfileId(defaultUserToken1);
            getProfileId(defaultUserToken2);
            getProfileId(defaultUserToken3);
            getProfileId(defaultUserToken4);
            getProfileId(defaultUserToken5);
        }
    }

    public ResourceOwner registerResourceOwner() {
        ResourceOwner randomResourceOwner = getRandomResourceOwner();
        ResponseEntity<DefaultOAuth2AccessToken> registerTokenResponse = getRegisterTokenResponse();
        registerResourceOwner(randomResourceOwner, registerTokenResponse.getBody().getValue());
        return randomResourceOwner;
    }

    public String getOrderId(String jwt, String profileId) {
        String url2 = proxyUrl + PROFILE_SVC + "/profiles/" + profileId + "/orders/id";
        ResponseEntity<String> exchange = restTemplate.exchange(url2, HttpMethod.GET, getHttpRequest(jwt), String.class);
        return exchange.getHeaders().getLocation().toString();
    }

    public OrderDetail createOrderDetailForUser(String defaultUserToken, String profileId1) {
        ResponseEntity<List<ProductSimple>> randomProducts = getRandomProducts();

        ProductSimple productSimple = randomProducts.getBody().get(new Random().nextInt(randomProducts.getBody().size()));
        while (productSimple.getOrderStorage() <= 0) {
            productSimple = randomProducts.getBody().get(new Random().nextInt(randomProducts.getBody().size()));
        }
        String url = proxyUrl + PRODUCT_SVC + "/productDetails/" + productSimple.getId();
        ResponseEntity<ProductDetail> exchange = restTemplate.exchange(url, HttpMethod.GET, null, ProductDetail.class);
        ProductDetail body = exchange.getBody();
        SnapshotProduct snapshotProduct = selectProduct(body);
        String url2 = proxyUrl + PROFILE_SVC + "/profiles/" + profileId1 + "/cart";
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
    public OrderDetail createBizOrderForUserAndProduct(String defaultUserToken, String profileId1, ProductSimple productSimple) {
        String url = proxyUrl + PRODUCT_SVC + "/productDetails/" + productSimple.getId();
        ResponseEntity<ProductDetail> exchange = restTemplate.exchange(url, HttpMethod.GET, null, ProductDetail.class);
        ProductDetail body = exchange.getBody();
        SnapshotProduct snapshotProduct = selectProduct(body);
        String url2 = proxyUrl + PROFILE_SVC + "/profiles/" + profileId1 + "/cart";
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
        ResponseEntity<DefaultOAuth2AccessToken> loginTokenResponse = getPasswordFlowTokenResponse(randomResourceOwner.getEmail(), randomResourceOwner.getPassword());
        return loginTokenResponse.getBody().getValue();
    }

    public ResponseEntity<List<ProductSimple>> getRandomProducts() {
        ResponseEntity<CategorySummaryCustomerRepresentation> categories = getCategories();
        List<CategorySummaryCardRepresentation> body = categories.getBody().getCategoryList();
        int i = new Random().nextInt(body.size());
        CategorySummaryCardRepresentation category = body.get(i);
        String url = proxyUrl + PRODUCT_SVC + "/categories/" + category.getTitle() + "?pageNum=0&pageSize=20&sortBy=price&sortOrder=asc";
        ParameterizedTypeReference<List<ProductSimple>> responseType = new ParameterizedTypeReference<>() {
        };
        ResponseEntity<List<ProductSimple>> exchange = restTemplate.exchange(url, HttpMethod.GET, null, responseType);
        while (exchange.getBody().size() == 0) {
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

    public ResponseEntity<CategorySummaryCustomerRepresentation> getCategories() {
        String url = proxyUrl + PRODUCT_SVC + "/categories";
        return restTemplate.exchange(url, HttpMethod.GET, null, CategorySummaryCustomerRepresentation.class);
    }

    public String getDefaultRootToken() {
        ResponseEntity<DefaultOAuth2AccessToken> loginTokenResponse = getPasswordFlowTokenResponse(ROOT_USERNAME, ROOT_PASSWORD);
        return loginTokenResponse.getBody().getValue();
    }

    public String getDefaultAdminToken() {
        ResponseEntity<DefaultOAuth2AccessToken> loginTokenResponse = getPasswordFlowTokenResponse(ADMIN_USERNAME, ADMIN_PASSWORD);
        return loginTokenResponse.getBody().getValue();
    }

    public String getDefaultUserToken() {
        ResponseEntity<DefaultOAuth2AccessToken> loginTokenResponse = getPasswordFlowTokenResponse(USER_USERNAME, USER_PASSWORD);
        return loginTokenResponse.getBody().getValue();
    }

    public String getProfileId(String authorizeToken) {
        String url = proxyUrl + PROFILE_SVC + "/profiles/search";
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
        headers.setAcceptCharset(List.of(StandardCharsets.UTF_8));
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
        headers.setAcceptCharset(List.of(StandardCharsets.UTF_8));
        headers.setBearerAuth(authorizeToken);
        return new HttpEntity<>(s, headers);
    }

    public String createProfile(String authorizeToken) {
        String url = proxyUrl + PROFILE_SVC + "/profiles";
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
        int i = new Random().nextInt(2000);
        productDetail.setOrderStorage(i);
        productDetail.setActualStorage(i + new Random().nextInt(1000));
        productDetail.setPrice(BigDecimal.valueOf(new Random().nextDouble()).abs());
        return productDetail;
    }

    public ResponseEntity<DefaultOAuth2AccessToken> getRegisterTokenResponse() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", CLIENT_CREDENTIALS);
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(REGISTER_ID, CLIENT_SECRET);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        return restTemplate.exchange(PROXY_URL_TOKEN, HttpMethod.POST, request, DefaultOAuth2AccessToken.class);
    }

    public ResponseEntity<DefaultOAuth2AccessToken> registerResourceOwner(ResourceOwner user, String registerToken) {
        String urlRegister = proxyUrl + AUTH_SVC + "/resourceOwners/register";
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

        String url = proxyUrl + AUTH_SVC + "/resourceOwners";
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


    public ResponseEntity<DefaultOAuth2AccessToken> getPasswordFlowTokenResponse(String username, String userPwd) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", PASSWORD);
        params.add("username", username);
        params.add("password", userPwd);
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(LOGIN_ID, CLIENT_SECRET);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        return restTemplate.exchange(PROXY_URL_TOKEN, HttpMethod.POST, request, DefaultOAuth2AccessToken.class);
    }

    public ResponseEntity<DefaultOAuth2AccessToken> getClientCredentialFlowResponse(String clientId, String clientSecret) {
        String url = UserAction.proxyUrl + AUTH_SVC + "/oauth/token";
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

    public String getRandomStr() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public String getBbsRootToken() {
        return getAuthorizationCodeTokenForUserAndClient(ROOT_USERNAME, ROOT_PASSWORD, BBS_ID, BBS_REDIRECT_URI);
    }

    public String getBbsAdminToken() {
        return getAuthorizationCodeTokenForUserAndClient(ADMIN_USERNAME, ADMIN_PASSWORD, BBS_ID, BBS_REDIRECT_URI);
    }

    public String getAuthorizationCodeTokenForUserAndClient(String username, String pwd, String clientId, String redirectUri) {
        ResponseEntity<DefaultOAuth2AccessToken> defaultOAuth2AccessTokenResponseEntity = getPasswordFlowTokenResponse(username, pwd);
        String accessToken = defaultOAuth2AccessTokenResponseEntity.getBody().getValue();
        ResponseEntity<String> codeResp = getCodeResp(clientId, accessToken, redirectUri);
        String code = JsonPath.read(codeResp.getBody(), "$.authorize_code");

        ResponseEntity<DefaultOAuth2AccessToken> authorizationToken = getAuthorizationToken(code, redirectUri, clientId);

        DefaultOAuth2AccessToken body = authorizationToken.getBody();
        return body.getValue();
    }

    private ResponseEntity<String> getCodeResp(String clientId, String bearerToken, String redirectUri) {
        String url = UserAction.proxyUrl + UserAction.AUTH_SVC + "/authorize";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("response_type", AUTHORIZE_RESPONSE_TYPE);
        params.add("client_id", clientId);
        params.add("state", AUTHORIZE_STATE);
        params.add("redirect_uri", redirectUri);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearerToken);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        return restTemplate.exchange(url, HttpMethod.POST, request, String.class);
    }

    private ResponseEntity<DefaultOAuth2AccessToken> getAuthorizationToken(String code, String redirect_uri, String clientId) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", AUTHORIZATION_CODE);
        params.add("code", code);
        params.add("redirect_uri", redirect_uri);
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, CLIENT_SECRET);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        return restTemplate.exchange(UserAction.PROXY_URL_TOKEN, HttpMethod.POST, request, DefaultOAuth2AccessToken.class);
    }

    public String createPost(String topic) {
        String s1 = getBbsRootToken();
        return createPost(topic, s1);
    }

    public String createPost(String topic, String bearerToken) {
        CreatePostCommand createPostCommand = new CreatePostCommand();
        createPostCommand.setTopic(topic);
        createPostCommand.setContent(getRandomStr());
        createPostCommand.setTitle(getRandomStr());
        String s = null;
        try {
            s = mapper.writeValueAsString(createPostCommand);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(bearerToken);
        HttpEntity<String> request = new HttpEntity<>(s, headers);
        String url = UserAction.proxyUrl + UserAction.BBS_SVC + "/private/posts";
        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
        return exchange.getHeaders().get("Location").get(0);
    }

    public void createCommentForPost(String post) {
        String s1 = getBbsRootToken();
        createCommentForPost(post, s1);
    }

    public void createCommentForPost(String post, String bearerToken) {
        String randomStr2 = getRandomStr();
        CreateCommentCommand createCommentCommand = new CreateCommentCommand();
        createCommentCommand.setContent(randomStr2);
        String url2 = UserAction.proxyUrl + UserAction.BBS_SVC + "/private/posts/" + post + "/comments";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(bearerToken);
        String s = null;
        try {
            s = mapper.writeValueAsString(createCommentCommand);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        HttpEntity<String> request = new HttpEntity<>(s, headers);
        ResponseEntity<String> exchange = restTemplate.exchange(url2, HttpMethod.POST, request, String.class);
    }
}
