package com.hw;

import com.hw.integration.identityaccess.oauth2.*;
import com.hw.integration.product.CatalogTest;
import com.hw.integration.product.ProductTest;
import com.hw.integration.profile.AddressTest;
import com.hw.integration.profile.CartTest;
import com.hw.integration.profile.OrderTest;
import com.hw.integration.identityaccess.proxy.*;
import com.hw.integration.identityaccess.security.SecurityTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses(value = {
        AuthorizationCodeTest.class,
        BizClientTest.class,
        ClientCredentialsTest.class,
        PasswordFlowTest.class,
        BIzUserTest.class,
//        ProductServiceTest.class,
        CatalogTest.class,
        ProductTest.class,
        AddressTest.class,
        CartTest.class,
        OrderTest.class,
//        OrderServiceTest.class,
        RevokeTokenTest.class,
        BizClientApiSecurityTest.class,
        CORSTest.class,
        BizUserApiSecurityTest.class,
        EndpointTest.class,
        SecurityTest.class

})
public class IntegrationTestSuite {
}
