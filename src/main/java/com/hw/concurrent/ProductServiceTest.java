package com.hw.concurrent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hw.helper.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static com.hw.helper.UserAction.USER_PROFILE_ID;
import static com.hw.helper.UserAction.USER_PROFILE_SECRET;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@Slf4j
@SpringBootTest
public class ProductServiceTest {
    @Autowired
    UserAction action;
    ObjectMapper mapper = new ObjectMapper();
    UUID uuid;
    @Rule
    public TestWatcher watchman = new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
            action.saveResult(description, uuid);
            log.error("test failed, method {}, uuid {}", description.getMethodName(), uuid);
        }
    };

    @Before
    public void setUp() {
        uuid = UUID.randomUUID();
        action.restTemplate.getRestTemplate().setInterceptors(Collections.singletonList(new OutgoingReqInterceptor(uuid)));
    }

    @Test
    public void concurrentValidation() {
        String url = UserAction.proxyUrl + UserAction.PRODUCT_SVC + "/productDetails/validate";
        OptionItem optionItem1 = new OptionItem("scoj", "+8.41");
        OptionItem optionItem2 = new OptionItem("yla", "+7.39");
        OptionItem optionItem3 = new OptionItem("azqv", "-2.79");
        OptionItem optionItem4 = new OptionItem("1", "*1");

        ProductOption reqAddOn1 = new ProductOption();
        reqAddOn1.setTitle("pp");
        reqAddOn1.setOptions(new ArrayList<>());
        reqAddOn1.getOptions().add(optionItem1);

        ProductOption reqAddOn2 = new ProductOption();
        reqAddOn2.setTitle("uqjcpae");
        reqAddOn2.setOptions(new ArrayList<>());
        reqAddOn2.getOptions().add(optionItem2);

        ProductOption reqAddOn3 = new ProductOption();
        reqAddOn3.setTitle("wpufhar");
        reqAddOn3.setOptions(new ArrayList<>());
        reqAddOn3.getOptions().add(optionItem3);

        ProductOption reqAddOn4 = new ProductOption();
        reqAddOn4.setTitle("Qty");
        reqAddOn4.setOptions(new ArrayList<>());
        reqAddOn4.getOptions().add(optionItem4);

        SnapshotProduct snapshotProduct = new SnapshotProduct();

        snapshotProduct.setFinalPrice("23.27");
        snapshotProduct.setProductId("15370");

        snapshotProduct.setSelectedOptions(new ArrayList<>());
        snapshotProduct.getSelectedOptions().add(reqAddOn1);
        snapshotProduct.getSelectedOptions().add(reqAddOn2);
        snapshotProduct.getSelectedOptions().add(reqAddOn3);
        snapshotProduct.getSelectedOptions().add(reqAddOn4);

        ArrayList<SnapshotProduct> products = new ArrayList<>();
        products.add(snapshotProduct);
        products.add(snapshotProduct);
        products.add(snapshotProduct);
        products.add(snapshotProduct);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(action.getClientCredentialFlowResponse(USER_PROFILE_ID, USER_PROFILE_SECRET).getBody().getValue());
        HttpEntity<List<SnapshotProduct>> listHttpEntity = new HttpEntity<>(products, headers);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                ResponseEntity<Object> exchange = action.restTemplate.exchange(url, HttpMethod.POST, listHttpEntity, Object.class);
                Assert.assertEquals(200, exchange.getStatusCodeValue());
            }
        };
        ArrayList<Runnable> runnables = new ArrayList<>();
        IntStream.range(0, 5).forEach(e -> {
            runnables.add(runnable);
        });
        try {
            assertConcurrent("", runnables, 30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void create_product_then_concurrent_decrease() {
        AtomicInteger iniOrderStorage = new AtomicInteger(1000);
        String url2 = UserAction.proxyUrl + UserAction.PRODUCT_SVC + "/productDetails/decreaseStorageBy?optToken=";
        ResponseEntity<List<Category>> categories = action.getCategories();
        List<Category> body = categories.getBody();
        int i = new Random().nextInt(body.size());
        Category category = body.get(i);
        ProductDetail randomProduct = action.getRandomProduct(category.getTitle());
        randomProduct.setOrderStorage(iniOrderStorage.get());
        String s = null;
        try {
            s = mapper.writeValueAsString(randomProduct);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        String s1 = action.getDefaultAdminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(s1);
        HttpEntity<String> request = new HttpEntity<>(s, headers);

        String url = UserAction.proxyUrl + UserAction.PRODUCT_SVC + "/productDetails";
        ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.POST, request, String.class);
        String productId = exchange.getHeaders().getLocation().toString();

        Integer threadCount = 50;
        HashMap<String, String> stringStringHashMap = new HashMap<>();
        stringStringHashMap.put(productId, "1");
        HttpHeaders headers2 = new HttpHeaders();
        headers2.setBearerAuth(action.getClientCredentialFlowResponse(USER_PROFILE_ID, USER_PROFILE_SECRET).getBody().getValue());
        HttpEntity<Object> listHttpEntity = new HttpEntity<>(stringStringHashMap, headers2);
        ArrayList<Integer> integers = new ArrayList<>();
        integers.add(200);
        integers.add(400);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                ResponseEntity<Object> exchange = action.restTemplate.exchange(url2 + UUID.randomUUID().toString(), HttpMethod.PUT, listHttpEntity, Object.class);
                if (exchange.getStatusCodeValue() == 200) {
                    iniOrderStorage.decrementAndGet();
                }
                Assert.assertTrue("expected status code but is " + exchange.getStatusCodeValue(), integers.contains(exchange.getStatusCodeValue()));
            }
        };
        ArrayList<Runnable> runnables = new ArrayList<>();
        IntStream.range(0, threadCount).forEach(e -> {
            runnables.add(runnable);
        });
        try {
            assertConcurrent("", runnables, 30000);
            // get product order count
            ResponseEntity<ProductDetail> exchange1 = action.restTemplate.exchange(url + "/" + productId, HttpMethod.GET, null, ProductDetail.class);
            assertTrue("remain storage should be " + iniOrderStorage.get(), exchange1.getBody().getOrderStorage().equals(iniOrderStorage.get()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void create_product_then_concurrent_decrease_same_opt() {
        Integer iniOrderStorage = 1000;
        String s2 = UUID.randomUUID().toString();
        String url2 = UserAction.proxyUrl + UserAction.PRODUCT_SVC + "/productDetails/decreaseStorageBy?optToken=" + s2;
        ResponseEntity<List<Category>> categories = action.getCategories();
        List<Category> body = categories.getBody();
        int i = new Random().nextInt(body.size());
        Category category = body.get(i);
        ProductDetail randomProduct = action.getRandomProduct(category.getTitle());
        randomProduct.setOrderStorage(iniOrderStorage);
        String s = null;
        try {
            s = mapper.writeValueAsString(randomProduct);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        String s1 = action.getDefaultAdminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(s1);
        HttpEntity<String> request = new HttpEntity<>(s, headers);

        String url = UserAction.proxyUrl + UserAction.PRODUCT_SVC + "/productDetails";
        ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.POST, request, String.class);
        String productId = exchange.getHeaders().getLocation().toString();

        Integer threadCount = 20;
        Integer expected = iniOrderStorage - 1;
        HashMap<String, String> stringStringHashMap = new HashMap<>();
        stringStringHashMap.put(productId, "1");
        HttpHeaders headers2 = new HttpHeaders();
        headers2.setBearerAuth(action.getClientCredentialFlowResponse(USER_PROFILE_ID, USER_PROFILE_SECRET).getBody().getValue());
        HttpEntity<Object> listHttpEntity = new HttpEntity<>(stringStringHashMap, headers2);
        ArrayList<Integer> integers = new ArrayList<>();
        integers.add(200);
        integers.add(400);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                ResponseEntity<Object> exchange = action.restTemplate.exchange(url2, HttpMethod.PUT, listHttpEntity, Object.class);
                Assert.assertTrue("expected status code but is " + exchange.getStatusCodeValue(), integers.contains(exchange.getStatusCodeValue()));
            }
        };
        ArrayList<Runnable> runnables = new ArrayList<>();
        IntStream.range(0, threadCount).forEach(e -> {
            runnables.add(runnable);
        });
        try {
            assertConcurrent("", runnables, 30000);
            // get product order count
            ResponseEntity<ProductDetail> exchange1 = action.restTemplate.exchange(url + "/" + productId, HttpMethod.GET, null, ProductDetail.class);
            assertTrue("remain storage should be " + expected, exchange1.getBody().getOrderStorage().equals(expected));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * if lock is pessimistic then deadlock exception will detected
     */
    public void create_three_product_then_concurrent_decrease_diff_product_concurrent(Integer initialStorage, Integer threads) {
        Integer initial = initialStorage;
        AtomicInteger iniOrderStorage = new AtomicInteger(initial);
        AtomicInteger iniOrderStorage2 = new AtomicInteger(initial);
        AtomicInteger iniOrderStorage3 = new AtomicInteger(initial);
        String url2 = UserAction.proxyUrl + UserAction.PRODUCT_SVC + "/productDetails/decreaseStorageBy?optToken=";
        ResponseEntity<List<Category>> categories = action.getCategories();
        List<Category> body = categories.getBody();
        int i = new Random().nextInt(body.size());
        Category category = body.get(i);
        ProductDetail randomProduct = action.getRandomProduct(category.getTitle());
        ProductDetail randomProduct2 = action.getRandomProduct(category.getTitle());
        ProductDetail randomProduct3 = action.getRandomProduct(category.getTitle());
        randomProduct.setOrderStorage(iniOrderStorage.get());
        randomProduct2.setOrderStorage(iniOrderStorage2.get());
        randomProduct3.setOrderStorage(iniOrderStorage3.get());
        String s = null;
        String s2 = null;
        String s3 = null;
        try {
            s = mapper.writeValueAsString(randomProduct);
            s2 = mapper.writeValueAsString(randomProduct2);
            s3 = mapper.writeValueAsString(randomProduct3);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        String s1 = action.getDefaultAdminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(s1);
        HttpEntity<String> request = new HttpEntity<>(s, headers);
        HttpEntity<String> request2 = new HttpEntity<>(s2, headers);
        HttpEntity<String> request3 = new HttpEntity<>(s3, headers);

        String url = UserAction.proxyUrl + UserAction.PRODUCT_SVC + "/productDetails";
        ResponseEntity<String> exchange = action.restTemplate.exchange(url, HttpMethod.POST, request, String.class);
        ResponseEntity<String> exchange2 = action.restTemplate.exchange(url, HttpMethod.POST, request2, String.class);
        ResponseEntity<String> exchange3 = action.restTemplate.exchange(url, HttpMethod.POST, request3, String.class);
        String productId = exchange.getHeaders().getLocation().toString();
        String productId2 = exchange2.getHeaders().getLocation().toString();
        String productId3 = exchange3.getHeaders().getLocation().toString();

        Integer threadCount = threads;
        HashMap<String, String> stringStringHashMap = new HashMap<>();
        stringStringHashMap.put(productId, "1");
        stringStringHashMap.put(productId2, "1");
        stringStringHashMap.put(productId3, "1");
        HttpHeaders headers2 = new HttpHeaders();
        headers2.setBearerAuth(action.getClientCredentialFlowResponse(USER_PROFILE_ID, USER_PROFILE_SECRET).getBody().getValue());
        headers2.setContentType(MediaType.APPLICATION_JSON);
        String swappedProduct = null;
        String replace = null;
        try {
            swappedProduct = mapper.writeValueAsString(stringStringHashMap);
            replace = swappedProduct.replace(productId3, "temp").replace(productId, productId3).replace("temp", productId);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        HttpEntity<String> listHttpEntity = new HttpEntity<>(swappedProduct, headers2);
        HttpEntity<String> listHttpEntity2 = new HttpEntity<>(replace, headers2);
        ArrayList<Integer> integers = new ArrayList<>();
        integers.add(200);
        integers.add(400);
        integers.add(500);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                ResponseEntity<Object> exchange = action.restTemplate.exchange(url2 + UUID.randomUUID().toString(), HttpMethod.PUT, listHttpEntity, Object.class);
                if (exchange.getStatusCodeValue() == 200) {
                    iniOrderStorage.decrementAndGet();
                    iniOrderStorage2.decrementAndGet();
                    iniOrderStorage3.decrementAndGet();
                }
                Assert.assertTrue("expected status code but is " + exchange.getStatusCodeValue(), integers.contains(exchange.getStatusCodeValue()));
            }
        };
        Runnable runnable2 = new Runnable() {
            @Override
            public void run() {
                ResponseEntity<Object> exchange = action.restTemplate.exchange(url2 + UUID.randomUUID().toString(), HttpMethod.PUT, listHttpEntity2, Object.class);
                if (exchange.getStatusCodeValue() == 200) {
                    iniOrderStorage.decrementAndGet();
                    iniOrderStorage2.decrementAndGet();
                    iniOrderStorage3.decrementAndGet();
                }
                Assert.assertTrue("expected status code but is " + exchange.getStatusCodeValue(), integers.contains(exchange.getStatusCodeValue()));
            }
        };
        ArrayList<Runnable> runnables = new ArrayList<>();
        IntStream.range(0, threadCount).forEach(e -> {
            runnables.add(runnable);
            runnables.add(runnable2);
        });
        try {
            assertConcurrent("", runnables, 30000);
            // get product order count
            ResponseEntity<ProductDetail> exchange1 = action.restTemplate.exchange(url + "/" + productId, HttpMethod.GET, null, ProductDetail.class);
            ResponseEntity<ProductDetail> exchange4 = action.restTemplate.exchange(url + "/" + productId2, HttpMethod.GET, null, ProductDetail.class);
            assertTrue("remain storage should be " + iniOrderStorage.get(), exchange1.getBody().getOrderStorage().equals(iniOrderStorage.get()));
            assertTrue("remain storage should be " + iniOrderStorage2.get(), exchange4.getBody().getOrderStorage().equals(iniOrderStorage2.get()));
            log.info("failed request number is {}", threadCount*2 - (initial - iniOrderStorage.get()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void transactionalDecreaseEnough() {
        create_three_product_then_concurrent_decrease_diff_product_concurrent(50, 30);
    }

    @Test
    public void transactionalDecreaseNotEnough() {
        create_three_product_then_concurrent_decrease_diff_product_concurrent(100, 30);
    }

    /**
     * copied from https://www.planetgeek.ch/2009/08/25/how-to-find-a-concurrency-bug-with-java/
     *
     * @param message
     * @param runnables
     * @param maxTimeoutSeconds
     * @throws InterruptedException
     */
    public static void assertConcurrent(final String message, final List<? extends Runnable> runnables, final int maxTimeoutSeconds) throws InterruptedException {
        final int numThreads = runnables.size();
        final List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<Throwable>());
        final ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        try {
            final CountDownLatch allExecutorThreadsReady = new CountDownLatch(numThreads);
            final CountDownLatch afterInitBlocker = new CountDownLatch(1);
            final CountDownLatch allDone = new CountDownLatch(numThreads);
            for (final Runnable submittedTestRunnable : runnables) {
                threadPool.submit(new Runnable() {
                    public void run() {
                        allExecutorThreadsReady.countDown();
                        try {
                            afterInitBlocker.await();
                            submittedTestRunnable.run();
                        } catch (final Throwable e) {
                            exceptions.add(e);
                        } finally {
                            allDone.countDown();
                        }
                    }
                });
            }
            // wait until all threads are ready
            assertTrue("Timeout initializing threads! Perform long lasting initializations before passing runnables to assertConcurrent", allExecutorThreadsReady.await(runnables.size() * 10, TimeUnit.MILLISECONDS));
            // start all test runners
            afterInitBlocker.countDown();
            assertTrue(message + " timeout! More than" + maxTimeoutSeconds + "seconds", allDone.await(maxTimeoutSeconds, TimeUnit.SECONDS));
        } finally {
            threadPool.shutdownNow();
        }
        assertTrue(message + "failed with exception(s)" + exceptions, exceptions.isEmpty());
    }

}