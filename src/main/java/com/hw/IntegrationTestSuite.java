package com.hw;

import com.hw.concurrent.OrderServiceTest;
import com.hw.concurrent.ProductServiceTest;
import com.hw.integration.oauth2.*;
import com.hw.integration.product.CategoryTest;
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
        ProductServiceTest.class,
        CategoryTest.class,
        ProductTest.class,
        AddressTest.class,
        CartTest.class,
        OrderTest.class,
        OrderServiceTest.class,
        BlackListControllerTest.class,
        ClientEPSecurityTest.class,
        CORSTest.class,
        ResourceOwnerEPSecurityTest.class,
        SecurityProfileControllerTest.class,
        SecurityTest.class

})
public class IntegrationTestSuite {
}
