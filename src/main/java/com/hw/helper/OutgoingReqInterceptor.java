package com.hw.helper;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Data
public class OutgoingReqInterceptor implements ClientHttpRequestInterceptor {
    private UUID testId;

    public OutgoingReqInterceptor(UUID testId) {
        this.testId = testId;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest httpRequest, byte[] bytes, ClientHttpRequestExecution clientHttpRequestExecution) throws IOException {
        httpRequest.getHeaders().set("testId", testId.toString());
        httpRequest.getHeaders().set("changeId", UUID.randomUUID().toString());
        return clientHttpRequestExecution.execute(httpRequest, bytes);
    }
}
