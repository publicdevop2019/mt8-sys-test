package com.hw.helper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.runner.Description;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.test.web.client.TestRestTemplate;
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
    public static final String ACCESS_ROLE_ROOT = "/root";
    public static final String ACCESS_ROLE_ADMIN = "/admin";
    public static final String ACCESS_ROLE_APP = "/app";
    public static final String ACCESS_ROLE_USER = "/user";
    public static final String ACCESS_ROLE_PUBLIC = "/public";
    public static final String GRANT_TYPE_PASSWORD = "password";
    public static final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";
    public static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";
    public static final String CLIENT_ID_LOGIN_ID = "0C8AZZ16LZB4";
    public static final String CLIENT_ID_OAUTH2_ID = "0C8AZTODP4HT";
    public static final String CLIENT_ID_REGISTER_ID = "0C8B00098WLD";
    public static final String CLIENT_ID_OM_ID = "0C8B11ZYRXFM";
    public static final String CLIENT_ID_RIGHT_ROLE_NOT_SUFFICIENT_RESOURCE_ID = "0C8AZTODP4H8";
    public static final String CLIENT_ID_RESOURCE_ID = "0C8AZTODP4I0";
    public static final String CLIENT_ID_USER_PROFILE_ID = "0C8AZTODP4H7";
    public static final String CLIENT_ID_SAGA_ID = "0C8AZTODP4H1";
    public static final String CLIENT_ID_TEST_ID = "0C8B00CSATJ6";
    public static final String CLIENT_ID_BBS_ID = "bbs-ui";
    public static final String COMMON_CLIENT_SECRET = "root";
    public static final String EMPTY_CLIENT_SECRET = "";
    public static final String AUTHORIZE_STATE = "login";
    public static final String AUTHORIZE_RESPONSE_TYPE = "code";
    public static final String ACCOUNT_USERNAME_ROOT = "haolinwei2015@gmail.com";
    public static final String ACCOUNT_PASSWORD_ROOT = "root";
    public static final String ACCOUNT_USERNAME_ADMIN = "haolinwei2017@gmail.com";
    public static final String ACCOUNT_PASSWORD_ADMIN = "root";
    public static final String ACCOUNT_USERNAME_USER = "haolinwei2018@gmail.com";
    public static final String ACCOUNT_PASSWORD_USER = "root";
    public static final String SVC_NAME_AUTH = "/auth-svc";
    public static final String SVC_NAME_PRODUCT = "/product-svc";
    public static final String SVC_NAME_FILE_UPLOAD = "/file-upload-svc";
    public static final String SVC_NAME_PROFILE = "/profile-svc";
    public static final String SVC_NAME_BBS = "/bbs-svc";
    public static final String SVC_NAME_TEST = "/test-svc";
    public static final String OBJECT_MARKET_REDIRECT_URI = "http://localhost:4200/account";
    public static final String BBS_REDIRECT_URI = "http://localhost:3000/account";
    public static final String CLIENTS = "/clients";
    public static final String FILES = "/files";
    public List<ResourceOwner> testUser = new ArrayList<>();
    public ObjectMapper mapper = new ObjectMapper().configure(MapperFeature.USE_ANNOTATIONS, false).setSerializationInclusion(JsonInclude.Include.NON_NULL);
    public TestRestTemplate restTemplate = new TestRestTemplate();
    //        public static String proxyUrl = "http://api.manytreetechnology.com:" + 8111;
    public static String proxyUrl = "http://localhost:" + 8111;
    public static final String URL1 = proxyUrl + SVC_NAME_AUTH + "/users/app";
    public static final String URL_REGISTER = proxyUrl + SVC_NAME_AUTH + "/pending-users/app";
    public static final String URL = UserAction.proxyUrl + SVC_NAME_AUTH + "/oauth/token";
    public static String PROXY_URL_TOKEN = proxyUrl + SVC_NAME_AUTH + "/oauth/token";
    public static final String URL2 = UserAction.proxyUrl + UserAction.SVC_NAME_AUTH + CLIENTS + ACCESS_ROLE_ROOT;

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
            ResourceOwner resourceOwner1 = randomRegisterAnUser();
            ResourceOwner resourceOwner2 = randomRegisterAnUser();
            ResourceOwner resourceOwner3 = randomRegisterAnUser();
            ResourceOwner resourceOwner4 = randomRegisterAnUser();
            ResourceOwner resourceOwner5 = randomRegisterAnUser();
            testUser.add(resourceOwner1);
            testUser.add(resourceOwner2);
            testUser.add(resourceOwner3);
            testUser.add(resourceOwner4);
            testUser.add(resourceOwner5);
        }
    }

    /**
     * @return different GRANT_TYPE_PASSWORD client obj
     */
    public Client getClientAsNonResource(String... resourceIds) {
        Client client = getClientRaw(resourceIds);
        client.setGrantedAuthorities(Collections.singletonList(ClientAuthorityEnum.ROLE_BACKEND));
        client.setResourceIndicator(false);
        return client;
    }

    public Client getClientAsResource(String... resourceIds) {
        Client client = getClientRaw(resourceIds);
        client.setGrantedAuthorities(Arrays.asList(ClientAuthorityEnum.ROLE_BACKEND, ClientAuthorityEnum.ROLE_FIRST_PARTY));
        client.setResourceIndicator(true);
        return client;
    }

    public Client getInvalidClientAsResource(String... resourceIds) {
        Client client = getClientRaw(resourceIds);
        client.setGrantedAuthorities(Arrays.asList(ClientAuthorityEnum.ROLE_THIRD_PARTY));
        client.setResourceIndicator(true);
        return client;
    }

    public Client getClientRaw(String... resourceIds) {
        Client client = new Client();
        client.setName(UUID.randomUUID().toString().replace("-", ""));
        client.setClientSecret(UUID.randomUUID().toString().replace("-", ""));
        client.setGrantTypeEnums(new HashSet<>(Arrays.asList(GrantTypeEnum.PASSWORD)));
        client.setScopeEnums(new HashSet<>(Arrays.asList(ScopeEnum.READ)));
        client.setAccessTokenValiditySeconds(1800);
        client.setRefreshTokenValiditySeconds(null);
        client.setHasSecret(true);
        client.setResourceIds(new HashSet<>(Arrays.asList(resourceIds)));
        return client;
    }


    public ResponseEntity<DefaultOAuth2AccessToken> getTokenResponse(String grantType, String username, String userPwd, String clientId, String clientSecret) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", grantType);
        params.add("username", username);
        params.add("password", userPwd);
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        return restTemplate.exchange(UserAction.PROXY_URL_TOKEN, HttpMethod.POST, request, DefaultOAuth2AccessToken.class);
    }


    public ResponseEntity<String> createClient(Client client) {
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(GRANT_TYPE_PASSWORD, ACCOUNT_USERNAME_ROOT, ACCOUNT_PASSWORD_ROOT, CLIENT_ID_LOGIN_ID, EMPTY_CLIENT_SECRET);
        String bearer = tokenResponse.getBody().getValue();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(bearer);
        HttpEntity<Client> request = new HttpEntity<>(client, headers);
        return restTemplate.exchange(URL2, HttpMethod.POST, request, String.class);
    }

    public ResponseEntity<String> createClient(Client client, String changeId) {
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = getTokenResponse(GRANT_TYPE_PASSWORD, ACCOUNT_USERNAME_ROOT, ACCOUNT_PASSWORD_ROOT, CLIENT_ID_LOGIN_ID, EMPTY_CLIENT_SECRET);
        String bearer = tokenResponse.getBody().getValue();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(bearer);
        headers.set("changeId", changeId);
        headers.set("X-XSRF-TOKEN","123");
        headers.add(HttpHeaders.COOKIE,"XSRF-TOKEN=123");
        HttpEntity<Client> request = new HttpEntity<>(client, headers);
        return restTemplate.exchange(URL2, HttpMethod.POST, request, String.class);
    }

    public ResourceOwner randomCreateUserDraft() {
        return userCreateDraft(UUID.randomUUID().toString().replace("-", "") + "@gmail.com", UUID.randomUUID().toString().replace("-", ""));
    }

    public ResourceOwner userCreateDraft(String username, String password) {
        ResourceOwner resourceOwner = new ResourceOwner();
        resourceOwner.setEmail(username);
        resourceOwner.setPassword(password);
        return resourceOwner;
    }

    public ResourceOwner randomRegisterAnUser() {
        ResourceOwner random = randomCreateUserDraft();
        registerAnUser(random);
        return random;
    }

    public ResponseEntity<DefaultOAuth2AccessToken> getRegisterToken() {
        return getJwtClientCredential(CLIENT_ID_REGISTER_ID, EMPTY_CLIENT_SECRET);
    }

    public ResponseEntity<DefaultOAuth2AccessToken> registerAnUser(ResourceOwner user) {
        ResponseEntity<DefaultOAuth2AccessToken> registerTokenResponse = getRegisterToken();
        PendingResourceOwner pendingResourceOwner = new PendingResourceOwner();
        ResponseEntity<Void> voidResponseEntity = createPendingUser(user, registerTokenResponse.getBody().getValue(), pendingResourceOwner);
        return enterActivationCode(user, registerTokenResponse.getBody().getValue(), pendingResourceOwner);
    }

    public ResponseEntity<DefaultOAuth2AccessToken> enterActivationCode(ResourceOwner user, String registerToken, PendingResourceOwner pendingResourceOwner) {
        HttpHeaders headers1 = new HttpHeaders();
        headers1.setContentType(MediaType.APPLICATION_JSON);
        headers1.setBearerAuth(registerToken);
        headers1.set("changeId", UUID.randomUUID().toString());
        pendingResourceOwner.setPassword(user.getPassword());
        pendingResourceOwner.setActivationCode("123456");
        HttpEntity<PendingResourceOwner> request1 = new HttpEntity<>(pendingResourceOwner, headers1);
        return restTemplate.exchange(URL1, HttpMethod.POST, request1, DefaultOAuth2AccessToken.class);
    }

    public ResponseEntity<Void> createPendingUser(ResourceOwner user, String registerToken, PendingResourceOwner pendingResourceOwner) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(registerToken);
        headers.set("changeId", UUID.randomUUID().toString());
        pendingResourceOwner.setEmail(user.getEmail());
        HttpEntity<PendingResourceOwner> request = new HttpEntity<>(pendingResourceOwner, headers);
        return restTemplate.exchange(URL_REGISTER, HttpMethod.POST, request, Void.class);
    }

    public String registerResourceOwnerThenLogin() {
        ResourceOwner randomResourceOwner = randomCreateUserDraft();
        registerAnUser(randomResourceOwner);
        ResponseEntity<DefaultOAuth2AccessToken> loginTokenResponse = getJwtPassword(randomResourceOwner.getEmail(), randomResourceOwner.getPassword());
        return loginTokenResponse.getBody().getValue();
    }

    public ResponseEntity<DefaultOAuth2AccessToken> getJwtPassword(String username, String userPwd) {
        return getJwtPasswordWithClient(CLIENT_ID_LOGIN_ID, EMPTY_CLIENT_SECRET, username, userPwd);
    }

    public ResponseEntity<DefaultOAuth2AccessToken> getJwtPasswordWithClient(String clientId, String clientSecret, String username, String userPwd) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", GRANT_TYPE_PASSWORD);
        params.add("username", username);
        params.add("password", userPwd);
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        return restTemplate.exchange(PROXY_URL_TOKEN, HttpMethod.POST, request, DefaultOAuth2AccessToken.class);
    }

    public ResponseEntity<DefaultOAuth2AccessToken> getJwtPasswordRoot() {
        return getJwtPassword(ACCOUNT_USERNAME_ROOT, ACCOUNT_PASSWORD_ROOT);
    }

    public ResponseEntity<DefaultOAuth2AccessToken> getJwtPasswordAdmin() {
        return getJwtPassword(ACCOUNT_USERNAME_ADMIN, ACCOUNT_PASSWORD_ADMIN);
    }

    public ResponseEntity<DefaultOAuth2AccessToken> getJwtPasswordUser() {
        return getJwtPassword(ACCOUNT_USERNAME_USER, ACCOUNT_PASSWORD_USER);
    }

    public ResponseEntity<DefaultOAuth2AccessToken> getJwtClientCredential(String clientId, String clientSecret) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", GRANT_TYPE_CLIENT_CREDENTIALS);
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        return restTemplate.exchange(URL, HttpMethod.POST, request, DefaultOAuth2AccessToken.class);
    }

    public String getAuthorizationCodeTokenForUserAndClient(String username, String pwd, String clientId, String redirectUri) {
        ResponseEntity<DefaultOAuth2AccessToken> defaultOAuth2AccessTokenResponseEntity = getJwtPassword(username, pwd);
        String accessToken = defaultOAuth2AccessTokenResponseEntity.getBody().getValue();
        ResponseEntity<String> codeResp = getCodeResp(clientId, accessToken, redirectUri);
        String code = JsonPath.read(codeResp.getBody(), "$.authorize_code");

        ResponseEntity<DefaultOAuth2AccessToken> authorizationToken = getAuthorizationToken(code, redirectUri, clientId, EMPTY_CLIENT_SECRET);

        DefaultOAuth2AccessToken body = authorizationToken.getBody();
        return body.getValue();
    }

    public ResponseEntity<String> getCodeResp(String clientId, String bearerToken, String redirectUri) {
        String url = UserAction.proxyUrl + UserAction.SVC_NAME_AUTH + "/authorize";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("response_type", AUTHORIZE_RESPONSE_TYPE);
        params.add("client_id", clientId);
        params.add("state", AUTHORIZE_STATE);
        params.add("redirect_uri", redirectUri);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearerToken);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        return restTemplate.exchange(url, HttpMethod.POST, request, String.class);
    }

    public ResponseEntity<DefaultOAuth2AccessToken> getAuthorizationToken(String code, String redirect_uri, String clientId, String clientSecret) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", GRANT_TYPE_AUTHORIZATION_CODE);
        params.add("code", code);
        params.add("redirect_uri", redirect_uri);
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        return restTemplate.exchange(UserAction.PROXY_URL_TOKEN, HttpMethod.POST, request, DefaultOAuth2AccessToken.class);
    }


    public OrderDetail createOrderDetailForUser(String defaultUserToken) {
        ResponseEntity<ProductCustomerSummaryPaginatedRepresentation> randomProducts = readProductsByQuery();
        List<ProductCustomerSummaryPaginatedRepresentation.ProductSearchRepresentation> data = randomProducts.getBody().getData();

        ProductCustomerSummaryPaginatedRepresentation.ProductSearchRepresentation productSimple = data.get(new Random().nextInt(data.size()));
        String url = proxyUrl + SVC_NAME_PRODUCT + "/products/public/" + productSimple.getId();
        ResponseEntity<ProductDetailCustomRepresentation> exchange = restTemplate.exchange(url, HttpMethod.GET, null, ProductDetailCustomRepresentation.class);
        while (exchange.getBody().getSkus().stream().anyMatch(e -> e.getStorage().equals(0))) {
            ResponseEntity<ProductCustomerSummaryPaginatedRepresentation> randomProducts2 = readProductsByQuery();

            ProductCustomerSummaryPaginatedRepresentation.ProductSearchRepresentation productSimple2 = data.get(new Random().nextInt(randomProducts2.getBody().getData().size()));
            String url3 = proxyUrl + SVC_NAME_PRODUCT + "/products/public/" + productSimple2.getId();
            exchange = restTemplate.exchange(url3, HttpMethod.GET, null, ProductDetailCustomRepresentation.class);
        }
        SnapshotProduct snapshotProduct = selectProduct(exchange.getBody());
        String url2 = proxyUrl + SVC_NAME_PROFILE + "/cart/user";
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
        String url2 = proxyUrl + SVC_NAME_PROFILE + "/cart/user";
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


    public ResponseEntity<ProductCustomerSummaryPaginatedRepresentation> readProductsByQuery() {
        String query = "query=attr:835604081303552-服装$835602958278656-女&page=num:0,size:20,by:lowestPrice,order:asc";
        return readProductsByQuery(query);
    }

    public ResponseEntity<ProductCustomerSummaryPaginatedRepresentation> readProductsByQuery(String query) {
        String url = proxyUrl + SVC_NAME_PRODUCT + "/products/public?" + query;
        ResponseEntity<ProductCustomerSummaryPaginatedRepresentation> exchange = restTemplate.exchange(url, HttpMethod.GET, null, ProductCustomerSummaryPaginatedRepresentation.class);
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
        String url = proxyUrl + SVC_NAME_PRODUCT + "/catalogs/public";
        return restTemplate.exchange(url, HttpMethod.GET, null, CategorySummaryCustomerRepresentation.class);
    }

    public CategorySummaryCardRepresentation getFixedCatalogFromList() {
        ResponseEntity<CategorySummaryCustomerRepresentation> categories = getCatalogs();
        List<CategorySummaryCardRepresentation> body = categories.getBody().getData();
        return body.get(0);
    }

    public ResponseEntity<String> createRandomProductDetail(Integer actualStorage) {
        return createRandomProductDetail(actualStorage, null);
    }

    public ResponseEntity<String> createRandomProductDetail(Integer actualStorage, Integer orderStorage) {
        CategorySummaryCardRepresentation catalogFromList = getFixedCatalogFromList();
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
        String url = UserAction.proxyUrl + UserAction.SVC_NAME_PRODUCT + "/products/admin";
        return restTemplate.exchange(url, HttpMethod.POST, request, String.class);
    }

    public ResponseEntity<String> createRandomProductDetail(Set<String> attrKeys, Set<String> attrProd,BigDecimal price) {
        CategorySummaryCardRepresentation catalogFromList = getFixedCatalogFromList();
        ProductDetail randomProduct = getRandomProduct(catalogFromList, 10, 10,price);
        CreateProductAdminCommand createProductAdminCommand = new CreateProductAdminCommand();
        BeanUtils.copyProperties(randomProduct, createProductAdminCommand);
        createProductAdminCommand.setSkus(randomProduct.getProductSkuList());
        createProductAdminCommand.setStartAt(new Date().getTime());
        createProductAdminCommand.setAttributesKey(attrKeys);
        createProductAdminCommand.setAttributesProd(attrProd);
        String s1 = getDefaultAdminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(s1);
        HttpEntity<CreateProductAdminCommand> request = new HttpEntity<>(createProductAdminCommand, headers);
        String url = UserAction.proxyUrl + UserAction.SVC_NAME_PRODUCT + "/products/admin";
        return restTemplate.exchange(url, HttpMethod.POST, request, String.class);
    }

    public String getDefaultRootToken() {
        ResponseEntity<DefaultOAuth2AccessToken> loginTokenResponse = getJwtPassword(ACCOUNT_USERNAME_ROOT, ACCOUNT_PASSWORD_ROOT);
        return loginTokenResponse.getBody().getValue();
    }

    public String getDefaultAdminToken() {
        ResponseEntity<DefaultOAuth2AccessToken> loginTokenResponse = getJwtPassword(ACCOUNT_USERNAME_ADMIN, ACCOUNT_PASSWORD_ADMIN);
        return loginTokenResponse.getBody().getValue();
    }

    public String getDefaultUserToken() {
        ResponseEntity<DefaultOAuth2AccessToken> loginTokenResponse = getJwtPassword(ACCOUNT_USERNAME_USER, ACCOUNT_PASSWORD_USER);
        return loginTokenResponse.getBody().getValue();
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

    public Catalog generateRandomFrontendCatalog() {
        Catalog category = new Catalog();
        category.setName(UUID.randomUUID().toString().replace("-", ""));
        category.setCatalogType(CatalogType.FRONTEND);
        HashSet<String> strings = new HashSet<>();
        strings.add(UUID.randomUUID().toString().replace("-", ""));
        category.setAttributes(strings);
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

    public ProductDetail getRandomProduct(CategorySummaryCardRepresentation catalog, Integer actualStorage, Integer orderStorage, BigDecimal price) {
        ProductDetail productDetail = new ProductDetail();
        productDetail.setImageUrlSmall("http://www.test.com/"+UUID.randomUUID().toString().replace("-", ""));
        HashSet<String> objects = new HashSet<>();
        objects.add(UUID.randomUUID().toString().replace("-", ""));
        objects.add(UUID.randomUUID().toString().replace("-", ""));
        productDetail.setSpecification(objects);
        productDetail.setName(UUID.randomUUID().toString().replace("-", ""));
        productDetail.setAttributesKey(catalog.getAttributes());
        productDetail.setStatus(ProductStatus.AVAILABLE);
        int i = new Random().nextInt(2000);
        ProductSku productSku = new ProductSku();
        if (price == null)
            productSku.setPrice(BigDecimal.valueOf(new Random().nextDouble()).abs());
        else
            productSku.setPrice(price);
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

    public ProductDetail getRandomProduct(CategorySummaryCardRepresentation catalog, Integer actualStorage, Integer orderStorage) {
        return getRandomProduct(catalog, actualStorage, orderStorage, null);
    }

    public ProductDetail getRandomProduct(CategorySummaryCardRepresentation catalog) {
        return getRandomProduct(catalog, null, null);
    }

    public ProductDetail getRandomProduct(CategorySummaryCardRepresentation catalog, Integer actualStorage) {
        return getRandomProduct(catalog, actualStorage, null);
    }


    public String getRandomStr() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public ResponseEntity<ProductDetailCustomRepresentation> readRandomProductDetail() {
        ResponseEntity<ProductCustomerSummaryPaginatedRepresentation> randomProducts = readProductsByQuery();
        ProductCustomerSummaryPaginatedRepresentation.ProductSearchRepresentation productSimple = randomProducts.getBody().getData().get(new Random().nextInt(randomProducts.getBody().getData().size()));
        return readProductDetailById(productSimple.getId());
    }

    public ResponseEntity<ProductDetailCustomRepresentation> readProductDetailById(Long id) {
        String url = UserAction.proxyUrl + UserAction.SVC_NAME_PRODUCT + "/products/public/" + id;
        return restTemplate.exchange(url, HttpMethod.GET, null, ProductDetailCustomRepresentation.class);
    }

    public ResponseEntity<ProductDetailAdminRepresentation> readProductDetailByIdAdmin(Long id) {
        String defaultAdminToken = getDefaultAdminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(defaultAdminToken);
        HttpEntity<String> request = new HttpEntity<>(null, headers);
        String url = UserAction.proxyUrl + UserAction.SVC_NAME_PRODUCT + "/products/admin/" + id;
        return restTemplate.exchange(url, HttpMethod.GET, request, ProductDetailAdminRepresentation.class);
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
        String url = UserAction.proxyUrl + UserAction.SVC_NAME_BBS + "/private/posts";
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
        String url2 = UserAction.proxyUrl + UserAction.SVC_NAME_BBS + "/private/posts/" + post + "/comments";
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

    public String getBbsRootToken() {
        return getAuthorizationCodeTokenForUserAndClient(ACCOUNT_USERNAME_ROOT, ACCOUNT_PASSWORD_ROOT, CLIENT_ID_BBS_ID, BBS_REDIRECT_URI);
    }

    public String getBbsAdminToken() {
        return getAuthorizationCodeTokenForUserAndClient(ACCOUNT_USERNAME_ADMIN, ACCOUNT_PASSWORD_ADMIN, CLIENT_ID_BBS_ID, BBS_REDIRECT_URI);
    }

}
