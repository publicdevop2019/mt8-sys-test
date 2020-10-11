package com.hw.helper;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class Client {

    private String clientSecret;

    private Set<GrantTypeEnum> grantTypeEnums;

    private List<ClientAuthorityEnum> grantedAuthorities;

    private Set<ScopeEnum> scopeEnums;

    private Integer accessTokenValiditySeconds;

    private Set<String> registeredRedirectUri;

    private Integer refreshTokenValiditySeconds;

    private Set<String> resourceIds;

    private Boolean resourceIndicator;

    private Boolean autoApprove;

    private Boolean hasSecret;

}
