package com.hw.longrun;

import com.hw.helper.*;
import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.junit.*;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertTrue;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
/**
 * create clean env before test
 * UPDATE product_simple SET order_storage = 1000;
 * UPDATE product_simple SET actual_storage = 500;
 * UPDATE product_simple SET sales = NULL ;
 */
public class LongRunTest {
    @Autowired
    UserAction action;
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
    @Ignore
    public void long_running_job_create_order_randomly_but_not_pay() {
        ResourceOwner resourceOwner1 = action.testUser.get(new Random().nextInt(5));
        String defaultUserToken = action.getLoginTokenResponse(resourceOwner1.getEmail(), resourceOwner1.getPassword()).getBody().getValue();
        String profileId1 = action.getProfileId(defaultUserToken);
        OrderDetail orderDetailForUser = action.createOrderDetailForUser(defaultUserToken, profileId1);
        String url3 = UserAction.proxyUrl + UserAction.PROFILE_SVC + "/profiles/" + profileId1 + "/orders";
        ResponseEntity<String> exchange = action.restTemplate.exchange(url3, HttpMethod.POST, action.getHttpRequest(defaultUserToken, orderDetailForUser), String.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
        Assert.assertNotNull(exchange.getHeaders().getLocation().toString());
    }

    @Test
    @Ignore
    public void long_running_job_create_order_randomly_pay_randomly() {
        ResourceOwner resourceOwner1 = action.testUser.get(new Random().nextInt(5));
        String defaultUserToken = action.getLoginTokenResponse(resourceOwner1.getEmail(), resourceOwner1.getPassword()).getBody().getValue();
        String profileId1 = action.getProfileId(defaultUserToken);
        OrderDetail orderDetailForUser = action.createOrderDetailForUser(defaultUserToken, profileId1);
        String url3 = UserAction.proxyUrl + UserAction.PROFILE_SVC + "/profiles/" + profileId1 + "/orders";
        ResponseEntity<String> exchange = action.restTemplate.exchange(url3, HttpMethod.POST, action.getHttpRequest(defaultUserToken, orderDetailForUser), String.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
        Assert.assertNotNull(exchange.getHeaders().getLocation().toString());
        String orderId = action.getOrderId(exchange.getHeaders());
        if (new Random().nextInt(10) > 5) {
            //randomly pay
            String url4 = UserAction.proxyUrl + UserAction.PROFILE_SVC + "/profiles/" + profileId1 + "/orders/" + orderId + "/confirm";
            ResponseEntity<String> exchange7 = action.restTemplate.exchange(url4, HttpMethod.GET, action.getHttpRequest(defaultUserToken), String.class);
            Assert.assertEquals(HttpStatus.OK, exchange7.getStatusCode());
            Boolean read = JsonPath.read(exchange7.getBody(), "$.paymentStatus");
            Assert.assertEquals(true, read);
        }
    }

    @Test
    @Ignore
    public void long_running_job_create_order_randomly_pay_randomly_replace_randomly_after_sometime_validate_order_storage_actually_storage_with_sales() {
        // randomly pick test user
        ResourceOwner resourceOwner1 = action.testUser.get(new Random().nextInt(5));
        String defaultUserToken = action.getLoginTokenResponse(resourceOwner1.getEmail(), resourceOwner1.getPassword()).getBody().getValue();
        String profileId1 = action.getProfileId(defaultUserToken);


        OrderDetail orderDetailForUser = action.createOrderDetailForUser(defaultUserToken, profileId1);
        String url3 = UserAction.proxyUrl + UserAction.PROFILE_SVC + "/profiles/" + profileId1 + "/orders";
        ResponseEntity<String> exchange = action.restTemplate.exchange(url3, HttpMethod.POST, action.getHttpRequest(defaultUserToken, orderDetailForUser), String.class);
        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
        Assert.assertNotNull(exchange.getHeaders().getLocation().toString());
        String orderId = action.getOrderId(exchange.getHeaders());
        int i = new Random().nextInt(20);
        if (i >= 0 && i < 5) {
            log.info("randomly pay");
            //randomly pay
            String url4 = UserAction.proxyUrl + UserAction.PROFILE_SVC + "/profiles/" + profileId1 + "/orders/" + orderId + "/confirm";
            ResponseEntity<String> exchange7 = action.restTemplate.exchange(url4, HttpMethod.GET, action.getHttpRequest(defaultUserToken), String.class);
            Assert.assertEquals(HttpStatus.OK, exchange7.getStatusCode());
            Boolean read = JsonPath.read(exchange7.getBody(), "$.paymentStatus");
            Assert.assertEquals(true, read);
        } else if (i >= 5 && i < 10) {
            log.info("randomly replace");
            // randomly replace old order
            String url4 = UserAction.proxyUrl + UserAction.PROFILE_SVC + "/profiles/" + profileId1 + "/orders";
            ParameterizedTypeReference<List<OrderDetail>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<List<OrderDetail>> exchange3 = action.restTemplate.exchange(url4, HttpMethod.GET, action.getHttpRequest(defaultUserToken), responseType);
            List<OrderDetail> body = exchange3.getBody();
            List<OrderDetail> collect = body.stream().filter(e -> e.getPaymentStatus().equals(PaymentStatus.unpaid)).collect(Collectors.toList());
            int size = collect.size();
            if (size > 0) {
                OrderDetail orderDetail = collect.get(new Random().nextInt(size));
                String url8 = UserAction.proxyUrl + UserAction.PROFILE_SVC + "/profiles/" + profileId1 + "/orders/" + orderDetail.getId();
                ResponseEntity<OrderDetail> exchange8 = action.restTemplate.exchange(url8, HttpMethod.GET, action.getHttpRequest(defaultUserToken), OrderDetail.class);
                Assert.assertEquals(HttpStatus.OK, exchange8.getStatusCode());
                OrderDetail body1 = exchange8.getBody();

                String url5 = UserAction.proxyUrl + UserAction.PROFILE_SVC + "/profiles/" + profileId1 + "/orders/" + orderDetail.getId() + "/replace";
                ResponseEntity<String> exchange5 = action.restTemplate.exchange(url5, HttpMethod.PUT, action.getHttpRequest(defaultUserToken, body1), String.class);
                Assert.assertEquals(HttpStatus.OK, exchange5.getStatusCode());
                if (i <= 7) {
                    log.info("after replace, directly pay");
                    // after replace, directly pay
                    String url6 = UserAction.proxyUrl + UserAction.PROFILE_SVC + "/profiles/" + profileId1 + "/orders/" + orderDetail.getId() + "/confirm";
                    ResponseEntity<String> exchange7 = action.restTemplate.exchange(url6, HttpMethod.GET, action.getHttpRequest(defaultUserToken), String.class);
                    Assert.assertEquals(HttpStatus.OK, exchange7.getStatusCode());
                    Boolean read = JsonPath.read(exchange7.getBody(), "$.paymentStatus");
                    Assert.assertEquals(true, read);
                } else {
                    log.info("after replace, not pay");
                    // after replace, not pay

                }

            }
            log.info("no pending order");
        } else if (i >= 10) {
            // randomly not pay
            log.info("randomly not pay");
        }
    }

    @Test
    public void long_running_job_concurrent_create_order_randomly_pay_randomly_replace_randomly_after_sometime_validate_order_storage_actually_storage_with_sales() {
        int numOfConcurrent = 10;
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                // randomly pick test user
                log.info("thread start ");
                ResourceOwner resourceOwner1 = action.testUser.get(new Random().nextInt(5));
                log.info("randomly pick test user " + resourceOwner1.toString());
                String defaultUserToken = action.getLoginTokenResponse(resourceOwner1.getEmail(), resourceOwner1.getPassword()).getBody().getValue();
                String profileId1 = action.getProfileId(defaultUserToken);
                OrderDetail orderDetailForUser = action.createOrderDetailForUser(defaultUserToken, profileId1);
                String url3 = UserAction.proxyUrl + UserAction.PROFILE_SVC + "/profiles/" + profileId1 + "/orders";
                ResponseEntity<String> exchange = action.restTemplate.exchange(url3, HttpMethod.POST, action.getHttpRequest(defaultUserToken, orderDetailForUser), String.class);
                Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
                Assert.assertNotNull(exchange.getHeaders().getLocation().toString());
                String orderId = action.getOrderId(exchange.getHeaders());
                int i = new Random().nextInt(20);
                if (i >= 0 && i < 5) {
                    //randomly pay
                    log.info("randomly pay");
                    String url4 = UserAction.proxyUrl + UserAction.PROFILE_SVC + "/profiles/" + profileId1 + "/orders/" + orderId + "/confirm";
                    ResponseEntity<String> exchange7 = action.restTemplate.exchange(url4, HttpMethod.GET, action.getHttpRequest(defaultUserToken), String.class);
                    Assert.assertEquals(HttpStatus.OK, exchange7.getStatusCode());
                    Boolean read = JsonPath.read(exchange7.getBody(), "$.paymentStatus");
                    Assert.assertEquals(true, read);
                } else if (i >= 5 && i < 10) {
                    log.info("randomly replace");
                    // randomly replace old order
                    String url4 = UserAction.proxyUrl + UserAction.PROFILE_SVC + "/profiles/" + profileId1 + "/orders";
                    ParameterizedTypeReference<List<OrderDetail>> responseType = new ParameterizedTypeReference<>() {
                    };
                    ResponseEntity<List<OrderDetail>> exchange3 = action.restTemplate.exchange(url4, HttpMethod.GET, action.getHttpRequest(defaultUserToken), responseType);
                    List<OrderDetail> body = exchange3.getBody();
                    if (body != null) {
                        log.info("pending order found");
                        List<OrderDetail> collect = body.stream().filter(e -> e.getPaymentStatus().equals(PaymentStatus.unpaid)).collect(Collectors.toList());
                        int size = collect.size();
                        if (size > 0) {
                            OrderDetail orderDetail = collect.get(new Random().nextInt(size));
                            String url8 = UserAction.proxyUrl + UserAction.PROFILE_SVC + "/profiles/" + profileId1 + "/orders/" + orderDetail.getId();
                            ResponseEntity<OrderDetail> exchange8 = action.restTemplate.exchange(url8, HttpMethod.GET, action.getHttpRequest(defaultUserToken), OrderDetail.class);
                            Assert.assertEquals(HttpStatus.OK, exchange8.getStatusCode());
                            OrderDetail body1 = exchange8.getBody();

                            String url5 = UserAction.proxyUrl + UserAction.PROFILE_SVC + "/profiles/" + profileId1 + "/orders/" + orderDetail.getId() + "/replace";
                            ResponseEntity<String> exchange5 = action.restTemplate.exchange(url5, HttpMethod.PUT, action.getHttpRequest(defaultUserToken, body1), String.class);
                            ArrayList<Integer> integers2 = new ArrayList<>();
                            integers2.add(200);
                            integers2.add(400);
                            integers2.add(500);
                            Assert.assertTrue("replace success or done by other thread", integers2.contains(exchange5.getStatusCode().value()));
                            if (i <= 7 && exchange5.getStatusCode().value() == 200) {
                                log.info("after replace, directly pay");
                                // after replace, directly pay
                                String url6 = UserAction.proxyUrl + UserAction.PROFILE_SVC + "/profiles/" + profileId1 + "/orders/" + orderDetail.getId() + "/confirm";
                                ResponseEntity<String> exchange7 = action.restTemplate.exchange(url6, HttpMethod.GET, action.getHttpRequest(defaultUserToken), String.class);
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
