package com.hw.helper;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class ResourceOwner {

    protected String id;

    private String email;

    private String password;

    private Boolean locked;

    private List<ResourceOwnerAuthorityEnum> grantedAuthorities;

    private Set<String> resourceId;
    private Integer version;

}
