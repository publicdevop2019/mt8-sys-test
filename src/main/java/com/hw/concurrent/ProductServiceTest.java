package com.hw.concurrent;

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

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static com.hw.helper.UserAction.*;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@Slf4j
@SpringBootTest
public class ProductServiceTest {
    public static final String URL_2 = UserAction.proxyUrl + UserAction.PRODUCT_SVC + "/products/app/storageOrder/decrease";
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
    public void create_product_then_concurrent_decrease_order_storage() {
        AtomicInteger iniOrderStorage = new AtomicInteger(1000);
        ResponseEntity<String> exchange = action.createRandomProductDetail(null, iniOrderStorage.get());
        Long productId = Long.parseLong(exchange.getHeaders().getLocation().toString());
        StorageChangeCommon storageChangeCommon = new StorageChangeCommon();
        StorageChangeDetail storageChangeDetail = new StorageChangeDetail();
        storageChangeDetail.setAmount(1);
        storageChangeDetail.setAttributeSales(new HashSet<>(List.of(TEST_TEST_VALUE)));
        storageChangeDetail.setProductId(productId);
        storageChangeCommon.setChangeList(List.of(storageChangeDetail));
        Integer threadCount = 50;
        HttpHeaders headers2 = new HttpHeaders();
        headers2.setBearerAuth(action.getClientCredentialFlowResponse(USER_PROFILE_ID, USER_PROFILE_SECRET).getBody().getValue());
        ArrayList<Integer> integers = new ArrayList<>();
        integers.add(200);
        integers.add(400);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                storageChangeCommon.setTxId(UUID.randomUUID().toString());
                HttpEntity<StorageChangeCommon> listHttpEntity = new HttpEntity<>(storageChangeCommon, headers2);
                ResponseEntity<Object> exchange = action.restTemplate.exchange(URL_2, HttpMethod.PUT, listHttpEntity, Object.class);
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
            ResponseEntity<ProductDetailAdminRepresentation> productDetailAdminRepresentationResponseEntity = action.readProductDetailByIdAdmin(productId);
            assertTrue("remain storage should be " + iniOrderStorage.get(), productDetailAdminRepresentationResponseEntity.getBody().getSkus().get(0).getStorageOrder().equals(iniOrderStorage.get()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void create_product_then_concurrent_decrease_same_opt() {
        Integer iniOrderStorage = 1000;
        ResponseEntity<String> exchange = action.createRandomProductDetail(null, iniOrderStorage);
        Long productId = Long.parseLong(exchange.getHeaders().getLocation().toString());
        StorageChangeCommon storageChangeCommon = new StorageChangeCommon();
        StorageChangeDetail storageChangeDetail = new StorageChangeDetail();
        storageChangeDetail.setAmount(1);
        storageChangeDetail.setAttributeSales(new HashSet<>(List.of(TEST_TEST_VALUE)));
        storageChangeDetail.setProductId(productId);
        storageChangeCommon.setChangeList(List.of(storageChangeDetail));
        storageChangeCommon.setTxId(UUID.randomUUID().toString());
        Integer threadCount = 20;
        Integer expected = iniOrderStorage - 1;
        HttpHeaders headers2 = new HttpHeaders();
        headers2.setBearerAuth(action.getClientCredentialFlowResponse(USER_PROFILE_ID, USER_PROFILE_SECRET).getBody().getValue());
        HttpEntity<StorageChangeCommon> listHttpEntity = new HttpEntity<>(storageChangeCommon, headers2);
        ArrayList<Integer> integers = new ArrayList<>();
        integers.add(200);
        integers.add(400);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                ResponseEntity<Object> exchange = action.restTemplate.exchange(URL_2, HttpMethod.PUT, listHttpEntity, Object.class);
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
            ResponseEntity<ProductDetailAdminRepresentation> productDetailAdminRepresentationResponseEntity = action.readProductDetailByIdAdmin(productId);
            assertTrue("remain storage should be " + expected, productDetailAdminRepresentationResponseEntity.getBody().getSkus().get(0).getStorageOrder().equals(expected));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * if lock is pessimistic then deadlock exception will detected
     */
    public void create_three_product_then_concurrent_decrease_diff_product_concurrent(Integer initialStorage, Integer threads) {
        AtomicInteger iniOrderStorage = new AtomicInteger(initialStorage);
        AtomicInteger iniOrderStorage2 = new AtomicInteger(initialStorage);
        AtomicInteger iniOrderStorage3 = new AtomicInteger(initialStorage);
        ResponseEntity<String> exchange = action.createRandomProductDetail(null, initialStorage);
        ResponseEntity<String> exchange2 = action.createRandomProductDetail(null, initialStorage);
        ResponseEntity<String> exchange3 = action.createRandomProductDetail(null, initialStorage);
        Long productId = Long.parseLong(exchange.getHeaders().getLocation().toString());
        Long productId2 = Long.parseLong(exchange2.getHeaders().getLocation().toString());
        Long productId3 = Long.parseLong(exchange3.getHeaders().getLocation().toString());

        StorageChangeCommon storageChangeCommon = new StorageChangeCommon();
        StorageChangeCommon storageChangeCommon2 = new StorageChangeCommon();
        StorageChangeDetail storageChangeDetail = new StorageChangeDetail();
        storageChangeDetail.setAmount(1);
        storageChangeDetail.setAttributeSales(new HashSet<>(List.of(TEST_TEST_VALUE)));
        storageChangeDetail.setProductId(productId);

        StorageChangeDetail storageChangeDetail2 = new StorageChangeDetail();
        storageChangeDetail2.setAmount(1);
        storageChangeDetail2.setAttributeSales(new HashSet<>(List.of(TEST_TEST_VALUE)));
        storageChangeDetail2.setProductId(productId2);

        StorageChangeDetail storageChangeDetail3 = new StorageChangeDetail();
        storageChangeDetail3.setAmount(1);
        storageChangeDetail3.setAttributeSales(new HashSet<>(List.of(TEST_TEST_VALUE)));
        storageChangeDetail3.setProductId(productId3);

        storageChangeCommon.setChangeList(List.of(storageChangeDetail3, storageChangeDetail, storageChangeDetail2));
        storageChangeCommon2.setChangeList(List.of(storageChangeDetail, storageChangeDetail2, storageChangeDetail3));
        HttpHeaders headers2 = new HttpHeaders();
        headers2.setBearerAuth(action.getClientCredentialFlowResponse(USER_PROFILE_ID, USER_PROFILE_SECRET).getBody().getValue());
        headers2.setContentType(MediaType.APPLICATION_JSON);
        ArrayList<Integer> integers = new ArrayList<>();
        integers.add(200);
        integers.add(400);
        integers.add(500);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                storageChangeCommon.setTxId(UUID.randomUUID().toString());
                HttpEntity<StorageChangeCommon> listHttpEntity = new HttpEntity<>(storageChangeCommon, headers2);
                ResponseEntity<Object> exchange = action.restTemplate.exchange(URL_2, HttpMethod.PUT, listHttpEntity, Object.class);
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
                storageChangeCommon2.setTxId(UUID.randomUUID().toString());
                HttpEntity<StorageChangeCommon> listHttpEntity2 = new HttpEntity<>(storageChangeCommon2, headers2);
                ResponseEntity<Object> exchange = action.restTemplate.exchange(URL_2, HttpMethod.PUT, listHttpEntity2, Object.class);
                if (exchange.getStatusCodeValue() == 200) {
                    iniOrderStorage.decrementAndGet();
                    iniOrderStorage2.decrementAndGet();
                    iniOrderStorage3.decrementAndGet();
                }
                Assert.assertTrue("expected status code but is " + exchange.getStatusCodeValue(), integers.contains(exchange.getStatusCodeValue()));
            }
        };
        ArrayList<Runnable> runnables = new ArrayList<>();
        IntStream.range(0, threads).forEach(e -> {
            runnables.add(runnable);
            runnables.add(runnable2);
        });
        try {
            assertConcurrent("", runnables, 30000);
            // get product order count
            ResponseEntity<ProductDetailAdminRepresentation> ex = action.readProductDetailByIdAdmin(productId);
            assertTrue("remain storage should be " + iniOrderStorage.get(), ex.getBody().getSkus().get(0).getStorageOrder().equals(iniOrderStorage.get()));
            ResponseEntity<ProductDetailAdminRepresentation> ex2 = action.readProductDetailByIdAdmin(productId2);
            assertTrue("remain storage should be " + iniOrderStorage2.get(), ex2.getBody().getSkus().get(0).getStorageOrder().equals(iniOrderStorage2.get()));
            ResponseEntity<ProductDetailAdminRepresentation> ex3 = action.readProductDetailByIdAdmin(productId3);
            assertTrue("remain storage should be " + iniOrderStorage3.get(), ex3.getBody().getSkus().get(0).getStorageOrder().equals(iniOrderStorage3.get()));
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