package com.hw.helper;

import lombok.Data;

@Data
public class SecurityProfile {
    private String expression;

    private String resourceID;

    private String path;

    private String method;

    private String url;

    private Long id;
}
