package com.hw.chaos;

import com.hw.TestHelper;
import com.hw.helper.*;
import com.jayway.jsonpath.JsonPath;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static com.hw.integration.profile.OrderTest.getOrderId;
import static org.junit.Assert.assertTrue;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
/**
 * create clean env before test
 * UPDATE product_detail SET order_storage = 1000;
 * UPDATE product_detail SET actual_storage = 500;
 * UPDATE product_detail SET sales = NULL ;
 * DELETE FROM change_record ;
 */
public class ChaosTest {
    @Autowired
    UserAction action;
    @Autowired
    TestHelper helper;
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
    public void long_running_job_concurrent_create_order_randomly_pay_randomly_replace_randomly_after_sometime_validate_order_storage_actually_storage_with_sales() {
        int numOfConcurrent = 10;

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                // randomly pick test user
                log.info("thread start ");
                ArrayList<Integer> integers4 = new ArrayList<>();
                integers4.add(200);
                integers4.add(500);
                ResourceOwner resourceOwner1 = action.testUser.get(new Random().nextInt(5));
                String defaultUserToken = action.getJwtPassword(resourceOwner1.getEmail(), resourceOwner1.getPassword()).getBody().getValue();
                log.info("defaultUserToken " + defaultUserToken);
                OrderDetail orderDetailForUser = action.createOrderDetailForUser(defaultUserToken);
                log.info("order with id created {}", orderDetailForUser.getId());
                String url3 = helper.getUserProfileUrl("/orders/user");
                ResponseEntity<String> exchange = action.restTemplate.exchange(url3, HttpMethod.POST, action.getHttpRequestAsString(defaultUserToken, orderDetailForUser), String.class);
                log.info("create status code " + exchange.getStatusCode());
                Assert.assertTrue("create success or concurrent-failure", integers4.contains(exchange.getStatusCode().value()));
                int i = new Random().nextInt(20);
                if (i >= 0 && i < 5) {
                    if (exchange.getStatusCode().is2xxSuccessful()) {
                        //randomly pay
                        log.info("randomly pay");
                        Assert.assertNotNull(exchange.getHeaders().getLocation().toString());
                        String orderId = getOrderId(exchange.getHeaders().getLocation().toString());
                        String url4 = helper.getUserProfileUrl("/orders/user/" + orderId + "/confirm");
                        ResponseEntity<String> exchange7 = action.restTemplate.exchange(url4, HttpMethod.PUT, action.getHttpRequest(defaultUserToken), String.class);
                        Assert.assertEquals(HttpStatus.OK, exchange7.getStatusCode());
                        Boolean read = JsonPath.read(exchange7.getBody(), "$.paymentStatus");
                        Assert.assertEquals(true, read);
                    }
                } else if (i >= 5 && i < 10) {
                    log.info("randomly replace");
                    // randomly replace order, regardless it's state
                    String url4 = helper.getUserProfileUrl("/orders/user");
                    ResponseEntity<SumTotalOrder> exchange3 = action.restTemplate.exchange(url4, HttpMethod.GET, action.getHttpRequest(defaultUserToken), SumTotalOrder.class);
                    List<OrderDetail> body = exchange3.getBody().getData();
                    if (body != null) {
                        int size = body.size();
                        if (size > 0) {
                            OrderDetail orderDetail = body.get(new Random().nextInt(size));
                            String url8 = helper.getUserProfileUrl("/orders/user/" + orderDetail.getId());
                            ResponseEntity<OrderDetail> exchange8 = action.restTemplate.exchange(url8, HttpMethod.GET, action.getHttpRequest(defaultUserToken), OrderDetail.class);
                            Assert.assertEquals(HttpStatus.OK, exchange8.getStatusCode());
                            OrderDetail body1 = exchange8.getBody();

                            String url5 = helper.getUserProfileUrl("/orders/user/" + orderDetail.getId() + "/reserve");
                            ResponseEntity<String> exchange5 = action.restTemplate.exchange(url5, HttpMethod.PUT, action.getHttpRequestAsString(defaultUserToken, body1), String.class);
                            ArrayList<Integer> integers2 = new ArrayList<>();
                            integers2.add(200);
                            integers2.add(400);
                            integers2.add(500);
                            Assert.assertTrue("replace success or done by other thread", integers2.contains(exchange5.getStatusCode().value()));
                            if (i <= 7 && exchange5.getStatusCode().value() == 200) {
                                log.info("after replace, directly pay");
                                // after replace, directly pay
                                String url6 = helper.getUserProfileUrl("/orders/user/" + orderDetail.getId() + "/confirm");
                                ResponseEntity<String> exchange7 = action.restTemplate.exchange(url6, HttpMethod.PUT, action.getHttpRequest(defaultUserToken), String.class);
                                log.info("exchange7.getBody() {}", exchange7.getBody());
                                ArrayList<Integer> integers = new ArrayList<>();
                                integers.add(200);
                                integers.add(400);
                                integers.add(500);
                                Assert.assertTrue("order pay success or already paid", integers.contains(exchange7.getStatusCode().value()));
                                Boolean read = JsonPath.read(exchange7.getBody(), "$.paymentStatus");
                                Assert.assertEquals(true, read);
                            } else {
                                log.info("after replace, not pay");
                                // after replace, not pay

                            }

                        }
                    }
                    log.info("no pending order");
                } else if (i >= 10) {
                    // randomly not pay
                    log.info("randomly not pay");
                }
            }
        };
        ArrayList<Runnable> runnables = new ArrayList<>();
        IntStream.range(0, numOfConcurrent).forEach(e -> {
            runnables.add(runnable);
        });
        try {
            assertConcurrent("", runnables, 300000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

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
