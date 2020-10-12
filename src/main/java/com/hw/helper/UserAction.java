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
    public static final String TEST_TEST_VALUE = "835606767755264:S";
    public static final String ACCESS_ROLE_USER = "/user";
    public static final String ACCESS_ROLE_PUBLIC = "/public";
    public static final String ACCESS_ROLE_ADMIN = "/admin";
    public static final String ACCESS_ROLE_ROOT = "/root";
    public static final String ACCESS_ROLE_APP = "/app";
    //    @Autowired
//    FailedRecordRepo failedRecordRepo;
    public List<ResourceOwner> testUser = new ArrayList<>();
    public static String PASSWORD = "password";
    public static String CLIENT_CREDENTIALS = "client_credentials";
    public static String AUTHORIZATION_CODE = "authorization_code";
    public static String LOGIN_ID = "838330249904133";
    public static String REGISTER_ID = "838330249904135";
    public static String USER_PROFILE_ID = "838330249904145";
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

    public OrderDetail createOrderDetailForUser(String defaultUserToken) {
        ResponseEntity<ProductCustomerSummaryPaginatedRepresentation> randomProducts = readRandomProducts();
        List<ProductCustomerSummaryPaginatedRepresentation.ProductSearchRepresentation> data = randomProducts.getBody().getData();

        ProductCustomerSummaryPaginatedRepresentation.ProductSearchRepresentation productSimple = data.get(new Random().nextInt(data.size()));
        String url = proxyUrl + PRODUCT_SVC + "/products/public/" + productSimple.getId();
        ResponseEntity<ProductDetailCustomRepresentation> exchange = restTemplate.exchange(url, HttpMethod.GET, null, ProductDetailCustomRepresentation.class);
        while (exchange.getBody().getSkus().stream().anyMatch(e -> e.getStorage().equals(0))) {
            ResponseEntity<ProductCustomerSummaryPaginatedRepresentation> randomProducts2 = readRandomProducts();

            ProductCustomerSummaryPaginatedRepresentation.ProductSearchRepresentation productSimple2 = data.get(new Random().nextInt(randomProducts2.getBody().getData().size()));
            String url3 = proxyUrl + PRODUCT_SVC + "/products/public/" + productSimple2.getId();
            exchange = restTemplate.exchange(url3, HttpMethod.GET, null, ProductDetailCustomRepresentation.class);
        }
        SnapshotProduct snapshotProduct = selectProduct(exchange.getBody());
        String url2 = proxyUrl + PROFILE_SVC + "/cart/user";
        restTemplate.exchange(url2, HttpMethod.POST, getHttpRequest(defaultUserToken, snapshotProduct), String.class);

        ResponseEntity<SumTotalSnapshotProduct> exchange5 = restTemplate.exchange(url2, HttpMethod.GET, getHttpRequest(defaultUserToken), SumTotalSnapshotProduct.class);

        OrderDetail orderDetail = new OrderDetail();
        SnapshotAddress snapshotAddress = new SnapshotAddress();
        BeanUtils.copyProperties(getRandomAddress(), snapshotAddress);
        orderDetail.setAddress(snapshotAddress);
        orderDetail.setProductList(exchange5.getBody().getData());
        orderDetail.setPaymentType("wechatpay");
        BigDecimal reduce = orderDetail.getProductList().stream().map(e -> BigDecimal.valueOf(Double.parseDouble(e.getFinalPrice()))).reduce(BigDecimal.valueOf(0), BigDecimal::add);
        orderDetail.setPaymentAmt(reduce);
        return orderDetail;
    }

    public OrderDetail createBizOrderForUserAndProduct(String defaultUserToken, Long productId) {
        ResponseEntity<ProductDetailCustomRepresentation> productDetailCustomRepresentationResponseEntity = readProductDetailById(productId);
        SnapshotProduct snapshotProduct = selectProduct(productDetailCustomRepresentationResponseEntity.getBody());
        String url2 = proxyUrl + PROFILE_SVC + "/cart/user";
        restTemplate.exchange(url2, HttpMethod.POST, getHttpRequest(defaultUserToken, snapshotProduct), String.class);

        ResponseEntity<SumTotalSnapshotProduct> exchange5 = restTemplate.exchange(url2, HttpMethod.GET, getHttpRequest(defaultUserToken), SumTotalSnapshotProduct.class);

        OrderDetail orderDetail = new OrderDetail();
        SnapshotAddress snapshotAddress = new SnapshotAddress();
        BeanUtils.copyProperties(getRandomAddress(), snapshotAddress);
        orderDetail.setAddress(snapshotAddress);
        orderDetail.setProductList(exchange5.getBody().getData());
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

    public ResponseEntity<ProductCustomerSummaryPaginatedRepresentation> readRandomProducts() {
        String query = "query=attr:835604081303552-服装$835602958278656-女&page=num:0,size:20,by:lowestPrice,order:asc";
        String url = proxyUrl + PRODUCT_SVC + "/products/public?" + query;
        ResponseEntity<ProductCustomerSummaryPaginatedRepresentation> exchange = restTemplate.exchange(url, HttpMethod.GET, null, ProductCustomerSummaryPaginatedRepresentation.class);
        while (exchange.getBody().getData().size() == 0) {
            exchange = readRandomProducts();
        }
        return exchange;
    }

    public SnapshotProduct selectProduct(ProductDetailCustomRepresentation productDetail) {
        List<ProductOption> selectedOptions = productDetail.getSelectedOptions();
        List<String> priceVarCollection = new ArrayList<>();
        if (selectedOptions != null && selectedOptions.size() != 0) {
            // pick first option
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
        // pick first option
        List<ProductSkuCustomerRepresentation> productSkuList = productDetail.getSkus();
        snapshotProduct.setFinalPrice(calc.add(productSkuList.get(0).getPrice()).toString());
        snapshotProduct.setAttributesSales(productSkuList.get(0).getAttributesSales());
        return snapshotProduct;
    }

    public ResponseEntity<CategorySummaryCustomerRepresentation> getCatalogs() {
        String url = proxyUrl + PRODUCT_SVC + "/catalogs/public";
        return restTemplate.exchange(url, HttpMethod.GET, null, CategorySummaryCustomerRepresentation.class);
    }

    public CategorySummaryCardRepresentation getCatalogFromList() {
        ResponseEntity<CategorySummaryCustomerRepresentation> categories = getCatalogs();
        List<CategorySummaryCardRepresentation> body = categories.getBody().getData();
        int i = new Random().nextInt(body.size());
        return body.get(i);
    }

    public ResponseEntity<String> createRandomProductDetail(Integer actualStorage) {
        return createRandomProductDetail(actualStorage, null);
    }

    public ResponseEntity<String> createRandomProductDetail(Integer actualStorage, Integer orderStorage) {
        CategorySummaryCardRepresentation catalogFromList = getCatalogFromList();
        ProductDetail randomProduct = getRandomProduct(catalogFromList, actualStorage, orderStorage);
        CreateProductAdminCommand createProductAdminCommand = new CreateProductAdminCommand();
        BeanUtils.copyProperties(randomProduct, createProductAdminCommand);
        createProductAdminCommand.setSkus(randomProduct.getProductSkuList());
        createProductAdminCommand.setStartAt(new Date().getTime());
        String s1 = getDefaultAdminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(s1);
        HttpEntity<CreateProductAdminCommand> request = new HttpEntity<>(createProductAdminCommand, headers);
        String url = UserAction.proxyUrl + UserAction.PRODUCT_SVC + "/products/admin";
        return restTemplate.exchange(url, HttpMethod.POST, request, String.class);
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

    public Catalog generateRandomFrontendCatalog() {
        Catalog category = new Catalog();
        category.setName(UUID.randomUUID().toString().replace("-", ""));
        category.setCatalogType(CatalogType.FRONTEND);
        HashSet<String> strings = new HashSet<>();
        strings.add(UUID.randomUUID().toString().replace("-", ""));
        category.setAttributesSearch(strings);
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

    public ProductDetail getRandomProduct(CategorySummaryCardRepresentation catalog, Integer actualStorage, Integer orderStorage) {
        ProductDetail productDetail = new ProductDetail();
        productDetail.setImageUrlSmall(UUID.randomUUID().toString().replace("-", ""));
        HashSet<String> objects = new HashSet<>();
        objects.add(UUID.randomUUID().toString().replace("-", ""));
        objects.add(UUID.randomUUID().toString().replace("-", ""));
        productDetail.setSpecification(objects);
        productDetail.setName(UUID.randomUUID().toString().replace("-", ""));
        productDetail.setAttrKey(catalog.getAttributesKey());
        productDetail.setStatus(ProductStatus.AVAILABLE);
        int i = new Random().nextInt(2000);
        ProductSku productSku = new ProductSku();
        productSku.setPrice(BigDecimal.valueOf(new Random().nextDouble()).abs());
        productSku.setAttributesSales(new HashSet<>(List.of(TEST_TEST_VALUE)));

        if (actualStorage == null) {
            productSku.setStorageActual(i + new Random().nextInt(1000));
        } else {
            productSku.setStorageActual(actualStorage);
        }
        if (orderStorage == null) {
            productSku.setStorageOrder(i);
        } else {
            productSku.setStorageOrder(orderStorage);
        }
        productDetail.setProductSkuList(new ArrayList<>(List.of(productSku)));
        return productDetail;
    }

    public ProductDetail getRandomProduct(CategorySummaryCardRepresentation catalog) {
        return getRandomProduct(catalog, null, null);
    }

    public ProductDetail getRandomProduct(CategorySummaryCardRepresentation catalog, Integer actualStorage) {
        return getRandomProduct(catalog, actualStorage, null);
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
        String urlRegister = proxyUrl + AUTH_SVC + "/pending-users/public";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(registerToken);
        headers.set("changeId",UUID.randomUUID().toString());
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

        String url = proxyUrl + AUTH_SVC + "/users/public";
        HttpHeaders headers1 = new HttpHeaders();
        headers1.setContentType(MediaType.APPLICATION_JSON);
        headers1.setBearerAuth(registerToken);
        headers1.set("changeId",UUID.randomUUID().toString());
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

    public ResponseEntity<ProductDetailCustomRepresentation> readRandomProductDetail() {
        ResponseEntity<ProductCustomerSummaryPaginatedRepresentation> randomProducts = readRandomProducts();
        ProductCustomerSummaryPaginatedRepresentation.ProductSearchRepresentation productSimple = randomProducts.getBody().getData().get(new Random().nextInt(randomProducts.getBody().getData().size()));
        return readProductDetailById(productSimple.getId());
    }

    public ResponseEntity<ProductDetailCustomRepresentation> readProductDetailById(Long id) {
        String url = UserAction.proxyUrl + UserAction.PRODUCT_SVC + "/products/public/" + id;
        return restTemplate.exchange(url, HttpMethod.GET, null, ProductDetailCustomRepresentation.class);
    }

    public ResponseEntity<ProductDetailAdminRepresentation> readProductDetailByIdAdmin(Long id) {
        String defaultAdminToken = getDefaultAdminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(defaultAdminToken);
        HttpEntity<String> request = new HttpEntity<>(null, headers);
        String url = UserAction.proxyUrl + UserAction.PRODUCT_SVC + "/products/admin/" + id;
        return restTemplate.exchange(url, HttpMethod.GET, request, ProductDetailAdminRepresentation.class);
    }
}
