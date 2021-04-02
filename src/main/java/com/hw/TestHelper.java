package com.hw;

import com.netflix.discovery.EurekaClient;
import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Getter
public class TestHelper {
    @Getter(AccessLevel.NONE)
    @Autowired
    private EurekaClient eurekaClient;

    public String getAccessUrl(String path) {
        String normalized = removeLeadingSlash(path);
        return eurekaClient.getApplication("OAUTH").getInstances().get(0).getHomePageUrl() + normalized;
    }

    public String getMallUrl(String path) {
        String normalized = removeLeadingSlash(path);
        return eurekaClient.getApplication("PRODUCT").getInstances().get(0).getHomePageUrl() + normalized;
    }

    public String getUserProfileUrl(String path) {
        String normalized = removeLeadingSlash(path);
        return eurekaClient.getApplication("PROFILE").getInstances().get(0).getHomePageUrl() + normalized;
    }

    private String removeLeadingSlash(String path) {
        return path.replaceAll("^/+", "");
    }
}
