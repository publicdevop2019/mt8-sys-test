package com.hw;

import com.hw.integration.oauth2.*;
import com.hw.integration.product.CatalogTest;
import com.hw.integration.product.ProductTest;
import com.hw.integration.profile.AddressTest;
import com.hw.integration.profile.CartTest;
import com.hw.integration.profile.OrderTest;
import com.hw.integration.proxy.*;
import com.hw.integration.security.SecurityTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses(value = {
        AuthorizationCodeTest.class,
        ClientControllerTest.class,
        ClientCredentialsTest.class,
        PasswordFlowTest.class,
        ResourceOwnerControllerTest.class,
//        ProductServiceTest.class,
        CatalogTest.class,
        ProductTest.class,
        AddressTest.class,
        CartTest.class,
        OrderTest.class,
//        OrderServiceTest.class,
        RevokeTokenTest.class,
        ClientEPSecurityTest.class,
        CORSTest.class,
        ResourceOwnerEPSecurityTest.class,
        EndpointTest.class,
        SecurityTest.class

})
public class IntegrationTestSuite {
}
