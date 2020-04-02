package com.hw.helper;

import lombok.Data;

@Data
public class SecurityProfile {
    private String expression;

    private String resourceId;

    private String lookupPath;

    private String method;

    private String url;

    private Long id;
    private String scheme;
    private String userInfo;
    private String host;
    private Integer port;
    private String path;
    private String query;
    private String fragment;
}
